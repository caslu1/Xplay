package com.xplay.iptv;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaPlayer;
import android.media.TimedText;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Fullscreen player.
 * Live (no EXTRA_URL): zaps through Repo.playlist and offers the channel's other qualities.
 * Movies/episodes: seek bar, ±10/30s, audio and subtitle tracks, next episode, resume position.
 */
public class PlayerActivity extends Activity {

    static final String EXTRA_INDEX = "index";
    static final String EXTRA_URL = "url";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_SUBTITLE = "subtitle";
    /** VodItem.favKey() of the movie or series, for "continue watching". */
    static final String EXTRA_KEY = "key";
    /** Following episodes (urls and "T1:E2  Title" labels) to play after this one. */
    static final String EXTRA_NEXT_URLS = "next_urls";
    static final String EXTRA_NEXT_SUBS = "next_subs";

    private static final long OVERLAY_MS = 6000;

    private final Handler handler = new Handler();
    private final Runnable hideOverlay = new Runnable() {
        public void run() {
            if (!seeking) overlay.setVisibility(View.GONE);
        }
    };
    private final Runnable tick = new Runnable() {
        public void run() {
            updateVodProgress();
            handler.postDelayed(this, 500);
        }
    };
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());

    private boolean live;
    private List<Channel> channels;
    private int index;
    private Channel channel;
    private String vodUrl, vodTitle, vodSubtitle, vodKey;
    private final List<String> nextUrls = new ArrayList<String>();
    private final List<String> nextSubs = new ArrayList<String>();
    private boolean paused, finished, seeking;
    /** Pending seek position while the remote keys are being pressed, or -1. */
    private int seekTarget = -1;
    private int retries;
    private final java.util.Set<Channel> tried = java.util.Collections.newSetFromMap(
            new java.util.IdentityHashMap<Channel, Boolean>());
    private final Runnable applySeek = new Runnable() {
        public void run() { if (seekTarget >= 0) seekNow(seekTarget); }
    };
    private final Runnable startLive = new Runnable() {
        public void run() {
            retries = 0;
            video.setVideoURI(Uri.parse(channel.url));
        }
    };
    private float touchY, touchX;
    private MediaPlayer player;

    private VideoView video;
    private ProgressBar loading;
    private TextView message, subtitleText;
    private FrameLayout overlay;

    // live overlay
    private LiveOverlay liveInfo;

    // vod overlay
    private TextView vodTitleView, vodSubView, position, duration;
    private SeekBar seek;
    private ImageView playPause;
    private View nextButton, episodesButton;
    private LinearLayout controlRow;
    private ImageView pauseSign;
    private TextView clockView, endsAt;
    /** ▼ on a series: the following episodes as cards. */
    private LinearLayout episodesPanel, episodesStrip;
    /** "A seguir" card shown in the last seconds of an episode. */
    private LinearLayout upNext;
    private TextView upNextTitle;
    private boolean upNextDismissed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        Ui.fit(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        vodUrl = getIntent().getStringExtra(EXTRA_URL);
        live = vodUrl == null;
        channels = Repo.playlist;
        if (live && (channels == null || channels.isEmpty())) {
            finish();
            return;
        }
        if (live) {
            index = Math.max(0, Math.min(getIntent().getIntExtra(EXTRA_INDEX, 0), channels.size() - 1));
            channel = channels.get(index);
        } else {
            vodTitle = getIntent().getStringExtra(EXTRA_TITLE);
            vodSubtitle = getIntent().getStringExtra(EXTRA_SUBTITLE);
            vodKey = getIntent().getStringExtra(EXTRA_KEY);
            String[] urls = getIntent().getStringArrayExtra(EXTRA_NEXT_URLS);
            String[] subs = getIntent().getStringArrayExtra(EXTRA_NEXT_SUBS);
            if (urls != null && subs != null && urls.length == subs.length) {
                for (int i = 0; i < urls.length; i++) {
                    nextUrls.add(urls[i]);
                    nextSubs.add(subs[i]);
                }
            }
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Palette.BLACK);
        video = new VideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        subtitleText = Ui.text(this, "", 22, Palette.TEXT, true);
        subtitleText.setGravity(Gravity.CENTER);
        subtitleText.setShadowLayer(6, 0, 2, Palette.BLACK);
        subtitleText.setBackgroundColor(0x66000000);
        subtitleText.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4));
        subtitleText.setVisibility(View.GONE);
        FrameLayout.LayoutParams sl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        sl.bottomMargin = Ui.dp(this, 48);
        root.addView(subtitleText, sl);
        loading = new ProgressBar(this);
        loading.setIndeterminateTintList(ColorStateList.valueOf(Palette.GOLD));
        root.addView(loading, new FrameLayout.LayoutParams(Ui.dp(this, 56), Ui.dp(this, 56), Gravity.CENTER));
        message = Ui.text(this, "", 18, Palette.TEXT, true);
        message.setGravity(Gravity.CENTER);
        message.setShadowLayer(8, 0, 0, Palette.BLACK);
        root.addView(message, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        overlay = new FrameLayout(this);
        overlay.setClickable(true);
        overlay.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { overlay.setVisibility(View.GONE); }
        });
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));
        if (live) buildLiveOverlay();
        else buildVodOverlay();
        setContentView(root);

        video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                player = mp;
                loading.setVisibility(View.GONE);
                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    public void onSeekComplete(MediaPlayer m) { loading.setVisibility(View.GONE); }
                });
                if (!live) {
                    resumePosition();
                    mp.setOnTimedTextListener(new MediaPlayer.OnTimedTextListener() {
                        public void onTimedText(MediaPlayer m, TimedText text) {
                            String t = text == null ? null : text.getText();
                            subtitleText.setText(t == null ? "" : t.trim());
                            subtitleText.setVisibility(t == null || t.trim().isEmpty() ? View.GONE : View.VISIBLE);
                        }
                    });
                }
                video.start();
                updatePlayPause();
            }
        });
        video.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) loading.setVisibility(View.VISIBLE);
                else if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END
                        || what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) loading.setVisibility(View.GONE);
                return false;
            }
        });
        video.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            public boolean onError(MediaPlayer mp, int what, int extra) {
                if (retries++ < 1) {
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            video.stopPlayback();
                            video.setVideoURI(Uri.parse(live ? channel.url : vodUrl));
                        }
                    }, 800);
                    return true;
                }
                if (live) {
                    // Off the air: try the same channel in another quality before giving up.
                    tried.add(channel);
                    Channel other = Variants.untried(channel, Adult.channels(), tried);
                    if (other != null) {
                        Toast.makeText(PlayerActivity.this, "Canal fora do ar, abrindo em " + Variants.quality(other),
                                Toast.LENGTH_SHORT).show();
                        channel = other;
                        play();
                        return true;
                    }
                }
                loading.setVisibility(View.GONE);
                message.setText(live ? "Canal indisponível no momento" : "Não foi possível reproduzir este conteúdo");
                showOverlay();
                return true;
            }
        });
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) {
                if (live) {
                    play();
                    return;
                }
                Repo.prefs().edit().remove("pos:" + vodUrl).apply();
                if (!nextUrls.isEmpty()) {
                    playNext();
                } else {
                    if (vodKey != null) Repo.removeProgress(vodKey);
                    finished = true;
                    finish();
                }
            }
        });

        play();
    }

    // ---------------------------------------------------------------- live overlay

    private void buildLiveOverlay() {
        liveInfo = new LiveOverlay(this, new LiveOverlay.Listener() {
            public void onVariant(Channel c) {
                tried.clear();
                channel = c;
                play();
            }
        });
        liveInfo.setClickable(false);
        overlay.addView(liveInfo, new FrameLayout.LayoutParams(-1, -1));
    }

    /** ◀ ▶ or a sideways swipe: the next quality of the same channel. */
    private void switchQuality(int delta) {
        Channel v = liveInfo.neighbour(delta);
        if (v == null) {
            showOverlay();
            return;
        }
        tried.clear();
        channel = v;
        play();
    }

    // ---------------------------------------------------------------- vod overlay

    private void buildVodOverlay() {
        LinearLayout top = Ui.row(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xE6000000, 0x00000000}));
        int p = Ui.dp(this, 28);
        top.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), p, Ui.dp(this, 56));
        ImageView back = Ui.iconButton(this, R.drawable.ic_back, Palette.TEXT, Palette.GOLD);
        back.setContentDescription("Voltar");
        top.addView(back, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        LinearLayout titles = Ui.column(this);
        titles.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        vodTitleView = Ui.text(this, vodTitle, 20, Palette.TEXT, true);
        vodTitleView.setSingleLine(true);
        vodTitleView.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(vodTitleView);
        vodSubView = Ui.text(this, "", 14, Palette.GOLD, true);
        vodSubView.setSingleLine(true);
        vodSubView.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(vodSubView);
        top.addView(titles, Ui.weight(1));
        LinearLayout clocks = Ui.column(this);
        clocks.setGravity(Gravity.END);
        clockView = Ui.text(this, "", 20, Palette.TEXT, true);
        clocks.addView(clockView);
        endsAt = Ui.text(this, "", 12, Palette.TEXT_DIM, false);
        clocks.addView(endsAt);
        top.addView(clocks);
        overlay.addView(top, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        // Big pause sign in the middle while paused (not a control: the controls are in the bottom row).
        pauseSign = Ui.icon(this, R.drawable.ic_pause, Palette.TEXT);
        pauseSign.setBackground(Ui.round(0x99000000, Ui.dp(this, 40)));
        int ps = Ui.dp(this, 20);
        pauseSign.setPadding(ps, ps, ps, ps);
        pauseSign.setVisibility(View.GONE);
        overlay.addView(pauseSign, new FrameLayout.LayoutParams(Ui.dp(this, 80), Ui.dp(this, 80), Gravity.CENTER));

        LinearLayout bottom = Ui.column(this);
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {0xF2000000, 0xB3000000, 0x00000000}));
        bottom.setPadding(p, Ui.dp(this, 56), p, Ui.dp(this, 18));
        LinearLayout bar = Ui.row(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        position = Ui.text(this, "00:00", 14, Palette.TEXT, true);
        duration = Ui.text(this, "00:00", 14, Palette.TEXT_DIM, true);
        seek = new SeekBar(this);
        seek.setMax(1000);
        seek.setFocusable(true);
        seek.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), Ui.dp(this, 10));
        seek.setBackground(Ui.focusRing(this, Ui.dp(this, 16)));
        seek.setProgressTintList(ColorStateList.valueOf(Palette.GOLD));
        seek.setThumbTintList(ColorStateList.valueOf(Palette.GOLD));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(0x55FFFFFF));
        seek.setSecondaryProgressTintList(ColorStateList.valueOf(0x88FFFFFF));
        bar.addView(position);
        bar.addView(seek, Ui.weight(1));
        bar.addView(duration);
        bottom.addView(bar, Ui.lp(-1, -2));

        LinearLayout buttons = Ui.row(this);
        buttons.setGravity(Gravity.CENTER_VERTICAL);
        ImageView rew = roundButton(R.drawable.ic_rewind, 46, "Voltar 10 segundos");
        playPause = roundButton(R.drawable.ic_pause, 52, "Reproduzir ou pausar");
        playPause.setBackground(Ui.selectable(this, Palette.GOLD, Palette.GOLD_LIGHT, Ui.dp(this, 26), Palette.TEXT));
        playPause.setColorFilter(Palette.BLACK);
        ImageView fwd = roundButton(R.drawable.ic_forward, 46, "Avançar 10 segundos");
        buttons.addView(rew, Ui.lp(Ui.dp(this, 46), Ui.dp(this, 46)));
        LinearLayout.LayoutParams ppl = Ui.lp(Ui.dp(this, 52), Ui.dp(this, 52));
        ppl.leftMargin = Ui.dp(this, 12);
        ppl.rightMargin = Ui.dp(this, 12);
        buttons.addView(playPause, ppl);
        buttons.addView(fwd, Ui.lp(Ui.dp(this, 46), Ui.dp(this, 46)));
        TextView keys = Ui.text(this, "   ◀ ▶ na barra: 10 s", 12, Palette.TEXT_DIM, false);
        buttons.addView(keys);
        buttons.addView(new View(this), Ui.spacer());
        episodesButton = textButton("Episódios");
        buttons.addView(episodesButton);
        nextButton = textButton("Próximo episódio  ›");
        buttons.addView(nextButton);
        LinearLayout.LayoutParams bl = Ui.lp(-1, -2);
        bl.topMargin = Ui.dp(this, 10);
        bottom.addView(buttons, bl);
        overlay.addView(bottom, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        controlRow = buttons;
        rew.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { seekBy(-10000); }
        });
        fwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { seekBy(10000); }
        });
        playPause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { togglePlay(); }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { playNext(); }
        });
        episodesButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showEpisodes(); }
        });
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean fromUser) {
                if (fromUser) position.setText(time((long) video.getDuration() * v / 1000));
            }
            public void onStartTrackingTouch(SeekBar s) { seeking = true; }
            public void onStopTrackingTouch(SeekBar s) {
                seekNow((int) ((long) video.getDuration() * s.getProgress() / 1000));
                showOverlay();
            }
        });

        buildEpisodesPanel();
        buildUpNext();
    }

    // ---------------------------------------------------------------- episodes (series)

    private void buildEpisodesPanel() {
        FrameLayout root = (FrameLayout) overlay.getParent();
        episodesPanel = Ui.column(this);
        episodesPanel.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {0xF7000000, 0xE6000000, 0x00000000}));
        int p = Ui.dp(this, 28);
        episodesPanel.setPadding(p, Ui.dp(this, 70), 0, Ui.dp(this, 22));
        episodesPanel.setVisibility(View.GONE);
        episodesPanel.setClickable(true);
        LinearLayout head = Ui.row(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = Ui.text(this, "PRÓXIMOS EPISÓDIOS", 15, Palette.TEXT, true);
        title.setLetterSpacing(0.06f);
        head.addView(title);
        TextView hint = Ui.text(this, "   ▲ ou Voltar para fechar", 12, Palette.TEXT_DIM, false);
        head.addView(hint);
        episodesPanel.addView(head, Ui.lp(-1, -2));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        hs.setClipToPadding(false);
        episodesStrip = Ui.row(this);
        episodesStrip.setPadding(0, Ui.dp(this, 12), p, Ui.dp(this, 4));
        hs.addView(episodesStrip);
        episodesPanel.addView(hs, Ui.lp(-1, -2));
        root.addView(episodesPanel, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
    }

    private void showEpisodes() {
        if (nextUrls.isEmpty()) {
            Toast.makeText(this, "Este é o último episódio", Toast.LENGTH_SHORT).show();
            return;
        }
        overlay.setVisibility(View.GONE);
        episodesStrip.removeAllViews();
        for (int i = 0; i < nextUrls.size() && i < 40; i++) {
            final int which = i;
            String label = nextSubs.get(i);
            int cut = label.indexOf("  ");
            String num = cut > 0 ? label.substring(0, cut) : "";
            String name = cut > 0 ? label.substring(cut).trim() : label;
            LinearLayout card = Ui.column(this);
            card.setFocusable(true);
            card.setClickable(true);
            float r = Ui.dp(this, 12);
            card.setBackground(Ui.selectable(this, 0xE61C1C1C, 0xFF2A2516, r, Palette.GOLD));
            int cp = Ui.dp(this, 14);
            card.setPadding(cp, Ui.dp(this, 12), cp, Ui.dp(this, 12));
            TextView n = Ui.text(this, i == 0 ? "A SEGUIR  •  " + num : num, 12, Palette.GOLD, true);
            n.setSingleLine(true);
            card.addView(n);
            TextView t = Ui.text(this, name, 15, Palette.TEXT, true);
            t.setMaxLines(2);
            t.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams tl = Ui.lp(-1, -2);
            tl.topMargin = Ui.dp(this, 6);
            card.addView(t, tl);
            View sp = new View(this);
            card.addView(sp, new LinearLayout.LayoutParams(1, 0, 1f));
            ImageView play = Ui.icon(this, R.drawable.ic_play, Palette.TEXT_DIM);
            card.addView(play, Ui.lp(Ui.dp(this, 18), Ui.dp(this, 18)));
            LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(this, 230), Ui.dp(this, 118));
            lp.rightMargin = Ui.dp(this, 12);
            episodesStrip.addView(card, lp);
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { playEpisode(which); }
            });
        }
        episodesPanel.setVisibility(View.VISIBLE);
        episodesPanel.setAlpha(0f);
        episodesPanel.setTranslationY(Ui.dp(this, 40));
        episodesPanel.animate().alpha(1f).translationY(0).setDuration(180).start();
        episodesStrip.getChildAt(0).requestFocus();
    }

    private void hideEpisodes() {
        episodesPanel.setVisibility(View.GONE);
    }

    private boolean episodesShown() {
        return episodesPanel != null && episodesPanel.getVisibility() == View.VISIBLE;
    }

    /** Plays the queued episode at position i, skipping the ones before it. */
    private void playEpisode(int i) {
        hideEpisodes();
        for (int k = 0; k < i && !nextUrls.isEmpty(); k++) {
            nextUrls.remove(0);
            nextSubs.remove(0);
        }
        playNext();
    }

    private void buildUpNext() {
        FrameLayout root = (FrameLayout) overlay.getParent();
        upNext = Ui.column(this);
        upNext.setBackground(Ui.stroke(0xF0141414, Ui.dp(this, 14), Ui.dp(this, 1), 0x44FFFFFF));
        int p = Ui.dp(this, 16);
        upNext.setPadding(p, p, p, p);
        upNext.setVisibility(View.GONE);
        upNext.addView(Ui.text(this, "A SEGUIR", 11, Palette.GOLD, true));
        upNextTitle = Ui.text(this, "", 15, Palette.TEXT, true);
        upNextTitle.setMaxLines(2);
        upNextTitle.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams tl = Ui.lp(-1, -2);
        tl.topMargin = Ui.dp(this, 4);
        tl.bottomMargin = Ui.dp(this, 10);
        upNext.addView(upNextTitle, tl);
        TextView go = textButton("▶  Assistir agora");
        go.setLayoutParams(Ui.lp(-2, -2));
        upNext.addView(go);
        go.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                upNext.setVisibility(View.GONE);
                playNext();
            }
        });
        FrameLayout.LayoutParams ul = new FrameLayout.LayoutParams(Ui.dp(this, 280), -2, Gravity.BOTTOM | Gravity.END);
        ul.setMargins(0, 0, Ui.dp(this, 28), Ui.dp(this, 110));
        root.addView(upNext, ul);
    }

    /** In the last 40 s of an episode, offers the next one. */
    private void updateUpNext(int pos, int dur) {
        if (upNext == null) return;
        boolean show = !nextUrls.isEmpty() && !upNextDismissed && dur > 120000 && dur - pos < 40000 && !episodesShown();
        if (show && upNext.getVisibility() != View.VISIBLE) {
            String label = nextSubs.get(0);
            upNextTitle.setText(label.replaceFirst("\\s{2,}", "  •  "));
            upNext.setVisibility(View.VISIBLE);
            upNext.getChildAt(2).requestFocus();
        } else if (!show && upNext.getVisibility() == View.VISIBLE) {
            upNext.setVisibility(View.GONE);
        }
    }

    private ImageView roundButton(int icon, int sizeDp, String desc) {
        ImageView b = Ui.iconButton(this, icon, Palette.TEXT, Palette.GOLD);
        b.setContentDescription(desc);
        b.setBackground(Ui.selectable(this, 0x66000000, 0x99E8B84A, Ui.dp(this, sizeDp / 2f), Palette.GOLD));
        int p = Ui.dp(this, sizeDp / 4f);
        b.setPadding(p, p, p, p);
        return b;
    }

    private TextView textButton(String label) {
        TextView t = Ui.text(this, label, 14, Palette.TEXT, true);
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(Ui.selectable(this, 0x33FFFFFF, 0x66E8B84A, Ui.dp(this, 18), Palette.GOLD));
        t.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
        lp.leftMargin = Ui.dp(this, 10);
        t.setLayoutParams(lp);
        return t;
    }

    private void playNext() {
        if (nextUrls.isEmpty()) return;
        saveProgress();
        vodUrl = nextUrls.remove(0);
        vodSubtitle = nextSubs.remove(0);
        play();
    }

    private void updateVodProgress() {
        if (live) return;
        int dur = video.getDuration();
        int pos = video.getCurrentPosition();
        if (dur > 0 && video.isPlaying()) updateUpNext(pos, dur);
        if (seeking || overlay.getVisibility() != View.VISIBLE) return;
        clockView.setText(clock.format(new Date()));
        if (dur > 0) {
            seek.setProgress((int) ((long) pos * 1000 / dur));
            seek.setSecondaryProgress(video.getBufferPercentage() * 10);
            duration.setText("-" + time(Math.max(0, dur - pos)));
            endsAt.setText("Termina às " + clock.format(new Date(System.currentTimeMillis() + dur - pos)));
        }
        position.setText(time(pos));
    }

    /**
     * Repeated presses add up (shown on the bar right away) and the player seeks once they stop;
     * seeking on every press made the position jump back while the stream was still buffering.
     */
    private void seekBy(int ms) {
        int dur = video.getDuration();
        if (dur <= 0) {
            Toast.makeText(this, "Este vídeo ainda está carregando ou não permite avançar", Toast.LENGTH_SHORT).show();
            return;
        }
        int from = seekTarget >= 0 ? seekTarget : video.getCurrentPosition();
        seekTarget = Math.max(0, Math.min(dur - 2000, from + ms));
        seeking = true;
        seek.setProgress((int) ((long) seekTarget * 1000 / dur));
        position.setText(time(seekTarget) + (ms > 0 ? "  ▶▶" : "  ◀◀"));
        handler.removeCallbacks(applySeek);
        handler.postDelayed(applySeek, 700);
        showOverlay();
    }

    private void seekNow(int target) {
        seekTarget = -1;
        seeking = false;
        loading.setVisibility(View.VISIBLE);
        video.seekTo(target);
        if (!video.isPlaying()) video.start();
        updatePlayPause();
    }

    private void togglePlay() {
        if (video.isPlaying()) video.pause();
        else video.start();
        updatePlayPause();
        showOverlay();
    }

    private void updatePlayPause() {
        if (playPause != null) playPause.setImageResource(video.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
        if (pauseSign != null) pauseSign.setVisibility(!live && !video.isPlaying() && player != null ? View.VISIBLE : View.GONE);
    }

    private void resumePosition() {
        int saved = Repo.prefs().getInt("pos:" + vodUrl, 0);
        if (saved > 30000 && saved < video.getDuration() - 60000) {
            video.seekTo(saved);
            Toast.makeText(this, "Continuando de " + time(saved), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveProgress() {
        if (live || vodUrl == null) return;
        int pos = video.getCurrentPosition();
        int dur = video.getDuration();
        if (pos > 30000 && dur > 0 && pos < dur - 60000) Repo.prefs().edit().putInt("pos:" + vodUrl, pos).apply();
        if (vodKey != null && !finished && dur > 0) Repo.saveProgress(vodKey, vodUrl, vodTitle, vodSubtitle, pos, dur);
    }

    private static String time(long ms) {
        long s = ms / 1000;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
                : String.format(Locale.US, "%02d:%02d", m, sec);
    }

    // ---------------------------------------------------------------- common

    private void play() {
        message.setText("");
        subtitleText.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        player = null;
        video.stopPlayback();
        if (live) {
            Repo.current = channel;
            Repo.prefs().edit().putString("last_channel", channel.url).apply();
            liveInfo.setZapList(channels);
            liveInfo.bind(channel, Adult.channels());
            handler.removeCallbacks(startLive);
            handler.postDelayed(startLive, 350);
        } else {
            vodSubView.setText(vodSubtitle == null ? "" : vodSubtitle);
            vodSubView.setVisibility(vodSubtitle == null ? View.GONE : View.VISIBLE);
            nextButton.setVisibility(nextUrls.isEmpty() ? View.GONE : View.VISIBLE);
            episodesButton.setVisibility(nextUrls.isEmpty() ? View.GONE : View.VISIBLE);
            upNextDismissed = false;
            if (upNext != null) upNext.setVisibility(View.GONE);
            retries = 0;
            seekTarget = -1;
            video.setVideoURI(Uri.parse(vodUrl));
        }
        showOverlay();
    }

    private void zap(int delta) {
        tried.clear();
        int n = channels.size();
        int i = channels.indexOf(channel);
        index = (((i < 0 ? index : i) + delta) % n + n) % n;
        channel = channels.get(index);
        play();
    }

    private void showOverlay() {
        overlay.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideOverlay);
        handler.postDelayed(hideOverlay, OVERLAY_MS);
        updateVodProgress();
    }

    /** Keys this screen handled on ACTION_DOWN; their ACTION_UP is swallowed too. */
    private int consumedKey = -1;

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (live || overlay == null) return super.dispatchKeyEvent(e);
        int code = e.getKeyCode();
        if (e.getAction() == KeyEvent.ACTION_UP && code == consumedKey) {
            consumedKey = -1;
            return true;
        }
        if (e.getAction() == KeyEvent.ACTION_DOWN && code != KeyEvent.KEYCODE_BACK && vodKey(code)) {
            consumedKey = code;
            return true;
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean visible = overlay.getVisibility() == View.VISIBLE;
        if (live) {
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

    /** Remote keys for movies and episodes; true when handled. */
    private boolean vodKey(int keyCode) {
        boolean visible = overlay.getVisibility() == View.VISIBLE;
            if (episodesShown()) {
                if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    hideEpisodes();
                    return true;
                }
                return false;
            }
            if (upNext != null && upNext.getVisibility() == View.VISIBLE
                    && (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER)
                    && !visible) {
                upNext.setVisibility(View.GONE);
                playNext();
                return true;
            }
            switch (keyCode) {
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    togglePlay();
                    return true;
                case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                    seekBy(30000);
                    return true;
                case KeyEvent.KEYCODE_MEDIA_REWIND:
                    seekBy(-10000);
                    return true;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    playNext();
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT: {
                    // Hidden controls or focus on the bar: ◀ ▶ rewind / fast-forward 10 s.
                    // On the buttons row they move between buttons (play, episodes, next episode…).
                    View f = getCurrentFocus();
                    if (!visible || f == seek || f == null) {
                        seekBy(keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -10000 : 10000);
                        seek.requestFocus();
                        return true;
                    }
                    showOverlay();
                    break;
                }
                case KeyEvent.KEYCODE_DPAD_DOWN: {
                    View f = getCurrentFocus();
                    boolean inRow = f != null && f.getParent() == controlRow;
                    // Series: ▼ (with the controls hidden, or from the buttons row) shows the next episodes.
                    if (!nextUrls.isEmpty() && (!visible || inRow)) {
                        showEpisodes();
                        return true;
                    }
                    if (!visible) {
                        showOverlay();
                        playPause.requestFocus();
                        return true;
                    }
                    showOverlay();
                    if (f == seek) {
                        playPause.requestFocus();
                        return true;
                    }
                    break;
                }
                case KeyEvent.KEYCODE_DPAD_UP: {
                    if (!visible) {
                        showOverlay();
                        seek.requestFocus();
                        return true;
                    }
                    showOverlay();
                    View f = getCurrentFocus();
                    if (f != null && f.getParent() == controlRow) {
                        seek.requestFocus();
                        return true;
                    }
                    break;
                }
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (!visible) {
                        showOverlay();
                        playPause.requestFocus();
                        return true;
                    }
                    showOverlay();
                    if (getCurrentFocus() == seek) {
                        togglePlay();
                        return true;
                    }
                    break;
                default:
                    break;
            }
                return false;
    }

    @Override
    public void onBackPressed() {
        if (episodesShown()) {
            hideEpisodes();
            return;
        }
        if (upNext != null && upNext.getVisibility() == View.VISIBLE) {
            upNextDismissed = true;
            upNext.setVisibility(View.GONE);
            return;
        }
        if (overlay.getVisibility() == View.VISIBLE && live) overlay.setVisibility(View.GONE);
        else super.onBackPressed();
    }

    /** Tap shows the controls; a vertical swipe changes channel in live mode. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (episodesShown() || (upNext != null && upNext.getVisibility() == View.VISIBLE)) {
            return super.dispatchTouchEvent(ev);
        }
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            touchY = ev.getY();
            touchX = ev.getX();
            if (overlay.getVisibility() != View.VISIBLE) return true;
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            float dy = ev.getY() - touchY;
            float dx = ev.getX() - touchX;
            float threshold = getResources().getDisplayMetrics().heightPixels / 5f;
            if (live && Math.abs(dy) > threshold && Math.abs(dy) > Math.abs(dx)) {
                zap(dy < 0 ? 1 : -1);
                return true;
            }
            if (live && Math.abs(dx) > getResources().getDisplayMetrics().widthPixels / 6f && Math.abs(dx) > Math.abs(dy)) {
                switchQuality(dx < 0 ? 1 : -1);
                return true;
            }
            if (overlay.getVisibility() != View.VISIBLE) {
                showOverlay();
                return true;
            }
            showOverlay();
        } else if (overlay.getVisibility() != View.VISIBLE) {
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
        handler.removeCallbacks(tick);
        saveProgress();
        video.stopPlayback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!live) handler.post(tick);
        if (paused) {
            paused = false;
            play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
