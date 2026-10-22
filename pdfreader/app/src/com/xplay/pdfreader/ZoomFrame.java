package com.xplay.pdfreader;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.FrameLayout;

/**
 * Watches touches going to its content for pinch and taps. While pinching the
 * content is scaled as a preview; the real zoom is applied when the fingers lift.
 */
final class ZoomFrame extends FrameLayout {

    interface Listener {
        float zoom();
        void onZoom(float factor, float focusX, float focusY);
        void onDoubleTap(float x, float y);
        void onSingleTap();
    }

    private final ScaleGestureDetector scale;
    private final GestureDetector gestures;
    private Listener listener;
    private View content;
    private boolean scaling;
    private boolean stolen;
    private float factor = 1f;

    ZoomFrame(Context c) {
        super(c);
        scale = new ScaleGestureDetector(c, new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override public boolean onScaleBegin(ScaleGestureDetector d) {
                if (listener == null || content == null) return false;
                scaling = true;
                factor = 1f;
                content.setPivotX(d.getFocusX());
                content.setPivotY(d.getFocusY());
                return true;
            }

            @Override public boolean onScale(ScaleGestureDetector d) {
                float z = listener.zoom();
                factor = Math.max(MainActivity.MIN_ZOOM / z,
                        Math.min(MainActivity.MAX_ZOOM / z, factor * d.getScaleFactor()));
                content.setScaleX(factor);
                content.setScaleY(factor);
                return true;
            }

            @Override public void onScaleEnd(ScaleGestureDetector d) {
                content.setScaleX(1f);
                content.setScaleY(1f);
                listener.onZoom(factor, d.getFocusX(), d.getFocusY());
            }
        });
        gestures = new GestureDetector(c, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onSingleTapConfirmed(MotionEvent e) {
                if (listener != null) listener.onSingleTap();
                return true;
            }

            @Override public boolean onDoubleTap(MotionEvent e) {
                if (listener != null) listener.onDoubleTap(e.getX(), e.getY());
                return true;
            }
        });
    }

    void setContent(View v, Listener l) {
        removeAllViews();
        content = v;
        listener = l;
        addView(v, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
    }

    /**
     * Sees every touch before the children do (a scrolling ListView blocks
     * onInterceptTouchEvent), and takes the gesture over once a second finger lands.
     */
    @Override public boolean dispatchTouchEvent(MotionEvent ev) {
        gestures.onTouchEvent(ev);
        scale.onTouchEvent(ev);
        int a = ev.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) {
            scaling = false;
            stolen = false;
        }
        if (scaling || stolen || ev.getPointerCount() > 1) {
            if (!stolen) {
                MotionEvent cancel = MotionEvent.obtain(ev);
                cancel.setAction(MotionEvent.ACTION_CANCEL);
                super.dispatchTouchEvent(cancel);
                cancel.recycle();
                stolen = true;
            }
            return true;
        }
        return super.dispatchTouchEvent(ev);
    }
}
