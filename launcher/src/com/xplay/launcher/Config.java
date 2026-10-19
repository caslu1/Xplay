package com.xplay.launcher;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * The launcher's look and content, edited in the web panel and stored as config/config.json in the
 * GitHub repository. The last copy downloaded is kept on disk so the launcher works offline; the copy
 * bundled in the APK is used until the first download succeeds.
 */
public final class Config {

    static final String REPO = "caslu1/Xplay";
    static final String BRANCH = "ccr-c1251b75-o5q4e9";
    static final String PATH = "config/config.json";

    /** The contents API is never cached by a CDN, so panel changes show up right away. */
    static final String API_URL = "https://api.github.com/repos/" + REPO + "/contents/" + PATH + "?ref=" + BRANCH;
    /** raw.githubusercontent.com has no rate limit but caches for a few minutes. */
    static final String RAW_URL = "https://raw.githubusercontent.com/" + REPO + "/" + BRANCH + "/" + PATH;

    private static Config current;

    public final JSONObject json;
    public final long fetchedAt;

    private Config(JSONObject json, long fetchedAt) {
        this.json = json;
        this.fetchedAt = fetchedAt;
    }

    public static Config get(Context c) {
        if (current == null) current = loadLocal(c);
        return current;
    }

    private static File file(Context c) {
        return new File(c.getFilesDir(), "config.json");
    }

    private static Config loadLocal(Context c) {
        File f = file(c);
        try {
            if (f.exists()) return new Config(new JSONObject(new String(readAll(new FileInputStream(f)), "UTF-8")), f.lastModified());
        } catch (Exception ignored) {
            // A broken cache falls back to the bundled config.
        }
        try {
            InputStream in = c.getResources().openRawResource(R.raw.default_config);
            return new Config(new JSONObject(new String(readAll(in), "UTF-8")), 0);
        } catch (Exception e) {
            return new Config(new JSONObject(), 0);
        }
    }

    /** Address set by hand in the launcher settings, or empty for the repository default. */
    public static String customUrl(Context c) {
        return prefs(c).getString("config_url", "");
    }

    public static void setCustomUrl(Context c, String url) {
        prefs(c).edit().putString("config_url", url == null ? "" : url.trim()).apply();
    }

    public static String sourceLabel(Context c) {
        String custom = customUrl(c);
        return custom.isEmpty() ? REPO + " @ " + BRANCH : custom;
    }

    static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("launcher", Context.MODE_PRIVATE);
    }

    /** Downloads the newest config (blocking; call from {@link Task}) and makes it current. */
    public static Config fetch(Context c) throws Exception {
        String custom = customUrl(c);
        byte[] data;
        if (!custom.isEmpty()) {
            data = Net.get(custom + (custom.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis(), null);
        } else {
            try {
                data = Net.get(API_URL, "application/vnd.github.raw");
            } catch (IOException e) {
                data = Net.get(RAW_URL + "?t=" + System.currentTimeMillis(), null);
            }
        }
        JSONObject json = new JSONObject(new String(data, "UTF-8"));
        FileOutputStream out = new FileOutputStream(file(c));
        out.write(data);
        out.close();
        current = new Config(json, System.currentTimeMillis());
        return current;
    }

    public boolean isStale() {
        int minutes = settings().optInt("refreshMinutes", 30);
        return System.currentTimeMillis() - fetchedAt > Math.max(1, minutes) * 60_000L;
    }

    public JSONObject section(String name) {
        JSONObject o = json.optJSONObject(name);
        return o == null ? new JSONObject() : o;
    }

    public JSONArray list(String name) {
        JSONArray a = json.optJSONArray(name);
        return a == null ? new JSONArray() : a;
    }

    public JSONObject branding() { return section("branding"); }
    public JSONObject settings() { return section("settings"); }
    public JSONObject devices() { return section("devices"); }
    public JSONObject store() { return section("store"); }
    public JSONObject grid() { return section("grid"); }
    public JSONObject ads() { return section("ads"); }

    public String name() {
        String n = branding().optString("name", "");
        return n.isEmpty() ? "Xplay Launcher" : n;
    }

    public JSONObject activeTheme() {
        JSONObject theme = section("theme");
        JSONArray all = theme.optJSONArray("themes");
        String id = theme.optString("active", "");
        if (all == null || all.length() == 0) return new JSONObject();
        for (int i = 0; i < all.length(); i++) {
            JSONObject t = all.optJSONObject(i);
            if (t != null && id.equals(t.optString("id"))) return t;
        }
        return all.optJSONObject(0);
    }

    public int themeColor(String key, int fallback) {
        return Ui.color(activeTheme().optString(key, ""), fallback);
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    /** Plain HTTP GET helpers shared by the config, store and updater. */
    static final class Net {
        private Net() {}

        static HttpURLConnection open(String address, String accept) throws IOException {
            URL url = new URL(address);
            for (int i = 0; i < 6; i++) {
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(12000);
                conn.setReadTimeout(30000);
                conn.setInstanceFollowRedirects(false);
                conn.setRequestProperty("User-Agent", "XplayLauncher");
                if (accept != null) conn.setRequestProperty("Accept", accept);
                int code = conn.getResponseCode();
                if (code >= 300 && code < 400 && conn.getHeaderField("Location") != null) {
                    url = new URL(url, conn.getHeaderField("Location"));
                    conn.disconnect();
                    continue;
                }
                if (code != HttpURLConnection.HTTP_OK) {
                    conn.disconnect();
                    throw new IOException("Servidor respondeu HTTP " + code);
                }
                return conn;
            }
            throw new IOException("Redirecionamentos demais");
        }

        static byte[] get(String address, String accept) throws IOException {
            HttpURLConnection conn = open(address, accept);
            try {
                return readAll(conn.getInputStream());
            } finally {
                conn.disconnect();
            }
        }
    }
}
