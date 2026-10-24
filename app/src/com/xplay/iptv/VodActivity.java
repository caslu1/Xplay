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
    private TextView heroTitle, heroPlot;
    private LinearLayout heroChips;
    private FrameLayout heroMark;
    private LinearLayout kidsPage;
    private GridView kidsGrid;
    private final List<VodItem> kidsItems = new ArrayList<VodItem>();
    private BaseAdapter kidsAdapter;
    private View heroButton;
    private VodItem heroItem;
    private LinearLayout homePage, searchPage;
    /** Rows of the page, in a scroll view that only moves when we move it (no jumps, no snapping back). */
    private RowScroll sectionsList;
    private LinearLayout rowsBox;
    private final List<FrameLayout> slots = new ArrayList<FrameLayout>();
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
    /** Title moves at once; chips, art and plot wait until the remote stops for a moment. */
    private VodItem pendingHero;
    private final Runnable applyHero = new Runnable() {
        public void run() { if (pendingHero != null) showHero(pendingHero); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        Ui.fit(this);
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
        else if (((page == TopBar.MOVIES && Repo.movies != null) || (page == TopBar.SERIES && Repo.series != null))
                && !state(page == TopBar.SERIES).equals(builtState)) {
            final int y = sectionsList.getScrollY();
            buildCatalog(page == TopBar.SERIES);
            sectionsList.post(new Runnable() {
                public void run() {
                    sectionsList.scrollTo(0, y);
                    populateVisible();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Posters.focusListener = null;
    }

    public void onTopBar(int item) {
        if (item == TopBar.MOVIES || item == TopBar.SERIES || item == TopBar.FAVORITES || item == TopBar.SEARCH
                || item == TopBar.KIDS || item == TopBar.NOVELAS) {
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
        kidsPage = buildKidsPage();
        host.addView(homePage, new FrameLayout.LayoutParams(-1, -1));
        host.addView(searchPage, new FrameLayout.LayoutParams(-1, -1));
        host.addView(kidsPage, new FrameLayout.LayoutParams(-1, -1));

        loading = new ProgressBar(this);
        loading.setIndeterminateTintList(ColorStateList.valueOf(Palette.GOLD));
        loading.setVisibility(View.GONE);
        host.addView(loading, new FrameLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48), Gravity.CENTER));
        setContentView(root);
    }

    private LinearLayout buildHome() {
        LinearLayout home = Ui.column(this);
        View heroText = buildHero();
        int h = getResources().getDisplayMetrics().heightPixels;
        // Tall enough for logo, chips, title and two lines of plot; anything extra is clipped, never drawn over the bar.
        FrameLayout heroBox = new FrameLayout(this);
        heroBox.setClipChildren(true);
        heroBox.addView(heroText, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        hero = heroBox;
        home.addView(heroBox, Ui.lp(-1, (int) (h * (Ui.isTv(this) ? 0.40f : 0.38f))));

        FrameLayout body = new FrameLayout(this);
        sectionsList = new RowScroll(this);
        sectionsList.setVerticalScrollBarEnabled(false);
        sectionsList.setFillViewport(true);
        rowsBox = Ui.column(this);
        rowsBox.setPadding(Ui.dp(this, 28) - Ui.ringGap(this), 0, 0, Ui.dp(this, 40));
        sectionsList.addView(rowsBox, new FrameLayout.LayoutParams(-1, -2));
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
        text.setPadding(Ui.dp(this, 40), 0, Ui.dp(this, 40), Ui.dp(this, 12));
        heroMark = new FrameLayout(this);
        LinearLayout.LayoutParams ml = Ui.lp(-2, -2);
        ml.bottomMargin = Ui.dp(this, 6);
        text.addView(heroMark, ml);
        android.widget.HorizontalScrollView cs = new android.widget.HorizontalScrollView(this);
        cs.setHorizontalScrollBarEnabled(false);
        heroChips = Ui.row(this);
        cs.addView(heroChips);
        text.addView(cs, Ui.lp(-1, -2));
        heroTitle = Ui.text(this, "", Ui.isTv(this) ? 28 : 22, Palette.TEXT, true);
        heroTitle.setMaxLines(1);
        heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        heroTitle.setShadowLayer(10, 0, 2, Palette.BLACK);
        LinearLayout.LayoutParams tl = Ui.lp(Ui.dp(this, 640), -2);
        tl.topMargin = Ui.dp(this, 8);
        text.addView(heroTitle, tl);
        heroPlot = Ui.text(this, "", Ui.isTv(this) ? 14 : 13, 0xFFC8C8C8, false);
        heroPlot.setMaxLines(2);
        heroPlot.setEllipsize(TextUtils.TruncateAt.END);
        heroPlot.setLineSpacing(0, 1.12f);
        heroPlot.setShadowLayer(6, 0, 1, Palette.BLACK);
        LinearLayout.LayoutParams pl = Ui.lp(Ui.dp(this, 600), -2);
        pl.topMargin = Ui.dp(this, 6);
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
        heroTitle.setText(v.name);
        pendingHero = v;
        handler.removeCallbacks(applyHero);
        if (heroItem == null) showHero(v);
        else handler.postDelayed(applyHero, 160);
    }

    private void showHero(VodItem v) {
        if (v == heroItem) return;
        heroItem = v;
        heroTitle.setText(v.name);
        heroMark.removeAllViews();
        MetaChips.fill(this, heroChips, null, null, null, v.rating, categoryList(v));
        heroPlot.setText("");
        JSONObject cached = InfoCache.peek(v);
        if (cached != null) applyHeroInfo(v, cached);
        else {
            ImageLoader.loadBlurred(v.poster, backdrop);
            handler.removeCallbacks(loadHeroInfo);
            handler.postDelayed(loadHeroInfo, 300);
        }
    }

    private void fetchHeroInfo(final VodItem v) {
        InfoCache.get(v, new InfoCache.Callback() {
            public void done(JSONObject info) {
                if (v == heroItem && !isFinishing()) applyHeroInfo(v, info);
            }
        });
    }

    private void applyHeroInfo(final VodItem v, final JSONObject answer) {
        String date = InfoCache.field(answer, "releasedate", "release_date", "releaseDate");
        JSONObject info = answer.optJSONObject("info");
        int secs = info == null ? 0 : info.optInt("duration_secs", 0);
        String genre = InfoCache.field(answer, "genre");
        List<String> genres = new ArrayList<String>();
        for (String g : genre.split("[,/]")) if (!g.trim().isEmpty()) genres.add(g.trim());
        if (genres.isEmpty()) genres = categoryList(v);
        MetaChips.fill(this, heroChips, v.series ? null : MetaChips.runtime(secs / 60),
                date.length() >= 4 ? date.substring(0, 4) : null, null, v.rating, genres);
        heroPlot.setText(InfoCache.field(answer, "plot", "description"));
        String art = Xtream.httpOrNull(InfoCache.field(answer, "backdrop_path"));
        if (art != null) showBackdrop(art);
        else ImageLoader.loadBlurred(v.poster, backdrop);
        // TMDB (when the user set a key) adds the streaming service, country and a better backdrop.
        Tmdb.get(v, answer, new Tmdb.Callback() {
            public void done(Tmdb.Details d) {
                if (d == null || v != heroItem || isFinishing()) return;
                heroMark.removeAllViews();
                View mark = MetaChips.providerMark(VodActivity.this, d.provider, d.providerLogo);
                if (mark != null) heroMark.addView(mark);
                String dur = v.series ? (d.seasons > 0 ? d.seasons + (d.seasons == 1 ? " temporada" : " temporadas") : null)
                        : MetaChips.runtime(d.runtime);
                MetaChips.fill(VodActivity.this, heroChips, dur, d.year, Tmdb.flag(d.country),
                        d.rating > 0 ? d.rating : v.rating, d.genres.isEmpty() ? categoryList(v) : d.genres);
                if (d.overview != null && !d.overview.isEmpty()) heroPlot.setText(d.overview);
                if (d.backdrop != null) showBackdrop(d.backdrop);
            }
        });
    }

    private void showBackdrop(String url) {
        backdrop.setAlpha(0f);
        ImageLoader.load(url, backdrop, getResources().getDisplayMetrics().widthPixels / 2, 0);
        backdrop.animate().alpha(1f).setDuration(350).start();
    }

    private static List<String> categoryList(VodItem v) {
        List<String> out = new ArrayList<String>();
        Map<String, String> cats = v.series ? Repo.seriesCategories : Repo.movieCategories;
        String c = cats == null ? null : cats.get(v.categoryId);
        out.add(v.series ? "Série" : "Filme");
        if (c != null) out.add(Variants.cleanGroup(c));
        return out;
    }

    /** Infantis: a grid of wide cards with the title underneath. */
    private LinearLayout buildKidsPage() {
        LinearLayout k = Ui.column(this);
        kidsGrid = new GridView(this);
        kidsGrid.setNumColumns(GridView.AUTO_FIT);
        kidsGrid.setColumnWidth(Ui.dp(this, 230));
        kidsGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        kidsGrid.setHorizontalSpacing(Ui.dp(this, 14));
        kidsGrid.setVerticalSpacing(Ui.dp(this, 14));
        kidsGrid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        kidsGrid.setDrawSelectorOnTop(false);
        kidsGrid.setPadding(Ui.dp(this, 30), Ui.dp(this, 12), Ui.dp(this, 30), Ui.dp(this, 24));
        kidsGrid.setClipToPadding(false);
        kidsAdapter = new BaseAdapter() {
            public int getCount() { return kidsItems.size(); }
            public Object getItem(int i) { return kidsItems.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View convert, ViewGroup parent) {
                int w = kidsGrid.getColumnWidth();
                return Posters.wideCard(VodActivity.this, kidsItems.get(i), w > 0 ? w : Ui.dp(VodActivity.this, 230));
            }
        };
        kidsGrid.setAdapter(kidsAdapter);
        k.addView(kidsGrid, new LinearLayout.LayoutParams(-1, 0, 1f));
        return k;
    }

    private static final java.util.regex.Pattern KIDS = java.util.regex.Pattern.compile(
            "(?i)infant|\\bkids?\\b|crian[cç]|desenhos?|cartoon|\\bbaby\\b|beb[eê]s?|nickelodeon|nick ?jr|"
                    + "disney ?(junior|jr|kids)|pixar|dreamworks|turma da m[oô]nica|galinha pintadinha|"
                    + "patrulha canina|peppa|baby ?shark|discovery kids|cartoonito|gloob|boomerang");
    /** Categories that are never for children, even if they also say "animação" or "família". */
    private static final java.util.regex.Pattern NOT_KIDS = java.util.regex.Pattern.compile(
            "(?i)adult|\\+ ?18|xxx|terror|horror|anime\\b|hentai|suspense|thriller|guerra|crime|policia|"
                    + "a[cç][aã]o e aventura|faroeste|erot|legendad");
    private static final java.util.regex.Pattern NOVELAS = java.util.regex.Pattern.compile(
            "(?i)novela|soap|telenovela|folhetim");

    /** Movies and series whose category matches the pattern (adult ones never appear here). */
    private static List<VodItem> themed(java.util.regex.Pattern p) {
        List<VodItem> out = new ArrayList<VodItem>();
        addThemed(out, Repo.series, Repo.seriesCategories, p);
        addThemed(out, Repo.movies, Repo.movieCategories, p);
        return out;
    }

    private static void addThemed(List<VodItem> out, List<VodItem> items, Map<String, String> cats,
                                  java.util.regex.Pattern p) {
        if (items == null || cats == null) return;
        for (VodItem v : items) {
            String c = cats.get(v.categoryId);
            if (c == null || !p.matcher(c).find() || Adult.isAdult(v)) continue;
            if (p == KIDS && (NOT_KIDS.matcher(c).find() || NOT_KIDS.matcher(v.name).find())) continue;
            out.add(v);
        }
    }

    private void buildKids() {
        kidsItems.clear();
        kidsItems.addAll(themed(KIDS));
        kidsAdapter.notifyDataSetChanged();
        emptyText.setText("");
        if (kidsItems.isEmpty()) {
            homePage.setVisibility(View.VISIBLE);
            hero.setVisibility(View.GONE);
            emptyText.setText("Nenhuma categoria infantil encontrada na sua lista");
        } else if (Ui.isTv(this)) {
            kidsGrid.post(new Runnable() {
                public void run() { if (kidsGrid.getChildCount() > 0) kidsGrid.getChildAt(0).requestFocus(); }
            });
        }
    }

    /** Novelas: hero + one row per novela category. */
    private void buildNovelas() {
        List<VodItem> all = themed(NOVELAS);
        LinkedHashMap<String, List<VodItem>> byCat = new LinkedHashMap<String, List<VodItem>>();
        for (VodItem v : all) {
            Map<String, String> cats = v.series ? Repo.seriesCategories : Repo.movieCategories;
            String name = Variants.cleanGroup(cats.get(v.categoryId));
            List<VodItem> l = byCat.get(name);
            if (l == null) {
                l = new ArrayList<VodItem>();
                byCat.put(name, l);
            }
            l.add(v);
        }
        List<Section> out = new ArrayList<Section>();
        List<VodItem> recent = new ArrayList<VodItem>(all);
        Collections.sort(recent, new Comparator<VodItem>() {
            public int compare(VodItem a, VodItem b) { return Long.compare(b.added, a.added); }
        });
        if (!recent.isEmpty()) out.add(new Section("Adicionadas recentemente", recent.subList(0, Math.min(60, recent.size()))));
        for (Map.Entry<String, List<VodItem>> e : byCat.entrySet()) out.add(new Section(e.getKey(), e.getValue()));
        sections.items = out;
        sections.notifyDataSetChanged();
        hero.setVisibility(all.isEmpty() ? View.GONE : View.VISIBLE);
        if (!recent.isEmpty()) setHero(recent.get(0));
        emptyText.setText(all.isEmpty() ? "Nenhuma categoria de novelas encontrada na sua lista" : "");
        focusFirstPoster();
    }

    /** On TV the remote starts on the first poster instead of somewhere in the top bar. */
    private void focusFirstPoster() {
        if (!Ui.isTv(this)) return;
        sectionsList.postDelayed(new Runnable() {
            public void run() {
                if (!slots.isEmpty()) focusRow(firstVisibleRow(), 0);
            }
        }, 120);
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
        boolean home = which != TopBar.SEARCH && which != TopBar.KIDS;
        homePage.setVisibility(home ? View.VISIBLE : View.GONE);
        searchPage.setVisibility(which == TopBar.SEARCH ? View.VISIBLE : View.GONE);
        kidsPage.setVisibility(which == TopBar.KIDS ? View.VISIBLE : View.GONE);
        backdrop.setVisibility(home ? View.VISIBLE : View.INVISIBLE);
        loading.setVisibility(View.GONE);
        if (which == TopBar.KIDS) {
            ensure(false, new Runnable() {
                public void run() { ensure(true, new Runnable() { public void run() { if (page == which) buildKids(); } }); }
            });
            return;
        }
        if (!home) {
            searchInput.requestFocus();
            ensure(false, new Runnable() {
                public void run() { ensure(true, new Runnable() { public void run() { runSearch(); } }); }
            });
            return;
        }
        heroItem = null;
        pendingHero = null;
        handler.removeCallbacks(applyHero);
        sections.items = new ArrayList<Section>();
        sections.notifyDataSetChanged();
        emptyText.setText("");
        if (which == TopBar.FAVORITES) {
            ensure(false, new Runnable() {
                public void run() { ensure(true, new Runnable() { public void run() { buildFavorites(); } }); }
            });
        } else if (which == TopBar.NOVELAS) {
            ensure(false, new Runnable() {
                public void run() { ensure(true, new Runnable() { public void run() { if (page == which) buildNovelas(); } }); }
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

    /** What the rows depend on besides the catalog: "continue watching" and favorites. */
    private String builtState = "";

    private static String state(boolean isSeries) {
        return Repo.continueList(isSeries).toString() + "|"
                + Repo.prefs().getStringSet("favorites", new java.util.HashSet<String>()).hashCode()
                + "|" + Adult.visible();
    }

    private void buildCatalog(boolean isSeries) {
        builtState = state(isSeries);
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
        sectionsList.scrollTo(0, 0);
        if (heroItem == null && !top.isEmpty()) setHero(top.get(0));
        hero.setVisibility(View.VISIBLE);
        focusFirstPoster();
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

    /** Back from the content first lifts the focus to the top bar; Back again leaves. */
    @Override
    public void onBackPressed() {
        if (!topBar.hasFocusInside() && topBar.focusSelected()) return;
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
        if (e.getAction() == KeyEvent.ACTION_DOWN && code >= KeyEvent.KEYCODE_DPAD_UP
                && code <= KeyEvent.KEYCODE_DPAD_RIGHT && TvRow.throttle(e)) return true;
        if (e.getAction() == KeyEvent.ACTION_DOWN && homePage.getVisibility() == View.VISIBLE
                && (code == KeyEvent.KEYCODE_DPAD_DOWN || code == KeyEvent.KEYCODE_DPAD_UP)) {
            View focused = getCurrentFocus();
            View slot = focused;
            while (slot != null && slot.getParent() != rowsBox) {
                slot = slot.getParent() instanceof View ? (View) slot.getParent() : null;
            }
            if (slot != null) {
                int pos = slots.indexOf(slot);
                final int target = pos + (code == KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1);
                if (target < 0) {
                    sectionsList.smoothScrollTo(0, 0);
                    return super.dispatchKeyEvent(e);
                }
                if (target >= slots.size()) return true;
                int[] loc = new int[2];
                focused.getLocationOnScreen(loc);
                populate(target);
                focusRow(target, loc[0]);
                // One smooth move that puts the row at the top of the list area.
                sectionsList.smoothScrollTo(0, Math.max(0, slots.get(target).getTop()));
                return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    /** Focuses the item of that row nearest to x; false when the row is not laid out. */
    private boolean focusRow(int position, int x) {
        if (position < 0 || position >= slots.size()) return false;
        populate(position);
        FrameLayout slot = slots.get(position);
        View row = slot.getChildCount() > 0 ? slot.getChildAt(0) : null;
        if (!(row instanceof ViewGroup) || ((ViewGroup) row).getChildCount() < 2) return false;
        View hs = ((ViewGroup) row).getChildAt(1);
        // Like Leanback: each row comes back to the item it had focused (the first one the first time).
        return hs instanceof TvRow && ((TvRow) hs).focusSelected();
    }

    // ---------------------------------------------------------------- rows

    /** A ScrollView that never scrolls by itself to show the focused poster; we scroll it per row. */
    private final class RowScroll extends android.widget.ScrollView {
        RowScroll(android.content.Context c) { super(c); }

        @Override
        protected int computeScrollDeltaToGetChildRectOnScreen(android.graphics.Rect rect) { return 0; }

        @Override
        protected void onScrollChanged(int l, int t, int oldl, int oldt) {
            super.onScrollChanged(l, t, oldl, oldt);
            populateVisible();
        }
    }

    /** One empty slot per section; posters are built when a slot comes near the screen. */
    private void renderRows() {
        rowsBox.removeAllViews();
        slots.clear();
        int estimate = Ui.dp(this, Ui.posterDp(this) * 1.5f + 64);
        for (int i = 0; i < sections.getCount(); i++) {
            FrameLayout slot = new FrameLayout(this);
            slot.setMinimumHeight(estimate);
            rowsBox.addView(slot, Ui.lp(-1, -2));
            slots.add(slot);
        }
        rowsBox.post(new Runnable() {
            public void run() { populateVisible(); }
        });
        for (int i = 0; i < Math.min(3, slots.size()); i++) populate(i);
    }

    private void populate(int i) {
        if (i < 0 || i >= slots.size()) return;
        FrameLayout slot = slots.get(i);
        if (slot.getChildCount() > 0) return;
        View row = sections.getView(i, null, slot);
        if (row.getParent() instanceof ViewGroup) ((ViewGroup) row.getParent()).removeView(row);
        slot.addView(row, new FrameLayout.LayoutParams(-1, -2));
        slot.setMinimumHeight(0);
    }

    private void populateVisible() {
        int h = sectionsList.getHeight();
        int top = sectionsList.getScrollY() - h, bottom = sectionsList.getScrollY() + 2 * h;
        for (int i = 0; i < slots.size(); i++) {
            FrameLayout s = slots.get(i);
            if (s.getBottom() >= top && s.getTop() <= bottom) populate(i);
        }
    }

    private int firstVisibleRow() {
        int y = sectionsList.getScrollY();
        for (int i = 0; i < slots.size(); i++) if (slots.get(i).getBottom() > y) return i;
        return 0;
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
        card.setBackground(Ui.selectable(this, 0xCC1C1C1C, 0xE62A2A2A, r, Palette.GOLD));
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
        FrameLayout box = new FrameLayout(this);
        float r = Ui.dp(this, 10);
        box.setBackground(Ui.stroke(0xCC1C1C1C, r, Ui.dp(this, 1), 0x33FFFFFF));
        TextView t = Ui.text(this, "Ver todos\n" + s.items.size() + " títulos", 15, Palette.GOLD, true);
        t.setGravity(Gravity.CENTER);
        box.addView(t, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        int g = 2 * Ui.ringGap(this);
        card.addView(Ui.ringed(this, box, r), Ui.lp(w + g, w * 3 / 2 + g));
        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { CategoryActivity.start(VodActivity.this, s.title, s.items); }
        });
        return card;
    }

    /** An empty section row: title over a horizontal strip. */
    /** An empty section row: title over a TV row (focused item lines up at the left keyline). */
    private LinearLayout newRow() {
        LinearLayout row = Ui.column(this);
        row.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        row.setClipChildren(false);
        LinearLayout header = Ui.row(this);
        // Title starts exactly where the first poster image starts.
        header.setPadding(Ui.ringGap(this), 0, Ui.dp(this, 24), Ui.dp(this, 4));
        TextView title = Ui.text(this, "", 17, Palette.TEXT, false);
        title.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
        header.addView(title, Ui.weight(1));
        row.addView(header, Ui.lp(-1, -2));
        TvRow hs = new TvRow(this);
        hs.setKeyline(0);
        hs.strip().setGravity(Gravity.TOP);
        hs.strip().setPadding(0, 0, Ui.dp(this, 28), 0);
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
            renderRows();
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
            LinearLayout strip = ((TvRow) row.getChildAt(1)).strip();
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
                    lp.rightMargin = TvRow.gap(VodActivity.this);
                    strip.addView(tile, lp);
                }
            } else if (s.resume != null) {
                for (JSONObject e : s.resume) {
                    String key = e.optString("key");
                    boolean series = key.startsWith("series:");
                    VodItem v = Repo.findVod(series, key.substring(key.indexOf(':') + 1));
                    LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
                    lp.rightMargin = TvRow.gap(VodActivity.this);
                    strip.addView(Posters.continueCard(VodActivity.this, v, e, poster, new Runnable() {
                        public void run() { showPage(page); }
                    }), lp);
                }
            } else if (s.channels != null) {
                for (int i = 0; i < s.channels.size(); i++) {
                    LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(VodActivity.this, 160), Ui.dp(VodActivity.this, 120));
                    lp.rightMargin = TvRow.gap(VodActivity.this);
                    strip.addView(channelCard(s.channels, i), lp);
                }
            } else {
                int n = Math.min(ROW_LIMIT, s.items.size());
                for (int i = 0; i < n; i++) {
                    LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
                    lp.rightMargin = TvRow.gap(VodActivity.this);
                    strip.addView(Posters.card(VodActivity.this, s.items.get(i), poster), lp);
                }
                if (s.items.size() > ROW_LIMIT) strip.addView(seeAllTile(s), Ui.lp(-2, -2));
            }
            return row;
        }
    }
}
