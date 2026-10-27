package com.xplay.mobile;

import android.content.Intent;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** Início: highlights carousel, "Continuar assistindo", newest titles, channels and category rows. */
public final class HomePage extends FrameLayout {

    private final MainActivity a;
    private final ScrollView scroll;
    private final LinearLayout col;
    private final LinearLayout continueBox;

    public HomePage(MainActivity activity) {
        super(activity);
        a = activity;
        scroll = new ScrollView(a);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, a.navSpace());
        col = Ui.column(a);
        scroll.addView(col);
        addView(scroll, Ui.frame(-1, -1, Gravity.TOP));

        col.addView(topBar());
        List<VodItem> hero = heroItems();
        if (!hero.isEmpty()) col.addView(new Carousel(a, hero), Ui.lp(-1, -2, 0, Ui.dp(a, 4), 0, 0));

        continueBox = Ui.column(a);
        col.addView(continueBox, Ui.lp(-1, -2));
        refreshContinue();

        final int w = Cards.rowPosterWidth(a);
        List<VodItem> movies = Index.recent(false);
        List<VodItem> series = Index.recent(true);
        if (!movies.isEmpty()) {
            posterRow("Filmes adicionados recentemente", movies.subList(0, Math.min(20, movies.size())), w,
                    new OnClickListener() {
                        public void onClick(View v) { a.openCategory(false, null); }
                    });
        }
        if (!series.isEmpty()) {
            posterRow("Séries novas", series.subList(0, Math.min(20, series.size())), w, new OnClickListener() {
                public void onClick(View v) { a.openCategory(true, null); }
            });
        }
        List<VodItem> top = Index.topRated(false, 20);
        if (top.size() >= 5) posterRow("Mais bem avaliados", top, w, null);
        channelsRow();

        int movieRows = 0, seriesRows = 0;
        for (final String[] c : Index.categories(false, false)) {
            List<VodItem> items = Index.category(false, c[0]);
            if (items.size() < 4) continue;
            posterRow(c[1], items.subList(0, Math.min(20, items.size())), w, new OnClickListener() {
                public void onClick(View v) { a.openCategory(false, c[0]); }
            });
            if (++movieRows == 6) break;
        }
        for (final String[] c : Index.categories(true, false)) {
            List<VodItem> items = Index.category(true, c[0]);
            if (items.size() < 4) continue;
            posterRow(c[1], items.subList(0, Math.min(20, items.size())), w, new OnClickListener() {
                public void onClick(View v) { a.openCategory(true, c[0]); }
            });
            if (++seriesRows == 4) break;
        }
    }

    void scrollTop() {
        scroll.smoothScrollTo(0, 0);
    }

    private View topBar() {
        LinearLayout bar = Ui.row(a);
        bar.setPadding(Ui.dp(a, 18), Ui.dp(a, 14), Ui.dp(a, 12), Ui.dp(a, 10));
        int s = Ui.dp(a, 46);
        FrameLayout avatar = Ui.avatar(a, Repo.profile(), s);
        avatar.setBackground(Ui.round(Repo.profile() == null ? Theme.AVATAR_BG[0] : Repo.profile().color, s / 2f));
        avatar.setClickable(true);
        avatar.setForeground(Ui.ripple(null, s / 2f));
        avatar.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { a.switchProfile(); }
        });
        bar.addView(avatar, Ui.lp(s, s));
        bar.addView(new View(a), Ui.weight(1));
        ImageView search = Ui.iconButton(a, R.drawable.ic_search, Theme.TEXT, 10);
        search.setContentDescription("Buscar");
        search.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { a.startActivity(new Intent(a, SearchActivity.class)); }
        });
        bar.addView(search, Ui.lp(Ui.dp(a, 48), Ui.dp(a, 48)));
        return bar;
    }

    /** Newest movies and series that have a poster, mixed: 3 movies, then 2 series… */
    private List<VodItem> heroItems() {
        List<VodItem> out = new ArrayList<VodItem>();
        List<VodItem> m = withPoster(Index.recent(false), 6);
        List<VodItem> s = withPoster(Index.recent(true), 4);
        int i = 0, j = 0;
        while (out.size() < 10 && (i < m.size() || j < s.size())) {
            for (int k = 0; k < 3 && i < m.size(); k++) out.add(m.get(i++));
            for (int k = 0; k < 2 && j < s.size(); k++) out.add(s.get(j++));
        }
        return out;
    }

    private static List<VodItem> withPoster(List<VodItem> list, int max) {
        List<VodItem> out = new ArrayList<VodItem>();
        for (VodItem v : list) {
            if (v.poster == null) continue;
            out.add(v);
            if (out.size() == max) break;
        }
        return out;
    }

    void refreshContinue() {
        continueBox.removeAllViews();
        List<JSONObject> list = Repo.continueList();
        if (list.isEmpty()) return;
        continueBox.addView(Cards.sectionTitle(a, "Continuar assistindo", null));
        LinearLayout strip = Cards.strip(a, continueBox);
        int w = Math.min((int) (Ui.screenWidth(a) / 2.85f), Ui.dp(a, 170));
        for (int i = 0; i < list.size() && i < 20; i++) Cards.addToStrip(a, strip, Cards.continueCard(a, list.get(i), w));
    }

    private void posterRow(String title, List<VodItem> items, int w, OnClickListener more) {
        col.addView(Cards.sectionTitle(a, title, more));
        LinearLayout strip = Cards.strip(a, col);
        for (VodItem v : items) Cards.addToStrip(a, strip, Cards.poster(a, v, w, true));
    }

    private void channelsRow() {
        if (Repo.channels == null || Repo.channels.isEmpty()) return;
        final List<Channel> list = new ArrayList<Channel>();
        for (String k : Repo.favOrder()) {
            Channel c = Repo.findChannel(k);
            if (c != null) list.add(c);
        }
        String title = list.isEmpty() ? "TV ao vivo" : "Seus canais";
        if (list.isEmpty()) {
            for (Channel c : Repo.channels) {
                if (Index.adultName(c.group)) continue;
                list.add(c);
                if (list.size() == 20) break;
            }
        }
        col.addView(Cards.sectionTitle(a, title, new OnClickListener() {
            public void onClick(View v) { a.select(MainActivity.TAB_LIVE); }
        }));
        LinearLayout strip = Cards.strip(a, col);
        int w = Math.min((int) (Ui.screenWidth(a) / 3.3f), Ui.dp(a, 140));
        for (Channel c : list) Cards.addToStrip(a, strip, Cards.channel(a, c, list, w));
    }
}
