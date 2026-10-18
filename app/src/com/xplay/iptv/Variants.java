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

    /** Every channel in pool with the same base name as c (c included), in pool order. */
    public static List<Channel> of(Channel c, List<Channel> pool) {
        String key = baseOf(c);
        List<Channel> out = new ArrayList<Channel>();
        for (Channel x : pool) if (x == c || baseOf(x).equals(key)) out.add(x);
        if (!out.contains(c)) out.add(0, c);
        return out;
    }
}
