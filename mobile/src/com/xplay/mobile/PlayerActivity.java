package com.xplay.mobile;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.PlaybackParams;
import android.media.TimedText;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fullscreen player for movies, episodes (Repo.queue) and live channels (Repo.livePlaylist): ±10s,
 * lock, audio and subtitle tracks, speed, screen format, episode / channel list, gestures and resume.
 */
public class PlayerActivity extends Activity implements SurfaceHolder.Callback {

    private static final String EXTRA_LIVE = "live";
    private static final String EXTRA_INDEX = "index";
    private static final long HIDE_MS = 4500;
    private static final float[] SPEEDS = {0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f};
    private static final Locale BR = new Locale("pt", "BR");

    public static void playVod(Activity a, List<Repo.PlayItem> queue, int index) {
        Repo.queue = queue;
        Repo.queueIndex = index;
        Intent i = new Intent(a, PlayerActivity.class);
        i.putExtra(EXTRA_LIVE, false);
        a.startActivity(i);
    }

    public static void playLive(Activity a, List<Channel> channels, int index) {
        Repo.livePlaylist = new ArrayList<Channel>(channels);
        Intent i = new Intent(a, PlayerActivity.class);
        i.putExtra(EXTRA_LIVE, true);
        i.putExtra(EXTRA_INDEX, index);
        a.startActivity(i);
    }

    private final Handler handler = new Handler();
    private final Runnable hide = new Runnable() {
        public void run() {
            if (!dragging && player != null && player.isPlaying()) showControls(false);
        }
    };
    private final Runnable tick = new Runnable() {
        public void run() {
            updateProgress();
            handler.postDelayed(this, 500);
        }
    };
    private final Runnable autosave = new Runnable() {
        public void run() {
            saveProgress();
            handler.postDelayed(this, 10000);
        }
    };

    private boolean live;
    private int liveIndex;
    private Channel channel;
    private Repo.PlayItem item;

    private MediaPlayer player;
    private SurfaceView surface;
    private SurfaceHolder holder;
    private boolean surfaceReady, prepared, dragging, locked, controlsShown, ended;
    private int pendingSeek = -1;
    private int videoW, videoH;
    private int aspect;
    private float speed = 1f;
    private int retries;
    private int audioIndex = -1, subtitleIndex = -1;
    private String audioLabel, subtitleLabel;

    private FrameLayout root, videoBox, controls, panel;
    private ProgressBar loading;
    private TextView subtitleView, feedback, seriesName, titleView, position, remaining, errorText, upNext;
    private LinearLayout errorBox, pills, topRight;
    private SeekBar seek;
    private ImageView playPause, lockButton, unlockButton, listButton;
    private View back10, fwd10, topShade, bottomShade, centerRow;
    private TextView audioPill, subtitlePill, speedPill, aspectPill, nextPill;
    private GestureDetector gestures;
    private AudioManager audio;

    // swipe gestures (brightness on the left, volume on the right)
    private float swipeStartY, swipeStartValue;
    private int swipeMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        aspect = Repo.aspectMode();
        if (!readIntent(getIntent())) {
            finish();
            return;
        }
        setContentView(build());
        immersive();
        bindItem();
        showControls(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        saveProgress();
        if (!readIntent(intent)) return;
        releasePlayer();
        bindItem();
        if (surfaceReady) open(startPosition());
        showControls(true);
    }

    private boolean readIntent(Intent i) {
        live = i.getBooleanExtra(EXTRA_LIVE, false);
        if (live) {
            if (Repo.livePlaylist == null || Repo.livePlaylist.isEmpty()) return false;
            liveIndex = Math.max(0, Math.min(i.getIntExtra(EXTRA_INDEX, 0), Repo.livePlaylist.size() - 1));
            channel = Repo.livePlaylist.get(liveIndex);
            item = null;
        } else {
            if (Repo.queue == null || Repo.queue.isEmpty()) return false;
            Repo.queueIndex = Math.max(0, Math.min(Repo.queueIndex, Repo.queue.size() - 1));
            item = Repo.queue.get(Repo.queueIndex);
            channel = null;
        }
        return true;
    }

    // ---------------------------------------------------------------- layout

    private View build() {
        root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);

        videoBox = new FrameLayout(this);
        surface = new SurfaceView(this);
        holder = surface.getHolder();
        holder.addCallback(this);
        videoBox.addView(surface, Ui.frame(-1, -1, Gravity.CENTER));
        root.addView(videoBox, Ui.frame(-1, -1, Gravity.CENTER));

        subtitleView = Ui.text(this, "", 20, Theme.TEXT, Ui.MEDIUM);
        subtitleView.setGravity(Gravity.CENTER);
        subtitleView.setShadowLayer(6, 0, 2, 0xFF000000);
        subtitleView.setBackground(Ui.round(0x80000000, Ui.dp(this, 6)));
        subtitleView.setPadding(Ui.dp(this, 10), Ui.dp(this, 3), Ui.dp(this, 10), Ui.dp(this, 3));
        subtitleView.setVisibility(View.GONE);
        FrameLayout.LayoutParams sl = Ui.frame(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        sl.setMargins(Ui.dp(this, 60), 0, Ui.dp(this, 60), Ui.dp(this, 36));
        root.addView(subtitleView, sl);

        // Touch layer: taps toggle the controls, double taps seek, vertical swipes set brightness / volume.
        View touch = new View(this);
        gestures = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                if (locked) {
                    flashUnlock();
                    return true;
                }
                showControls(!controlsShown);
                return true;
            }

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                if (locked || live) return false;
                boolean right = e.getX() > root.getWidth() / 2f;
                seekBy(right ? 10000 : -10000);
                return true;
            }
        });
        touch.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent e) {
                gestures.onTouchEvent(e);
                if (!locked) swipe(e);
                return true;
            }
        });
        root.addView(touch, Ui.frame(-1, -1, Gravity.CENTER));

        loading = new ProgressBar(this);
        loading.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        root.addView(loading, Ui.frame(Ui.dp(this, 54), Ui.dp(this, 54), Gravity.CENTER));

        feedback = Ui.text(this, "", 18, Theme.TEXT, Ui.MEDIUM);
        feedback.setBackground(Ui.round(0xB3000000, Ui.dp(this, 14)));
        feedback.setPadding(Ui.dp(this, 18), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
        feedback.setVisibility(View.GONE);
        FrameLayout.LayoutParams fl = Ui.frame(-2, -2, Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        fl.topMargin = Ui.dp(this, 40);
        root.addView(feedback, fl);

        buildControls();
        buildError();

        unlockButton = round(R.drawable.ic_lock, 46, 11);
        unlockButton.setBackground(Ui.ripple(Ui.round(0x99000000, Ui.dp(this, 23)), Ui.dp(this, 23)));
        unlockButton.setVisibility(View.GONE);
        unlockButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setLocked(false); }
        });
        FrameLayout.LayoutParams ul = Ui.frame(Ui.dp(this, 46), Ui.dp(this, 46), Gravity.TOP | Gravity.START);
        ul.setMargins(Ui.dp(this, 92), Ui.dp(this, 22), 0, 0);
        root.addView(unlockButton, ul);
        return root;
    }

    private void buildControls() {
        controls = new FrameLayout(this);
        int side = Ui.dp(this, 32);

        topShade = new View(this);
        topShade.setBackground(Ui.vertical(0xB3000000, 0x00000000));
        controls.addView(topShade, Ui.frame(-1, Ui.dp(this, 120), Gravity.TOP));
        bottomShade = new View(this);
        bottomShade.setBackground(Ui.vertical(0x00000000, 0x99000000, 0xE6000000));
        controls.addView(bottomShade, Ui.frame(-1, Ui.dp(this, 210), Gravity.BOTTOM));
        View dim = new View(this);
        dim.setBackgroundColor(0x33000000);
        controls.addView(dim, 0, Ui.frame(-1, -1, Gravity.CENTER));

        // Top: back, lock … more
        LinearLayout top = Ui.row(this);
        top.setPadding(side, Ui.dp(this, 16), side, 0);
        ImageView backButton = round(R.drawable.ic_back, 48, 11);
        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        top.addView(backButton, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        lockButton = round(R.drawable.ic_lock_open, 48, 12);
        lockButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setLocked(true); }
        });
        top.addView(lockButton, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48), Ui.dp(this, 44), 0, 0, 0));
        top.addView(new View(this), Ui.weight(1));
        topRight = Ui.row(this);
        top.addView(topRight);
        ImageView moreButton = round(R.drawable.ic_more, 48, 11);
        moreButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showMore(); }
        });
        top.addView(moreButton, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        controls.addView(top, Ui.frame(-1, -2, Gravity.TOP));

        // Center: -10, play/pause, +10 (live: previous / next channel)
        LinearLayout center = Ui.row(this);
        center.setGravity(Gravity.CENTER);
        back10 = skipButton(R.drawable.ic_replay, "10");
        back10.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (live) zap(-1);
                else seekBy(-10000);
            }
        });
        center.addView(back10, Ui.lp(Ui.dp(this, 64), Ui.dp(this, 64)));
        playPause = Ui.icon(this, R.drawable.ic_pause, Theme.TEXT);
        int pp = Ui.dp(this, 20);
        playPause.setPadding(pp, pp, pp, pp);
        playPause.setClickable(true);
        playPause.setFocusable(true);
        playPause.setBackground(Ui.ripple(Ui.round(0x59000000, Ui.dp(this, 44)), Ui.dp(this, 44)));
        playPause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { togglePlay(); }
        });
        center.addView(playPause, Ui.lp(Ui.dp(this, 88), Ui.dp(this, 88), Ui.dp(this, 64), 0, Ui.dp(this, 64), 0));
        fwd10 = skipButton(R.drawable.ic_forward, "10");
        fwd10.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (live) zap(1);
                else seekBy(10000);
            }
        });
        center.addView(fwd10, Ui.lp(Ui.dp(this, 64), Ui.dp(this, 64)));
        centerRow = center;
        controls.addView(center, Ui.frame(-2, -2, Gravity.CENTER));

        // Bottom: names, seek bar, pills
        LinearLayout bottom = Ui.column(this);
        bottom.setPadding(side, 0, side, Ui.dp(this, 18));
        LinearLayout names = Ui.row(this);
        LinearLayout nameCol = Ui.column(this);
        seriesName = Ui.oneLine(Ui.text(this, "", 13, 0xFFBDBDC2, Ui.MEDIUM));
        seriesName.setLetterSpacing(0.04f);
        nameCol.addView(seriesName);
        titleView = Ui.oneLine(Ui.text(this, "", 22, Theme.TEXT, Ui.MEDIUM));
        nameCol.addView(titleView, Ui.lp(-1, -2, 0, Ui.dp(this, 2), 0, 0));
        names.addView(nameCol, Ui.weight(1));
        listButton = round(R.drawable.ic_episodes, 48, 11);
        listButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showList(); }
        });
        names.addView(listButton, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        bottom.addView(names, Ui.lp(-1, -2));

        LinearLayout bar = Ui.row(this);
        position = Ui.text(this, "00:00", 13, 0xFFD0D0D4);
        bar.addView(position, Ui.lp(Ui.dp(this, 58), -2));
        seek = new SeekBar(this);
        styleSeek(seek);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean fromUser) {
                if (fromUser) {
                    position.setText(Ui.time(v));
                    remaining.setText("-" + Ui.time(Math.max(0, s.getMax() - v)));
                }
            }

            public void onStartTrackingTouch(SeekBar s) {
                dragging = true;
                handler.removeCallbacks(hide);
            }

            public void onStopTrackingTouch(SeekBar s) {
                dragging = false;
                seekTo(s.getProgress());
                scheduleHide();
            }
        });
        bar.addView(seek, Ui.weight(1));
        remaining = Ui.text(this, "", 13, 0xFFD0D0D4);
        remaining.setGravity(Gravity.END);
        bar.addView(remaining, Ui.lp(Ui.dp(this, 64), -2));
        bottom.addView(bar, Ui.lp(-1, -2, 0, Ui.dp(this, 10), 0, Ui.dp(this, 10)));

        pills = Ui.row(this);
        audioPill = pill(R.drawable.ic_audio, "Áudio");
        audioPill.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickTrack(true); }
        });
        subtitlePill = pill(R.drawable.ic_subtitles, "Sem legenda");
        subtitlePill.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickTrack(false); }
        });
        speedPill = pill(R.drawable.ic_speed, "1x");
        speedPill.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickSpeed(); }
        });
        aspectPill = pill(R.drawable.ic_aspect, SettingsPage.ASPECTS[aspect]);
        aspectPill.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { pickAspect(); }
        });
        pills.addView(new View(this), Ui.weight(1));
        nextPill = pill(R.drawable.ic_skip_next, "Próximo episódio");
        nextPill.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { playNext(); }
        });
        bottom.addView(pills, Ui.lp(-1, -2));
        controls.addView(bottom, Ui.frame(-1, -2, Gravity.BOTTOM));

        upNext = Ui.text(this, "", 15, 0xFF111111, Ui.MEDIUM);
        upNext.setBackground(Ui.ripple(Ui.round(0xFFFFFFFF, Ui.dp(this, 26)), Ui.dp(this, 26)));
        upNext.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 12));
        upNext.setClickable(true);
        upNext.setVisibility(View.GONE);
        upNext.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { playNext(); }
        });

        root.addView(controls, Ui.frame(-1, -1, Gravity.CENTER));
        FrameLayout.LayoutParams ul = Ui.frame(-2, -2, Gravity.BOTTOM | Gravity.END);
        ul.setMargins(0, 0, Ui.dp(this, 32), Ui.dp(this, 130));
        root.addView(upNext, ul);
    }

    private void buildError() {
        errorBox = Ui.column(this);
        errorBox.setGravity(Gravity.CENTER);
        errorBox.setVisibility(View.GONE);
        errorText = Ui.text(this, "", 17, Theme.TEXT, Ui.MEDIUM);
        errorText.setGravity(Gravity.CENTER);
        errorBox.addView(errorText);
        TextView retry = Sheet.button(this, "Tentar de novo", true);
        retry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                retries = 0;
                errorBox.setVisibility(View.GONE);
                open(lastPosition());
            }
        });
        errorBox.addView(retry, Ui.lp(-2, -2, 0, Ui.dp(this, 18), 0, 0));
        root.addView(errorBox, Ui.frame(-2, -2, Gravity.CENTER));
    }

    private ImageView round(int icon, int sizeDp, int padDp) {
        ImageView v = Ui.icon(this, icon, Theme.TEXT);
        int p = Ui.dp(this, padDp);
        v.setPadding(p, p, p, p);
        v.setClickable(true);
        v.setFocusable(true);
        v.setBackground(Ui.pressable(this, 0, Ui.dp(this, sizeDp / 2f)));
        return v;
    }

    private View skipButton(int icon, String label) {
        FrameLayout f = new FrameLayout(this);
        f.setClickable(true);
        f.setFocusable(true);
        f.setBackground(Ui.pressable(this, 0, Ui.dp(this, 32)));
        ImageView i = Ui.icon(this, icon, Theme.TEXT);
        f.addView(i, Ui.frame(Ui.dp(this, 52), Ui.dp(this, 52), Gravity.CENTER));
        TextView t = Ui.text(this, label, 12, Theme.TEXT, Ui.BOLD);
        t.setTag("label");
        FrameLayout.LayoutParams tl = Ui.frame(-2, -2, Gravity.CENTER);
        tl.topMargin = Ui.dp(this, 3);
        f.addView(t, tl);
        return f;
    }

    private TextView pill(int icon, String label) {
        TextView t = Ui.oneLine(Ui.text(this, label, 15, Theme.TEXT));
        Drawable d = getDrawable(icon).mutate();
        d.setTint(Theme.TEXT);
        d.setBounds(0, 0, Ui.dp(this, 20), Ui.dp(this, 20));
        t.setCompoundDrawables(d, null, null, null);
        t.setCompoundDrawablePadding(Ui.dp(this, 10));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), Ui.dp(this, 18), Ui.dp(this, 10));
        t.setClickable(true);
        t.setFocusable(true);
        t.setMaxWidth(Ui.dp(this, 220));
        t.setBackground(Ui.ripple(Ui.stroke(0x33000000, Ui.dp(this, 26), Ui.dp(this, 1), 0x59FFFFFF), Ui.dp(this, 26)));
        return t;
    }

    private void layoutPills() {
        pills.removeAllViews();
        List<TextView> list = new ArrayList<TextView>();
        list.add(audioPill);
        if (!live) list.add(subtitlePill);
        if (!live && Build.VERSION.SDK_INT >= 23) list.add(speedPill);
        list.add(aspectPill);
        for (TextView t : list) {
            LinearLayout.LayoutParams l = Ui.lp(-2, -2);
            l.rightMargin = Ui.dp(this, 12);
            pills.addView(t, l);
        }
        pills.addView(new View(this), Ui.weight(1));
        if (!live && hasNext()) pills.addView(nextPill);
    }

    private void styleSeek(SeekBar s) {
        int h = Ui.dp(this, 4);
        GradientDrawable bg = Ui.round(0x4DFFFFFF, h);
        GradientDrawable sec = Ui.round(0x80FFFFFF, h);
        GradientDrawable prog = Ui.round(0xFFFFFFFF, h);
        LayerDrawable layers = new LayerDrawable(new Drawable[] {bg,
                new ClipDrawable(sec, Gravity.START, ClipDrawable.HORIZONTAL),
                new ClipDrawable(prog, Gravity.START, ClipDrawable.HORIZONTAL)});
        layers.setId(0, android.R.id.background);
        layers.setId(1, android.R.id.secondaryProgress);
        layers.setId(2, android.R.id.progress);
        s.setProgressDrawable(layers);
        GradientDrawable thumb = Ui.round(0xFFFFFFFF, Ui.dp(this, 2));
        thumb.setSize(Ui.dp(this, 4), Ui.dp(this, 20));
        s.setThumb(thumb);
        s.setSplitTrack(false);
        s.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        s.setMinimumHeight(h);
    }

    // ---------------------------------------------------------------- what is playing

    private void bindItem() {
        ended = false;
        audioIndex = -1;
        subtitleIndex = -1;
        audioLabel = null;
        subtitleLabel = null;
        subtitleView.setVisibility(View.GONE);
        upNext.setVisibility(View.GONE);
        errorBox.setVisibility(View.GONE);
        if (live) {
            seriesName.setText(channel.group.toUpperCase(BR));
            titleView.setText(channel.name);
            ((TextView) back10.findViewWithTag("label")).setText("");
            ((TextView) fwd10.findViewWithTag("label")).setText("");
            ((ImageView) ((FrameLayout) back10).getChildAt(0)).setImageResource(R.drawable.ic_skip_next);
            ((FrameLayout) back10).getChildAt(0).setRotation(180);
            ((ImageView) ((FrameLayout) fwd10).getChildAt(0)).setImageResource(R.drawable.ic_skip_next);
            seek.setVisibility(View.INVISIBLE);
            position.setText("");
            remaining.setText("AO VIVO");
            remaining.setTextColor(Theme.PINK);
            listButton.setImageResource(R.drawable.ic_list);
            loadLiveGuide();
        } else {
            ((TextView) back10.findViewWithTag("label")).setText("10");
            ((TextView) fwd10.findViewWithTag("label")).setText("10");
            ((ImageView) ((FrameLayout) back10).getChildAt(0)).setImageResource(R.drawable.ic_replay);
            ((FrameLayout) back10).getChildAt(0).setRotation(0);
            ((ImageView) ((FrameLayout) fwd10).getChildAt(0)).setImageResource(R.drawable.ic_forward);
            boolean episode = item.episodeId != null;
            seriesName.setText(episode ? item.title.toUpperCase(BR) : (item.year == null ? "" : item.year));
            seriesName.setVisibility(seriesName.length() > 0 ? View.VISIBLE : View.GONE);
            titleView.setText(episode ? item.subtitle : item.title);
            listButton.setVisibility(episode && Repo.queue.size() > 1 ? View.VISIBLE : View.GONE);
            listButton.setImageResource(R.drawable.ic_episodes);
            seek.setVisibility(View.VISIBLE);
            remaining.setTextColor(0xFFD0D0D4);
            seek.setProgress(0);
            seek.setMax(item.durationSecs > 0 ? item.durationSecs * 1000 : 1);
            position.setText("00:00");
            remaining.setText(item.durationSecs > 0 ? "-" + Ui.time(item.durationSecs * 1000L) : "");
        }
        audioPill.setText(live ? "Áudio" : "Português");
        subtitlePill.setText("Sem legenda");
        speedPill.setText(speedLabel(speed));
        aspectPill.setText(SettingsPage.ASPECTS[aspect]);
        layoutPills();
    }

    private void loadLiveGuide() {
        final Channel c = channel;
        Epg.programs(c, new Epg.ListCallback() {
            public void done(List<Xtream.Program> p) {
                if (c != channel) return;
                Xtream.Program now = Epg.current(p);
                if (now == null) return;
                java.text.SimpleDateFormat f = new java.text.SimpleDateFormat("HH:mm", BR);
                titleView.setText(c.name + "  ·  " + now.title);
                Xtream.Program next = Epg.next(p);
                seriesName.setText(c.group.toUpperCase(BR) + "  ·  até " + f.format(new java.util.Date(now.end))
                        + (next != null ? "  ·  depois: " + next.title : ""));
            }
        });
    }

    private int startPosition() {
        return live ? 0 : Repo.resumePosition(item);
    }

    private int lastPosition() {
        if (live) return 0;
        if (player != null && prepared) {
            try {
                return player.getCurrentPosition();
            } catch (IllegalStateException ignored) {
                // Fall through.
            }
        }
        return pendingSeek > 0 ? pendingSeek : seek.getProgress();
    }

    // ---------------------------------------------------------------- playback

    public void surfaceCreated(SurfaceHolder h) {
        surfaceReady = true;
        if (player == null) open(pendingSeek >= 0 ? pendingSeek : startPosition());
        else player.setDisplay(h);
    }

    public void surfaceChanged(SurfaceHolder h, int format, int w, int ht) {}

    public void surfaceDestroyed(SurfaceHolder h) {
        surfaceReady = false;
        // Remember where we were; the player is rebuilt when the screen comes back.
        pendingSeek = lastPosition();
        saveProgress();
        releasePlayer();
    }

    private void open(final int startMs) {
        releasePlayer();
        prepared = false;
        pendingSeek = startMs;
        loading.setVisibility(View.VISIBLE);
        errorBox.setVisibility(View.GONE);
        final String url = live ? channel.url : item.url;
        final MediaPlayer mp = new MediaPlayer();
        player = mp;
        try {
            Map<String, String> headers = new HashMap<String, String>();
            headers.put("User-Agent", Http.AGENT);
            mp.setDataSource(this, Uri.parse(url), headers);
            mp.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mp.setScreenOnWhilePlaying(true);
            if (surfaceReady) mp.setDisplay(holder);
        } catch (Exception e) {
            showError("Não foi possível abrir o vídeo");
            return;
        }
        mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer m) {
                if (m != player) return;
                prepared = true;
                retries = 0;
                if (!live) {
                    int d = m.getDuration();
                    if (d > 0) seek.setMax(d);
                    if (startMs > 0 && (d <= 0 || startMs < d - 5000)) m.seekTo(startMs);
                }
                pendingSeek = -1;
                applySpeed();
                readTracks();
                m.start();
                loading.setVisibility(View.GONE);
                updatePlayPause();
                scheduleHide();
            }
        });
        mp.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
            public void onVideoSizeChanged(MediaPlayer m, int w, int h) {
                videoW = w;
                videoH = h;
                applyAspect();
            }
        });
        mp.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            public boolean onInfo(MediaPlayer m, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) loading.setVisibility(View.VISIBLE);
                else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END
                        || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) loading.setVisibility(View.GONE);
                return false;
            }
        });
        mp.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            public void onBufferingUpdate(MediaPlayer m, int percent) {
                if (!live) seek.setSecondaryProgress(seek.getMax() * percent / 100);
            }
        });
        mp.setOnTimedTextListener(new MediaPlayer.OnTimedTextListener() {
            public void onTimedText(MediaPlayer m, TimedText text) {
                String t = text == null ? null : text.getText();
                subtitleView.setText(t == null ? "" : t.trim());
                subtitleView.setVisibility(t == null || t.trim().isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer m, int what, int extra) {
                if (m != player) return true;
                final int at = lastPosition();
                if (retries < 3) {
                    retries++;
                    loading.setVisibility(View.VISIBLE);
                    handler.postDelayed(new Runnable() {
                        public void run() { open(at); }
                    }, 1500L * retries);
                } else {
                    showError(live ? "Este canal não está abrindo agora" : "Não foi possível reproduzir este vídeo");
                }
                return true;
            }
        });
        mp.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer m) {
                if (m != player) return;
                if (live) {
                    // Live streams that "end" usually just dropped: reconnect.
                    open(0);
                    return;
                }
                ended = true;
                int d = seek.getMax();
                if (item != null) Repo.saveProgress(item, d, d);
                if (hasNext()) playNext();
                else finish();
            }
        });
        try {
            mp.prepareAsync();
        } catch (IllegalStateException e) {
            showError("Não foi possível abrir o vídeo");
        }
    }

    private void showError(String msg) {
        loading.setVisibility(View.GONE);
        errorText.setText(msg);
        errorBox.setVisibility(View.VISIBLE);
        showControls(true);
    }

    private void releasePlayer() {
        MediaPlayer p = player;
        player = null;
        prepared = false;
        if (p != null) {
            try {
                p.reset();
            } catch (Exception ignored) {
                // Already broken; release anyway.
            }
            p.release();
        }
    }

    private void togglePlay() {
        if (player == null || !prepared) return;
        if (player.isPlaying()) {
            player.pause();
            saveProgress();
            handler.removeCallbacks(hide);
        } else {
            player.start();
            scheduleHide();
        }
        updatePlayPause();
    }

    private void updatePlayPause() {
        boolean playing = player != null && prepared && player.isPlaying();
        playPause.setImageResource(playing ? R.drawable.ic_pause : R.drawable.ic_play);
    }

    private void seekBy(int ms) {
        if (live || player == null || !prepared) return;
        int target = Math.max(0, Math.min(player.getCurrentPosition() + ms, Math.max(0, player.getDuration() - 1000)));
        seekTo(target);
        flash((ms > 0 ? "+" : "−") + Math.abs(ms / 1000) + "s");
    }

    private void seekTo(int ms) {
        if (player == null || !prepared) {
            pendingSeek = ms;
            return;
        }
        player.seekTo(ms);
        seek.setProgress(ms);
        updateProgress();
    }

    private void updateProgress() {
        if (live || player == null || !prepared || dragging) return;
        int pos, dur;
        try {
            pos = player.getCurrentPosition();
            dur = player.getDuration();
        } catch (IllegalStateException e) {
            return;
        }
        if (dur > 0) {
            if (seek.getMax() != dur) seek.setMax(dur);
            seek.setProgress(pos);
            remaining.setText("-" + Ui.time(Math.max(0, dur - pos)));
        }
        position.setText(Ui.time(pos));
        // "Próximo episódio" card in the last 30 seconds.
        boolean near = dur > 60000 && dur - pos < 30000 && hasNext();
        if (near && upNext.getVisibility() != View.VISIBLE) {
            Repo.PlayItem n = Repo.queue.get(Repo.queueIndex + 1);
            upNext.setText("▶  Próximo: T" + n.season + ":E" + n.episode);
            upNext.setVisibility(View.VISIBLE);
        } else if (!near && upNext.getVisibility() == View.VISIBLE) {
            upNext.setVisibility(View.GONE);
        }
        updatePlayPause();
    }

    private void saveProgress() {
        if (live || item == null || ended || player == null || !prepared) return;
        try {
            Repo.saveProgress(item, player.getCurrentPosition(), player.getDuration());
        } catch (IllegalStateException ignored) {
            // Player not ready.
        }
    }

    private boolean hasNext() {
        return !live && item != null && item.episodeId != null && Repo.queueIndex + 1 < Repo.queue.size();
    }

    private void playNext() {
        if (!hasNext()) return;
        saveProgress();
        playIndex(Repo.queueIndex + 1);
    }

    private void playIndex(int i) {
        if (!live && player != null && prepared && !ended) saveProgress();
        Repo.queueIndex = i;
        item = Repo.queue.get(i);
        releasePlayer();
        bindItem();
        open(Repo.resumePosition(item));
        showControls(true);
    }

    private void zap(int delta) {
        int n = Repo.livePlaylist.size();
        if (n < 2) return;
        liveIndex = (liveIndex + delta + n) % n;
        channel = Repo.livePlaylist.get(liveIndex);
        retries = 0;
        releasePlayer();
        bindItem();
        open(0);
        showControls(true);
    }

    private void playChannel(int i) {
        liveIndex = i;
        channel = Repo.livePlaylist.get(i);
        retries = 0;
        releasePlayer();
        bindItem();
        open(0);
        showControls(true);
    }

    // ---------------------------------------------------------------- tracks, speed, format

    private void readTracks() {
        MediaPlayer.TrackInfo[] tracks = tracks();
        int audioCount = 0, subCount = 0;
        for (int i = 0; i < tracks.length; i++) {
            int t = tracks[i].getTrackType();
            if (t == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) audioCount++;
            if (isSubtitle(t)) subCount++;
        }
        int selected = -1;
        if (Build.VERSION.SDK_INT >= 21) {
            try {
                selected = player.getSelectedTrack(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO);
            } catch (Exception ignored) {
                // Some devices throw here.
            }
        }
        if (selected >= 0 && selected < tracks.length) {
            audioIndex = selected;
            audioLabel = trackName(tracks[selected], 1);
        } else if (audioCount > 0) {
            for (int i = 0; i < tracks.length; i++) {
                if (tracks[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                    audioLabel = trackName(tracks[i], 1);
                    break;
                }
            }
        }
        audioPill.setText(audioLabel != null ? audioLabel : "Áudio");
        audioPill.setAlpha(audioCount > 1 ? 1f : 0.85f);
        subtitlePill.setAlpha(subCount > 0 ? 1f : 0.6f);
        subtitlePill.setText(subtitleLabel != null ? subtitleLabel : "Sem legenda");
    }

    private MediaPlayer.TrackInfo[] tracks() {
        if (player == null || !prepared) return new MediaPlayer.TrackInfo[0];
        try {
            MediaPlayer.TrackInfo[] t = player.getTrackInfo();
            return t == null ? new MediaPlayer.TrackInfo[0] : t;
        } catch (Exception e) {
            return new MediaPlayer.TrackInfo[0];
        }
    }

    private static boolean isSubtitle(int type) {
        return type == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT || type == 4 /* MEDIA_TRACK_TYPE_SUBTITLE */;
    }

    private static String trackName(MediaPlayer.TrackInfo t, int n) {
        String lang = t.getLanguage();
        if (lang == null || lang.isEmpty() || lang.equals("und")) return "Faixa " + n;
        String name = new Locale(lang).getDisplayLanguage(BR);
        if (name == null || name.isEmpty() || name.equalsIgnoreCase(lang)) return lang.toUpperCase(BR);
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private void pickTrack(final boolean audioTracks) {
        final MediaPlayer.TrackInfo[] tracks = tracks();
        final List<Integer> idx = new ArrayList<Integer>();
        List<String> names = new ArrayList<String>();
        if (!audioTracks) {
            idx.add(-1);
            names.add("Sem legenda");
        }
        int n = 0;
        for (int i = 0; i < tracks.length; i++) {
            int t = tracks[i].getTrackType();
            if (audioTracks ? t == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO : isSubtitle(t)) {
                n++;
                idx.add(i);
                names.add(trackName(tracks[i], n));
            }
        }
        if (audioTracks && idx.isEmpty()) {
            Ui.toast(this, "Este vídeo tem só uma faixa de áudio");
            return;
        }
        if (!audioTracks && idx.size() == 1) {
            Ui.toast(this, "Este vídeo não tem legendas");
            return;
        }
        int current = idx.indexOf(audioTracks ? audioIndex : subtitleIndex);
        if (audioTracks && current < 0) current = 0;
        Sheet.options(this, audioTracks ? "Áudio" : "Legendas", names, current, new Sheet.Pick() {
            public void picked(int which) {
                int track = idx.get(which);
                try {
                    if (audioTracks) {
                        player.selectTrack(track);
                        audioIndex = track;
                        audioLabel = trackName(tracks[track], which + 1);
                        audioPill.setText(audioLabel);
                    } else if (track < 0) {
                        if (subtitleIndex >= 0) player.deselectTrack(subtitleIndex);
                        subtitleIndex = -1;
                        subtitleLabel = null;
                        subtitleView.setVisibility(View.GONE);
                        subtitlePill.setText("Sem legenda");
                    } else {
                        player.selectTrack(track);
                        subtitleIndex = track;
                        subtitleLabel = trackName(tracks[track], which);
                        subtitlePill.setText(subtitleLabel);
                    }
                } catch (Exception e) {
                    Ui.toast(PlayerActivity.this, "Não foi possível trocar a faixa");
                }
            }
        });
    }

    private static String speedLabel(float s) {
        return s == (int) s ? (int) s + "x" : String.valueOf(s).replace('.', ',') + "x";
    }

    private void pickSpeed() {
        List<String> names = new ArrayList<String>();
        int current = 2;
        for (int i = 0; i < SPEEDS.length; i++) {
            names.add(SPEEDS[i] == 1f ? "Normal (1x)" : speedLabel(SPEEDS[i]));
            if (SPEEDS[i] == speed) current = i;
        }
        Sheet.options(this, "Velocidade", names, current, new Sheet.Pick() {
            public void picked(int which) {
                speed = SPEEDS[which];
                speedPill.setText(speedLabel(speed));
                applySpeed();
            }
        });
    }

    private void applySpeed() {
        if (Build.VERSION.SDK_INT < 23 || player == null || !prepared) return;
        try {
            boolean playing = player.isPlaying();
            PlaybackParams p = player.getPlaybackParams();
            player.setPlaybackParams(p.setSpeed(speed));
            // Setting the speed starts playback on some devices; keep a paused video paused.
            if (!playing && player.isPlaying()) player.pause();
        } catch (Exception ignored) {
            // Not supported by this stream / device.
        }
    }

    private void pickAspect() {
        Sheet.options(this, "Formato da tela", java.util.Arrays.asList(SettingsPage.ASPECTS), aspect, new Sheet.Pick() {
            public void picked(int which) {
                aspect = which;
                aspectPill.setText(SettingsPage.ASPECTS[aspect]);
                applyAspect();
            }
        });
    }

    /** Sizes the surface: fit, crop to fill, stretch, or forced 16:9 / 4:3. */
    private void applyAspect() {
        int bw = videoBox.getWidth(), bh = videoBox.getHeight();
        if (bw == 0 || bh == 0) {
            videoBox.post(new Runnable() {
                public void run() { applyAspect(); }
            });
            return;
        }
        float videoRatio = videoW > 0 && videoH > 0 ? videoW / (float) videoH : 16f / 9f;
        float boxRatio = bw / (float) bh;
        int w = bw, h = bh;
        switch (aspect) {
            case 1: // fill (crop)
                if (videoRatio > boxRatio) w = (int) (bh * videoRatio);
                else h = (int) (bw / videoRatio);
                break;
            case 2: // stretch
                break;
            case 3:
            case 4: {
                float r = aspect == 3 ? 16f / 9f : 4f / 3f;
                if (r > boxRatio) h = (int) (bw / r);
                else w = (int) (bh * r);
                break;
            }
            default: // fit
                if (videoRatio > boxRatio) h = (int) (bw / videoRatio);
                else w = (int) (bh * videoRatio);
        }
        FrameLayout.LayoutParams l = (FrameLayout.LayoutParams) surface.getLayoutParams();
        l.width = w;
        l.height = h;
        l.gravity = Gravity.CENTER;
        surface.setLayoutParams(l);
    }

    // ---------------------------------------------------------------- more, lists

    private void showMore() {
        final List<String> names = new ArrayList<String>();
        names.add("Recarregar vídeo");
        if (live) names.add(Repo.isFav(channel.favKey()) ? "Remover dos favoritos" : "Adicionar aos favoritos");
        else {
            names.add("Velocidade: " + speedLabel(speed));
            if (item.episodeId != null) names.add("Lista de episódios");
        }
        names.add("Formato da tela: " + SettingsPage.ASPECTS[aspect]);
        Sheet.options(this, live ? channel.name : item.title, names, -1, new Sheet.Pick() {
            public void picked(int which) {
                String n = names.get(which);
                if (n.startsWith("Recarregar")) {
                    retries = 0;
                    open(lastPosition());
                } else if (n.startsWith("Velocidade")) {
                    pickSpeed();
                } else if (n.startsWith("Formato")) {
                    pickAspect();
                } else if (n.startsWith("Lista")) {
                    showList();
                } else {
                    boolean on = Repo.toggleFav(channel.favKey());
                    Ui.toast(PlayerActivity.this, on ? "Canal adicionado aos favoritos" : "Canal removido dos favoritos");
                }
            }
        });
    }

    /** Side panel with the season's episodes (or the channel list on live TV). */
    private void showList() {
        if (panel != null) {
            closePanel();
            return;
        }
        showControls(false);
        panel = new FrameLayout(this);
        panel.setBackgroundColor(0x66000000);
        panel.setClickable(true);
        panel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { closePanel(); }
        });
        LinearLayout box = Ui.column(this);
        box.setBackground(Ui.vertical(0xF2121214, 0xF2121214));
        box.setClickable(true);
        box.setPadding(0, Ui.dp(this, 18), 0, 0);
        TextView title = Ui.text(this, live ? "Canais" : "Temporada " + item.season, 19, Theme.TEXT, Ui.BOLD);
        title.setPadding(Ui.dp(this, 20), 0, Ui.dp(this, 20), Ui.dp(this, 10));
        box.addView(title);
        ListView list = new ListView(this);
        list.setDivider(null);
        list.setSelector(new ColorDrawable(0));
        box.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));
        int w = Math.min(Ui.dp(this, 380), (int) (Ui.screenWidth(this) * 0.5f));
        panel.addView(box, Ui.frame(w, -1, Gravity.END));
        root.addView(panel, Ui.frame(-1, -1, Gravity.CENTER));
        box.setTranslationX(w);
        box.animate().translationX(0).setDuration(200).start();

        if (live) {
            final List<Channel> chans = Repo.livePlaylist;
            list.setAdapter(new android.widget.BaseAdapter() {
                public int getCount() { return chans.size(); }

                public Object getItem(int i) { return chans.get(i); }

                public long getItemId(int i) { return i; }

                public View getView(final int i, View c, android.view.ViewGroup p) {
                    Channel ch = chans.get(i);
                    LinearLayout row = Ui.row(PlayerActivity.this);
                    row.setPadding(Ui.dp(PlayerActivity.this, 16), Ui.dp(PlayerActivity.this, 8),
                            Ui.dp(PlayerActivity.this, 16), Ui.dp(PlayerActivity.this, 8));
                    row.setBackground(Ui.pressable(PlayerActivity.this, i == liveIndex ? 0x22FFFFFF : 0, 0));
                    ImageView logo = new ImageView(PlayerActivity.this);
                    logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    logo.setBackground(Ui.round(Theme.SURFACE_HI, Ui.dp(PlayerActivity.this, 8)));
                    int lp = Ui.dp(PlayerActivity.this, 4);
                    logo.setPadding(lp, lp, lp, lp);
                    ImageLoader.load(ch.logo, logo, Ui.dp(PlayerActivity.this, 60), 0);
                    row.addView(logo, Ui.lp(Ui.dp(PlayerActivity.this, 60), Ui.dp(PlayerActivity.this, 40)));
                    TextView t = Ui.oneLine(Ui.text(PlayerActivity.this, ch.name, 15,
                            i == liveIndex ? Theme.PINK : Theme.TEXT, i == liveIndex ? Ui.MEDIUM : null));
                    LinearLayout.LayoutParams tl = Ui.weight(1);
                    tl.leftMargin = Ui.dp(PlayerActivity.this, 12);
                    row.addView(t, tl);
                    row.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            closePanel();
                            if (i != liveIndex) playChannel(i);
                        }
                    });
                    return row;
                }
            });
            list.setSelection(Math.max(0, liveIndex - 2));
            return;
        }

        final List<Integer> idx = new ArrayList<Integer>();
        for (int i = 0; i < Repo.queue.size(); i++) if (Repo.queue.get(i).season == item.season) idx.add(i);
        list.setAdapter(new android.widget.BaseAdapter() {
            public int getCount() { return idx.size(); }

            public Object getItem(int i) { return idx.get(i); }

            public long getItemId(int i) { return i; }

            public View getView(final int i, View c, android.view.ViewGroup p) {
                final int q = idx.get(i);
                Repo.PlayItem e = Repo.queue.get(q);
                boolean cur = q == Repo.queueIndex;
                LinearLayout row = Ui.column(PlayerActivity.this);
                row.setPadding(Ui.dp(PlayerActivity.this, 20), Ui.dp(PlayerActivity.this, 12),
                        Ui.dp(PlayerActivity.this, 20), Ui.dp(PlayerActivity.this, 12));
                row.setBackground(Ui.pressable(PlayerActivity.this, cur ? 0x22FFFFFF : 0, 0));
                row.addView(Ui.oneLine(Ui.text(PlayerActivity.this, e.episode + ". " + e.episodeTitle, 15,
                        cur ? Theme.PINK : Theme.TEXT, cur ? Ui.MEDIUM : null)));
                int[] prog = Repo.episodeProgress(e.episodeId);
                String sub = e.durationSecs > 0 ? Meta.runtime(Math.max(1, e.durationSecs / 60)) : "";
                if (cur) sub = "Assistindo agora" + (sub.isEmpty() ? "" : " · " + sub);
                else if (prog != null && prog[1] > 0) sub = (sub.isEmpty() ? "" : sub + " · ") + Ui.remaining(prog[1] - prog[0]);
                if (!sub.isEmpty()) row.addView(Ui.text(PlayerActivity.this, sub, 12, Theme.TEXT_MUTED));
                row.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        closePanel();
                        if (q != Repo.queueIndex) playIndex(q);
                    }
                });
                return row;
            }
        });
        list.setSelection(Math.max(0, idx.indexOf(Repo.queueIndex) - 1));
    }

    private void closePanel() {
        if (panel == null) return;
        root.removeView(panel);
        panel = null;
        immersive();
    }

    // ---------------------------------------------------------------- controls visibility, lock

    private void showControls(boolean show) {
        controlsShown = show && !locked;
        controls.animate().cancel();
        if (controlsShown) {
            controls.setVisibility(View.VISIBLE);
            controls.animate().alpha(1f).setDuration(150).start();
            scheduleHide();
        } else {
            controls.animate().alpha(0f).setDuration(200).withEndAction(new Runnable() {
                public void run() { if (!controlsShown) controls.setVisibility(View.GONE); }
            }).start();
            immersive();
        }
    }

    private void scheduleHide() {
        handler.removeCallbacks(hide);
        handler.postDelayed(hide, HIDE_MS);
    }

    private void setLocked(boolean on) {
        locked = on;
        if (on) {
            showControls(false);
            flashUnlock();
            Ui.toast(this, "Tela bloqueada");
        } else {
            unlockButton.setVisibility(View.GONE);
            showControls(true);
        }
    }

    private final Runnable hideUnlock = new Runnable() {
        public void run() { if (locked) unlockButton.setVisibility(View.GONE); }
    };

    private void flashUnlock() {
        unlockButton.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideUnlock);
        handler.postDelayed(hideUnlock, 3000);
    }

    private final Runnable hideFeedback = new Runnable() {
        public void run() { feedback.setVisibility(View.GONE); }
    };

    private void flash(String text) {
        feedback.setText(text);
        feedback.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideFeedback);
        handler.postDelayed(hideFeedback, 900);
    }

    private void swipe(MotionEvent e) {
        float h = root.getHeight();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                swipeStartY = e.getY();
                swipeMode = 0;
                // Ignore the edges, where the system gestures live.
                if (e.getY() < h * 0.12f || e.getY() > h * 0.88f) swipeMode = -1;
                swipeModeLeft = e.getX() < root.getWidth() / 2f;
                break;
            case MotionEvent.ACTION_MOVE:
                if (swipeMode == -1) return;
                float dy = swipeStartY - e.getY();
                if (swipeMode == 0 && Math.abs(dy) > Ui.dp(this, 24)) {
                    swipeMode = swipeModeLeft ? 1 : 2;
                    swipeStartY = e.getY();
                    swipeStartValue = swipeMode == 1 ? brightness()
                            : audio.getStreamVolume(AudioManager.STREAM_MUSIC) / (float) audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    return;
                }
                if (swipeMode == 0) return;
                float v = Math.max(0f, Math.min(1f, swipeStartValue + dy / (h * 0.7f)));
                if (swipeMode == 1) {
                    WindowManager.LayoutParams lp = getWindow().getAttributes();
                    lp.screenBrightness = Math.max(0.02f, v);
                    getWindow().setAttributes(lp);
                    flash("☀  " + Math.round(v * 100) + "%");
                } else {
                    int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                    audio.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(v * max), 0);
                    flash("🔊  " + Math.round(v * 100) + "%");
                }
                break;
            default:
                break;
        }
    }

    private boolean swipeModeLeft;

    private float brightness() {
        float b = getWindow().getAttributes().screenBrightness;
        if (b >= 0) return b;
        try {
            return android.provider.Settings.System.getInt(getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255f;
        } catch (Exception e) {
            return 0.5f;
        }
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // ---------------------------------------------------------------- keys & lifecycle

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
            case KeyEvent.KEYCODE_SPACE:
                togglePlay();
                return true;
            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                seekBy(30000);
                return true;
            case KeyEvent.KEYCODE_MEDIA_REWIND:
                seekBy(-10000);
                return true;
            case KeyEvent.KEYCODE_CHANNEL_UP:
                if (live) zap(1);
                return true;
            case KeyEvent.KEYCODE_CHANNEL_DOWN:
                if (live) zap(-1);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                if (!controlsShown) {
                    showControls(true);
                    return true;
                }
                break;
            default:
                break;
        }
        if (!controlsShown && !locked && keyCode >= KeyEvent.KEYCODE_DPAD_UP && keyCode <= KeyEvent.KEYCODE_DPAD_RIGHT) {
            showControls(true);
            return true;
        }
        if (controlsShown) scheduleHide();
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (panel != null) {
            closePanel();
            return;
        }
        if (locked) {
            flashUnlock();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
        handler.postDelayed(autosave, 10000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveProgress();
        handler.removeCallbacks(tick);
        handler.removeCallbacks(autosave);
        if (player != null && prepared && player.isPlaying()) {
            player.pause();
            updatePlayPause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        releasePlayer();
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration c) {
        super.onConfigurationChanged(c);
        videoBox.post(new Runnable() {
            public void run() { applyAspect(); }
        });
    }
}
