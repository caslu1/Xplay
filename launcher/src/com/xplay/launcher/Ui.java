package com.xplay.launcher;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Small helpers for building the UI in code (the launcher has no XML layouts). */
public final class Ui {

    /** Colors of the active theme; set from the panel config by {@link #applyTheme}. */
    public static int ACCENT = 0xFF3B82F6;
    public static int CARD = 0xCC111827;
    public static int TEXT = Color.WHITE;
    public static int MUTED = 0xFF9CA3AF;
    public static int FOCUS = Color.WHITE;

    private Ui() {}

    public static void applyTheme(Config cfg) {
        ACCENT = cfg.themeColor("accent", 0xFF3B82F6);
        CARD = cfg.themeColor("card", 0xCC111827);
        TEXT = cfg.themeColor("text", Color.WHITE);
        MUTED = cfg.themeColor("muted", 0xFF9CA3AF);
        FOCUS = cfg.themeColor("focus", Color.WHITE);
    }

    public static int color(String s, int fallback) {
        if (s == null || s.trim().isEmpty()) return fallback;
        try {
            return Color.parseColor(s.trim());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

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

    /** Card background that gets a ring in the theme's focus color while focused or pressed. */
    public static Drawable card(Context c, int color, float radiusPx) {
        StateListDrawable s = new StateListDrawable();
        GradientDrawable on = round(color, radiusPx);
        on.setStroke(dp(c, 3), FOCUS);
        s.addState(new int[] {android.R.attr.state_focused}, on);
        s.addState(new int[] {android.R.attr.state_pressed}, on);
        s.addState(new int[] {android.R.attr.state_selected}, on);
        s.addState(new int[] {}, round(color, radiusPx));
        return s;
    }

    /** Foreground ring for cards whose content (an image) would cover a background ring. */
    public static Drawable ring(Context c, float radiusPx) {
        StateListDrawable s = new StateListDrawable();
        GradientDrawable on = new GradientDrawable();
        on.setColor(0x00000000);
        on.setCornerRadius(radiusPx);
        on.setStroke(dp(c, 3), FOCUS);
        s.addState(new int[] {android.R.attr.state_focused}, on);
        s.addState(new int[] {android.R.attr.state_pressed}, on);
        s.addState(new int[] {}, new android.graphics.drawable.ColorDrawable(0));
        return s;
    }

    /** Makes v focusable by the remote and zooms it slightly while it has focus. */
    public static void focusable(View v, final float zoom) {
        v.setFocusable(true);
        v.setClickable(true);
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            public void onFocusChange(View view, boolean has) {
                float s = has ? zoom : 1f;
                // translationZ draws the zoomed view above its neighbours; bringToFront() would
                // reorder the children of a LinearLayout.
                view.animate().scaleX(s).scaleY(s).translationZ(has ? 12f : 0f).setDuration(120).start();
            }
        });
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
        if (tint != 0) v.setColorFilter(tint);
        v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        return v;
    }

    /** Round icon button for the top bar. */
    public static ImageView iconButton(Context c, int res) {
        ImageView v = icon(c, res, TEXT);
        int p = dp(c, 9);
        v.setPadding(p, p, p, p);
        v.setBackground(card(c, 0x00000000, dp(c, 24)));
        focusable(v, 1.1f);
        return v;
    }

    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
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
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, w);
    }

    public static int iconRes(String name) {
        if (name == null) return 0;
        switch (name) {
            case "tv": return R.drawable.ic_tv;
            case "store": return R.drawable.ic_store;
            case "apps": return R.drawable.ic_apps;
            case "settings": return R.drawable.ic_settings;
            case "android": return R.drawable.ic_android;
            case "wifi": return R.drawable.ic_wifi;
            case "refresh": return R.drawable.ic_refresh;
            default: return 0;
        }
    }
}
