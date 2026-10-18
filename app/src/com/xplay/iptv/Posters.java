package com.xplay.iptv;

import android.app.Activity;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Poster cards and genre tiles used by the movie/series screens. */
public final class Posters {

    private static final int[][] TILE_COLORS = {
            {0xFF4A2C7A, 0xFF2A1B4D}, {0xFF6B3A2E, 0xFF3A1E1A}, {0xFF3B3F8F, 0xFF22265A},
            {0xFF2E5E6B, 0xFF173740}, {0xFF6B2E5A, 0xFF3D1A34}, {0xFF4F5A2A, 0xFF2C3318},
    };

    private Posters() {}

    /** A 2:3 poster with a rating badge and the title underneath; opens the detail screen. */
    public static View card(final Activity a, final VodItem v, int widthDp) {
        int w = Ui.dp(a, widthDp);
        float r = Ui.dp(a, 10);
        LinearLayout card = Ui.column(a);
        card.setFocusable(true);
        card.setClickable(true);
        int p = Ui.dp(a, 3);
        card.setPadding(p, p, p, p);
        card.setBackground(Ui.selectable(a, 0, 0x1FFFFFFF, Ui.dp(a, 12), Palette.GOLD));

        FrameLayout frame = new FrameLayout(a);
        frame.setBackground(Ui.round(Palette.SURFACE_HI, r));
        Ui.clipRound(frame, r);
        ImageView placeholder = Ui.icon(a, v.series ? R.drawable.ic_series : R.drawable.ic_movie, 0xFF55555A);
        frame.addView(placeholder, new FrameLayout.LayoutParams(Ui.dp(a, 36), Ui.dp(a, 36), Gravity.CENTER));
        ImageView img = new ImageView(a);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ImageLoader.load(v.poster, img, w, 0);
        frame.addView(img, new FrameLayout.LayoutParams(-1, -1));

        String rating = v.ratingText();
        if (rating != null) {
            TextView badge = Ui.text(a, "★ " + rating, 12, Palette.BLACK, true);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Palette.GOLD);
            bg.setCornerRadii(new float[] {0, 0, r, r, 0, 0, r, r});
            badge.setBackground(bg);
            badge.setPadding(Ui.dp(a, 8), Ui.dp(a, 3), Ui.dp(a, 8), Ui.dp(a, 3));
            frame.addView(badge, new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END));
        }
        if (Repo.isFav(v.favKey())) {
            ImageView heart = Ui.icon(a, R.drawable.ic_heart_fill, Palette.BLACK);
            heart.setBackground(Ui.round(Palette.GOLD, Ui.dp(a, 12)));
            int hp = Ui.dp(a, 4);
            heart.setPadding(hp, hp, hp, hp);
            FrameLayout.LayoutParams hl = new FrameLayout.LayoutParams(Ui.dp(a, 24), Ui.dp(a, 24),
                    Gravity.BOTTOM | Gravity.END);
            hl.setMargins(0, 0, Ui.dp(a, 6), Ui.dp(a, 6));
            frame.addView(heart, hl);
        }
        card.addView(frame, Ui.lp(w, w * 3 / 2));

        TextView title = Ui.text(a, v.name, 14, 0xFFDDDDDD, false);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams tl = Ui.lp(w, -2);
        tl.topMargin = Ui.dp(a, 6);
        card.addView(title, tl);

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { DetailActivity.start(a, v); }
        });
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View view, boolean has) {
                view.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(120).start();
            }
        });
        return card;
    }

    /** A colored tile with a category name. */
    public static View genreTile(Activity a, String name, int index, View.OnClickListener click) {
        int[] colors = TILE_COLORS[index % TILE_COLORS.length];
        FrameLayout tile = new FrameLayout(a);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        bg.setCornerRadius(Ui.dp(a, 12));
        tile.setBackground(bg);
        tile.setForeground(Ui.selectable(a, 0, 0x22FFFFFF, Ui.dp(a, 12), Palette.GOLD));
        tile.setFocusable(true);
        tile.setClickable(true);
        TextView t = Ui.text(a, name, 15, Palette.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        int p = Ui.dp(a, 8);
        t.setPadding(p, 0, p, 0);
        tile.addView(t, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        tile.setOnClickListener(click);
        return tile;
    }
}
