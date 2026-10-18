package com.xplay.iptv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
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
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;

/** Movies and series home (chips + poster rows), search and account pages. */
public class VodActivity extends Activity {

    static final String EXTRA_TAB = "tab";
    static final int TAB_MOVIES = 0;
    static final int TAB_SERIES = 1;
    static final int TAB_PROFILE = 2;

    private static final int CHIP_MOVIES = 0, CHIP_SERIES = 1, CHIP_FAVS = 2;
    private static final int PAGE_HOME = 0, PAGE_SEARCH = 1, PAGE_PROFILE = 2;
    private static final int ROW_LIMIT = 20;

    private int chip = CHIP_MOVIES;
    private int page = PAGE_HOME;

    private FrameLayout pageHost;
    private LinearLayout homePage, searchPage;
    private View profilePage;
    private ListView sectionsList;
    private SectionAdapter sections = new SectionAdapter();
    private ProgressBar loading;
    private TextView emptyText;
    private final List<TextView> chips = new ArrayList<TextView>();
    private final List<View> navItems = new ArrayList<View>();
    private EditText searchInput;
    private GridView searchGrid;
    private final List<VodItem> searchResults = new ArrayList<VodItem>();
    private BaseAdapter searchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (!Repo.isXtream() || Repo.server == null) {
            finish();
            return;
        }
        buildUi();
        int tab = getIntent().getIntExtra(EXTRA_TAB, TAB_MOVIES);
        if (tab == TAB_PROFILE) showPage(PAGE_PROFILE);
        else {
            chip = tab == TAB_SERIES ? CHIP_SERIES : CHIP_MOVIES;
            showPage(PAGE_HOME);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Favorites may have changed on the detail screen.
        if (sectionsList != null && page == PAGE_HOME) {
            if (chip == CHIP_FAVS) loadHome();
            else sections.notifyDataSetChanged();
        }
    }

    // ---------------------------------------------------------------- layout

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Palette.VOD_BG);

        LinearLayout top = Ui.row(this);
        int p = Ui.dp(this, 16);
        top.setPadding(p, Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 4));
        top.addView(Ui.logo(this, 20));
        top.addView(new View(this), Ui.spacer());
        ImageView liveBtn = Ui.iconButton(this, R.drawable.ic_live, Palette.TEXT, Palette.BLUE);
        liveBtn.setContentDescription("TV ao vivo");
        top.addView(liveBtn, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        liveBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        root.addView(top, Ui.lp(-1, -2));

        pageHost = new FrameLayout(this);
        root.addView(pageHost, new LinearLayout.LayoutParams(-1, 0, 1f));

        homePage = buildHome();
        searchPage = buildSearch();
        profilePage = buildProfile();
        pageHost.addView(homePage, new FrameLayout.LayoutParams(-1, -1));
        pageHost.addView(searchPage, new FrameLayout.LayoutParams(-1, -1));
        pageHost.addView(profilePage, new FrameLayout.LayoutParams(-1, -1));

        View divider = new View(this);
        divider.setBackgroundColor(0xFF242428);
        root.addView(divider, Ui.lp(-1, 1));
        LinearLayout nav = Ui.row(this);
        nav.setBackgroundColor(0xFF0E0E10);
        nav.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 6));
        navItem(nav, R.drawable.ic_home, "Início", new Runnable() {
            public void run() { showPage(PAGE_HOME); }
        });
        navItem(nav, R.drawable.ic_search, "Buscar", new Runnable() {
            public void run() { showPage(PAGE_SEARCH); }
        });
        navItem(nav, R.drawable.ic_live, "TV ao vivo", new Runnable() {
            public void run() { finish(); }
        });
        navItem(nav, R.drawable.ic_person, "Perfil", new Runnable() {
            public void run() { showPage(PAGE_PROFILE); }
        });
        root.addView(nav, Ui.lp(-1, -2));
        setContentView(root);
    }

    private void navItem(LinearLayout nav, int icon, String label, final Runnable action) {
        LinearLayout item = Ui.column(this);
        item.setGravity(Gravity.CENTER);
        item.setFocusable(true);
        item.setClickable(true);
        item.setBackground(Ui.selectable(this, 0, 0x1FFFFFFF, Ui.dp(this, 12), 0));
        item.setPadding(0, Ui.dp(this, 4), 0, Ui.dp(this, 4));
        item.addView(Ui.icon(this, icon, Palette.TEXT_DIM), Ui.lp(Ui.dp(this, 26), Ui.dp(this, 26)));
        item.addView(Ui.text(this, label, 12, Palette.TEXT_DIM, true));
        nav.addView(item, Ui.weight(1));
        navItems.add(item);
        item.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { action.run(); }
        });
    }

    private LinearLayout buildHome() {
        LinearLayout home = Ui.column(this);
        HorizontalScrollView chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout chipRow = Ui.row(this);
        chipRow.setPadding(Ui.dp(this, 12), Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 8));
        chipScroll.addView(chipRow);
        String[] labels = {"Filmes", "Séries", "Favoritos"};
        for (int i = 0; i < labels.length; i++) {
            final int which = i;
            TextView c = Ui.text(this, labels[i], 16, Palette.TEXT, true);
            c.setFocusable(true);
            c.setClickable(true);
            c.setPadding(Ui.dp(this, 22), Ui.dp(this, 10), Ui.dp(this, 22), Ui.dp(this, 10));
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.rightMargin = Ui.dp(this, 10);
            chipRow.addView(c, lp);
            chips.add(c);
            c.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    chip = which;
                    loadHome();
                }
            });
        }
        home.addView(chipScroll, Ui.lp(-1, -2));

        FrameLayout body = new FrameLayout(this);
        sectionsList = new ListView(this);
        sectionsList.setDivider(null);
        sectionsList.setSelector(new android.graphics.drawable.ColorDrawable(0));
        sectionsList.setItemsCanFocus(true);
        sectionsList.setVerticalScrollBarEnabled(false);
        sectionsList.setPadding(0, 0, 0, Ui.dp(this, 16));
        sectionsList.setClipToPadding(false);
        sectionsList.setAdapter(sections);
        body.addView(sectionsList, new FrameLayout.LayoutParams(-1, -1));
        loading = new ProgressBar(this);
        loading.setIndeterminateTintList(ColorStateList.valueOf(Palette.BLUE));
        body.addView(loading, new FrameLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48), Gravity.CENTER));
        emptyText = Ui.text(this, "", 16, Palette.TEXT_DIM, false);
        emptyText.setGravity(Gravity.CENTER);
        emptyText.setPadding(Ui.dp(this, 32), 0, Ui.dp(this, 32), 0);
        body.addView(emptyText, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        home.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        return home;
    }

    private LinearLayout buildSearch() {
        LinearLayout s = Ui.column(this);
        int p = Ui.dp(this, 16);
        s.setPadding(p, Ui.dp(this, 8), p, 0);
        LinearLayout box = Ui.row(this);
        box.setBackground(Ui.round(Palette.VOD_SURFACE, Ui.dp(this, 24)));
        box.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        box.addView(Ui.icon(this, R.drawable.ic_search, Palette.TEXT_DIM), Ui.lp(Ui.dp(this, 22), Ui.dp(this, 22)));
        searchInput = new EditText(this);
        searchInput.setHint("Buscar filmes e séries");
        searchInput.setHintTextColor(0xFF77777C);
        searchInput.setTextColor(Palette.TEXT);
        searchInput.setSingleLine(true);
        searchInput.setBackground(null);
        searchInput.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), 0, Ui.dp(this, 14));
        box.addView(searchInput, Ui.weight(1));
        s.addView(box, Ui.lp(-1, -2));

        searchGrid = new GridView(this);
        searchGrid.setNumColumns(GridView.AUTO_FIT);
        searchGrid.setColumnWidth(Ui.dp(this, 118));
        searchGrid.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
        searchGrid.setVerticalSpacing(Ui.dp(this, 12));
        searchGrid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        searchGrid.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        searchGrid.setClipToPadding(false);
        searchAdapter = posterAdapter(searchResults);
        searchGrid.setAdapter(searchAdapter);
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(-1, 0, 1f);
        s.addView(searchGrid, gl);

        searchInput.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            public void onTextChanged(CharSequence c, int a, int b, int d) {}
            public void afterTextChanged(Editable e) { runSearch(); }
        });
        return s;
    }

    BaseAdapter posterAdapter(final List<VodItem> items) {
        return new BaseAdapter() {
            public int getCount() { return items.size(); }
            public Object getItem(int i) { return items.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View convert, ViewGroup parent) {
                return Posters.card(VodActivity.this, items.get(i), 110);
            }
        };
    }

    private View buildProfile() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout c = Ui.column(this);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = Ui.dp(this, 24);
        c.setPadding(p, p, p, p);
        scroll.addView(c);

        ImageView avatar = Ui.icon(this, R.drawable.ic_person, Palette.TEXT);
        avatar.setBackground(Ui.round(Palette.BLUE, Ui.dp(this, 44)));
        int ap = Ui.dp(this, 20);
        avatar.setPadding(ap, ap, ap, ap);
        c.addView(avatar, Ui.lp(Ui.dp(this, 88), Ui.dp(this, 88)));

        TextView name = Ui.text(this, Repo.user, 22, Palette.TEXT, true);
        LinearLayout.LayoutParams nl = Ui.lp(-2, -2);
        nl.topMargin = Ui.dp(this, 12);
        c.addView(name, nl);

        org.json.JSONObject a = Repo.account;
        String status = a == null ? "" : a.optString("status", "");
        if (!status.isEmpty()) {
            TextView chipView = Ui.text(this, status.equalsIgnoreCase("Active") ? "Conta ativa" : status, 13,
                    Palette.TEXT, true);
            chipView.setBackground(Ui.round(status.equalsIgnoreCase("Active") ? 0xFF1F7A4A : Palette.HOT,
                    Ui.dp(this, 12)));
            chipView.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4));
            LinearLayout.LayoutParams sl = Ui.lp(-2, -2);
            sl.topMargin = Ui.dp(this, 8);
            c.addView(chipView, sl);
        }

        LinearLayout info = Ui.column(this);
        info.setBackground(Ui.round(Palette.VOD_SURFACE, Ui.dp(this, 16)));
        info.setPadding(Ui.dp(this, 18), Ui.dp(this, 8), Ui.dp(this, 18), Ui.dp(this, 8));
        infoRow(info, "Servidor", Repo.server);
        if (a != null) {
            infoRow(info, "Vencimento", date(a.optString("exp_date", "")));
            infoRow(info, "Conexões", a.optString("active_cons", "0") + " de " + a.optString("max_connections", "-"));
            infoRow(info, "Criada em", date(a.optString("created_at", "")));
        }
        LinearLayout.LayoutParams il = Ui.lp(-1, -2);
        il.topMargin = Ui.dp(this, 24);
        c.addView(info, il);

        TextView logout = Ui.text(this, "SAIR DA CONTA", 15, Palette.ERROR, true);
        logout.setGravity(Gravity.CENTER);
        logout.setFocusable(true);
        logout.setClickable(true);
        logout.setBackground(Ui.selectable(this, 0, 0x22FF6B5E, Ui.dp(this, 26), Palette.ERROR));
        logout.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
        LinearLayout.LayoutParams ll = Ui.lp(-1, -2);
        ll.topMargin = Ui.dp(this, 24);
        c.addView(logout, ll);
        logout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmLogout(); }
        });
        return scroll;
    }

    private void infoRow(LinearLayout parent, String label, String value) {
        LinearLayout r = Ui.row(this);
        r.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        r.addView(Ui.text(this, label, 15, Palette.TEXT_DIM, false), Ui.weight(1));
        TextView v = Ui.text(this, value == null || value.isEmpty() ? "-" : value, 15, Palette.TEXT, true);
        v.setGravity(Gravity.END);
        r.addView(v, Ui.weight(1.4f));
        parent.addView(r, Ui.lp(-1, -2));
    }

    private static String date(String unix) {
        try {
            long t = Long.parseLong(unix.trim());
            if (t <= 0) return "Ilimitado";
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(t * 1000L));
        } catch (NumberFormatException e) {
            return unix.isEmpty() || unix.equals("null") ? "Ilimitado" : unix;
        }
    }

    // ---------------------------------------------------------------- pages

    private void showPage(int which) {
        page = which;
        homePage.setVisibility(which == PAGE_HOME ? View.VISIBLE : View.GONE);
        searchPage.setVisibility(which == PAGE_SEARCH ? View.VISIBLE : View.GONE);
        profilePage.setVisibility(which == PAGE_PROFILE ? View.VISIBLE : View.GONE);
        int navIndex = which == PAGE_HOME ? 0 : which == PAGE_SEARCH ? 1 : 3;
        for (int i = 0; i < navItems.size(); i++) {
            LinearLayout item = (LinearLayout) navItems.get(i);
            int color = i == navIndex ? Palette.BLUE : Palette.TEXT_DIM;
            ((ImageView) item.getChildAt(0)).setColorFilter(color);
            ((TextView) item.getChildAt(1)).setTextColor(color);
        }
        if (which == PAGE_HOME) loadHome();
        if (which == PAGE_SEARCH) {
            searchInput.requestFocus();
            ensure(true, new Runnable() {
                public void run() { ensure(false, new Runnable() { public void run() { runSearch(); } }); }
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
        emptyText.setText("");
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

    private void loadHome() {
        for (int i = 0; i < chips.size(); i++) {
            boolean on = i == chip;
            chips.get(i).setBackground(Ui.selectable(this, on ? Palette.BLUE : Palette.CHIP,
                    on ? 0xFF4A82F0 : 0xFF4A4A4E, Ui.dp(this, 24), Palette.TEXT));
        }
        sections.items = new ArrayList<Section>();
        sections.notifyDataSetChanged();
        emptyText.setText("");
        if (chip == CHIP_FAVS) {
            ensure(false, new Runnable() {
                public void run() { ensure(true, new Runnable() { public void run() { buildFavorites(); } }); }
            });
        } else {
            final boolean isSeries = chip == CHIP_SERIES;
            ensure(isSeries, new Runnable() {
                public void run() { if ((chip == CHIP_SERIES) == isSeries) buildCatalog(isSeries); }
            });
        }
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

        List<Section> out = new ArrayList<Section>();
        Section genres = new Section("Por gênero", null);
        genres.genres = new ArrayList<Map.Entry<String, List<VodItem>>>();
        for (Map.Entry<String, List<VodItem>> e : byCat.entrySet()) {
            if (!e.getValue().isEmpty()) genres.genres.add(e);
        }
        genres.catNames = cats;
        if (!genres.genres.isEmpty()) out.add(genres);

        List<VodItem> recent = new ArrayList<VodItem>(all);
        Collections.sort(recent, new Comparator<VodItem>() {
            public int compare(VodItem a, VodItem b) { return Long.compare(b.added, a.added); }
        });
        out.add(new Section("Adicionados recentemente", recent.subList(0, Math.min(100, recent.size()))));
        List<VodItem> top = new ArrayList<VodItem>(all);
        Collections.sort(top, new Comparator<VodItem>() {
            public int compare(VodItem a, VodItem b) { return Double.compare(b.rating, a.rating); }
        });
        out.add(new Section("Mais bem avaliados", top.subList(0, Math.min(100, top.size()))));
        for (Map.Entry<String, List<VodItem>> e : byCat.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            String name = cats.get(e.getKey());
            out.add(new Section(name == null ? "Outros" : name, e.getValue()));
        }
        sections.items = out;
        sections.notifyDataSetChanged();
        sectionsList.setSelection(0);
        if (all.isEmpty()) emptyText.setText(isSeries ? "Nenhuma série neste servidor" : "Nenhum filme neste servidor");
    }

    private void buildFavorites() {
        List<Section> out = new ArrayList<Section>();
        List<VodItem> favMovies = new ArrayList<VodItem>();
        for (VodItem v : Repo.movies) if (Repo.isFav(v.favKey())) favMovies.add(v);
        List<VodItem> favSeries = new ArrayList<VodItem>();
        for (VodItem v : Repo.series) if (Repo.isFav(v.favKey())) favSeries.add(v);
        if (!favMovies.isEmpty()) out.add(new Section("Filmes favoritos", favMovies));
        if (!favSeries.isEmpty()) out.add(new Section("Séries favoritas", favSeries));
        sections.items = out;
        sections.notifyDataSetChanged();
        emptyText.setText(out.isEmpty() ? "Nenhum favorito ainda.\nAbra um filme ou série e toque em ♥ Favoritos." : "");
    }

    private void runSearch() {
        if (Repo.movies == null || Repo.series == null) return;
        String q = searchInput.getText().toString().trim().toLowerCase(Locale.ROOT);
        searchResults.clear();
        if (q.length() >= 2) {
            for (VodItem v : Repo.movies) {
                if (v.name.toLowerCase(Locale.ROOT).contains(q)) searchResults.add(v);
                if (searchResults.size() >= 150) break;
            }
            for (VodItem v : Repo.series) {
                if (searchResults.size() >= 300) break;
                if (v.name.toLowerCase(Locale.ROOT).contains(q)) searchResults.add(v);
            }
        }
        searchAdapter.notifyDataSetChanged();
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sair da conta?")
                .setPositiveButton("Sair", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        Repo.logout();
                        Intent i = new Intent(VodActivity.this, LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (page != PAGE_HOME) showPage(PAGE_HOME);
        else super.onBackPressed();
    }

    // ---------------------------------------------------------------- sections

    private static final class Section {
        final String title;
        final List<VodItem> items;
        List<Map.Entry<String, List<VodItem>>> genres;
        Map<String, String> catNames;

        Section(String title, List<VodItem> items) {
            this.title = title;
            this.items = items;
        }
    }

    private final class SectionAdapter extends BaseAdapter {
        List<Section> items = new ArrayList<Section>();

        public int getCount() { return items.size(); }
        public Object getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return i; }
        @Override public int getViewTypeCount() { return 1; }
        @Override public boolean isEnabled(int position) { return false; }

        public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout row;
            if (convert == null) {
                row = Ui.column(VodActivity.this);
                row.setPadding(0, Ui.dp(VodActivity.this, 10), 0, Ui.dp(VodActivity.this, 6));
                LinearLayout header = Ui.row(VodActivity.this);
                header.setPadding(Ui.dp(VodActivity.this, 16), 0, Ui.dp(VodActivity.this, 8), Ui.dp(VodActivity.this, 8));
                TextView title = Ui.text(VodActivity.this, "", 20, Palette.TEXT, true);
                header.addView(title, Ui.weight(1));
                TextView more = Ui.text(VodActivity.this, "Ver todos ›", 14, Palette.BLUE, true);
                more.setFocusable(true);
                more.setClickable(true);
                more.setBackground(Ui.selectable(VodActivity.this, 0, 0x22FFFFFF, Ui.dp(VodActivity.this, 10), 0));
                more.setPadding(Ui.dp(VodActivity.this, 8), Ui.dp(VodActivity.this, 4), Ui.dp(VodActivity.this, 8),
                        Ui.dp(VodActivity.this, 4));
                header.addView(more);
                row.addView(header, Ui.lp(-1, -2));
                HorizontalScrollView hs = new HorizontalScrollView(VodActivity.this);
                hs.setHorizontalScrollBarEnabled(false);
                LinearLayout strip = Ui.row(VodActivity.this);
                strip.setPadding(Ui.dp(VodActivity.this, 12), 0, Ui.dp(VodActivity.this, 12), 0);
                hs.addView(strip);
                row.addView(hs, Ui.lp(-1, -2));
            } else {
                row = (LinearLayout) convert;
            }
            final Section s = items.get(pos);
            LinearLayout header = (LinearLayout) row.getChildAt(0);
            ((TextView) header.getChildAt(0)).setText(s.title);
            View more = header.getChildAt(1);
            LinearLayout strip = (LinearLayout) ((HorizontalScrollView) row.getChildAt(1)).getChildAt(0);
            strip.removeAllViews();
            ((HorizontalScrollView) row.getChildAt(1)).scrollTo(0, 0);

            if (s.genres != null) {
                more.setVisibility(View.GONE);
                for (int i = 0; i < s.genres.size(); i++) {
                    final Map.Entry<String, List<VodItem>> e = s.genres.get(i);
                    final String name = s.catNames.get(e.getKey()) == null ? "Outros" : s.catNames.get(e.getKey());
                    View tile = Posters.genreTile(VodActivity.this, name, i, new View.OnClickListener() {
                        public void onClick(View v) { CategoryActivity.start(VodActivity.this, name, e.getValue()); }
                    });
                    LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(VodActivity.this, 160), Ui.dp(VodActivity.this, 72));
                    lp.leftMargin = Ui.dp(VodActivity.this, 4);
                    lp.rightMargin = Ui.dp(VodActivity.this, 6);
                    strip.addView(tile, lp);
                }
            } else {
                more.setVisibility(s.items.size() > ROW_LIMIT ? View.VISIBLE : View.GONE);
                more.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { CategoryActivity.start(VodActivity.this, s.title, s.items); }
                });
                int n = Math.min(ROW_LIMIT, s.items.size());
                for (int i = 0; i < n; i++) {
                    View card = Posters.card(VodActivity.this, s.items.get(i), 116);
                    LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
                    lp.rightMargin = Ui.dp(VodActivity.this, 4);
                    strip.addView(card, lp);
                }
            }
            return row;
        }
    }
}
