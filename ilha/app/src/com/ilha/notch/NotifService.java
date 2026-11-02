package com.ilha.notch;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reads notifications and media sessions and turns them into island items. */
public class NotifService extends NotificationListenerService {
    private static final long PAUSED_KEEP_MS = 90_000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final LinkedHashMap<String, Item> liveNotifs = new LinkedHashMap<>();
    private final Map<String, Long> lastPopup = new HashMap<>();
    private final Map<MediaController, MediaController.Callback> callbacks = new HashMap<>();
    private final Map<String, Long> pausedSince = new HashMap<>();
    private MediaSessionManager msm;
    private MediaSessionManager.OnActiveSessionsChangedListener sessionsListener;
    private List<MediaController> controllers = new ArrayList<>();
    /** Sessions reported by the system as active. */
    private List<MediaController> activeSessions = new ArrayList<>();
    /** Sessions found in media notifications (YouTube and others that the active list misses). */
    private final Map<String, MediaController> notifSessions = new HashMap<>();
    /** Title/text/picture of each app's media notification, used when the session has no metadata. */
    private final Map<String, Item> mediaNotifs = new HashMap<>();

    private final Runnable republish = new Runnable() {
        @Override public void run() { publish(); }
    };

    @Override
    public void onListenerConnected() {
        Hub.notif = this;
        msm = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
        final ComponentName me = new ComponentName(this, NotifService.class);
        sessionsListener = new MediaSessionManager.OnActiveSessionsChangedListener() {
            @Override public void onActiveSessionsChanged(List<MediaController> list) {
                setControllers(list);
            }
        };
        try {
            msm.addOnActiveSessionsChangedListener(sessionsListener, me, handler);
            setControllers(msm.getActiveSessions(me));
        } catch (SecurityException ignored) {
        }
        resync();
    }

    // API 24+; on older systems it is simply never called
    public void onListenerDisconnected() {
        if (Hub.notif == this) Hub.notif = null;
        if (msm != null && sessionsListener != null) msm.removeOnActiveSessionsChangedListener(sessionsListener);
        setControllers(new ArrayList<MediaController>());
    }

    @Override
    public void onDestroy() {
        if (Hub.notif == this) Hub.notif = null;
        super.onDestroy();
    }

    /** Rebuilds the live list from the notifications currently posted (no pop-ups). */
    void resync() {
        liveNotifs.clear();
        try {
            StatusBarNotification[] all = getActiveNotifications();
            if (all != null) for (StatusBarNotification sbn : all) handle(sbn, false);
        } catch (Exception ignored) {
        }
        publish();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        boolean wasLive = liveNotifs.remove(sbn.getKey()) != null;
        handle(sbn, true);
        // e.g. a download that finished: no longer a live activity
        if (wasLive && !liveNotifs.containsKey(sbn.getKey())) publish();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (liveNotifs.remove(sbn.getKey()) != null) publish();
        if (notifSessions.remove(sbn.getKey()) != null) {
            mediaNotifs.remove(sbn.getPackageName());
            setControllers(activeSessions);
        }
        Hub.removed(sbn.getKey());
    }

    private void handle(StatusBarNotification sbn, boolean popup) {
        String pkg = sbn.getPackageName();
        Notification n = sbn.getNotification();
        if (n == null || getPackageName().equals(pkg)) return;
        Bundle ex = n.extras;
        if (ex == null) ex = new Bundle();
        if (ex.containsKey(Notification.EXTRA_MEDIA_SESSION)) {
            mediaNotification(sbn, ex);
            return;
        }

        boolean ongoing = (n.flags & Notification.FLAG_ONGOING_EVENT) != 0
                || (n.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0;
        boolean isCall = Notification.CATEGORY_CALL.equals(n.category);
        boolean chrono = ex.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false);
        int pMax = ex.getInt(Notification.EXTRA_PROGRESS_MAX, 0);
        boolean pInd = ex.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false);
        boolean nav = "navigation".equals(n.category);

        if (popup && !ongoing) Prefs.markSeen(this, pkg);

        if (isCall) {
            if (!Prefs.calls(this)) return;
            boolean incoming = n.fullScreenIntent != null && !chrono;
            if (incoming) {
                if (popup) Hub.notify(build(sbn, Item.INCOMING));
                return;
            }
            if (popup) Hub.removed(sbn.getKey()); // answered: close the incoming-call card
            if (ongoing) {
                Item it = build(sbn, Item.CALL);
                it.chronoBase = n.when;
                putLive(it);
            }
            return;
        }

        if (ongoing) {
            if (!Prefs.live(this) || !Prefs.appAllowed(this, pkg)) return;
            if (pMax > 0 || pInd) {
                Item it = build(sbn, Item.PROGRESS);
                it.max = pMax;
                it.progress = ex.getInt(Notification.EXTRA_PROGRESS, 0);
                it.indeterminate = pInd;
                putLive(it);
            } else if (chrono || nav) {
                Item it = build(sbn, Item.TIMER);
                it.chronoBase = chrono ? n.when : 0;
                it.countDown = ex.getBoolean("android.chronometerCountDown", false);
                putLive(it);
            }
            return;
        }

        if (!popup || !Prefs.notifs(this) || !Prefs.appAllowed(this, pkg)) return;
        if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) return;
        if (n.priority <= Notification.PRIORITY_MIN) return;
        Long last = lastPopup.get(sbn.getKey());
        if ((n.flags & Notification.FLAG_ONLY_ALERT_ONCE) != 0 && last != null) return;
        long now = SystemClock.elapsedRealtime();
        if (last != null && now - last < 1500) return; // quick repeated updates
        lastPopup.put(sbn.getKey(), now);
        Item it = build(sbn, Item.NOTIF);
        if (it.title == null && it.text == null) return;
        Hub.notify(it);
    }

    private void putLive(Item it) {
        liveNotifs.put(it.key, it);
        publish();
    }

    private Item build(StatusBarNotification sbn, int type) {
        Notification n = sbn.getNotification();
        Bundle ex = n.extras != null ? n.extras : new Bundle();
        Item it = new Item();
        it.type = type;
        it.key = sbn.getKey();
        it.pkg = sbn.getPackageName();
        it.app = appLabel(it.pkg);
        CharSequence title = ex.getCharSequence(Notification.EXTRA_TITLE);
        if (title == null) title = ex.getCharSequence(Notification.EXTRA_TITLE_BIG);
        CharSequence text = ex.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (text == null) text = ex.getCharSequence(Notification.EXTRA_TEXT);
        if (text == null) {
            CharSequence[] lines = ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
            if (lines != null && lines.length > 0) text = lines[lines.length - 1];
        }
        if (text == null) text = ex.getCharSequence(Notification.EXTRA_SUB_TEXT);
        it.title = title != null ? title.toString() : null;
        it.text = text != null ? text.toString() : null;
        it.icon = appIcon(it.pkg);
        try {
            Icon li = n.getLargeIcon();
            if (li != null) {
                Drawable d = li.loadDrawable(this);
                if (d instanceof BitmapDrawable) it.art = ((BitmapDrawable) d).getBitmap();
                else it.large = d;
            }
        } catch (Exception ignored) {
        }
        if (it.art == null && it.large == null) {
            Object b = ex.get(Notification.EXTRA_LARGE_ICON);
            if (b instanceof Bitmap) it.art = (Bitmap) b;
        }
        if (n.color != 0 && n.color != Notification.COLOR_DEFAULT) it.color = Colors.forDark(n.color);
        it.contentIntent = n.contentIntent != null ? n.contentIntent : n.fullScreenIntent;
        it.actions = n.actions;
        it.postedAt = sbn.getPostTime();
        return it;
    }

    String appLabel(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    Drawable appIcon(String pkg) {
        try {
            return getPackageManager().getApplicationIcon(pkg);
        } catch (Exception e) {
            return null;
        }
    }

    // ---------------------------------------------------------------- media

    private void mediaNotification(StatusBarNotification sbn, Bundle ex) {
        Object t = ex.getParcelable(Notification.EXTRA_MEDIA_SESSION);
        if (!(t instanceof android.media.session.MediaSession.Token)) return;
        mediaNotifs.put(sbn.getPackageName(), build(sbn, Item.MEDIA));
        MediaController old = notifSessions.get(sbn.getKey());
        if (old != null && old.getSessionToken().equals(t)) {
            schedule();
            return;
        }
        try {
            notifSessions.put(sbn.getKey(), new MediaController(this, (android.media.session.MediaSession.Token) t));
        } catch (Exception e) {
            return;
        }
        try {
            activeSessions = msm.getActiveSessions(new ComponentName(this, NotifService.class));
        } catch (Exception ignored) {
        }
        setControllers(activeSessions);
    }

    private void setControllers(List<MediaController> list) {
        for (Map.Entry<MediaController, MediaController.Callback> e : callbacks.entrySet()) {
            e.getKey().unregisterCallback(e.getValue());
        }
        callbacks.clear();
        activeSessions = list != null ? list : new ArrayList<MediaController>();
        controllers = new ArrayList<>(activeSessions);
        for (MediaController n : notifSessions.values()) {
            boolean dup = false;
            for (MediaController c : controllers) if (c.getSessionToken().equals(n.getSessionToken())) dup = true;
            if (!dup) controllers.add(n);
        }
        for (MediaController c : controllers) {
            MediaController.Callback cb = new MediaController.Callback() {
                @Override public void onPlaybackStateChanged(PlaybackState state) { schedule(); }
                @Override public void onMetadataChanged(MediaMetadata metadata) { schedule(); }
                @Override public void onSessionDestroyed() { schedule(); }
            };
            c.registerCallback(cb, handler);
            callbacks.put(c, cb);
        }
        publish();
    }

    private void schedule() {
        handler.removeCallbacks(republish);
        handler.postDelayed(republish, 120);
    }

    private Item mediaItem() {
        if (!Prefs.music(this)) return null;
        MediaController best = null;
        boolean bestPlaying = false;
        long now = SystemClock.elapsedRealtime();
        for (MediaController c : controllers) {
            PlaybackState st = c.getPlaybackState();
            MediaMetadata md = c.getMetadata();
            if (st == null || (md == null && !mediaNotifs.containsKey(c.getPackageName()))) continue;
            int s = st.getState();
            boolean playing = s == PlaybackState.STATE_PLAYING || s == PlaybackState.STATE_BUFFERING;
            String id = c.getPackageName();
            if (playing) {
                pausedSince.remove(id);
            } else if (s == PlaybackState.STATE_PAUSED) {
                Long since = pausedSince.get(id);
                if (since == null) pausedSince.put(id, since = now);
                if (now - since > PAUSED_KEEP_MS) continue;
            } else {
                continue;
            }
            if (!Prefs.appAllowed(this, id)) continue;
            if (best == null || (playing && !bestPlaying)) {
                best = c;
                bestPlaying = playing;
            }
        }
        if (best == null) return null;
        if (!bestPlaying) {
            // hide a paused player after a while
            Long since = pausedSince.get(best.getPackageName());
            long left = PAUSED_KEEP_MS - (now - (since != null ? since : now));
            handler.removeCallbacks(republish);
            handler.postDelayed(republish, Math.max(1000, left + 500));
        }
        MediaMetadata md = best.getMetadata();
        Item fb = mediaNotifs.get(best.getPackageName());
        Item it = new Item();
        it.type = Item.MEDIA;
        it.key = "media:" + best.getPackageName();
        it.pkg = best.getPackageName();
        it.app = appLabel(it.pkg);
        it.icon = appIcon(it.pkg);
        Bitmap art = null;
        if (md != null) {
            it.title = str(md, MediaMetadata.METADATA_KEY_TITLE, MediaMetadata.METADATA_KEY_DISPLAY_TITLE);
            it.text = str(md, MediaMetadata.METADATA_KEY_ARTIST, MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
            if (it.text == null) it.text = str(md, MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, MediaMetadata.METADATA_KEY_ALBUM);
            art = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (art == null) art = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
            if (art == null) art = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
            it.max = (int) Math.max(0, md.getLong(MediaMetadata.METADATA_KEY_DURATION));
        }
        if (fb != null) {
            if (it.title == null) it.title = fb.title;
            if (it.text == null) it.text = fb.text;
            if (art == null) art = fb.art;
        }
        it.art = art;
        it.color = art != null ? Colors.accentOf(art) : 0xFFFF5A6E;
        it.controller = best;
        it.playing = bestPlaying;
        it.contentIntent = best.getSessionActivity();
        if (it.contentIntent == null && fb != null) it.contentIntent = fb.contentIntent;
        return it;
    }

    private static String str(MediaMetadata md, String a, String b) {
        CharSequence s = md.getText(a);
        if (s == null || s.length() == 0) s = md.getText(b);
        return s != null && s.length() > 0 ? s.toString() : null;
    }

    private void publish() {
        List<Item> out = new ArrayList<>();
        Item m = mediaItem();
        if (m != null) out.add(m);
        List<Item> rest = new ArrayList<>(liveNotifs.values());
        // calls first, newest of the rest after
        for (Item it : rest) if (it.type == Item.CALL) out.add(0, it);
        for (int i = rest.size() - 1; i >= 0; i--) if (rest.get(i).type != Item.CALL) out.add(rest.get(i));
        Hub.publishLive(out);
    }
}
