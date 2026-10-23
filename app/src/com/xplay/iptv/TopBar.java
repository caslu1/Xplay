package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The app's top navigation: logo, section tabs as pills (the current one filled), search, settings
 * and a clock. Same on every main screen; transparent so a backdrop can show behind it.
 */
public class TopBar extends LinearLayout {

    public static final int LIVE = 0, MOVIES = 1, SERIES = 2, FAVORITES = 3, SEARCH = 4, ACCOUNT = 5, SPORTS = 6,
            KIDS = 7, NOVELAS = 8;
    static final String EXTRA_ITEM = "top_bar_item";

    public interface Listener {
        void onTopBar(int item);
    }

    private final List<TextView> tabs = new ArrayList<TextView>();
    private final List<Integer> tabIds = new ArrayList<Integer>();
    private final ImageView search, settings;
    private final TextView clock;
    private final SimpleDateFormat clockFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final Runnable tick = new Runnable() {
        public void run() {
            clock.setText(clockFormat.format(new Date()));
            postDelayed(this, 20000);
        }
    };

    public TopBar(Activity a, boolean withVod, final Listener listener) {
        super(a);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setPadding(Ui.dp(a, 24), Ui.dp(a, 10), Ui.dp(a, 20), Ui.dp(a, 6));

        addView(Ui.logo(a, 20));

        // Tabs scroll sideways on narrow screens instead of being cut off.
        HorizontalScrollView scroll = new HorizontalScrollView(a);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(a);
        row.setOrientation(HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(Ui.dp(a, 24), Ui.dp(a, 2), Ui.dp(a, 8), Ui.dp(a, 2));
        scroll.addView(row);
        addView(scroll, new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f));

        tab(a, row, LIVE, "TV", listener);
        tab(a, row, SPORTS, "Esportes", listener);
        if (withVod) {
            tab(a, row, MOVIES, "Filmes", listener);
            tab(a, row, SERIES, "Séries", listener);
            tab(a, row, NOVELAS, "Novelas", listener);
            tab(a, row, KIDS, "Infantis", listener);
        }
        tab(a, row, FAVORITES, "Favoritos", listener);

        search = icon(a, R.drawable.ic_search, "Buscar", SEARCH, listener);
        settings = icon(a, R.drawable.ic_settings, "Configurações", ACCOUNT, listener);
        clock = Ui.text(a, "", 17, 0xFFDDDDDD, false);
        clock.setPadding(Ui.dp(a, 14), 0, 0, 0);
        addView(clock);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        post(tick);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(tick);
    }

    private void tab(Activity a, LinearLayout row, final int id, String label, final Listener listener) {
        TextView t = Ui.text(a, label, 16, Palette.TEXT_DIM, true);
        t.setSingleLine(true);
        t.setFocusable(true);
        t.setClickable(true);
        t.setPadding(Ui.dp(a, 18), Ui.dp(a, 8), Ui.dp(a, 18), Ui.dp(a, 8));
        LayoutParams lp = Ui.lp(-2, -2);
        lp.rightMargin = Ui.dp(a, 4);
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
        LayoutParams lp = Ui.lp(Ui.dp(a, 42), Ui.dp(a, 42));
        lp.leftMargin = Ui.dp(a, 4);
        addView(v, lp);
        v.setOnClickListener(new OnClickListener() {
            public void onClick(View view) { listener.onTopBar(id); }
        });
        return v;
    }

    /** Fills the pill of the current section (search/settings light up their icon instead). */
    public void select(int id) {
        float r = Ui.dp(getContext(), 22);
        for (int i = 0; i < tabs.size(); i++) {
            boolean on = tabIds.get(i) == id;
            TextView t = tabs.get(i);
            t.setTextColor(on ? Palette.TEXT : 0xFFAAAAAA);
            t.setBackground(Ui.selectable(getContext(), on ? 0x40FFFFFF : 0, on ? 0x59FFFFFF : 0x26FFFFFF, r,
                    Palette.GOLD));
        }
        search.setColorFilter(id == SEARCH ? Palette.GOLD : Palette.TEXT);
        settings.setColorFilter(id == ACCOUNT ? Palette.GOLD : Palette.TEXT);
    }

    /** Moves the remote's focus to the current tab (Back from the content does this first). */
    public boolean focusSelected() {
        for (TextView t : tabs) {
            if (t.getCurrentTextColor() == Palette.TEXT) return t.requestFocus();
        }
        return !tabs.isEmpty() && tabs.get(0).requestFocus();
    }

    /** True while the focus is somewhere in this bar. */
    public boolean hasFocusInside() {
        return findFocus() != null;
    }

    /**
     * Goes to another section from any screen. Live TV is always the bottom of the stack; the other
     * sections replace each other on top of it, so Back always returns to Live TV.
     */
    public static void open(Activity from, int item) {
        boolean vodItem = item == MOVIES || item == SERIES || item == KIDS || item == NOVELAS
                || (Repo.hasVod() && (item == FAVORITES || item == SEARCH));
        Intent i;
        if (item == SPORTS) i = new Intent(from, SportsActivity.class);
        else if (item == ACCOUNT) i = new Intent(from, SettingsActivity.class);
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
