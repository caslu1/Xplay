package com.xplay.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Movie / series details (plot, backdrop, episodes…) fetched once per title and shared by every screen. */
public final class InfoCache {

    public interface Callback {
        void done(JSONObject info);
    }

    private static final Map<String, JSONObject> CACHE = new HashMap<String, JSONObject>();
    private static final Map<String, List<Callback>> PENDING = new HashMap<String, List<Callback>>();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(3);

    private InfoCache() {}

    public static void clear() {
        CACHE.clear();
    }

    /** The server answer when already loaded, else null. Main thread. */
    public static JSONObject peek(VodItem v) {
        return CACHE.get(v.favKey());
    }

    /** Loads (or returns cached) details. M3U titles answer their episodes only. Main thread. */
    public static void get(final VodItem v, Callback cb) {
        final String key = v.favKey();
        JSONObject cached = CACHE.get(key);
        if (cached != null) {
            cb.done(cached);
            return;
        }
        if (!Repo.isXtream()) {
            JSONObject o = m3uInfo(v);
            CACHE.put(key, o);
            cb.done(o);
            return;
        }
        List<Callback> waiting = PENDING.get(key);
        if (waiting != null) {
            waiting.add(cb);
            return;
        }
        waiting = new ArrayList<Callback>();
        waiting.add(cb);
        PENDING.put(key, waiting);
        POOL.execute(new Runnable() {
            public void run() {
                JSONObject r;
                try {
                    r = v.series ? Xtream.seriesInfo(v.id) : Xtream.vodInfo(v.id);
                } catch (Exception e) {
                    r = null;
                }
                final JSONObject result = r;
                Task.MAIN.post(new Runnable() {
                    public void run() {
                        if (result != null) CACHE.put(key, result);
                        List<Callback> cbs = PENDING.remove(key);
                        JSONObject out = result != null ? result : new JSONObject();
                        if (cbs != null) for (Callback c : cbs) c.done(out);
                    }
                });
            }
        });
    }

    private static JSONObject m3uInfo(VodItem v) {
        JSONObject o = new JSONObject();
        if (!v.series) return o;
        LinkedHashMap<Integer, List<JSONObject>> seasons = M3u.EPISODES.get(v.id);
        if (seasons == null) return o;
        try {
            JSONObject eps = new JSONObject();
            for (Map.Entry<Integer, List<JSONObject>> s : seasons.entrySet()) {
                eps.put(String.valueOf(s.getKey()), new JSONArray(s.getValue()));
            }
            o.put("episodes", eps);
        } catch (org.json.JSONException ignored) {
            // Plain values; cannot fail.
        }
        return o;
    }
}
