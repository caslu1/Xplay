package com.xplay.iptv;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/** Session state shared by all screens: credentials, loaded catalogs and favorites. */
public final class Repo {

    public static final String MODE_XTREAM = "xtream";
    public static final String MODE_M3U = "m3u";

    private static SharedPreferences prefs;

    public static String mode;
    public static String server;
    public static String user;
    public static String pass;
    public static String m3uUrl;

    public static JSONObject account;
    public static List<Channel> channels;
    /** Live group names in server order. */
    public static List<String> liveGroups;
    public static List<VodItem> movies;
    public static LinkedHashMap<String, String> movieCategories;
    public static List<VodItem> series;
    public static LinkedHashMap<String, String> seriesCategories;

    /** Channel list the player zaps through, and the channel last watched. */
    public static List<Channel> playlist = new ArrayList<Channel>();
    public static Channel current;

    private static Set<String> favorites;

    private Repo() {}

    public static void init(Context c) {
        if (prefs != null) return;
        prefs = c.getApplicationContext().getSharedPreferences("xplay", Context.MODE_PRIVATE);
        mode = prefs.getString("mode", null);
        server = prefs.getString("xtream_dns", null);
        user = prefs.getString("xtream_user", null);
        pass = prefs.getString("xtream_pass", null);
        m3uUrl = prefs.getString("playlist_url", null);
        favorites = new HashSet<String>(prefs.getStringSet("favorites", new HashSet<String>()));
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

    private static void clearCatalogs() {
        account = null;
        channels = null;
        liveGroups = null;
        movies = null;
        movieCategories = null;
        series = null;
        seriesCategories = null;
        current = null;
    }

    public static boolean isFav(String key) {
        return favorites.contains(key);
    }

    /** Returns true when the key is now a favorite. */
    public static boolean toggleFav(String key) {
        boolean added = favorites.add(key);
        if (!added) favorites.remove(key);
        prefs.edit().putStringSet("favorites", new HashSet<String>(favorites)).apply();
        return added;
    }

    public static String liveKey(Channel c) {
        // Older versions stored the bare URL; keep that format so favorites survive.
        return c.url;
    }

    public static VodItem findVod(boolean isSeries, String id) {
        List<VodItem> list = isSeries ? series : movies;
        if (list == null) return null;
        for (VodItem v : list) if (v.id.equals(id)) return v;
        return null;
    }

    public static float listZoom() {
        return prefs.getFloat("list_zoom", 1f);
    }

    public static void setListZoom(float z) {
        prefs.edit().putFloat("list_zoom", z).apply();
    }
}
