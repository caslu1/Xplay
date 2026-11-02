package com.ilha.notch;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.view.View;

/** Small hand-drawn views used on the island. */
final class Widgets {
    private Widgets() {}

    static GradientDrawable round(int color, float radius) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    /** Animated level bars shown while music plays. */
    static class Bars extends View {
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean playing;
        private final float[] phase = {0f, 1.7f, 0.6f, 2.4f};

        Bars(Context c, int color) {
            super(c);
            p.setColor(color);
        }

        void setPlaying(boolean b) { playing = b; invalidate(); }
        void setColor(int c) { p.setColor(c); invalidate(); }

        @Override protected void onDraw(Canvas c) {
            int n = 4;
            float w = getWidth(), h = getHeight();
            float bw = w / (n * 2f - 1f);
            float t = SystemClock.uptimeMillis() / 1000f;
            for (int i = 0; i < n; i++) {
                float lvl = playing
                        ? 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(t * (6.5f + i * 1.3f) + phase[i]))
                        * (0.6f + 0.4f * (float) Math.abs(Math.sin(t * 2.1f + i)))
                        : 0.28f;
                float bh = Math.max(bw, h * lvl);
                float x = i * bw * 2;
                float y = (h - bh) / 2f;
                c.drawRoundRect(new RectF(x, y, x + bw, y + bh), bw / 2, bw / 2, p);
            }
            if (playing && isShown()) postInvalidateDelayed(33);
        }
    }

    /** Circular progress ring. */
    static class Ring extends View {
        private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float value; // 0..1, <0 = indeterminate

        Ring(Context c, int color) {
            super(c);
            for (Paint p : new Paint[]{track, arc}) {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeCap(Paint.Cap.ROUND);
            }
            track.setColor(0x33FFFFFF);
            arc.setColor(color);
        }

        void setValue(float v) { value = v; invalidate(); }

        @Override protected void onDraw(Canvas c) {
            float s = Math.min(getWidth(), getHeight());
            float sw = s * 0.16f;
            track.setStrokeWidth(sw);
            arc.setStrokeWidth(sw);
            RectF r = new RectF(sw, sw, s - sw, s - sw);
            r.offset((getWidth() - s) / 2f, (getHeight() - s) / 2f);
            c.drawOval(r, track);
            if (value < 0) {
                float a = (SystemClock.uptimeMillis() % 1200) / 1200f * 360f;
                c.drawArc(r, a, 90, false, arc);
                if (isShown()) postInvalidateDelayed(33);
            } else {
                c.drawArc(r, -90, 360 * Math.min(1f, value), false, arc);
            }
        }
    }

    /** Vector glyphs for buttons. */
    static class Glyph extends View {
        static final int PLAY = 0, PAUSE = 1, PREV = 2, NEXT = 3, PHONE = 4, SEND = 5, CLOSE = 6;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private int kind;

        Glyph(Context c, int kind, int color) {
            super(c);
            this.kind = kind;
            p.setColor(color);
        }

        void setKind(int k) { kind = k; invalidate(); }

        @Override protected void onDraw(Canvas c) {
            float s = Math.min(getWidth(), getHeight()) * 0.42f;
            float cx = getWidth() / 2f, cy = getHeight() / 2f;
            Path path = new Path();
            p.setStyle(Paint.Style.FILL);
            switch (kind) {
                case PLAY:
                    path.moveTo(cx - s * 0.38f, cy - s * 0.5f);
                    path.lineTo(cx + s * 0.5f, cy);
                    path.lineTo(cx - s * 0.38f, cy + s * 0.5f);
                    path.close();
                    c.drawPath(path, p);
                    break;
                case PAUSE: {
                    float bw = s * 0.26f, r = bw / 2.5f;
                    c.drawRoundRect(new RectF(cx - s * 0.36f, cy - s * 0.48f, cx - s * 0.36f + bw, cy + s * 0.48f), r, r, p);
                    c.drawRoundRect(new RectF(cx + s * 0.36f - bw, cy - s * 0.48f, cx + s * 0.36f, cy + s * 0.48f), r, r, p);
                    break;
                }
                case PREV:
                case NEXT: {
                    float d = kind == NEXT ? 1 : -1;
                    path.moveTo(cx - d * s * 0.4f, cy - s * 0.42f);
                    path.lineTo(cx + d * s * 0.3f, cy);
                    path.lineTo(cx - d * s * 0.4f, cy + s * 0.42f);
                    path.close();
                    c.drawPath(path, p);
                    float bw = s * 0.16f;
                    float x = cx + d * s * 0.36f;
                    c.drawRoundRect(new RectF(x - bw / 2, cy - s * 0.42f, x + bw / 2, cy + s * 0.42f), bw / 2, bw / 2, p);
                    break;
                }
                case PHONE: {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.32f);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    RectF r = new RectF(cx - s * 0.6f, cy - s * 0.2f, cx + s * 0.6f, cy + s * 0.9f);
                    c.drawArc(r, 200, 140, false, p);
                    break;
                }
                case SEND:
                    path.moveTo(cx - s * 0.5f, cy - s * 0.5f);
                    path.lineTo(cx + s * 0.55f, cy);
                    path.lineTo(cx - s * 0.5f, cy + s * 0.5f);
                    path.lineTo(cx - s * 0.3f, cy);
                    path.close();
                    c.drawPath(path, p);
                    break;
                case CLOSE:
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeWidth(s * 0.18f);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    c.drawLine(cx - s * 0.4f, cy - s * 0.4f, cx + s * 0.4f, cy + s * 0.4f, p);
                    c.drawLine(cx + s * 0.4f, cy - s * 0.4f, cx - s * 0.4f, cy + s * 0.4f, p);
                    break;
            }
        }
    }
}
