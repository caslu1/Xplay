package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Movies, series, favorites and search. ARVIO-style: a full-bleed backdrop of the highlighted title
 * with its name, details and plot, over rows of posters.
 */
public class VodActivity extends Activity implements TopBar.Listener {

    /** One of TopBar.MOVIES, SERIES, FAVORITES or SEARCH. */
    static final String EXTRA_TAB = "tab";

    private static final int ROW_LIMIT = 20;

    private final Handler handler = new Handler();
    private int page = TopBar.MOVIES;

    private TopBar topBar;
    private ImageView backdrop;
    private View hero;
    private TextView heroTitle, heroMeta, heroRating, heroPlot;
    private View heroButton;
    private VodItem heroItem;
    private LinearLayout homePage, searchPage;
    private ListView sectionsList;
    private final SectionAdapter sections = new SectionAdapter();
    private ProgressBar loading;
    private TextView emptyText;
    private EditText searchInput;
    private TextView searchHint;
    private final List<VodItem> searchResults = new ArrayList<VodItem>();
    private BaseAdapter searchAdapter;

    private final Runnable loadHeroInfo = new Runnable() {
        public void run() { if (heroItem != null) fetchHeroInfo(heroItem); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (!Repo.hasVod() || Repo.channels == null) {
            finish();
            return;
        }
        buildUi();
        showPage(getIntent().getIntExtra(EXTRA_TAB, TopBar.MOVIES));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        showPage(intent.getIntExtra(EXTRA_TAB, page));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (topBar == null) return;
        Posters.focusListener = new Posters.FocusListener() {
            public void focused(VodItem v) { setHero(v); }
        };
        // Favorites and "continue watching" may have changed while away.
        if (page == TopBar.FAVORITES) buildFavorites();
        else if ((page == TopBar.MOVIES && Repo.movies != null) || (page == TopBar.SERIES && Repo.series != null)) {
            int first = sectionsList.getFirstVisiblePosition();
            buildCatalog(page == TopBar.SERIES);
            sectionsList.setSelection(first);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Posters.focusListener = null;
    }

    public void onTopBar(int item) {
        if (item == TopBar.MOVIES || item == TopBar.SERIES || item == TopBar.FAVORITES || item == TopBar.SEARCH) {
            showPage(item);
        } else {
            TopBar.open(this, item);
        }
    }

    // ---------------------------------------------------------------- layout

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Palette.BLACK);

        // Backdrop of the highlighted title, faded into black on the left and at the bottom.
        int h = getResources().getDisplayMetrics().heightPixels;
        backdrop = new ImageView(this);
        backdrop.setScaleType(ImageView.ScaleType.CENTER_CROP);
        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(-1, (int) (h * 0.78f), Gravity.TOP | Gravity.END);
        root.addView(backdrop, bl);
        View left = new View(this);
        left.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0xF5000000, 0xCC000000, 0x40000000, 0x10000000}));
        root.addView(left, new FrameLayout.LayoutParams(-1, (int) (h * 0.78f), Gravity.TOP));
        View bottom = new View(this);
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {Palette.BLACK, Palette.BLACK, 0x00000000}));
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(-1, (int) (h * 0.62f), Gravity.BOTTOM);
        root.addView(bottom, fl);
        View top = new View(this);
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xB3000000, 0x00000000}));
        root.addView(top, new FrameLayout.LayoutParams(-1, Ui.dp(this, 90), Gravity.TOP));

        LinearLayout column = Ui.column(this);
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));
        topBar = new TopBar(this, true, this);
        column.addView(topBar, Ui.lp(-1, -2));

        FrameLayout host = new FrameLayout(this);
        column.addView(host, new LinearLayout.LayoutParams(-1, 0, 1f));
        homePage = buildHome();
        searchPage = buildSearch();
        host.addView(homePage, new FrameLayout.LayoutParams(-1, -1));
        host.addView(searchPage, new FrameLayout.LayoutParams(-1, -1));

        loading = new ProgressBar(this);
        loading.setIndeterminateTintList(ColorStateList.valueOf(Palette.GOLD));
        loading.setVisibility(View.GONE);
        host.addView(loading, new FrameLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48), Gravity.CENTER));
        setContentView(root);
    }

    private LinearLayout buildHome() {
        LinearLayout home = Ui.column(this);
        hero = buildHero();
        int h = getResources().getDisplayMetrics().heightPixels;
        home.addView(hero, Ui.lp(-1, (int) (h * (Ui.isTv(this) ? 0.38f : 0.34f))));

        FrameLayout body = new FrameLayout(this);
        sectionsList = new ListView(this);
        sectionsList.setDivider(null);
        sectionsList.setSelector(new android.graphics.drawable.ColorDrawable(0));
        sectionsList.setItemsCanFocus(true);
        sectionsList.setVerticalScrollBarEnabled(false);
        sectionsList.setPadding(Ui.dp(this, 26), 0, 0, Ui.dp(this, 24));
        sectionsList.setClipToPadding(false);
        sectionsList.setAdapter(sections);
        body.addView(sectionsList, new FrameLayout.LayoutParams(-1, -1));
        emptyText = Ui.text(this, "", 16, Palette.TEXT_DIM, false);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(Ui.dp(this, 32), 0, Ui.dp(this, 32), 0);
        body.addView(emptyText, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        home.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        return home;
    }

    private View buildHero() {
        LinearLayout text = Ui.column(this);
        text.setGravity(Gravity.BOTTOM);
        text.setPadding(Ui.dp(this, 36), 0, Ui.dp(this, 36), Ui.dp(this, 10));
        heroTitle = Ui.text(this, "", 34, Palette.TEXT, true);
        heroTitle.setMaxLines(2);
        heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        heroTitle.setShadowLayer(10, 0, 2, Palette.BLACK);
        text.addView(heroTitle, Ui.lp(Ui.dp(this, 620), -2));
        LinearLayout metaLine = Ui.row(this);
        heroRating = Ui.text(this, "", 13, Palette.BLACK, true);
        heroRating.setBackground(Ui.round(Palette.GOLD, Ui.dp(this, 4)));
        heroRating.setPadding(Ui.dp(this, 7), Ui.dp(this, 1), Ui.dp(this, 7), Ui.dp(this, 1));
        LinearLayout.LayoutParams rl = Ui.lp(-2, -2);
        rl.rightMargin = Ui.dp(this, 12);
        metaLine.addView(heroRating, rl);
        heroMeta = Ui.text(this, "", 16, 0xFFE6E6E6, false);
        heroMeta.setSingleLine(true);
        heroMeta.setEllipsize(TextUtils.TruncateAt.END);
        metaLine.addView(heroMeta);
        LinearLayout.LayoutParams ml = Ui.lp(-2, -2);
        ml.topMargin = Ui.dp(this, 6);
        text.addView(metaLine, ml);
        heroPlot = Ui.text(this, "", 15, 0xFFD0D0D0, false);
        heroPlot.setMaxLines(3);
        heroPlot.setEllipsize(TextUtils.TruncateAt.END);
        heroPlot.setLineSpacing(0, 1.15f);
        heroPlot.setShadowLayer(6, 0, 1, Palette.BLACK);
        LinearLayout.LayoutParams pl = Ui.lp(Ui.dp(this, 620), -2);
        pl.topMargin = Ui.dp(this, 8);
        text.addView(heroPlot, pl);
        if (!Ui.isTv(this)) {
            // On touch screens the hero gets its own button; on TV, OK on the poster opens it.
            TextView b = Ui.text(this, "▶  Ver detalhes", 15, Palette.BLACK, true);
            b.setFocusable(true);
            b.setClickable(true);
            b.setBackground(Ui.selectable(this, Palette.TEXT, 0xFFDDDDDD, Ui.dp(this, 22), Palette.GOLD));
            b.setPadding(Ui.dp(this, 20), Ui.dp(this, 9), Ui.dp(this, 22), Ui.dp(this, 9));
            LinearLayout.LayoutParams bl = Ui.lp(-2, -2);
            bl.topMargin = Ui.dp(this, 12);
            text.addView(b, bl);
            heroButton = b;
            b.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { if (heroItem != null) DetailActivity.start(VodActivity.this, heroItem); }
            });
        }
        return text;
    }

    /** Shows a title in the hero right away; its backdrop and plot follow once the remote settles. */
    private void setHero(VodItem v) {
        if (v == null || v == heroItem) return;
        heroItem = v;
        heroTitle.setText(v.name);
        heroRating.setText(v.ratingText() == null ? "" : "★ " + v.ratingText());
        heroRating.setVisibility(v.ratingText() == null ? View.GONE : View.VISIBLE);
        heroMeta.setText(categoryOf(v));
        heroPlot.setText("");
        JSONObject cached = InfoCache.peek(v);
        if (cached != null) applyHeroInfo(v, cached);
        else {
            ImageLoader.loadBlurred(v.poster, backdrop);
            handler.removeCallbacks(loadHeroInfo);
            handler.postDelayed(loadHeroInfo, 450);
        }
    }

    private void fetchHeroInfo(final VodItem v) {
        InfoCache.get(v, new InfoCache.Callback() {
            public void done(JSONObject info) {
                if (v == heroItem && !isFinishing()) applyHeroInfo(v, info);
            }
        });
    }

    private void applyHeroInfo(VodItem v, JSONObject answer) {
        List<String> parts = new ArrayList<String>();
        String date = InfoCache.field(answer, "releasedate", "release_date", "releaseDate");
        if (date.length() >= 4) parts.add(date.substring(0, 4));
        int secs = answer.optJSONObject("info") == null ? 0 : answer.optJSONObject("info").optInt("duration_secs", 0);
        if (secs > 0) parts.add(secs / 60 + " min");
        String genre = InfoCache.field(answer, "genre");
        parts.add(genre.isEmpty() ? categoryOf(v) : genre);
        heroMeta.setText(TextUtils.join("   |   ", parts));
        heroPlot.setText(InfoCache.field(answer, "plot", "description"));
        String art = Xtream.httpOrNull(InfoCache.field(answer, "backdrop_path"));
        if (art != null) {
            backdrop.setAlpha(0f);
            ImageLoader.load(art, backdrop, getResources().getDisplayMetrics().widthPixels / 2, 0);
            backdrop.animate().alpha(1f).setDuration(350).start();
        } else {
            ImageLoader.loadBlurred(v.poster, backdrop);
        }
    }

    private static String categoryOf(VodItem v) {
        Map<String, String> cats = v.series ? Repo.seriesCategories : Repo.movieCategories;
        String c = cats == null ? null : cats.get(v.categoryId);
        return (v.series ? "Série" : "Filme") + (c == null ? "" : "  •  " + c);
    }

    private LinearLayout buildSearch() {
        LinearLayout s = Ui.column(this);
        int p = Ui.dp(this, 32);
        s.setPadding(p, Ui.dp(this, 8), p, 0);
        LinearLayout box = Ui.row(this);
        box.setBackground(Ui.stroke(0xCC111111, Ui.dp(this, 26), Ui.dp(this, 1), 0x40FFFFFF));
        box.setPadding(Ui.dp(this, 18), 0, Ui.dp(this, 18), 0);
        box.addView(Ui.icon(this, R.drawable.ic_search, Palette.GOLD), Ui.lp(Ui.dp(this, 22), Ui.dp(this, 22)));
        searchInput = new EditText(this);
        searchInput.setHint("Buscar filmes e séries");
        searchInput.setHintTextColor(0xFF888888);
        searchInput.setTextColor(Palette.TEXT);
        searchInput.setSingleLine(true);
        searchInput.setBackground(null);
        searchInput.setPadding(Ui.dp(this, 12), Ui.dp(this, 13), 0, Ui.dp(this, 13));
        box.addView(searchInput, Ui.weight(1));
        s.addView(box, Ui.lp(-1, -2));

        searchHint = Ui.text(this, "Digite pelo menos 2 letras", 14, Palette.TEXT_DIM, false);
        searchHint.setPadding(Ui.dp(this, 8), Ui.dp(this, 12), 0, 0);
        s.addView(searchHint);

        GridView grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(Ui.dp(this, Ui.posterDp(this) + 14));
        grid.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
        grid.setVerticalSpacing(Ui.dp(this, 14));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        grid.setPadding(Ui.dp(this, 4), Ui.dp(this, 14), Ui.dp(this, 4), Ui.dp(this, 20));
        grid.setClipToPadding(false);
        searchAdapter = new BaseAdapter() {
            public int getCount() { return searchResults.size(); }
            public Object getItem(int i) { return searchResults.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View convert, ViewGroup parent) {
                return Posters.card(VodActivity.this, searchResults.get(i), Ui.posterDp(VodActivity.this));
            }
        };
        grid.setAdapter(searchAdapter);
        s.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));

        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            public void onTextChanged(CharSequence c, int a, int b, int d) {}
            public void afterTextChanged(Editable e) { runSearch(); }
        });
        return s;
    }

    // ---------------------------------------------------------------- pages

    private void showPage(final int which) {
        page = which;
        topBar.select(which);
        boolean home = which != TopBar.SEARCH;
        homePage.setVisibility(home ? View.VISIBLE : View.GONE);
        searchPage.setVisibility(home ? View.GONE : View.VISIBLE);
        backdrop.setVisibility(home ? View.VISIBLE : View.INVISIBLE);
        loading.setVisibility(View.GONE);
        if (!home) {
            searchInput.requestFocus();
            ensure(false, new Runnable() {
                public void run() { ensure(true, new Runnable() { public void run() { runSearch(); } }); }
            });
            return;
        }
        heroItem = null;
        sections.items = new ArrayList<Section>();
        sections.notifyDataSetChanged();
        emptyText.setText("");
        if (which == TopBar.FAVORITES) {
            ensure(false, new Runnable() {
                public void run() { ensure(true, new Runnable() { public void run() { buildFavorites(); } }); }
            });
        } else {
            final boolean isSeries = which == TopBar.SERIES;
            ensure(isSeries, new Runnable() {
                public void run() { if (page == which) buildCatalog(isSeries); }
            });
        }
    }

    /** Loads movies (series=false) or series if needed, then runs next. */
    private void ensure(final boolean seriesList, final Runnable next) {
        if ((seriesList ? Repo.series : Repo.movies) != null) {
            next.run();
            return;
        }
        loading.setVisibility(View.VISIBLE);
        Task.run(this, new Callable<Void>() {
            public Void call() throws Exception {
                if (seriesList) Xtream.loadSeries();
                else Xtream.loadMovies();
                return null;
            }
        }, new Task.Done<Void>() {
            public void ok(Void v) {
                loading.setVisibility(View.GONE);
                next.run();
            }

            public void fail(Exception e) {
                loading.setVisibility(View.GONE);
                emptyText.setText("Erro ao carregar: " + Task.message(e));
            }
        });
    }

    private void buildCatalog(boolean isSeries) {
        List<VodItem> all = isSeries ? Repo.series : Repo.movies;
        LinkedHashMap<String, String> cats = isSeries ? Repo.seriesCategories : Repo.movieCategories;
        LinkedHashMap<String, List<VodItem>> byCat = new LinkedHashMap<String, List<VodItem>>();
        for (String id : cats.keySet()) byCat.put(id, new ArrayList<VodItem>());
        for (VodItem v : all) {
            List<VodItem> l = byCat.get(v.categoryId);
            if (l == null) {
                l = new ArrayList<VodItem>();
                byCat.put(v.categoryId, l);
            }
            l.add(v);
        }

        List<VodItem> visibleAll = Adult.filter(all);
        List<Section> out = new ArrayList<Section>();
        List<JSONObject> resume = new ArrayList<JSONObject>();
        for (JSONObject e : Repo.continueList(isSeries)) {
            String key = e.optString("key");
            VodItem v = Repo.findVod(isSeries, key.substring(key.indexOf(':') + 1));
            if (v == null || Adult.visible() || !Adult.isAdult(v)) resume.add(e);
        }
        if (!resume.isEmpty()) {
            Section c = new Section("Continuar assistindo", null);
            c.resume = resume;
            out.add(c);
        }

        List<VodItem> recent = new ArrayList<VodItem>(visibleAll);
        Collections.sort(recent, new Comparator<VodItem>() {
            public int compare(VodItem a, VodItem b) { return Long.compare(b.added, a.added); }
        });
        out.add(new Section("Adicionados recentemente", recent.subList(0, Math.min(100, recent.size()))));
        List<VodItem> top = new ArrayList<VodItem>(visibleAll);
        Collections.sort(top, new Comparator<VodItem>() {
            public int compare(VodItem a, VodItem b) { return Double.compare(b.rating, a.rating); }
        });
        out.add(new Section(isSeries ? "Séries em alta" : "Filmes em alta", top.subList(0, Math.min(100, top.size()))));

        Section genres = new Section("Categorias", null);
        genres.genres = new ArrayList<Map.Entry<String, List<VodItem>>>();
        genres.genreArt = new ArrayList<String>();
        java.util.Set<String> usedArt = new java.util.HashSet<String>();
        for (Map.Entry<String, List<VodItem>> e : byCat.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            boolean adult = Adult.isAdultName(cats.get(e.getKey()));
            if (adult && Adult.hidden()) continue;
            genres.genres.add(e);
            genres.genreArt.add(adult && !Adult.visible() ? null : pickArt(e.getValue(), usedArt));
        }
        genres.catNames = cats;
        if (!genres.genres.isEmpty()) out.add(genres);

        for (Map.Entry<String, List<VodItem>> e : byCat.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            String name = cats.get(e.getKey());
            if (Adult.isAdultName(name) && !Adult.visible()) continue;
            out.add(new Section(name == null ? "Outros" : name, e.getValue()));
        }
        sections.items = out;
        sections.notifyDataSetChanged();
        sectionsList.setSelection(0);
        if (heroItem == null && !top.isEmpty()) setHero(top.get(0));
        hero.setVisibility(View.VISIBLE);
        if (all.isEmpty()) emptyText.setText(isSeries ? "Nenhuma série neste servidor" : "Nenhum filme neste servidor");
    }

    /** Best-rated poster of a category that no other tile uses yet. */
    private static String pickArt(List<VodItem> items, java.util.Set<String> used) {
        VodItem best = null;
        for (VodItem v : items) {
            if (v.poster == null || used.contains(v.poster)) continue;
            if (best == null || v.rating > best.rating) best = v;
        }
        if (best == null) return null;
        used.add(best.poster);
        return best.poster;
    }

    private void buildFavorites() {
        if (Repo.movies == null || Repo.series == null) return;
        List<Section> out = new ArrayList<Section>();
        List<Channel> favChannels = new ArrayList<Channel>();
        for (Channel c : Adult.channels()) if (Repo.isFav(Repo.liveKey(c))) favChannels.add(c);
        List<VodItem> favMovies = new ArrayList<VodItem>();
        for (VodItem v : Adult.filter(Repo.movies)) if (Repo.isFav(v.favKey())) favMovies.add(v);
        List<VodItem> favSeries = new ArrayList<VodItem>();
        for (VodItem v : Adult.filter(Repo.series)) if (Repo.isFav(v.favKey())) favSeries.add(v);
        if (!favChannels.isEmpty()) {
            Section s = new Section("Canais favoritos", null);
            s.channels = favChannels;
            out.add(s);
        }
        if (!favMovies.isEmpty()) out.add(new Section("Filmes favoritos", favMovies));
        if (!favSeries.isEmpty()) out.add(new Section("Séries favoritas", favSeries));
        sections.items = out;
        sections.notifyDataSetChanged();
        VodItem first = !favMovies.isEmpty() ? favMovies.get(0) : !favSeries.isEmpty() ? favSeries.get(0) : null;
        hero.setVisibility(first == null ? View.GONE : View.VISIBLE);
        backdrop.setVisibility(first == null ? View.INVISIBLE : View.VISIBLE);
        if (first != null) setHero(first);
        emptyText.setText(out.isEmpty() ? "Nenhum favorito ainda.\nSegure um canal para favoritar, ou toque em "
                + "♥ Favoritos em um filme ou série." : "");
    }

    private void runSearch() {
        if (Repo.movies == null || Repo.series == null) return;
        String q = searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        searchResults.clear();
        if (q.length() >= 2) {
            for (VodItem v : Adult.filter(Repo.movies)) {
                if (v.name.toLowerCase(Locale.ROOT).contains(q)) searchResults.add(v);
                if (searchResults.size() >= 150) break;
            }
            for (VodItem v : Adult.filter(Repo.series)) {
                if (searchResults.size() >= 300) break;
                if (v.name.toLowerCase(Locale.ROOT).contains(q)) searchResults.add(v);
            }
            searchHint.setText(searchResults.isEmpty() ? "Nada encontrado" : searchResults.size() + " resultados");
        } else {
            searchHint.setText("Digite pelo menos 2 letras");
        }
        searchAdapter.notifyDataSetChanged();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }

    // ---------------------------------------------------------------- remote navigation

    /** ▲ ▼ jump straight to the poster above/below (one press per row) instead of walking the list. */
    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        int code = e.getKeyCode();
        if (e.getAction() == KeyEvent.ACTION_DOWN && homePage.getVisibility() == View.VISIBLE
                && (code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_DPAD_UP)) {
            View focused = getCurrentFocus();
            View row = focused;
            while (row != null && row.getParent() != sectionsList) {
                row = row.getParent() instanceof View ? (View) row.getParent() : null;
            }
            if (row != null) {
                int pos = sectionsList.getPositionForView(row);
                final int target = pos + (code == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1);
                if (target < 0) return super.dispatchKeyEvent(e);
                if (target >= sections.getCount()) return true;
                int[] loc = new int[2];
                focused.getLocationOnScreen(loc);
                final int x = loc[0];
                sectionsList.setSelectionFromTop(target, Ui.dp(this, 2));
                sectionsList.post(new Runnable() {
                    public void run() { focusRow(target, x); }
                });
                return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    private void focusRow(int position, int x) {
        View row = sectionsList.getChildAt(position - sectionsList.getFirstVisiblePosition());
        if (!(row instanceof ViewGroup) || ((ViewGroup) row).getChildCount() < 2) return;
        View hs = ((ViewGroup) row).getChildAt(1);
        if (!(hs instanceof HorizontalScrollView)) return;
        ViewGroup strip = (ViewGroup) ((HorizontalScrollView) hs).getChildAt(0);
        View best = null;
        int bestDist = Integer.MAX_VALUE;
        int[] loc = new int[2];
        for (int i = 0; i < strip.getChildCount(); i++) {
            View c = strip.getChildAt(i);
            if (!c.isFocusable()) continue;
            c.getLocationOnScreen(loc);
            int d = Math.abs(loc[0] - x);
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        if (best != null) best.requestFocus();
    }

    // ---------------------------------------------------------------- sections

    private static final class Section {
        final String title;
        final List<VodItem> items;
        List<Map.Entry<String, List<VodItem>>> genres;
        Map<String, String> catNames;
        List<String> genreArt;
        List<Channel> channels;
        List<JSONObject> resume;

        Section(String title, List<VodItem> items) {
            this.title = title;
            this.items = items;
        }
    }

    private View channelCard(final List<Channel> list, final int index) {
        Channel c = list.get(index);
        LinearLayout card = Ui.column(this);
        card.setGravity(Gravity.CENTER);
        card.setFocusable(true);
        card.setClickable(true);
        float r = Ui.dp(this, 12);
        card.setBackground(Ui.round(0xCC1C1C1C, r));
        card.setForeground(Ui.focusRing(this, r));
        int p = Ui.dp(this, 10);
        card.setPadding(p, p, p, p);
        ImageView logo = new ImageView(this);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ImageLoader.load(c.logo, logo, Ui.dp(this, 64), R.drawable.ic_launcher);
        card.addView(logo, Ui.lp(Ui.dp(this, 64), Ui.dp(this, 48)));
        TextView name = Ui.text(this, c.name, 13, Palette.TEXT, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(2);
        name.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nl = Ui.lp(-1, -2);
        nl.topMargin = Ui.dp(this, 8);
        card.addView(name, nl);
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Repo.playlist = list;
                Intent i = new Intent(VodActivity.this, PlayerActivity.class);
                i.putExtra(PlayerActivity.EXTRA_INDEX, index);
                startActivity(i);
            }
        });
        return card;
    }

    /** "Ver todos" at the end of a row, sized like a poster. */
    private View seeAllTile(final Section s) {
        int w = Ui.dp(this, Ui.posterDp(this));
        LinearLayout card = Ui.column(this);
        card.setFocusable(true);
        card.setClickable(true);
        int p = Ui.dp(this, 4);
        card.setPadding(p, p, p, p);
        FrameLayout box = new FrameLayout(this);
        float r = Ui.dp(this, 10);
        box.setBackground(Ui.stroke(0xCC1C1C1C, r, Ui.dp(this, 1), 0x33FFFFFF));
        box.setDuplicateParentStateEnabled(true);
        box.setForeground(Ui.focusRing(this, r));
        TextView t = Ui.text(this, "Ver todos\n" + s.items.size() + " títulos", 15, Palette.GOLD, true);
        t.setGravity(Gravity.CENTER);
        box.addView(t, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        card.addView(box, Ui.lp(w, w * 3 / 2));
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { CategoryActivity.start(VodActivity.this, s.title, s.items); }
        });
        return card;
    }

    /** An empty section row: title over a horizontal strip. */
    private LinearLayout newRow() {
        // The ListView "selects" whole rows on the remote; only the focused poster should light up.
        LinearLayout row = new LinearLayout(VodActivity.this) {
            @Override
            public void dispatchSetSelected(boolean selected) {}
        };
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4));
        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 24), 0);
        TextView title = Ui.text(this, "", 20, Palette.TEXT, false);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        header.addView(title, Ui.weight(1));
        row.addView(header, Ui.lp(-1, -2));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout strip = Ui.row(this);
        strip.setGravity(Gravity.TOP);
        // Room around the posters so the focused one (slightly enlarged, gold ring) is never clipped.
        strip.setPadding(Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 24), Ui.dp(this, 8));
        hs.addView(strip);
        row.addView(hs, Ui.lp(-1, -2));
        return row;
    }

    private final class SectionAdapter extends BaseAdapter {
        List<Section> items = new ArrayList<Section>();
        /** Built rows are kept (a few) so scrolling back does not rebuild every poster. */
        final LinkedHashMap<Section, View> rowCache = new LinkedHashMap<Section, View>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Section, View> eldest) { return size() > 14; }
        };

        @Override
        public void notifyDataSetChanged() {
            rowCache.clear();
            super.notifyDataSetChanged();
        }

        public int getCount() { return items.size(); }
        public Object getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int pos, View convert, ViewGroup parent) {
            final Section s = items.get(pos);
            View cached = rowCache.get(s);
            if (cached != null) return cached;
            LinearLayout row = newRow();
            rowCache.put(s, row);
            LinearLayout header = (LinearLayout) row.getChildAt(0);
            ((TextView) header.getChildAt(0)).setText(s.title);
            LinearLayout strip = (LinearLayout) ((HorizontalScrollView) row.getChildAt(1)).getChildAt(0);
            int poster = Ui.posterDp(VodActivity.this);

            if (s.genres != null) {
                for (int i = 0; i < s.genres.size(); i++) {
                    final Map.Entry<String, List<VodItem>> e = s.genres.get(i);
                    final String name = s.catNames.get(e.getKey()) == null ? "Outros" : s.catNames.get(e.getKey());
                    final boolean adult = Adult.isAdultName(name);
                    String label = adult && !Adult.visible() ? "🔒 " + name : name;
                    View tile = Posters.genreTile(VodActivity.this, label, i, s.genreArt.get(i), new View.OnClickListener() {
                        public void onClick(View v) {
                            Runnable open = new Runnable() {
                                public void run() { CategoryActivity.start(VodActivity.this, name, e.getValue()); }
                            };
                            if (adult) Adult.unlock(VodActivity.this, open);
                            else open.run();
                        }
                    });
                    LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(VodActivity.this, 200), Ui.dp(VodActivity.this, 90));
                    lp.rightMargin = Ui.dp(VodActivity.this, 12);
                    strip.addView(tile, lp);
                }
            } else if (s.resume != null) {
                for (JSONObject e : s.resume) {
                    String key = e.optString("key");
                    boolean series = key.startsWith("series:");
                    VodItem v = Repo.findVod(series, key.substring(key.indexOf(':') + 1));
                    LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
                    lp.rightMargin = Ui.dp(VodActivity.this, 8);
                    strip.addView(Posters.continueCard(VodActivity.this, v, e, poster, new Runnable() {
                        public void run() { showPage(page); }
                    }), lp);
                }
            } else if (s.channels != null) {
                for (int i = 0; i < s.channels.size(); i++) {
                    LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(VodActivity.this, 160), Ui.dp(VodActivity.this, 120));
                    lp.rightMargin = Ui.dp(VodActivity.this, 12);
                    strip.addView(channelCard(s.channels, i), lp);
                }
            } else {
                int n = Math.min(ROW_LIMIT, s.items.size());
                for (int i = 0; i < n; i++) {
                    LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
                    lp.rightMargin = Ui.dp(VodActivity.this, 8);
                    strip.addView(Posters.card(VodActivity.this, s.items.get(i), poster), lp);
                }
                if (s.items.size() > ROW_LIMIT) strip.addView(seeAllTile(s), Ui.lp(-2, -2));
            }
            return row;
        }
    }
}
