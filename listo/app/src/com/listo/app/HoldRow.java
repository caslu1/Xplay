package com.listo.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import android.widget.LinearLayout;

/**
 * A row you press and hold to complete: a fill sweeps across it while the finger stays down,
 * and when it reaches the end the row reports completion. A quick tap is a normal click.
 */
public class HoldRow extends LinearLayout {

    public interface OnHoldComplete {
        void onHoldComplete(HoldRow row);
    }

    private static final long HOLD_MS = 750;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF r = new RectF();
    private final float radius;
    private final int slop;
    private int bg;
    private int fillColor;
    private float progress;
    private OnHoldComplete listener;
    private float downX, downY;
    private long downAt;
    private boolean finished;

    public HoldRow(Context c, int bg, int fillColor, float radiusPx) {
        super(c);
        this.bg = bg;
        this.fillColor = fillColor;
        this.radius = radiusPx;
        slop = ViewConfiguration.get(c).getScaledTouchSlop();
        setWillNotDraw(false);
    }

    public void setOnHoldComplete(OnHoldComplete l) {
        listener = l;
    }

    @Override
    protected void onDraw(Canvas c) {
        r.set(0, 0, getWidth(), getHeight());
        clip.reset();
        clip.addRoundRect(r, radius, radius, Path.Direction.CW);
        c.save();
        c.clipPath(clip);
        fill.setColor(bg);
        c.drawRect(r, fill);
        if (progress > 0) {
            fill.setColor(fillColor);
            c.drawRect(0, 0, getWidth() * progress, getHeight(), fill);
        }
        c.restore();
        super.onDraw(c);
    }

    // Progress runs on its own clock so holding works even with system animations turned off.
    private float from, target;
    private long animStart, animMs;
    private boolean running;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            float t = animMs <= 0 ? 1 : Math.min(1f, (android.os.SystemClock.uptimeMillis() - animStart) / (float) animMs);
            progress = from + (target - from) * t;
            invalidate();
            if (t < 1) {
                postOnAnimation(this);
                return;
            }
            running = false;
            if (target >= 1 && !finished) {
                finished = true;
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                if (listener != null) listener.onHoldComplete(HoldRow.this);
            }
        }
    };

    private void animateTo(float to, long ms) {
        from = progress;
        target = to;
        animStart = android.os.SystemClock.uptimeMillis();
        animMs = ms;
        running = true;
        removeCallbacks(tick);
        postOnAnimation(tick);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent e) {
        return true;   // the row handles the gesture, children are just content
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (finished) return true;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                downAt = System.currentTimeMillis();
                animateTo(1, (long) (HOLD_MS * (1 - progress)));
                return true;
            case MotionEvent.ACTION_MOVE:
                if (Math.abs(e.getX() - downX) > slop * 2 || Math.abs(e.getY() - downY) > slop * 2) release();
                return true;
            case MotionEvent.ACTION_UP:
                boolean tap = System.currentTimeMillis() - downAt < 220;
                release();
                if (tap) performClick();
                return true;
            case MotionEvent.ACTION_CANCEL:
                release();
                return true;
        }
        return true;
    }

    private void release() {
        if (finished) return;
        if (progress > 0) animateTo(0, 220);
    }
}
