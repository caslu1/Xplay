package com.xplay.mobile;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reads the details screens show (genres, year, runtime, country…) from an Xtream info answer. */
public final class Meta {

    private static final Locale BR = new Locale("pt", "BR");

    private Meta() {}

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

    public static List<String> genres(JSONObject answer) {
        List<String> out = new ArrayList<String>();
        for (String g : field(answer, "genre", "genres").split("\\s*[,/|&]\\s*")) {
            g = g.trim();
            if (g.isEmpty()) continue;
            g = Character.toUpperCase(g.charAt(0)) + g.substring(1);
            if (!out.contains(g)) out.add(g);
        }
        return out;
    }

    public static String plot(JSONObject answer) {
        return field(answer, "plot", "description", "overview");
    }

    public static String backdrop(JSONObject answer) {
        return Xtream.httpOrNull(field(answer, "backdrop_path", "backdrop"));
    }

    public static String cover(JSONObject answer) {
        return Xtream.httpOrNull(field(answer, "movie_image", "cover_big", "cover"));
    }

    public static String trailer(JSONObject answer) {
        String t = field(answer, "youtube_trailer", "trailer");
        if (t.isEmpty()) return null;
        if (t.startsWith("http")) return t;
        return "https://www.youtube.com/watch?v=" + t;
    }

    public static String cast(JSONObject answer) {
        return field(answer, "cast", "actors");
    }

    public static String director(JSONObject answer) {
        return field(answer, "director");
    }

    public static double rating(JSONObject answer, VodItem v) {
        try {
            double r = Double.parseDouble(field(answer, "rating").replace(',', '.'));
            if (r > 0 && r <= 10) return r;
        } catch (NumberFormatException ignored) {
            // Fall back to the list's rating.
        }
        return v == null ? 0 : v.rating;
    }

    public static Date releaseDate(JSONObject answer) {
        String s = field(answer, "releasedate", "release_date", "releaseDate", "air_date");
        if (s.length() >= 10) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(s.substring(0, 10));
            } catch (Exception ignored) {
                // Not a date; maybe just a year.
            }
        }
        return null;
    }

    public static String year(JSONObject answer, VodItem v) {
        String s = field(answer, "releasedate", "release_date", "releaseDate", "year");
        if (s.length() >= 4 && s.substring(0, 4).matches("\\d{4}")) return s.substring(0, 4);
        String y = v == null ? null : v.yearInName();
        return y == null ? "" : y;
    }

    /** "9 out" for titles from this year, else the year ("2023"). */
    public static String shortDate(JSONObject answer, VodItem v) {
        Date d = releaseDate(answer);
        if (d != null) {
            Calendar c = Calendar.getInstance();
            int now = c.get(Calendar.YEAR);
            c.setTime(d);
            if (c.get(Calendar.YEAR) == now) {
                return new SimpleDateFormat("d MMM", BR).format(d).replace(".", "");
            }
        }
        return year(answer, v);
    }

    /** Runtime in minutes, or 0. */
    public static int minutes(JSONObject answer) {
        String secs = field(answer, "duration_secs");
        if (secs.matches("\\d+") && Integer.parseInt(secs) > 0) return Integer.parseInt(secs) / 60;
        String d = field(answer, "duration");
        if (d.matches("\\d{1,2}:\\d{2}(:\\d{2})?")) {
            String[] p = d.split(":");
            return p.length == 3 ? Integer.parseInt(p[0]) * 60 + Integer.parseInt(p[1]) : Integer.parseInt(p[0]);
        }
        String run = field(answer, "episode_run_time", "runtime");
        if (run.matches("\\d+")) return Integer.parseInt(run);
        return 0;
    }

    /** "1h 45m" / "45m". */
    public static String runtime(int minutes) {
        if (minutes <= 0) return "";
        return minutes >= 60 ? (minutes / 60) + "h " + (minutes % 60) + "m" : minutes + "m";
    }

    private static final Map<String, String> COUNTRIES = new HashMap<String, String>();

    static {
        String[] pairs = {
                "brasil", "BR", "brazil", "BR", "estados unidos", "US", "united states", "US", "usa", "US",
                "eua", "US", "united states of america", "US", "reino unido", "GB", "united kingdom", "GB",
                "uk", "GB", "inglaterra", "GB", "canadá", "CA", "canada", "CA", "frança", "FR", "france", "FR",
                "alemanha", "DE", "germany", "DE", "espanha", "ES", "spain", "ES", "itália", "IT", "italy", "IT",
                "japão", "JP", "japan", "JP", "coreia do sul", "KR", "south korea", "KR", "korea", "KR",
                "méxico", "MX", "mexico", "MX", "argentina", "AR", "portugal", "PT", "índia", "IN", "india", "IN",
                "china", "CN", "austrália", "AU", "australia", "AU", "irlanda", "IE", "ireland", "IE",
                "colômbia", "CO", "colombia", "CO", "chile", "CL", "suécia", "SE", "sweden", "SE",
                "noruega", "NO", "norway", "NO", "dinamarca", "DK", "denmark", "DK", "turquia", "TR",
                "turkey", "TR", "rússia", "RU", "russia", "RU", "bélgica", "BE", "belgium", "BE",
                "holanda", "NL", "países baixos", "NL", "netherlands", "NL", "nova zelândia", "NZ",
                "new zealand", "NZ", "áfrica do sul", "ZA", "south africa", "ZA", "polônia", "PL", "poland", "PL",
        };
        for (int i = 0; i < pairs.length; i += 2) COUNTRIES.put(pairs[i], pairs[i + 1]);
    }

    /** Flag emojis for the countries in the answer ("Brasil, Estados Unidos" → 🇧🇷🇺🇸), max 3. */
    public static String flags(JSONObject answer) {
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (String c : field(answer, "country", "countries").split("\\s*[,/|]\\s*")) {
            String k = c.trim().toLowerCase(BR);
            String iso = k.length() == 2 ? k.toUpperCase(Locale.ROOT) : COUNTRIES.get(k);
            if (iso == null || !iso.matches("[A-Z]{2}") || n == 3) continue;
            out.append(flag(iso));
            n++;
        }
        return out.toString();
    }

    public static String flag(String iso) {
        return new String(Character.toChars(0x1F1E6 + iso.charAt(0) - 'A'))
                + new String(Character.toChars(0x1F1E6 + iso.charAt(1) - 'A'));
    }

    /** Seasons of a series answer as {season number → episodes}, in order. */
    public static java.util.TreeMap<Integer, List<JSONObject>> seasons(JSONObject answer) {
        java.util.TreeMap<Integer, List<JSONObject>> out = new java.util.TreeMap<Integer, List<JSONObject>>();
        Object eps = answer == null ? null : answer.opt("episodes");
        if (eps instanceof JSONObject) {
            JSONObject o = (JSONObject) eps;
            java.util.Iterator<String> it = o.keys();
            while (it.hasNext()) {
                String k = it.next();
                JSONArray arr = o.optJSONArray(k);
                if (arr == null) continue;
                int season;
                try {
                    season = Integer.parseInt(k.trim());
                } catch (NumberFormatException e) {
                    season = out.size() + 1;
                }
                out.put(season, list(arr));
            }
        } else if (eps instanceof JSONArray) {
            // Some panels send [[ep, ep], [ep]] instead of {"1": [...]}.
            JSONArray a = (JSONArray) eps;
            for (int i = 0; i < a.length(); i++) {
                JSONArray arr = a.optJSONArray(i);
                if (arr == null || arr.length() == 0) continue;
                int season = arr.optJSONObject(0) == null ? i + 1 : arr.optJSONObject(0).optInt("season", i + 1);
                out.put(season, list(arr));
            }
        }
        for (List<JSONObject> l : out.values()) {
            java.util.Collections.sort(l, new java.util.Comparator<JSONObject>() {
                public int compare(JSONObject a, JSONObject b) {
                    return Integer.compare(a.optInt("episode_num"), b.optInt("episode_num"));
                }
            });
        }
        return out;
    }

    private static List<JSONObject> list(JSONArray arr) {
        List<JSONObject> l = new ArrayList<JSONObject>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.optJSONObject(i);
            if (e != null) l.add(e);
        }
        return l;
    }

    /** Episode title without the series name and "S01E02" prefix most panels repeat. */
    public static String episodeTitle(JSONObject ep, String seriesName) {
        String t = ep.optString("title", "").trim();
        String cleaned = t.replaceFirst("(?i)^.*?[Ss]\\d{1,2}\\s*[Ee]\\d{1,3}\\s*[-–:|.]*\\s*", "");
        if (seriesName != null && cleaned.toLowerCase(BR).startsWith(seriesName.toLowerCase(BR))) {
            cleaned = cleaned.substring(seriesName.length()).replaceFirst("^[\\s\\-–:|.]+", "");
        }
        return cleaned.isEmpty() ? "Episódio " + ep.optInt("episode_num") : cleaned;
    }

    public static String episodeUrl(JSONObject ep) {
        String direct = ep.optString("url", "");
        if (!direct.isEmpty()) return direct;
        return Xtream.episodeUrl(ep.optString("id"), ep.optString("container_extension", "mp4"));
    }

    public static String episodeId(JSONObject ep) {
        String id = ep.optString("id", "");
        return id.isEmpty() ? ep.optString("url") : id;
    }

    public static int episodeSeconds(JSONObject ep) {
        JSONObject info = ep.optJSONObject("info");
        if (info == null) return 0;
        int s = info.optInt("duration_secs", 0);
        if (s > 0) return s;
        String d = info.optString("duration", "");
        if (d.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
            String[] p = d.split(":");
            return Integer.parseInt(p[0]) * 3600 + Integer.parseInt(p[1]) * 60 + Integer.parseInt(p[2]);
        }
        return 0;
    }
}
