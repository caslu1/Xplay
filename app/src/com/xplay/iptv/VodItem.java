package com.xplay.iptv;

/** A movie or a series from the Xtream catalog. */
public final class VodItem {
    public final boolean series;
    public final String id;
    public final String name;
    public final String poster;
    public final double rating;
    public final String categoryId;
    public final String extension;
    public final long added;
    /** Direct stream address for movies from M3U lists; null for Xtream titles. */
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

    public String ratingText() {
        return rating > 0 ? String.format(java.util.Locale.US, "%.1f", rating) : null;
    }
}
