package com.xplay.iptv;

import android.content.Context;
import android.graphics.Rect;
import android.view.View;
import android.widget.ScrollView;

/**
 * Vertical page that glides to the focused item (with a margin above and below) instead of jumping
 * there, the same window alignment as TvRow but up and down.
 */
public class TvScroll extends ScrollView {

    private boolean sliding;

    public TvScroll(Context c) {
        super(c);
        setVerticalScrollBarEnabled(false);
        setSmoothScrollingEnabled(true);
    }

    @Override
    public void requestChildFocus(View child, View focused) {
        int dy = 0;
        if (focused != null && getChildCount() > 0) {
            Rect r = new Rect();
            focused.getDrawingRect(r);
            offsetDescendantRectToMyCoords(focused, r);
            dy = delta(r);
        }
        sliding = true;
        super.requestChildFocus(child, focused);
        sliding = false;
        if (dy != 0) smoothScrollBy(0, dy);
    }

    private int delta(Rect r) {
        int margin = Ui.dp(getContext(), 48);
        int top = getScrollY(), bottom = top + getHeight();
        int max = Math.max(0, getChildAt(0).getHeight() - getHeight());
        int target = top;
        if (r.bottom + margin > bottom) target = r.bottom + margin - getHeight();
        if (r.top - margin < target) target = r.top - margin;
        target = Math.max(0, Math.min(max, target));
        return target - top;
    }

    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return sliding ? 0 : super.computeScrollDeltaToGetChildRectOnScreen(rect);
    }
}
