package com.xplay.iptv;

import android.os.Handler;
import android.os.Looper;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Now/next for channels. Xtream channels ask the quick per-channel API first; anything it does not
 * cover comes from the full XMLTV guide (Xtream xmltv.php or the M3U url-tvg), loaded once.
 */
public final class Epg {

    public interface Callback {
        void done(List<Xtream.Program> programs);
    }

    private static final Map<String, List<Xtream.Program>> CACHE = new HashMap<String, List<Xtream.Program>>();
    private static final Map<String, List<Callback>> PENDING = new HashMap<String, List<Callback>>();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(4, 4, 30, TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>() {
                @Override
                public boolean offer(Runnable r) {
                    return offerFirst(r);
                }
            });

    // Full guide, keyed by lower-case channel id; display names map to ids.
    private static String guideUrl;
    private static volatile Map<String, List<Xtream.Program>> guide;
    private static volatile Map<String, String> guideNames;
    private static boolean guideLoading;
    private static final List<Runnable> GUIDE_WAITERS = new ArrayList<Runnable>();

    private Epg() {}

    /** Sets the XMLTV address for the current login (null when there is none). Main thread. */
    public static void setGuideUrl(final String url) {
        MAIN.post(new Runnable() {
            public void run() {
                if (url == null ? guideUrl == null : url.equals(guideUrl)) return;
                guideUrl = url;
                guide = null;
                guideNames = null;
                guideLoading = false;
                CACHE.clear();
            }
        });
    }

    private static String key(Channel c) {
        return c.streamId != null ? c.streamId : "n:" + c.url;
    }

    /** Cached programs that are still current, or null when unknown. Main thread only. */
    public static List<Xtream.Program> peek(Channel c) {
        List<Xtream.Program> p = CACHE.get(key(c));
        if (p == null) return null;
        long now = System.currentTimeMillis();
        List<Xtream.Program> live = new ArrayList<Xtream.Program>();
        for (Xtream.Program x : p) if (x.end > now) live.add(x);
        // An empty answer is cached too, so it is not asked again; a stale list is refetched.
        return live.isEmpty() && !p.isEmpty() ? null : live;
    }

    /** The current program, if known. */
    public static Xtream.Program now(Channel c) {
        List<Xtream.Program> p = peek(c);
        if (p == null || p.isEmpty()) return null;
        Xtream.Program first = p.get(0);
        return first.start <= System.currentTimeMillis() ? first : null;
    }

    /** Delivers programs on the main thread (immediately when cached). Main thread only. */
    public static void get(final Channel c, Callback cb) {
        List<Xtream.Program> cached = peek(c);
        if (cached != null) {
            cb.done(cached);
            return;
        }
        final String k = key(c);
        List<Callback> waiting = PENDING.get(k);
        if (waiting != null) {
            waiting.add(cb);
            return;
        }
        waiting = new ArrayList<Callback>();
        waiting.add(cb);
        PENDING.put(k, waiting);
        if (c.streamId != null && Repo.isXtream()) {
            POOL.execute(new Runnable() {
                public void run() {
                    List<Xtream.Program> r;
                    try {
                        r = Xtream.shortEpg(c.streamId);
                    } catch (Exception e) {
                        r = new ArrayList<Xtream.Program>();
                    }
                    final List<Xtream.Program> result = r;
                    MAIN.post(new Runnable() {
                        public void run() {
                            if (!result.isEmpty()) deliver(k, result);
                            else fromGuide(c, k);
                        }
                    });
                }
            });
        } else {
            fromGuide(c, k);
        }
    }

    private static void deliver(String k, List<Xtream.Program> programs) {
        CACHE.put(k, programs);
        List<Callback> cbs = PENDING.remove(k);
        if (cbs != null) for (Callback x : cbs) x.done(programs);
    }

    /** Looks the channel up in the full guide, loading the guide first if needed. Main thread. */
    private static void fromGuide(final Channel c, final String k) {
        if (guideUrl == null) {
            deliver(k, new ArrayList<Xtream.Program>());
            return;
        }
        if (guide != null) {
            deliver(k, lookup(c));
            return;
        }
        GUIDE_WAITERS.add(new Runnable() {
            public void run() { deliver(k, lookup(c)); }
        });
        if (guideLoading) return;
        guideLoading = true;
        final String url = guideUrl;
        new Thread(new Runnable() {
            public void run() {
                final Map<String, String> names = new HashMap<String, String>();
                final Map<String, List<Xtream.Program>> loaded = loadGuide(url, names);
                MAIN.post(new Runnable() {
                    public void run() {
                        if (!url.equals(guideUrl)) return;
                        guideNames = names;
                        guide = loaded;
                        guideLoading = false;
                        List<Runnable> w = new ArrayList<Runnable>(GUIDE_WAITERS);
                        GUIDE_WAITERS.clear();
                        for (Runnable r : w) r.run();
                    }
                });
            }
        }, "xmltv").start();
    }

    private static List<Xtream.Program> lookup(Channel c) {
        Map<String, List<Xtream.Program>> g = guide;
        if (g == null) return new ArrayList<Xtream.Program>();
        List<Xtream.Program> p = null;
        if (c.epgId != null) p = g.get(c.epgId.toLowerCase(Locale.ROOT));
        if (p == null && guideNames != null) {
            String id = guideNames.get(c.name.trim().toLowerCase(Locale.ROOT));
            if (id != null) p = g.get(id);
        }
        if (p == null) return new ArrayList<Xtream.Program>();
        long now = System.currentTimeMillis();
        List<Xtream.Program> out = new ArrayList<Xtream.Program>();
        for (Xtream.Program x : p) {
            if (x.end > now) out.add(x);
            if (out.size() >= 4) break;
        }
        return out;
    }

    /** Streams the XMLTV file, keeping only programs from one hour ago to a day ahead. */
    private static Map<String, List<Xtream.Program>> loadGuide(String url, Map<String, String> names) {
        Map<String, List<Xtream.Program>> out = new HashMap<String, List<Xtream.Program>>();
        long now = System.currentTimeMillis();
        long from = now - 3600000L, to = now + 86400000L;
        SimpleDateFormat withZone = new SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US);
        SimpleDateFormat noZone = new SimpleDateFormat("yyyyMMddHHmmss", Locale.US);
        noZone.setTimeZone(TimeZone.getTimeZone("UTC"));
        InputStream in = null;
        try {
            in = M3uParser.open(url, 120000);
            XmlPullParser x = Xml.newPullParser();
            x.setInput(in, null);
            String channel = null, title = null, desc = null, channelId = null;
            long start = 0, stop = 0;
            String tag = null;
            for (int ev = x.getEventType(); ev != XmlPullParser.END_DOCUMENT; ev = x.next()) {
                if (ev == XmlPullParser.START_TAG) {
                    tag = x.getName();
                    if ("programme".equals(tag)) {
                        channel = x.getAttributeValue(null, "channel");
                        start = time(x.getAttributeValue(null, "start"), withZone, noZone);
                        stop = time(x.getAttributeValue(null, "stop"), withZone, noZone);
                        title = null;
                        desc = null;
                    } else if ("channel".equals(tag)) {
                        channelId = x.getAttributeValue(null, "id");
                    }
                } else if (ev == XmlPullParser.TEXT && tag != null) {
                    if ("title".equals(tag) && title == null) title = x.getText().trim();
                    else if ("desc".equals(tag) && desc == null) desc = x.getText().trim();
                    else if ("display-name".equals(tag) && channelId != null) {
                        names.put(x.getText().trim().toLowerCase(Locale.ROOT), channelId.toLowerCase(Locale.ROOT));
                    }
                } else if (ev == XmlPullParser.END_TAG) {
                    if ("programme".equals(x.getName()) && channel != null && stop > from && start < to
                            && title != null) {
                        String k = channel.toLowerCase(Locale.ROOT);
                        List<Xtream.Program> l = out.get(k);
                        if (l == null) {
                            l = new ArrayList<Xtream.Program>();
                            out.put(k, l);
                        }
                        l.add(new Xtream.Program(title, desc == null ? "" : desc, start, stop));
                    } else if ("channel".equals(x.getName())) {
                        channelId = null;
                    }
                    tag = null;
                }
            }
        } catch (Exception | OutOfMemoryError e) {
            // Keep whatever was read before the failure.
        } finally {
            try {
                if (in != null) in.close();
            } catch (Exception ignored) {
                // Nothing to do.
            }
        }
        Comparator<Xtream.Program> byStart = new Comparator<Xtream.Program>() {
            public int compare(Xtream.Program a, Xtream.Program b) { return Long.compare(a.start, b.start); }
        };
        for (List<Xtream.Program> l : out.values()) Collections.sort(l, byStart);
        return out;
    }

    private static long time(String s, SimpleDateFormat withZone, SimpleDateFormat noZone) {
        if (s == null) return 0;
        try {
            return withZone.parse(s.trim()).getTime();
        } catch (Exception e) {
            try {
                return noZone.parse(s.trim().length() >= 14 ? s.trim().substring(0, 14) : s.trim()).getTime();
            } catch (Exception e2) {
                return 0;
            }
        }
    }

    /** 0..1000 progress through the program. */
    public static int progress(Xtream.Program p) {
        long span = Math.max(1, p.end - p.start);
        long done = System.currentTimeMillis() - p.start;
        return (int) Math.max(0, Math.min(1000, done * 1000 / span));
    }
}
