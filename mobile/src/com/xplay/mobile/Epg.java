package com.xplay.mobile;

import android.app.Activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Short guide (now / next) per channel from the Xtream API, cached for a few minutes. */
public final class Epg {

    public interface Callback {
        void done(Xtream.Program now);
    }

    public interface ListCallback {
        void done(List<Xtream.Program> programs);
    }

    private static final long TTL = 10 * 60 * 1000L;
    private static final Map<String, List<Xtream.Program>> CACHE = new HashMap<String, List<Xtream.Program>>();
    private static final Map<String, Long> LOADED = new HashMap<String, Long>();
    private static final Map<String, List<ListCallback>> PENDING = new HashMap<String, List<ListCallback>>();
    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(3, 3, 30, TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>() {
                @Override
                public boolean offer(Runnable r) {
                    // Newest first: rows on screen load before the ones scrolled past.
                    return offerFirst(r);
                }
            });

    private Epg() {}

    public static void now(Activity a, Channel c, final Callback cb) {
        programs(c, new ListCallback() {
            public void done(List<Xtream.Program> p) { cb.done(current(p)); }
        });
    }

    public static Xtream.Program current(List<Xtream.Program> p) {
        long t = System.currentTimeMillis();
        if (p == null) return null;
        for (Xtream.Program x : p) if (x.start <= t && x.end > t) return x;
        return null;
    }

    public static Xtream.Program next(List<Xtream.Program> p) {
        long t = System.currentTimeMillis();
        if (p == null) return null;
        for (Xtream.Program x : p) if (x.start > t) return x;
        return null;
    }

    /** Main thread; answers null when the channel has no guide. */
    public static void programs(Channel c, ListCallback cb) {
        if (!Repo.isXtream() || c.streamId == null) {
            cb.done(null);
            return;
        }
        final String k = c.streamId;
        Long at = LOADED.get(k);
        if (at != null && System.currentTimeMillis() - at < TTL) {
            cb.done(CACHE.get(k));
            return;
        }
        List<ListCallback> waiting = PENDING.get(k);
        if (waiting != null) {
            waiting.add(cb);
            return;
        }
        waiting = new ArrayList<ListCallback>();
        waiting.add(cb);
        PENDING.put(k, waiting);
        POOL.execute(new Runnable() {
            public void run() {
                List<Xtream.Program> r;
                try {
                    r = Xtream.shortEpg(k);
                } catch (Exception e) {
                    r = null;
                }
                final List<Xtream.Program> result = r;
                Task.MAIN.post(new Runnable() {
                    public void run() {
                        CACHE.put(k, result);
                        LOADED.put(k, System.currentTimeMillis());
                        List<ListCallback> cbs = PENDING.remove(k);
                        if (cbs != null) for (ListCallback c : cbs) c.done(result);
                    }
                });
            }
        });
    }

    /** 0–100 elapsed share of a program. */
    public static int progress(Xtream.Program p) {
        long t = System.currentTimeMillis();
        if (p.end <= p.start) return 0;
        return (int) Math.max(0, Math.min(100, (t - p.start) * 100 / (p.end - p.start)));
    }
}
