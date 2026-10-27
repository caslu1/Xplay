package com.xplay.mobile;

import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Ao vivo: group chips over the channel list, with what is on now when the server has a guide. */
public final class LivePage extends LinearLayout {

    private final MainActivity a;
    private final LinearLayout chips;
    private final HorizontalScrollView chipScroll;
    private final ListView list;
    private final TextView empty;
    private List<Channel> channels = new ArrayList<Channel>();
    /** Selected group; null = all, "" = favorites. */
    private String selected;
    private boolean picked;

    public LivePage(MainActivity activity) {
        super(activity);
        a = activity;
        setOrientation(VERTICAL);

        LinearLayout head = Ui.row(a);
        head.setPadding(Ui.dp(a, 20), Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 6));
        head.addView(Ui.text(a, "TV ao vivo", 28, Theme.TEXT, Ui.BOLD), Ui.weight(1));
        ImageView search = Ui.iconButton(a, R.drawable.ic_search, Theme.TEXT, 10);
        search.setContentDescription("Buscar");
        search.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                a.startActivity(new android.content.Intent(a, SearchActivity.class));
            }
        });
        head.addView(search, Ui.lp(Ui.dp(a, 48), Ui.dp(a, 48)));
        addView(head, Ui.lp(-1, -2));

        chipScroll = new HorizontalScrollView(a);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setClipToPadding(false);
        chipScroll.setPadding(Ui.dp(a, 16), Ui.dp(a, 4), Ui.dp(a, 16), Ui.dp(a, 10));
        chips = Ui.row(a);
        chipScroll.addView(chips);
        addView(chipScroll, Ui.lp(-1, -2));

        FrameLayout body = new FrameLayout(a);
        list = new ListView(a);
        list.setDivider(null);
        list.setSelector(new android.graphics.drawable.ColorDrawable(0));
        list.setClipToPadding(false);
        list.setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 10), a.navSpace());
        list.setVerticalScrollBarEnabled(false);
        list.setAdapter(adapter);
        body.addView(list, Ui.frame(-1, -1, Gravity.TOP));
        empty = Ui.text(a, "", 15, Theme.TEXT_MUTED);
        empty.setGravity(Gravity.CENTER);
        body.addView(empty, Ui.frame(-2, -2, Gravity.CENTER));
        addView(body, new LayoutParams(-1, 0, 1f));

        addChip("Todos", null);
        addChip("Favoritos", "");
        for (String g : Index.liveGroups(Repo.showAdult())) addChip(g, g);
        show(null);
        picked = false;
    }

    /** Back from a group returns to "Todos" before leaving the tab. */
    boolean back() {
        if (!picked) return false;
        show(null);
        picked = false;
        return true;
    }

    private void addChip(String label, final String id) {
        TextView t = Ui.text(a, label, 14, Theme.TEXT_DIM, Ui.MEDIUM);
        t.setPadding(Ui.dp(a, 16), Ui.dp(a, 9), Ui.dp(a, 16), Ui.dp(a, 9));
        t.setClickable(true);
        t.setFocusable(true);
        t.setTag(id == null ? "*" : id);
        t.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                show(id);
                picked = id != null;
            }
        });
        LinearLayout.LayoutParams l = Ui.lp(-2, -2);
        l.rightMargin = Ui.dp(a, 8);
        chips.addView(t, l);
    }

    private void show(String group) {
        selected = group;
        channels = new ArrayList<Channel>();
        if (Repo.channels != null) {
            if (group == null) {
                for (Channel c : Repo.channels) if (Repo.showAdult() || !Index.adultName(c.group)) channels.add(c);
            } else if (group.isEmpty()) {
                for (String k : Repo.favOrder()) {
                    Channel c = Repo.findChannel(k);
                    if (c != null) channels.add(c);
                }
            } else {
                for (Channel c : Repo.channels) if (c.group.equals(group)) channels.add(c);
            }
        }
        String key = group == null ? "*" : group;
        for (int i = 0; i < chips.getChildCount(); i++) {
            TextView t = (TextView) chips.getChildAt(i);
            boolean on = key.equals(t.getTag());
            t.setTextColor(on ? 0xFF111111 : Theme.TEXT_DIM);
            t.setBackground(Ui.ripple(on ? Ui.round(0xFFFFFFFF, Ui.dp(a, 20))
                    : Ui.stroke(Theme.SURFACE, Ui.dp(a, 20), Ui.dp(a, 1), Theme.STROKE), Ui.dp(a, 20)));
            if (on) {
                final View chip = t;
                chipScroll.post(new Runnable() {
                    public void run() { chipScroll.smoothScrollTo(Math.max(0, chip.getLeft() - Ui.dp(a, 40)), 0); }
                });
            }
        }
        adapter.notifyDataSetChanged();
        list.setSelection(0);
        empty.setText(group != null && group.isEmpty() ? "Toque e segure um canal para favoritar" : "Nenhum canal aqui");
        empty.setVisibility(channels.isEmpty() ? VISIBLE : GONE);
    }

    private View row(final int i) {
        final Channel c = channels.get(i);
        LinearLayout row = Ui.row(a);
        row.setPadding(Ui.dp(a, 10), Ui.dp(a, 8), Ui.dp(a, 10), Ui.dp(a, 8));
        row.setBackground(Ui.pressable(a, 0, Ui.dp(a, 14)));
        FrameLayout box = new FrameLayout(a);
        box.setBackground(Ui.round(Theme.SURFACE_HI, Ui.dp(a, 12)));
        ImageView logo = new ImageView(a);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int p = Ui.dp(a, 7);
        logo.setPadding(p, p, p, p);
        box.addView(logo, Ui.frame(-1, -1, Gravity.CENTER));
        ImageLoader.load(c.logo, logo, Ui.dp(a, 80), 0);
        row.addView(box, Ui.lp(Ui.dp(a, 76), Ui.dp(a, 54)));
        LinearLayout texts = Ui.column(a);
        texts.addView(Ui.oneLine(Ui.text(a, c.name, 16, Theme.TEXT, Ui.MEDIUM)));
        final TextView now = Ui.oneLine(Ui.text(a, c.group, 13, Theme.TEXT_MUTED));
        texts.addView(now, Ui.lp(-1, -2, 0, Ui.dp(a, 3), 0, 0));
        LinearLayout.LayoutParams tl = Ui.weight(1);
        tl.leftMargin = Ui.dp(a, 14);
        row.addView(texts, tl);
        if (Repo.isFav(c.favKey())) {
            row.addView(Ui.icon(a, R.drawable.ic_heart_fill, Theme.PINK), Ui.lp(Ui.dp(a, 18), Ui.dp(a, 18)));
        }
        Epg.now(a, c, new Epg.Callback() {
            public void done(Xtream.Program p) {
                if (p != null && now.getTag() == c) now.setText("Agora: " + p.title);
            }
        });
        now.setTag(c);
        row.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { PlayerActivity.playLive(a, channels, i); }
        });
        row.setOnLongClickListener(new OnLongClickListener() {
            public boolean onLongClick(View v) {
                boolean on = Repo.toggleFav(c.favKey());
                Ui.toast(a, on ? "Canal adicionado aos favoritos" : "Canal removido dos favoritos");
                if ("".equals(selected)) show("");
                else adapter.notifyDataSetChanged();
                return true;
            }
        });
        return row;
    }

    private final BaseAdapter adapter = new BaseAdapter() {
        public int getCount() { return channels.size(); }

        public Object getItem(int i) { return channels.get(i); }

        public long getItemId(int i) { return i; }

        public View getView(int i, View convert, ViewGroup parent) { return row(i); }
    };
}
