package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Sports hub: live preview with now/next on the left, sports channels with their programs on the right. */
public class SportsActivity extends Activity implements TopBar.Listener {

    private static final Pattern SPORTS = Pattern.compile(
            "esport|sport|futebol|futbol|premiere|combate|ufc|luta|fight|nba|nfl|mlb|nhl|formula|\\bf1\\b|"
                    + "motor|tenis|tennis|golf|ppv|pay.per.view|espn|dazn|campeonato|copa|libertadores|"
                    + "brasileirao|champions|liga|jogos|eleven|fox sports|tnt sports|band sports|sportv");

    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private List<Channel> all = new ArrayList<Channel>();
    private List<Channel> shown = new ArrayList<Channel>();
    private final List<String> groups = new ArrayList<String>();
    private String group;
    private Channel playing;

    private TopBar topBar;
    private VideoView preview;
    private ProgressBar previewLoading;
    private TextView previewMessage, liveBadge;
    private ImageView infoLogo;
    private TextView infoName, infoNow, infoTime, infoNext;
    private ProgressBar infoProgress;
    private LinearLayout chips;
    private GridView grid;
    private CardAdapter adapter;
    private TextView favButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (Repo.channels == null) {
            finish();
            return;
        }
        for (Channel c : Repo.channels) {
            if (SPORTS.matcher(plain(c.group)).find() || SPORTS.matcher(plain(c.name)).find()) all.add(c);
        }
        for (Channel c : all) if (!groups.contains(c.group)) groups.add(c.group);
        buildUi();
        selectGroup(null);
        if (!all.isEmpty()) {
            Channel start = all.contains(Repo.current) ? Repo.current : all.get(0);
            select(start, false);
        } else {
            previewMessage.setText("Nenhum canal de esportes encontrado na sua lista");
        }
    }

    private static String plain(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    public void onTopBar(int item) {
        if (item != TopBar.SPORTS) TopBar.open(this, item);
    }

    // ---------------------------------------------------------------- layout

    private void buildUi() {
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Palette.BLACK);
        topBar = new TopBar(this, Repo.hasVod(), this);
        topBar.select(TopBar.SPORTS);
        root.addView(topBar, Ui.lp(-1, -2));

        LinearLayout body = Ui.row(this);
        body.setGravity(Gravity.TOP);
        int p = Ui.dp(this, 24);
        body.setPadding(p, Ui.dp(this, 6), p, Ui.dp(this, 12));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        body.addView(buildHero(), new LinearLayout.LayoutParams(0, -1, 0.44f));
        LinearLayout right = Ui.column(this);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(0, -1, 0.56f);
        rl.leftMargin = Ui.dp(this, 20);
        body.addView(right, rl);

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        chips = Ui.row(this);
        hs.addView(chips);
        right.addView(hs, Ui.lp(-1, -2));

        grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(Ui.dp(this, 250));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setHorizontalSpacing(Ui.dp(this, 10));
        grid.setVerticalSpacing(Ui.dp(this, 10));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        grid.setDrawSelectorOnTop(false);
        grid.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        grid.setClipToPadding(false);
        adapter = new CardAdapter();
        grid.setAdapter(adapter);
        right.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
                Channel c = shown.get(pos);
                if (c == playing) openFullscreen();
                else select(c, true);
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id) {
                toggleFav(shown.get(pos));
                return true;
            }
        });
        setContentView(root);
    }

    private View buildHero() {
        LinearLayout hero = Ui.column(this);

        FrameLayout frame = new FrameLayout(this);
        float r = Ui.dp(this, 14);
        frame.setBackground(Ui.round(Palette.SURFACE, r));
        Ui.clipRound(frame, r);
        frame.setFocusable(true);
        frame.setClickable(true);
        frame.setForeground(Ui.selectable(this, 0, 0, r, Palette.GOLD));
        preview = new VideoView(this);
        frame.addView(preview, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        previewLoading = new ProgressBar(this);
        previewLoading.setIndeterminateTintList(ColorStateList.valueOf(Palette.GOLD));
        previewLoading.setVisibility(View.GONE);
        frame.addView(previewLoading, new FrameLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44), Gravity.CENTER));
        previewMessage = Ui.text(this, "", 15, Palette.TEXT_DIM, false);
        previewMessage.setGravity(Gravity.CENTER);
        previewMessage.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        frame.addView(previewMessage, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        liveBadge = Ui.text(this, "● AO VIVO", 12, Palette.TEXT, true);
        liveBadge.setBackground(Ui.round(Palette.HOT, Ui.dp(this, 6)));
        liveBadge.setPadding(Ui.dp(this, 8), Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3));
        FrameLayout.LayoutParams bl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        bl.setMargins(Ui.dp(this, 10), Ui.dp(this, 10), 0, 0);
        frame.addView(liveBadge, bl);
        hero.addView(frame, new LinearLayout.LayoutParams(-1, 0, 1f));
        frame.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (playing != null) openFullscreen(); }
        });

        preview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                previewLoading.setVisibility(View.GONE);
                preview.start();
            }
        });
        preview.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                previewLoading.setVisibility(View.GONE);
                previewMessage.setText("Canal indisponível no momento");
                return true;
            }
        });

        LinearLayout info = Ui.column(this);
        info.setBackground(Ui.round(Palette.SURFACE_HI, Ui.dp(this, 14)));
        int p = Ui.dp(this, 14);
        info.setPadding(p, p, p, p);
        LinearLayout head = Ui.row(this);
        infoLogo = new ImageView(this);
        infoLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        head.addView(infoLogo, Ui.lp(Ui.dp(this, 52), Ui.dp(this, 38)));
        LinearLayout titles = Ui.column(this);
        titles.setPadding(Ui.dp(this, 12), 0, 0, 0);
        infoName = Ui.text(this, "", 17, Palette.TEXT, true);
        infoName.setSingleLine(true);
        infoName.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(infoName);
        infoNow = Ui.text(this, "", 14, Palette.GOLD, true);
        infoNow.setSingleLine(true);
        infoNow.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(infoNow);
        head.addView(titles, Ui.weight(1));
        favButton = Ui.text(this, "☆", 24, Palette.GOLD, false);
        favButton.setFocusable(true);
        favButton.setClickable(true);
        favButton.setGravity(Gravity.CENTER);
        favButton.setBackground(Ui.selectable(this, 0, 0x22FFFFFF, Ui.dp(this, 20), Palette.GOLD));
        head.addView(favButton, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        favButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (playing != null) toggleFav(playing); }
        });
        info.addView(head, Ui.lp(-1, -2));

        infoProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        infoProgress.setMax(1000);
        infoProgress.setProgressTintList(ColorStateList.valueOf(Palette.GOLD));
        infoProgress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF444444));
        LinearLayout.LayoutParams pl = Ui.lp(-1, Ui.dp(this, 4));
        pl.topMargin = Ui.dp(this, 10);
        info.addView(infoProgress, pl);
        LinearLayout times = Ui.row(this);
        infoTime = Ui.text(this, "", 13, Palette.TEXT_DIM, false);
        times.addView(infoTime, Ui.weight(1));
        infoNext = Ui.text(this, "", 13, Palette.TEXT_DIM, false);
        infoNext.setSingleLine(true);
        infoNext.setEllipsize(TextUtils.TruncateAt.END);
        infoNext.setGravity(Gravity.END);
        times.addView(infoNext, Ui.weight(1.6f));
        LinearLayout.LayoutParams tl = Ui.lp(-1, -2);
        tl.topMargin = Ui.dp(this, 6);
        info.addView(times, tl);

        LinearLayout.LayoutParams il = Ui.lp(-1, -2);
        il.topMargin = Ui.dp(this, 12);
        hero.addView(info, il);
        return hero;
    }

    // ---------------------------------------------------------------- state

    private void selectGroup(String g) {
        group = g;
        shown = new ArrayList<Channel>();
        for (Channel c : all) if (g == null || g.equals(c.group)) shown.add(c);
        chips.removeAllViews();
        addChip(null, "Todos");
        for (String name : groups) addChip(name, name);
        adapter.notifyDataSetChanged();
        grid.setSelection(0);
    }

    private void addChip(final String value, String label) {
        boolean on = value == null ? group == null : value.equals(group);
        TextView chip = Ui.text(this, label, 14, on ? Palette.BLACK : Palette.TEXT, true);
        chip.setSingleLine(true);
        chip.setFocusable(true);
        chip.setClickable(true);
        chip.setBackground(Ui.selectable(this, on ? Palette.GOLD : Palette.SURFACE_HI,
                on ? Palette.GOLD_LIGHT : 0xFF2A2A2A, Ui.dp(this, 18), Palette.GOLD));
        chip.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
        lp.rightMargin = Ui.dp(this, 8);
        chips.addView(chip, lp);
        chip.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { selectGroup(value); }
        });
    }

    private void select(final Channel c, boolean startNow) {
        playing = c;
        Repo.current = c;
        Repo.playlist = shown.contains(c) ? shown : all;
        adapter.notifyDataSetChanged();
        ImageLoader.load(c.logo, infoLogo, Ui.dp(this, 52), R.drawable.ic_launcher);
        infoName.setText(c.name);
        infoNow.setText("");
        infoTime.setText("");
        infoNext.setText("");
        infoProgress.setVisibility(View.INVISIBLE);
        favButton.setText(Repo.isFav(Repo.liveKey(c)) ? "★" : "☆");
        Epg.get(c, new Epg.Callback() {
            public void done(List<Xtream.Program> programs) {
                if (isFinishing() || c != playing || programs.isEmpty()) return;
                Xtream.Program now = programs.get(0);
                infoNow.setText(now.title);
                infoTime.setText(clock.format(new Date(now.start)) + " - " + clock.format(new Date(now.end)));
                infoProgress.setProgress(Epg.progress(now));
                infoProgress.setVisibility(View.VISIBLE);
                if (programs.size() > 1) infoNext.setText("A seguir: " + programs.get(1).title);
            }
        });
        if (startNow) startPreview();
    }

    private void startPreview() {
        if (playing == null) return;
        previewLoading.setVisibility(View.VISIBLE);
        previewMessage.setText("");
        preview.stopPlayback();
        preview.setVideoURI(Uri.parse(playing.url));
    }

    private void toggleFav(Channel c) {
        boolean on = Repo.toggleFav(Repo.liveKey(c));
        Toast.makeText(this, on ? "★ Adicionado aos favoritos" : "Removido dos favoritos", Toast.LENGTH_SHORT).show();
        if (c == playing) favButton.setText(on ? "★" : "☆");
        adapter.notifyDataSetChanged();
    }

    private void openFullscreen() {
        Repo.playlist = shown.contains(playing) ? shown : all;
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra(PlayerActivity.EXTRA_INDEX, Math.max(0, Repo.playlist.indexOf(playing)));
        startActivity(i);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (preview != null) preview.stopPlayback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preview == null) return;
        if (Repo.current != null && Repo.current != playing && all.contains(Repo.current)) select(Repo.current, false);
        startPreview();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    // ---------------------------------------------------------------- cards

    private final Runnable refresh = new Runnable() {
        public void run() { adapter.notifyDataSetChanged(); }
    };

    private final class CardAdapter extends BaseAdapter {
        public int getCount() { return shown.size(); }
        public Object getItem(int i) { return shown.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int pos, View convert, ViewGroup parent) {
            Card h = convert == null ? new Card() : (Card) convert.getTag();
            Channel c = shown.get(pos);
            boolean on = c == playing;
            h.root.setBackground(on
                    ? Ui.stroke(Palette.SURFACE_HI, Ui.dp(SportsActivity.this, 12), Ui.dp(SportsActivity.this, 2), Palette.GOLD)
                    : Ui.selectable(SportsActivity.this, Palette.SURFACE_HI, 0xFF2A2A2A, Ui.dp(SportsActivity.this, 12),
                    Palette.GOLD));
            h.name.setText(c.name);
            h.name.setTextColor(on ? Palette.GOLD : Palette.TEXT);
            h.fav.setVisibility(Repo.isFav(Repo.liveKey(c)) ? View.VISIBLE : View.GONE);
            ImageLoader.load(c.logo, h.logo, Ui.dp(SportsActivity.this, 56), R.drawable.ic_launcher);
            Xtream.Program now = Epg.now(c);
            if (now != null) {
                h.now.setText(clock.format(new Date(now.start)) + "  " + now.title);
                h.progress.setProgress(Epg.progress(now));
                h.progress.setVisibility(View.VISIBLE);
                h.live.setVisibility(View.VISIBLE);
            } else {
                h.now.setText(c.group);
                h.progress.setVisibility(View.INVISIBLE);
                h.live.setVisibility(View.GONE);
                if (c.streamId != null && Epg.peek(c) == null) {
                    Epg.get(c, new Epg.Callback() {
                        public void done(List<Xtream.Program> programs) {
                            if (programs.isEmpty() || isFinishing()) return;
                            grid.removeCallbacks(refresh);
                            grid.postDelayed(refresh, 150);
                        }
                    });
                }
            }
            return h.root;
        }
    }

    private final class Card {
        final LinearLayout root;
        final ImageView logo;
        final TextView name, now, live, fav;
        final ProgressBar progress;

        Card() {
            Activity a = SportsActivity.this;
            root = Ui.row(a);
            int p = Ui.dp(a, 12);
            root.setPadding(p, p, p, p);
            root.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, Ui.dp(a, 88)));
            logo = new ImageView(a);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            root.addView(logo, Ui.lp(Ui.dp(a, 56), Ui.dp(a, 42)));
            LinearLayout col = Ui.column(a);
            col.setPadding(Ui.dp(a, 12), 0, 0, 0);
            LinearLayout top = Ui.row(a);
            name = Ui.text(a, "", 15, Palette.TEXT, true);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            top.addView(name, Ui.weight(1));
            fav = Ui.text(a, "★", 14, Palette.GOLD, false);
            fav.setPadding(Ui.dp(a, 6), 0, 0, 0);
            top.addView(fav);
            live = Ui.text(a, "AO VIVO", 10, Palette.TEXT, true);
            live.setBackground(Ui.round(Palette.HOT, Ui.dp(a, 4)));
            live.setPadding(Ui.dp(a, 5), Ui.dp(a, 1), Ui.dp(a, 5), Ui.dp(a, 1));
            LinearLayout.LayoutParams ll = Ui.lp(-2, -2);
            ll.leftMargin = Ui.dp(a, 6);
            top.addView(live, ll);
            col.addView(top, Ui.lp(-1, -2));
            now = Ui.text(a, "", 13, Palette.TEXT_DIM, false);
            now.setSingleLine(true);
            now.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nl = Ui.lp(-1, -2);
            nl.topMargin = Ui.dp(a, 3);
            col.addView(now, nl);
            progress = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
            progress.setMax(1000);
            progress.setProgressTintList(ColorStateList.valueOf(Palette.GOLD));
            progress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFF444444));
            LinearLayout.LayoutParams pl = Ui.lp(-1, Ui.dp(a, 3));
            pl.topMargin = Ui.dp(a, 6);
            col.addView(progress, pl);
            root.addView(col, Ui.weight(1));
            root.setTag(this);
        }
    }
}
