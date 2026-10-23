package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "Jogos de hoje": games on now and coming up later (finished ones are hidden), with the selected
 * game playing in a small player on the right. Press again to watch fullscreen.
 */
public class SportsActivity extends Activity implements TopBar.Listener {

    private static final Pattern TODAY = Pattern.compile(
            "jogos? (de |do )?(hoje|dia)|agenda|eventos? (de |do )?(hoje|dia)|ao vivo hoje|hoje");
    private static final Pattern SPORTS = Pattern.compile(
            "esport|sport|futebol|premiere|combate|ufc|nba|nfl|formula|espn|dazn|copa|libertadores|brasileirao");
    private static final Pattern TIME = Pattern.compile("\\b([01]?\\d|2[0-3])\\s*[:hH]\\s*([0-5]\\d)\\b");
    /** A game counts as live for this long after kick-off. */
    private static final int LIVE_MINUTES = 150;

    private final Handler handler = new Handler();
    private final Runnable refresh = new Runnable() {
        public void run() {
            rebuild();
            handler.postDelayed(this, 60000);
        }
    };

    private final List<Channel> pool = new ArrayList<Channel>();
    /** Rows: a String is a section header, a Channel is a game. */
    private final List<Object> rows = new ArrayList<Object>();
    private Channel playing;
    private boolean fromTodayFolder;

    private TopBar topBar;
    private ListView list;
    private RowAdapter adapter;
    private TextView emptyText;
    private VideoView preview;
    private ProgressBar previewLoading;
    private TextView previewMessage, infoTitle, infoSub, infoStatus, infoNow;
    private ImageView infoLogo;
    private TextView favButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (Repo.channels == null) {
            finish();
            return;
        }
        for (Channel c : Adult.channels()) if (TODAY.matcher(plain(c.group)).find()) pool.add(c);
        fromTodayFolder = !pool.isEmpty();
        if (!fromTodayFolder) {
            for (Channel c : Adult.channels()) {
                if (SPORTS.matcher(plain(c.group)).find() || SPORTS.matcher(plain(c.name)).find()) pool.add(c);
            }
        }
        buildUi();
        rebuild();
        Channel first = null;
        for (Object o : rows) if (o instanceof Channel) { first = (Channel) o; break; }
        if (first != null) select(first, false);
        else previewMessage.setText("Nenhum jogo ao vivo ou programado");
    }

    private static String plain(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    /** Kick-off in minutes since midnight written in the name ("Flamengo x Palmeiras 21:30"), or -1. */
    private static int kickoff(Channel c) {
        Matcher m = TIME.matcher(c.name);
        return m.find() ? Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2)) : -1;
    }

    private static String hhmm(int minutes) {
        return String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60);
    }

    /** The name without its time, for a cleaner title. */
    private static String title(Channel c) {
        String t = TIME.matcher(c.name).replaceAll(" ").replaceAll("\\s*[-|•]\\s*$", "").replaceAll("^\\s*[-|•]\\s*", "");
        return t.replaceAll("\\s+", " ").trim();
    }

    private static int nowMinutes() {
        Calendar cal = Calendar.getInstance();
        return cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE);
    }

    private boolean isLive(Channel c) {
        int k = kickoff(c);
        int now = nowMinutes();
        return k >= 0 && now >= k && now < k + LIVE_MINUTES;
    }

    /** Splits the games into live and upcoming; games that already ended are left out. */
    private void rebuild() {
        List<Channel> live = new ArrayList<Channel>();
        List<Channel> next = new ArrayList<Channel>();
        List<Channel> untimed = new ArrayList<Channel>();
        int now = nowMinutes();
        for (Channel c : pool) {
            int k = kickoff(c);
            if (k < 0) untimed.add(c);
            else if (now >= k && now < k + LIVE_MINUTES) live.add(c);
            else if (k > now) next.add(c);
        }
        Comparator<Channel> byTime = new Comparator<Channel>() {
            public int compare(Channel a, Channel b) { return Integer.compare(kickoff(a), kickoff(b)); }
        };
        Collections.sort(live, byTime);
        Collections.sort(next, byTime);
        rows.clear();
        if (!live.isEmpty()) {
            rows.add("● AO VIVO AGORA");
            rows.addAll(live);
        }
        if (!next.isEmpty()) {
            rows.add("PRÓXIMOS JOGOS");
            rows.addAll(next);
        }
        // Without times in the names there is no way to tell what ended; list them as channels.
        boolean timed = !live.isEmpty() || !next.isEmpty() || untimed.size() < pool.size();
        if (!untimed.isEmpty() && (!timed || !fromTodayFolder)) {
            rows.add(fromTodayFolder ? "TRANSMISSÕES" : "CANAIS DE ESPORTES");
            rows.addAll(untimed);
        }
        adapter.notifyDataSetChanged();
        emptyText.setVisibility(rows.isEmpty() ? View.VISIBLE : View.GONE);
        emptyText.setText(fromTodayFolder ? "Nenhum jogo ao vivo ou programado para hoje"
                : "Sua lista não tem a pasta \"Jogos de hoje\"");
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
        body.setPadding(p, Ui.dp(this, 4), p, Ui.dp(this, 12));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        // Left: the games.
        LinearLayout left = Ui.column(this);
        LinearLayout head = Ui.row(this);
        TextView heading = Ui.text(this, "JOGOS DE HOJE", 20, Palette.TEXT, true);
        head.addView(heading);
        View line = new View(this);
        line.setBackground(Ui.round(Palette.GOLD, Ui.dp(this, 2)));
        LinearLayout.LayoutParams ll = Ui.lp(Ui.dp(this, 40), Ui.dp(this, 3));
        ll.leftMargin = Ui.dp(this, 12);
        head.addView(line, ll);
        left.addView(head, Ui.lp(-1, -2));
        FrameLayout listBox = new FrameLayout(this);
        list = new ListView(this);
        list.setDivider(new android.graphics.drawable.ColorDrawable(0));
        list.setDividerHeight(Ui.dp(this, 6));
        list.setSelector(new android.graphics.drawable.ColorDrawable(0));
        list.setVerticalScrollBarEnabled(false);
        list.setPadding(0, Ui.dp(this, 10), Ui.dp(this, 4), Ui.dp(this, 10));
        list.setClipToPadding(false);
        adapter = new RowAdapter();
        list.setAdapter(adapter);
        listBox.addView(list, new FrameLayout.LayoutParams(-1, -1));
        emptyText = Ui.text(this, "", 16, Palette.TEXT_DIM, false);
        emptyText.setGravity(Gravity.CENTER);
        listBox.addView(emptyText, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        left.addView(listBox, new LinearLayout.LayoutParams(-1, 0, 1f));
        body.addView(left, new LinearLayout.LayoutParams(0, -1, 0.56f));

        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (!(o instanceof Channel)) return;
                Channel c = (Channel) o;
                // First press plays it on the right; pressing it again goes fullscreen.
                if (c != playing) select(c, true);
                else openFullscreen();
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View v, int pos, long id) {
                Object o = rows.get(pos);
                if (o instanceof Channel) toggleFav((Channel) o);
                return true;
            }
        });

        // Right: the small player and the selected game.
        LinearLayout right = Ui.column(this);
        LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(0, -1, 0.44f);
        rl.leftMargin = Ui.dp(this, 20);
        body.addView(right, rl);

        FrameLayout frame = new FrameLayout(this);
        float r = Ui.dp(this, 14);
        frame.setBackground(Ui.round(Palette.SURFACE, r));
        Ui.clipRound(frame, r);
        frame.setFocusable(true);
        frame.setClickable(true);
        frame.setForeground(Ui.focusRing(this, r));
        preview = new VideoView(this);
        frame.addView(preview, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        previewLoading = new ProgressBar(this);
        previewLoading.setIndeterminateTintList(ColorStateList.valueOf(Palette.GOLD));
        previewLoading.setVisibility(View.GONE);
        frame.addView(previewLoading, new FrameLayout.LayoutParams(Ui.dp(this, 44), Ui.dp(this, 44), Gravity.CENTER));
        previewMessage = Ui.text(this, "", 14, Palette.TEXT_DIM, false);
        previewMessage.setGravity(Gravity.CENTER);
        previewMessage.setPadding(Ui.dp(this, 16), 0, Ui.dp(this, 16), 0);
        frame.addView(previewMessage, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        int w = (int) ((getResources().getDisplayMetrics().widthPixels - Ui.dp(this, 68)) * 0.44f);
        right.addView(frame, Ui.lp(-1, w * 9 / 16));
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
                previewMessage.setText("Transmissão indisponível no momento");
                return true;
            }
        });

        LinearLayout info = Ui.column(this);
        info.setBackground(Ui.round(Palette.SURFACE_HI, Ui.dp(this, 14)));
        int ip = Ui.dp(this, 16);
        info.setPadding(ip, ip, ip, ip);
        LinearLayout top = Ui.row(this);
        infoLogo = new ImageView(this);
        infoLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        top.addView(infoLogo, Ui.lp(Ui.dp(this, 52), Ui.dp(this, 40)));
        infoStatus = Ui.text(this, "", 12, Palette.TEXT, true);
        infoStatus.setPadding(Ui.dp(this, 8), Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3));
        LinearLayout.LayoutParams sl = Ui.lp(-2, -2);
        sl.leftMargin = Ui.dp(this, 12);
        top.addView(infoStatus, sl);
        top.addView(new View(this), Ui.spacer());
        favButton = Ui.text(this, "☆", 24, Palette.GOLD, false);
        favButton.setGravity(Gravity.CENTER);
        favButton.setFocusable(true);
        favButton.setClickable(true);
        favButton.setBackground(Ui.selectable(this, 0, 0x22FFFFFF, Ui.dp(this, 20), Palette.GOLD));
        top.addView(favButton, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        favButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (playing != null) toggleFav(playing); }
        });
        info.addView(top, Ui.lp(-1, -2));
        infoTitle = Ui.text(this, "", 20, Palette.TEXT, true);
        infoTitle.setMaxLines(2);
        infoTitle.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tl = Ui.lp(-1, -2);
        tl.topMargin = Ui.dp(this, 10);
        info.addView(infoTitle, tl);
        infoSub = Ui.text(this, "", 14, Palette.TEXT_DIM, false);
        info.addView(infoSub);
        infoNow = Ui.text(this, "", 14, Palette.GOLD, false);
        infoNow.setSingleLine(true);
        infoNow.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nl = Ui.lp(-1, -2);
        nl.topMargin = Ui.dp(this, 6);
        info.addView(infoNow, nl);
        TextView hint = Ui.text(this, "Pressione OK de novo para ver em tela cheia", 12, Palette.TEXT_DIM, false);
        LinearLayout.LayoutParams hl = Ui.lp(-1, -2);
        hl.topMargin = Ui.dp(this, 10);
        info.addView(hint, hl);
        LinearLayout.LayoutParams il = Ui.lp(-1, -2);
        il.topMargin = Ui.dp(this, 12);
        right.addView(info, il);

        setContentView(root);
    }

    // ---------------------------------------------------------------- state

    private void select(final Channel c, boolean startNow) {
        playing = c;
        Repo.current = c;
        adapter.notifyDataSetChanged();
        ImageLoader.load(c.logo, infoLogo, Ui.dp(this, 52), R.drawable.ic_launcher);
        infoTitle.setText(title(c));
        int k = kickoff(c);
        infoSub.setText((k >= 0 ? "Início " + hhmm(k) + "  •  " : "") + c.group);
        boolean live = isLive(c);
        infoStatus.setText(live ? "● AO VIVO" : k >= 0 ? "EM BREVE" : "CANAL");
        infoStatus.setBackground(Ui.round(live ? Palette.HOT : 0xFF3A3A3A, Ui.dp(this, 6)));
        infoNow.setText("");
        favButton.setText(Repo.isFav(Repo.liveKey(c)) ? "★" : "☆");
        Epg.get(c, new Epg.Callback() {
            public void done(List<Xtream.Program> programs) {
                if (isFinishing() || c != playing || programs.isEmpty()) return;
                infoNow.setText("Agora: " + programs.get(0).title);
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

    private List<Channel> games() {
        List<Channel> out = new ArrayList<Channel>();
        for (Object o : rows) if (o instanceof Channel) out.add((Channel) o);
        return out;
    }

    private void toggleFav(Channel c) {
        boolean on = Repo.toggleFav(Repo.liveKey(c));
        Toast.makeText(this, on ? "★ Adicionado aos favoritos" : "Removido dos favoritos", Toast.LENGTH_SHORT).show();
        if (c == playing) favButton.setText(on ? "★" : "☆");
        adapter.notifyDataSetChanged();
    }

    private void openFullscreen() {
        Repo.playlist = games();
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra(PlayerActivity.EXTRA_INDEX, Math.max(0, Repo.playlist.indexOf(playing)));
        startActivity(i);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(refresh);
        if (preview != null) preview.stopPlayback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (preview == null) return;
        handler.postDelayed(refresh, 60000);
        if (Repo.current != null && Repo.current != playing && pool.contains(Repo.current)) select(Repo.current, false);
        startPreview();
    }

    @Override
    public void onBackPressed() {
        if (!topBar.hasFocusInside() && topBar.focusSelected()) return;
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    // ---------------------------------------------------------------- rows

    private final class RowAdapter extends BaseAdapter {
        public int getCount() { return rows.size(); }
        public Object getItem(int i) { return rows.get(i); }
        public long getItemId(int i) { return i; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int i) { return rows.get(i) instanceof String ? 0 : 1; }
        @Override public boolean isEnabled(int i) { return rows.get(i) instanceof Channel; }

        public View getView(int pos, View convert, ViewGroup parent) {
            Object o = rows.get(pos);
            if (o instanceof String) {
                TextView h = convert instanceof TextView ? (TextView) convert
                        : Ui.text(SportsActivity.this, "", 14, Palette.TEXT, true);
                String text = (String) o;
                h.setText(text);
                h.setTextColor(text.startsWith("●") ? Palette.ERROR : Palette.GOLD);
                h.setPadding(Ui.dp(SportsActivity.this, 4), Ui.dp(SportsActivity.this, pos == 0 ? 2 : 14), 0,
                        Ui.dp(SportsActivity.this, 2));
                return h;
            }
            Card h = convert == null || !(convert.getTag() instanceof Card) ? new Card() : (Card) convert.getTag();
            Channel c = (Channel) o;
            boolean on = c == playing;
            float r = Ui.dp(SportsActivity.this, 12);
            h.root.setBackground(on
                    ? Ui.stroke(Palette.SURFACE_HI, r, Ui.dp(SportsActivity.this, 2), Palette.GOLD)
                    : Ui.selectable(SportsActivity.this, Palette.SURFACE, Palette.SURFACE_HI, r, Palette.GOLD));
            int k = kickoff(c);
            boolean live = isLive(c);
            h.time.setText(k >= 0 ? hhmm(k) : "--:--");
            h.time.setTextColor(live ? Palette.TEXT : Palette.BLACK);
            h.time.setBackground(Ui.round(live ? Palette.HOT : Palette.GOLD, Ui.dp(SportsActivity.this, 8)));
            h.live.setVisibility(live ? View.VISIBLE : View.GONE);
            h.name.setText(title(c));
            h.name.setTextColor(on ? Palette.GOLD : Palette.TEXT);
            h.sub.setText((Repo.isFav(Repo.liveKey(c)) ? "★  " : "") + c.group);
            ImageLoader.load(c.logo, h.logo, Ui.dp(SportsActivity.this, 48), R.drawable.ic_launcher);
            return h.root;
        }
    }

    private final class Card {
        final LinearLayout root;
        final TextView time, live, name, sub;
        final ImageView logo;

        Card() {
            Activity a = SportsActivity.this;
            root = Ui.row(a);
            int p = Ui.dp(a, 10);
            root.setPadding(p, p, Ui.dp(a, 14), p);
            root.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, Ui.dp(a, 76)));
            LinearLayout timeCol = Ui.column(a);
            timeCol.setGravity(Gravity.CENTER);
            time = Ui.text(a, "", 16, Palette.BLACK, true);
            time.setGravity(Gravity.CENTER);
            timeCol.addView(time, Ui.lp(Ui.dp(a, 66), Ui.dp(a, 34)));
            live = Ui.text(a, "AO VIVO", 10, Palette.ERROR, true);
            live.setGravity(Gravity.CENTER);
            timeCol.addView(live, Ui.lp(Ui.dp(a, 66), -2));
            root.addView(timeCol);
            logo = new ImageView(a);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams ll = Ui.lp(Ui.dp(a, 48), Ui.dp(a, 38));
            ll.leftMargin = Ui.dp(a, 12);
            root.addView(logo, ll);
            LinearLayout col = Ui.column(a);
            col.setPadding(Ui.dp(a, 12), 0, 0, 0);
            name = Ui.text(a, "", 16, Palette.TEXT, true);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            col.addView(name);
            sub = Ui.text(a, "", 12, Palette.TEXT_DIM, false);
            sub.setSingleLine(true);
            sub.setEllipsize(TextUtils.TruncateAt.END);
            col.addView(sub);
            root.addView(col, Ui.weight(1));
            root.setTag(this);
        }
    }
}
