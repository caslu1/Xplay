package com.xplay.mobile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Lookups built once per loaded catalog: titles by category, newest first, adult categories. */
public final class Index {

    private static final Pattern ADULT = Pattern.compile("(?i)(adult|xxx|\\+ ?18|18 ?\\+|er[oó]tic|porn|hot ?club)");

    private static List<VodItem> source;
    private static Map<String, List<VodItem>> byCat;
    private static List<VodItem> recent;
    private static List<VodItem> seriesSource;
    private static Map<String, List<VodItem>> seriesByCat;
    private static List<VodItem> seriesRecent;

    private Index() {}

    public static boolean adultName(String s) {
        return s != null && ADULT.matcher(s).find();
    }

    public static boolean adult(VodItem v) {
        Map<String, String> cats = v.series ? Repo.seriesCategories : Repo.movieCategories;
        return cats != null && adultName(cats.get(v.categoryId));
    }

    private static void build(boolean series) {
        List<VodItem> list = series ? Repo.series : Repo.movies;
        if (list == null) list = new ArrayList<VodItem>();
        if (series ? list == seriesSource : list == source) return;
        Map<String, List<VodItem>> map = new HashMap<String, List<VodItem>>();
        List<VodItem> clean = new ArrayList<VodItem>();
        for (VodItem v : list) {
            List<VodItem> l = map.get(v.categoryId);
            if (l == null) {
                l = new ArrayList<VodItem>();
                map.put(v.categoryId, l);
            }
            l.add(v);
            if (!adult(v)) clean.add(v);
        }
        // Newest first; M3U lists have no dates, so their order (added = position) is reversed.
        Collections.sort(clean, new Comparator<VodItem>() {
            public int compare(VodItem a, VodItem b) { return Long.compare(b.added, a.added); }
        });
        if (series) {
            seriesSource = list;
            seriesByCat = map;
            seriesRecent = clean;
        } else {
            source = list;
            byCat = map;
            recent = clean;
        }
    }

    /** Titles of a category in server order. */
    public static List<VodItem> category(boolean series, String id) {
        build(series);
        List<VodItem> l = (series ? seriesByCat : byCat).get(id);
        return l == null ? new ArrayList<VodItem>() : l;
    }

    /** Every title except adult ones, newest first. */
    public static List<VodItem> recent(boolean series) {
        build(series);
        return series ? seriesRecent : recent;
    }

    /** Categories that have titles, adult ones last (and only when asked). */
    public static List<String[]> categories(boolean series, boolean withAdult) {
        build(series);
        Map<String, String> cats = series ? Repo.seriesCategories : Repo.movieCategories;
        List<String[]> out = new ArrayList<String[]>();
        List<String[]> adult = new ArrayList<String[]>();
        if (cats == null) return out;
        for (Map.Entry<String, String> e : cats.entrySet()) {
            if (category(series, e.getKey()).isEmpty()) continue;
            String[] c = {e.getKey(), e.getValue()};
            if (adultName(e.getValue())) adult.add(c);
            else out.add(c);
        }
        if (withAdult) out.addAll(adult);
        return out;
    }

    /** Best rated titles among the newest ones. */
    public static List<VodItem> topRated(boolean series, int max) {
        List<VodItem> pool = new ArrayList<VodItem>(recent(series).subList(0, Math.min(600, recent(series).size())));
        Collections.sort(pool, new Comparator<VodItem>() {
            public int compare(VodItem a, VodItem b) { return Double.compare(b.rating, a.rating); }
        });
        List<VodItem> out = new ArrayList<VodItem>();
        for (VodItem v : pool) {
            if (v.rating < 6.5 || v.rating > 9.8 || v.poster == null) continue;
            out.add(v);
            if (out.size() == max) break;
        }
        return out;
    }

    public static String normalize(String s) {
        String n = java.text.Normalizer.normalize(s == null ? "" : s, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return n.toLowerCase(Locale.ROOT);
    }

    public static List<String> liveGroups(boolean withAdult) {
        List<String> out = new ArrayList<String>();
        if (Repo.liveGroups == null) return out;
        List<String> adult = new ArrayList<String>();
        for (String g : Repo.liveGroups) {
            if (adultName(g)) adult.add(g);
            else out.add(g);
        }
        if (withAdult) out.addAll(adult);
        return out;
    }
}
