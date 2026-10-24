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
    private LinearLayout faceOff;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        Ui.fit(this);
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
        // One card per game: the HD version stands for its SD / FHD copies.
        List<Channel> one = Variants.collapse(pool);
        pool.clear();
        pool.addAll(one);
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
        String t = TIME.matcher(Variants.displayName(c)).replaceAll(" ").replaceAll("\\s*[-|•]\\s*$", "").replaceAll("^\\s*[-|•]\\s*", "");
        return t.replaceAll("\\s+", " ").trim();
    }

    private static final Pattern VERSUS = Pattern.compile("\\s+(?:x|X|vs\\.?|VS|×)\\s+");
    private static final Pattern COMPETITION = Pattern.compile("^(.*?)\\s*[:|•]\\s+(.+)$");

    /** "Brasileirão: Flamengo x Palmeiras" → {"Flamengo", "Palmeiras", "Brasileirão"}; null teams when not a match. */
    private static String[] teams(Channel c) {
        String t = title(c);
        String comp = null;
        Matcher cm = COMPETITION.matcher(t);
        if (cm.find() && VERSUS.matcher(cm.group(2)).find() && !VERSUS.matcher(cm.group(1)).find()) {
            comp = cm.group(1).trim();
            t = cm.group(2).trim();
        }
        String[] parts = VERSUS.split(t, 2);
        if (parts.length < 2) return new String[] {null, null, comp};
        String away = parts[1].replaceAll("\\s*(?:[\\[(|]|\\s[-–]\\s).*$", "").trim();
        String extra = parts[1].length() > away.length() ? parts[1].substring(away.length()).replaceAll("[\\[\\]()|–-]", " ").trim() : "";
        extra = extra.replaceAll("(?i)\\b(sd|hd|fhd|full ?hd|uhd|4k|h\\.?265|hevc|alt|backup)\\b", "").trim();
        if (comp == null && !extra.isEmpty()) comp = extra;
        return new String[] {parts[0].trim(), away, comp};
    }

    /** Minutes since kick-off for live games ("37'"), else null. */
    private String elapsed(Channel c) {
        int k = kickoff(c);
        if (!isLive(c)) return null;
        int m = nowMinutes() - k;
        if (m <= 45) return Math.max(1, m) + "'";
        if (m <= 60) return "Intervalo";
        return Math.min(90, m - 15) + "'";
    }

    /** Round badge with a team's initials, colored from its name. */
    private View badge(String team, int sizeDp) {
        String init = team == null ? "?" : team.replaceAll("[^\\p{L}\\p{N} ]", "").trim();
        String[] w = init.split("\\s+");
        init = w.length > 1 && w[1].length() > 0 ? ("" + w[0].charAt(0) + w[1].charAt(0)) : init.length() >= 3 ? init.substring(0, 3) : init;
        int[] colors = {0xFF1E5A3C, 0xFF5A1E2A, 0xFF1E3A5A, 0xFF5A4A1E, 0xFF3E1E5A, 0xFF1E525A, 0xFF5A2E1E};
        int col = colors[Math.abs((team == null ? 0 : team.hashCode()) % colors.length)];
        TextView b = Ui.text(this, init.toUpperCase(Locale.ROOT), sizeDp * 0.30f, Palette.TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Ui.stroke(col, Ui.dp(this, sizeDp / 2f), Ui.dp(this, 1.5f), 0x55FFFFFF));
        return b;
    }

    private static final int FILTER_ALL = 0, FILTER_LIVE = 1, FILTER_NEXT = 2;
    private int filter = FILTER_ALL;
    private final List<TextView> filterChips = new ArrayList<TextView>();

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
        if (filter == FILTER_NEXT) live.clear();
        if (filter == FILTER_LIVE) next.clear();
        if (filter != FILTER_ALL) untimed.clear();
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
        emptyText.setText(filter == FILTER_LIVE ? "Nenhum jogo ao vivo agora"
                : filter == FILTER_NEXT ? "Nenhum jogo programado para mais tarde"
                : fromTodayFolder ? "Nenhum jogo ao vivo ou programado para hoje"
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
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = Ui.column(this);
        TextView heading = Ui.text(this, "JOGOS DE HOJE", 18, Palette.TEXT, true);
        heading.setLetterSpacing(0.05f);
        titles.addView(heading);
        String today = new java.text.SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("pt", "BR")).format(new java.util.Date());
        titles.addView(Ui.text(this, today.substring(0, 1).toUpperCase(Locale.ROOT) + today.substring(1), 12,
                Palette.TEXT_DIM, false));
        head.addView(titles, Ui.weight(1));
        String[] labels = {"Todos", "Ao vivo", "Próximos"};
        for (int i = 0; i < labels.length; i++) {
            final int which = i;
            TextView chip = Ui.text(this, labels[i], 13, Palette.TEXT_DIM, true);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setPadding(Ui.dp(this, 14), Ui.dp(this, 6), Ui.dp(this, 14), Ui.dp(this, 6));
            LinearLayout.LayoutParams cl = Ui.lp(-2, -2);
            cl.leftMargin = Ui.dp(this, 6);
            head.addView(chip, cl);
            filterChips.add(chip);
            chip.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    filter = which;
                    styleFilters();
                    rebuild();
                }
            });
        }
        styleFilters();
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
        if (!Ui.isTv(this)) w = Math.min(w, (int) ((getResources().getDisplayMetrics().heightPixels - Ui.dp(this, 240)) * 16 / 9f));
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
                // Off the air: same game in another quality before giving up.
                if (playing != null) {
                    tried.add(playingUrl);
                    Channel other = Variants.untried(playing, Adult.channels(), triedChannels(playing));
                    if (other != null) {
                        tried.add(other.url);
                        playingUrl = other.url;
                        previewLoading.setVisibility(View.VISIBLE);
                        preview.stopPlayback();
                        preview.setVideoURI(Uri.parse(other.url));
                        return true;
                    }
                }
                previewLoading.setVisibility(View.GONE);
                previewMessage.setText("Transmissão indisponível no momento");
                return true;
            }
        });

        // Compact info card: status line, the match on one row, and what is on now.
        LinearLayout info = Ui.column(this);
        info.setBackground(Ui.round(Palette.SURFACE_HI, Ui.dp(this, 14)));
        int ip = Ui.dp(this, 14);
        info.setPadding(ip, Ui.dp(this, 10), Ui.dp(this, 8), ip);
        LinearLayout top = Ui.row(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        infoStatus = Ui.text(this, "", 11, Palette.TEXT, true);
        infoStatus.setPadding(Ui.dp(this, 8), Ui.dp(this, 3), Ui.dp(this, 8), Ui.dp(this, 3));
        top.addView(infoStatus);
        infoSub = Ui.text(this, "", 12, Palette.TEXT_DIM, false);
        infoSub.setSingleLine(true);
        infoSub.setEllipsize(TextUtils.TruncateAt.END);
        infoSub.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 6), 0);
        top.addView(infoSub, Ui.weight(1));
        infoLogo = new ImageView(this);
        infoLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        top.addView(infoLogo, Ui.lp(Ui.dp(this, 40), Ui.dp(this, 28)));
        favButton = Ui.text(this, "☆", 20, Palette.GOLD, false);
        favButton.setGravity(Gravity.CENTER);
        favButton.setFocusable(true);
        favButton.setClickable(true);
        favButton.setBackground(Ui.selectable(this, 0, 0x22FFFFFF, Ui.dp(this, 18), Palette.GOLD));
        top.addView(favButton, Ui.lp(Ui.dp(this, 38), Ui.dp(this, 38)));
        favButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (playing != null) toggleFav(playing); }
        });
        info.addView(top, Ui.lp(-1, -2));
        faceOff = Ui.row(this);
        faceOff.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams fl = Ui.lp(-1, -2);
        fl.topMargin = Ui.dp(this, 8);
        info.addView(faceOff, fl);
        infoTitle = Ui.text(this, "", 16, Palette.TEXT, true);
        infoTitle.setMaxLines(2);
        infoTitle.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tl = Ui.lp(-1, -2);
        tl.topMargin = Ui.dp(this, 8);
        info.addView(infoTitle, tl);
        infoNow = Ui.text(this, "", 12, Palette.GOLD, false);
        infoNow.setSingleLine(true);
        infoNow.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nl = Ui.lp(-1, -2);
        nl.topMargin = Ui.dp(this, 8);
        info.addView(infoNow, nl);
        LinearLayout.LayoutParams il = Ui.lp(-1, -2);
        il.topMargin = Ui.dp(this, 10);
        right.addView(info, il);

        setContentView(root);
    }

    private void styleFilters() {
        float r = Ui.dp(this, 16);
        for (int i = 0; i < filterChips.size(); i++) {
            boolean on = i == filter;
            TextView t = filterChips.get(i);
            t.setTextColor(on ? Palette.BLACK : Palette.TEXT_DIM);
            t.setBackground(Ui.selectable(this, on ? Palette.GOLD : 0x1AFFFFFF, on ? Palette.GOLD_LIGHT : 0x33FFFFFF, r,
                    on ? Palette.TEXT : Palette.GOLD));
        }
    }

    // ---------------------------------------------------------------- state

    private void select(final Channel c, boolean startNow) {
        playing = c;
        Repo.current = c;
        adapter.notifyDataSetChanged();
        ImageLoader.load(c.logo, infoLogo, Ui.dp(this, 52), R.drawable.logo_mark);
        int k = kickoff(c);
        boolean live = isLive(c);
        String[] t = teams(c);
        faceOff.removeAllViews();
        if (t[0] != null) {
            faceOff.setVisibility(View.VISIBLE);
            faceOff.addView(teamSide(t[0], true), Ui.weight(1));
            String mid = live ? elapsed(c) : k >= 0 ? hhmm(k) : "x";
            TextView vs = Ui.text(this, mid, 16, live ? Palette.ERROR : Palette.GOLD, true);
            vs.setGravity(Gravity.CENTER);
            faceOff.addView(vs, Ui.lp(Ui.dp(this, 78), -2));
            faceOff.addView(teamSide(t[1], false), Ui.weight(1));
            infoTitle.setVisibility(View.GONE);
        } else {
            faceOff.setVisibility(View.GONE);
            infoTitle.setVisibility(View.VISIBLE);
            infoTitle.setText(title(c));
        }
        infoSub.setText((k >= 0 ? "Início " + hhmm(k) + "  •  " : "") + (t[2] != null ? t[2] : Variants.cleanGroup(c.group)));
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

    /** Badge and name on one line; the away side is mirrored (name, then badge). */
    private View teamSide(String team, boolean home) {
        LinearLayout row = Ui.row(this);
        row.setGravity(Gravity.CENTER_VERTICAL | (home ? Gravity.START : Gravity.END));
        TextView n = Ui.text(this, team, 14, Palette.TEXT, true);
        n.setMaxLines(2);
        n.setEllipsize(TextUtils.TruncateAt.END);
        n.setGravity(home ? Gravity.START : Gravity.END);
        View b = badge(team, 36);
        LinearLayout.LayoutParams bl = Ui.lp(Ui.dp(this, 36), Ui.dp(this, 36));
        LinearLayout.LayoutParams nl = new LinearLayout.LayoutParams(0, -2, 1f);
        if (home) {
            row.addView(b, bl);
            nl.leftMargin = Ui.dp(this, 8);
            row.addView(n, nl);
        } else {
            nl.rightMargin = Ui.dp(this, 8);
            row.addView(n, nl);
            row.addView(b, bl);
        }
        return row;
    }

    private View teamColumn(String team) {
        LinearLayout col = Ui.column(this);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        col.addView(badge(team, 52), Ui.lp(Ui.dp(this, 52), Ui.dp(this, 52)));
        TextView n = Ui.text(this, team, 14, Palette.TEXT, true);
        n.setGravity(Gravity.CENTER);
        n.setMaxLines(2);
        n.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nl = Ui.lp(-1, -2);
        nl.topMargin = Ui.dp(this, 6);
        col.addView(n, nl);
        return col;
    }

    /** Stream URLs already tried for the selected game. */
    private final java.util.Set<String> tried = new java.util.HashSet<String>();
    private String playingUrl;

    private java.util.Set<Channel> triedChannels(Channel c) {
        java.util.Set<Channel> out = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Channel, Boolean>());
        for (Channel v : Variants.of(c, Adult.channels())) if (tried.contains(v.url)) out.add(v);
        return out;
    }

    private void startPreview() {
        if (playing == null) return;
        tried.clear();
        playingUrl = playing.url;
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
            float r = Ui.dp(SportsActivity.this, 14);
            h.root.setBackground(on
                    ? Ui.stroke(0xFF1A1610, r, Ui.dp(SportsActivity.this, 2), Palette.GOLD)
                    : Ui.selectable(SportsActivity.this, Palette.SURFACE, Palette.SURFACE_HI, r, Palette.GOLD));
            int k = kickoff(c);
            boolean live = isLive(c);
            String el = elapsed(c);
            h.time.setText(live ? (el == null ? "AO VIVO" : el) : k >= 0 ? hhmm(k) : "--:--");
            h.time.setTextColor(live ? Palette.TEXT : Palette.GOLD);
            h.time.setBackground(live ? Ui.round(Palette.HOT, Ui.dp(SportsActivity.this, 8))
                    : Ui.stroke(0, Ui.dp(SportsActivity.this, 8), Ui.dp(SportsActivity.this, 1), 0x66E8B84A));
            h.live.setText(live ? "● AO VIVO" : k >= 0 ? "HOJE" : "");
            h.live.setTextColor(live ? Palette.ERROR : Palette.TEXT_DIM);
            String[] t = teams(c);
            h.badges.removeAllViews();
            if (t[0] != null) {
                h.badges.setVisibility(View.VISIBLE);
                h.badges.addView(badge(t[0], 30), Ui.lp(Ui.dp(SportsActivity.this, 30), Ui.dp(SportsActivity.this, 30)));
                LinearLayout.LayoutParams bl = Ui.lp(Ui.dp(SportsActivity.this, 30), Ui.dp(SportsActivity.this, 30));
                bl.leftMargin = -Ui.dp(SportsActivity.this, 8);
                h.badges.addView(badge(t[1], 30), bl);
                h.name.setText(t[0] + "  x  " + t[1]);
            } else {
                h.badges.setVisibility(View.GONE);
                h.name.setText(title(c));
            }
            h.name.setTextColor(on ? Palette.GOLD : Palette.TEXT);
            String comp = t[2] != null ? t[2] : Variants.cleanGroup(c.group);
            final String fav = Repo.isFav(Repo.liveKey(c)) ? "★  " : "";
            h.sub.setText(fav + comp);
            h.sub.setTextColor(Palette.TEXT_DIM);
            h.bound = c;
            if (t[0] == null) {
                // Plain sports channels: show what is on now instead of the folder name.
                final Card card = h;
                final Channel ch = c;
                Epg.get(c, new Epg.Callback() {
                    public void done(List<Xtream.Program> programs) {
                        if (card.bound != ch || programs.isEmpty()) return;
                        card.sub.setText(fav + "Agora: " + programs.get(0).title);
                        card.sub.setTextColor(0xFFCFAE5E);
                    }
                });
            }
            ImageLoader.load(c.logo, h.logo, Ui.dp(SportsActivity.this, 44), R.drawable.logo_mark);
            return h.root;
        }
    }

    private final class Card {
        final LinearLayout root, badges;
        final TextView time, live, name, sub;
        final ImageView logo;
        Channel bound;

        Card() {
            Activity a = SportsActivity.this;
            root = Ui.row(a);
            root.setGravity(Gravity.CENTER_VERTICAL);
            int p = Ui.dp(a, 10);
            root.setPadding(p, p, Ui.dp(a, 14), p);
            root.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, Ui.dp(a, 72)));
            LinearLayout timeCol = Ui.column(a);
            timeCol.setGravity(Gravity.CENTER);
            time = Ui.text(a, "", 14, Palette.GOLD, true);
            time.setGravity(Gravity.CENTER);
            timeCol.addView(time, Ui.lp(Ui.dp(a, 70), Ui.dp(a, 30)));
            live = Ui.text(a, "", 9, Palette.ERROR, true);
            live.setGravity(Gravity.CENTER);
            live.setLetterSpacing(0.06f);
            LinearLayout.LayoutParams lvl = Ui.lp(Ui.dp(a, 70), -2);
            lvl.topMargin = Ui.dp(a, 3);
            timeCol.addView(live, lvl);
            root.addView(timeCol);
            badges = Ui.row(a);
            LinearLayout.LayoutParams bl = Ui.lp(-2, -2);
            bl.leftMargin = Ui.dp(a, 14);
            root.addView(badges, bl);
            LinearLayout col = Ui.column(a);
            col.setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 8), 0);
            name = Ui.text(a, "", 15, Palette.TEXT, true);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            col.addView(name);
            sub = Ui.text(a, "", 12, Palette.TEXT_DIM, false);
            sub.setSingleLine(true);
            sub.setEllipsize(TextUtils.TruncateAt.END);
            col.addView(sub);
            root.addView(col, Ui.weight(1));
            logo = new ImageView(a);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            logo.setAlpha(0.85f);
            root.addView(logo, Ui.lp(Ui.dp(a, 44), Ui.dp(a, 32)));
            root.setTag(this);
        }
    }
}
