package com.xplay.mobile;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Helpers for building the UI in code (the app has no XML layouts). */
public final class Ui {

    public static final Typeface MEDIUM = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    public static final Typeface BOLD = Typeface.create("sans-serif", Typeface.BOLD);
    public static final Typeface BLACK = Typeface.create("sans-serif-black", Typeface.NORMAL);

    private Ui() {}

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, c.getResources().getDisplayMetrics());
    }

    public static GradientDrawable round(int color, float radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static GradientDrawable stroke(int color, float radiusPx, int widthPx, int strokeColor) {
        GradientDrawable d = round(color, radiusPx);
        d.setStroke(widthPx, strokeColor);
        return d;
    }

    public static GradientDrawable vertical(int... colors) {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, colors);
    }

    /** Touch ripple over a background (or over nothing when content is null). */
    public static Drawable ripple(Drawable content, float radiusPx) {
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, round(0xFFFFFFFF, radiusPx));
    }

    /** Ripple background that also shows a pink ring when focused with a remote / keyboard. */
    public static Drawable pressable(Context c, int color, float radiusPx) {
        android.graphics.drawable.StateListDrawable s = new android.graphics.drawable.StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused}, stroke(color, radiusPx, dp(c, 2), Theme.PINK));
        s.addState(new int[] {}, color == 0 ? new android.graphics.drawable.ColorDrawable(0) : round(color, radiusPx));
        return ripple(s, radiusPx);
    }

    public static TextView text(Context c, CharSequence s, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        return t;
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, Typeface face) {
        TextView t = text(c, s, sp, color);
        t.setTypeface(face);
        return t;
    }

    public static TextView oneLine(TextView t) {
        t.setSingleLine(true);
        t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        return t;
    }

    public static ImageView icon(Context c, int res, int tint) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setColorFilter(tint);
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return v;
    }

    /** A round touch target with an icon inside. */
    public static ImageView iconButton(Context c, int res, int tint, int paddingDp) {
        ImageView v = icon(c, res, tint);
        int p = dp(c, paddingDp);
        v.setPadding(p, p, p, p);
        v.setClickable(true);
        v.setFocusable(true);
        v.setBackground(pressable(c, 0, dp(c, 40)));
        return v;
    }

    public static void clipRound(View v, final float radiusPx) {
        v.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radiusPx);
            }
        });
        v.setClipToOutline(true);
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        return l;
    }

    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    public static LinearLayout.LayoutParams lp(int w, int h) {
        return new LinearLayout.LayoutParams(w, h);
    }

    public static LinearLayout.LayoutParams lp(int w, int h, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h);
        p.setMargins(left, top, right, bottom);
        return p;
    }

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
    }

    /** Flexible gap for rows; zero height so it never stretches the row vertically. */
    public static LinearLayout.LayoutParams spacer() {
        return new LinearLayout.LayoutParams(0, 0, 1f);
    }

    public static FrameLayout.LayoutParams frame(int w, int h, int gravity) {
        return new FrameLayout.LayoutParams(w, h, gravity);
    }

    public static int screenWidth(Context c) {
        return c.getResources().getDisplayMetrics().widthPixels;
    }

    public static boolean landscape(Context c) {
        android.util.DisplayMetrics m = c.getResources().getDisplayMetrics();
        return m.widthPixels > m.heightPixels;
    }

    /** Applies a left-to-right pink → peach gradient to the text. */
    public static void gradientText(final TextView t, final int from, final int to) {
        t.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v, int l, int top, int r, int b, int ol, int ot, int or, int ob) {
                float w = t.getPaint().measureText(t.getText().toString());
                t.getPaint().setShader(new LinearGradient(0, 0, Math.max(1, w), 0, from, to, Shader.TileMode.CLAMP));
                t.invalidate();
            }
        });
    }

    /** "Xplay" wordmark: white X, pink-to-peach "play", next to the round mark. */
    public static LinearLayout logo(Context c, float sp) {
        LinearLayout row = row(c);
        int mark = dp(c, sp * 1.9f);
        row.addView(new LogoMark(c), lp(mark, mark));
        TextView x = text(c, "X", sp, Theme.TEXT, BLACK);
        x.setIncludeFontPadding(false);
        TextView play = text(c, "play", sp, Theme.PINK, BLACK);
        play.setIncludeFontPadding(false);
        gradientText(play, 0xFFF4A6C8, Theme.PEACH);
        LinearLayout.LayoutParams xl = lp(-2, -2);
        xl.leftMargin = dp(c, sp * 0.35f);
        row.addView(x, xl);
        row.addView(play);
        return row;
    }

    /** The round mark: a thick ring open on the right with a play triangle inside. */
    public static final class LogoMark extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        private final RectF oval = new RectF();

        public LogoMark(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight();
            float s = Math.min(w, h);
            float cx = w / 2, cy = h / 2;
            paint.setShader(new LinearGradient(0, 0, s, s, 0xFFF7ACCD, 0xFFE05592, Shader.TileMode.CLAMP));
            float stroke = s * 0.22f;
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setStrokeCap(Paint.Cap.ROUND);
            float r = s / 2 - stroke / 2;
            oval.set(cx - r, cy - r, cx + r, cy + r);
            canvas.drawArc(oval, 35, 290, false, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setShader(null);
            paint.setColor(0xFFFFFFFF);
            float t = s * 0.15f;
            path.reset();
            path.moveTo(cx - t * 0.6f, cy - t);
            path.lineTo(cx - t * 0.6f, cy + t);
            path.lineTo(cx + t, cy);
            path.close();
            canvas.drawPath(path, paint);
        }
    }

    /** Rounded square with the profile's emoji. */
    public static FrameLayout avatar(Context c, Repo.Profile p, int sizePx) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(round(p == null ? Theme.AVATAR_BG[0] : p.color, sizePx * 0.12f));
        TextView e = text(c, p == null ? "🍿" : p.emoji, 0, Theme.TEXT);
        e.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx * 0.5f);
        e.setGravity(Gravity.CENTER);
        f.addView(e, frame(-1, -1, Gravity.CENTER));
        return f;
    }

    /** Small rounded label on top of posters ("Faltam 46min", "5"). */
    public static TextView badge(Context c, String s) {
        TextView t = text(c, s, 12, Theme.TEXT, MEDIUM);
        t.setBackground(round(0xB3141416, dp(c, 20)));
        t.setPadding(dp(c, 10), dp(c, 4), dp(c, 10), dp(c, 4));
        t.setGravity(Gravity.CENTER);
        return t;
    }

    /** Thin progress line (white over translucent white). */
    public static View progressLine(Context c, final float fraction) {
        return new View(c) {
            private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            protected void onDraw(Canvas canvas) {
                float h = getHeight(), w = getWidth();
                p.setColor(0x66FFFFFF);
                canvas.drawRoundRect(0, 0, w, h, h / 2, h / 2, p);
                p.setColor(0xFFFFFFFF);
                canvas.drawRoundRect(0, 0, Math.max(h, w * Math.max(0, Math.min(1, fraction))), h, h / 2, h / 2, p);
            }
        };
    }

    public static View divider(Context c) {
        View v = new View(c);
        v.setBackgroundColor(0x14FFFFFF);
        v.setLayoutParams(lp(-1, Math.max(1, dp(c, 0.7f))));
        return v;
    }

    public static String time(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600, m = (s / 60) % 60, sec = s % 60;
        return h > 0 ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, sec)
                : String.format(java.util.Locale.US, "%02d:%02d", m, sec);
    }

    /** "8:27" style (no leading zero on minutes) for buttons. */
    public static String shortTime(long ms) {
        long s = Math.max(0, ms / 1000);
        long h = s / 3600, m = (s / 60) % 60, sec = s % 60;
        return h > 0 ? String.format(java.util.Locale.US, "%d:%02d:%02d", h, m, sec)
                : String.format(java.util.Locale.US, "%d:%02d", m, sec);
    }

    /** "Faltam 46min" / "Faltam 1h 05min". */
    public static String remaining(long ms) {
        long min = Math.max(1, Math.round(ms / 60000.0));
        return "Faltam " + (min >= 60 ? (min / 60) + "h " + String.format(java.util.Locale.US, "%02d", min % 60) + "min"
                : min + "min");
    }

    public static void toast(Context c, String s) {
        android.widget.Toast.makeText(c, s, android.widget.Toast.LENGTH_SHORT).show();
    }
}
