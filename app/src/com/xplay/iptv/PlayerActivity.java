package com.xplay.iptv;

import android.app.Activity;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/**
 * Fullscreen player. Live mode zaps through Repo.playlist and shows the channel/EPG overlay;
 * VOD mode (EXTRA_URL set) shows seek controls and remembers the position.
 */
public class PlayerActivity extends Activity {

    static final String EXTRA_INDEX = "index";
    static final String EXTRA_URL = "url";
    static final String EXTRA_TITLE = "title";
    static final String EXTRA_SUBTITLE = "subtitle";

    private static final long OVERLAY_MS = 6000;
    private static final int CARD_WINDOW = 40;

    private final Handler handler = new Handler();
    private final Runnable hideOverlay = new Runnable() {
        public void run() { overlay.setVisibility(View.GONE); }
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
    private String vodUrl;
    private boolean paused;
    private float touchY, touchX;

    private VideoView video;
    private ProgressBar loading;
    private TextView message;
    private FrameLayout overlay;

    // live overlay
    private TextView number, channelName, nowLine, nowDesc, nextLine;
    private ProgressBar nowProgress;
    private LinearLayout cards;
    private HorizontalScrollView cardScroll;

    // vod overlay
    private SeekBar seek;
    private TextView position, duration;
    private ImageView playPause;
    private boolean seeking;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        vodUrl = getIntent().getStringExtra(EXTRA_URL);
        live = vodUrl == null;
        channels = Repo.playlist;
        index = getIntent().getIntExtra(EXTRA_INDEX, 0);
        if (live && (channels == null || channels.isEmpty())) {
            finish();
            return;
        }
        index = Math.max(0, Math.min(index, channels == null ? 0 : channels.size() - 1));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Palette.BLACK);
        video = new VideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        loading = new ProgressBar(this);
        loading.setIndeterminateTintList(ColorStateList.valueOf(live ? Palette.GOLD : Palette.GOLD));
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
                loading.setVisibility(View.GONE);
                if (!live) resumePosition();
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
                if (live) play();
                else {
                    Repo.prefs().edit().remove("pos:" + vodUrl).apply();
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
        number = Ui.text(this, "", 64, Palette.TEXT, true);
        number.setIncludeFontPadding(false);
        left.addView(number);
        channelName = Ui.text(this, "", 20, Palette.TEXT, true);
        channelName.setGravity(Gravity.CENTER);
        channelName.setMaxLines(2);
        left.addView(channelName);
        top.addView(left, Ui.lp(Ui.dp(this, 300), -2));

        LinearLayout right = Ui.column(this);
        right.setPadding(Ui.dp(this, 24), Ui.dp(this, 8), 0, 0);
        nowLine = Ui.text(this, "", 20, Palette.TEXT, true);
        nowLine.setSingleLine(true);
        nowLine.setEllipsize(TextUtils.TruncateAt.END);
        right.addView(nowLine);
        nowDesc = Ui.text(this, "", 14, 0xFFCCCCCC, false);
        nowDesc.setMaxLines(2);
        nowDesc.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams dp = Ui.lp(-1, -2);
        dp.topMargin = Ui.dp(this, 6);
        right.addView(nowDesc, dp);
        nowProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        nowProgress.setMax(1000);
        nowProgress.setProgressTintList(ColorStateList.valueOf(Palette.GOLD));
        nowProgress.setProgressBackgroundTintList(ColorStateList.valueOf(0xFFDDDDDD));
        LinearLayout.LayoutParams pp = Ui.lp(-1, Ui.dp(this, 6));
        pp.topMargin = Ui.dp(this, 8);
        right.addView(nowProgress, pp);
        nextLine = Ui.text(this, "", 20, 0xFFBBBBBB, false);
        nextLine.setSingleLine(true);
        nextLine.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams np = Ui.lp(-1, -2);
        np.topMargin = Ui.dp(this, 10);
        right.addView(nextLine, np);
        top.addView(right, Ui.weight(1));
        overlay.addView(top, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        LinearLayout bottom = Ui.column(this);
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {0xE6000000, 0x99000000, 0x00000000}));
        bottom.setPadding(p, Ui.dp(this, 40), 0, Ui.dp(this, 16));
        TextView hint = Ui.text(this, "outros canais  ‹ ›", 14, Palette.TEXT_DIM, false);
        hint.setPadding(Ui.dp(this, 12), 0, 0, Ui.dp(this, 10));
        bottom.addView(hint);
        cardScroll = new HorizontalScrollView(this);
        cardScroll.setHorizontalScrollBarEnabled(false);
        cards = Ui.row(this);
        cards.setPadding(0, Ui.dp(this, 4), Ui.dp(this, 24), Ui.dp(this, 4));
        cardScroll.addView(cards);
        bottom.addView(cardScroll, Ui.lp(-1, -2));
        overlay.addView(bottom, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
    }

    private void buildCards() {
        cards.removeAllViews();
        int from = Math.max(0, index - CARD_WINDOW / 2);
        int to = Math.min(channels.size(), from + CARD_WINDOW);
        View currentCard = null;
        for (int i = from; i < to; i++) {
            final int target = i;
            Channel c = channels.get(i);
            boolean current = i == index;
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
            ImageLoader.load(c.logo, logo, Ui.dp(this, 64), R.drawable.ic_launcher);
            card.addView(logo, Ui.lp(Ui.dp(this, 64), Ui.dp(this, 52)));
            TextView name = Ui.text(this, c.name.toUpperCase(Locale.getDefault()), 13, Palette.TEXT, true);
            name.setGravity(Gravity.CENTER);
            name.setMaxLines(2);
            name.setEllipsize(TextUtils.TruncateAt.END);
            LinearLayout.LayoutParams nl = Ui.lp(-1, -2);
            nl.topMargin = Ui.dp(this, 8);
            card.addView(name, nl);
            LinearLayout.LayoutParams lp = Ui.lp(Ui.dp(this, 170), Ui.dp(this, 130));
            lp.leftMargin = Ui.dp(this, 12);
            cards.addView(card, lp);
            card.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    index = target;
                    play();
                }
            });
            card.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                public void onFocusChange(View v, boolean has) { if (has) showOverlay(); }
            });
            if (current) currentCard = card;
        }
        if (currentCard != null) {
            final View cc = currentCard;
            cardScroll.post(new Runnable() {
                public void run() { cardScroll.smoothScrollTo(Math.max(0, cc.getLeft() - Ui.dp(PlayerActivity.this, 200)), 0); }
            });
        }
    }

    private void loadEpg(final Channel c) {
        nowLine.setText(c.name);
        nowDesc.setText("");
        nextLine.setText("");
        nowProgress.setVisibility(View.INVISIBLE);
        if (c.streamId == null) return;
        Task.run(this, new Callable<List<Xtream.Program>>() {
            public List<Xtream.Program> call() throws Exception { return Xtream.shortEpg(c.streamId); }
        }, new Task.Done<List<Xtream.Program>>() {
            public void ok(List<Xtream.Program> list) {
                if (channels.get(index) != c || list.isEmpty()) return;
                Xtream.Program now = list.get(0);
                nowLine.setText(range(now) + "   " + now.title);
                nowDesc.setText(now.description);
                long span = Math.max(1, now.end - now.start);
                long done = System.currentTimeMillis() - now.start;
                nowProgress.setProgress((int) Math.max(0, Math.min(1000, done * 1000 / span)));
                nowProgress.setVisibility(View.VISIBLE);
                if (list.size() > 1) {
                    Xtream.Program next = list.get(1);
                    nextLine.setText(range(next) + "   " + next.title);
                }
            }

            public void fail(Exception e) {}
        });
    }

    private String range(Xtream.Program p) {
        return clock.format(new Date(p.start)) + " às " + clock.format(new Date(p.end));
    }

    // ---------------------------------------------------------------- vod overlay

    private void buildVodOverlay() {
        LinearLayout top = Ui.row(this);
        top.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xCC000000, 0x00000000}));
        int p = Ui.dp(this, 20);
        top.setPadding(p, p, p, Ui.dp(this, 40));
        ImageView back = Ui.iconButton(this, R.drawable.ic_back, Palette.TEXT, Palette.GOLD);
        back.setContentDescription("Voltar");
        top.addView(back, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        LinearLayout titles = Ui.column(this);
        titles.setPadding(Ui.dp(this, 12), 0, 0, 0);
        TextView title = Ui.text(this, getIntent().getStringExtra(EXTRA_TITLE), 20, Palette.TEXT, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        titles.addView(title);
        String sub = getIntent().getStringExtra(EXTRA_SUBTITLE);
        if (sub != null) titles.addView(Ui.text(this, sub, 14, 0xFFCCCCCC, false));
        top.addView(titles, Ui.weight(1));
        overlay.addView(top, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        LinearLayout center = Ui.row(this);
        ImageView rew = Ui.iconButton(this, R.drawable.ic_rewind, Palette.TEXT, Palette.GOLD);
        playPause = Ui.iconButton(this, R.drawable.ic_pause, Palette.TEXT, Palette.GOLD);
        playPause.setBackground(Ui.selectable(this, 0x99000000, 0xCCE8B84A, Ui.dp(this, 40), Palette.TEXT));
        ImageView fwd = Ui.iconButton(this, R.drawable.ic_forward, Palette.TEXT, Palette.GOLD);
        rew.setContentDescription("Voltar 10 segundos");
        playPause.setContentDescription("Reproduzir ou pausar");
        fwd.setContentDescription("Avançar 30 segundos");
        center.addView(rew, Ui.lp(Ui.dp(this, 60), Ui.dp(this, 60)));
        LinearLayout.LayoutParams pl = Ui.lp(Ui.dp(this, 80), Ui.dp(this, 80));
        pl.leftMargin = Ui.dp(this, 40);
        pl.rightMargin = Ui.dp(this, 40);
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

        LinearLayout bottom = Ui.row(this);
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {0xCC000000, 0x00000000}));
        bottom.setPadding(p, Ui.dp(this, 40), p, p);
        position = Ui.text(this, "00:00", 14, Palette.TEXT, false);
        duration = Ui.text(this, "00:00", 14, Palette.TEXT, false);
        seek = new SeekBar(this);
        seek.setMax(1000);
        seek.setProgressTintList(ColorStateList.valueOf(Palette.GOLD));
        seek.setThumbTintList(ColorStateList.valueOf(Palette.GOLD));
        bottom.addView(position);
        bottom.addView(seek, Ui.weight(1));
        bottom.addView(duration);
        overlay.addView(bottom, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean fromUser) {
                if (fromUser) position.setText(time((long) video.getDuration() * v / 1000));
            }
            public void onStartTrackingTouch(SeekBar s) { seeking = true; handler.removeCallbacks(hideOverlay); }
            public void onStopTrackingTouch(SeekBar s) {
                seeking = false;
                video.seekTo((int) ((long) video.getDuration() * s.getProgress() / 1000));
                showOverlay();
            }
        });
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

    private static String time(long ms) {
        long s = ms / 1000;
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        return h > 0 ? String.format(Locale.US, "%d:%02d:%02d", h, m, sec)
                : String.format(Locale.US, "%02d:%02d", m, sec);
    }

    // ---------------------------------------------------------------- common

    private void play() {
        message.setText("");
        loading.setVisibility(View.VISIBLE);
        video.stopPlayback();
        if (live) {
            Channel c = channels.get(index);
            Repo.current = c;
            Repo.prefs().edit().putString("last_channel", c.url).apply();
            video.setVideoURI(Uri.parse(c.url));
            number.setText(Ui.pad3(c.num > 0 ? c.num : index + 1));
            channelName.setText(c.name.toUpperCase(Locale.getDefault()));
            loadEpg(c);
            buildCards();
        } else {
            video.setVideoURI(Uri.parse(vodUrl));
        }
        showOverlay();
    }

    private void zap(int delta) {
        int n = channels.size();
        index = ((index + delta) % n + n) % n;
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
                        focusCurrentCard();
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
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
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

    private void focusCurrentCard() {
        int i = index - Math.max(0, index - CARD_WINDOW / 2);
        if (i >= 0 && i < cards.getChildCount()) cards.getChildAt(i).requestFocus();
    }

    @Override
    public void onBackPressed() {
        if (overlay.getVisibility() == View.VISIBLE && live) overlay.setVisibility(View.GONE);
        else super.onBackPressed();
    }

    /** Tap shows the overlay; a vertical swipe changes channel in live mode. */
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
        if (!live) {
            int pos = video.getCurrentPosition();
            int dur = video.getDuration();
            if (pos > 30000 && dur > 0 && pos < dur - 60000) Repo.prefs().edit().putInt("pos:" + vodUrl, pos).apply();
        }
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
