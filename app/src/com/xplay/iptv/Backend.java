package com.xplay.iptv;

import android.os.Build;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;

/**
 * The Xplay panel (Supabase): clients log in with a 9-digit code that the admin created for their M3U,
 * and every install has its own device user (3 letters + 3 numbers) the admin links to recharges.
 */
public final class Backend {

    /** Supabase → Project Settings → API. */
    static final String BASE = "https://SEU-PROJETO.supabase.co";
    static final String ANON_KEY = "SUA-CHAVE-ANON";

    private static final String LETTERS = "ABCDEFGHJKLMNPQRSTUVWXYZ";

    private Backend() {}

    public static boolean configured() {
        return !BASE.contains("SEU-PROJETO") && !ANON_KEY.startsWith("SUA-");
    }

    /** This install's user, created once (e.g. "KTR482"). */
    public static String deviceUser() {
        String u = Repo.prefs().getString("device_user", null);
        if (u == null || !u.matches("[A-Z]{3}[0-9]{3}")) {
            SecureRandom r = new SecureRandom();
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 3; i++) b.append(LETTERS.charAt(r.nextInt(LETTERS.length())));
            for (int i = 0; i < 3; i++) b.append((char) ('0' + r.nextInt(10)));
            u = b.toString();
            Repo.prefs().edit().putString("device_user", u).apply();
        }
        return u;
    }

    public static String savedCode() {
        return Repo.prefs().getString("access_code", null);
    }

    public static String expiresAt() {
        return Repo.prefs().getString("access_expires", null);
    }

    public static String clientName() {
        return Repo.prefs().getString("access_name", null);
    }

    public static void clearCode() {
        Repo.prefs().edit().remove("access_code").remove("access_expires").remove("access_name").apply();
    }

    /** Tells the panel this device exists. Worker thread; failures are ignored. */
    public static void registerDevice() {
        if (!configured()) return;
        try {
            JSONObject body = new JSONObject();
            body.put("p_user", deviceUser());
            body.put("p_model", Build.MANUFACTURER + " " + Build.MODEL);
            rpc("app_register_device", body);
        } catch (Exception ignored) {
            // Offline: try again next start.
        }
    }

    /** Thrown when the panel answered and refused the code (wrong, blocked or expired). */
    public static final class Refused extends IOException {
        Refused(String msg) { super(msg); }
    }

    /** Checks the code; on success stores it, the TMDB key and the client's M3U. Worker thread. */
    public static String login(String code) throws Exception {
        JSONObject body = new JSONObject();
        body.put("p_code", code);
        body.put("p_user", deviceUser());
        JSONObject r = rpc("app_login", body);
        if (!r.optBoolean("ok")) throw new Refused(r.optString("error", "Código inválido"));
        String m3u = r.optString("m3u", "");
        if (m3u.isEmpty()) throw new Refused("Este código ainda não tem lista. Fale com o seu revendedor.");
        Tmdb.setKey(r.isNull("tmdb_key") ? "" : r.optString("tmdb_key", ""));
        Repo.prefs().edit().putString("access_code", code)
                .putString("access_expires", r.optString("expires_at", ""))
                .putString("access_name", r.optString("name", "")).apply();
        return m3u;
    }

    /** "2026-11-22T13:05:00+00:00" → "22/11/2026". */
    public static String date(String iso) {
        if (iso == null || iso.length() < 10) return "-";
        return iso.substring(8, 10) + "/" + iso.substring(5, 7) + "/" + iso.substring(0, 4);
    }

    private static JSONObject rpc(String fn, JSONObject body) throws IOException, org.json.JSONException {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE + "/rest/v1/rpc/" + fn).openConnection();
        try {
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setRequestProperty("apikey", ANON_KEY);
            conn.setRequestProperty("Authorization", "Bearer " + ANON_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            OutputStream out = conn.getOutputStream();
            out.write(body.toString().getBytes("UTF-8"));
            out.close();
            int status = conn.getResponseCode();
            InputStream in = status < 400 ? conn.getInputStream() : conn.getErrorStream();
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            if (in != null) {
                byte[] b = new byte[8192];
                int n;
                while ((n = in.read(b)) > 0) buf.write(b, 0, n);
                in.close();
            }
            String text = buf.toString("UTF-8");
            if (status >= 400) throw new IOException("Painel respondeu HTTP " + status);
            return new JSONObject(text);
        } finally {
            conn.disconnect();
        }
    }
}
