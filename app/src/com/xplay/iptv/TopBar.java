package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** The app's top navigation: logo, section tabs, search and account. Same on every main screen. */
public class TopBar extends LinearLayout {

    public static final int LIVE = 0, MOVIES = 1, SERIES = 2, FAVORITES = 3, SEARCH = 4, ACCOUNT = 5, SPORTS = 6;
    static final String EXTRA_ITEM = "top_bar_item";

    public interface Listener {
        void onTopBar(int item);
    }

    private final List<View> tabs = new ArrayList<View>();
    private final List<Integer> tabIds = new ArrayList<Integer>();
    private final ImageView search, account;

    public TopBar(Activity a, boolean withVod, final Listener listener) {
        super(a);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(Ui.dp(a, 20), Ui.dp(a, 6), Ui.dp(a, 12), Ui.dp(a, 2));

        addView(Ui.logo(a, 20));

        // Tabs scroll sideways on narrow screens instead of being cut off.
        HorizontalScrollView scroll = new HorizontalScrollView(a);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setFillViewport(false);
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(a, 20), 0, Ui.dp(a, 8), 0);
        scroll.addView(row);
        LayoutParams sl = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
        addView(scroll, sl);

        tab(a, row, LIVE, "TV", listener);
        tab(a, row, SPORTS, "ESPORTES", listener);
        if (withVod) {
            tab(a, row, MOVIES, "FILMES", listener);
            tab(a, row, SERIES, "SÉRIES", listener);
        }
        tab(a, row, FAVORITES, "FAVORITOS", listener);

        search = icon(a, R.drawable.ic_search, "Buscar", SEARCH, listener);
        account = icon(a, R.drawable.ic_person, "Conta", ACCOUNT, listener);
    }

    private void tab(Activity a, LinearLayout row, final int id, String label, final Listener listener) {
        LinearLayout t = new LinearLayout(a);
        t.setOrientation(VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(Ui.selectable(a, 0, Palette.SURFACE_HI, Ui.dp(a, 10), Palette.GOLD));
        t.setPadding(Ui.dp(a, 12), Ui.dp(a, 8), Ui.dp(a, 12), Ui.dp(a, 4));
        TextView text = Ui.text(a, label, 15, Palette.TEXT_DIM, true);
        text.setSingleLine(true);
        t.addView(text, Ui.lp(-2, -2));
        View bar = new View(a);
        LayoutParams bl = Ui.lp(Ui.dp(a, 22), Ui.dp(a, 3));
        bl.topMargin = Ui.dp(a, 5);
        t.addView(bar, bl);
        LayoutParams lp = Ui.lp(-2, -2);
        lp.rightMargin = Ui.dp(a, 2);
        row.addView(t, lp);
        tabs.add(t);
        tabIds.add(id);
        t.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { listener.onTopBar(id); }
        });
    }

    private ImageView icon(Activity a, int res, String desc, final int id, final Listener listener) {
        ImageView v = Ui.iconButton(a, res, Palette.TEXT, Palette.GOLD);
        v.setContentDescription(desc);
        LayoutParams lp = Ui.lp(Ui.dp(a, 44), Ui.dp(a, 44));
        lp.leftMargin = Ui.dp(a, 4);
        addView(v, lp);
        v.setOnClickListener(new OnClickListener() {
            public void onClick(View view) { listener.onTopBar(id); }
        });
        return v;
    }

    /** Highlights the current section (a tab, or the search/account icon). */
    public void select(int id) {
        for (int i = 0; i < tabs.size(); i++) {
            boolean on = tabIds.get(i) == id;
            LinearLayout t = (LinearLayout) tabs.get(i);
            ((TextView) t.getChildAt(0)).setTextColor(on ? Palette.GOLD : Palette.TEXT_DIM);
            t.getChildAt(1).setBackground(on ? Ui.round(Palette.GOLD, Ui.dp(getContext(), 2)) : null);
        }
        search.setColorFilter(id == SEARCH ? Palette.GOLD : Palette.TEXT);
        account.setColorFilter(id == ACCOUNT ? Palette.GOLD : Palette.TEXT);
    }

    /**
     * Goes to another section from any screen. Live TV is always the bottom of the stack; the other
     * sections replace each other on top of it, so Back always returns to Live TV.
     */
    public static void open(Activity from, int item) {
        boolean vodItem = item == MOVIES || item == SERIES
                || (Repo.hasVod() && (item == FAVORITES || item == SEARCH))
                || (Repo.isXtream() && item == ACCOUNT);
        Intent i;
        if (item == SPORTS) i = new Intent(from, SportsActivity.class);
        else if (vodItem) i = new Intent(from, VodActivity.class).putExtra(VodActivity.EXTRA_TAB, item);
        else {
            i = new Intent(from, LiveActivity.class).putExtra(EXTRA_ITEM, item);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
        boolean same = i.getComponent().getClassName().equals(from.getClass().getName());
        if (same) return;
        from.startActivity(i);
        if (!(from instanceof LiveActivity)) from.finish();
        from.overridePendingTransition(0, 0);
    }
}
