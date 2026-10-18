package com.xplay.iptv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/** Live TV: channel groups on the left, preview + now/next on the right, side menu. */
public class LiveActivity extends Activity {

    private static final String ALL = "TODOS";
    private static final String FAVS = "FAVORITOS";

    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private boolean showingChannels;
    private String groupTitle;
    private List<String> groups = new ArrayList<String>();
    private List<Channel> channelItems = new ArrayList<Channel>();
    private List<Channel> playingList = new ArrayList<Channel>();
    private Channel playing;
    private float zoom;

    private ListView list;
    private ListAdapter adapter;
    private ImageView backButton;
    private TextView headerTitle;
    private VideoView preview;
    private ProgressBar previewLoading;
    private TextView previewMessage;
    private ImageView epgLogo;
    private TextView nowTime, nowTitle, nextTime, nextTitle, nextLabel;
    private FrameLayout drawer;
    private LinearLayout drawerItems;
    private View menuButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (Repo.channels == null) {
            // Process was recreated: log in again from the saved credentials.
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        zoom = Repo.listZoom();
        buildUi();
        showGroups();

        String last = Repo.prefs().getString("last_channel", null);
        for (Channel c : Repo.channels) {
            if (c.url.equals(last)) {
                playingList = Repo.channels;
                select(c, false);
                break;
            }
        }
        if (playing == null) previewMessage.setText("Escolha um canal na lista");
    }

    // ---------------------------------------------------------------- layout

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Palette.BLACK);

        LinearLayout content = Ui.column(this);
        int h = Ui.dp(this, 24);
        content.setPadding(h, Ui.dp(this, 12), h, Ui.dp(this, 12));
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));

        // Top bar
        LinearLayout top = Ui.row(this);
        top.addView(Ui.logo(this, 22));
        top.addView(new View(this), Ui.spacer());
        ImageView search = Ui.iconButton(this, R.drawable.ic_search, Palette.TEXT, Palette.GOLD);
        search.setContentDescription("Buscar");
        top.addView(search, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        ImageView menu = Ui.iconButton(this, R.drawable.ic_menu, Palette.TEXT, Palette.GOLD);
        LinearLayout.LayoutParams mp = Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48));
        mp.leftMargin = Ui.dp(this, 8);
        top.addView(menu, mp);
        menu.setContentDescription("Menu");
        menuButton = menu;
        content.addView(top, Ui.lp(-1, -2));
        search.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { askSearch(); }
        });
        menu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openDrawer(); }
        });

        LinearLayout body = Ui.row(this);
        body.setGravity(Gravity.TOP);
        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(-1, 0, 1f);
        bodyLp.topMargin = Ui.dp(this, 8);
        content.addView(body, bodyLp);

        body.addView(buildLeftPanel(), new LinearLayout.LayoutParams(0, -1, 0.36f));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(0, -1, 0.64f);
        rp.leftMargin = Ui.dp(this, 24);
        body.addView(buildRightPanel(), rp);

        drawer = buildDrawer();
        drawer.setVisibility(View.GONE);
        root.addView(drawer, new FrameLayout.LayoutParams(-1, -1));

        setContentView(root);
    }

    private View buildLeftPanel() {
        LinearLayout left = Ui.column(this);

        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 6));
        backButton = Ui.iconButton(this, R.drawable.ic_back, Palette.TEXT, Palette.GOLD);
        backButton.setContentDescription("Voltar");
        header.addView(backButton, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        headerTitle = Ui.text(this, "GRUPOS DE CANAIS", 16, Palette.TEXT, true);
        headerTitle.setSingleLine(true);
        headerTitle.setEllipsize(TextUtils.TruncateAt.END);
        headerTitle.setPadding(Ui.dp(this, 10), 0, Ui.dp(this, 6), 0);
        header.addView(headerTitle, Ui.weight(1));
        ImageView zoomOut = Ui.iconButton(this, R.drawable.ic_zoom_out, Palette.TEXT, Palette.GOLD);
        ImageView zoomIn = Ui.iconButton(this, R.drawable.ic_zoom_in, Palette.TEXT, Palette.GOLD);
        zoomOut.setContentDescription("Diminuir");
        zoomIn.setContentDescription("Aumentar");
        header.addView(zoomOut, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        header.addView(zoomIn, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        left.addView(header, Ui.lp(-1, -2));

        View line = new View(this);
        line.setBackgroundColor(Palette.GOLD);
        left.addView(line, Ui.lp(-1, Ui.dp(this, 3)));

        list = new ListView(this);
        list.setDivider(new android.graphics.drawable.ColorDrawable(0));
        list.setDividerHeight(Ui.dp(this, 6));
        list.setSelector(new android.graphics.drawable.ColorDrawable(0));
        list.setPadding(Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8), Ui.dp(this, 8));
        list.setClipToPadding(false);
        list.setVerticalScrollBarEnabled(false);
        list.setItemsCanFocus(false);
        adapter = new ListAdapter();
        list.setAdapter(adapter);
        left.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        backButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showGroups(); }
        });
        zoomOut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setZoom(zoom - 0.1f); }
        });
        zoomIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { setZoom(zoom + 0.1f); }
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                if (!showingChannels) {
                    openGroup(groups.get(pos));
                } else {
                    Channel c = channelItems.get(pos);
                    if (c == playing) openFullscreen();
                    else {
                        playingList = channelItems;
                        select(c, true);
                    }
                }
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                if (!showingChannels) return false;
                Channel c = channelItems.get(pos);
                boolean on = Repo.toggleFav(Repo.liveKey(c));
                Toast.makeText(LiveActivity.this, on ? "★ Adicionado aos favoritos" : "Removido dos favoritos",
                        Toast.LENGTH_SHORT).show();
                if (FAVS.equals(groupTitle)) openGroup(FAVS);
                return true;
            }
        });
        return left;
    }

    private View buildRightPanel() {
        LinearLayout right = Ui.column(this);

        FrameLayout frame = new FrameLayout(this);
        frame.setBackground(Ui.round(Palette.SURFACE, Ui.dp(this, 14)));
        Ui.clipRound(frame, Ui.dp(this, 14));
        frame.setFocusable(true);
        frame.setClickable(true);
        frame.setForeground(Ui.selectable(this, 0, 0x00000000, Ui.dp(this, 14), Palette.GOLD));
        preview = new VideoView(this);
        frame.addView(preview, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        previewLoading = new ProgressBar(this);
        previewLoading.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Palette.GOLD));
        previewLoading.setVisibility(View.GONE);
        frame.addView(previewLoading, new FrameLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48), Gravity.CENTER));
        previewMessage = Ui.text(this, "", 15, Palette.TEXT_DIM, false);
        previewMessage.setGravity(Gravity.CENTER);
        frame.addView(previewMessage, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        LinearLayout.LayoutParams fp = new LinearLayout.LayoutParams(-1, 0, 1f);
        fp.leftMargin = Ui.dp(this, 40);
        fp.rightMargin = Ui.dp(this, 40);
        right.addView(frame, fp);
        frame.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { if (playing != null) openFullscreen(); }
        });

        preview.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                previewLoading.setVisibility(View.GONE);
                previewMessage.setText("");
                preview.start();
            }
        });
        preview.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) previewLoading.setVisibility(View.VISIBLE);
                else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END
                        || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) previewLoading.setVisibility(View.GONE);
                return false;
            }
        });
        preview.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                previewLoading.setVisibility(View.GONE);
                previewMessage.setText("Canal indisponível no momento");
                return true;
            }
        });

        // Now / next card
        LinearLayout epg = Ui.row(this);
        epg.setBackground(Ui.round(Palette.SURFACE, Ui.dp(this, 14)));
        int p = Ui.dp(this, 16);
        epg.setPadding(Ui.dp(this, 24), p, p, p);
        epgLogo = new ImageView(this);
        epgLogo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        epg.addView(epgLogo, Ui.lp(Ui.dp(this, 56), Ui.dp(this, 48)));

        LinearLayout lines = Ui.column(this);
        lines.setPadding(Ui.dp(this, 28), 0, 0, 0);
        TextView nowLabel = Ui.text(this, "AGORA", 14, Palette.GOLD, true);
        nowTime = Ui.text(this, "", 15, Palette.GOLD, false);
        nowTitle = Ui.text(this, "", 15, Palette.TEXT, false);
        nextLabel = Ui.text(this, "PRÓXIMO", 14, Palette.TEXT_DIM, true);
        nextTime = Ui.text(this, "", 15, Palette.TEXT_DIM, false);
        nextTitle = Ui.text(this, "", 15, Palette.TEXT_DIM, false);
        lines.addView(epgLine(nowLabel, nowTime, nowTitle));
        View gap = new View(this);
        lines.addView(gap, Ui.lp(1, Ui.dp(this, 6)));
        lines.addView(epgLine(nextLabel, nextTime, nextTitle));
        epg.addView(lines, Ui.weight(1));

        LinearLayout.LayoutParams ep = Ui.lp(-1, -2);
        ep.topMargin = Ui.dp(this, 14);
        right.addView(epg, ep);
        return right;
    }

    private View epgLine(TextView label, TextView time, TextView title) {
        LinearLayout l = Ui.row(this);
        l.addView(label, Ui.lp(Ui.dp(this, 96), -2));
        l.addView(time, Ui.lp(Ui.dp(this, 116), -2));
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        l.addView(title, Ui.weight(1));
        return l;
    }

    private FrameLayout buildDrawer() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x99000000);
        overlay.setClickable(true);
        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { closeDrawer(); }
        });

        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Palette.BLACK);
        panel.setClickable(true);
        panel.setPadding(Ui.dp(this, 16), Ui.dp(this, 64), Ui.dp(this, 16), Ui.dp(this, 16));
        overlay.addView(panel, new FrameLayout.LayoutParams(Ui.dp(this, 300), -1, Gravity.END));

        android.widget.ScrollView scroll = new android.widget.ScrollView(this);
        LinearLayout items = Ui.column(this);
        drawerItems = items;
        scroll.addView(items);
        panel.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        drawerItem(items, R.drawable.ic_arrow_right, "TV AO VIVO", new Runnable() {
            public void run() { showGroups(); }
        });
        drawerItem(items, R.drawable.ic_star, "FAVORITOS", new Runnable() {
            public void run() { openGroup(FAVS); }
        });
        if (Repo.isXtream()) {
            drawerItem(items, R.drawable.ic_arrow_right, "FILMES", new Runnable() {
                public void run() { openVod(VodActivity.TAB_MOVIES); }
            });
            drawerItem(items, R.drawable.ic_arrow_right, "SÉRIES", new Runnable() {
                public void run() { openVod(VodActivity.TAB_SERIES); }
            });
            drawerItem(items, R.drawable.ic_person, "MINHA CONTA", new Runnable() {
                public void run() { openVod(VodActivity.TAB_PROFILE); }
            });
        }
        drawerItem(items, R.drawable.ic_refresh, "ATUALIZAR LISTA", new Runnable() {
            public void run() { reload(); }
        });
        drawerItem(items, R.drawable.ic_logout, "SAIR", new Runnable() {
            public void run() { confirmLogout(); }
        });
        return overlay;
    }

    private void drawerItem(LinearLayout parent, int icon, String label, final Runnable action) {
        LinearLayout item = Ui.row(this);
        item.setFocusable(true);
        item.setClickable(true);
        item.setBackground(Ui.selectable(this, 0, Palette.SURFACE_HI, Ui.dp(this, 12), Palette.GOLD));
        int p = Ui.dp(this, 14);
        item.setPadding(p, p, p, p);
        item.addView(Ui.icon(this, icon, Palette.TEXT), Ui.lp(Ui.dp(this, 22), Ui.dp(this, 22)));
        TextView t = Ui.text(this, label, 20, Palette.TEXT, false);
        t.setPadding(Ui.dp(this, 18), 0, 0, 0);
        item.addView(t);
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.bottomMargin = Ui.dp(this, 6);
        parent.addView(item, lp);
        item.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                closeDrawer();
                action.run();
            }
        });
    }

    // ---------------------------------------------------------------- list state

    private void showGroups() {
        showingChannels = false;
        groupTitle = null;
        groups = new ArrayList<String>();
        groups.add(ALL);
        groups.add(FAVS);
        groups.addAll(Repo.liveGroups);
        headerTitle.setText("GRUPOS DE CANAIS");
        backButton.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
        list.setSelection(0);
    }

    private void openGroup(String group) {
        List<Channel> out = new ArrayList<Channel>();
        for (Channel c : Repo.channels) {
            if (ALL.equals(group) || (FAVS.equals(group) ? Repo.isFav(Repo.liveKey(c)) : group.equals(c.group))) {
                out.add(c);
            }
        }
        showChannels(group, out);
        if (out.isEmpty() && FAVS.equals(group)) {
            Toast.makeText(this, "Segure o dedo (ou OK) em um canal para favoritar", Toast.LENGTH_LONG).show();
        }
    }

    private void showChannels(String title, List<Channel> items) {
        showingChannels = true;
        groupTitle = title;
        channelItems = items;
        headerTitle.setText(title.toUpperCase(Locale.getDefault()));
        backButton.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        int idx = playing == null ? -1 : items.indexOf(playing);
        list.setSelection(Math.max(0, idx - 2));
        list.requestFocus();
    }

    private void askSearch() {
        final EditText input = new EditText(this);
        input.setHint("Nome do canal");
        input.setSingleLine(true);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(Ui.dp(this, 20), Ui.dp(this, 8), Ui.dp(this, 20), 0);
        box.addView(input, new LinearLayout.LayoutParams(-1, -2));
        new AlertDialog.Builder(this)
                .setTitle("Buscar canal")
                .setView(box)
                .setPositiveButton("Buscar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        String q = input.getText().toString().trim().toLowerCase(Locale.ROOT);
                        if (q.isEmpty()) return;
                        List<Channel> out = new ArrayList<Channel>();
                        for (Channel c : Repo.channels) {
                            if (c.name.toLowerCase(Locale.ROOT).contains(q)) out.add(c);
                        }
                        showChannels("Busca: " + input.getText().toString().trim(), out);
                        if (out.isEmpty()) Toast.makeText(LiveActivity.this, "Nenhum canal encontrado",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setZoom(float z) {
        zoom = Math.max(0.8f, Math.min(1.5f, Math.round(z * 10) / 10f));
        Repo.setListZoom(zoom);
        adapter.notifyDataSetChanged();
    }

    // ---------------------------------------------------------------- playback

    private void select(Channel c, boolean userAction) {
        playing = c;
        Repo.current = c;
        Repo.prefs().edit().putString("last_channel", c.url).apply();
        adapter.notifyDataSetChanged();
        showEpg(c);
        // On first launch onResume starts the preview.
        if (userAction) startPreview();
    }

    private void startPreview() {
        if (playing == null) return;
        previewLoading.setVisibility(View.VISIBLE);
        previewMessage.setText("");
        preview.stopPlayback();
        preview.setVideoURI(Uri.parse(playing.url));
    }

    private void showEpg(final Channel c) {
        ImageLoader.load(c.logo, epgLogo, Ui.dp(this, 56), R.drawable.ic_launcher);
        nowTime.setText("");
        nowTitle.setText(c.name);
        nextLabel.setVisibility(View.INVISIBLE);
        nextTime.setText("");
        nextTitle.setText("");
        if (c.streamId == null) return;
        Task.run(this, new Callable<List<Xtream.Program>>() {
            public List<Xtream.Program> call() throws Exception { return Xtream.shortEpg(c.streamId); }
        }, new Task.Done<List<Xtream.Program>>() {
            public void ok(List<Xtream.Program> programs) {
                if (c != playing || programs.isEmpty()) return;
                Xtream.Program now = programs.get(0);
                nowTime.setText(range(now));
                nowTitle.setText(now.title);
                if (programs.size() > 1) {
                    Xtream.Program next = programs.get(1);
                    nextLabel.setVisibility(View.VISIBLE);
                    nextTime.setText(range(next));
                    nextTitle.setText(next.title);
                }
            }

            public void fail(Exception e) {}
        });
    }

    private String range(Xtream.Program p) {
        return clock.format(new Date(p.start)) + " - " + clock.format(new Date(p.end));
    }

    private void openFullscreen() {
        Repo.playlist = playingList;
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra(PlayerActivity.EXTRA_INDEX, Math.max(0, playingList.indexOf(playing)));
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
        if (Repo.current != null && Repo.current != playing) {
            // The user zapped to another channel in fullscreen.
            playing = Repo.current;
            playingList = Repo.playlist;
            adapter.notifyDataSetChanged();
            showEpg(playing);
        }
        startPreview();
    }

    // ---------------------------------------------------------------- menu actions

    private void openDrawer() {
        drawer.setVisibility(View.VISIBLE);
        drawerItems.getChildAt(0).requestFocus();
    }

    private void closeDrawer() {
        drawer.setVisibility(View.GONE);
        menuButton.requestFocus();
    }

    private void openVod(int tab) {
        Intent i = new Intent(this, VodActivity.class);
        i.putExtra(VodActivity.EXTRA_TAB, tab);
        startActivity(i);
    }

    private void reload() {
        Toast.makeText(this, "Atualizando lista…", Toast.LENGTH_SHORT).show();
        Task.run(this, new Callable<Void>() {
            public Void call() throws Exception {
                if (Repo.isXtream()) Xtream.loadLive();
                else {
                    List<Channel> l = M3uParser.download(Repo.m3uUrl);
                    List<String> g = new ArrayList<String>();
                    for (Channel c : l) if (!g.contains(c.group)) g.add(c.group);
                    Repo.channels = l;
                    Repo.liveGroups = g;
                }
                return null;
            }
        }, new Task.Done<Void>() {
            public void ok(Void v) {
                Toast.makeText(LiveActivity.this, Repo.channels.size() + " canais carregados",
                        Toast.LENGTH_SHORT).show();
                showGroups();
            }

            public void fail(Exception e) {
                Toast.makeText(LiveActivity.this, "Erro: " + Task.message(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sair da conta?")
                .setMessage("Você poderá entrar com outro login ou lista.")
                .setPositiveButton("Sair", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        Repo.logout();
                        Intent i = new Intent(LiveActivity.this, LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (drawer.getVisibility() == View.VISIBLE) closeDrawer();
        else if (showingChannels) showGroups();
        else super.onBackPressed();
    }

    // ---------------------------------------------------------------- adapter

    private class ListAdapter extends BaseAdapter {
        public int getCount() { return showingChannels ? channelItems.size() : groups.size(); }
        public Object getItem(int pos) { return null; }
        public long getItemId(int pos) { return pos; }

        public View getView(int pos, View convert, ViewGroup parent) {
            Holder h = convert == null ? new Holder() : (Holder) convert.getTag();
            int height = (int) (Ui.dp(LiveActivity.this, 62) * zoom);
            h.card.setLayoutParams(new android.widget.AbsListView.LayoutParams(-1, height));
            h.name.setTextSize(17 * zoom);
            if (showingChannels) {
                Channel c = channelItems.get(pos);
                boolean on = c == playing;
                h.name.setText(c.name);
                h.name.setTextColor(on ? Palette.GOLD : Palette.TEXT);
                h.name.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                h.extra.setText(Repo.isFav(Repo.liveKey(c)) ? "★" : "");
                h.extra.setTextColor(Palette.GOLD);
                h.logo.setVisibility(View.VISIBLE);
                ImageLoader.load(c.logo, h.logo, Ui.dp(LiveActivity.this, 56), 0);
                h.bar.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
            } else {
                String g = groups.get(pos);
                h.name.setText(g.toUpperCase(Locale.getDefault()));
                h.name.setTextColor(Palette.TEXT);
                h.extra.setText(count(g));
                h.extra.setTextColor(Palette.TEXT_DIM);
                h.logo.setVisibility(View.GONE);
                h.bar.setVisibility(playing != null && g.equals(playing.group) ? View.VISIBLE : View.INVISIBLE);
            }
            return h.card;
        }

        private String count(String g) {
            int n = 0;
            if (ALL.equals(g)) return String.valueOf(Repo.channels.size());
            for (Channel c : Repo.channels) {
                if (FAVS.equals(g) ? Repo.isFav(Repo.liveKey(c)) : g.equals(c.group)) n++;
            }
            return String.valueOf(n);
        }
    }

    private class Holder {
        final FrameLayout card;
        final View bar;
        final ImageView logo;
        final TextView name;
        final TextView extra;

        Holder() {
            card = new FrameLayout(LiveActivity.this);
            float r = Ui.dp(LiveActivity.this, 10);
            card.setBackground(Ui.selectable(LiveActivity.this, Palette.SURFACE, Palette.SURFACE_HI, r, Palette.GOLD));
            Ui.clipRound(card, r);
            LinearLayout row = Ui.row(LiveActivity.this);
            card.addView(row, new FrameLayout.LayoutParams(-1, -1));

            bar = new View(LiveActivity.this);
            bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {Palette.ACCENT_TOP, Palette.ACCENT_BOTTOM}));
            row.addView(bar, Ui.lp(Ui.dp(LiveActivity.this, 5), -1));

            logo = new ImageView(LiveActivity.this);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(LiveActivity.this, 52), Ui.dp(LiveActivity.this, 36));
            lp.leftMargin = Ui.dp(LiveActivity.this, 6);
            row.addView(logo, lp);

            name = Ui.text(LiveActivity.this, "", 17, Palette.TEXT, true);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            name.setPadding(Ui.dp(LiveActivity.this, 12), 0, Ui.dp(LiveActivity.this, 8), 0);
            row.addView(name, Ui.weight(1));

            extra = Ui.text(LiveActivity.this, "", 14, Palette.TEXT_DIM, false);
            extra.setPadding(0, 0, Ui.dp(LiveActivity.this, 14), 0);
            row.addView(extra);
            card.setTag(this);
        }
    }
}
