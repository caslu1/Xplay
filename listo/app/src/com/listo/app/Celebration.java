package com.listo.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import java.util.Random;

/**
 * Full-screen "Missão concluída!" overlay: a green badge pops in, the check mark draws itself,
 * a ring pulses out and confetti bursts and falls. Removes itself after a couple of seconds or on tap.
 */
public class Celebration extends View {

    private static final long DURATION = 2600;
    private static final int[] CONFETTI = {0xFF7B9EEA, 0xFFF2A33A, 0xFFE5533D, 0xFF6FD3CE, 0xFFF2A2BC, 0xFFF3C766, 0xFF7FCB8E};

    private final String title;
    private final String subtitle;
    private final float dp;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();
    private long start;
    private boolean gone;
    private float ms;

    private final int n = 70;
    private final float[] px = new float[n], py = new float[n], vx = new float[n], vy = new float[n];
    private final float[] rot = new float[n], spin = new float[n], size = new float[n];
    private final int[] col = new int[n];
    private final boolean[] round = new boolean[n];

    public static void show(ViewGroup root, String title, String subtitle) {
        Celebration c = new Celebration(root.getContext(), title, subtitle);
        root.addView(c, new ViewGroup.LayoutParams(-1, -1));
        c.start = android.os.SystemClock.uptimeMillis();
        c.invalidate();
    }

    private Celebration(Context ctx, String title, String subtitle) {
        super(ctx);
        this.title = title;
        this.subtitle = subtitle;
        dp = ctx.getResources().getDisplayMetrics().density;
        setElevation(30 * dp);
        Random rnd = new Random();
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            float speed = (260 + rnd.nextFloat() * 520) * dp;   // px per second
            vx[i] = (float) Math.cos(a) * speed;
            vy[i] = (float) Math.sin(a) * speed - 260 * dp;
            rot[i] = rnd.nextFloat() * 360;
            spin[i] = (rnd.nextFloat() - 0.5f) * 900;
            size[i] = (5 + rnd.nextFloat() * 6) * dp;
            col[i] = CONFETTI[rnd.nextInt(CONFETTI.length)];
            round[i] = rnd.nextInt(3) == 0;
        }
    }

    private void dismiss() {
        if (gone) return;
        gone = true;
        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) parent.removeView(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_UP && ms > 500) dismiss();
        return true;
    }

    private static float clamp(float v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    /** Overshoot ease, 0..1 -> 0..1 with a bounce past 1. */
    private static float pop(float t) {
        t = clamp(t);
        float s = 2.2f;
        t -= 1;
        return t * t * ((s + 1) * t + s) + 1;
    }

    @Override
    protected void onDraw(Canvas c) {
        // own clock, so it still plays when the system animation scale is off
        ms = android.os.SystemClock.uptimeMillis() - start;
        if (ms >= DURATION) {
            post(new Runnable() { @Override public void run() { dismiss(); } });
            return;
        }
        postInvalidateOnAnimation();
        float w = getWidth(), h = getHeight();
        float cx = w / 2, cy = h * 0.42f;

        // fade the whole overlay out at the end
        float out = clamp((DURATION - ms) / 400f);

        // dim background
        p.setStyle(Paint.Style.FILL);
        p.setColor(0x000000 | ((int) (150 * clamp(ms / 200f) * out) << 24));
        c.drawRect(0, 0, w, h, p);

        // pulse rings
        for (int k = 0; k < 2; k++) {
            float t = clamp((ms - 150 - k * 180) / 700f);
            if (t > 0 && t < 1) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(6 * dp * (1 - t));
                p.setColor(0x4CAF50 | ((int) (200 * (1 - t) * out) << 24));
                c.drawCircle(cx, cy, 52 * dp + t * 110 * dp, p);
            }
        }

        // badge
        float s = pop(ms / 450f);
        float rad = 52 * dp * s;
        p.setStyle(Paint.Style.FILL);
        p.setColor((int) (0x33 * out) << 24);
        c.drawCircle(cx, cy + 6 * dp, rad, p);
        p.setColor(0x43A047 | ((int) (255 * out) << 24));
        c.drawCircle(cx, cy, rad, p);
        p.setColor(0x66BB6A | ((int) (255 * out) << 24));
        c.drawCircle(cx, cy - rad * 0.08f, rad * 0.86f, p);

        // check mark drawing itself
        float ct = clamp((ms - 280) / 350f);
        if (ct > 0) {
            float x1 = cx - 22 * dp, y1 = cy + 1 * dp;
            float x2 = cx - 6 * dp, y2 = cy + 17 * dp;
            float x3 = cx + 25 * dp, y3 = cy - 16 * dp;
            float l1 = (float) Math.hypot(x2 - x1, y2 - y1), l2 = (float) Math.hypot(x3 - x2, y3 - y2);
            float d = ct * (l1 + l2);
            path.reset();
            path.moveTo(x1, y1);
            if (d <= l1) {
                path.lineTo(x1 + (x2 - x1) * d / l1, y1 + (y2 - y1) * d / l1);
            } else {
                path.lineTo(x2, y2);
                float e = (d - l1) / l2;
                path.lineTo(x2 + (x3 - x2) * e, y2 + (y3 - y2) * e);
            }
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(9 * dp);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
            p.setColor(0xFFFFFF | ((int) (255 * out) << 24));
            c.drawPath(path, p);
        }

        // confetti
        float ct2 = Math.max(0, ms - 250) / 1000f;   // seconds since burst
        if (ms > 250) {
            float g = 900 * dp;
            for (int i = 0; i < n; i++) {
                float drag = (float) Math.exp(-1.6 * ct2);
                float x = cx + vx[i] * (1 - drag) / 1.6f;
                float y = cy + vy[i] * (1 - drag) / 1.6f + 0.5f * g * ct2 * ct2 * 0.35f;
                p.setStyle(Paint.Style.FILL);
                p.setColor((col[i] & 0xFFFFFF) | ((int) (255 * out) << 24));
                c.save();
                c.translate(x, y);
                c.rotate(rot[i] + spin[i] * ct2);
                if (round[i]) {
                    c.drawCircle(0, 0, size[i] * 0.5f, p);
                } else {
                    float flip = (float) Math.abs(Math.cos(ct2 * 6 + i));
                    r.set(-size[i] / 2, -size[i] * 0.3f * flip, size[i] / 2, size[i] * 0.3f * flip);
                    c.drawRect(r, p);
                }
                c.restore();
            }
        }

        // texts
        float tt = pop((ms - 420) / 450f);
        if (ms > 420) {
            p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
            p.setTextSize(30 * dp * Math.max(0.01f, tt));
            p.setColor(0xFFFFFF | ((int) (255 * out * clamp(tt)) << 24));
            p.setShadowLayer(6 * dp, 0, 2 * dp, 0x66000000);
            c.drawText("MISSÃO CONCLUÍDA!", cx, cy + 110 * dp, p);
            p.setTypeface(Typeface.DEFAULT);
            p.setTextSize(17 * dp);
            float a2 = clamp((ms - 650) / 300f) * out;
            p.setColor(0xFFFFFF | ((int) (255 * a2) << 24));
            c.drawText(ellipsize(title, w - 48 * dp), cx, cy + 142 * dp, p);
            p.setTextSize(14 * dp);
            p.setColor((0xFFE082) | ((int) (255 * a2) << 24));
            c.drawText(subtitle, cx, cy + 168 * dp, p);
            p.clearShadowLayer();
        }
    }

    private String ellipsize(String s, float max) {
        if (p.measureText(s) <= max) return s;
        while (s.length() > 1 && p.measureText(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
