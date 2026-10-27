package com.xplay.mobile;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Favoritos of the current profile: movies, series and channels. */
public final class FavoritesPage extends FrameLayout {

    private final MainActivity a;
    private final LinearLayout col;
    private final TextView empty;

    public FavoritesPage(MainActivity activity) {
        super(activity);
        a = activity;
        ScrollView scroll = new ScrollView(a);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, a.navSpace());
        scroll.setVerticalScrollBarEnabled(false);
        col = Ui.column(a);
        scroll.addView(col);
        addView(scroll, Ui.frame(-1, -1, Gravity.TOP));
        empty = Ui.text(a, "Seus favoritos aparecem aqui.\nToque no ♡ de um filme ou série, ou segure um canal.",
                15, Theme.TEXT_MUTED);
        empty.setGravity(Gravity.CENTER);
        empty.setLineSpacing(0, 1.2f);
        int p = Ui.dp(a, 32);
        empty.setPadding(p, 0, p, 0);
        addView(empty, Ui.frame(-2, -2, Gravity.CENTER));
    }

    void refresh() {
        col.removeAllViews();
        TextView title = Ui.text(a, "Favoritos", 28, Theme.TEXT, Ui.BOLD);
        title.setPadding(Ui.dp(a, 20), Ui.dp(a, 22), Ui.dp(a, 20), 0);
        col.addView(title);
        List<VodItem> movies = new ArrayList<VodItem>(), series = new ArrayList<VodItem>();
        final List<Channel> channels = new ArrayList<Channel>();
        for (String k : Repo.favOrder()) {
            VodItem v = Repo.findVod(k);
            if (v != null) (v.series ? series : movies).add(v);
            Channel c = Repo.findChannel(k);
            if (c != null) channels.add(c);
        }
        int w = Cards.rowPosterWidth(a);
        if (!movies.isEmpty()) {
            col.addView(Cards.sectionTitle(a, "Filmes", null));
            LinearLayout s = Cards.strip(a, col);
            for (VodItem v : movies) Cards.addToStrip(a, s, Cards.poster(a, v, w, true));
        }
        if (!series.isEmpty()) {
            col.addView(Cards.sectionTitle(a, "Séries", null));
            LinearLayout s = Cards.strip(a, col);
            for (VodItem v : series) Cards.addToStrip(a, s, Cards.poster(a, v, w, true));
        }
        if (!channels.isEmpty()) {
            col.addView(Cards.sectionTitle(a, "Canais", null));
            LinearLayout s = Cards.strip(a, col);
            int cw = Math.min((int) (Ui.screenWidth(a) / 3.3f), Ui.dp(a, 140));
            for (Channel c : channels) Cards.addToStrip(a, s, Cards.channel(a, c, channels, cw));
        }
        empty.setVisibility(movies.isEmpty() && series.isEmpty() && channels.isEmpty() ? VISIBLE : GONE);
    }
}
