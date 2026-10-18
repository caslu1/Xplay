package com.xplay.iptv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
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
import android.widget.Toast;

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

/** Movies, series, favorites, search and account pages under the shared top bar. */
public class VodActivity extends Activity implements TopBar.Listener {

    /** One of TopBar.MOVIES, SERIES, FAVORITES, SEARCH or ACCOUNT. */
    static final String EXTRA_TAB = "tab";

    private static final int ROW_LIMIT = 20;

    private int page = TopBar.MOVIES;

    private TopBar topBar;
    private LinearLayout homePage, searchPage;
    private View profilePage;
    private ListView sectionsList;
    private final SectionAdapter sections = new SectionAdapter();
    private ProgressBar loading;
    private TextView emptyText;
    private EditText searchInput;
    private TextView searchHint;
    private final List<VodItem> searchResults = new ArrayList<VodItem>();
    private BaseAdapter searchAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (!Repo.isXtream() || Repo.server == null || Repo.channels == null) {
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
        // Favorites may have changed on the detail screen.
        if (topBar == null) return;
        if (page == TopBar.FAVORITES) buildFavorites();
        else sections.notifyDataSetChanged();
    }

    public void onTopBar(int item) {
        if (item == TopBar.LIVE) {
            finish();
            overridePendingTransition(0, 0);
        } else {
            showPage(item);
        }
    }

    // ---------------------------------------------------------------- layout

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Palette.BLACK);
        topBar = new TopBar(this, true, this);
        root.addView(topBar, Ui.lp(-1, -2));

        FrameLayout host = new FrameLayout(this);
        root.addView(host, new LinearLayout.LayoutParams(-1, 0, 1f));
        homePage = buildHome();
        searchPage = buildSearch();
        profilePage = buildProfile();
        host.addView(homePage, new FrameLayout.LayoutParams(-1, -1));
        host.addView(searchPage, new FrameLayout.LayoutParams(-1, -1));
        host.addView(profilePage, new FrameLayout.LayoutParams(-1, -1));

        loading = new ProgressBar(this);
        loading.setIndeterminateTintList(ColorStateList.valueOf(Palette.GOLD));
        loading.setVisibility(View.GONE);
        host.addView(loading, new FrameLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48), Gravity.CENTER));
        setContentView(root);
    }

    private LinearLayout buildHome() {
        LinearLayout home = Ui.column(this);
        FrameLayout body = new FrameLayout(this);
        sectionsList = new ListView(this);
        sectionsList.setDivider(null);
        sectionsList.setSelector(new android.graphics.drawable.ColorDrawable(0));
        sectionsList.setItemsCanFocus(true);
        sectionsList.setVerticalScrollBarEnabled(false);
        sectionsList.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 16));
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

    private LinearLayout buildSearch() {
        LinearLayout s = Ui.column(this);
        int p = Ui.dp(this, 24);
        s.setPadding(p, Ui.dp(this, 8), p, 0);
        LinearLayout box = Ui.row(this);
        box.setBackground(Ui.stroke(Palette.SURFACE_HI, Ui.dp(this, 24), Ui.dp(this, 1), 0xFF333333));
        box.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        box.addView(Ui.icon(this, R.drawable.ic_search, Palette.GOLD), Ui.lp(Ui.dp(this, 22), Ui.dp(this, 22)));
        searchInput = new EditText(this);
        searchInput.setHint("Buscar filmes e séries");
        searchInput.setHintTextColor(0xFF77777C);
        searchInput.setTextColor(Palette.TEXT);
        searchInput.setSingleLine(true);
        searchInput.setBackground(null);
        searchInput.setPadding(Ui.dp(this, 12), Ui.dp(this, 12), 0, Ui.dp(this, 12));
        box.addView(searchInput, Ui.weight(1));
        s.addView(box, Ui.lp(-1, -2));

        searchHint = Ui.text(this, "Digite pelo menos 2 letras", 14, Palette.TEXT_DIM, false);
        searchHint.setPadding(Ui.dp(this, 8), Ui.dp(this, 12), 0, 0);
        s.addView(searchHint);

        GridView grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(Ui.dp(this, 128));
        grid.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
        grid.setVerticalSpacing(Ui.dp(this, 12));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        grid.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 16));
        grid.setClipToPadding(false);
        searchAdapter = new BaseAdapter() {
            public int getCount() { return searchResults.size(); }
            public Object getItem(int i) { return searchResults.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View convert, ViewGroup parent) {
                return Posters.card(VodActivity.this, searchResults.get(i), 116);
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

    private View buildProfile() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.TOP);
        int p = Ui.dp(this, 32);
        row.setPadding(p, Ui.dp(this, 16), p, p);
        scroll.addView(row);

        LinearLayout left = Ui.column(this);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        left.setPadding(0, Ui.dp(this, 24), 0, 0);
        ImageView avatar = Ui.icon(this, R.drawable.ic_person, Palette.BLACK);
        avatar.setBackground(Ui.round(Palette.GOLD, Ui.dp(this, 48)));
        int ap = Ui.dp(this, 22);
        avatar.setPadding(ap, ap, ap, ap);
        left.addView(avatar, Ui.lp(Ui.dp(this, 96), Ui.dp(this, 96)));
        TextView name = Ui.text(this, Repo.user, 24, Palette.TEXT, true);
        LinearLayout.LayoutParams nl = Ui.lp(-2, -2);
        nl.topMargin = Ui.dp(this, 14);
        left.addView(name, nl);
        org.json.JSONObject a = Repo.account;
        String status = a == null ? "" : a.optString("status", "");
        if (!status.isEmpty()) {
            boolean active = status.equalsIgnoreCase("Active");
            TextView chip = Ui.text(this, active ? "Conta ativa" : status, 13, Palette.TEXT, true);
            chip.setBackground(Ui.round(active ? 0xFF1F7A4A : Palette.HOT, Ui.dp(this, 12)));
            chip.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4));
            LinearLayout.LayoutParams sl = Ui.lp(-2, -2);
            sl.topMargin = Ui.dp(this, 8);
            left.addView(chip, sl);
        }
        row.addView(left, Ui.weight(1));

        LinearLayout right = Ui.column(this);
        LinearLayout info = Ui.column(this);
        info.setBackground(Ui.round(Palette.SURFACE_HI, Ui.dp(this, 16)));
        info.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), Ui.dp(this, 8));
        infoRow(info, "Servidor", Repo.server);
        if (a != null) {
            infoRow(info, "Vencimento", date(a.optString("exp_date", "")));
            infoRow(info, "Conexões", a.optString("active_cons", "0") + " de " + a.optString("max_connections", "-"));
            infoRow(info, "Criada em", date(a.optString("created_at", "")));
        }
        right.addView(info, Ui.lp(-1, -2));

        LinearLayout buttons = Ui.row(this);
        View refresh = button("ATUALIZAR LISTAS", true);
        View logout = button("SAIR DA CONTA", false);
        buttons.addView(refresh, Ui.weight(1));
        View gap = new View(this);
        buttons.addView(gap, Ui.lp(Ui.dp(this, 12), 0));
        buttons.addView(logout, Ui.weight(1));
        LinearLayout.LayoutParams bl = Ui.lp(-1, -2);
        bl.topMargin = Ui.dp(this, 18);
        right.addView(buttons, bl);
        row.addView(right, Ui.weight(1.5f));

        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { refreshAll(); }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmLogout(); }
        });
        return scroll;
    }

    private View button(String label, boolean primary) {
        TextView b = Ui.text(this, label, 15, primary ? Palette.BLACK : Palette.ERROR, true);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        b.setPadding(0, Ui.dp(this, 14), 0, Ui.dp(this, 14));
        float r = Ui.dp(this, 26);
        b.setBackground(primary
                ? Ui.selectable(this, Palette.GOLD, Palette.GOLD_LIGHT, r, Palette.TEXT)
                : Ui.selectable(this, 0x14FF6B5E, 0x33FF6B5E, r, Palette.ERROR));
        return b;
    }

    private void infoRow(LinearLayout parent, String label, String value) {
        LinearLayout r = Ui.row(this);
        r.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        r.addView(Ui.text(this, label, 15, Palette.TEXT_DIM, false), Ui.weight(1));
        TextView v = Ui.text(this, value == null || value.isEmpty() ? "-" : value, 15, Palette.TEXT, true);
        v.setGravity(Gravity.END);
        v.setSingleLine(true);
        v.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        r.addView(v, Ui.weight(1.6f));
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

    private void showPage(final int which) {
        page = which;
        topBar.select(which);
        boolean home = which == TopBar.MOVIES || which == TopBar.SERIES || which == TopBar.FAVORITES;
        homePage.setVisibility(home ? View.VISIBLE : View.GONE);
        searchPage.setVisibility(which == TopBar.SEARCH ? View.VISIBLE : View.GONE);
        profilePage.setVisibility(which == TopBar.ACCOUNT ? View.VISIBLE : View.GONE);
        loading.setVisibility(View.GONE);
        if (!home) {
            if (which == TopBar.SEARCH) {
                searchInput.requestFocus();
                ensure(false, new Runnable() {
                    public void run() { ensure(true, new Runnable() { public void run() { runSearch(); } }); }
                });
            }
            return;
        }
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
        if (Repo.movies == null || Repo.series == null) return;
        List<Section> out = new ArrayList<Section>();
        List<Channel> favChannels = new ArrayList<Channel>();
        for (Channel c : Repo.channels) if (Repo.isFav(Repo.liveKey(c))) favChannels.add(c);
        List<VodItem> favMovies = new ArrayList<VodItem>();
        for (VodItem v : Repo.movies) if (Repo.isFav(v.favKey())) favMovies.add(v);
        List<VodItem> favSeries = new ArrayList<VodItem>();
        for (VodItem v : Repo.series) if (Repo.isFav(v.favKey())) favSeries.add(v);
        if (!favChannels.isEmpty()) {
            Section s = new Section("Canais favoritos", null);
            s.channels = favChannels;
            out.add(s);
        }
        if (!favMovies.isEmpty()) out.add(new Section("Filmes favoritos", favMovies));
        if (!favSeries.isEmpty()) out.add(new Section("Séries favoritas", favSeries));
        sections.items = out;
        sections.notifyDataSetChanged();
        emptyText.setText(out.isEmpty() ? "Nenhum favorito ainda.\nSegure um canal para favoritar, ou toque em "
                + "♥ Favoritos em um filme ou série." : "");
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
            searchHint.setText(searchResults.isEmpty() ? "Nada encontrado" : searchResults.size() + " resultados");
        } else {
            searchHint.setText("Digite pelo menos 2 letras");
        }
        searchAdapter.notifyDataSetChanged();
    }

    private void refreshAll() {
        Toast.makeText(this, "Atualizando listas…", Toast.LENGTH_SHORT).show();
        Task.run(this, new Callable<Void>() {
            public Void call() throws Exception {
                Xtream.loadLive();
                Xtream.loadMovies();
                Xtream.loadSeries();
                return null;
            }
        }, new Task.Done<Void>() {
            public void ok(Void v) {
                Toast.makeText(VodActivity.this, "Listas atualizadas", Toast.LENGTH_SHORT).show();
            }

            public void fail(Exception e) {
                Toast.makeText(VodActivity.this, "Erro: " + Task.message(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sair da conta?")
                .setMessage("Você poderá entrar com outro login ou lista.")
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
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    // ---------------------------------------------------------------- sections

    private static final class Section {
        final String title;
        final List<VodItem> items;
        List<Map.Entry<String, List<VodItem>>> genres;
        Map<String, String> catNames;
        List<Channel> channels;

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
        card.setBackground(Ui.selectable(this, Palette.SURFACE_HI, 0xFF2A2A2A, Ui.dp(this, 12), Palette.GOLD));
        int p = Ui.dp(this, 10);
        card.setPadding(p, p, p, p);
        ImageView logo = new ImageView(this);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        ImageLoader.load(c.logo, logo, Ui.dp(this, 64), R.drawable.ic_launcher);
        card.addView(logo, Ui.lp(Ui.dp(this, 64), Ui.dp(this, 48)));
        TextView name = Ui.text(this, c.name.toUpperCase(Locale.getDefault()), 13, Palette.TEXT, true);
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

    private final class SectionAdapter extends BaseAdapter {
        List<Section> items = new ArrayList<Section>();

        public int getCount() { return items.size(); }
        public Object getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return i; }
        @Override public boolean isEnabled(int position) { return false; }

        public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout row;
            if (convert == null) {
                row = Ui.column(VodActivity.this);
                row.setPadding(0, Ui.dp(VodActivity.this, 12), 0, Ui.dp(VodActivity.this, 6));
                LinearLayout header = Ui.row(VodActivity.this);
                header.setPadding(Ui.dp(VodActivity.this, 12), 0, Ui.dp(VodActivity.this, 8), Ui.dp(VodActivity.this, 8));
                View accent = new View(VodActivity.this);
                accent.setBackground(Ui.round(Palette.GOLD, Ui.dp(VodActivity.this, 2)));
                LinearLayout.LayoutParams al = Ui.lp(Ui.dp(VodActivity.this, 4), Ui.dp(VodActivity.this, 20));
                al.rightMargin = Ui.dp(VodActivity.this, 10);
                header.addView(accent, al);
                TextView title = Ui.text(VodActivity.this, "", 19, Palette.TEXT, true);
                header.addView(title, Ui.weight(1));
                TextView more = Ui.text(VodActivity.this, "Ver todos ›", 14, Palette.GOLD, true);
                more.setFocusable(true);
                more.setClickable(true);
                more.setBackground(Ui.selectable(VodActivity.this, 0, 0x22FFFFFF, Ui.dp(VodActivity.this, 10), Palette.GOLD));
                more.setPadding(Ui.dp(VodActivity.this, 10), Ui.dp(VodActivity.this, 4), Ui.dp(VodActivity.this, 10),
                        Ui.dp(VodActivity.this, 4));
                header.addView(more);
                row.addView(header, Ui.lp(-1, -2));
                HorizontalScrollView hs = new HorizontalScrollView(VodActivity.this);
                hs.setHorizontalScrollBarEnabled(false);
                LinearLayout strip = Ui.row(VodActivity.this);
                strip.setPadding(Ui.dp(VodActivity.this, 6), 0, Ui.dp(VodActivity.this, 12), 0);
                hs.addView(strip);
                row.addView(hs, Ui.lp(-1, -2));
            } else {
                row = (LinearLayout) convert;
            }
            final Section s = items.get(pos);
            LinearLayout header = (LinearLayout) row.getChildAt(0);
            ((TextView) header.getChildAt(1)).setText(s.title);
            View more = header.getChildAt(2);
            HorizontalScrollView hs = (HorizontalScrollView) row.getChildAt(1);
            LinearLayout strip = (LinearLayout) hs.getChildAt(0);
            strip.removeAllViews();
            hs.scrollTo(0, 0);

            if (s.genres != null) {
                more.setVisibility(View.GONE);
                for (int i = 0; i < s.genres.size(); i++) {
                    final Map.Entry<String, List<VodItem>> e = s.genres.get(i);
                    final String name = s.catNames.get(e.getKey()) == null ? "Outros" : s.catNames.get(e.getKey());
                    View tile = Posters.genreTile(VodActivity.this, name, i, new View.OnClickListener() {
                        public void onClick(View v) { CategoryActivity.start(VodActivity.this, name, e.getValue()); }
                    });
                    LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(VodActivity.this, 170), Ui.dp(VodActivity.this, 72));
                    lp.rightMargin = Ui.dp(VodActivity.this, 10);
                    strip.addView(tile, lp);
                }
            } else if (s.channels != null) {
                more.setVisibility(View.GONE);
                for (int i = 0; i < s.channels.size(); i++) {
                    LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(VodActivity.this, 160), Ui.dp(VodActivity.this, 120));
                    lp.rightMargin = Ui.dp(VodActivity.this, 10);
                    strip.addView(channelCard(s.channels, i), lp);
                }
            } else {
                more.setVisibility(s.items.size() > ROW_LIMIT ? View.VISIBLE : View.GONE);
                more.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) { CategoryActivity.start(VodActivity.this, s.title, s.items); }
                });
                int n = Math.min(ROW_LIMIT, s.items.size());
                for (int i = 0; i < n; i++) {
                    LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
                    lp.rightMargin = Ui.dp(VodActivity.this, 6);
                    strip.addView(Posters.card(VodActivity.this, s.items.get(i), 128), lp);
                }
            }
            return row;
        }
    }
}
