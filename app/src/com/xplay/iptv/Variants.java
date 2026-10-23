package com.xplay.iptv;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Groups versions of the same channel: "Globo SP SD", "Globo SP HD", "Globo SP FHD", "Globo SP 4K". */
public final class Variants {

    private static final Pattern QUALITY = Pattern.compile(
            "\\b(sd|hd|fhd|full ?hd|uhd|4k|8k|2k|h\\.?26[45]|hevc|x26[45]|1080p?|720p?|480p?|60 ?fps|alt|backup|"
                    + "op[cç]ao ?\\d|vip|premium|\\d{3,4}p)\\b|[\\[(][^\\])]*[\\])]|[¹²³⁴]|\\*+");

    private static final java.util.Map<Channel, String> BASES = new java.util.IdentityHashMap<Channel, String>();

    private Variants() {}

    private static synchronized String baseOf(Channel c) {
        String b = BASES.get(c);
        if (b == null) {
            b = base(c.name);
            BASES.put(c, b);
        }
        return b;
    }

    /** Name without quality tags, accents and punctuation, lower case. */
    public static String base(String name) {
        String n = Normalizer.normalize(name == null ? "" : name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        n = n.toLowerCase(Locale.ROOT).replace('_', ' ');
        n = QUALITY.matcher(n).replaceAll(" ");
        n = n.replaceAll("[|:;,.\\-]+", " ").replaceAll("\\s+", " ").trim();
        return n.isEmpty() ? (name == null ? "" : name.toLowerCase(Locale.ROOT)) : n;
    }

    private static final Pattern TAG_UHD = Pattern.compile("(?i)\\b(4k|8k|uhd)\\b");
    private static final Pattern TAG_FHD = Pattern.compile("(?i)\\b(fhd|full ?hd|1080p?)\\b");
    private static final Pattern TAG_HD = Pattern.compile("(?i)\\b(hd|720p?)\\b");
    private static final Pattern TAG_SD = Pattern.compile("(?i)\\b(sd|480p?)\\b");
    private static final Pattern TAG_EXTRA = Pattern.compile("(?i)\\b(h\\.?265|hevc|alt|backup)\\b|[\\[(]");

    /** Lower is preferred: plain HD first, then FHD, SD, untagged, 4K, and backups last. */
    static int rank(Channel c) {
        String n = c.name;
        int extra = TAG_EXTRA.matcher(n).find() ? 10 : 0;
        if (TAG_UHD.matcher(n).find()) return 4 + extra;
        if (TAG_FHD.matcher(n).find()) return 1 + extra;
        if (TAG_HD.matcher(n).find()) return extra;
        if (TAG_SD.matcher(n).find()) return 2 + extra;
        return 3 + extra;
    }

    /**
     * One entry per channel name: the HD version stands for its SD / FHD / 4K siblings, kept at the
     * position where the name first appears.
     */
    public static List<Channel> collapse(List<Channel> channels) {
        java.util.LinkedHashMap<String, Channel> best = new java.util.LinkedHashMap<String, Channel>();
        for (Channel c : channels) {
            String key = baseOf(c);
            Channel cur = best.get(key);
            if (cur == null || rank(c) < rank(cur)) best.put(key, c);
        }
        return new ArrayList<Channel>(best.values());
    }

    private static final Pattern SHOWN_TAGS = Pattern.compile(
            "(?i)\\s*[\\[(]?\\b(sd|hd|fhd|full ?hd|uhd|4k|8k|h\\.?26[45]|hevc|1080p?|720p?|480p?)\\b[\\])]?");

    /** The name as written in the list, minus quality tags ("Globo SP HD" → "Globo SP"). */
    public static String displayName(Channel c) {
        String n = SHOWN_TAGS.matcher(c.name).replaceAll("").replaceAll("[\\s|\\-–:]+$", "").replaceAll("\\s{2,}", " ").trim();
        return n.isEmpty() ? c.name : n;
    }

    /** Group name without the redundant word "canais" ("CANAIS | ESPORTES" → "ESPORTES"). */
    public static String cleanGroup(String g) {
        if (g == null) return "";
        String s = g.replaceAll("(?i)\\bcana(is|l)\\b", " ").replaceAll("^[\\s|:•\\-–]+|[\\s|:•\\-–]+$", "")
                .replaceAll("\\s{2,}", " ").trim();
        return s.isEmpty() ? g.trim() : s;
    }

    /** Every channel in pool with the same base name as c (c included), in pool order. */
    public static List<Channel> of(Channel c, List<Channel> pool) {
        String key = baseOf(c);
        List<Channel> out = new ArrayList<Channel>();
        for (Channel x : pool) if (x == c || baseOf(x).equals(key)) out.add(x);
        if (!out.contains(c)) out.add(0, c);
        return out;
    }

    /** Best-ranked version of c's channel not tried yet (for when a stream is off), or null. */
    public static Channel untried(Channel c, List<Channel> pool, java.util.Set<Channel> tried) {
        Channel best = null;
        for (Channel x : of(c, pool)) {
            if (tried.contains(x)) continue;
            if (best == null || rank(x) < rank(best)) best = x;
        }
        return best;
    }

    /** Short quality label for messages: "FHD", "SD", "4K"… or "outra versão". */
    public static String quality(Channel c) {
        java.util.regex.Matcher m = Pattern.compile("(?i)\\b(sd|hd|fhd|full ?hd|uhd|4k|8k|h\\.?265|hevc|1080p?|720p?)\\b")
                .matcher(c.name);
        return m.find() ? m.group(1).toUpperCase(Locale.ROOT) : "outra versão";
    }
}
