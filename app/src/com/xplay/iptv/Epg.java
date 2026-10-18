package com.xplay.iptv;

import android.os.Handler;
import android.os.Looper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Now/next cache shared by every screen; one request per channel, newest requests first. */
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

    private Epg() {}

    /** Cached programs that are still current, or null. Main thread only. */
    public static List<Xtream.Program> peek(Channel c) {
        if (c.streamId == null) return null;
        List<Xtream.Program> p = CACHE.get(c.streamId);
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
    public static void get(Channel c, Callback cb) {
        if (c.streamId == null) {
            cb.done(new ArrayList<Xtream.Program>());
            return;
        }
        List<Xtream.Program> cached = peek(c);
        if (cached != null) {
            cb.done(cached);
            return;
        }
        final String id = c.streamId;
        List<Callback> waiting = PENDING.get(id);
        if (waiting != null) {
            waiting.add(cb);
            return;
        }
        waiting = new ArrayList<Callback>();
        waiting.add(cb);
        PENDING.put(id, waiting);
        POOL.execute(new Runnable() {
            public void run() {
                List<Xtream.Program> result;
                try {
                    result = Xtream.shortEpg(id);
                } catch (Exception e) {
                    result = new ArrayList<Xtream.Program>();
                }
                final List<Xtream.Program> r = result;
                MAIN.post(new Runnable() {
                    public void run() {
                        CACHE.put(id, r);
                        List<Callback> cbs = PENDING.remove(id);
                        if (cbs != null) for (Callback x : cbs) x.done(r);
                    }
                });
            }
        });
    }

    /** 0..1000 progress through the program. */
    public static int progress(Xtream.Program p) {
        long span = Math.max(1, p.end - p.start);
        long done = System.currentTimeMillis() - p.start;
        return (int) Math.max(0, Math.min(1000, done * 1000 / span));
    }
}
