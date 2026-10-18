package com.xplay.iptv;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Loads live channels from an Xtream Codes compatible server (player_api.php). */
public final class XtreamClient {

    private XtreamClient() {}

    public static String normalizeServer(String dns) {
        String s = dns.trim();
        if (!s.contains("://")) s = "http://" + s;
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    public static List<Channel> load(String dns, String user, String pass) throws IOException {
        String server = normalizeServer(dns);
        String api = server + "/player_api.php?username=" + enc(user) + "&password=" + enc(pass);
        try {
            Object login = new JSONObject(get(api)).opt("user_info");
            if (!(login instanceof JSONObject) || ((JSONObject) login).optInt("auth", 0) != 1) {
                throw new IOException("Usuário ou senha inválidos");
            }
            String accountStatus = ((JSONObject) login).optString("status", "Active");
            if (!accountStatus.equalsIgnoreCase("Active")) {
                throw new IOException("Conta " + accountStatus);
            }

            Map<String, String> categories = new HashMap<String, String>();
            JSONArray cats = new JSONArray(get(api + "&action=get_live_categories"));
            for (int i = 0; i < cats.length(); i++) {
                JSONObject c = cats.getJSONObject(i);
                categories.put(c.optString("category_id"), c.optString("category_name"));
            }

            JSONArray streams = new JSONArray(get(api + "&action=get_live_streams"));
            List<Channel> channels = new ArrayList<Channel>(streams.length());
            for (int i = 0; i < streams.length(); i++) {
                JSONObject s = streams.getJSONObject(i);
                String group = categories.get(s.optString("category_id"));
                if (group == null || group.isEmpty()) group = "Sem grupo";
                // HLS (.m3u8) is the format the platform MediaPlayer handles best.
                String url = server + "/live/" + enc(user) + "/" + enc(pass) + "/"
                        + s.optString("stream_id") + ".m3u8";
                String logo = s.optString("stream_icon", "");
                channels.add(new Channel(s.optString("name", "Canal"), url, group,
                        logo.startsWith("http") ? logo : null));
            }
            return channels;
        } catch (JSONException e) {
            throw new IOException("Resposta inválida do servidor (confira o DNS)");
        }
    }

    private static String enc(String s) throws IOException {
        return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
    }

    private static String get(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "Xplay/1.0");
        try {
            int code = conn.getResponseCode();
            if (code != HttpURLConnection.HTTP_OK) throw new IOException("Servidor respondeu HTTP " + code);
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            return out.toString("UTF-8");
        } finally {
            conn.disconnect();
        }
    }
}
