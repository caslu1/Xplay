package com.xplay.iptv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
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
    private float touchY, touchX;
    private MediaPlayer player;

    private VideoView video;
    private ProgressBar loading;
    private TextView message, subtitleText;
    private FrameLayout overlay;

    // live overlay
    private TextView number, channelName, nowLine, nowDesc, nextLine;
    private ProgressBar nowProgress;
    private LinearLayout cards;
    private View cardsBlock;

    // vod overlay
    private TextView vodTitleView, vodSubView, position, duration;
    private SeekBar seek;
    private ImageView playPause;
    private View nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
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
        LinearLayout top = Ui.row(this);
        top.setGravity(Gravity.TOP);
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xE6000000, 0x99000000, 0x00000000}));
        int p = Ui.dp(this, 24);
        top.setPadding(p, Ui.dp(this, 16), p, Ui.dp(this, 48));
        LinearLayout left = Ui.column(this);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        number = Ui.text(this, "", 60, Palette.TEXT, true);
        number.setIncludeFontPadding(false);
        left.addView(number);
        channelName = Ui.text(this, "", 19, Palette.TEXT, true);
        channelName.setGravity(Gravity.CENTER);
        channelName.setMaxLines(2);
        left.addView(channelName);
        top.addView(left, Ui.lp(Ui.dp(this, 280), -2));

        LinearLayout right = Ui.column(this);
        right.setPadding(Ui.dp(this, 24), Ui.dp(this, 8), 0, 0);
        nowLine = Ui.text(this, "", 20, Palette.TEXT, true);
        nowLine.setSingleLine(true);
        nowLine.setEllipsize(TextUtils.TruncateAt.END);
        right.addView(nowLine);
        nowDesc = Ui.text(this, "", 14, 0xFFCCCCCC, false);
        nowDesc.setMaxLines(2);
        nowDesc.setEllipsize(TextUtils.TruncateAt.END);
        right.addView(nowDesc);
        nowProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        nowProgress.setMax(1000);
        nowProgress.setProgressTintList(ColorStateList.valueOf(Palette.GOLD));
        nowProgress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFFDDDDDD));
        LinearLayout.LayoutParams pp = Ui.lp(-1, Ui.dp(this, 6));
        pp.topMargin = Ui.dp(this, 8);
        right.addView(nowProgress, pp);
        nextLine = Ui.text(this, "", 18, 0xFFBBBBBB, false);
        nextLine.setSingleLine(true);
        nextLine.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np = Ui.lp(-1, -2);
        np.topMargin = Ui.dp(this, 8);
        right.addView(nextLine, np);
        top.addView(right, Ui.weight(1));
        overlay.addView(top, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        LinearLayout bottom = Ui.column(this);
        cardsBlock = bottom;
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {0xE6000000, 0x99000000, 0x00000000}));
        bottom.setPadding(p, Ui.dp(this, 40), 0, Ui.dp(this, 16));
        TextView hint = Ui.text(this, "outras qualidades deste canal", 14, Palette.TEXT_DIM, false);
        hint.setPadding(Ui.dp(this, 12), 0, 0, Ui.dp(this, 10));
        bottom.addView(hint);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        cards = Ui.row(this);
        cards.setPadding(0, Ui.dp(this, 4), Ui.dp(this, 24), Ui.dp(this, 4));
        hs.addView(cards);
        bottom.addView(hs, Ui.lp(-1, -2));
        overlay.addView(bottom, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
    }

    /** Cards for the other qualities of the current channel (Globo SP SD / HD / FHD / 4K). */
    private void buildVariants() {
        cards.removeAllViews();
        List<Channel> variants = Variants.of(channel, Adult.channels());
        cardsBlock.setVisibility(variants.size() > 1 ? View.VISIBLE : View.GONE);
        if (variants.size() <= 1) return;
        for (final Channel v : variants) {
            boolean current = v == channel;
            LinearLayout card = Ui.column(this);
            card.setGravity(Gravity.CENTER);
            card.setFocusable(true);
            card.setClickable(true);
            float r = Ui.dp(this, 10);
            card.setBackground(current
                    ? Ui.stroke(0xCC141414, r, Ui.dp(this, 3), Palette.GOLD)
                    : Ui.selectable(this, 0xB3141414, 0xE6222222, r, Palette.GOLD));
            int pad = Ui.dp(this, 10);
            card.setPadding(pad, pad, pad, pad);
            ImageView logo = new ImageView(this);
            logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ImageLoader.load(v.logo, logo, Ui.dp(this, 60), R.drawable.ic_launcher);
            card.addView(logo, Ui.lp(Ui.dp(this, 60), Ui.dp(this, 46)));
            TextView name = Ui.text(this, v.name.toUpperCase(Locale.getDefault()), 13, Palette.TEXT, true);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            name.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nl = Ui.lp(-1, -2);
            nl.topMargin = Ui.dp(this, 6);
            card.addView(name, nl);
            LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(this, 170), Ui.dp(this, 124));
            lp.leftMargin = Ui.dp(this, 12);
            cards.addView(card, lp);
            if (current) card.setTag("current");
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (v == channel) return;
                    channel = v;
                    play();
                }
            });
            card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                public void onFocusChange(View view, boolean has) { if (has) showOverlay(); }
            });
        }
    }

    private void loadEpg(final Channel c) {
        nowLine.setText(c.name);
        nowDesc.setText("");
        nextLine.setText("");
        nowProgress.setVisibility(View.INVISIBLE);
        Epg.get(c, new Epg.Callback() {
            public void done(List<Xtream.Program> list) {
                if (isFinishing() || channel != c || list.isEmpty()) return;
                Xtream.Program now = list.get(0);
                nowLine.setText(range(now) + "   " + now.title);
                nowDesc.setText(now.description);
                nowProgress.setProgress(Epg.progress(now));
                nowProgress.setVisibility(View.VISIBLE);
                if (list.size() > 1) {
                    Xtream.Program next = list.get(1);
                    nextLine.setText(range(next) + "   " + next.title);
                }
            }
        });
    }

    private String range(Xtream.Program p) {
        return clock.format(new Date(p.start)) + " às " + clock.format(new Date(p.end));
    }

    // ---------------------------------------------------------------- vod overlay

    private void buildVodOverlay() {
        LinearLayout top = Ui.row(this);
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xD9000000, 0x00000000}));
        int p = Ui.dp(this, 24);
        top.setPadding(Ui.dp(this, 16), Ui.dp(this, 16), p, Ui.dp(this, 48));
        ImageView back = Ui.iconButton(this, R.drawable.ic_back, Palette.TEXT, Palette.GOLD);
        back.setContentDescription("Voltar");
        top.addView(back, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        LinearLayout titles = Ui.column(this);
        titles.setPadding(Ui.dp(this, 12), 0, 0, 0);
        vodTitleView = Ui.text(this, vodTitle, 22, Palette.TEXT, true);
        vodTitleView.setSingleLine(true);
        vodTitleView.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(vodTitleView);
        vodSubView = Ui.text(this, "", 15, Palette.GOLD, false);
        vodSubView.setSingleLine(true);
        vodSubView.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(vodSubView);
        top.addView(titles, Ui.weight(1));
        overlay.addView(top, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        LinearLayout center = Ui.row(this);
        ImageView rew = roundButton(R.drawable.ic_rewind, 60, "Voltar 10 segundos");
        playPause = roundButton(R.drawable.ic_pause, 84, "Reproduzir ou pausar");
        playPause.setBackground(Ui.selectable(this, 0x99000000, 0xCCE8B84A, Ui.dp(this, 42), Palette.GOLD));
        ImageView fwd = roundButton(R.drawable.ic_forward, 60, "Avançar 30 segundos");
        center.addView(rew, Ui.lp(Ui.dp(this, 60), Ui.dp(this, 60)));
        LinearLayout.LayoutParams pl = Ui.lp(Ui.dp(this, 84), Ui.dp(this, 84));
        pl.leftMargin = Ui.dp(this, 44);
        pl.rightMargin = Ui.dp(this, 44);
        center.addView(playPause, pl);
        center.addView(fwd, Ui.lp(Ui.dp(this, 60), Ui.dp(this, 60)));
        overlay.addView(center, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        rew.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { seekBy(-10000); }
        });
        fwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { seekBy(30000); }
        });
        playPause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { togglePlay(); }
        });

        LinearLayout bottom = Ui.column(this);
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {0xD9000000, 0x00000000}));
        bottom.setPadding(p, Ui.dp(this, 48), p, Ui.dp(this, 18));
        LinearLayout bar = Ui.row(this);
        position = Ui.text(this, "00:00", 14, Palette.TEXT, true);
        duration = Ui.text(this, "00:00", 14, Palette.TEXT_DIM, true);
        seek = new SeekBar(this);
        seek.setMax(1000);
        seek.setProgressTintList(ColorStateList.valueOf(Palette.GOLD));
        seek.setThumbTintList(ColorStateList.valueOf(Palette.GOLD));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(0x66FFFFFF));
        bar.addView(position);
        bar.addView(seek, Ui.weight(1));
        bar.addView(duration);
        bottom.addView(bar, Ui.lp(-1, -2));

        LinearLayout buttons = Ui.row(this);
        buttons.addView(new View(this), Ui.spacer());
        View audio = textButton("Áudio");
        View subs = textButton("Legendas");
        buttons.addView(audio);
        buttons.addView(subs);
        nextButton = textButton("Próximo episódio ›");
        buttons.addView(nextButton);
        LinearLayout.LayoutParams bl = Ui.lp(-1, -2);
        bl.topMargin = Ui.dp(this, 8);
        bottom.addView(buttons, bl);
        overlay.addView(bottom, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        audio.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { chooseTrack(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO); }
        });
        subs.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { chooseTrack(MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT); }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { playNext(); }
        });
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean fromUser) {
                if (fromUser) position.setText(time((long) video.getDuration() * v / 1000));
            }
            public void onStartTrackingTouch(SeekBar s) { seeking = true; }
            public void onStopTrackingTouch(SeekBar s) {
                seeking = false;
                video.seekTo((int) ((long) video.getDuration() * s.getProgress() / 1000));
                showOverlay();
            }
        });
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

    /** Lists the audio or subtitle tracks the stream offers and switches to the chosen one. */
    private void chooseTrack(final int type) {
        if (player == null) return;
        final MediaPlayer.TrackInfo[] tracks;
        try {
            tracks = player.getTrackInfo();
        } catch (Exception e) {
            Toast.makeText(this, "Faixas indisponíveis", Toast.LENGTH_SHORT).show();
            return;
        }
        final List<Integer> ids = new ArrayList<Integer>();
        List<String> labels = new ArrayList<String>();
        boolean subs = type == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT;
        if (subs) {
            ids.add(-1);
            labels.add("Desligadas");
        }
        for (int i = 0; i < tracks.length; i++) {
            int t = tracks[i].getTrackType();
            boolean match = t == type || (subs && t == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_SUBTITLE);
            if (!match) continue;
            String lang = tracks[i].getLanguage();
            String name = lang == null || lang.equals("und") || lang.isEmpty()
                    ? (subs ? "Legenda " : "Áudio ") + (ids.size() + (subs ? 0 : 1))
                    : new Locale(lang).getDisplayLanguage(new Locale("pt", "BR"));
            ids.add(i);
            labels.add(name);
        }
        if (ids.size() <= (subs ? 1 : 0)) {
            Toast.makeText(this, subs ? "Este conteúdo não tem legendas" : "Este conteúdo tem só um áudio",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(subs ? "Legendas" : "Áudio")
                .setItems(labels.toArray(new String[0]), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        int id = ids.get(which);
                        try {
                            if (id < 0) {
                                for (int i = 0; i < tracks.length; i++) {
                                    if (tracks[i].getTrackType() == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
                                        player.deselectTrack(i);
                                    }
                                }
                                subtitleText.setVisibility(View.GONE);
                            } else {
                                player.selectTrack(id);
                            }
                        } catch (Exception e) {
                            Toast.makeText(PlayerActivity.this, "Não foi possível trocar a faixa", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .show();
    }

    private void playNext() {
        if (nextUrls.isEmpty()) return;
        saveProgress();
        vodUrl = nextUrls.remove(0);
        vodSubtitle = nextSubs.remove(0);
        play();
    }

    private void updateVodProgress() {
        if (live || seeking || overlay.getVisibility() != View.VISIBLE) return;
        int dur = video.getDuration();
        int pos = video.getCurrentPosition();
        if (dur > 0) {
            seek.setProgress((int) ((long) pos * 1000 / dur));
            duration.setText(time(dur));
        }
        position.setText(time(pos));
    }

    private void seekBy(int ms) {
        int dur = video.getDuration();
        if (dur <= 0) return;
        video.seekTo(Math.max(0, Math.min(dur - 1000, video.getCurrentPosition() + ms)));
        showOverlay();
    }

    private void togglePlay() {
        if (video.isPlaying()) video.pause();
        else video.start();
        updatePlayPause();
        showOverlay();
    }

    private void updatePlayPause() {
        if (playPause != null) playPause.setImageResource(video.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play);
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
            video.setVideoURI(Uri.parse(channel.url));
            number.setText(Ui.pad3(Adult.number(channel)));
            channelName.setText(channel.name.toUpperCase(Locale.getDefault()));
            loadEpg(channel);
            buildVariants();
        } else {
            vodSubView.setText(vodSubtitle == null ? "" : vodSubtitle);
            vodSubView.setVisibility(vodSubtitle == null ? View.GONE : View.VISIBLE);
            nextButton.setVisibility(nextUrls.isEmpty() ? View.GONE : View.VISIBLE);
            video.setVideoURI(Uri.parse(vodUrl));
        }
        showOverlay();
    }

    private void zap(int delta) {
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
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (!visible) {
                        showOverlay();
                        View current = cards.findViewWithTag("current");
                        if (current != null) current.requestFocus();
                        return true;
                    }
                    showOverlay();
                    break;
                default:
                    break;
            }
        } else {
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
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (!visible) {
                        // With the controls hidden, left/right seek like on most TV players.
                        seekBy(keyCode == KeyEvent.KEYCODE_DPAD_LEFT ? -10000 : 10000);
                        return true;
                    }
                    showOverlay();
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                case KeyEvent.KEYCODE_DPAD_DOWN:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (!visible) {
                        showOverlay();
                        playPause.requestFocus();
                        return true;
                    }
                    showOverlay();
                    break;
                default:
                    break;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (overlay.getVisibility() == View.VISIBLE && live) overlay.setVisibility(View.GONE);
        else super.onBackPressed();
    }

    /** Tap shows the controls; a vertical swipe changes channel in live mode. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
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
