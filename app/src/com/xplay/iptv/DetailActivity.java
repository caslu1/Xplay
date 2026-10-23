package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
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

/**
 * Movie / series page: a hero with poster, info and actions over a blurred backdrop, then the
 * seasons and episodes (series) and related titles, stacked for easy remote navigation.
 */
public class DetailActivity extends Activity {

    private static final String EXTRA_SERIES = "series";
    private static final String EXTRA_ID = "id";

    private VodItem item;
    private JSONObject info;
    private final List<String> seasonNames = new ArrayList<String>();
    private final List<List<JSONObject>> seasons = new ArrayList<List<JSONObject>>();
    private int season;

    private ImageView backdrop;
    private FrameLayout providerMark;
    private TextView subtitle, meta, plot, more, playLabel;
    private LinearLayout genres, actions, seasonChips, episodeStrip;
    private TextView episodesState;
    private LinearLayout favButton;
    private View trailerButton;
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

    @Override
    protected void onResume() {
        super.onResume();
        if (playLabel == null) return;
        updateFav();
        updatePlayLabel();
        if (item.series && info != null && !seasons.isEmpty()) renderEpisodes();
    }

    // ---------------------------------------------------------------- layout

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Palette.BLACK);
        scroll.setFillViewport(true);
        LinearLayout page = Ui.column(this);
        scroll.addView(page);

        page.addView(buildHero(), Ui.lp(-1, -2));

        int p = Ui.dp(this, 32);
        LinearLayout body = Ui.column(this);
        body.setPadding(p, 0, p, p);
        page.addView(body, Ui.lp(-1, -2));

        if (item.series) {
            body.addView(sectionTitle("Episódios"));
            HorizontalScrollView chipScroll = new HorizontalScrollView(this);
            chipScroll.setHorizontalScrollBarEnabled(false);
            seasonChips = Ui.row(this);
            chipScroll.addView(seasonChips);
            body.addView(chipScroll, Ui.lp(-1, -2));
            episodesState = Ui.text(this, "Carregando episódios…", 15, Palette.TEXT_DIM, false);
            episodesState.setPadding(0, Ui.dp(this, 12), 0, 0);
            body.addView(episodesState);
            HorizontalScrollView epScroll = new HorizontalScrollView(this);
            epScroll.setHorizontalScrollBarEnabled(false);
            episodeStrip = Ui.row(this);
            episodeStrip.setGravity(Gravity.TOP);
            episodeStrip.setPadding(0, Ui.dp(this, 12), Ui.dp(this, 16), 0);
            epScroll.addView(episodeStrip);
            body.addView(epScroll, Ui.lp(-1, -2));
        }

        List<VodItem> related = related();
        if (!related.isEmpty()) {
            body.addView(sectionTitle("Relacionados"));
            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout strip = Ui.row(this);
            strip.setGravity(Gravity.TOP);
            strip.setPadding(Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            hs.addView(strip);
            int w = Ui.posterDp(this);
            for (VodItem v : related) {
                LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
                lp.rightMargin = Ui.dp(this, 8);
                strip.addView(Posters.card(this, v, w), lp);
            }
            body.addView(hs, Ui.lp(-1, -2));
        }


        setContentView(scroll);
    }

    private View buildHero() {
        FrameLayout hero = new FrameLayout(this);
        backdrop = new ImageView(this);
        backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        backdrop.setAlpha(0.7f);
        ImageLoader.loadBlurred(item.poster, backdrop);
        hero.addView(backdrop, new FrameLayout.LayoutParams(-1, -1));
        View shadeLeft = new View(this);
        shadeLeft.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0xF5000000, 0xCC000000, 0x33000000, 0x00000000}));
        hero.addView(shadeLeft, new FrameLayout.LayoutParams(-1, -1));
        View shadeBottom = new View(this);
        shadeBottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {Palette.BLACK, 0x00000000}));
        hero.addView(shadeBottom, new FrameLayout.LayoutParams(-1, Ui.dp(this, 120), Gravity.BOTTOM));
        // Tall enough for the backdrop to read as a full-screen image, like ARVIO.
        hero.setMinimumHeight((int) (getResources().getDisplayMetrics().heightPixels * 0.82f));

        LinearLayout content = Ui.column(this);
        content.setPadding(Ui.dp(this, 20), Ui.dp(this, 10), Ui.dp(this, 32), Ui.dp(this, 20));
        hero.addView(content, new FrameLayout.LayoutParams(-1, -2));

        ImageView back = Ui.iconButton(this, R.drawable.ic_back, Palette.TEXT, Palette.GOLD);
        back.setContentDescription("Voltar");
        content.addView(back, Ui.lp(Ui.dp(this, 46), Ui.dp(this, 46)));
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });

        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.TOP);
        row.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), 0, 0);
        content.addView(row, Ui.lp(-1, -2));

        LinearLayout side = Ui.column(this);
        side.setPadding(Ui.dp(this, 12), Ui.dp(this, 24), 0, 0);
        row.addView(side, Ui.lp(Math.min(Ui.dp(this, 720), (int) (getResources().getDisplayMetrics().widthPixels * 0.62f)), -2));

        providerMark = new FrameLayout(this);
        LinearLayout.LayoutParams pml = Ui.lp(-2, -2);
        pml.bottomMargin = Ui.dp(this, 8);
        side.addView(providerMark, pml);
        TextView title = Ui.text(this, item.name, 38, Palette.TEXT, true);
        title.setShadowLayer(10, 0, 2, Palette.BLACK);
        title.setMaxLines(2);
        title.setEllipsize(TextUtils.TruncateAt.END);
        side.addView(title);
        subtitle = Ui.text(this, "", 15, Palette.TEXT_DIM, false);
        subtitle.setVisibility(View.GONE);
        side.addView(subtitle);

        LinearLayout metaRow = Ui.row(this);
        if (item.ratingText() != null) {
            TextView rt = Ui.text(this, "★ " + item.ratingText(), 14, Palette.BLACK, true);
            rt.setBackground(Ui.round(Palette.GOLD, Ui.dp(this, 4)));
            rt.setPadding(Ui.dp(this, 7), Ui.dp(this, 1), Ui.dp(this, 7), Ui.dp(this, 1));
            LinearLayout.LayoutParams rtl = Ui.lp(-2, -2);
            rtl.rightMargin = Ui.dp(this, 14);
            metaRow.addView(rt, rtl);
        }
        meta = Ui.text(this, "", 14, 0xFFCCCCCC, false);
        metaRow.addView(meta);
        infoLoading = new ProgressBar(this);
        infoLoading.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Palette.GOLD));
        LinearLayout.LayoutParams il = Ui.lp(Ui.dp(this, 20), Ui.dp(this, 20));
        il.leftMargin = Ui.dp(this, 12);
        metaRow.addView(infoLoading, il);
        LinearLayout.LayoutParams ml = Ui.lp(-1, -2);
        ml.topMargin = Ui.dp(this, 8);
        side.addView(metaRow, ml);

        HorizontalScrollView gs = new HorizontalScrollView(this);
        gs.setHorizontalScrollBarEnabled(false);
        genres = Ui.row(this);
        gs.addView(genres);
        LinearLayout.LayoutParams gl = Ui.lp(-1, -2);
        gl.topMargin = Ui.dp(this, 10);
        side.addView(gs, gl);

        plot = Ui.text(this, "", 15, 0xFFCFCFCF, false);
        plot.setMaxLines(3);
        plot.setEllipsize(TextUtils.TruncateAt.END);
        plot.setLineSpacing(0, 1.15f);
        LinearLayout.LayoutParams pl = Ui.lp(-1, -2);
        pl.topMargin = Ui.dp(this, 12);
        side.addView(plot, pl);
        more = Ui.text(this, "Saiba mais", 14, Palette.GOLD, true);
        more.setFocusable(true);
        more.setClickable(true);
        more.setPadding(0, Ui.dp(this, 4), Ui.dp(this, 8), Ui.dp(this, 4));
        more.setBackground(Ui.selectable(this, 0, 0x22FFFFFF, Ui.dp(this, 8), Palette.GOLD));
        more.setVisibility(View.GONE);
        side.addView(more);
        more.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean expanded = plot.getMaxLines() > 3;
                plot.setMaxLines(expanded ? 3 : 100);
                more.setText(expanded ? "Saiba mais" : "Mostrar menos");
            }
        });

        actions = Ui.row(this);
        LinearLayout.LayoutParams al = Ui.lp(-1, -2);
        al.topMargin = Ui.dp(this, 16);
        side.addView(actions, al);

        LinearLayout play = pill(R.drawable.ic_play, item.series ? "CARREGANDO…" : "REPRODUZIR", true);
        playLabel = (TextView) play.getChildAt(1);
        play.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { playMain(); }
        });
        favButton = pill(R.drawable.ic_heart, "Favoritos", false);
        favButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean on = Repo.toggleFav(item.favKey());
                updateFav();
                Toast.makeText(DetailActivity.this, on ? "Adicionado aos favoritos" : "Removido dos favoritos",
                        Toast.LENGTH_SHORT).show();
            }
        });
        updateFav();
        updatePlayLabel();
        play.requestFocus();
        return hero;
    }

    private LinearLayout pill(int icon, String label, boolean primary) {
        LinearLayout b = Ui.row(this);
        b.setFocusable(true);
        b.setClickable(true);
        float r = Ui.dp(this, 26);
        b.setBackground(primary
                ? Ui.selectable(this, Palette.TEXT, Palette.GOLD_LIGHT, r, Palette.GOLD)
                : Ui.selectable(this, 0x26FFFFFF, 0x40FFFFFF, r, Palette.GOLD));
        b.setPadding(Ui.dp(this, 22), Ui.dp(this, 13), Ui.dp(this, 26), Ui.dp(this, 13));
        int color = primary ? Palette.BLACK : Palette.TEXT;
        b.addView(Ui.icon(this, icon, color), Ui.lp(Ui.dp(this, 20), Ui.dp(this, 20)));
        TextView t = Ui.text(this, label, 15, color, true);
        t.setSingleLine(true);
        t.setPadding(Ui.dp(this, 8), 0, 0, 0);
        b.addView(t);
        LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
        lp.rightMargin = Ui.dp(this, 10);
        actions.addView(b, lp);
        return b;
    }

    private TextView sectionTitle(String text) {
        TextView t = Ui.text(this, text, 20, Palette.TEXT, true);
        t.setPadding(0, Ui.dp(this, 22), 0, Ui.dp(this, 10));
        return t;
    }

    private void updateFav() {
        boolean on = Repo.isFav(item.favKey());
        ImageView icon = (ImageView) favButton.getChildAt(0);
        icon.setImageResource(on ? R.drawable.ic_heart_fill : R.drawable.ic_heart);
        icon.setColorFilter(on ? 0xFFFF5C7A : Palette.TEXT);
        ((TextView) favButton.getChildAt(1)).setText(on ? "Favorito" : "Favoritos");
    }

    /** "Continuar" when there is saved progress, otherwise play from the start / first episode. */
    private void updatePlayLabel() {
        JSONObject resume = resumeEntry();
        if (resume != null) {
            String sub = resume.optString("sub", "");
            int gap = sub.indexOf("  ");
            String episode = gap > 0 ? sub.substring(0, gap) : sub;
            playLabel.setText("CONTINUAR" + (item.series && !episode.isEmpty() ? "  " + episode : ""));
        } else if (item.series) {
            if (info == null) playLabel.setText("CARREGANDO…");
            else if (seasons.isEmpty()) playLabel.setText("SEM EPISÓDIOS");
            else playLabel.setText("ASSISTIR  T" + seasonNumber(0) + ":E" + episodeNumber(seasons.get(0).get(0), 0));
        } else {
            playLabel.setText("REPRODUZIR");
        }
    }

    private JSONObject resumeEntry() {
        for (JSONObject e : Repo.continueList(item.series)) {
            if (item.favKey().equals(e.optString("key"))) return e;
        }
        return null;
    }

    /** Same category first; filled up with the best-rated titles when the category is small. */
    private List<VodItem> related() {
        List<VodItem> pool = Adult.filter(item.series ? Repo.series : Repo.movies);
        List<VodItem> out = new ArrayList<VodItem>();
        if (pool == null) return out;
        for (VodItem v : pool) {
            if (v != item && v.categoryId != null && v.categoryId.equals(item.categoryId)) out.add(v);
            if (out.size() >= 20) return out;
        }
        if (out.size() < 8) {
            List<VodItem> top = new ArrayList<VodItem>(pool);
            Collections.sort(top, new Comparator<VodItem>() {
                public int compare(VodItem a, VodItem b) { return Double.compare(b.rating, a.rating); }
            });
            for (VodItem v : top) {
                if (out.size() >= 20) break;
                if (v != item && !out.contains(v)) out.add(v);
            }
        }
        return out;
    }

    // ---------------------------------------------------------------- data

    private void loadInfo() {
        if (!Repo.isXtream()) {
            // M3U titles have no info API: show what the playlist gave and its episodes.
            info = new JSONObject();
            if (item.series) loadM3uEpisodes();
            fillInfo();
            return;
        }
        // Shares the answer with the movies/series screen, so a title seen in the hero opens instantly.
        InfoCache.get(item, new InfoCache.Callback() {
            public void done(JSONObject o) {
                if (isFinishing()) return;
                info = o.optJSONObject("info");
                boolean failed = info == null && o.length() == 0;
                if (info == null) info = new JSONObject();
                if (item.series) parseEpisodes(o);
                fillInfo();
                if (failed) plot.setText("Não foi possível carregar os detalhes.");
            }
        });
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

    private void parseEpisodes(JSONObject o) {
        seasonNames.clear();
        seasons.clear();
        Object eps = o.opt("episodes");
        if (eps instanceof JSONObject) {
            List<String> keys = new ArrayList<String>();
            Iterator<String> it = ((JSONObject) eps).keys();
            while (it.hasNext()) keys.add(it.next());
            Collections.sort(keys, new Comparator<String>() {
                public int compare(String a, String b) { return Integer.compare(num(a), num(b)); }
            });
            for (String k : keys) addSeason("Temporada " + k, ((JSONObject) eps).optJSONArray(k));
        } else if (eps instanceof JSONArray) {
            // Some servers send a list of seasons instead of an object keyed by season.
            JSONArray outer = (JSONArray) eps;
            for (int i = 0; i < outer.length(); i++) addSeason("Temporada " + (i + 1), outer.optJSONArray(i));
        }
    }

    private void addSeason(String name, JSONArray arr) {
        if (arr == null || arr.length() == 0) return;
        List<JSONObject> list = new ArrayList<JSONObject>();
        for (int i = 0; i < arr.length(); i++) {
            JSONObject e = arr.optJSONObject(i);
            if (e != null) list.add(e);
        }
        Collections.sort(list, new Comparator<JSONObject>() {
            public int compare(JSONObject a, JSONObject b) {
                return Integer.compare(num(a.optString("episode_num", "")), num(b.optString("episode_num", "")));
            }
        });
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
        infoLoading.setVisibility(View.GONE);
        String original = str(info, "o_name");
        if (!original.isEmpty() && !original.equalsIgnoreCase(item.name)) {
            subtitle.setText(original);
            subtitle.setVisibility(View.VISIBLE);
        }

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
        meta.setText(TextUtils.join("  •  ", parts));

        genres.removeAllViews();
        String genre = str(info, "genre");
        java.util.Map<String, String> cats = item.series ? Repo.seriesCategories : Repo.movieCategories;
        if (genre.isEmpty() && cats != null && item.categoryId != null && cats.get(item.categoryId) != null) {
            genre = cats.get(item.categoryId);
        }
        for (String g : genre.split("[,/]")) {
            String t = g.trim();
            if (t.isEmpty()) continue;
            TextView chip = Ui.text(this, t, 13, Palette.TEXT, false);
            chip.setBackground(Ui.stroke(0x33000000, Ui.dp(this, 8), Ui.dp(this, 1), 0x55FFFFFF));
            chip.setPadding(Ui.dp(this, 12), Ui.dp(this, 5), Ui.dp(this, 12), Ui.dp(this, 5));
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.rightMargin = Ui.dp(this, 8);
            genres.addView(chip, lp);
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

        String back = Xtream.httpOrNull(str(info, "backdrop_path"));
        if (back != null) {
            backdrop.setAlpha(0.9f);
            ImageLoader.load(back, backdrop, getResources().getDisplayMetrics().widthPixels / 2, 0);
        }

        final String trailer = str(info, "youtube_trailer");
        if (!trailer.isEmpty() && trailerButton == null) {
            LinearLayout t = pill(R.drawable.ic_movie, "Trailer", false);
            trailerButton = t;
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

        updatePlayLabel();
        if (item.series) renderSeasons();
        final boolean hasBackdrop = back != null;
        final boolean hasPlot = !text.isEmpty();
        Tmdb.get(item, InfoCache.peek(item), new Tmdb.Callback() {
            public void done(Tmdb.Details d) {
                if (d == null || isFinishing()) return;
                View mark = MetaChips.providerMark(DetailActivity.this, d.provider, d.providerLogo);
                if (mark != null) providerMark.addView(mark);
                if (!hasPlot && d.overview != null && !d.overview.isEmpty()) plot.setText(d.overview);
                if (!hasBackdrop && d.backdrop != null) {
                    backdrop.setAlpha(0.9f);
                    ImageLoader.load(d.backdrop, backdrop, getResources().getDisplayMetrics().widthPixels / 2, 0);
                }
            }
        });
    }

    private String durationText() {
        int secs = info.optInt("duration_secs", 0);
        if (secs <= 0 && item.series) return "";
        if (secs <= 0) {
            String[] hms = str(info, "duration").split(":");
            if (hms.length == 3) {
                secs = num(hms[0]) * 3600 + num(hms[1]) * 60 + num(hms[2]);
                if (secs >= 9999 * 60) secs = 0;
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

    // ---------------------------------------------------------------- seasons and episodes

    private void renderSeasons() {
        seasonChips.removeAllViews();
        if (seasons.isEmpty()) {
            episodesState.setText("Nenhum episódio disponível.");
            episodesState.setVisibility(View.VISIBLE);
            episodeStrip.removeAllViews();
            return;
        }
        episodesState.setVisibility(View.GONE);
        for (int i = 0; i < seasonNames.size(); i++) {
            final int which = i;
            boolean on = i == season;
            TextView chip = Ui.text(this, seasonNames.get(i), 14, on ? Palette.BLACK : Palette.TEXT, true);
            chip.setSingleLine(true);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setBackground(Ui.selectable(this, on ? Palette.GOLD : Palette.SURFACE_HI,
                    on ? Palette.GOLD_LIGHT : 0xFF2C2C2C, Ui.dp(this, 18), Palette.GOLD));
            chip.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.rightMargin = Ui.dp(this, 8);
            seasonChips.addView(chip, lp);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (season == which) return;
                    season = which;
                    renderSeasons();
                    // Keep the remote on the season row after it is rebuilt.
                    seasonChips.getChildAt(which).requestFocus();
                }
            });
        }
        renderEpisodes();
    }

    private void renderEpisodes() {
        episodeStrip.removeAllViews();
        ((HorizontalScrollView) episodeStrip.getParent()).scrollTo(0, 0);
        JSONObject resume = resumeEntry();
        String resumeUrl = resume == null ? null : resume.optString("url");
        List<JSONObject> eps = seasons.get(season);
        int w = Ui.dp(this, 220);
        for (int i = 0; i < eps.size(); i++) {
            final int index = i;
            JSONObject e = eps.get(i);
            JSONObject ei = e.optJSONObject("info");

            LinearLayout card = Ui.column(this);
            card.setFocusable(true);
            card.setClickable(true);
            int p = Ui.dp(this, 4);
            card.setPadding(p, p, p, p);
            card.setBackground(Ui.selectable(this, 0, 0x1FFFFFFF, Ui.dp(this, 12), Palette.GOLD));

            FrameLayout thumb = new FrameLayout(this);
            float r = Ui.dp(this, 10);
            thumb.setBackground(Ui.round(Palette.SURFACE_HI, r));
            Ui.clipRound(thumb, r);
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            String still = Xtream.httpOrNull(str(ei, "movie_image"));
            ImageLoader.load(still != null ? still : item.poster, img, w, 0);
            thumb.addView(img, new FrameLayout.LayoutParams(-1, -1));
            View shade = new View(this);
            shade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[] {0xB3000000, 0x00000000}));
            thumb.addView(shade, new FrameLayout.LayoutParams(-1, Ui.dp(this, 50), Gravity.BOTTOM));
            TextView number = Ui.text(this, "E" + episodeNumber(e, i), 13, Palette.TEXT, true);
            number.setBackground(Ui.round(0xCC000000, Ui.dp(this, 6)));
            number.setPadding(Ui.dp(this, 7), Ui.dp(this, 2), Ui.dp(this, 7), Ui.dp(this, 2));
            FrameLayout.LayoutParams nl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
            nl.setMargins(Ui.dp(this, 8), Ui.dp(this, 8), 0, 0);
            thumb.addView(number, nl);
            ImageView playIcon = Ui.icon(this, R.drawable.ic_play, Palette.BLACK);
            playIcon.setBackground(Ui.round(Palette.GOLD, Ui.dp(this, 16)));
            int ip = Ui.dp(this, 6);
            playIcon.setPadding(ip, ip, ip, ip);
            thumb.addView(playIcon, new FrameLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32), Gravity.CENTER));
            String url = episodeUrl(e);
            if (url != null && url.equals(resumeUrl)) {
                FrameLayout track = new FrameLayout(this);
                track.setBackgroundColor(0x66FFFFFF);
                View bar = new View(this);
                bar.setBackgroundColor(Palette.GOLD);
                int dur = Math.max(1, resume.optInt("dur", 1));
                track.addView(bar, new FrameLayout.LayoutParams(w * Math.min(dur, resume.optInt("pos")) / dur, -1));
                thumb.addView(track, new FrameLayout.LayoutParams(-1, Ui.dp(this, 4), Gravity.BOTTOM));
            }
            card.addView(thumb, Ui.lp(w, w * 9 / 16));

            String title = str(e, "title");
            TextView t = Ui.text(this, title.isEmpty() ? "Episódio " + episodeNumber(e, i) : title, 14, Palette.TEXT, true);
            t.setSingleLine(true);
            t.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tl = Ui.lp(w, -2);
            tl.topMargin = Ui.dp(this, 8);
            card.addView(t, tl);
            String dur = str(ei, "duration");
            String desc = str(ei, "plot");
            String line = dur.isEmpty() ? desc : (desc.isEmpty() ? dur : dur + "  •  " + desc);
            if (!line.isEmpty()) {
                TextView d = Ui.text(this, line, 12, Palette.TEXT_DIM, false);
                d.setMaxLines(2);
                d.setEllipsize(TextUtils.TruncateAt.END);
                card.addView(d, Ui.lp(w, -2));
            }
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.rightMargin = Ui.dp(this, 10);
            episodeStrip.addView(card, lp);
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { playEpisode(season, index); }
            });
        }
    }

    // ---------------------------------------------------------------- playback

    private void playMain() {
        JSONObject resume = resumeEntry();
        if (resume != null) {
            String url = resume.optString("url");
            for (int s = 0; item.series && s < seasons.size(); s++) {
                for (int i = 0; i < seasons.get(s).size(); i++) {
                    if (url.equals(episodeUrl(seasons.get(s).get(i)))) {
                        playEpisode(s, i);
                        return;
                    }
                }
            }
            play(url, resume.optString("title", item.name), resume.has("sub") ? resume.optString("sub") : null);
            return;
        }
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
            infoLoading.setVisibility(View.VISIBLE);
            loadInfo();
            return;
        }
        playEpisode(0, 0);
    }

    private String episodeUrl(JSONObject e) {
        if (e.has("url")) return e.optString("url");
        try {
            return Xtream.episodeUrl(e.optString("id"), e.optString("container_extension", "mp4"));
        } catch (Exception ex) {
            return null;
        }
    }

    private void playEpisode(int seasonIndex, int episodeIndex) {
        JSONObject e = seasons.get(seasonIndex).get(episodeIndex);
        String url = episodeUrl(e);
        if (url == null) {
            Toast.makeText(this, "Episódio indisponível", Toast.LENGTH_SHORT).show();
            return;
        }
        // Queue the rest of the series so "next episode" and autoplay work in the player.
        List<String> urls = new ArrayList<String>();
        List<String> subs = new ArrayList<String>();
        for (int s = seasonIndex; s < seasons.size(); s++) {
            List<JSONObject> eps = seasons.get(s);
            for (int i = s == seasonIndex ? episodeIndex + 1 : 0; i < eps.size(); i++) {
                String u = episodeUrl(eps.get(i));
                if (u == null) continue;
                urls.add(u);
                subs.add(episodeLabel(s, i));
            }
        }
        Intent i = playIntent(url, item.name, episodeLabel(seasonIndex, episodeIndex));
        i.putExtra(PlayerActivity.EXTRA_NEXT_URLS, urls.toArray(new String[0]));
        i.putExtra(PlayerActivity.EXTRA_NEXT_SUBS, subs.toArray(new String[0]));
        startActivity(i);
    }

    private String episodeLabel(int seasonIndex, int episodeIndex) {
        JSONObject e = seasons.get(seasonIndex).get(episodeIndex);
        String title = str(e, "title");
        return "T" + seasonNumber(seasonIndex) + ":E" + episodeNumber(e, episodeIndex)
                + (title.isEmpty() ? "" : "  " + title);
    }

    private void play(String url, String title, String subtitle) {
        startActivity(playIntent(url, title, subtitle));
    }

    private Intent playIntent(String url, String title, String subtitle) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra(PlayerActivity.EXTRA_URL, url);
        i.putExtra(PlayerActivity.EXTRA_TITLE, title);
        if (subtitle != null) i.putExtra(PlayerActivity.EXTRA_SUBTITLE, subtitle);
        i.putExtra(PlayerActivity.EXTRA_KEY, item.favKey());
        return i;
    }
}
