package com.xplay.iptv;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Locale;

/** The hero's streaming-service mark and its row of info chips ("01h 46m", "2026", 🇧🇷, "★ 7.3/10", • GENRE). */
public final class MetaChips {

    private MetaChips() {}

    /** A brand-like wordmark for well-known services, else the TMDB logo with its name; null for none. */
    public static View providerMark(Context c, String name, String logoUrl) {
        if (name == null || name.isEmpty()) return null;
        String n = name.toLowerCase(Locale.ROOT);
        String text = null;
        int color = Palette.TEXT;
        Typeface face = Typeface.create("sans-serif-condensed", Typeface.BOLD);
        if (n.contains("netflix")) { text = "NETFLIX"; color = 0xFFE50914; }
        else if (n.contains("globoplay")) { text = "globoplay"; color = 0xFFF0153C; face = Typeface.DEFAULT_BOLD; }
        else if (n.contains("prime video") || n.contains("amazon")) { text = "prime video"; color = 0xFF1FA8E0; face = Typeface.DEFAULT_BOLD; }
        else if (n.contains("disney")) { text = "Disney+"; color = 0xFF3D8BFF; face = Typeface.DEFAULT_BOLD; }
        else if (n.equals("max") || n.contains("hbo")) { text = "max"; color = 0xFF3A6BFF; face = Typeface.DEFAULT_BOLD; }
        else if (n.contains("apple")) { text = "Apple TV+"; face = Typeface.DEFAULT_BOLD; }
        else if (n.contains("paramount")) { text = "Paramount+"; color = 0xFF2E7BFF; face = Typeface.DEFAULT_BOLD; }
        else if (n.contains("star+") || n.equals("star plus")) { text = "star+"; face = Typeface.DEFAULT_BOLD; }
        else if (n.contains("crunchyroll")) { text = "crunchyroll"; color = 0xFFF47521; face = Typeface.DEFAULT_BOLD; }
        if (text != null) {
            TextView t = Ui.text(c, text, 26, color, true);
            t.setTypeface(face);
            t.setIncludeFontPadding(false);
            t.setShadowLayer(8, 0, 2, 0x99000000);
            return t;
        }
        LinearLayout row = Ui.row(c);
        if (logoUrl != null) {
            ImageView logo = new ImageView(c);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            Ui.clipRound(logo, Ui.dp(c, 8));
            ImageLoader.load(logoUrl, logo, Ui.dp(c, 48), 0);
            row.addView(logo, Ui.lp(Ui.dp(c, 30), Ui.dp(c, 30)));
        }
        TextView t = Ui.text(c, name, 16, Palette.TEXT, true);
        t.setPadding(Ui.dp(c, 10), 0, 0, 0);
        row.addView(t);
        return row;
    }

    /** "01h 46m" for movies; series show their seasons instead. */
    public static String runtime(int minutes) {
        if (minutes <= 0) return null;
        return String.format(Locale.US, "%02dh %02dm", minutes / 60, minutes % 60);
    }

    public static void fill(Context c, LinearLayout row, String duration, String year, String flag, double rating,
                            List<String> genres) {
        row.removeAllViews();
        row.setGravity(Gravity.CENTER_VERTICAL);
        if (duration != null) chip(c, row, duration);
        if (year != null && !year.isEmpty()) chip(c, row, year).setTypeface(Typeface.DEFAULT_BOLD);
        if (flag != null && !flag.isEmpty()) chip(c, row, flag);
        if (rating > 0) chip(c, row, String.format(Locale.US, "★ %.1f/10", rating));
        if (genres != null && !genres.isEmpty()) {
            StringBuilder sb = new StringBuilder("•  ");
            for (int i = 0; i < genres.size() && i < 3; i++) {
                if (i > 0) sb.append("  ·  ");
                sb.append(genres.get(i).toUpperCase(Locale.getDefault()));
            }
            TextView g = Ui.text(c, sb, 12, Palette.TEXT, true);
            g.setLetterSpacing(0.08f);
            g.setSingleLine(true);
            g.setPadding(Ui.dp(c, 4), 0, 0, 0);
            row.addView(g);
        }
    }

    private static TextView chip(Context c, LinearLayout row, String text) {
        TextView t = Ui.text(c, text, 12, 0xFFE6E6E6, false);
        t.setGravity(Gravity.CENTER);
        t.setSingleLine(true);
        t.setBackground(Ui.round(0x33FFFFFF, Ui.dp(c, 8)));
        t.setPadding(Ui.dp(c, 9), Ui.dp(c, 3), Ui.dp(c, 9), Ui.dp(c, 3));
        LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
        lp.rightMargin = Ui.dp(c, 6);
        row.addView(t, lp);
        return t;
    }
}
