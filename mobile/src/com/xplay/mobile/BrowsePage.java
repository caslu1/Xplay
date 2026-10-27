package com.xplay.mobile;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Filmes / Séries: category chips over a poster grid. */
public final class BrowsePage extends LinearLayout {

    private final MainActivity a;
    private final boolean series;
    private final LinearLayout chips;
    private final HorizontalScrollView chipScroll;
    private final GridView grid;
    private final TextView empty;
    private List<VodItem> items = new ArrayList<VodItem>();
    private List<String[]> cats;
    /** Selected category id; null = all, "" = favorites. */
    private String selected;
    private int columns, cellWidth;

    public BrowsePage(MainActivity activity, boolean isSeries) {
        super(activity);
        a = activity;
        series = isSeries;
        setOrientation(VERTICAL);

        LinearLayout head = Ui.row(a);
        head.setPadding(Ui.dp(a, 20), Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 6));
        head.addView(Ui.text(a, series ? "Séries" : "Filmes", 28, Theme.TEXT, Ui.BOLD), Ui.weight(1));
        ImageView search = Ui.iconButton(a, R.drawable.ic_search, Theme.TEXT, 10);
        search.setContentDescription("Buscar");
        search.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { a.startActivity(new Intent(a, SearchActivity.class)); }
        });
        head.addView(search, Ui.lp(Ui.dp(a, 48), Ui.dp(a, 48)));
        addView(head, Ui.lp(-1, -2));

        chipScroll = new HorizontalScrollView(a);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setClipToPadding(false);
        chipScroll.setPadding(Ui.dp(a, 16), Ui.dp(a, 4), Ui.dp(a, 16), Ui.dp(a, 12));
        chips = Ui.row(a);
        chipScroll.addView(chips);
        addView(chipScroll, Ui.lp(-1, -2));

        FrameLayout body = new FrameLayout(a);
        grid = new GridView(a);
        int screen = Ui.screenWidth(a);
        int pad = Ui.dp(a, 14);
        columns = Math.max(3, Math.round((screen - 2 * pad) / (float) Ui.dp(a, 125)));
        int spacing = Ui.dp(a, 10);
        cellWidth = (screen - 2 * pad - spacing * (columns - 1)) / columns;
        grid.setNumColumns(columns);
        grid.setHorizontalSpacing(spacing);
        grid.setVerticalSpacing(Ui.dp(a, 16));
        grid.setStretchMode(GridView.NO_STRETCH);
        grid.setColumnWidth(cellWidth);
        grid.setPadding(pad, 0, pad, a.navSpace());
        grid.setClipToPadding(false);
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        grid.setVerticalScrollBarEnabled(false);
        grid.setAdapter(adapter);
        body.addView(grid, Ui.frame(-1, -1, Gravity.TOP));
        empty = Ui.text(a, "", 15, Theme.TEXT_MUTED);
        empty.setGravity(Gravity.CENTER);
        body.addView(empty, Ui.frame(-2, -2, Gravity.CENTER));
        addView(body, new LayoutParams(-1, 0, 1f));

        buildChips();
        show(null);
    }

    private void buildChips() {
        chips.removeAllViews();
        cats = Index.categories(series, Repo.showAdult());
        addChip("Todos", null);
        addChip("Favoritos", "");
        for (String[] c : cats) addChip(c[1], c[0]);
    }

    private void addChip(String label, final String id) {
        TextView t = Ui.text(a, label, 14, Theme.TEXT_DIM, Ui.MEDIUM);
        t.setPadding(Ui.dp(a, 16), Ui.dp(a, 9), Ui.dp(a, 16), Ui.dp(a, 9));
        t.setClickable(true);
        t.setFocusable(true);
        t.setTag(id == null ? "*" : id);
        t.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { show(id); }
        });
        LinearLayout.LayoutParams l = Ui.lp(-2, -2);
        l.rightMargin = Ui.dp(a, 8);
        chips.addView(t, l);
    }

    private void styleChips() {
        String key = selected == null ? "*" : selected;
        for (int i = 0; i < chips.getChildCount(); i++) {
            TextView t = (TextView) chips.getChildAt(i);
            boolean on = key.equals(t.getTag());
            t.setTextColor(on ? 0xFF111111 : Theme.TEXT_DIM);
            t.setBackground(Ui.ripple(on ? Ui.round(0xFFFFFFFF, Ui.dp(a, 20))
                    : Ui.stroke(Theme.SURFACE, Ui.dp(a, 20), Ui.dp(a, 1), Theme.STROKE), Ui.dp(a, 20)));
            if (on) {
                final View chip = t;
                chipScroll.post(new Runnable() {
                    public void run() {
                        chipScroll.smoothScrollTo(Math.max(0, chip.getLeft() - Ui.dp(a, 40)), 0);
                    }
                });
            }
        }
    }

    void selectCategory(String id) {
        show(id);
    }

    private void show(String id) {
        selected = id;
        if (id == null) {
            items = Index.recent(series);
        } else if (id.isEmpty()) {
            items = new ArrayList<VodItem>();
            for (String k : Repo.favOrder()) {
                VodItem v = Repo.findVod(k);
                if (v != null && v.series == series) items.add(v);
            }
        } else {
            items = Index.category(series, id);
        }
        styleChips();
        adapter.notifyDataSetChanged();
        grid.setSelection(0);
        empty.setText(id != null && id.isEmpty() ? "Toque e segure um pôster para favoritar"
                : series ? "Nenhuma série aqui" : "Nenhum filme aqui");
        empty.setVisibility(items.isEmpty() ? VISIBLE : GONE);
    }

    private final BaseAdapter adapter = new BaseAdapter() {
        public int getCount() { return items.size(); }

        public Object getItem(int i) { return items.get(i); }

        public long getItemId(int i) { return i; }

        public View getView(int i, View convert, ViewGroup parent) {
            // Cards are cheap to build; images come from the shared cache.
            return Cards.poster(a, items.get(i), cellWidth, true);
        }
    };
}
