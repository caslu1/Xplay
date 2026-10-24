package com.xplay.iptv;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

/**
 * A horizontal row that moves like the Android TV Leanback rows (the open-source mechanic used by
 * most TV and IPTV apps): the focused item always lines up at the same spot on the left (the
 * "keyline"), the row slides under it, and the row remembers which item was focused so ▲ ▼ come
 * back to it. Leanback itself cannot be bundled here, so this is the same window alignment on a
 * plain HorizontalScrollView.
 */
public class TvRow extends HorizontalScrollView {

    /** Even gap between items, same in every row of the app. */
    public static int gap(Context c) {
        return Ui.dp(c, 10);
    }

    private final LinearLayout strip;
    private int keyline;
    private int selected;

    public TvRow(Context c) {
        super(c);
        setHorizontalScrollBarEnabled(false);
        setSmoothScrollingEnabled(true);
        setClipToPadding(false);
        setClipChildren(false);
        strip = Ui.row(c);
        strip.setClipChildren(false);
        strip.setClipToPadding(false);
        addView(strip, new LayoutParams(-2, -2));
    }

    public LinearLayout strip() {
        return strip;
    }

    /** Distance from the row's left edge where the focused item sits. */
    public void setKeyline(int px) {
        keyline = px;
    }

    /** Adds an item with the row's standard gap after it. */
    public void addItem(View v, int width, int height) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        lp.rightMargin = gap(getContext());
        strip.addView(v, lp);
    }

    public int selectedIndex() {
        return Math.min(selected, Math.max(0, strip.getChildCount() - 1));
    }

    /** Focuses the remembered item (the first one the first time). */
    public boolean focusSelected() {
        int n = strip.getChildCount();
        for (int i = selectedIndex(); i < n; i++) {
            View v = strip.getChildAt(i);
            if (v.isFocusable() && v.getVisibility() == VISIBLE) return v.requestFocus();
        }
        for (int i = 0; i < n; i++) {
            View v = strip.getChildAt(i);
            if (v.isFocusable() && v.getVisibility() == VISIBLE) return v.requestFocus();
        }
        return false;
    }

    /** While true, the base class must not jump: we slide there ourselves. */
    private boolean sliding;

    @Override
    public void requestChildFocus(View child, View focused) {
        View item = focused;
        while (item != null && item.getParent() != strip) {
            item = item.getParent() instanceof View ? (View) item.getParent() : null;
        }
        if (item != null) selected = strip.indexOfChild(item);
        int dx = item == null ? 0 : alignDelta(item.getLeft());
        sliding = true;
        super.requestChildFocus(child, focused);
        sliding = false;
        if (dx != 0) smoothScrollBy(dx, 0);
    }

    private int alignDelta(int left) {
        if (getChildCount() == 0) return 0;
        int max = Math.max(0, getChildAt(0).getWidth() - getWidth() + getPaddingLeft() + getPaddingRight());
        int target = Math.max(0, Math.min(max, left - keyline));
        return target - getScrollX();
    }

    /** Leanback window alignment: put the focused item at the keyline, without scrolling past either end. */
    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return sliding ? 0 : alignDelta(rect.left);
    }

    /** The TvRow holding v, or null. */
    public static TvRow of(View v) {
        while (v != null) {
            if (v instanceof TvRow) return (TvRow) v;
            v = v.getParent() instanceof View ? (View) v.getParent() : null;
        }
        return null;
    }

    /** True for a held key repeating faster than the UI should move (keeps fast scrolling smooth). */
    private static long lastMove;

    public static boolean throttle(android.view.KeyEvent e) {
        long now = android.os.SystemClock.uptimeMillis();
        if (e.getRepeatCount() > 0 && now - lastMove < 90) return true;
        lastMove = now;
        return false;
    }
}
