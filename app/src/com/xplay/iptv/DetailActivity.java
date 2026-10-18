package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

/** Movie / series page: poster, rating, genres, plot, play button, favorites and tabs. */
public class DetailActivity extends Activity {

    private static final String EXTRA_SERIES = "series";
    private static final String EXTRA_ID = "id";

    private VodItem item;
    private JSONObject info;
    /** Seasons in order; each holds its episodes. */
    private final List<String> seasonNames = new ArrayList<String>();
    private final List<List<JSONObject>> seasons = new ArrayList<List<JSONObject>>();
    private int season;
    private int tab;

    private TextView subtitle, meta, plot, more, playLabel, favLabel;
    private ImageView favIcon;
    private LinearLayout genres, tabsRow, tabContent, actions;
    private View playButton;
    private ProgressBar infoLoading;

    public static void start(Activity from, VodItem v) {
        Intent i = new Intent(from, DetailActivity.class);
        i.putExtra(EXTRA_SERIES, v.series);
        i.putExtra(EXTRA_ID, v.id);
        from.startActivity(i);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        item = Repo.findVod(getIntent().getBooleanExtra(EXTRA_SERIES, false), getIntent().getStringExtra(EXTRA_ID));
        if (item == null) {
            finish();
            return;
        }
        buildUi();
        loadInfo();
    }

    // ---------------------------------------------------------------- layout

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Palette.BLACK);
        LinearLayout c = Ui.column(this);
        int p = Ui.dp(this, 32);
        c.setPadding(p, Ui.dp(this, 8), p, p);
        scroll.addView(c);

        ImageView back = Ui.iconButton(this, R.drawable.ic_back, Palette.TEXT, Palette.GOLD);
        LinearLayout.LayoutParams bl = Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48));
        bl.leftMargin = -Ui.dp(this, 8);
        back.setContentDescription("Voltar");
        c.addView(back, bl);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        LinearLayout header = Ui.row(this);
        header.setGravity(Gravity.TOP);
        FrameLayout poster = new FrameLayout(this);
        float r = Ui.dp(this, 10);
        poster.setBackground(Ui.round(Palette.SURFACE_HI, r));
        Ui.clipRound(poster, r);
        ImageView img = new ImageView(this);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        ImageLoader.load(item.poster, img, Ui.dp(this, 170), 0);
        poster.addView(img, new FrameLayout.LayoutParams(-1, -1));
        header.addView(poster, Ui.lp(Ui.dp(this, 170), Ui.dp(this, 255)));

        LinearLayout side = Ui.column(this);
        side.setPadding(Ui.dp(this, 28), 0, 0, 0);
        TextView title = Ui.text(this, item.name, 24, Palette.TEXT, true);
        title.setMaxLines(3);
        title.setEllipsize(TextUtils.TruncateAt.END);
        side.addView(title);
        subtitle = Ui.text(this, "", 15, Palette.TEXT_DIM, false);
        side.addView(subtitle);
        if (item.ratingText() != null) {
            LinearLayout rating = Ui.row(this);
            rating.addView(Ui.icon(this, R.drawable.ic_star, Palette.GOLD), Ui.lp(Ui.dp(this, 20), Ui.dp(this, 20)));
            TextView rt = Ui.text(this, item.ratingText(), 17, Palette.GOLD, true);
            rt.setPadding(Ui.dp(this, 6), 0, 0, 0);
            rating.addView(rt);
            LinearLayout.LayoutParams rl = Ui.lp(-2, -2);
            rl.topMargin = Ui.dp(this, 6);
            side.addView(rating, rl);
        }
        meta = Ui.text(this, "", 14, 0xFFBBBBBB, false);
        LinearLayout.LayoutParams ml = Ui.lp(-1, -2);
        ml.topMargin = Ui.dp(this, 6);
        side.addView(meta, ml);
        HorizontalScrollView gs = new HorizontalScrollView(this);
        gs.setHorizontalScrollBarEnabled(false);
        genres = Ui.row(this);
        gs.addView(genres);
        LinearLayout.LayoutParams gl = Ui.lp(-1, -2);
        gl.topMargin = Ui.dp(this, 12);
        side.addView(gs, gl);
        infoLoading = new ProgressBar(this);
        infoLoading.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Palette.GOLD));
        side.addView(infoLoading, Ui.lp(Ui.dp(this, 28), Ui.dp(this, 28)));
        header.addView(side, Ui.weight(1));
        c.addView(header, Ui.lp(-1, -2));

        plot = Ui.text(this, "", 15, 0xFFBBBBBB, false);
        plot.setMaxLines(3);
        plot.setEllipsize(TextUtils.TruncateAt.END);
        plot.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams pl = Ui.lp(-1, -2);
        pl.topMargin = Ui.dp(this, 14);
        side.addView(plot, pl);
        more = Ui.text(this, "Saiba mais", 15, Palette.GOLD, true);
        more.setFocusable(true);
        more.setClickable(true);
        more.setVisibility(View.GONE);
        side.addView(more);
        more.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean expanded = plot.getMaxLines() > 3;
                plot.setMaxLines(expanded ? 3 : 100);
                more.setText(expanded ? "Saiba mais" : "Mostrar menos");
            }
        });

        FrameLayout play = new FrameLayout(this);
        play.setFocusable(true);
        play.setClickable(true);
        play.setBackground(Ui.selectable(this, Palette.GOLD, Palette.GOLD_LIGHT, Ui.dp(this, 28), Palette.TEXT));
        playLabel = Ui.text(this, item.series ? "CARREGANDO…" : "REPRODUZIR", 17, Palette.BLACK, true);
        play.addView(playLabel, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        LinearLayout.LayoutParams pb = Ui.lp(Ui.dp(this, 320), Ui.dp(this, 54));
        pb.topMargin = Ui.dp(this, 18);
        side.addView(play, pb);
        playButton = play;
        play.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { playMain(); }
        });
        play.requestFocus();

        actions = Ui.row(this);
        LinearLayout fav = action(R.drawable.ic_heart, "Favoritos");
        favIcon = (ImageView) fav.getChildAt(0);
        favLabel = (TextView) fav.getChildAt(1);
        fav.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean on = Repo.toggleFav(item.favKey());
                updateFav();
                Toast.makeText(DetailActivity.this, on ? "Adicionado aos favoritos" : "Removido dos favoritos",
                        Toast.LENGTH_SHORT).show();
            }
        });
        updateFav();
        LinearLayout.LayoutParams al = Ui.lp(-1, -2);
        al.topMargin = Ui.dp(this, 12);
        side.addView(actions, al);

        tabsRow = Ui.row(this);
        LinearLayout.LayoutParams tl = Ui.lp(-1, -2);
        tl.topMargin = Ui.dp(this, 20);
        c.addView(tabsRow, tl);
        View line = new View(this);
        line.setBackgroundColor(0xFF2C2C30);
        c.addView(line, Ui.lp(-1, 1));
        tabContent = Ui.column(this);
        LinearLayout.LayoutParams cl = Ui.lp(-1, -2);
        cl.topMargin = Ui.dp(this, 14);
        c.addView(tabContent, cl);

        setContentView(scroll);
        tab = 0;
        renderTabs();
    }

    private LinearLayout action(int icon, String label) {
        LinearLayout a = Ui.column(this);
        a.setGravity(Gravity.CENTER_HORIZONTAL);
        a.setFocusable(true);
        a.setClickable(true);
        a.setBackground(Ui.selectable(this, 0, 0x1FFFFFFF, Ui.dp(this, 12), Palette.GOLD));
        a.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6));
        ImageView i = Ui.icon(this, icon, Palette.TEXT);
        i.setBackground(Ui.round(Palette.SURFACE_HI, Ui.dp(this, 22)));
        int p = Ui.dp(this, 10);
        i.setPadding(p, p, p, p);
        a.addView(i, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        TextView t = Ui.text(this, label, 13, Palette.TEXT, false);
        t.setPadding(0, Ui.dp(this, 6), 0, 0);
        a.addView(t);
        LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
        lp.rightMargin = Ui.dp(this, 8);
        actions.addView(a, lp);
        return a;
    }

    private void updateFav() {
        boolean on = Repo.isFav(item.favKey());
        favIcon.setImageResource(on ? R.drawable.ic_heart_fill : R.drawable.ic_heart);
        favIcon.setColorFilter(on ? 0xFFFF5C7A : Palette.TEXT);
        favLabel.setText(on ? "Favorito" : "Favoritos");
    }

    // ---------------------------------------------------------------- data

    private void loadInfo() {
        if (!Repo.isXtream()) {
            // M3U titles have no info API: show what the playlist gave and its episodes.
            infoLoading.setVisibility(View.GONE);
            info = new JSONObject();
            if (item.series) loadM3uEpisodes();
            fillInfo();
            return;
        }
        Task.run(this, new Callable<JSONObject>() {
            public JSONObject call() throws Exception {
                return item.series ? Xtream.seriesInfo(item.id) : Xtream.vodInfo(item.id);
            }
        }, new Task.Done<JSONObject>() {
            public void ok(JSONObject o) {
                infoLoading.setVisibility(View.GONE);
                info = o.optJSONObject("info");
                if (info == null) info = new JSONObject();
                if (item.series) parseEpisodes(o);
                fillInfo();
            }

            public void fail(Exception e) {
                infoLoading.setVisibility(View.GONE);
                info = new JSONObject();
                plot.setText("Não foi possível carregar os detalhes: " + Task.message(e));
                if (item.series) playLabel.setText("TENTE NOVAMENTE");
            }
        });
    }

    private void parseEpisodes(JSONObject o) {
        seasonNames.clear();
        seasons.clear();
        Object eps = o.opt("episodes");
        List<String> keys = new ArrayList<String>();
        if (eps instanceof JSONObject) {
            Iterator<String> it = ((JSONObject) eps).keys();
            while (it.hasNext()) keys.add(it.next());
            Collections.sort(keys, new Comparator<String>() {
                public int compare(String a, String b) { return Integer.compare(num(a), num(b)); }
            });
            for (String k : keys) {
                JSONArray arr = ((JSONObject) eps).optJSONArray(k);
                addSeason("Temporada " + k, arr);
            }
        } else if (eps instanceof JSONArray) {
            // Some servers send a list of seasons instead of an object keyed by season.
            JSONArray outer = (JSONArray) eps;
            for (int i = 0; i < outer.length(); i++) addSeason("Temporada " + (i + 1), outer.optJSONArray(i));
        }
    }

    private void loadM3uEpisodes() {
        seasonNames.clear();
        seasons.clear();
        java.util.LinkedHashMap<Integer, List<JSONObject>> bySeason = M3uCatalog.EPISODES.get(item.id);
        if (bySeason == null) return;
        List<Integer> keys = new ArrayList<Integer>(bySeason.keySet());
        Collections.sort(keys);
        for (Integer k : keys) {
            seasonNames.add("Temporada " + k);
            seasons.add(bySeason.get(k));
        }
    }

    private void addSeason(String name, JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        List<JSONObject> list = new ArrayList<JSONObject>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.optJSONObject(i);
            if (e != null) list.add(e);
        }
        seasonNames.add(name);
        seasons.add(list);
    }

    private static int num(String s) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return 9999;
        }
    }

    private void fillInfo() {
        String original = str(info, "o_name");
        String genre = str(info, "genre");
        subtitle.setText(!original.isEmpty() && !original.equalsIgnoreCase(item.name) ? original : "");
        subtitle.setVisibility(subtitle.getText().length() == 0 ? View.GONE : View.VISIBLE);

        List<String> parts = new ArrayList<String>();
        String date = first(info, "releasedate", "release_date", "releaseDate");
        if (date.length() >= 4) parts.add(date.substring(0, 4));
        String duration = durationText();
        if (!duration.isEmpty()) parts.add(duration);
        String country = str(info, "country");
        if (!country.isEmpty()) parts.add(country);
        if (item.series && !seasons.isEmpty()) {
            parts.add(seasons.size() + (seasons.size() == 1 ? " temporada" : " temporadas"));
        }
        meta.setText(TextUtils.join("   ", parts));

        genres.removeAllViews();
        if (!genre.isEmpty()) {
            for (String g : genre.split("[,/]")) {
                String t = g.trim();
                if (t.isEmpty()) continue;
                TextView chip = Ui.text(this, t, 14, Palette.TEXT, false);
                chip.setBackground(Ui.round(Palette.SURFACE_HI, Ui.dp(this, 8)));
                chip.setPadding(Ui.dp(this, 14), Ui.dp(this, 7), Ui.dp(this, 14), Ui.dp(this, 7));
                LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
                lp.rightMargin = Ui.dp(this, 8);
                genres.addView(chip, lp);
            }
        }

        String text = first(info, "plot", "description");
        plot.setText(text.isEmpty() ? "Sem sinopse disponível." : text);
        plot.post(new Runnable() {
            public void run() {
                android.text.Layout l = plot.getLayout();
                boolean cut = l != null && l.getLineCount() > 0 && l.getEllipsisCount(l.getLineCount() - 1) > 0;
                more.setVisibility(cut ? View.VISIBLE : View.GONE);
            }
        });

        final String trailer = str(info, "youtube_trailer");
        if (!trailer.isEmpty()) {
            LinearLayout t = action(R.drawable.ic_play, "Trailer");
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String url = trailer.startsWith("http") ? trailer : "https://www.youtube.com/watch?v=" + trailer;
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception e) {
                        Toast.makeText(DetailActivity.this, "Nenhum app para abrir o trailer", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        if (item.series) {
            if (seasons.isEmpty()) playLabel.setText("SEM EPISÓDIOS");
            else playLabel.setText("REPRODUZIR  T" + seasonNumber(0) + ":E" + episodeNumber(seasons.get(0).get(0), 0));
        }
        renderTabs();
    }

    private String durationText() {
        int secs = info.optInt("duration_secs", 0);
        if (secs <= 0 && item.series) return "";
        if (secs <= 0) {
            String d = str(info, "duration");
            String[] hms = d.split(":");
            if (hms.length == 3) {
                secs = num(hms[0]) * 3600 + num(hms[1]) * 60 + num(hms[2]);
                if (secs >= 9999) secs = 0;
            }
        }
        return secs > 0 ? (secs / 60) + " min" : "";
    }

    private String seasonNumber(int index) {
        return seasonNames.get(index).replace("Temporada ", "");
    }

    private static String episodeNumber(JSONObject e, int index) {
        String n = e.optString("episode_num", "");
        return n.isEmpty() ? String.valueOf(index + 1) : n;
    }

    private static String str(JSONObject o, String key) {
        if (o == null) return "";
        Object v = o.opt(key);
        if (v == null || v == JSONObject.NULL) return "";
        if (v instanceof JSONArray) {
            JSONArray a = (JSONArray) v;
            return a.length() > 0 ? a.optString(0, "") : "";
        }
        String s = String.valueOf(v).trim();
        return s.equals("null") ? "" : s;
    }

    private static String first(JSONObject o, String... keys) {
        for (String k : keys) {
            String s = str(o, k);
            if (!s.isEmpty()) return s;
        }
        return "";
    }

    // ---------------------------------------------------------------- playback

    private void playMain() {
        if (!item.series) {
            try {
                play(item.url != null ? item.url : Xtream.movieUrl(item.id, item.extension), item.name, null);
            } catch (Exception e) {
                Toast.makeText(this, Task.message(e), Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (info == null) return;
        if (seasons.isEmpty()) {
            loadInfo();
            return;
        }
        playEpisode(0, 0);
    }

    private void playEpisode(int seasonIndex, int episodeIndex) {
        JSONObject e = seasons.get(seasonIndex).get(episodeIndex);
        try {
            String url = e.has("url") ? e.optString("url")
                    : Xtream.episodeUrl(e.optString("id"), e.optString("container_extension", "mp4"));
            String sub = "T" + seasonNumber(seasonIndex) + ":E" + episodeNumber(e, episodeIndex)
                    + "  " + str(e, "title");
            play(url, item.name, sub);
        } catch (Exception ex) {
            Toast.makeText(this, Task.message(ex), Toast.LENGTH_SHORT).show();
        }
    }

    private void play(String url, String title, String subtitle) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra(PlayerActivity.EXTRA_URL, url);
        i.putExtra(PlayerActivity.EXTRA_TITLE, title);
        if (subtitle != null) i.putExtra(PlayerActivity.EXTRA_SUBTITLE, subtitle);
        i.putExtra(PlayerActivity.EXTRA_KEY, item.favKey());
        startActivity(i);
    }

    // ---------------------------------------------------------------- tabs

    private List<String> tabNames() {
        List<String> t = new ArrayList<String>();
        if (item.series) t.add("Episódios");
        t.add("Similares");
        t.add("Elenco & Produção");
        return t;
    }

    private void renderTabs() {
        tabsRow.removeAllViews();
        List<String> names = tabNames();
        for (int i = 0; i < names.size(); i++) {
            final int which = i;
            LinearLayout t = Ui.column(this);
            t.setFocusable(true);
            t.setClickable(true);
            t.setBackground(Ui.selectable(this, 0, 0x1FFFFFFF, Ui.dp(this, 8), 0));
            TextView label = Ui.text(this, names.get(i), 17, i == tab ? Palette.TEXT : Palette.TEXT_DIM, i == tab);
            label.setPadding(Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4), Ui.dp(this, 10));
            t.addView(label);
            View bar = new View(this);
            bar.setBackgroundColor(i == tab ? Palette.GOLD : 0);
            t.addView(bar, Ui.lp(-1, Ui.dp(this, 3)));
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.rightMargin = Ui.dp(this, 22);
            tabsRow.addView(t, lp);
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    tab = which;
                    renderTabs();
                }
            });
        }
        tabContent.removeAllViews();
        String current = names.get(tab);
        if (current.equals("Episódios")) renderEpisodes();
        else if (current.equals("Similares")) renderSimilar();
        else renderCredits();
    }

    private void renderEpisodes() {
        if (info == null) {
            tabContent.addView(Ui.text(this, "Carregando episódios…", 15, Palette.TEXT_DIM, false));
            return;
        }
        if (seasons.isEmpty()) {
            tabContent.addView(Ui.text(this, "Nenhum episódio disponível.", 15, Palette.TEXT_DIM, false));
            return;
        }
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = Ui.row(this);
        hs.addView(chips);
        for (int i = 0; i < seasonNames.size(); i++) {
            final int which = i;
            boolean on = i == season;
            TextView chip = Ui.text(this, seasonNames.get(i), 15, on ? Palette.BLACK : Palette.TEXT, true);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setBackground(Ui.selectable(this, on ? Palette.GOLD : Palette.SURFACE_HI, on ? Palette.GOLD_LIGHT : 0xFF4A4A4E,
                    Ui.dp(this, 20), Palette.TEXT));
            chip.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 8));
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.rightMargin = Ui.dp(this, 8);
            chips.addView(chip, lp);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    season = which;
                    renderTabs();
                }
            });
        }
        tabContent.addView(hs, Ui.lp(-1, -2));

        List<JSONObject> eps = seasons.get(season);
        for (int i = 0; i < eps.size(); i++) {
            final int index = i;
            JSONObject e = eps.get(i);
            JSONObject ei = e.optJSONObject("info");
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.TOP);
            row.setFocusable(true);
            row.setClickable(true);
            row.setBackground(Ui.selectable(this, 0, 0x1FFFFFFF, Ui.dp(this, 10), Palette.GOLD));
            int p = Ui.dp(this, 6);
            row.setPadding(p, p, p, p);

            FrameLayout thumb = new FrameLayout(this);
            float r = Ui.dp(this, 8);
            thumb.setBackground(Ui.round(Palette.SURFACE_HI, r));
            Ui.clipRound(thumb, r);
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            String still = Xtream.httpOrNull(str(ei, "movie_image"));
            ImageLoader.load(still != null ? still : item.poster, img, Ui.dp(this, 128), 0);
            thumb.addView(img, new FrameLayout.LayoutParams(-1, -1));
            ImageView playIcon = Ui.icon(this, R.drawable.ic_play, Palette.TEXT);
            playIcon.setBackground(Ui.round(0x99000000, Ui.dp(this, 16)));
            int ip = Ui.dp(this, 4);
            playIcon.setPadding(ip, ip, ip, ip);
            thumb.addView(playIcon, new FrameLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32), Gravity.CENTER));
            row.addView(thumb, Ui.lp(Ui.dp(this, 128), Ui.dp(this, 72)));

            LinearLayout text = Ui.column(this);
            text.setPadding(Ui.dp(this, 12), 0, 0, 0);
            String title = str(e, "title");
            TextView t = Ui.text(this, episodeNumber(e, i) + ". " + (title.isEmpty() ? "Episódio" : title), 15,
                    Palette.TEXT, true);
            t.setMaxLines(2);
            t.setEllipsize(TextUtils.TruncateAt.END);
            text.addView(t);
            String dur = str(ei, "duration");
            if (!dur.isEmpty()) text.addView(Ui.text(this, dur, 13, Palette.TEXT_DIM, false));
            String desc = str(ei, "plot");
            if (!desc.isEmpty()) {
                TextView d = Ui.text(this, desc, 13, 0xFF9E9EA3, false);
                d.setMaxLines(2);
                d.setEllipsize(TextUtils.TruncateAt.END);
                text.addView(d);
            }
            row.addView(text, Ui.weight(1));
            LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
            lp.topMargin = Ui.dp(this, 10);
            tabContent.addView(row, lp);
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { playEpisode(season, index); }
            });
        }
    }

    private void renderSimilar() {
        List<VodItem> pool = item.series ? Repo.series : Repo.movies;
        List<VodItem> similar = new ArrayList<VodItem>();
        if (pool != null) {
            for (VodItem v : pool) {
                if (v != item && v.categoryId.equals(item.categoryId)) similar.add(v);
                if (similar.size() >= 12) break;
            }
        }
        if (similar.isEmpty()) {
            tabContent.addView(Ui.text(this, "Nada parecido por aqui ainda.", 15, Palette.TEXT_DIM, false));
            return;
        }
        int width = getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 40);
        int columns = Math.max(2, width / Ui.dp(this, Ui.posterDp(this) + 16));
        int cardDp = (int) ((width / (float) columns) / getResources().getDisplayMetrics().density) - 10;
        LinearLayout row = null;
        for (int i = 0; i < similar.size(); i++) {
            if (i % columns == 0) {
                row = Ui.row(this);
                row.setGravity(Gravity.TOP);
                LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
                lp.bottomMargin = Ui.dp(this, 10);
                tabContent.addView(row, lp);
            }
            row.addView(Posters.card(this, similar.get(i), cardDp), Ui.weight(1));
        }
        // Keep the last row's cards the same width as the others.
        while (row != null && row.getChildCount() < columns) row.addView(new View(this), Ui.spacer());
    }

    private void renderCredits() {
        if (info == null) {
            tabContent.addView(Ui.text(this, "Carregando…", 15, Palette.TEXT_DIM, false));
            return;
        }
        boolean any = false;
        any |= credit("Elenco", first(info, "cast", "actors"));
        any |= credit("Direção", str(info, "director"));
        any |= credit("Gênero", str(info, "genre"));
        any |= credit("Lançamento", first(info, "releasedate", "release_date", "releaseDate"));
        any |= credit("País", str(info, "country"));
        if (!any) tabContent.addView(Ui.text(this, "Sem informações de elenco.", 15, Palette.TEXT_DIM, false));
    }

    private boolean credit(String label, String value) {
        if (value.isEmpty()) return false;
        tabContent.addView(Ui.text(this, label, 14, Palette.TEXT_DIM, true));
        TextView v = Ui.text(this, value, 15, Palette.TEXT, false);
        v.setLineSpacing(0, 1.1f);
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = Ui.dp(this, 2);
        lp.bottomMargin = Ui.dp(this, 14);
        tabContent.addView(v, lp);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (favIcon != null) updateFav();
    }
}
