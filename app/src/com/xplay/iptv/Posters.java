package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

/** Poster cards, "continue watching" cards and genre tiles used by the movie/series screens. */
public final class Posters {

    private static final int[][] TILE_COLORS = {
            {0xFF4A2C7A, 0xFF2A1B4D}, {0xFF6B3A2E, 0xFF3A1E1A}, {0xFF3B3F8F, 0xFF22265A},
            {0xFF2E5E6B, 0xFF173740}, {0xFF6B2E5A, 0xFF3D1A34}, {0xFF4F5A2A, 0xFF2C3318},
    };

    /** Told about every poster that gets remote focus (the browse hero on TV). */
    public interface FocusListener {
        void focused(VodItem v);
    }

    public static FocusListener focusListener;

    private Posters() {}

    /** A 2:3 poster with a rating badge and the title underneath; opens the detail screen. */
    public static View card(final Activity a, final VodItem v, int widthDp) {
        LinearLayout card = frameCard(a);
        FrameLayout frame = posterFrame(a, v.poster, v.series, widthDp);

        String rating = v.ratingText();
        float r = Ui.dp(a, 10);
        if (rating != null) {
            TextView badge = Ui.text(a, "★ " + rating, 11, Palette.TEXT, true);
            badge.setBackground(Ui.round(0xB3000000, Ui.dp(a, 8)));
            badge.setPadding(Ui.dp(a, 7), Ui.dp(a, 2), Ui.dp(a, 7), Ui.dp(a, 2));
            FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
            bl.setMargins(Ui.dp(a, 6), Ui.dp(a, 6), 0, 0);
            frame.addView(badge, bl);
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
        addPoster(a, card, frame, widthDp);
        addTitle(a, card, v.name, null, widthDp);

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { DetailActivity.start(a, v); }
        });
        final View.OnFocusChangeListener scale = card.getOnFocusChangeListener();
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View view, boolean has) {
                scale.onFocusChange(view, has);
                if (has && focusListener != null) focusListener.focused(v);
            }
        });
        return card;
    }

    /**
     * A poster with a progress bar for a "continue watching" entry; plays right away.
     * Long press removes it and runs onRemoved.
     */
    public static View continueCard(final Activity a, VodItem v, final JSONObject e, int widthDp,
                                    final Runnable onRemoved) {
        LinearLayout card = frameCard(a);
        FrameLayout frame = posterFrame(a, v == null ? null : v.poster, v != null && v.series, widthDp);

        View shade = new View(a);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {0xCC000000, 0x00000000}));
        frame.addView(shade, new FrameLayout.LayoutParams(-1, Ui.dp(a, 60), Gravity.BOTTOM));
        ImageView play = Ui.icon(a, R.drawable.ic_play, Palette.BLACK);
        play.setBackground(Ui.round(Palette.GOLD, Ui.dp(a, 18)));
        int pp = Ui.dp(a, 6);
        play.setPadding(pp, pp, pp, pp);
        frame.addView(play, new FrameLayout.LayoutParams(Ui.dp(a, 36), Ui.dp(a, 36), Gravity.CENTER));

        FrameLayout track = new FrameLayout(a);
        track.setBackgroundColor(0x66FFFFFF);
        View bar = new View(a);
        bar.setBackgroundColor(Palette.GOLD);
        int dur = Math.max(1, e.optInt("dur", 1));
        int w = Ui.dp(a, widthDp);
        track.addView(bar, new FrameLayout.LayoutParams(w * Math.min(dur, e.optInt("pos")) / dur, -1));
        frame.addView(track, new FrameLayout.LayoutParams(-1, Ui.dp(a, 4), Gravity.BOTTOM));

        addPoster(a, card, frame, widthDp);
        String sub = e.optString("sub", "");
        int left = Math.max(1, (dur - e.optInt("pos")) / 60000);
        addTitle(a, card, e.optString("title"), sub.isEmpty() ? "Faltam " + left + " min" : sub, widthDp);

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Intent i = new Intent(a, PlayerActivity.class);
                i.putExtra(PlayerActivity.EXTRA_URL, e.optString("url"));
                i.putExtra(PlayerActivity.EXTRA_TITLE, e.optString("title"));
                if (e.has("sub")) i.putExtra(PlayerActivity.EXTRA_SUBTITLE, e.optString("sub"));
                i.putExtra(PlayerActivity.EXTRA_KEY, e.optString("key"));
                a.startActivity(i);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                Repo.removeProgress(e.optString("key"));
                Toast.makeText(a, "Removido de Continuar assistindo", Toast.LENGTH_SHORT).show();
                onRemoved.run();
                return true;
            }
        });
        return card;
    }

    /** A category tile with a blurred poster behind its name. */
    public static View genreTile(Activity a, String name, int index, String posterUrl, View.OnClickListener click) {
        float r = Ui.dp(a, 12);
        FrameLayout tile = new FrameLayout(a);
        int[] colors = TILE_COLORS[index % TILE_COLORS.length];
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, colors);
        bg.setCornerRadius(r);
        tile.setBackground(bg);
        Ui.clipRound(tile, r);
        if (posterUrl != null) {
            ImageView img = new ImageView(a);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            ImageLoader.loadBlurred(posterUrl, img);
            tile.addView(img, new FrameLayout.LayoutParams(-1, -1));
        }
        View shade = new View(a);
        shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0xB3000000, 0x66000000}));
        tile.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        tile.setForeground(Ui.focusRing(a, r));
        tile.setFocusable(true);
        tile.setClickable(true);
        TextView t = Ui.text(a, name, 16, Palette.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setShadowLayer(6, 0, 1, Palette.BLACK);
        int p = Ui.dp(a, 10);
        t.setPadding(p, 0, p, 0);
        tile.addView(t, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        tile.setOnClickListener(click);
        return tile;
    }

    // ---------------------------------------------------------------- shared pieces

    private static LinearLayout frameCard(Activity a) {
        LinearLayout card = Ui.column(a);
        card.setFocusable(true);
        card.setClickable(true);
        int p = Ui.dp(a, 4);
        card.setPadding(p, p, p, p);
        card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View view, boolean has) {
                view.animate().scaleX(has ? 1.06f : 1f).scaleY(has ? 1.06f : 1f).setDuration(120).start();
            }
        });
        return card;
    }

    private static FrameLayout posterFrame(Activity a, String poster, boolean series, int widthDp) {
        float r = Ui.dp(a, 10);
        FrameLayout frame = new FrameLayout(a);
        frame.setBackground(Ui.round(Palette.SURFACE_HI, r));
        Ui.clipRound(frame, r);
        // The ring follows the card's focus, drawn on top of the poster.
        frame.setDuplicateParentStateEnabled(true);
        frame.setForeground(Ui.focusRing(a, r));
        ImageView placeholder = Ui.icon(a, series ? R.drawable.ic_series : R.drawable.ic_movie, 0xFF55555A);
        frame.addView(placeholder, new FrameLayout.LayoutParams(Ui.dp(a, 36), Ui.dp(a, 36), Gravity.CENTER));
        ImageView img = new ImageView(a);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ImageLoader.load(poster, img, Ui.dp(a, widthDp), 0);
        frame.addView(img, new FrameLayout.LayoutParams(-1, -1));
        return frame;
    }

    private static void addPoster(Activity a, LinearLayout card, FrameLayout frame, int widthDp) {
        int w = Ui.dp(a, widthDp);
        card.addView(frame, Ui.lp(w, w * 3 / 2));
    }

    private static void addTitle(Activity a, LinearLayout card, String title, String subtitle, int widthDp) {
        int w = Ui.dp(a, widthDp);
        TextView t = Ui.text(a, title, 14, 0xFFDDDDDD, false);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams tl = Ui.lp(w, -2);
        tl.topMargin = Ui.dp(a, 6);
        card.addView(t, tl);
        if (subtitle != null) {
            TextView s = Ui.text(a, subtitle, 12, Palette.GOLD, false);
            s.setSingleLine(true);
            s.setEllipsize(TextUtils.TruncateAt.END);
            s.setGravity(Gravity.CENTER_HORIZONTAL);
            card.addView(s, Ui.lp(w, -2));
        }
    }
}
