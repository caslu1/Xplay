package com.xplay.iptv;

import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Optional TMDB lookup (the user's own free API key, set in Settings): where a title streams in
 * Brazil (Netflix, Globoplay…), runtime, year, country, rating, genres, Portuguese plot and backdrop.
 */
public final class Tmdb {

    public static final class Details {
        public String provider;
        public String providerLogo;
        public String backdrop;
        public String overview;
        public String year;
        public int runtime;
        public int seasons;
        public double rating;
        public String country;
        public final List<String> genres = new ArrayList<String>();
    }

    public interface Callback {
        void done(Details d);
    }

    private static final String API = "https://api.themoviedb.org/3";
    private static final String IMG = "https://image.tmdb.org/t/p/";
    private static final Map<String, Details> CACHE = new HashMap<String, Details>();
    private static final ExecutorService POOL = Executors.newFixedThreadPool(2);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private Tmdb() {}

    public static String key() {
        return Repo.prefs().getString("tmdb_key", "").trim();
    }

    public static void setKey(String k) {
        Repo.prefs().edit().putString("tmdb_key", k.trim()).apply();
        CACHE.clear();
    }

    public static boolean enabled() {
        return !key().isEmpty();
    }

    public static Details peek(VodItem v) {
        return CACHE.get(v.favKey());
    }

    /** Looks the title up (using the server's tmdb id when it has one); null when not found. Main thread. */
    public static void get(final VodItem v, final JSONObject xtream, final Callback cb) {
        if (!enabled()) {
            cb.done(null);
            return;
        }
        final String k = v.favKey();
        if (CACHE.containsKey(k)) {
            cb.done(CACHE.get(k));
            return;
        }
        POOL.execute(new Runnable() {
            public void run() {
                Details d = null;
                try {
                    d = lookup(v, xtream);
                } catch (Exception ignored) {
                    // Missing or invalid key, no network, no match: just no extra details.
                }
                final Details r = d;
                MAIN.post(new Runnable() {
                    public void run() {
                        CACHE.put(k, r);
                        cb.done(r);
                    }
                });
            }
        });
    }

    /** Checks the key against TMDB; returns null when valid, else an error message. Worker thread. */
    public static String test(String k) {
        try {
            get(API + "/configuration", k);
            return null;
        } catch (Exception e) {
            String m = e.getMessage();
            return m != null && m.contains("401") ? "Chave inválida" : Task.message(e);
        }
    }

    private static Details lookup(VodItem v, JSONObject xtream) throws Exception {
        String type = v.series ? "tv" : "movie";
        String id = InfoCache.field(xtream, "tmdb_id", "tmdb");
        if (!id.matches("\\d+")) {
            String year = InfoCache.field(xtream, "releasedate", "release_date", "releaseDate");
            String name = v.name.replaceAll("\\(\\d{4}\\)", "").replaceAll("\\[[^]]*]", "").trim();
            String q = API + "/search/" + type + "?language=pt-BR&query=" + URLEncoder.encode(name, "UTF-8")
                    + (year.length() >= 4 ? (v.series ? "&first_air_date_year=" : "&year=") + year.substring(0, 4) : "");
            JSONArray results = new JSONObject(get(q, key())).optJSONArray("results");
            if (results == null || results.length() == 0) return null;
            id = results.getJSONObject(0).optString("id");
        }
        JSONObject o = new JSONObject(get(API + "/" + type + "/" + id
                + "?language=pt-BR&append_to_response=watch/providers", key()));
        Details d = new Details();
        d.overview = o.optString("overview", "");
        String bd = o.optString("backdrop_path", "");
        if (bd.startsWith("/")) d.backdrop = IMG + "w1280" + bd;
        String date = o.optString(v.series ? "first_air_date" : "release_date", "");
        d.year = date.length() >= 4 ? date.substring(0, 4) : "";
        d.runtime = o.optInt("runtime", 0);
        if (d.runtime == 0) {
            JSONArray er = o.optJSONArray("episode_run_time");
            if (er != null && er.length() > 0) d.runtime = er.optInt(0);
        }
        d.seasons = o.optInt("number_of_seasons", 0);
        d.rating = o.optDouble("vote_average", 0);
        JSONArray g = o.optJSONArray("genres");
        for (int i = 0; g != null && i < g.length(); i++) d.genres.add(g.getJSONObject(i).optString("name"));
        JSONArray oc = o.optJSONArray("origin_country");
        if (oc != null && oc.length() > 0) d.country = oc.optString(0);
        else {
            JSONArray pc = o.optJSONArray("production_countries");
            if (pc != null && pc.length() > 0) d.country = pc.getJSONObject(0).optString("iso_3166_1");
        }
        JSONObject wp = o.optJSONObject("watch/providers");
        JSONObject br = wp == null || wp.optJSONObject("results") == null ? null
                : wp.optJSONObject("results").optJSONObject("BR");
        JSONArray flat = br == null ? null : br.optJSONArray("flatrate");
        if (flat != null && flat.length() > 0) {
            JSONObject p = flat.getJSONObject(0);
            d.provider = p.optString("provider_name");
            String logo = p.optString("logo_path", "");
            if (logo.startsWith("/")) d.providerLogo = IMG + "w154" + logo;
        }
        return d;
    }

    private static String get(String url, String k) throws Exception {
        boolean bearer = k.startsWith("eyJ");
        String full = bearer ? url : url + (url.contains("?") ? "&" : "?") + "api_key=" + URLEncoder.encode(k, "UTF-8");
        HttpURLConnection conn = (HttpURLConnection) new URL(full).openConnection();
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Accept", "application/json");
        if (bearer) conn.setRequestProperty("Authorization", "Bearer " + k);
        try {
            int code = conn.getResponseCode();
            if (code != 200) throw new java.io.IOException("TMDB respondeu HTTP " + code);
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            return out.toString("UTF-8");
        } finally {
            conn.disconnect();
        }
    }

    /** Flag emoji for a country code ("BR" → 🇧🇷). */
    public static String flag(String iso) {
        if (iso == null || iso.length() != 2) return "";
        String c = iso.toUpperCase(java.util.Locale.ROOT);
        return new String(Character.toChars(0x1F1E6 + c.charAt(0) - 'A'))
                + new String(Character.toChars(0x1F1E6 + c.charAt(1) - 'A'));
    }
}
