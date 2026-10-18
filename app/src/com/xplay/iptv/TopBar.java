package com.xplay.iptv;

import android.app.Activity;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** The app's top navigation: logo, section tabs, search and account. Same on every main screen. */
public class TopBar extends LinearLayout {

    public static final int LIVE = 0, MOVIES = 1, SERIES = 2, FAVORITES = 3, SEARCH = 4, ACCOUNT = 5;

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
        setPadding(Ui.dp(a, 24), Ui.dp(a, 8), Ui.dp(a, 16), Ui.dp(a, 4));

        addView(Ui.logo(a, 22));
        View gap = new View(a);
        addView(gap, Ui.lp(Ui.dp(a, 32), 0));

        tab(a, LIVE, "TV AO VIVO", listener);
        if (withVod) {
            tab(a, MOVIES, "FILMES", listener);
            tab(a, SERIES, "SÉRIES", listener);
        }
        tab(a, FAVORITES, "FAVORITOS", listener);

        addView(new View(a), Ui.spacer());
        search = icon(a, R.drawable.ic_search, "Buscar", SEARCH, listener);
        account = icon(a, R.drawable.ic_person, "Conta", ACCOUNT, listener);
    }

    private void tab(Activity a, final int id, String label, final Listener listener) {
        LinearLayout t = new LinearLayout(a);
        t.setOrientation(VERTICAL);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(Ui.selectable(a, 0, Palette.SURFACE_HI, Ui.dp(a, 10), Palette.GOLD));
        t.setPadding(Ui.dp(a, 14), Ui.dp(a, 8), Ui.dp(a, 14), Ui.dp(a, 4));
        TextView text = Ui.text(a, label, 15, Palette.TEXT_DIM, true);
        t.addView(text);
        View bar = new View(a);
        LayoutParams bl = Ui.lp(Ui.dp(a, 24), Ui.dp(a, 3));
        bl.topMargin = Ui.dp(a, 5);
        t.addView(bar, bl);
        LayoutParams lp = Ui.lp(-2, -2);
        lp.rightMargin = Ui.dp(a, 4);
        addView(t, lp);
        tabs.add(t);
        tabIds.add(id);
        t.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { listener.onTopBar(id); }
        });
    }

    private ImageView icon(Activity a, int res, String desc, final int id, final Listener listener) {
        ImageView v = Ui.iconButton(a, res, Palette.TEXT, Palette.GOLD);
        v.setContentDescription(desc);
        LayoutParams lp = Ui.lp(Ui.dp(a, 46), Ui.dp(a, 46));
        lp.leftMargin = Ui.dp(a, 6);
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
}
