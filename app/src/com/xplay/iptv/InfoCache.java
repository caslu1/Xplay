package com.xplay.iptv;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Movie / series details (plot, backdrop, year…) fetched once per title and shared by every screen. */
public final class InfoCache {

    public interface Callback {
        void done(JSONObject info);
    }

    private static final Map<String, JSONObject> CACHE = new HashMap<String, JSONObject>();
    private static final Map<String, List<Callback>> PENDING = new HashMap<String, List<Callback>>();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private InfoCache() {}

    /** The whole server answer ({info, episodes…}) when already loaded, else null. Main thread. */
    public static JSONObject peek(VodItem v) {
        return CACHE.get(v.favKey());
    }

    /** Loads (or returns cached) details; M3U titles answer an empty object. Main thread. */
    public static void get(final VodItem v, Callback cb) {
        final String key = v.favKey();
        JSONObject cached = CACHE.get(key);
        if (cached != null) {
            cb.done(cached);
            return;
        }
        if (!Repo.isXtream()) {
            cb.done(new JSONObject());
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
                MAIN.post(new Runnable() {
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

    /** First usable value of a field inside "info" (arrays give their first entry). */
    public static String field(JSONObject answer, String... keys) {
        JSONObject info = answer == null ? null : answer.optJSONObject("info");
        if (info == null) return "";
        for (String k : keys) {
            Object o = info.opt(k);
            if (o instanceof JSONArray) o = ((JSONArray) o).length() > 0 ? ((JSONArray) o).opt(0) : null;
            if (o == null || o == JSONObject.NULL) continue;
            String s = String.valueOf(o).trim();
            if (!s.isEmpty() && !s.equals("null")) return s;
        }
        return "";
    }
}
