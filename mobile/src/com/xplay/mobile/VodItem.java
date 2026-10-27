package com.xplay.mobile;

/** A movie or a series. */
public final class VodItem {
    public final boolean series;
    public final String id;
    public final String name;
    public final String poster;
    public final double rating;
    public final String categoryId;
    public final String extension;
    public final long added;
    /** Direct stream address for M3U movies; null for Xtream titles. */
    public final String url;

    public VodItem(boolean series, String id, String name, String poster, double rating,
                   String categoryId, String extension, long added, String url) {
        this.series = series;
        this.id = id;
        this.name = name;
        this.poster = poster;
        this.rating = rating;
        this.categoryId = categoryId;
        this.extension = extension;
        this.added = added;
        this.url = url;
    }

    public String favKey() {
        return (series ? "series:" : "movie:") + id;
    }

    /** Name without the year and tags some lists add ("Silo (2023) [L]" → "Silo"). */
    public String cleanName() {
        String n = name.replaceAll("\\(\\d{4}\\)", "").replaceAll("\\[[^]]*]", "")
                .replaceAll("(?i)\\b(legendado|dublado|leg|dub|4k|fhd|hd)\\b\\s*$", "").trim();
        n = n.replaceAll("[\\s\\-|:]+$", "").trim();
        return n.isEmpty() ? name : n;
    }

    /** Year written in the name ("Moana (2016)"), or null. */
    public String yearInName() {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\((\\d{4})\\)").matcher(name);
        return m.find() ? m.group(1) : null;
    }
}
