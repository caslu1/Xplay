package com.xplay.mobile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

/** Home screen with the floating bottom bar: Início, Filmes, Séries, Ao vivo, Favoritos, Ajustes. */
public class MainActivity extends Activity {

    static final int TAB_HOME = 0, TAB_MOVIES = 1, TAB_SERIES = 2, TAB_LIVE = 3, TAB_FAVORITES = 4, TAB_SETTINGS = 5;

    private static final String[] LABELS = {"Início", "Filmes", "Séries", "Ao vivo", "Favoritos", "Ajustes"};
    private static final int[] ICONS = {R.drawable.ic_home, R.drawable.ic_movie_outline, R.drawable.ic_tv,
            R.drawable.ic_live, R.drawable.ic_heart, R.drawable.ic_settings};

    private FrameLayout root, content;
    private LinearLayout nav;
    private final View[] pages = new View[LABELS.length];
    private final LinearLayout[] navItems = new LinearLayout[LABELS.length];
    private int tab = -1;
    private View loadingView;
    private TextView loadingStatus;
    private boolean ready;
    private final Catalog.Listener onLoaded = new Catalog.Listener() {
        public void loaded(Exception e) {
            if (e != null) showError(e);
            else showReady();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (!Repo.hasLogin()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (Repo.profile() == null && !Repo.profiles().isEmpty()) {
            startActivity(new Intent(this, ProfilesActivity.class));
            finish();
            return;
        }
        root = new FrameLayout(this);
        root.setBackgroundColor(Theme.BG);
        content = new FrameLayout(this);
        root.addView(content, Ui.frame(-1, -1, Gravity.TOP));
        nav = buildNav();
        FrameLayout.LayoutParams nl = Ui.frame(-1, Ui.dp(this, 66), Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        int m = Ui.dp(this, 12);
        nl.setMargins(m, 0, m, Ui.dp(this, 10));
        if (Ui.landscape(this)) {
            nl.width = Math.min(Ui.dp(this, 560), Ui.screenWidth(this) - 2 * m);
        }
        root.addView(nav, nl);
        setContentView(root);
        showLoading();
        Catalog.whenLoaded(onLoaded);
    }

    // ---------------------------------------------------------------- loading / error

    private void showLoading() {
        nav.setVisibility(View.GONE);
        LinearLayout c = Ui.column(this);
        c.setGravity(Gravity.CENTER);
        c.addView(Ui.logo(this, 32));
        ProgressBar bar = new ProgressBar(this);
        bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Theme.PINK));
        c.addView(bar, Ui.lp(Ui.dp(this, 40), Ui.dp(this, 40), 0, Ui.dp(this, 36), 0, 0));
        loadingStatus = Ui.text(this, Catalog.status().isEmpty() ? "Carregando sua lista…" : Catalog.status(), 14,
                Theme.TEXT_MUTED);
        c.addView(loadingStatus, Ui.lp(-2, -2, 0, Ui.dp(this, 16), 0, 0));
        Catalog.onStatus(new Catalog.Progress() {
            public void step(String what) { loadingStatus.setText(what); }
        });
        setOverlay(c);
    }

    private void setOverlay(View v) {
        if (loadingView != null) root.removeView(loadingView);
        loadingView = v;
        if (v != null) root.addView(v, 0, Ui.frame(-1, -1, Gravity.CENTER));
    }

    private void showError(Exception e) {
        Catalog.onStatus(null);
        LinearLayout c = Ui.column(this);
        c.setGravity(Gravity.CENTER);
        int p = Ui.dp(this, 32);
        c.setPadding(p, p, p, p);
        c.addView(Ui.logo(this, 30));
        c.addView(Ui.text(this, "Não foi possível carregar a lista", 19, Theme.TEXT, Ui.MEDIUM),
                Ui.lp(-2, -2, 0, Ui.dp(this, 32), 0, 0));
        TextView msg = Ui.text(this, Task.message(e), 15, Theme.TEXT_DIM);
        msg.setGravity(Gravity.CENTER);
        c.addView(msg, Ui.lp(-2, -2, 0, Ui.dp(this, 8), 0, 0));
        TextView retry = Sheet.button(this, "Tentar de novo", true);
        c.addView(retry, Ui.lp(Ui.dp(this, 240), -2, 0, Ui.dp(this, 28), 0, 0));
        TextView change = Sheet.button(this, "Trocar login / lista", false);
        c.addView(change, Ui.lp(Ui.dp(this, 240), -2, 0, Ui.dp(this, 12), 0, 0));
        retry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showLoading();
                Catalog.reload();
                Catalog.whenLoaded(onLoaded);
            }
        });
        change.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { editLogin(); }
        });
        setOverlay(c);
    }

    private void showReady() {
        Catalog.onStatus(null);
        setOverlay(null);
        ready = true;
        for (int i = 0; i < pages.length; i++) pages[i] = null;
        content.removeAllViews();
        nav.setVisibility(View.VISIBLE);
        int start = tab < 0 ? TAB_HOME : tab;
        tab = -1;
        select(start);
    }

    // ---------------------------------------------------------------- tabs

    private LinearLayout buildNav() {
        LinearLayout bar = Ui.row(this);
        bar.setBackground(Ui.stroke(0xF21A1A1E, Ui.dp(this, 26), Ui.dp(this, 1), 0x1FFFFFFF));
        bar.setElevation(Ui.dp(this, 8));
        int p = Ui.dp(this, 4);
        bar.setPadding(p, 0, p, 0);
        for (int i = 0; i < LABELS.length; i++) {
            final int index = i;
            LinearLayout item = Ui.column(this);
            item.setGravity(Gravity.CENTER);
            item.setClickable(true);
            item.setFocusable(true);
            item.setBackground(Ui.pressable(this, 0, Ui.dp(this, 22)));
            View glow = new View(this);
            item.addView(glow, Ui.lp(Ui.dp(this, 26), Ui.dp(this, 3), 0, 0, 0, Ui.dp(this, 6)));
            ImageView icon = Ui.icon(this, ICONS[i], Theme.TEXT_MUTED);
            item.addView(icon, Ui.lp(Ui.dp(this, 25), Ui.dp(this, 25)));
            TextView label = Ui.oneLine(Ui.text(this, LABELS[i], 11, Theme.TEXT_MUTED, Ui.MEDIUM));
            item.addView(label, Ui.lp(-2, -2, 0, Ui.dp(this, 3), 0, Ui.dp(this, 6)));
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { select(index); }
            });
            bar.addView(item, new LinearLayout.LayoutParams(0, -1, 1f));
            navItems[i] = item;
        }
        return bar;
    }

    void select(int t) {
        if (!ready) return;
        if (t == tab) {
            if (pages[t] instanceof HomePage) ((HomePage) pages[t]).scrollTop();
            return;
        }
        tab = t;
        for (int i = 0; i < navItems.length; i++) {
            boolean on = i == t;
            LinearLayout item = navItems[i];
            item.getChildAt(0).setBackground(on ? Ui.round(Theme.PINK, Ui.dp(this, 2)) : null);
            ((ImageView) item.getChildAt(1)).setColorFilter(on ? Theme.TEXT : Theme.TEXT_MUTED);
            ((ImageView) item.getChildAt(1)).setImageResource(i == TAB_FAVORITES && on ? R.drawable.ic_heart_fill
                    : i == TAB_MOVIES && on ? R.drawable.ic_movie : ICONS[i]);
            ((TextView) item.getChildAt(2)).setTextColor(on ? Theme.TEXT : Theme.TEXT_MUTED);
        }
        if (pages[t] == null) pages[t] = buildPage(t);
        content.removeAllViews();
        content.addView(pages[t], Ui.frame(-1, -1, Gravity.TOP));
        if (pages[t] instanceof FavoritesPage) ((FavoritesPage) pages[t]).refresh();
        if (pages[t] instanceof SettingsPage) ((SettingsPage) pages[t]).refresh();
    }

    private View buildPage(int t) {
        switch (t) {
            case TAB_MOVIES: return new BrowsePage(this, false);
            case TAB_SERIES: return new BrowsePage(this, true);
            case TAB_LIVE: return new LivePage(this);
            case TAB_FAVORITES: return new FavoritesPage(this);
            case TAB_SETTINGS: return new SettingsPage(this);
            default: return new HomePage(this);
        }
    }

    /** Opens the movies or series tab on a category ("Ver tudo" on the home). */
    void openCategory(boolean series, String categoryId) {
        int t = series ? TAB_SERIES : TAB_MOVIES;
        if (pages[t] == null) pages[t] = buildPage(t);
        ((BrowsePage) pages[t]).selectCategory(categoryId);
        select(t);
    }

    /** Bottom space pages leave free so the floating bar never covers content. */
    int navSpace() {
        return Ui.dp(this, 92);
    }

    void refreshHome() {
        if (pages[TAB_HOME] instanceof HomePage) ((HomePage) pages[TAB_HOME]).refreshContinue();
    }

    void reloadAll() {
        ready = false;
        tab = TAB_HOME;
        content.removeAllViews();
        showLoading();
        Catalog.reload();
        Catalog.whenLoaded(onLoaded);
    }

    /** Rebuilds the catalog tabs (after the adult filter changes), staying on the current tab. */
    void reloadPages() {
        for (int i = 0; i < pages.length; i++) if (i != tab) pages[i] = null;
    }

    void editLogin() {
        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra("edit", true);
        startActivity(i);
    }

    void switchProfile() {
        startActivity(new Intent(this, ProfilesActivity.class));
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!Repo.hasLogin()) {
            // The login was changed (and failed) from the login screen.
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        if (ready && !Repo.catalogsLoaded()) {
            reloadAll();
            return;
        }
        if (!ready) return;
        refreshHome();
        if (pages[TAB_FAVORITES] != null && tab == TAB_FAVORITES) ((FavoritesPage) pages[TAB_FAVORITES]).refresh();
    }

    @Override
    public void onBackPressed() {
        if (ready && tab == TAB_LIVE && pages[TAB_LIVE] instanceof LivePage && ((LivePage) pages[TAB_LIVE]).back()) return;
        if (ready && tab != TAB_HOME) {
            select(TAB_HOME);
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Catalog.forget(onLoaded);
        Catalog.onStatus(null);
    }
}
