package com.xplay.mobile;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/** Searches movies, series and channels by name (accents and case ignored). */
public class SearchActivity extends Activity {

    private static final int MAX = 60;

    private EditText query;
    private LinearLayout results;
    private TextView hint;
    private int generation;
    private final Runnable search = new Runnable() {
        public void run() { runSearch(); }
    };

    private static final class Found {
        final List<VodItem> movies = new ArrayList<VodItem>();
        final List<VodItem> series = new ArrayList<VodItem>();
        final List<Channel> channels = new ArrayList<Channel>();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (!Repo.catalogsLoaded()) {
            finish();
            return;
        }
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Theme.BG);

        LinearLayout bar = Ui.row(this);
        bar.setPadding(Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 16), Ui.dp(this, 10));
        ImageView back = Ui.iconButton(this, R.drawable.ic_back, Theme.TEXT, 10);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        bar.addView(back, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        query = Sheet.field(this, "Buscar filmes, séries e canais", "", InputType.TYPE_CLASS_TEXT);
        query.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        query.setCompoundDrawablePadding(Ui.dp(this, 10));
        android.graphics.drawable.Drawable lens = getDrawable(R.drawable.ic_search).mutate();
        lens.setTint(Theme.TEXT_MUTED);
        lens.setBounds(0, 0, Ui.dp(this, 20), Ui.dp(this, 20));
        query.setCompoundDrawables(lens, null, null, null);
        LinearLayout.LayoutParams ql = Ui.weight(1);
        ql.leftMargin = Ui.dp(this, 4);
        bar.addView(query, ql);
        root.addView(bar, Ui.lp(-1, -2));

        FrameLayout body = new FrameLayout(this);
        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, Ui.dp(this, 24));
        results = Ui.column(this);
        scroll.addView(results);
        body.addView(scroll, Ui.frame(-1, -1, Gravity.TOP));
        hint = Ui.text(this, "Digite pelo menos 2 letras", 15, Theme.TEXT_MUTED);
        hint.setGravity(Gravity.CENTER);
        body.addView(hint, Ui.frame(-2, -2, Gravity.CENTER));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);

        query.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}

            public void onTextChanged(CharSequence s, int a, int b, int c) {}

            public void afterTextChanged(Editable s) {
                query.removeCallbacks(search);
                query.postDelayed(search, 280);
            }
        });
        query.requestFocus();
    }

    private void runSearch() {
        final String q = Index.normalize(query.getText().toString().trim());
        final int gen = ++generation;
        if (q.length() < 2) {
            results.removeAllViews();
            hint.setText("Digite pelo menos 2 letras");
            hint.setVisibility(View.VISIBLE);
            return;
        }
        final boolean adult = Repo.showAdult();
        Task.run(this, new Callable<Found>() {
            public Found call() {
                Found f = new Found();
                for (VodItem v : Repo.movies) {
                    if (f.movies.size() < MAX && (adult || !Index.adult(v)) && Index.normalize(v.name).contains(q)) f.movies.add(v);
                }
                for (VodItem v : Repo.series) {
                    if (f.series.size() < MAX && (adult || !Index.adult(v)) && Index.normalize(v.name).contains(q)) f.series.add(v);
                }
                for (Channel c : Repo.channels) {
                    if (f.channels.size() < MAX && (adult || !Index.adultName(c.group))
                            && Index.normalize(c.name).contains(q)) f.channels.add(c);
                }
                return f;
            }
        }, new Task.Done<Found>() {
            public void ok(Found f) {
                if (gen == generation) show(f);
            }

            public void fail(Exception e) {}
        });
    }

    private void show(Found f) {
        results.removeAllViews();
        boolean none = f.movies.isEmpty() && f.series.isEmpty() && f.channels.isEmpty();
        hint.setText("Nada encontrado");
        hint.setVisibility(none ? View.VISIBLE : View.GONE);
        grid("Filmes", f.movies);
        grid("Séries", f.series);
        if (!f.channels.isEmpty()) {
            results.addView(Cards.sectionTitle(this, "Canais", null));
            LinearLayout s = Cards.strip(this, results);
            int w = Math.min((int) (Ui.screenWidth(this) / 3.3f), Ui.dp(this, 140));
            for (Channel c : f.channels) Cards.addToStrip(this, s, Cards.channel(this, c, f.channels, w));
        }
    }

    private void grid(String title, List<VodItem> items) {
        if (items.isEmpty()) return;
        results.addView(Cards.sectionTitle(this, title + " (" + items.size() + (items.size() == MAX ? "+" : "") + ")", null));
        int pad = Ui.dp(this, 14), gap = Ui.dp(this, 10);
        int cols = Math.max(3, Math.round((Ui.screenWidth(this) - 2 * pad) / (float) Ui.dp(this, 125)));
        int w = (Ui.screenWidth(this) - 2 * pad - gap * (cols - 1)) / cols;
        LinearLayout row = null;
        for (int i = 0; i < items.size(); i++) {
            if (i % cols == 0) {
                row = Ui.row(this);
                row.setGravity(Gravity.TOP);
                row.setPadding(pad, 0, pad, Ui.dp(this, 14));
                results.addView(row, Ui.lp(-1, -2));
            }
            LinearLayout.LayoutParams l = Ui.lp(w, -2);
            if (i % cols != 0) l.leftMargin = gap;
            row.addView(Cards.poster(this, items.get(i), w, true), l);
        }
    }
}
