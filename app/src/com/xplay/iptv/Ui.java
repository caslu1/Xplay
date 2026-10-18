package com.xplay.iptv;

import android.content.Context;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building the UI in code (the app has no XML layouts). */
public final class Ui {

    private Ui() {}

    public static int dp(Context c, float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics());
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

    /** Background that highlights on press, D-pad focus or ListView selection. */
    public static Drawable selectable(Context c, int normal, int active, float radiusPx, int borderColor) {
        StateListDrawable s = new StateListDrawable();
        Drawable on = borderColor == 0 ? round(active, radiusPx)
                : stroke(active, radiusPx, dp(c, 2), borderColor);
        s.addState(new int[] {android.R.attr.state_pressed}, on);
        s.addState(new int[] {android.R.attr.state_focused}, on);
        s.addState(new int[] {android.R.attr.state_selected}, on);
        s.addState(new int[] {}, normal == 0 ? new ColorDrawable(0) : round(normal, radiusPx));
        return s;
    }

    public static TextView text(Context c, CharSequence s, float sp, int color, boolean bold) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    public static ImageView icon(Context c, int res, int tint) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setColorFilter(tint);
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        return v;
    }

    /** A focusable round icon button (works with touch and TV remotes). */
    public static ImageView iconButton(Context c, int res, int tint, int focusBorder) {
        ImageView v = icon(c, res, tint);
        int p = dp(c, 8);
        v.setPadding(p, p, p, p);
        v.setFocusable(true);
        v.setClickable(true);
        v.setBackground(selectable(c, 0, 0x22FFFFFF, dp(c, 12), focusBorder));
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

    public static LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, w);
    }

    /** Flexible gap for rows; zero height so it never stretches the row vertically. */
    public static LinearLayout.LayoutParams spacer() {
        return new LinearLayout.LayoutParams(0, 0, 1f);
    }

    /** "Xplay" wordmark: gold X, white rest. */
    public static View logo(Context c, float sp) {
        LinearLayout l = row(c);
        ImageView mark = new ImageView(c);
        mark.setImageResource(R.drawable.ic_launcher);
        int s = (int) (dp(c, sp) * 1.25f);
        l.addView(mark, lp(s, s));
        SpannableString word = new SpannableString("Xplay");
        word.setSpan(new ForegroundColorSpan(Palette.GOLD), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextView t = text(c, word, sp, Palette.TEXT, true);
        t.setPadding(dp(c, 8), 0, 0, 0);
        l.addView(t);
        return l;
    }

    public static String pad3(int n) {
        return n < 10 ? "00" + n : n < 100 ? "0" + n : String.valueOf(n);
    }
}
