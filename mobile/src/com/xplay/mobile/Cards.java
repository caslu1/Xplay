package com.xplay.mobile;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.List;

/** Poster cards and the horizontal rows that hold them. */
public final class Cards {

    private Cards() {}

    public static void openDetail(Activity a, VodItem v) {
        Intent i = new Intent(a, DetailActivity.class);
        i.putExtra(DetailActivity.EXTRA_KEY, v.favKey());
        a.startActivity(i);
    }

    /** Rounded poster image with a soft placeholder showing the title until the image arrives. */
    public static FrameLayout posterFrame(Activity a, String poster, String name, int widthPx, int heightPx) {
        FrameLayout f = new FrameLayout(a);
        f.setBackground(Ui.round(Theme.SURFACE_HI, Ui.dp(a, 12)));
        Ui.clipRound(f, Ui.dp(a, 12));
        TextView ph = Ui.text(a, name, 13, Theme.TEXT_MUTED, Ui.MEDIUM);
        ph.setGravity(Gravity.CENTER);
        ph.setMaxLines(4);
        ph.setEllipsize(android.text.TextUtils.TruncateAt.END);
        int p = Ui.dp(a, 8);
        ph.setPadding(p, p, p, p);
        f.addView(ph, Ui.frame(-1, -1, Gravity.CENTER));
        ImageView img = new ImageView(a);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        f.addView(img, Ui.frame(-1, -1, Gravity.CENTER));
        ImageLoader.load(poster, img, widthPx, 0);
        f.setLayoutParams(new LinearLayout.LayoutParams(widthPx, heightPx));
        return f;
    }

    /** Poster with the name below; tap opens the details, long press toggles favorite. */
    public static View poster(final Activity a, final VodItem v, int widthPx, boolean showName) {
        LinearLayout card = Ui.column(a);
        card.setClickable(true);
        card.setFocusable(true);
        card.setBackground(Ui.pressable(a, 0, Ui.dp(a, 14)));
        FrameLayout frame = posterFrame(a, v.poster, v.cleanName(), widthPx, widthPx * 3 / 2);
        card.addView(frame);
        if (v.rating >= 0.1) {
            TextView r = Ui.badge(a, "★ " + String.format(java.util.Locale.US, "%.1f", v.rating));
            r.setTextSize(11);
            r.setPadding(Ui.dp(a, 7), Ui.dp(a, 2), Ui.dp(a, 7), Ui.dp(a, 2));
            FrameLayout.LayoutParams rl = Ui.frame(-2, -2, Gravity.TOP | Gravity.END);
            rl.setMargins(0, Ui.dp(a, 6), Ui.dp(a, 6), 0);
            frame.addView(r, rl);
        }
        if (showName) {
            TextView t = Ui.oneLine(Ui.text(a, v.cleanName(), 14, Theme.TEXT));
            card.addView(t, Ui.lp(widthPx, -2, 0, Ui.dp(a, 8), 0, 0));
            String year = v.yearInName();
            if (year != null) card.addView(Ui.text(a, year, 13, Theme.TEXT_MUTED));
        }
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) { openDetail(a, v); }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View x) {
                boolean on = Repo.toggleFav(v.favKey());
                Ui.toast(a, on ? "Adicionado aos favoritos" : "Removido dos favoritos");
                return true;
            }
        });
        return card;
    }

    /** "Continuar assistindo" card: badges with episode and time left, progress line, name and year. */
    public static View continueCard(final Activity a, final JSONObject e, int widthPx) {
        final VodItem v = Repo.findVod(e.optString("key"));
        String name = v != null ? v.cleanName() : e.optString("title");
        String poster = v != null && v.poster != null ? v.poster : e.optString("poster", null);
        int pos = e.optInt("pos"), dur = e.optInt("dur");
        LinearLayout card = Ui.column(a);
        card.setClickable(true);
        card.setFocusable(true);
        card.setBackground(Ui.pressable(a, 0, Ui.dp(a, 14)));
        FrameLayout frame = posterFrame(a, poster, name, widthPx, widthPx * 3 / 2);
        card.addView(frame);

        View shade = new View(a);
        shade.setBackground(Ui.vertical(0x66000000, 0x00000000, 0x00000000, 0x99000000));
        frame.addView(shade, Ui.frame(-1, -1, Gravity.CENTER));

        LinearLayout top = Ui.row(a);
        int m = Ui.dp(a, 8);
        top.setPadding(m, m, m, 0);
        if (e.has("episode")) top.addView(Ui.badge(a, String.valueOf(e.optInt("episode"))));
        top.addView(new View(a), Ui.spacer());
        if (dur > 0) top.addView(Ui.badge(a, Ui.remaining(dur - pos)));
        frame.addView(top, Ui.frame(-1, -2, Gravity.TOP));

        if (e.has("season")) {
            TextView ep = Ui.badge(a, "T" + e.optInt("season") + " • E" + e.optInt("episode"));
            FrameLayout.LayoutParams el = Ui.frame(-2, -2, Gravity.BOTTOM | Gravity.END);
            el.setMargins(0, 0, m, Ui.dp(a, 18));
            frame.addView(ep, el);
        }
        View line = Ui.progressLine(a, dur > 0 ? pos / (float) dur : 0);
        FrameLayout.LayoutParams ll = Ui.frame(-1, Ui.dp(a, 4), Gravity.BOTTOM);
        ll.setMargins(m * 2, 0, m * 2, Ui.dp(a, 8));
        frame.addView(line, ll);

        card.addView(Ui.oneLine(Ui.text(a, name, 16, Theme.TEXT)), Ui.lp(widthPx, -2, 0, Ui.dp(a, 10), 0, 0));
        String year = e.optString("year", "");
        if (year.isEmpty() && v != null && v.yearInName() != null) year = v.yearInName();
        if (!year.isEmpty()) card.addView(Ui.text(a, year, 14, Theme.TEXT_MUTED));

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                if (v != null) openDetail(a, v);
                else resumeDirect(a, e);
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View x) {
                Sheet.confirm(a, "Remover de Continuar assistindo?", null, "Remover", new Runnable() {
                    public void run() {
                        Repo.removeProgress(e.optString("key"));
                        if (a instanceof MainActivity) ((MainActivity) a).refreshHome();
                    }
                });
                return true;
            }
        });
        return card;
    }

    /** Plays a "continue" entry whose title is no longer in the catalog. */
    private static void resumeDirect(Activity a, JSONObject e) {
        Repo.PlayItem it = new Repo.PlayItem();
        it.url = e.optString("url");
        it.title = e.optString("title");
        it.subtitle = e.optString("sub", null);
        it.key = e.optString("key");
        it.poster = e.optString("poster", null);
        it.year = e.optString("year", null);
        if (e.has("epId")) {
            it.episodeId = e.optString("epId");
            it.season = e.optInt("season");
            it.episode = e.optInt("episode");
            it.episodeTitle = e.optString("epTitle", null);
        }
        java.util.List<Repo.PlayItem> q = new java.util.ArrayList<Repo.PlayItem>();
        q.add(it);
        PlayerActivity.playVod(a, q, 0);
    }

    /** Square-ish channel tile: logo on a dark card, name below. */
    public static View channel(final Activity a, final Channel c, final List<Channel> playlist, int widthPx) {
        LinearLayout card = Ui.column(a);
        card.setClickable(true);
        card.setFocusable(true);
        card.setBackground(Ui.pressable(a, 0, Ui.dp(a, 14)));
        FrameLayout box = new FrameLayout(a);
        box.setBackground(Ui.round(Theme.SURFACE_HI, Ui.dp(a, 14)));
        TextView initials = Ui.text(a, c.name.length() > 3 ? c.name.substring(0, 3) : c.name, 18, Theme.TEXT_MUTED, Ui.BOLD);
        initials.setGravity(Gravity.CENTER);
        box.addView(initials, Ui.frame(-1, -1, Gravity.CENTER));
        ImageView logo = new ImageView(a);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int p = Ui.dp(a, 14);
        logo.setPadding(p, p, p, p);
        box.addView(logo, Ui.frame(-1, -1, Gravity.CENTER));
        ImageLoader.load(c.logo, logo, widthPx, 0);
        card.addView(box, Ui.lp(widthPx, widthPx * 3 / 4));
        card.addView(Ui.oneLine(Ui.text(a, c.name, 13, Theme.TEXT_DIM)), Ui.lp(widthPx, -2, 0, Ui.dp(a, 8), 0, 0));
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                int i = playlist.indexOf(c);
                PlayerActivity.playLive(a, playlist, Math.max(0, i));
            }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View x) {
                boolean on = Repo.toggleFav(c.favKey());
                Ui.toast(a, on ? "Canal adicionado aos favoritos" : "Canal removido dos favoritos");
                return true;
            }
        });
        return card;
    }

    /** Section title with an optional "Ver tudo" action. */
    public static View sectionTitle(Activity a, String title, View.OnClickListener more) {
        LinearLayout row = Ui.row(a);
        row.setPadding(Ui.dp(a, 20), Ui.dp(a, 26), Ui.dp(a, 12), Ui.dp(a, 12));
        TextView t = Ui.oneLine(Ui.text(a, title, 21, Theme.TEXT, Ui.MEDIUM));
        row.addView(t, Ui.weight(1));
        if (more != null) {
            LinearLayout m = Ui.row(a);
            m.setClickable(true);
            m.setFocusable(true);
            m.setBackground(Ui.pressable(a, 0, Ui.dp(a, 20)));
            m.setPadding(Ui.dp(a, 10), Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4));
            m.addView(Ui.text(a, "Ver tudo", 14, Theme.TEXT_DIM));
            m.addView(Ui.icon(a, R.drawable.ic_chevron_right, Theme.TEXT_DIM), Ui.lp(Ui.dp(a, 20), Ui.dp(a, 20)));
            m.setOnClickListener(more);
            row.addView(m);
        }
        return row;
    }

    /** Horizontally scrolling strip; children get a gap between them. */
    public static LinearLayout strip(Activity a, LinearLayout parent) {
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipToPadding(false);
        hs.setPadding(Ui.dp(a, 14), 0, Ui.dp(a, 14), 0);
        LinearLayout row = Ui.row(a);
        row.setGravity(Gravity.TOP);
        hs.addView(row);
        parent.addView(hs, Ui.lp(-1, -2));
        return row;
    }

    public static void addToStrip(Activity a, LinearLayout strip, View card) {
        LinearLayout.LayoutParams l = Ui.lp(-2, -2);
        int g = Ui.dp(a, 5);
        l.setMargins(g, 0, g, 0);
        strip.addView(card, l);
    }

    /** Width for posters in rows: ~3.2 per phone screen, never huge on tablets / landscape. */
    public static int rowPosterWidth(Activity a) {
        int w = Ui.screenWidth(a);
        return Math.min((int) (w / 3.3f), Ui.dp(a, 150));
    }
}
