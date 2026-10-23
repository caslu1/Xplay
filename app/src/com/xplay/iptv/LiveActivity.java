package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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

/**
 * Live TV: numbered channel groups on the left, preview + now/next on the right. Choosing a channel
 * expands the same player to fullscreen (no reload), with zapping, number entry and the channel's
 * other qualities (SD / HD / FHD / 4K) at the bottom.
 */
public class LiveActivity extends Activity implements TopBar.Listener {

    private static final String ALL = "TODOS";
    private static final String FAVS = "FAVORITOS";
    private static final long OVERLAY_MS = 6000;

    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final Handler handler = new Handler();

    private boolean showingChannels;
    private final java.util.Map<String, Integer> counts = new java.util.HashMap<String, Integer>();
    private String groupTitle;
    private List<String> groups = new ArrayList<String>();
    private List<Channel> channelItems = new ArrayList<Channel>();
    private List<Channel> playingList = new ArrayList<Channel>();
    private Channel playing;
    private float zoom;
    private boolean fullscreen;
    private String typedNumber = "";
    private float touchX, touchY;

    private TopBar topBar;
    private LinearLayout content, leftPanel, rightPanel, body;
    private View epgCard;
    private FrameLayout previewFrame;
    private ListView list;
    private ListAdapter adapter;
    private ImageView backButton;
    private TextView headerTitle;
    private VideoView preview;
    private ProgressBar previewLoading;
    private TextView previewMessage;
    private ImageView epgLogo;
    private TextView nowTime, nowTitle, nextTime, nextTitle, nextLabel;
    /** Channel list the groups were built from; changes when the lists are refreshed. */
    private List<Channel> shownChannels;

    // fullscreen overlay
    private LiveOverlay overlay;
    /** Channel whose now/next the small panel shows (follows the remote's highlight). */
    private Channel epgChannel;
    private Channel highlighted;

    private final Runnable hideOverlay = new Runnable() {
        public void run() { overlay.setVisibility(View.GONE); }
    };
    private final Runnable jumpToTyped = new Runnable() {
        public void run() { jumpTo(typedNumber); }
    };
    private int retries;
    /** Starts the player once zapping pauses, so racing through channels does not error out. */
    private final Runnable startPlayback = new Runnable() {
        public void run() { startPreview(); }
    };
    private final Runnable showHighlighted = new Runnable() {
        public void run() { if (highlighted != null) showEpg(highlighted); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        Ui.fit(this);
        if (Repo.channels == null || !Repo.hasLogin()) {
            // Process was recreated: log in again from the saved credentials.
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        zoom = Repo.listZoom();
        buildUi();
        showGroups();
        preloadCatalogs();
        Updater.checkOnStart(this);

        String last = Repo.prefs().getString("last_channel", null);
        for (Channel c : Adult.channels()) {
            if (c.url.equals(last)) {
                playingList = Adult.channels();
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

        LinearLayout page = Ui.column(this);
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));
        topBar = new TopBar(this, Repo.hasVod(), this);
        topBar.select(TopBar.LIVE);
        page.addView(topBar, Ui.lp(-1, -2));

        content = Ui.column(this);
        page.addView(content, new LinearLayout.LayoutParams(-1, 0, 1f));

        body = Ui.row(this);
        body.setGravity(Gravity.TOP);
        content.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        leftPanel = buildLeftPanel();
        body.addView(leftPanel, new LinearLayout.LayoutParams(0, -1, 0.36f));
        rightPanel = buildRightPanel();
        body.addView(rightPanel, new LinearLayout.LayoutParams(0, -1, 0.64f));

        overlay = buildOverlay();
        overlay.setVisibility(View.GONE);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        setContentView(root);
        applyMode();
    }

    private LinearLayout buildLeftPanel() {
        LinearLayout left = Ui.column(this);

        LinearLayout header = Ui.row(this);
        header.setPadding(Ui.dp(this, 4), 0, 0, Ui.dp(this, 6));
        backButton = Ui.iconButton(this, R.drawable.ic_back, Palette.TEXT, Palette.GOLD);
        backButton.setContentDescription("Voltar");
        header.addView(backButton, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        headerTitle = Ui.text(this, "CATEGORIAS", 16, Palette.TEXT, true);
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
                    playingList = channelItems;
                    // First press plays it in the small player; pressing it again goes fullscreen.
                    if (c != playing) select(c, true);
                    else enterFullscreen();
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
                else adapter.notifyDataSetChanged();
                return true;
            }
        });
        list.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (!showingChannels || pos >= channelItems.size()) return;
                // Moving through the list with the remote previews each channel's programs.
                highlighted = channelItems.get(pos);
                handler.removeCallbacks(showHighlighted);
                handler.postDelayed(showHighlighted, 200);
            }

            public void onNothingSelected(AdapterView<?> p) {}
        });
        return left;
    }

    private LinearLayout buildRightPanel() {
        LinearLayout right = Ui.column(this);

        previewFrame = new FrameLayout(this);
        previewFrame.setBackgroundColor(Palette.SURFACE);
        previewFrame.setFocusable(true);
        previewFrame.setClickable(true);
        previewFrame.setForeground(Ui.focusRing(this, Ui.dp(this, 14)));
        preview = new VideoView(this);
        previewFrame.addView(preview, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        previewLoading = new ProgressBar(this);
        previewLoading.setIndeterminateTintList(ColorStateList.valueOf(Palette.GOLD));
        previewLoading.setVisibility(View.GONE);
        previewFrame.addView(previewLoading, new FrameLayout.LayoutParams(Ui.dp(this, 48), Ui.dp(this, 48), Gravity.CENTER));
        previewMessage = Ui.text(this, "", 15, Palette.TEXT_DIM, false);
        previewMessage.setGravity(Gravity.CENTER);
        previewFrame.addView(previewMessage, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        right.addView(previewFrame, new LinearLayout.LayoutParams(-1, 0, 1f));
        previewFrame.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (fullscreen) toggleOverlay();
                else if (playing != null) enterFullscreen();
            }
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
                // Streams often fail on the first try right after a switch; retry once before giving up.
                if (retries++ < 1) {
                    handler.postDelayed(new Runnable() {
                        public void run() { load(); }
                    }, 800);
                    return true;
                }
                previewLoading.setVisibility(View.GONE);
                previewMessage.setText("Canal indisponível no momento");
                return true;
            }
        });
        preview.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) { startPreview(); }
        });

        // Now / next card
        LinearLayout epg = Ui.row(this);
        epgCard = epg;
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
        lines.addView(new View(this), Ui.lp(1, Ui.dp(this, 6)));
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

    private LiveOverlay buildOverlay() {
        LiveOverlay o = new LiveOverlay(this, new LiveOverlay.Listener() {
            public void onVariant(Channel c) {
                select(c, true);
                showOverlay();
            }
        });
        o.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { overlay.setVisibility(View.GONE); }
        });
        return o;
    }

    // ---------------------------------------------------------------- fullscreen

    private void enterFullscreen() {
        if (playing == null) return;
        fullscreen = true;
        applyMode();
        showOverlay();
    }

    private void exitFullscreen() {
        fullscreen = false;
        overlay.setVisibility(View.GONE);
        applyMode();
        list.requestFocus();
    }

    /** Hides or restores everything around the player; the player itself is never recreated. */
    private void applyMode() {
        int vis = fullscreen ? View.GONE : View.VISIBLE;
        topBar.setVisibility(vis);
        leftPanel.setVisibility(vis);
        epgCard.setVisibility(vis);
        int h = fullscreen ? 0 : Ui.dp(this, 24);
        content.setPadding(h, 0, h, fullscreen ? 0 : Ui.dp(this, 12));
        LinearLayout.LayoutParams bl = (LinearLayout.LayoutParams) body.getLayoutParams();
        bl.topMargin = fullscreen ? 0 : Ui.dp(this, 8);
        body.setLayoutParams(bl);
        LinearLayout.LayoutParams rl = (LinearLayout.LayoutParams) rightPanel.getLayoutParams();
        rl.leftMargin = fullscreen ? 0 : Ui.dp(this, 24);
        rightPanel.setLayoutParams(rl);
        previewFrame.setBackgroundColor(fullscreen ? Palette.BLACK : Palette.SURFACE);
        // In fullscreen the small player's focus ring and rounded corners must not show.
        previewFrame.setForeground(fullscreen ? null : Ui.focusRing(this, Ui.dp(this, 14)));
        previewFrame.setFocusable(!fullscreen);
        if (fullscreen) {
            previewFrame.setClipToOutline(false);
            previewFrame.setPadding(0, 0, 0, 0);
        } else {
            Ui.clipRound(previewFrame, Ui.dp(this, 14));
        }
        getWindow().getDecorView().setSystemUiVisibility(fullscreen
                ? View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                : 0);
        if (fullscreen) getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void showOverlay() {
        if (!fullscreen) return;
        overlay.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideOverlay);
        handler.postDelayed(hideOverlay, OVERLAY_MS);
    }

    private void toggleOverlay() {
        if (overlay.getVisibility() == View.VISIBLE) overlay.setVisibility(View.GONE);
        else showOverlay();
    }

    private void fillOverlay() {
        if (playing != null) overlay.bind(playing, Adult.channels());
    }

    private void switchQuality(int delta) {
        Channel v = overlay.neighbour(delta);
        if (v == null) {
            showOverlay();
            return;
        }
        select(v, true);
        showOverlay();
    }

    private void zap(int delta) {
        List<Channel> l = playingList.isEmpty() ? Adult.channels() : playingList;
        if (l.isEmpty()) return;
        int i = l.indexOf(playing);
        int n = l.size();
        select(l.get(((i + delta) % n + n) % n), true);
        showOverlay();
    }

    /** Remote number keys: jump to the channel with that number. */
    private void jumpTo(String number) {
        typedNumber = "";
        overlay.showTyped(null);
        int n;
        try {
            n = Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return;
        }
        if (Repo.channels == null || n < 1 || n > Repo.channels.size()) {
            Toast.makeText(this, "Canal " + number + " não existe", Toast.LENGTH_SHORT).show();
            return;
        }
        final Channel c = Repo.channels.get(n - 1);
        Runnable go = new Runnable() {
            public void run() {
                playingList = Adult.channels();
                select(c, true);
                if (!fullscreen) enterFullscreen();
                else showOverlay();
            }
        };
        if (Adult.isAdult(c)) {
            if (Adult.hidden()) return;
            Adult.unlock(this, go);
        } else {
            go.run();
        }
    }

    // ---------------------------------------------------------------- list state

    private void showGroups() {
        shownChannels = Repo.channels;
        showingChannels = false;
        groupTitle = null;
        groups = new ArrayList<String>();
        groups.add(ALL);
        groups.add(FAVS);
        for (String g : Repo.liveGroups) if (Adult.listed() || !Adult.isAdultName(g)) groups.add(g);
        headerTitle.setText("CATEGORIAS");
        counts.clear();
        backButton.setVisibility(View.GONE);
        adapter.notifyDataSetChanged();
        list.setSelection(0);
        list.requestFocus();
    }

    private void openGroup(final String group) {
        if (Adult.isAdultName(group)) {
            Adult.unlock(this, new Runnable() {
                public void run() { openGroupNow(group); }
            });
        } else {
            openGroupNow(group);
        }
    }

    private void openGroupNow(String group) {
        List<Channel> out = new ArrayList<Channel>();
        if (ALL.equals(group) || FAVS.equals(group)) {
            for (Channel c : Adult.channels()) {
                if (ALL.equals(group) || Repo.isFav(Repo.liveKey(c))) out.add(c);
            }
        } else {
            for (Channel c : Repo.channels) if (group.equals(c.group)) out.add(c);
        }
        showChannels(group, Variants.collapse(out));
        if (out.isEmpty() && FAVS.equals(group)) {
            Toast.makeText(this, "Segure o dedo (ou OK) em um canal para favoritar", Toast.LENGTH_LONG).show();
        }
    }

    private void showChannels(String title, List<Channel> items) {
        showingChannels = true;
        groupTitle = title;
        channelItems = items;
        headerTitle.setText(Variants.cleanGroup(title).toUpperCase(Locale.getDefault()));
        backButton.setVisibility(View.VISIBLE);
        adapter.notifyDataSetChanged();
        int idx = playing == null ? -1 : items.indexOf(playing);
        list.setSelection(Math.max(0, idx - 2));
        list.requestFocus();
    }

    private void askSearch() {
        final Popup p = new Popup(this, "Buscar canal", null);
        final EditText input = p.input("Nome ou número do canal", android.text.InputType.TYPE_CLASS_TEXT, null);
        p.button("Cancelar", false, null).button("Buscar", true, new Runnable() {
            public void run() {
                String raw = input.getText().toString().trim();
                if (raw.isEmpty()) return;
                if (raw.matches("\\d+")) {
                    jumpTo(raw);
                    return;
                }
                String q = raw.toLowerCase(Locale.ROOT);
                List<Channel> out = new ArrayList<Channel>();
                for (Channel c : Adult.channels()) {
                    if (c.name.toLowerCase(Locale.ROOT).contains(q)) out.add(c);
                }
                showChannels("Busca: " + raw, Variants.collapse(out));
                if (out.isEmpty()) Toast.makeText(LiveActivity.this, "Nenhum canal encontrado",
                        Toast.LENGTH_SHORT).show();
            }
        }).show();
    }

    private void setZoom(float z) {
        zoom = Math.max(0.8f, Math.min(1.5f, Math.round(z * 10) / 10f));
        Repo.setListZoom(zoom);
        adapter.notifyDataSetChanged();
    }

    // ---------------------------------------------------------------- playback

    private void select(Channel c, boolean startNow) {
        playing = c;
        Repo.current = c;
        Repo.prefs().edit().putString("last_channel", c.url).apply();
        adapter.notifyDataSetChanged();
        showEpg(c);
        fillOverlay();
        // On first launch onResume starts the preview.
        if (startNow) {
            preview.stopPlayback();
            previewLoading.setVisibility(View.VISIBLE);
            previewMessage.setText("");
            handler.removeCallbacks(startPlayback);
            handler.postDelayed(startPlayback, 350);
        }
    }

    private void startPreview() {
        if (playing == null) return;
        handler.removeCallbacks(startPlayback);
        retries = 0;
        load();
    }

    private void load() {
        previewLoading.setVisibility(View.VISIBLE);
        previewMessage.setText("");
        preview.stopPlayback();
        preview.setVideoURI(Uri.parse(playing.url));
    }

    private void showEpg(final Channel c) {
        epgChannel = c;
        ImageLoader.load(c.logo, epgLogo, Ui.dp(this, 56), R.drawable.ic_launcher);
        nowTime.setText("");
        nowTitle.setText(Variants.displayName(c));
        nextLabel.setVisibility(View.INVISIBLE);
        nextTime.setText("");
        nextTitle.setText("");
        Epg.get(c, new Epg.Callback() {
            public void done(List<Xtream.Program> programs) {
                if (isFinishing() || c != epgChannel || programs.isEmpty()) return;
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
        });
    }

    private String range(Xtream.Program p) {
        return clock.format(new Date(p.start)) + " - " + clock.format(new Date(p.end));
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
        topBar.select(TopBar.LIVE);
        if (Repo.channels != shownChannels || (!showingChannels && groups.size() - 2 != countListedGroups())) {
            showGroups();
        }
        if (Repo.current != null && Repo.current != playing) {
            // Another screen (sports, favorites) played a channel.
            playing = Repo.current;
            playingList = Repo.playlist;
            adapter.notifyDataSetChanged();
            showEpg(playing);
            fillOverlay();
        }
        startPreview();
    }

    private int countListedGroups() {
        int n = 0;
        for (String g : Repo.liveGroups) if (Adult.listed() || !Adult.isAdultName(g)) n++;
        return n;
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            typedNumber += (char) ('0' + keyCode - KeyEvent.KEYCODE_0);
            if (typedNumber.length() > 5) typedNumber = typedNumber.substring(typedNumber.length() - 5);
            if (fullscreen) {
                overlay.showTyped(typedNumber);
                showOverlay();
            } else {
                Toast.makeText(this, "Canal " + typedNumber, Toast.LENGTH_SHORT).show();
            }
            handler.removeCallbacks(jumpToTyped);
            handler.postDelayed(jumpToTyped, 1500);
            return true;
        }
        if (!fullscreen && showingChannels && keyCode == KeyEvent.KEYCODE_DPAD_LEFT && list.hasFocus()) {
            showGroups();
            return true;
        }
        if (fullscreen) {
            boolean visible = overlay.getVisibility() == View.VISIBLE;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_CHANNEL_UP:
                case KeyEvent.KEYCODE_PAGE_UP:
                    zap(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_CHANNEL_DOWN:
                case KeyEvent.KEYCODE_PAGE_DOWN:
                    zap(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    switchQuality(-1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    switchQuality(1);
                    return true;
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (visible) overlay.setVisibility(View.GONE);
                    else showOverlay();
                    return true;
                default:
                    break;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    /** In fullscreen: swipe up/down changes channel, sideways changes quality, a tap shows the info. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (!fullscreen) return super.dispatchTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            touchX = ev.getX();
            touchY = ev.getY();
            if (overlay.getVisibility() != View.VISIBLE) return true;
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            float dy = ev.getY() - touchY, dx = ev.getX() - touchX;
            if (Math.abs(dy) > getResources().getDisplayMetrics().heightPixels / 5f && Math.abs(dy) > Math.abs(dx)) {
                zap(dy < 0 ? 1 : -1);
                return true;
            }
            if (Math.abs(dx) > getResources().getDisplayMetrics().widthPixels / 6f && Math.abs(dx) > Math.abs(dy)) {
                switchQuality(dx < 0 ? 1 : -1);
                return true;
            }
            if (overlay.getVisibility() != View.VISIBLE) {
                showOverlay();
                return true;
            }
        } else if (overlay.getVisibility() != View.VISIBLE) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onBackPressed() {
        if (fullscreen) {
            if (overlay.getVisibility() == View.VISIBLE) overlay.setVisibility(View.GONE);
            else exitFullscreen();
        } else if (showingChannels) {
            showGroups();
        } else if (!topBar.hasFocusInside() && topBar.focusSelected()) {
            // Back from the categories lifts the focus to the top bar; Back again leaves.
        } else {
            super.onBackPressed();
        }
    }

    // ---------------------------------------------------------------- menu actions

    public void onTopBar(int item) {
        switch (item) {
            case TopBar.LIVE:
                showGroups();
                break;
            case TopBar.FAVORITES:
                if (Repo.hasVod()) TopBar.open(this, item);
                else openGroup(FAVS);
                break;
            case TopBar.SEARCH:
                askSearch();
                break;

            default:
                TopBar.open(this, item);
                break;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int item = intent.getIntExtra(TopBar.EXTRA_ITEM, TopBar.LIVE);
        if (item != TopBar.LIVE) onTopBar(item);
    }

    /** Loads movies and series in the background so those tabs open instantly. */
    private void preloadCatalogs() {
        if (!Repo.isXtream()) return;
        Task.run(this, new Callable<Void>() {
            public Void call() throws Exception {
                Xtream.loadMovies();
                Xtream.loadSeries();
                return null;
            }
        }, new Task.Done<Void>() {
            public void ok(Void v) {}
            public void fail(Exception e) {}
        });
    }

    private final Runnable refreshList = new Runnable() {
        public void run() { adapter.notifyDataSetChanged(); }
    };

    /** Coalesces many EPG answers into one list refresh. */
    private void scheduleRefresh() {
        list.removeCallbacks(refreshList);
        list.postDelayed(refreshList, 150);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
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
            h.number.setTextSize(14 * zoom);
            if (showingChannels) {
                Channel c = channelItems.get(pos);
                boolean on = c == playing;
                h.number.setVisibility(View.VISIBLE);
                h.number.setText(Ui.pad3(Adult.number(c)));
                h.name.setText(Variants.displayName(c));
                h.name.setTextColor(on ? Palette.GOLD : Palette.TEXT);
                h.extra.setText(Repo.isFav(Repo.liveKey(c)) ? "★" : "");
                h.extra.setTextColor(Palette.GOLD);
                h.logo.setVisibility(View.VISIBLE);
                ImageLoader.load(c.logo, h.logo, Ui.dp(LiveActivity.this, 56), 0);
                h.bar.setVisibility(on ? View.VISIBLE : View.INVISIBLE);
                h.now.setTextSize(13 * zoom);
                Xtream.Program p = Epg.now(c);
                h.now.setText(p == null ? "" : p.title);
                h.now.setVisibility(p == null ? View.GONE : View.VISIBLE);
                if (p == null && Epg.peek(c) == null) {
                    Epg.get(c, new Epg.Callback() {
                        public void done(List<Xtream.Program> programs) {
                            if (!programs.isEmpty() && !isFinishing()) scheduleRefresh();
                        }
                    });
                }
            } else {
                String g = groups.get(pos);
                h.number.setVisibility(View.GONE);
                h.name.setText(Variants.cleanGroup(g).toUpperCase(Locale.getDefault()));
                h.name.setTextColor(Palette.TEXT);
                boolean locked = Adult.isAdultName(g) && !Adult.visible();
                h.extra.setText((locked ? "🔒  " : "") + count(g));
                h.extra.setTextColor(Palette.TEXT_DIM);
                h.logo.setVisibility(View.GONE);
                h.now.setVisibility(View.GONE);
                h.bar.setVisibility(playing != null && g.equals(playing.group) ? View.VISIBLE : View.INVISIBLE);
            }
            return h.card;
        }

        /** Channels a group shows once qualities are merged; computed once per group. */
        private String count(String g) {
            Integer cached = counts.get(g);
            if (cached != null) return String.valueOf(cached);
            List<Channel> in = new ArrayList<Channel>();
            for (Channel c : ALL.equals(g) || FAVS.equals(g) ? Adult.channels() : Repo.channels) {
                if (ALL.equals(g) || (FAVS.equals(g) ? Repo.isFav(Repo.liveKey(c)) : g.equals(c.group))) in.add(c);
            }
            int n = Variants.collapse(in).size();
            counts.put(g, n);
            return String.valueOf(n);
        }
    }

    private class Holder {
        final FrameLayout card;
        final View bar;
        final TextView number;
        final ImageView logo;
        final TextView name;
        final TextView now;
        final TextView extra;

        Holder() {
            Activity a = LiveActivity.this;
            card = new FrameLayout(a);
            float r = Ui.dp(a, 10);
            card.setBackground(Ui.selectable(a, Palette.SURFACE, Palette.SURFACE_HI, r, Palette.GOLD));
            Ui.clipRound(card, r);
            LinearLayout row = Ui.row(a);
            card.addView(row, new FrameLayout.LayoutParams(-1, -1));

            bar = new View(a);
            bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[] {Palette.ACCENT_TOP, Palette.ACCENT_BOTTOM}));
            row.addView(bar, Ui.lp(Ui.dp(a, 5), -1));

            number = Ui.text(a, "", 14, Palette.GOLD, true);
            number.setGravity(Gravity.CENTER);
            row.addView(number, Ui.lp(Ui.dp(a, 46), -2));

            logo = new ImageView(a);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            row.addView(logo, Ui.lp(Ui.dp(a, 52), Ui.dp(a, 36)));

            LinearLayout texts = Ui.column(a);
            texts.setPadding(Ui.dp(a, 12), 0, Ui.dp(a, 8), 0);
            name = Ui.text(a, "", 17, Palette.TEXT, true);
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(name);
            now = Ui.text(a, "", 13, Palette.TEXT_DIM, false);
            now.setSingleLine(true);
            now.setEllipsize(TextUtils.TruncateAt.END);
            texts.addView(now);
            row.addView(texts, Ui.weight(1));

            extra = Ui.text(a, "", 14, Palette.TEXT_DIM, false);
            extra.setPadding(0, 0, Ui.dp(a, 14), 0);
            row.addView(extra);
            card.setTag(this);
        }
    }
}
