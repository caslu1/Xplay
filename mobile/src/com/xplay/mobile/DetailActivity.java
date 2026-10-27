package com.xplay.mobile;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Movie / series page: cover, facts, play or continue, favorite, seasons and episodes. */
public class DetailActivity extends Activity {

    static final String EXTRA_KEY = "key";

    private static final Locale BR = new Locale("pt", "BR");

    private VodItem item;
    private JSONObject answer;
    private TreeMap<Integer, List<JSONObject>> seasons = new TreeMap<Integer, List<JSONObject>>();
    private int season = -1;

    private ImageView cover;
    private LinearLayout chips, buttons, seasonTabs, episodes, more;
    private TextView genres, plot, playLabel, extra;
    private HorizontalScrollView seasonScroll;
    private View seasonsTitle, episodesTitle;
    private ProgressBar loading;
    private ImageView favButton;
    private boolean plotOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        item = Repo.findVod(getIntent().getStringExtra(EXTRA_KEY));
        if (item == null) {
            finish();
            return;
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(build());
        answer = InfoCache.peek(item);
        fill();
        InfoCache.get(item, new InfoCache.Callback() {
            public void done(JSONObject info) {
                answer = info;
                loading.setVisibility(View.GONE);
                fill();
            }
        });
    }

    private View build() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Theme.BG);
        ScrollView scroll = new ScrollView(this);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout col = Ui.column(this);
        scroll.addView(col);
        root.addView(scroll, Ui.frame(-1, -1, Gravity.TOP));

        // Cover with the title over a fade into the page.
        int w = Ui.screenWidth(this);
        int h = Ui.landscape(this) ? (int) (getResources().getDisplayMetrics().heightPixels * 0.75f)
                : Math.min((int) (w * 0.98f), (int) (getResources().getDisplayMetrics().heightPixels * 0.58f));
        FrameLayout header = new FrameLayout(this);
        cover = new ImageView(this);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        header.addView(cover, Ui.frame(-1, -1, Gravity.CENTER));
        View fade = new View(this);
        fade.setBackground(Ui.vertical(0x88000000, 0x00000000, 0x00000000, 0xB308080A, Theme.BG));
        header.addView(fade, Ui.frame(-1, -1, Gravity.CENTER));
        TextView title = Ui.text(this, item.cleanName().toUpperCase(BR), 40, Theme.PEACH,
                Typeface.create("sans-serif-condensed", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(3);
        title.setLetterSpacing(0.04f);
        title.setShadowLayer(16, 0, 3, 0xCC000000);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        FrameLayout.LayoutParams tl = Ui.frame(-1, -2, Gravity.BOTTOM);
        tl.setMargins(Ui.dp(this, 24), 0, Ui.dp(this, 24), Ui.dp(this, 14));
        header.addView(title, tl);
        col.addView(header, Ui.lp(-1, h));

        chips = Ui.row(this);
        chips.setGravity(Gravity.CENTER);
        col.addView(chips, Ui.lp(-1, -2, Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), 0));
        genres = Ui.text(this, "", 14, Theme.TEXT_DIM, Ui.BOLD);
        genres.setGravity(Gravity.CENTER);
        genres.setLetterSpacing(0.06f);
        col.addView(genres, Ui.lp(-1, -2, Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), 0));

        int side = Ui.dp(this, 20);
        FrameLayout play = new FrameLayout(this);
        play.setClickable(true);
        play.setFocusable(true);
        play.setBackground(Ui.ripple(Ui.round(0xFFFFFFFF, Ui.dp(this, 40)), Ui.dp(this, 40)));
        LinearLayout pl = Ui.row(this);
        pl.setGravity(Gravity.CENTER);
        pl.addView(Ui.icon(this, R.drawable.ic_play, 0xFF111111), Ui.lp(Ui.dp(this, 26), Ui.dp(this, 26)));
        playLabel = Ui.oneLine(Ui.text(this, "Assistir", 19, 0xFF111111));
        pl.addView(playLabel, Ui.lp(-2, -2, Ui.dp(this, 12), 0, 0, 0));
        play.addView(pl, Ui.frame(-2, -2, Gravity.CENTER));
        play.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { play(false); }
        });
        col.addView(play, Ui.lp(-1, Ui.dp(this, 56), side, Ui.dp(this, 26), side, 0));

        buttons = Ui.row(this);
        col.addView(buttons, Ui.lp(-1, Ui.dp(this, 52), side, Ui.dp(this, 16), side, 0));

        plot = Ui.text(this, "", 15, 0xFFD6D6DA);
        plot.setLineSpacing(0, 1.18f);
        plot.setMaxLines(4);
        plot.setEllipsize(android.text.TextUtils.TruncateAt.END);
        plot.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                plotOpen = !plotOpen;
                plot.setMaxLines(plotOpen ? 100 : 4);
                extra.setVisibility(plotOpen && extra.length() > 0 ? View.VISIBLE : View.GONE);
            }
        });
        col.addView(plot, Ui.lp(-1, -2, side, Ui.dp(this, 20), side, 0));
        extra = Ui.text(this, "", 14, Theme.TEXT_MUTED);
        extra.setLineSpacing(0, 1.15f);
        extra.setVisibility(View.GONE);
        col.addView(extra, Ui.lp(-1, -2, side, Ui.dp(this, 10), side, 0));

        loading = new ProgressBar(this);
        loading.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Theme.PINK));
        LinearLayout.LayoutParams ll = Ui.lp(Ui.dp(this, 32), Ui.dp(this, 32), 0, Ui.dp(this, 24), 0, 0);
        ll.gravity = Gravity.CENTER_HORIZONTAL;
        col.addView(loading, ll);
        if (InfoCache.peek(item) != null) loading.setVisibility(View.GONE);

        seasonsTitle = Ui.text(this, "Temporadas", 21, Theme.TEXT, Ui.MEDIUM);
        seasonsTitle.setPadding(side, Ui.dp(this, 26), side, Ui.dp(this, 6));
        seasonsTitle.setVisibility(View.GONE);
        col.addView(seasonsTitle);
        seasonScroll = new HorizontalScrollView(this);
        seasonScroll.setHorizontalScrollBarEnabled(false);
        seasonScroll.setClipToPadding(false);
        seasonScroll.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 10), 0);
        seasonTabs = Ui.row(this);
        seasonScroll.addView(seasonTabs);
        col.addView(seasonScroll, Ui.lp(-1, -2));
        episodesTitle = Ui.text(this, "Episódios", 21, Theme.TEXT, Ui.MEDIUM);
        episodesTitle.setPadding(side, Ui.dp(this, 20), side, Ui.dp(this, 8));
        episodesTitle.setVisibility(View.GONE);
        col.addView(episodesTitle);
        episodes = Ui.column(this);
        col.addView(episodes, Ui.lp(-1, -2));
        more = Ui.column(this);
        col.addView(more, Ui.lp(-1, -2, 0, 0, 0, Ui.dp(this, 32)));

        ImageView back = Ui.iconButton(this, R.drawable.ic_back, Theme.TEXT, 10);
        back.setBackground(Ui.ripple(Ui.round(0x55000000, Ui.dp(this, 24)), Ui.dp(this, 24)));
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        FrameLayout.LayoutParams bl = Ui.frame(Ui.dp(this, 46), Ui.dp(this, 46), Gravity.TOP | Gravity.START);
        bl.setMargins(Ui.dp(this, 14), statusBar() + Ui.dp(this, 8), 0, 0);
        root.addView(back, bl);
        return root;
    }

    private int statusBar() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : Ui.dp(this, 24);
    }

    private void fill() {
        String bd = Meta.backdrop(answer);
        String img = Ui.landscape(this) && bd != null ? bd : item.poster != null ? item.poster
                : Meta.cover(answer) != null ? Meta.cover(answer) : bd;
        if (img != null && !img.equals(cover.getTag(R.id.image_url))) {
            ImageLoader.load(img, cover, Ui.screenWidth(this), 0);
        }

        chips.removeAllViews();
        int min = Meta.minutes(answer);
        if (min > 0) chip(R.drawable.ic_movie_outline, Meta.runtime(min));
        String year = Meta.year(answer, item);
        if (!year.isEmpty()) chip(0, year);
        String flags = Meta.flags(answer);
        if (!flags.isEmpty()) chip(0, flags);
        double r = Meta.rating(answer, item);
        if (r > 0) chip(R.drawable.ic_star, String.format(Locale.US, "%.1f/10", r));

        List<String> g = Meta.genres(answer);
        StringBuilder gs = new StringBuilder();
        for (int i = 0; i < g.size() && i < 3; i++) gs.append(i == 0 ? "" : "  ·  ").append(g.get(i).toUpperCase(BR));
        genres.setText(gs);
        genres.setVisibility(gs.length() > 0 ? View.VISIBLE : View.GONE);

        String p = Meta.plot(answer);
        plot.setText(p);
        plot.setVisibility(p.isEmpty() ? View.GONE : View.VISIBLE);
        StringBuilder ex = new StringBuilder();
        if (!Meta.director(answer).isEmpty()) ex.append("Direção: ").append(Meta.director(answer));
        if (!Meta.cast(answer).isEmpty()) ex.append(ex.length() > 0 ? "\n" : "").append("Elenco: ").append(Meta.cast(answer));
        extra.setText(ex);

        if (item.series) {
            seasons = Meta.seasons(answer);
            if (!seasons.isEmpty() && (season < 0 || !seasons.containsKey(season))) {
                JSONObject e = Repo.continueEntry(item.favKey());
                season = e != null && seasons.containsKey(e.optInt("season")) ? e.optInt("season") : seasons.firstKey();
            }
            buildSeasons();
        } else {
            buildMore();
        }
        updateButtons();
    }

    private void chip(int icon, String label) {
        LinearLayout c = Ui.row(this);
        c.setBackground(Ui.round(0xFF1E1E22, Ui.dp(this, 10)));
        c.setPadding(Ui.dp(this, 12), Ui.dp(this, 7), Ui.dp(this, 12), Ui.dp(this, 7));
        if (icon != 0) {
            c.addView(Ui.icon(this, icon, icon == R.drawable.ic_star ? Theme.STAR : Theme.TEXT),
                    Ui.lp(Ui.dp(this, 17), Ui.dp(this, 17), 0, 0, Ui.dp(this, 6), 0));
        }
        c.addView(Ui.text(this, label, 15, Theme.TEXT, Ui.MEDIUM));
        LinearLayout.LayoutParams l = Ui.lp(-2, -2);
        l.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
        chips.addView(c, l);
    }

    /** "Assistir" or "Continuar T1:E6 em 8:27", plus restart / trailer / favorite. */
    private void updateButtons() {
        JSONObject e = Repo.continueEntry(item.favKey());
        if (e != null) {
            String at = Ui.shortTime(e.optInt("pos"));
            playLabel.setText(e.has("season") ? "Continuar T" + e.optInt("season") + ":E" + e.optInt("episode") + " em " + at
                    : "Continuar de " + at);
        } else {
            playLabel.setText(item.series ? (seasons.isEmpty() ? "Assistir" : "Assistir T" + seasons.firstKey() + ":E1")
                    : "Assistir");
        }

        buttons.removeAllViews();
        if (e != null) {
            squareButton(R.drawable.ic_restart, e.has("season") ? "Episódio do início" : "Assistir do início", new Runnable() {
                public void run() { play(true); }
            });
        } else {
            squareButton(R.drawable.ic_info, "Informações", new Runnable() {
                public void run() { showInfo(); }
            });
        }
        final String trailer = Meta.trailer(answer);
        squareButton(R.drawable.ic_movie, "Trailer", new Runnable() {
            public void run() {
                if (trailer == null) {
                    Ui.toast(DetailActivity.this, "Trailer não disponível");
                    return;
                }
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(trailer)));
                } catch (Exception ex) {
                    Ui.toast(DetailActivity.this, "Nenhum app para abrir o trailer");
                }
            }
        });
        if (trailer == null) buttons.getChildAt(buttons.getChildCount() - 1).setAlpha(0.4f);
        favButton = squareButton(Repo.isFav(item.favKey()) ? R.drawable.ic_heart_fill : R.drawable.ic_heart, "Favorito",
                new Runnable() {
                    public void run() {
                        boolean on = Repo.toggleFav(item.favKey());
                        favButton.setImageResource(on ? R.drawable.ic_heart_fill : R.drawable.ic_heart);
                        favButton.setColorFilter(on ? Theme.PINK : Theme.TEXT);
                        Ui.toast(DetailActivity.this, on ? "Adicionado aos favoritos" : "Removido dos favoritos");
                    }
                });
        favButton.setColorFilter(Repo.isFav(item.favKey()) ? Theme.PINK : Theme.TEXT);
    }

    private ImageView squareButton(int icon, final String label, final Runnable r) {
        ImageView b = Ui.icon(this, icon, Theme.TEXT);
        int p = Ui.dp(this, 13);
        b.setPadding(p, p, p, p);
        b.setClickable(true);
        b.setFocusable(true);
        b.setContentDescription(label);
        b.setBackground(Ui.ripple(Ui.stroke(0xFF141416, Ui.dp(this, 20), Ui.dp(this, 1), 0xFF2C2C31), Ui.dp(this, 20)));
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { r.run(); }
        });
        b.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) {
                Ui.toast(DetailActivity.this, label);
                return true;
            }
        });
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(0, -1, 1f);
        if (buttons.getChildCount() > 0) l.leftMargin = Ui.dp(this, 12);
        buttons.addView(b, l);
        return b;
    }

    private void showInfo() {
        StringBuilder s = new StringBuilder();
        s.append(item.name);
        String y = Meta.year(answer, item);
        if (!y.isEmpty()) s.append("\nAno: ").append(y);
        int min = Meta.minutes(answer);
        if (min > 0) s.append("\nDuração: ").append(Meta.runtime(min));
        String country = Meta.field(answer, "country");
        if (!country.isEmpty()) s.append("\nPaís: ").append(country);
        if (!Meta.director(answer).isEmpty()) s.append("\nDireção: ").append(Meta.director(answer));
        if (!Meta.cast(answer).isEmpty()) s.append("\nElenco: ").append(Meta.cast(answer));
        if (item.series && !seasons.isEmpty()) {
            int eps = 0;
            for (List<JSONObject> l : seasons.values()) eps += l.size();
            s.append("\n").append(seasons.size()).append(seasons.size() == 1 ? " temporada · " : " temporadas · ")
                    .append(eps).append(" episódios");
        }
        Sheet.message(this, "Informações", s.toString());
    }

    // ---------------------------------------------------------------- seasons / episodes

    private void buildSeasons() {
        seasonTabs.removeAllViews();
        boolean has = !seasons.isEmpty();
        seasonsTitle.setVisibility(has ? View.VISIBLE : View.GONE);
        episodesTitle.setVisibility(has ? View.VISIBLE : View.GONE);
        if (!has) {
            episodes.removeAllViews();
            if (InfoCache.peek(item) != null || !Repo.isXtream()) {
                TextView none = Ui.text(this, "Nenhum episódio disponível", 15, Theme.TEXT_MUTED);
                none.setPadding(Ui.dp(this, 20), Ui.dp(this, 24), 0, 0);
                episodes.addView(none);
            }
            return;
        }
        for (final int s : seasons.keySet()) {
            LinearLayout tab = Ui.column(this);
            tab.setGravity(Gravity.CENTER_HORIZONTAL);
            tab.setClickable(true);
            tab.setFocusable(true);
            tab.setBackground(Ui.pressable(this, 0, Ui.dp(this, 10)));
            tab.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), Ui.dp(this, 12), 0);
            boolean on = s == season;
            tab.addView(Ui.text(this, "Temporada " + s, 18, on ? Theme.TEXT : 0xFF8A8A90, on ? Ui.MEDIUM : null));
            View line = new View(this);
            line.setBackground(on ? Ui.round(0xFFFFFFFF, Ui.dp(this, 2)) : null);
            tab.addView(line, Ui.lp(-1, Ui.dp(this, 3), 0, Ui.dp(this, 12), 0, 0));
            tab.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    season = s;
                    buildSeasons();
                }
            });
            seasonTabs.addView(tab, Ui.lp(-2, -2, Ui.dp(this, 2), 0, Ui.dp(this, 2), 0));
        }
        buildEpisodes();
    }

    private void buildEpisodes() {
        episodes.removeAllViews();
        List<JSONObject> list = seasons.get(season);
        if (list == null) return;
        JSONObject cont = Repo.continueEntry(item.favKey());
        String current = cont == null ? null : cont.optString("epId", null);
        int thumbW = Math.min((int) (Ui.screenWidth(this) * 0.38f), Ui.dp(this, 200));
        for (int i = 0; i < list.size(); i++) {
            final JSONObject ep = list.get(i);
            final String epId = Meta.episodeId(ep);
            LinearLayout row = Ui.column(this);
            row.setClickable(true);
            row.setFocusable(true);
            row.setBackground(Ui.pressable(this, epId.equals(current) ? 0xFF141418 : 0, Ui.dp(this, 16)));
            row.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            LinearLayout top = Ui.row(this);
            FrameLayout thumb = new FrameLayout(this);
            thumb.setBackground(Ui.round(Theme.SURFACE_HI, Ui.dp(this, 10)));
            Ui.clipRound(thumb, Ui.dp(this, 10));
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            thumb.addView(img, Ui.frame(-1, -1, Gravity.CENTER));
            JSONObject info = ep.optJSONObject("info");
            String still = info == null ? null : Xtream.httpOrNull(info.optString("movie_image", ""));
            ImageLoader.load(still != null ? still : Meta.backdrop(answer) != null ? Meta.backdrop(answer) : item.poster,
                    img, thumbW, 0);
            ImageView playIcon = Ui.icon(this, R.drawable.ic_play, Theme.TEXT);
            int pp = Ui.dp(this, 7);
            playIcon.setPadding(pp, pp, pp, pp);
            playIcon.setBackground(Ui.round(0x99000000, Ui.dp(this, 20)));
            thumb.addView(playIcon, Ui.frame(Ui.dp(this, 36), Ui.dp(this, 36), Gravity.CENTER));
            int[] prog = Repo.episodeProgress(epId);
            if (prog != null && prog[1] > 0) {
                View line = Ui.progressLine(this, prog[0] / (float) prog[1]);
                FrameLayout.LayoutParams pl = Ui.frame(-1, Ui.dp(this, 3), Gravity.BOTTOM);
                pl.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 8), Ui.dp(this, 6));
                thumb.addView(line, pl);
            }
            top.addView(thumb, Ui.lp(thumbW, thumbW * 9 / 16));
            LinearLayout texts = Ui.column(this);
            int num = ep.optInt("episode_num", i + 1);
            TextView t = Ui.text(this, num + ". " + Meta.episodeTitle(ep, item.cleanName()), 16, Theme.TEXT, Ui.MEDIUM);
            t.setMaxLines(2);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            texts.addView(t);
            int secs = Meta.episodeSeconds(ep);
            String sub = secs > 0 ? Meta.runtime(Math.max(1, secs / 60)) : "";
            if (prog != null && prog[1] > 0) {
                boolean done = prog[0] >= prog[1] - Math.min(120000, prog[1] / 12);
                sub = (sub.isEmpty() ? "" : sub + " · ") + (done ? "Assistido" : Ui.remaining(prog[1] - prog[0]));
            }
            if (!sub.isEmpty()) texts.addView(Ui.text(this, sub, 13, Theme.TEXT_MUTED), Ui.lp(-2, -2, 0, Ui.dp(this, 4), 0, 0));
            LinearLayout.LayoutParams tl = Ui.weight(1);
            tl.leftMargin = Ui.dp(this, 14);
            top.addView(texts, tl);
            row.addView(top);
            String plotText = info == null ? "" : info.optString("plot", "").trim();
            if (!plotText.isEmpty() && !plotText.equals("null")) {
                TextView pt = Ui.text(this, plotText, 13, Theme.TEXT_DIM);
                pt.setMaxLines(3);
                pt.setLineSpacing(0, 1.12f);
                pt.setEllipsize(android.text.TextUtils.TruncateAt.END);
                row.addView(pt, Ui.lp(-1, -2, 0, Ui.dp(this, 10), 0, 0));
            }
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { playEpisode(epId, false); }
            });
            LinearLayout.LayoutParams rl = Ui.lp(-1, -2);
            rl.setMargins(Ui.dp(this, 8), 0, Ui.dp(this, 8), Ui.dp(this, 4));
            episodes.addView(row, rl);
        }
    }

    /** "Você também pode gostar": other titles from the same category. */
    private void buildMore() {
        if (more.getChildCount() > 0) return;
        List<VodItem> same = new ArrayList<VodItem>();
        for (VodItem v : Index.category(item.series, item.categoryId)) {
            if (v != item) same.add(v);
            if (same.size() == 20) break;
        }
        if (same.isEmpty()) return;
        more.addView(Cards.sectionTitle(this, "Você também pode gostar", null));
        LinearLayout strip = Cards.strip(this, more);
        int w = Cards.rowPosterWidth(this);
        for (VodItem v : same) Cards.addToStrip(this, strip, Cards.poster(this, v, w, true));
    }

    // ---------------------------------------------------------------- play

    private void play(boolean fromStart) {
        if (item.series) {
            if (seasons.isEmpty()) {
                Ui.toast(this, InfoCache.peek(item) == null ? "Carregando episódios…" : "Nenhum episódio disponível");
                return;
            }
            JSONObject e = Repo.continueEntry(item.favKey());
            String epId = e != null ? e.optString("epId", null) : null;
            if (epId == null) epId = Meta.episodeId(seasons.firstEntry().getValue().get(0));
            playEpisode(epId, fromStart);
            return;
        }
        Repo.PlayItem it = new Repo.PlayItem();
        it.url = item.url != null ? item.url : Xtream.movieUrl(item.id, item.extension);
        it.title = item.cleanName();
        it.key = item.favKey();
        it.itemId = item.id;
        it.poster = item.poster;
        String y = Meta.year(answer, item);
        it.year = y.isEmpty() ? null : y;
        it.durationSecs = Meta.minutes(answer) * 60;
        List<Repo.PlayItem> q = new ArrayList<Repo.PlayItem>();
        q.add(it);
        if (fromStart) Repo.removeProgress(item.favKey());
        PlayerActivity.playVod(this, q, 0);
    }

    /** Queues every episode of every season (so "next" continues into the next season). */
    private void playEpisode(String epId, boolean fromStart) {
        List<Repo.PlayItem> q = new ArrayList<Repo.PlayItem>();
        int start = 0;
        String y = Meta.year(answer, item);
        for (Map.Entry<Integer, List<JSONObject>> s : seasons.entrySet()) {
            for (JSONObject ep : s.getValue()) {
                Repo.PlayItem it = new Repo.PlayItem();
                it.url = Meta.episodeUrl(ep);
                it.title = item.cleanName();
                it.key = item.favKey();
                it.itemId = item.id;
                it.poster = item.poster;
                it.year = y.isEmpty() ? null : y;
                it.season = s.getKey();
                it.episode = ep.optInt("episode_num", 1);
                it.episodeId = Meta.episodeId(ep);
                it.episodeTitle = Meta.episodeTitle(ep, item.cleanName());
                it.subtitle = "S" + it.season + " · E" + it.episode + " — " + it.episodeTitle;
                it.durationSecs = Meta.episodeSeconds(ep);
                if (it.episodeId.equals(epId)) start = q.size();
                q.add(it);
            }
        }
        if (q.isEmpty()) return;
        if (fromStart) Repo.clearEpisodeProgress(q.get(start).episodeId);
        PlayerActivity.playVod(this, q, start);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Back from the player: refresh "continue" and the episode progress bars.
        if (answer != null) {
            if (item.series && !seasons.isEmpty()) {
                JSONObject e = Repo.continueEntry(item.favKey());
                if (e != null && seasons.containsKey(e.optInt("season")) && e.optInt("season") != season) {
                    season = e.optInt("season");
                    buildSeasons();
                } else {
                    buildEpisodes();
                }
            }
            updateButtons();
        }
    }
}
