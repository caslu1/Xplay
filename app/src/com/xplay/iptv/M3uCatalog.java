package com.xplay.iptv;

import android.net.Uri;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns an M3U playlist into the same catalogs the Xtream API gives: live channels, movies and
 * series (episodes grouped by show and season).
 */
public final class M3uCatalog {

    private static final Pattern EPISODE = Pattern.compile(
            "^(.*?)[\\s._\\-|:]*[Ss](\\d{1,2})\\s*[Ee](\\d{1,3})\\b.*$");
    private static final Pattern VIDEO_FILE = Pattern.compile("(?i)\\.(mp4|mkv|avi|mov|wmv|flv)(\\?.*)?$");

    /** Episodes of M3U series, keyed by the series id; each entry looks like an Xtream episode. */
    public static final Map<String, LinkedHashMap<Integer, List<JSONObject>>> EPISODES =
            new HashMap<String, LinkedHashMap<Integer, List<JSONObject>>>();

    private M3uCatalog() {}

    /**
     * Xtream servers hand out M3U links like http://host:port/get.php?username=U&password=P.
     * Returns {server, user, pass} for such a link, else null.
     */
    public static String[] xtreamCredentials(String url) {
        try {
            Uri u = Uri.parse(url);
            String user = u.getQueryParameter("username");
            String pass = u.getQueryParameter("password");
            String path = u.getPath() == null ? "" : u.getPath();
            if (user == null || pass == null || user.isEmpty() || !path.endsWith(".php")) return null;
            String server = u.getScheme() + "://" + u.getEncodedAuthority();
            return new String[] {server, user, pass};
        } catch (Exception e) {
            return null;
        }
    }

    public static void load(String url) throws IOException {
        M3uParser.Playlist p = M3uParser.download(url);
        List<Channel> live = new ArrayList<Channel>();
        List<String> liveGroups = new ArrayList<String>();
        List<VodItem> movies = new ArrayList<VodItem>();
        LinkedHashMap<String, String> movieCats = new LinkedHashMap<String, String>();
        List<VodItem> series = new ArrayList<VodItem>();
        LinkedHashMap<String, String> seriesCats = new LinkedHashMap<String, String>();
        Map<String, VodItem> seriesByName = new HashMap<String, VodItem>();
        EPISODES.clear();

        for (Channel c : p.entries) {
            String lower = c.url.toLowerCase(Locale.ROOT);
            Matcher ep = EPISODE.matcher(c.name);
            boolean isSeries = lower.contains("/series/") || (ep.matches() && VIDEO_FILE.matcher(lower).find());
            boolean isMovie = !isSeries && (lower.contains("/movie/") || VIDEO_FILE.matcher(lower).find());
            if (isSeries) {
                String show = ep.matches() ? ep.group(1).trim() : c.name;
                if (show.isEmpty()) show = c.name;
                int season = ep.matches() ? Integer.parseInt(ep.group(2)) : 1;
                String key = c.group + "|" + show.toLowerCase(Locale.ROOT);
                VodItem s = seriesByName.get(key);
                if (s == null) {
                    s = new VodItem(true, "m" + series.size(), show, c.logo, 0, c.group, null, 0, null);
                    seriesByName.put(key, s);
                    series.add(s);
                    seriesCats.put(c.group, c.group);
                    EPISODES.put(s.id, new LinkedHashMap<Integer, List<JSONObject>>());
                }
                LinkedHashMap<Integer, List<JSONObject>> seasons = EPISODES.get(s.id);
                List<JSONObject> eps = seasons.get(season);
                if (eps == null) {
                    eps = new ArrayList<JSONObject>();
                    seasons.put(season, eps);
                }
                try {
                    JSONObject e = new JSONObject();
                    e.put("episode_num", ep.matches() ? ep.group(3).replaceFirst("^0+(?=\\d)", "") : String.valueOf(eps.size() + 1));
                    e.put("title", c.name);
                    e.put("url", c.url);
                    if (c.logo != null) e.put("info", new JSONObject().put("movie_image", c.logo));
                    eps.add(e);
                } catch (JSONException ignored) {
                    // Values are plain strings; this cannot fail.
                }
            } else if (isMovie) {
                movies.add(new VodItem(false, "m" + movies.size(), c.name, c.logo, 0, c.group, null,
                        movies.size(), c.url));
                movieCats.put(c.group, c.group);
            } else {
                live.add(c);
                if (!liveGroups.contains(c.group)) liveGroups.add(c.group);
            }
        }
        Repo.channels = live;
        Repo.liveGroups = liveGroups;
        Repo.movies = movies;
        Repo.movieCategories = movieCats;
        Repo.series = series;
        Repo.seriesCategories = seriesCats;
        Epg.setGuideUrl(p.tvgUrl);
    }
}
