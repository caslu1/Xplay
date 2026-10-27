package com.xplay.mobile;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** Session state shared by all screens: login, profiles, catalogs, favorites and watch progress. */
public final class Repo {

    public static final String MODE_XTREAM = "xtream";
    public static final String MODE_M3U = "m3u";

    /** One person watching: own favorites and own "continue watching". */
    public static final class Profile {
        public final String id;
        public String name;
        public String emoji;
        public int color;

        Profile(String id, String name, String emoji, int color) {
            this.id = id;
            this.name = name;
            this.emoji = emoji;
            this.color = color;
        }
    }

    /** Something the player can open: a movie or an episode. */
    public static final class PlayItem {
        public String url;
        /** Movie or series name. */
        public String title;
        /** "S1 · E6 — A relíquia" for episodes, null for movies. */
        public String subtitle;
        /** VodItem.favKey() of the movie or series. */
        public String key;
        public String itemId;
        public String poster;
        public String year;
        public String episodeId;
        public int season;
        public int episode;
        public String episodeTitle;
        public int durationSecs;
    }

    private static SharedPreferences prefs;

    public static String mode;
    public static String server;
    public static String user;
    public static String pass;
    public static String m3uUrl;

    public static JSONObject account;
    public static List<Channel> channels;
    public static List<String> liveGroups;
    public static List<VodItem> movies;
    public static LinkedHashMap<String, String> movieCategories;
    public static List<VodItem> series;
    public static LinkedHashMap<String, String> seriesCategories;

    /** What the player plays: the episodes of a season (or one movie) and where to start. */
    public static List<PlayItem> queue = new ArrayList<PlayItem>();
    public static int queueIndex;
    /** Channels the player zaps through when playing live TV. */
    public static List<Channel> livePlaylist = new ArrayList<Channel>();

    private static List<Profile> profiles;
    private static Profile profile;
    private static Set<String> favorites;

    private Repo() {}

    public static void init(Context c) {
        if (prefs != null) return;
        ImageLoader.init(c.getApplicationContext());
        prefs = c.getApplicationContext().getSharedPreferences("xplay_mobile", Context.MODE_PRIVATE);
        mode = prefs.getString("mode", null);
        server = prefs.getString("xtream_dns", null);
        user = prefs.getString("xtream_user", null);
        pass = prefs.getString("xtream_pass", null);
        m3uUrl = prefs.getString("playlist_url", null);
        loadProfiles();
    }

    public static SharedPreferences prefs() {
        return prefs;
    }

    public static boolean hasLogin() {
        if (MODE_XTREAM.equals(mode)) return server != null && user != null && pass != null;
        return MODE_M3U.equals(mode) && m3uUrl != null;
    }

    public static boolean isXtream() {
        return MODE_XTREAM.equals(mode);
    }

    public static boolean catalogsLoaded() {
        return channels != null && movies != null && series != null;
    }

    public static void saveXtream(String dns, String u, String p) {
        clearCatalogs();
        mode = MODE_XTREAM;
        server = dns;
        user = u;
        pass = p;
        prefs.edit().putString("mode", mode).putString("xtream_dns", dns)
                .putString("xtream_user", u).putString("xtream_pass", p).apply();
    }

    public static void saveM3u(String url) {
        clearCatalogs();
        mode = MODE_M3U;
        m3uUrl = url;
        prefs.edit().putString("mode", mode).putString("playlist_url", url).apply();
    }

    /** Forgets the active login but keeps the typed values to pre-fill the form. */
    public static void logout() {
        clearCatalogs();
        mode = null;
        prefs.edit().remove("mode").apply();
    }

    public static void clearCatalogs() {
        account = null;
        channels = null;
        liveGroups = null;
        movies = null;
        movieCategories = null;
        series = null;
        seriesCategories = null;
        InfoCache.clear();
    }

    public static VodItem findVod(boolean isSeries, String id) {
        List<VodItem> list = isSeries ? series : movies;
        if (list == null || id == null) return null;
        for (VodItem v : list) if (v.id.equals(id)) return v;
        return null;
    }

    public static VodItem findVod(String favKey) {
        if (favKey == null) return null;
        if (favKey.startsWith("series:")) return findVod(true, favKey.substring(7));
        if (favKey.startsWith("movie:")) return findVod(false, favKey.substring(6));
        return null;
    }

    public static Channel findChannel(String favKey) {
        if (channels == null || favKey == null) return null;
        for (Channel c : channels) if (c.favKey().equals(favKey)) return c;
        return null;
    }

    // ---------------------------------------------------------------- profiles

    private static void loadProfiles() {
        profiles = new ArrayList<Profile>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("profiles", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                profiles.add(new Profile(o.getString("id"), o.optString("name", "Perfil"),
                        o.optString("emoji", "🍿"), o.optInt("color", Theme.AVATAR_BG[0])));
            }
        } catch (JSONException ignored) {
            // Start over with no profiles.
        }
        String current = prefs.getString("profile", null);
        for (Profile p : profiles) if (p.id.equals(current)) profile = p;
        favorites = null;
    }

    private static void saveProfiles() {
        JSONArray arr = new JSONArray();
        try {
            for (Profile p : profiles) {
                arr.put(new JSONObject().put("id", p.id).put("name", p.name).put("emoji", p.emoji)
                        .put("color", p.color));
            }
        } catch (JSONException ignored) {
            return;
        }
        prefs.edit().putString("profiles", arr.toString()).apply();
    }

    public static List<Profile> profiles() {
        return profiles;
    }

    public static Profile profile() {
        return profile;
    }

    public static void selectProfile(Profile p) {
        profile = p;
        favorites = null;
        prefs.edit().putString("profile", p.id).apply();
    }

    public static Profile addProfile(String name, String emoji, int color) {
        Profile p = new Profile(Long.toString(System.currentTimeMillis(), 36), name, emoji, color);
        profiles.add(p);
        saveProfiles();
        return p;
    }

    public static void updateProfile(Profile p, String name, String emoji, int color) {
        p.name = name;
        p.emoji = emoji;
        p.color = color;
        saveProfiles();
    }

    public static void deleteProfile(Profile p) {
        profiles.remove(p);
        saveProfiles();
        prefs.edit().remove("fav_" + p.id).remove("cont_" + p.id).remove("eprog_" + p.id).apply();
        if (profile == p) {
            profile = null;
            favorites = null;
            prefs.edit().remove("profile").apply();
        }
    }

    private static String pid() {
        return profile == null ? "default" : profile.id;
    }

    // ---------------------------------------------------------------- favorites

    private static Set<String> favs() {
        if (favorites == null) {
            favorites = new HashSet<String>(prefs.getStringSet("fav_" + pid(), new HashSet<String>()));
        }
        return favorites;
    }

    public static boolean isFav(String key) {
        return favs().contains(key);
    }

    /** Returns true when the key is now a favorite. */
    public static boolean toggleFav(String key) {
        Set<String> f = favs();
        boolean added = f.add(key);
        if (!added) f.remove(key);
        prefs.edit().putStringSet("fav_" + pid(), new HashSet<String>(f)).apply();
        // Newest first: keep the order in a separate list.
        List<String> order = favOrder();
        order.remove(key);
        if (added) order.add(0, key);
        prefs.edit().putString("favorder_" + pid(), new JSONArray(order).toString()).apply();
        return added;
    }

    /** Favorite keys, most recently added first. */
    public static List<String> favOrder() {
        List<String> out = new ArrayList<String>();
        try {
            JSONArray arr = new JSONArray(prefs.getString("favorder_" + pid(), "[]"));
            for (int i = 0; i < arr.length(); i++) {
                String k = arr.optString(i);
                if (favs().contains(k) && !out.contains(k)) out.add(k);
            }
        } catch (JSONException ignored) {
            // Fall through to the unordered set.
        }
        for (String k : favs()) if (!out.contains(k)) out.add(k);
        return out;
    }

    // ---------------------------------------------------------------- watch progress

    /**
     * Remembers where a movie or episode stopped: one "continue watching" entry per movie or series
     * (its last episode), plus the position of every episode for the progress bars.
     */
    public static void saveProgress(PlayItem it, int pos, int dur) {
        if (it == null || it.key == null || dur <= 0) return;
        if (it.episodeId != null) saveEpisodeProgress(it.episodeId, pos, dur);
        JSONArray old = continueArray();
        JSONArray out = new JSONArray();
        boolean nearEnd = pos >= dur - Math.min(120000, dur / 12);
        boolean keep = pos > 20000 && !nearEnd;
        try {
            if (keep) {
                JSONObject e = new JSONObject();
                e.put("key", it.key).put("url", it.url).put("title", it.title).put("pos", pos).put("dur", dur)
                        .put("ts", System.currentTimeMillis());
                if (it.poster != null) e.put("poster", it.poster);
                if (it.year != null) e.put("year", it.year);
                if (it.subtitle != null) e.put("sub", it.subtitle);
                if (it.episodeId != null) {
                    e.put("epId", it.episodeId).put("season", it.season).put("episode", it.episode);
                    if (it.episodeTitle != null) e.put("epTitle", it.episodeTitle);
                }
                out.put(e);
            }
            for (int i = 0; i < old.length() && out.length() < 40; i++) {
                JSONObject e = old.optJSONObject(i);
                if (e != null && !it.key.equals(e.optString("key"))) out.put(e);
            }
        } catch (JSONException ignored) {
            return;
        }
        prefs.edit().putString("cont_" + pid(), out.toString()).apply();
    }

    public static void removeProgress(String key) {
        JSONArray old = continueArray();
        JSONArray out = new JSONArray();
        for (int i = 0; i < old.length(); i++) {
            JSONObject e = old.optJSONObject(i);
            if (e != null && !key.equals(e.optString("key"))) out.put(e);
        }
        prefs.edit().putString("cont_" + pid(), out.toString()).apply();
    }

    /** Entries of the current profile, most recent first. */
    public static List<JSONObject> continueList() {
        List<JSONObject> out = new ArrayList<JSONObject>();
        JSONArray arr = continueArray();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.optJSONObject(i);
            if (e != null) out.add(e);
        }
        return out;
    }

    public static JSONObject continueEntry(String key) {
        for (JSONObject e : continueList()) if (key.equals(e.optString("key"))) return e;
        return null;
    }

    private static JSONArray continueArray() {
        try {
            return new JSONArray(prefs.getString("cont_" + pid(), "[]"));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    private static JSONObject episodeProgress() {
        try {
            return new JSONObject(prefs.getString("eprog_" + pid(), "{}"));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    private static void saveEpisodeProgress(String episodeId, int pos, int dur) {
        JSONObject all = episodeProgress();
        try {
            all.put(episodeId, new JSONArray().put(pos).put(dur));
            // Keep the store small: forget the oldest entries past 600.
            while (all.length() > 600) all.remove(all.keys().next());
        } catch (JSONException ignored) {
            return;
        }
        prefs.edit().putString("eprog_" + pid(), all.toString()).apply();
    }

    public static void clearEpisodeProgress(String episodeId) {
        JSONObject all = episodeProgress();
        all.remove(episodeId);
        prefs.edit().putString("eprog_" + pid(), all.toString()).apply();
    }

    /** {position, duration} in ms of an episode, or null when never played. */
    public static int[] episodeProgress(String episodeId) {
        JSONArray a = episodeProgress().optJSONArray(episodeId);
        return a == null ? null : new int[] {a.optInt(0), a.optInt(1)};
    }

    /** Resume position for a url (movie or episode) in ms, or 0. */
    public static int resumePosition(PlayItem it) {
        if (it.episodeId != null) {
            int[] p = episodeProgress(it.episodeId);
            if (p != null && p[0] < p[1] - 60000) return p[0];
            return 0;
        }
        JSONObject e = continueEntry(it.key);
        return e != null && it.url != null && it.url.equals(e.optString("url")) ? e.optInt("pos") : 0;
    }

    // ---------------------------------------------------------------- settings

    public static boolean showAdult() {
        return prefs.getBoolean("adult", false);
    }

    public static void setShowAdult(boolean on) {
        prefs.edit().putBoolean("adult", on).apply();
    }

    public static int aspectMode() {
        return prefs.getInt("aspect", 0);
    }

    public static void setAspectMode(int m) {
        prefs.edit().putInt("aspect", m).apply();
    }
}
