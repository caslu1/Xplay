package com.listo.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Small line icons drawn in code, so the app needs no image assets. */
public class Glyph extends View {

    public static final int LIST = 1, CALENDAR = 2, FLAG = 3, PLUS = 4, LEFT = 5, RIGHT = 6,
            TODAY = 7, CLOSE = 8, CHECKBOX = 9, MORE = 10, TRASH = 11;

    private int kind;
    private int color;
    private boolean filled;
    private String label = "";
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();

    public Glyph(Context c, int kind, int color) {
        super(c);
        this.kind = kind;
        this.color = color;
    }

    public void set(int kind, int color, boolean filled) {
        this.kind = kind;
        this.color = color;
        this.filled = filled;
        invalidate();
    }

    public void setColor(int color) {
        this.color = color;
        invalidate();
    }

    public void setFilled(boolean f) {
        filled = f;
        invalidate();
    }

    public void setLabel(String s) {
        label = s;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float s = Math.min(w, h) * 0.5f;      // icon box size
        float cx = w / 2, cy = h / 2;
        float l = cx - s / 2, t = cy - s / 2, rr = cx + s / 2, b = cy + s / 2;
        float sw = Math.max(2f, s / 12f);
        p.setColor(color);
        p.setStrokeWidth(sw);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStyle(Paint.Style.STROKE);
        path.reset();
        switch (kind) {
            case LIST:
                for (int i = 0; i < 3; i++) {
                    float y = t + s * (0.15f + i * 0.35f);
                    c.drawCircle(l + s * 0.06f, y, sw * 0.7f, fill());
                    p.setStyle(Paint.Style.STROKE);
                    c.drawLine(l + s * 0.3f, y, rr, y, p);
                }
                break;
            case CALENDAR:
            case TODAY:
                r.set(l, t + s * 0.1f, rr, b);
                c.drawRoundRect(r, s * 0.15f, s * 0.15f, p);
                c.drawLine(l + s * 0.3f, t - s * 0.02f, l + s * 0.3f, t + s * 0.2f, p);
                c.drawLine(rr - s * 0.3f, t - s * 0.02f, rr - s * 0.3f, t + s * 0.2f, p);
                if (kind == CALENDAR) {
                    c.drawLine(l, t + s * 0.38f, rr, t + s * 0.38f, p);
                } else {
                    p.setStyle(Paint.Style.FILL);
                    p.setTextAlign(Paint.Align.CENTER);
                    p.setTextSize(s * 0.5f);
                    p.setTypeface(Typeface.DEFAULT);
                    c.drawText(label, cx, b - s * 0.2f, p);
                }
                break;
            case FLAG:
                path.moveTo(l + s * 0.15f, b);
                path.lineTo(l + s * 0.15f, t);
                path.lineTo(rr - s * 0.05f, t + s * 0.05f);
                path.lineTo(rr - s * 0.3f, t + s * 0.3f);
                path.lineTo(rr - s * 0.05f, t + s * 0.58f);
                path.lineTo(l + s * 0.15f, t + s * 0.58f);
                if (filled) {
                    Path f = new Path(path);
                    f.close();
                    p.setStyle(Paint.Style.FILL);
                    p.setAlpha(70);
                    c.drawPath(f, p);
                    p.setAlpha(255);
                    p.setStyle(Paint.Style.STROKE);
                }
                c.drawPath(path, p);
                break;
            case PLUS:
                c.drawLine(cx, t, cx, b, p);
                c.drawLine(l, cy, rr, cy, p);
                break;
            case LEFT:
            case RIGHT:
                p.setStyle(Paint.Style.FILL);
                float d = kind == LEFT ? -1 : 1;
                path.moveTo(cx + d * s * 0.22f, cy);
                path.lineTo(cx - d * s * 0.18f, cy - s * 0.3f);
                path.lineTo(cx - d * s * 0.18f, cy + s * 0.3f);
                path.close();
                c.drawPath(path, p);
                break;
            case CLOSE:
                float k = s * 0.32f;
                c.drawLine(cx - k, cy - k, cx + k, cy + k, p);
                c.drawLine(cx - k, cy + k, cx + k, cy - k, p);
                break;
            case CHECKBOX:
                float rad = s * 0.5f;
                if (filled) {
                    p.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx, cy, rad, p);
                    p.setStyle(Paint.Style.STROKE);
                    p.setColor(0xFFFFFFFF);
                    path.moveTo(cx - rad * 0.45f, cy);
                    path.lineTo(cx - rad * 0.1f, cy + rad * 0.35f);
                    path.lineTo(cx + rad * 0.5f, cy - rad * 0.3f);
                    c.drawPath(path, p);
                } else {
                    c.drawCircle(cx, cy, rad - sw / 2, p);
                }
                break;
            case MORE:
                p.setStyle(Paint.Style.FILL);
                for (int i = -1; i <= 1; i++) c.drawCircle(cx, cy + i * s * 0.38f, sw * 0.9f, p);
                break;
            case TRASH:
                c.drawLine(l, t + s * 0.15f, rr, t + s * 0.15f, p);
                c.drawLine(cx - s * 0.15f, t, cx + s * 0.15f, t, p);
                r.set(l + s * 0.12f, t + s * 0.15f, rr - s * 0.12f, b);
                path.moveTo(r.left, r.top);
                path.lineTo(r.left + s * 0.05f, r.bottom);
                path.lineTo(r.right - s * 0.05f, r.bottom);
                path.lineTo(r.right, r.top);
                c.drawPath(path, p);
                break;
        }
    }

    private Paint fill() {
        p.setStyle(Paint.Style.FILL);
        return p;
    }
}
