package com.xplay.iptv;

import android.util.Base64;

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
import java.util.LinkedHashMap;
import java.util.List;

/** Client for Xtream Codes compatible servers (player_api.php), using the credentials in Repo. */
public final class Xtream {

    public static final class Program {
        public final String title;
        public final String description;
        public final long start;
        public final long end;

        Program(String title, String description, long start, long end) {
            this.title = title;
            this.description = description;
            this.start = start;
            this.end = end;
        }
    }

    private Xtream() {}

    public static String normalizeServer(String dns) {
        String s = dns.trim();
        if (!s.contains("://")) s = "http://" + s;
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /** Validates the credentials and loads the live channel list. */
    public static void login() throws IOException {
        try {
            Object info = new JSONObject(get(api())).opt("user_info");
            if (!(info instanceof JSONObject) || ((JSONObject) info).optInt("auth", 0) != 1) {
                throw new IOException("Usuário ou senha inválidos");
            }
            String status = ((JSONObject) info).optString("status", "Active");
            if (!status.equalsIgnoreCase("Active")) throw new IOException("Conta " + status);
            Repo.account = (JSONObject) info;
            Epg.setGuideUrl(Repo.server + "/xmltv.php?username=" + enc(Repo.user) + "&password=" + enc(Repo.pass));
        } catch (JSONException e) {
            throw new IOException("Resposta inválida do servidor (confira o DNS)");
        }
        loadLive();
    }

    public static void loadLive() throws IOException {
        try {
            LinkedHashMap<String, String> cats = categories("get_live_categories");
            JSONArray streams = new JSONArray(get(api() + "&action=get_live_streams"));
            List<Channel> list = new ArrayList<Channel>(streams.length());
            for (int i = 0; i < streams.length(); i++) {
                JSONObject s = streams.getJSONObject(i);
                String group = cats.get(s.optString("category_id"));
                if (group == null || group.isEmpty()) group = "Sem grupo";
                String id = s.optString("stream_id");
                // HLS (.m3u8) is the format the platform MediaPlayer handles best.
                String url = Repo.server + "/live/" + enc(Repo.user) + "/" + enc(Repo.pass) + "/" + id + ".m3u8";
                list.add(new Channel(s.optString("name", "Canal"), url, group,
                        httpOrNull(s.optString("stream_icon", "")), id, s.optInt("num", i + 1),
                        blankToNull(s.optString("epg_channel_id", ""))));
            }
            List<String> groups = new ArrayList<String>();
            for (String name : cats.values()) {
                for (Channel c : list) {
                    if (c.group.equals(name)) {
                        groups.add(name);
                        break;
                    }
                }
            }
            for (Channel c : list) if (!groups.contains(c.group)) groups.add(c.group);
            Repo.channels = list;
            Repo.liveGroups = groups;
        } catch (JSONException e) {
            throw new IOException("Não foi possível ler os canais");
        }
    }

    /** Current and next programs for a channel (may be empty). */
    public static List<Program> shortEpg(String streamId) throws IOException {
        List<Program> out = new ArrayList<Program>();
        try {
            JSONObject o = new JSONObject(get(api() + "&action=get_short_epg&limit=4&stream_id=" + streamId));
            JSONArray arr = o.optJSONArray("epg_listings");
            long now = System.currentTimeMillis();
            for (int i = 0; arr != null && i < arr.length(); i++) {
                JSONObject p = arr.getJSONObject(i);
                long start = p.optLong("start_timestamp") * 1000L;
                long end = p.optLong("stop_timestamp") * 1000L;
                if (end < now) continue;
                out.add(new Program(b64(p.optString("title")), b64(p.optString("description")), start, end));
            }
        } catch (JSONException ignored) {
            // Many servers return an empty or malformed EPG; treat it as no data.
        }
        return out;
    }

    /** Loads the movie catalog unless it is already loaded (clear Repo.movies to force). */
    public static synchronized void loadMovies() throws IOException {
        if (Repo.movies != null) return;
        try {
            LinkedHashMap<String, String> cats = categories("get_vod_categories");
            JSONArray arr = new JSONArray(get(api() + "&action=get_vod_streams"));
            List<VodItem> list = new ArrayList<VodItem>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.getJSONObject(i);
                list.add(new VodItem(false, s.optString("stream_id"), s.optString("name", "Filme"),
                        httpOrNull(s.optString("stream_icon", "")), rating(s), s.optString("category_id"),
                        s.optString("container_extension", "mp4"), s.optLong("added", 0), null));
            }
            Repo.movieCategories = cats;
            Repo.movies = list;
        } catch (JSONException e) {
            throw new IOException("Não foi possível ler os filmes");
        }
    }

    /** Loads the series catalog unless it is already loaded (clear Repo.series to force). */
    public static synchronized void loadSeries() throws IOException {
        if (Repo.series != null) return;
        try {
            LinkedHashMap<String, String> cats = categories("get_series_categories");
            JSONArray arr = new JSONArray(get(api() + "&action=get_series"));
            List<VodItem> list = new ArrayList<VodItem>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.getJSONObject(i);
                list.add(new VodItem(true, s.optString("series_id"), s.optString("name", "Série"),
                        httpOrNull(s.optString("cover", "")), rating(s), s.optString("category_id"),
                        null, s.optLong("last_modified", 0), null));
            }
            Repo.seriesCategories = cats;
            Repo.series = list;
        } catch (JSONException e) {
            throw new IOException("Não foi possível ler as séries");
        }
    }

    public static JSONObject vodInfo(String id) throws IOException {
        return object(api() + "&action=get_vod_info&vod_id=" + id);
    }

    public static JSONObject seriesInfo(String id) throws IOException {
        return object(api() + "&action=get_series_info&series_id=" + id);
    }

    public static String movieUrl(String id, String ext) throws IOException {
        return Repo.server + "/movie/" + enc(Repo.user) + "/" + enc(Repo.pass) + "/" + id + "." + ext(ext);
    }

    public static String episodeUrl(String id, String ext) throws IOException {
        return Repo.server + "/series/" + enc(Repo.user) + "/" + enc(Repo.pass) + "/" + id + "." + ext(ext);
    }

    private static String blankToNull(String s) {
        return s == null || s.trim().isEmpty() || s.equals("null") ? null : s.trim();
    }

    public static String httpOrNull(String s) {
        return s != null && s.startsWith("http") ? s : null;
    }

    private static String ext(String e) {
        return e == null || e.isEmpty() ? "mp4" : e;
    }

    private static double rating(JSONObject s) {
        double r = s.optDouble("rating", 0);
        if (Double.isNaN(r) || r <= 0) {
            double five = s.optDouble("rating_5based", 0);
            r = Double.isNaN(five) ? 0 : five * 2;
        }
        return r > 10 ? 10 : r;
    }

    private static LinkedHashMap<String, String> categories(String action) throws IOException, JSONException {
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>();
        JSONArray cats = new JSONArray(get(api() + "&action=" + action));
        for (int i = 0; i < cats.length(); i++) {
            JSONObject c = cats.getJSONObject(i);
            map.put(c.optString("category_id"), c.optString("category_name"));
        }
        return map;
    }

    private static JSONObject object(String url) throws IOException {
        try {
            return new JSONObject(get(url));
        } catch (JSONException e) {
            throw new IOException("Resposta inválida do servidor");
        }
    }

    private static String b64(String s) {
        if (s == null || s.isEmpty()) return "";
        try {
            return new String(Base64.decode(s, Base64.DEFAULT), "UTF-8").trim();
        } catch (Exception e) {
            return s;
        }
    }

    private static String api() throws IOException {
        return Repo.server + "/player_api.php?username=" + enc(Repo.user) + "&password=" + enc(Repo.pass);
    }

    private static String enc(String s) throws IOException {
        return URLEncoder.encode(s, "UTF-8").replace("+", "%20");
    }

    private static String get(String url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "Xplay/2.0");
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
