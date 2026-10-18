package com.xplay.iptv;

import android.app.Activity;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.List;

public class PlayerActivity extends Activity {

    static final String EXTRA_INDEX = "index";

    private final Handler handler = new Handler();
    private final Runnable hideInfo = new Runnable() {
        public void run() { info.setVisibility(View.GONE); }
    };

    private List<Channel> channels;
    private int index;
    private VideoView video;
    private ProgressBar loading;
    private TextView info;
    private float touchStartY;
    private boolean paused;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        channels = MainActivity.visible;
        index = getIntent().getIntExtra(EXTRA_INDEX, 0);
        if (channels.isEmpty()) {
            finish();
            return;
        }

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        video = new VideoView(this);
        root.addView(video, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));

        loading = new ProgressBar(this);
        root.addView(loading, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        info = new TextView(this);
        info.setTextColor(Color.WHITE);
        info.setTextSize(20);
        info.setBackgroundColor(0xAA000000);
        info.setPadding(32, 16, 32, 16);
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
        ip.setMargins(32, 32, 32, 32);
        root.addView(info, ip);

        setContentView(root);

        MediaController controller = new MediaController(this);
        controller.setAnchorView(video);
        video.setMediaController(controller);
        video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            public void onPrepared(MediaPlayer mp) {
                loading.setVisibility(View.GONE);
                video.start();
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
                showInfo(channels.get(index).name + "\nCanal indisponível (erro " + what + "/" + extra + ")", 6000);
                return true;
            }
        });
        video.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mp) { play(); }
        });

        play();
    }

    private void play() {
        Channel c = channels.get(index);
        loading.setVisibility(View.VISIBLE);
        video.stopPlayback();
        video.setVideoURI(Uri.parse(c.url));
        showInfo((index + 1) + "  " + c.name, 3000);
    }

    private void zap(int delta) {
        int n = channels.size();
        index = ((index + delta) % n + n) % n;
        play();
    }

    private void showInfo(String text, long ms) {
        info.setText(text);
        info.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideInfo);
        handler.postDelayed(hideInfo, ms);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
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
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                showInfo((index + 1) + "  " + channels.get(index).name, 3000);
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    /** Vertical swipe changes channel; a tap falls through to the media controller. */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            touchStartY = ev.getY();
        } else if (ev.getAction() == MotionEvent.ACTION_UP) {
            float dy = ev.getY() - touchStartY;
            float threshold = getResources().getDisplayMetrics().heightPixels / 5f;
            if (Math.abs(dy) > threshold) {
                zap(dy < 0 ? 1 : -1);
                return true;
            }
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
        video.stopPlayback();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            paused = false;
            play();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(hideInfo);
    }
}
