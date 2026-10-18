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
    private ImageView heroArt;
    private TextView heroTitle, heroMeta, heroHint;

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
        // Favorites may have changed on the detail screen.
        if (topBar == null) return;
        if (heroTitle != null) {
            Posters.focusListener = new Posters.FocusListener() {
                public void focused(VodItem v) { showHero(v); }
            };
        }
        if (page == TopBar.FAVORITES) buildFavorites();
        else if ((page == TopBar.MOVIES && Repo.movies != null) || (page == TopBar.SERIES && Repo.series != null)) {
            int first = sectionsList.getFirstVisiblePosition();
            buildCatalog(page == TopBar.SERIES);
            sectionsList.setSelection(first);
        }
    }

    public void onTopBar(int item) {
        if (item == TopBar.LIVE || item == TopBar.SPORTS || (item == TopBar.ACCOUNT && !Repo.isXtream())) {
            TopBar.open(this, item);
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
        if (Ui.isTv(this)) home.addView(buildHero(), Ui.lp(-1,
                (int) (getResources().getDisplayMetrics().heightPixels * 0.30f)));
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

    /** Stremio-like preview of the focused title, shown on TVs above the rows. */
    private View buildHero() {
        FrameLayout hero = new FrameLayout(this);
        heroArt = new ImageView(this);
        heroArt.setScaleType(ImageView.ScaleType.CENTER_CROP);
        heroArt.setAlpha(0.6f);
        hero.addView(heroArt, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this);
        shade.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[] {0xF2000000, 0xB3000000, 0x33000000}));
        hero.addView(shade, new FrameLayout.LayoutParams(-1, -1));
        View fade = new View(this);
        fade.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.BOTTOM_TOP, new int[] {Palette.BLACK, 0}));
        hero.addView(fade, new FrameLayout.LayoutParams(-1, Ui.dp(this, 40), Gravity.BOTTOM));
        LinearLayout text = Ui.column(this);
        text.setGravity(Gravity.CENTER_VERTICAL);
        text.setPadding(Ui.dp(this, 32), 0, Ui.dp(this, 32), 0);
        heroTitle = Ui.text(this, "", 30, Palette.TEXT, true);
        heroTitle.setMaxLines(1);
        heroTitle.setEllipsize(TextUtils.TruncateAt.END);
        text.addView(heroTitle);
        heroMeta = Ui.text(this, "", 16, Palette.GOLD, true);
        text.addView(heroMeta);
        heroHint = Ui.text(this, "OK para ver detalhes  •  ◀ ▶ para navegar", 13, Palette.TEXT_DIM, false);
        heroHint.setPadding(0, Ui.dp(this, 8), 0, 0);
        text.addView(heroHint);
        hero.addView(text, new FrameLayout.LayoutParams(Ui.dp(this, 560), -1, Gravity.START));
        return hero;
    }

    private void showHero(VodItem v) {
        if (heroTitle == null) return;
        ImageLoader.loadBlurred(v.poster, heroArt);
        heroTitle.setText(v.name);
        java.util.Map<String, String> cats = v.series ? Repo.seriesCategories : Repo.movieCategories;
        String cat = cats == null ? null : cats.get(v.categoryId);
        StringBuilder meta = new StringBuilder();
        if (v.ratingText() != null) meta.append("★ ").append(v.ratingText()).append("   ");
        meta.append(v.series ? "Série" : "Filme");
        if (cat != null) meta.append("  •  ").append(cat);
        heroMeta.setText(meta);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Posters.focusListener = null;
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
        grid.setColumnWidth(Ui.dp(this, Ui.posterDp(this) + 12));
        grid.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
        grid.setVerticalSpacing(Ui.dp(this, 12));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        grid.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 20));
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

        TextView settingsTitle = Ui.text(this, "Configurações", 18, Palette.TEXT, true);
        settingsTitle.setPadding(Ui.dp(this, 4), Ui.dp(this, 24), 0, Ui.dp(this, 10));
        right.addView(settingsTitle);
        LinearLayout settings = Ui.column(this);
        settings.setBackground(Ui.round(Palette.SURFACE_HI, Ui.dp(this, 16)));
        settings.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), Ui.dp(this, 16));
        infoRow(settings, "Versão do app", Updater.currentName(this));
        final TextView adultButton = (TextView) button(adultLabel(), false);
        adultButton.setTextColor(Palette.TEXT);
        LinearLayout.LayoutParams abl = Ui.lp(-1, -2);
        abl.topMargin = Ui.dp(this, 8);
        settings.addView(adultButton, abl);
        adultButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final boolean hide = !Adult.hidden();
                Adult.askPin(VodActivity.this, hide ? "Ocultar conteúdo adulto" : "Mostrar conteúdo adulto",
                        new Runnable() {
                            public void run() {
                                Adult.setHidden(hide);
                                adultButton.setText(adultLabel());
                                Toast.makeText(VodActivity.this, hide ? "Conteúdo adulto oculto em todo o app"
                                        : "Conteúdo adulto visível (pede senha)", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        TextView pinButton = (TextView) button("ALTERAR SENHA DO CONTEÚDO ADULTO", false);
        pinButton.setTextColor(Palette.TEXT);
        LinearLayout.LayoutParams pbl = Ui.lp(-1, -2);
        pbl.topMargin = Ui.dp(this, 8);
        settings.addView(pinButton, pbl);
        pinButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { changePin(); }
        });
        View update = button("VERIFICAR ATUALIZAÇÕES", true);
        LinearLayout.LayoutParams ul = Ui.lp(-1, -2);
        ul.topMargin = Ui.dp(this, 8);
        settings.addView(update, ul);
        right.addView(settings, Ui.lp(-1, -2));
        update.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Updater.check(VodActivity.this, true); }
        });
        row.addView(right, Ui.weight(1.5f));

        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { refreshAll(); }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmLogout(); }
        });
        return scroll;
    }

    private static String adultLabel() {
        return Adult.hidden() ? "CONTEÚDO ADULTO: OCULTO  (tocar para mostrar)"
                : "CONTEÚDO ADULTO: COM SENHA  (tocar para ocultar)";
    }

    private void changePin() {
        Adult.askPin(this, "Senha atual", new Runnable() {
            public void run() {
                final android.widget.EditText input = new android.widget.EditText(VodActivity.this);
                input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER
                        | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                input.setGravity(Gravity.CENTER);
                input.setTextSize(24);
                LinearLayout box = new LinearLayout(VodActivity.this);
                box.setPadding(Ui.dp(VodActivity.this, 24), Ui.dp(VodActivity.this, 8), Ui.dp(VodActivity.this, 24), 0);
                box.addView(input, new LinearLayout.LayoutParams(-1, -2));
                new AlertDialog.Builder(VodActivity.this)
                        .setTitle("Nova senha (4 números ou mais)")
                        .setView(box)
                        .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                String pin = input.getText().toString();
                                if (pin.length() < 4) {
                                    Toast.makeText(VodActivity.this, "Use pelo menos 4 números", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Adult.setPin(pin);
                                Toast.makeText(VodActivity.this, "Senha alterada", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
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

        List<VodItem> visibleAll = Adult.filter(all);
        List<Section> out = new ArrayList<Section>();
        List<org.json.JSONObject> resume = new ArrayList<org.json.JSONObject>();
        for (org.json.JSONObject e : Repo.continueList(isSeries)) {
            String key = e.optString("key");
            VodItem v = Repo.findVod(isSeries, key.substring(key.indexOf(':') + 1));
            if (v == null || Adult.visible() || !Adult.isAdult(v)) resume.add(e);
        }
        if (!resume.isEmpty()) {
            Section c = new Section("Continuar assistindo", null);
            c.resume = resume;
            out.add(c);
        }

        Section genres = new Section("Por gênero", null);
        genres.genres = new ArrayList<Map.Entry<String, List<VodItem>>>();
        genres.genreArt = new ArrayList<String>();
        java.util.Set<String> usedArt = new java.util.HashSet<String>();
        for (Map.Entry<String, List<VodItem>> e : byCat.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            boolean adult = Adult.isAdultName(cats.get(e.getKey()));
            if (adult && Adult.hidden()) continue;
            genres.genres.add(e);
            // Locked adult tiles never show their artwork.
            genres.genreArt.add(adult && !Adult.visible() ? null : pickArt(e.getValue(), usedArt));
        }
        genres.catNames = cats;
        if (!genres.genres.isEmpty()) out.add(genres);

        List<VodItem> recent = new ArrayList<VodItem>(visibleAll);
        Collections.sort(recent, new Comparator<VodItem>() {
            public int compare(VodItem a, VodItem b) { return Long.compare(b.added, a.added); }
        });
        out.add(new Section("Adicionados recentemente", recent.subList(0, Math.min(100, recent.size()))));
        List<VodItem> top = new ArrayList<VodItem>(visibleAll);
        Collections.sort(top, new Comparator<VodItem>() {
            public int compare(VodItem a, VodItem b) { return Double.compare(b.rating, a.rating); }
        });
        out.add(new Section("Mais bem avaliados", top.subList(0, Math.min(100, top.size()))));
        for (Map.Entry<String, List<VodItem>> e : byCat.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            String name = cats.get(e.getKey());
            if (Adult.isAdultName(name) && !Adult.visible()) continue;
            out.add(new Section(name == null ? "Outros" : name, e.getValue()));
        }
        if (heroTitle != null && !visibleAll.isEmpty()) showHero(top.isEmpty() ? visibleAll.get(0) : top.get(0));
        sections.items = out;
        sections.notifyDataSetChanged();
        sectionsList.setSelection(0);
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

    private void refreshAll() {
        Toast.makeText(this, "Atualizando listas…", Toast.LENGTH_SHORT).show();
        Task.run(this, new Callable<Void>() {
            public Void call() throws Exception {
                if (!Repo.isXtream()) {
                    M3uCatalog.load(Repo.m3uUrl);
                    return null;
                }
                Xtream.loadLive();
                Repo.movies = null;
                Repo.series = null;
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
                sectionsList.setSelectionFromTop(target, Ui.dp(this, 4));
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
        List<org.json.JSONObject> resume;

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
        box.setBackground(Ui.stroke(Palette.SURFACE_HI, r, Ui.dp(this, 1), 0x33FFFFFF));
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

    /** An empty section row: header (accent, title, "see all") over a horizontal strip. */
    private LinearLayout newRow() {
        // The ListView "selects" whole rows on the remote; only the focused poster should light up.
        LinearLayout row = new LinearLayout(VodActivity.this) {
            @Override
            public void dispatchSetSelected(boolean selected) {}
        };
        row.setOrientation(LinearLayout.VERTICAL);
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
        // On a remote the header link would be an extra stop between rows; a tile ends the strip instead.
        more.setFocusable(!Ui.isTv(VodActivity.this));
        more.setClickable(true);
        more.setBackground(Ui.selectable(VodActivity.this, 0, 0x22FFFFFF, Ui.dp(VodActivity.this, 10), Palette.GOLD));
        more.setPadding(Ui.dp(VodActivity.this, 10), Ui.dp(VodActivity.this, 4), Ui.dp(VodActivity.this, 10),
                Ui.dp(VodActivity.this, 4));
        header.addView(more);
        row.addView(header, Ui.lp(-1, -2));
        HorizontalScrollView hs = new HorizontalScrollView(VodActivity.this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout strip = Ui.row(VodActivity.this);
        // Room around the posters so the focused one (slightly enlarged, gold ring) is never clipped.
        strip.setPadding(Ui.dp(VodActivity.this, 10), Ui.dp(VodActivity.this, 10), Ui.dp(VodActivity.this, 14),
                Ui.dp(VodActivity.this, 10));
        hs.addView(strip);
        row.addView(hs, Ui.lp(-1, -2));
        return row;
    }

    private final class SectionAdapter extends BaseAdapter {
        List<Section> items = new ArrayList<Section>();
        /** Built rows are kept (a few) so scrolling back does not rebuild every poster. */
        final java.util.LinkedHashMap<Section, View> rowCache = new java.util.LinkedHashMap<Section, View>(16, 0.75f, true) {
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
            final Section sec = items.get(pos);
            View cached = rowCache.get(sec);
            if (cached != null) return cached;
            LinearLayout row = newRow();
            rowCache.put(sec, row);
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
                    LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(VodActivity.this, 190), Ui.dp(VodActivity.this, 84));
                    lp.rightMargin = Ui.dp(VodActivity.this, 10);
                    strip.addView(tile, lp);
                }
            } else if (s.resume != null) {
                more.setVisibility(View.GONE);
                for (org.json.JSONObject e : s.resume) {
                    String key = e.optString("key");
                    boolean series = key.startsWith("series:");
                    VodItem v = Repo.findVod(series, key.substring(key.indexOf(':') + 1));
                    LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
                    lp.rightMargin = Ui.dp(VodActivity.this, 6);
                    strip.addView(Posters.continueCard(VodActivity.this, v, e, Ui.posterDp(VodActivity.this),
                            new Runnable() {
                                public void run() { showPage(page); }
                            }), lp);
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
                    strip.addView(Posters.card(VodActivity.this, s.items.get(i), Ui.posterDp(VodActivity.this)), lp);
                }
                if (s.items.size() > ROW_LIMIT) strip.addView(seeAllTile(s), Ui.lp(-2, -2));
            }
            return row;
        }
    }
}
