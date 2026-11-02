package com.ilha.notch;

import android.accessibilityservice.AccessibilityService;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Draws the island. An accessibility overlay is the only window type that may cover the
 * status bar and the camera cut-out; the service does not read any screen content.
 */
public class IslandService extends AccessibilityService {
    private static final int GREY = 0xFFA7A7AD;
    private static final int BTN = 0x26FFFFFF;

    private final Handler h = new Handler(Looper.getMainLooper());
    private WindowManager wm;
    private WindowManager.LayoutParams lp;
    private FrameLayout root, shape, compact, expanded, bubble;
    private View calRing;
    private GradientDrawable shapeBg;
    private float dp;
    private int sw, sh;

    // geometry
    private int camCx, camCy, camD, pillH, pillW;

    // state
    private List<Item> live = new ArrayList<>();
    private String primaryKey;
    private boolean userExpanded;
    private Item notif;
    private boolean replying;
    private boolean calibrating;
    private boolean landscape;
    private String expandedKey;
    private Item expandedItem;

    // current visual
    private final RectF cur = new RectF();
    private float curF, curA, curB;
    private ValueAnimator anim;
    private boolean windowShown;

    private final List<Runnable> tickers = new ArrayList<>();     // pill
    private final List<Runnable> cardTickers = new ArrayList<>(); // expanded card
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            for (Runnable r : new ArrayList<>(tickers)) r.run();
            for (Runnable r : new ArrayList<>(cardTickers)) r.run();
            if (windowShown && (!tickers.isEmpty() || !cardTickers.isEmpty())) h.postDelayed(this, 500);
        }
    };
    private final Runnable autoClose = new Runnable() {
        @Override public void run() {
            if (replying) return;
            if (notif != null) {
                notif = null;
            } else {
                userExpanded = false;
            }
            render();
        }
    };

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onServiceConnected() {
        Hub.island = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        dp = getResources().getDisplayMetrics().density;
        buildWindow();
        if (!Prefs.calibrated(this)) detectCutout();
        loadGeometry();
        live = Hub.live;
        Hub.requestResync();
        render();
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() {}

    @Override
    public boolean onUnbind(Intent intent) {
        teardown();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        teardown();
        super.onDestroy();
    }

    private void teardown() {
        if (Hub.island == this) Hub.island = null;
        h.removeCallbacksAndMessages(null);
        if (anim != null) anim.cancel();
        if (root != null) {
            try { wm.removeView(root); } catch (Exception ignored) {}
            root = null;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        loadGeometry();
        if (anim != null) anim.cancel();
        curA = 0;
        render();
    }

    // ------------------------------------------------------------------ public API (Hub / settings)

    void onLive(List<Item> items) {
        live = items;
        boolean found = false;
        for (Item it : items) if (it.key.equals(primaryKey)) found = true;
        if (!found) {
            primaryKey = items.isEmpty() ? null : items.get(0).key;
            if (items.isEmpty()) userExpanded = false;
        }
        render();
    }

    void onNotification(Item it) {
        if (landscape && it.type != Item.INCOMING) return;
        if (notif != null && notif.type == Item.INCOMING && it.type != Item.INCOMING) return;
        if (replying) return;
        notif = it;
        userExpanded = false;
        if (Prefs.vibrate(this)) {
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(it.type == Item.INCOMING ? 400 : 30);
        }
        h.removeCallbacks(autoClose);
        if (it.type != Item.INCOMING) h.postDelayed(autoClose, Prefs.duration(this) * 1000L);
        render();
    }

    void onRemoved(String key) {
        if (notif != null && key.equals(notif.key) && !replying) {
            notif = null;
            render();
        }
    }

    void setCalibrating(boolean b) {
        calibrating = b;
        render();
    }

    void reloadGeometry() {
        loadGeometry();
        if (anim != null) anim.cancel();
        expandedKey = null;
        render();
    }

    void showTest() {
        Item it = new Item();
        it.type = Item.TEST;
        it.key = "test";
        it.pkg = getPackageName();
        it.app = "Ilha";
        it.title = "Tudo pronto!";
        it.text = "Suas notificações, músicas e chamadas aparecem aqui, ao redor da câmera.";
        it.icon = getApplicationInfo().loadIcon(getPackageManager());
        it.large = it.icon;
        it.color = 0xFF9EE6C8;
        onNotification(it);
    }

    /** Finds the camera from the display cut-out. Returns true if one was found. */
    boolean detectCutout() {
        Rect r = cutoutRect();
        if (r == null) return false;
        DisplayMetrics m = realMetrics();
        int d = Math.min(r.width(), r.height());
        Prefs.setCamera(this, r.centerX() - m.widthPixels / 2, r.centerY(), d);
        loadGeometry();
        render();
        return true;
    }

    // ------------------------------------------------------------------ geometry

    private DisplayMetrics realMetrics() {
        DisplayMetrics m = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(m);
        return m;
    }

    private int statusBar() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : Math.round(24 * dp);
    }

    @SuppressWarnings("unchecked")
    private Rect cutoutRect() {
        try {
            Method gc = wm.getDefaultDisplay().getClass().getMethod("getCutout");
            Object cut = gc.invoke(wm.getDefaultDisplay());
            if (cut == null && root != null && root.getRootWindowInsets() != null) {
                Object ins = root.getRootWindowInsets();
                cut = ins.getClass().getMethod("getDisplayCutout").invoke(ins);
            }
            if (cut == null) return null;
            List<Rect> rects = (List<Rect>) cut.getClass().getMethod("getBoundingRects").invoke(cut);
            DisplayMetrics m = realMetrics();
            for (Rect r : rects) if (r.top < m.heightPixels / 4 && r.width() < m.widthPixels / 2) return r;
        } catch (Throwable ignored) {
        }
        return null;
    }

    private void loadGeometry() {
        DisplayMetrics m = realMetrics();
        sw = m.widthPixels;
        sh = m.heightPixels;
        landscape = sw > sh && Prefs.hideLandscape(this);
        int sb = statusBar();
        int y = Prefs.camY(this), d = Prefs.camD(this);
        camCx = sw / 2 + Prefs.camX(this);
        camCy = y >= 0 ? y : sb / 2;
        camD = d > 0 ? d : Math.round(sb * 0.55f);
        pillH = Math.max(camD + Math.round(8 * dp), Math.round(26 * dp));
        pillW = Math.max(Math.round(Prefs.pillW(this) * dp), pillH * 2 + camD);
    }

    private RectF pillRect() {
        float top = Math.max(0, camCy - pillH / 2f);
        return new RectF(camCx - pillW / 2f, top, camCx + pillW / 2f, top + pillH);
    }

    private float cardW() {
        return Math.min(sw - 16 * dp, 440 * dp);
    }

    private RectF cardRect(int height) {
        float w = cardW();
        float left = (sw - w) / 2f;
        RectF p = pillRect();
        return new RectF(left, p.top, left + w, p.top + Math.max(height, pillH));
    }

    private RectF dotRect() {
        float r = camD / 2f + 2 * dp;
        RectF p = pillRect();
        float cy = p.centerY();
        return new RectF(camCx - r, cy - r, camCx + r, cy + r);
    }

    private RectF bubbleRect() {
        RectF p = pillRect();
        float gap = 6 * dp;
        return new RectF(p.right + gap, p.top, p.right + gap + pillH, p.bottom);
    }

    // ------------------------------------------------------------------ window

    private void buildWindow() {
        root = new FrameLayout(this);
        root.setClipChildren(false);
        root.setVisibility(View.INVISIBLE);

        shape = new FrameLayout(this);
        shapeBg = Widgets.round(0xFF000000, 100);
        shape.setBackground(shapeBg);
        shape.setClipToOutline(true);
        root.addView(shape, new FrameLayout.LayoutParams(1, 1));

        compact = new FrameLayout(this);
        shape.addView(compact, new FrameLayout.LayoutParams(1, 1));
        expanded = new FrameLayout(this);
        shape.addView(expanded, new FrameLayout.LayoutParams(1, ViewGroup.LayoutParams.WRAP_CONTENT));

        bubble = new FrameLayout(this);
        bubble.setBackground(Widgets.round(0xFF000000, 1000));
        bubble.setClipToOutline(true);
        root.addView(bubble, new FrameLayout.LayoutParams(1, 1));
        bubble.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Item s = secondary();
                if (s != null) {
                    primaryKey = s.key;
                    v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    render();
                }
            }
        });

        final GestureDetector gd = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override public boolean onDown(MotionEvent e) { return true; }

            @Override public boolean onSingleTapUp(MotionEvent e) {
                bumpAutoClose();
                if (notif != null) {
                    if (notif.type == Item.TEST) { notif = null; render(); return true; }
                    if (notif.type == Item.INCOMING) { open(notif); return true; }
                    open(notif);
                    notif = null;
                    render();
                } else if (userExpanded) {
                    Item p = primary();
                    if (p != null && p.type != Item.MEDIA) { open(p); userExpanded = false; render(); }
                } else if (primary() != null) {
                    userExpanded = true;
                    shape.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                    render();
                }
                return true;
            }

            @Override public void onLongPress(MotionEvent e) {
                Item p = primary();
                if (notif == null && !userExpanded && p != null) {
                    shape.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    open(p);
                }
            }

            @Override public boolean onFling(MotionEvent a, MotionEvent b, float vx, float vy) {
                if (vy < -400) {
                    if (notif != null && notif.type != Item.INCOMING && !replying) { notif = null; render(); }
                    else if (userExpanded) { userExpanded = false; render(); }
                    return true;
                }
                if (vy > 400 && notif == null && !userExpanded && primary() != null) {
                    userExpanded = true;
                    render();
                    return true;
                }
                return false;
            }
        });
        shape.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) { return gd.onTouchEvent(e); }
        });
        root.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) {
                if (e.getActionMasked() == MotionEvent.ACTION_OUTSIDE || e.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    onOutside();
                }
                return false;
            }
        });

        calRing = new View(this) {
            final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(android.graphics.Canvas c) {
                p.setStyle(android.graphics.Paint.Style.STROKE);
                p.setStrokeWidth(2 * dp);
                p.setColor(0xFF7CF2C4);
                float r = Math.min(getWidth(), getHeight()) / 2f - dp;
                c.drawCircle(getWidth() / 2f, getHeight() / 2f, r, p);
            }
        };

        lp = new WindowManager.LayoutParams(1, 1, WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        try {
            // draw into the camera cut-out (API 28+): SHORT_EDGES = 1, ALWAYS = 3
            WindowManager.LayoutParams.class.getField("layoutInDisplayCutoutMode")
                    .setInt(lp, android.os.Build.VERSION.SDK_INT >= 30 ? 3 : 1);
        } catch (Throwable ignored) {
        }
        wm.addView(root, lp);
    }

    private void onOutside() {
        if (replying) return;
        if (notif != null && notif.type != Item.INCOMING) {
            notif = null;
            render();
        } else if (userExpanded) {
            userExpanded = false;
            render();
        }
    }

    private void setWindow(RectF r, boolean touchable) {
        int x = Math.max(0, (int) Math.floor(r.left));
        int y = Math.max(0, (int) Math.floor(r.top));
        int w = Math.max(1, Math.min(sw, (int) Math.ceil(r.right)) - x);
        int ht = Math.max(1, Math.min(sh, (int) Math.ceil(r.bottom)) - y);
        int flags = lp.flags;
        if (touchable) flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        else flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        if (replying) flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        else flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        if (x == lp.x && y == lp.y && w == lp.width && ht == lp.height && flags == lp.flags) return;
        lp.x = x;
        lp.y = y;
        lp.width = w;
        lp.height = ht;
        lp.flags = flags;
        try { wm.updateViewLayout(root, lp); } catch (Exception ignored) {}
    }

    // ------------------------------------------------------------------ state → visuals

    private Item primary() {
        for (Item it : live) if (it.key.equals(primaryKey)) return it;
        return live.isEmpty() ? null : live.get(0);
    }

    private Item secondary() {
        Item p = primary();
        for (Item it : live) if (p != null && !it.key.equals(p.key)) return it;
        return null;
    }

    private void render() {
        if (root == null) return;
        Item primary = primary();
        Item secondary = secondary();
        Item card = notif != null ? notif : (userExpanded ? primary : null);
        boolean showCompact = primary != null || calibrating;
        if (landscape && (card == null || card.type != Item.INCOMING)) {
            card = null;
            showCompact = false;
        }

        buildCompact(primary);
        buildBubble(showCompact && card == null ? secondary : null);

        RectF target;
        float tf, ta;
        if (card != null) {
            String key = card.type + ":" + card.key + ":" + replying;
            if (!key.equals(expandedKey) || card != expandedItem) {
                buildExpanded(card);
                expandedKey = key;
                expandedItem = card;
            }
            target = cardRect(measureExpanded());
            tf = 1;
            ta = 1;
        } else if (showCompact) {
            target = pillRect();
            tf = 0;
            ta = 1;
        } else {
            target = dotRect();
            tf = 0;
            ta = 0;
        }
        float tb = (card == null && showCompact && secondary != null) ? 1 : 0;
        if (card != null && notif == null) bumpAutoClose();
        if (card == null && notif == null) h.removeCallbacks(autoClose);
        animateTo(target, tf, ta, tb);
        h.removeCallbacks(tick);
        h.post(tick);
    }

    private void bumpAutoClose() {
        if (notif != null) {
            if (notif.type == Item.INCOMING) return;
            h.removeCallbacks(autoClose);
            h.postDelayed(autoClose, Prefs.duration(this) * 1000L);
        } else if (userExpanded) {
            h.removeCallbacks(autoClose);
            h.postDelayed(autoClose, 8000);
        }
    }

    private int measureExpanded() {
        int w = Math.round(cardW());
        expanded.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        return expanded.getMeasuredHeight();
    }

    private void animateTo(final RectF target, final float tf, final float ta, final float tb) {
        if (anim != null) anim.cancel();
        if (curA <= 0.001f && ta <= 0.001f) {
            cur.set(target);
            curF = tf;
            curA = 0;
            curB = tb;
            hideWindow();
            return;
        }
        final RectF from = new RectF(curA <= 0.001f ? dotRect() : cur);
        final float f0 = curA <= 0.001f ? 0 : curF, a0 = curA, b0 = curB;
        RectF win = new RectF(from);
        win.union(target);
        if (b0 > 0 || tb > 0) win.union(bubbleRect());
        win.inset(-14 * dp, -14 * dp);
        root.setVisibility(View.VISIBLE);
        windowShown = true;
        setWindow(win, ta > 0);
        final boolean grow = target.width() * target.height() > from.width() * from.height();
        anim = ValueAnimator.ofFloat(0, 1);
        anim.setDuration(grow ? 460 : 320);
        anim.setInterpolator(grow ? new OvershootInterpolator(1.1f) : new DecelerateInterpolator(1.6f));
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator va) {
                float v = (Float) va.getAnimatedValue();
                float c = Math.max(0, Math.min(1, v));
                cur.set(lerp(from.left, target.left, v), lerp(from.top, target.top, Math.min(v, 1)),
                        lerp(from.right, target.right, v), lerp(from.bottom, target.bottom, v));
                curF = lerp(f0, tf, c);
                curA = lerp(a0, ta, c);
                curB = lerp(b0, tb, c);
                apply();
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            boolean cancelled;
            @Override public void onAnimationCancel(Animator a) { cancelled = true; }
            @Override public void onAnimationEnd(Animator a) {
                if (cancelled) return;
                cur.set(target);
                curF = tf;
                curA = ta;
                curB = tb;
                if (ta <= 0.001f) {
                    hideWindow();
                } else {
                    RectF w = new RectF(target);
                    if (tb > 0) w.union(bubbleRect());
                    w.inset(-2 * dp, -2 * dp);
                    setWindow(w, true);
                    apply();
                }
                if (tf == 0) {
                    expanded.removeAllViews();
                    cardTickers.clear();
                    expandedKey = null;
                    expandedItem = null;
                }
            }
        });
        apply();
        anim.start();
    }

    private void hideWindow() {
        windowShown = false;
        root.setVisibility(View.INVISIBLE);
        expanded.removeAllViews();
        expandedKey = null;
        expandedItem = null;
        tickers.clear();
        cardTickers.clear();
        setWindow(new RectF(0, 0, 1, 1), false);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Places every piece for the current animated values. */
    private void apply() {
        float wx = lp.x, wy = lp.y;
        FrameLayout.LayoutParams sp = (FrameLayout.LayoutParams) shape.getLayoutParams();
        sp.width = Math.max(1, Math.round(cur.width()));
        sp.height = Math.max(1, Math.round(cur.height()));
        shape.setLayoutParams(sp);
        shape.setTranslationX(cur.left - wx);
        shape.setTranslationY(cur.top - wy);
        float rPill = Math.min(cur.height(), cur.width()) / 2f;
        float rCard = Math.min(30 * dp, rPill);
        shapeBg.setCornerRadius(lerp(rPill, rCard, curF));
        shape.setAlpha(Math.min(1, curA * 1.4f));

        RectF p = pillRect();
        FrameLayout.LayoutParams cp = (FrameLayout.LayoutParams) compact.getLayoutParams();
        if (cp.width != Math.round(p.width()) || cp.height != Math.round(p.height())) {
            cp.width = Math.round(p.width());
            cp.height = Math.round(p.height());
            compact.setLayoutParams(cp);
        }
        compact.setTranslationX(p.left - cur.left);
        compact.setTranslationY(p.top - cur.top);
        compact.setAlpha(Math.max(0, 1 - curF * 2.2f) * curA);

        RectF c = cardRect(1);
        FrameLayout.LayoutParams ep = (FrameLayout.LayoutParams) expanded.getLayoutParams();
        if (ep.width != Math.round(c.width())) {
            ep.width = Math.round(c.width());
            expanded.setLayoutParams(ep);
        }
        expanded.setTranslationX(c.left - cur.left);
        expanded.setTranslationY(c.top - cur.top);
        float ea = Math.max(0, (curF - 0.35f) / 0.65f);
        expanded.setAlpha(ea);
        expanded.setVisibility(curF > 0.01f ? View.VISIBLE : View.INVISIBLE);

        RectF b = bubbleRect();
        FrameLayout.LayoutParams bp = (FrameLayout.LayoutParams) bubble.getLayoutParams();
        if (bp.width != Math.round(b.width())) {
            bp.width = bp.height = Math.round(b.width());
            bubble.setLayoutParams(bp);
        }
        float shift = (1 - curB) * -b.width() * 0.8f;
        bubble.setTranslationX(b.left - wx + shift);
        bubble.setTranslationY(b.top - wy);
        bubble.setAlpha(curB * curA);
        bubble.setScaleX(0.5f + 0.5f * curB);
        bubble.setScaleY(0.5f + 0.5f * curB);
        bubble.setVisibility(curB > 0.01f ? View.VISIBLE : View.INVISIBLE);
    }

    // ------------------------------------------------------------------ compact pill

    private void buildCompact(Item it) {
        compact.removeAllViews();
        tickers.clear();
        int pad = Math.max(Math.round(4 * dp), (pillH - Math.round(22 * dp)) / 2);
        int s = pillH - pad * 2;
        int side = (pillW - camD) / 2 - pad - Math.round(4 * dp);
        if (it == null) {
            if (calibrating) {
                FrameLayout.LayoutParams rp = new FrameLayout.LayoutParams(camD + Math.round(6 * dp), camD + Math.round(6 * dp));
                rp.leftMargin = camCx - Math.round(pillRect().left) - rp.width / 2;
                rp.topMargin = (pillH - rp.height) / 2 + Math.round(camCy - pillRect().centerY());
                compact.addView(calRing, rp);
            }
            return;
        }
        View left = leadingView(it, s);
        FrameLayout.LayoutParams lpL = new FrameLayout.LayoutParams(s, s, Gravity.LEFT | Gravity.CENTER_VERTICAL);
        lpL.leftMargin = pad;
        compact.addView(left, lpL);

        View right;
        int rw = s;
        switch (it.type) {
            case Item.MEDIA: {
                Widgets.Bars bars = new Widgets.Bars(this, it.color);
                bars.setPlaying(it.playing);
                right = bars;
                rw = Math.round(s * 0.9f);
                break;
            }
            case Item.PROGRESS: {
                Widgets.Ring ring = new Widgets.Ring(this, it.color);
                ring.setValue(it.indeterminate || it.max <= 0 ? -1 : it.progress / (float) it.max);
                right = ring;
                rw = Math.round(s * 0.85f);
                break;
            }
            default: {
                final TextView t = text(13, it.type == Item.CALL ? 0xFF4CE08A : it.color, true);
                t.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
                rw = Math.max(s, side);
                final Item item = it;
                if (it.chronoBase > 0) {
                    Runnable r = new Runnable() { @Override public void run() { t.setText(chrono(item)); } };
                    r.run();
                    tickers.add(r);
                } else {
                    t.setText(it.title != null ? it.title : "");
                    t.setTextSize(11);
                }
                right = t;
            }
        }
        FrameLayout.LayoutParams lpR = new FrameLayout.LayoutParams(rw, s, Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        lpR.rightMargin = pad + (it.type == Item.MEDIA ? Math.round(3 * dp) : 0);
        compact.addView(right, lpR);
    }

    private View leadingView(Item it, int s) {
        if (it.type == Item.CALL || it.type == Item.INCOMING) {
            FrameLayout f = new FrameLayout(this);
            f.setBackground(Widgets.round(0xFF2BC46A, s));
            f.addView(new Widgets.Glyph(this, Widgets.Glyph.PHONE, 0xFFFFFFFF));
            return f;
        }
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        if (it.art != null) {
            iv.setImageBitmap(it.art);
            iv.setBackground(Widgets.round(0xFF222222, s * (it.type == Item.MEDIA ? 0.5f : 0.3f)));
            iv.setClipToOutline(true);
        } else if (it.icon != null) {
            iv.setImageDrawable(it.icon);
        }
        return iv;
    }

    private void buildBubble(Item it) {
        bubble.removeAllViews();
        if (it == null) return;
        int pad = Math.round(5 * dp);
        int s = pillH - pad * 2;
        View v = it.type == Item.PROGRESS ? progressRing(it) : leadingView(it, s);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(s, s, Gravity.CENTER);
        bubble.addView(v, p);
    }

    private View progressRing(Item it) {
        Widgets.Ring ring = new Widgets.Ring(this, it.color);
        ring.setValue(it.indeterminate || it.max <= 0 ? -1 : it.progress / (float) it.max);
        return ring;
    }

    private String chrono(Item it) {
        long ms = it.countDown ? it.chronoBase - System.currentTimeMillis() : System.currentTimeMillis() - it.chronoBase;
        return fmt(Math.max(0, ms));
    }

    private static String fmt(long ms) {
        long s = ms / 1000;
        long hh = s / 3600, mm = (s / 60) % 60, ss = s % 60;
        return hh > 0 ? String.format("%d:%02d:%02d", hh, mm, ss) : String.format("%d:%02d", mm, ss);
    }

    // ------------------------------------------------------------------ expanded card

    private TextView text(float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setIncludeFontPadding(false);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }

    private int px(float v) { return Math.round(v * dp); }

    private void buildExpanded(final Item it) {
        expanded.removeAllViews();
        cardTickers.clear();
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(18), 0, px(18), px(16));
        expanded.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // header beside the camera
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        RectF card = cardRect(1);
        int leftSpace = Math.round(camCx - camD / 2f - card.left) - px(18) - px(10);
        if (it.icon != null) {
            ImageView ic = new ImageView(this);
            ic.setImageDrawable(it.icon);
            head.addView(ic, new LinearLayout.LayoutParams(px(16), px(16)));
        }
        TextView app = text(12, GREY, false);
        app.setText(it.app);
        app.setPadding(px(7), 0, 0, 0);
        app.setMaxWidth(Math.max(px(40), leftSpace - px(16)));
        head.addView(app);
        View spacer = new View(this);
        head.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        if (it.type == Item.MEDIA) {
            Widgets.Bars bars = new Widgets.Bars(this, it.color);
            bars.setPlaying(it.playing);
            head.addView(bars, new LinearLayout.LayoutParams(px(16), px(12)));
        } else if (!it.isLive()) {
            TextView when = text(11, GREY, false);
            when.setText(it.type == Item.INCOMING ? "chamada" : "agora");
            head.addView(when);
        }
        col.addView(head, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, pillH));

        if (it.type == Item.MEDIA) buildMedia(col, it);
        else buildMessage(col, it);
    }

    private ImageView bigIcon(Item it, int size, float radius) {
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setBackground(Widgets.round(0xFF1E1E22, radius));
        iv.setClipToOutline(true);
        if (it.art != null) iv.setImageBitmap(it.art);
        else if (it.large != null) iv.setImageDrawable(it.large);
        else if (it.icon != null) iv.setImageDrawable(it.icon);
        return iv;
    }

    private void buildMessage(LinearLayout col, final Item it) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int is = px(it.type == Item.INCOMING ? 52 : 46);
        View lead = (it.type == Item.CALL || it.type == Item.INCOMING) && it.art == null && it.large == null
                ? leadingView(it, is) : bigIcon(it, is, it.type == Item.INCOMING ? is / 2f : px(14));
        row.addView(lead, new LinearLayout.LayoutParams(is, is));

        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(px(12), 0, 0, 0);
        TextView title = text(15.5f, 0xFFFFFFFF, true);
        title.setText(it.title != null ? it.title : it.app);
        txt.addView(title);
        if (it.text != null) {
            TextView body = text(13.5f, 0xFFCFCFD4, false);
            body.setSingleLine(false);
            body.setMaxLines(it.isLive() ? 2 : 3);
            body.setLineSpacing(px(2), 1);
            body.setText(it.text);
            body.setPadding(0, px(4), 0, 0);
            txt.addView(body);
        }
        row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        if ((it.type == Item.TIMER || it.type == Item.CALL) && it.chronoBase > 0) {
            final TextView t = text(22, it.type == Item.CALL ? 0xFF4CE08A : it.color, true);
            t.setPadding(px(10), 0, 0, 0);
            Runnable r = new Runnable() { @Override public void run() { t.setText(chrono(it)); } };
            r.run();
            cardTickers.add(r);
            row.addView(t);
        }
        col.addView(row);

        if (it.type == Item.PROGRESS) {
            LinearLayout pr = new LinearLayout(this);
            pr.setGravity(Gravity.CENTER_VERTICAL);
            ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            bar.setIndeterminate(it.indeterminate || it.max <= 0);
            bar.setMax(Math.max(1, it.max));
            bar.setProgress(it.progress);
            bar.setProgressTintList(ColorStateList.valueOf(it.color));
            bar.setIndeterminateTintList(ColorStateList.valueOf(it.color));
            bar.setProgressBackgroundTintList(ColorStateList.valueOf(0x55FFFFFF));
            pr.addView(bar, new LinearLayout.LayoutParams(0, px(8), 1));
            if (!bar.isIndeterminate()) {
                TextView pc = text(12, GREY, true);
                pc.setText(Math.round(100f * it.progress / it.max) + "%");
                pc.setPadding(px(10), 0, 0, 0);
                pr.addView(pc);
            }
            LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            plp.topMargin = px(12);
            col.addView(pr, plp);
        }

        if (replying) {
            buildReply(col, it);
        } else if (it.actions != null && it.actions.length > 0) {
            buildActions(col, it);
        }
    }

    private void buildActions(LinearLayout col, final Item it) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int n = Math.min(3, it.actions.length);
        for (int i = 0; i < n; i++) {
            final Notification.Action a = it.actions[i];
            if (a == null || a.title == null) continue;
            int bg, fg;
            if (it.type == Item.INCOMING && n >= 2) {
                boolean answer = i == n - 1;
                bg = answer ? 0xFF2BC46A : 0xFFF0524F;
                fg = 0xFFFFFFFF;
            } else if (i == 0) {
                bg = it.color;
                fg = 0xFF101012;
            } else {
                bg = BTN;
                fg = 0xFFFFFFFF;
            }
            TextView b = text(13.5f, fg, true);
            b.setGravity(Gravity.CENTER);
            b.setText(a.title);
            b.setBackground(Widgets.round(bg, px(22)));
            b.setPadding(px(10), 0, px(10), 0);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { runAction(it, a); }
            });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, px(it.type == Item.INCOMING ? 48 : 42), 1);
            if (row.getChildCount() > 0) p.leftMargin = px(8);
            row.addView(b, p);
        }
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = px(14);
        col.addView(row, lp2);
    }

    private Notification.Action replyAction(Item it) {
        if (it.actions == null) return null;
        for (Notification.Action a : it.actions) {
            if (a != null && a.getRemoteInputs() != null && a.getRemoteInputs().length > 0) return a;
        }
        return null;
    }

    private void runAction(Item it, Notification.Action a) {
        if (a.getRemoteInputs() != null && a.getRemoteInputs().length > 0) {
            replying = true;
            h.removeCallbacks(autoClose);
            expandedKey = null;
            render();
            return;
        }
        try {
            a.actionIntent.send();
        } catch (Exception ignored) {
        }
        if (it == notif) notif = null;
        else userExpanded = false;
        render();
    }

    private void buildReply(LinearLayout col, final Item it) {
        final Notification.Action a = replyAction(it);
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        final EditText et = new EditText(this);
        et.setTextColor(0xFFFFFFFF);
        et.setHintTextColor(0xFF8A8A90);
        et.setHint(a != null && a.getRemoteInputs()[0].getLabel() != null ? a.getRemoteInputs()[0].getLabel() : "Responder");
        et.setTextSize(14);
        et.setBackground(Widgets.round(BTN, px(22)));
        et.setPadding(px(16), px(10), px(16), px(10));
        et.setMaxLines(3);
        et.setImeOptions(EditorInfo.IME_ACTION_SEND);
        et.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_CAP_SENTENCES | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE);
        row.addView(et, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        FrameLayout send = new FrameLayout(this);
        send.setBackground(Widgets.round(it.color, px(22)));
        send.addView(new Widgets.Glyph(this, Widgets.Glyph.SEND, 0xFF101012));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(px(44), px(44));
        sp.leftMargin = px(8);
        row.addView(send, sp);

        FrameLayout close = new FrameLayout(this);
        close.setBackground(Widgets.round(BTN, px(22)));
        close.addView(new Widgets.Glyph(this, Widgets.Glyph.CLOSE, 0xFFFFFFFF));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(px(44), px(44));
        cp.leftMargin = px(8);
        row.addView(close, cp);

        final Runnable doSend = new Runnable() {
            @Override public void run() {
                String msg = et.getText().toString().trim();
                if (msg.length() == 0 || a == null) return;
                Intent intent = new Intent().addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
                Bundle results = new Bundle();
                for (RemoteInput ri : a.getRemoteInputs()) results.putCharSequence(ri.getResultKey(), msg);
                RemoteInput.addResultsToIntent(a.getRemoteInputs(), intent, results);
                try {
                    a.actionIntent.send(IslandService.this, 0, intent);
                } catch (PendingIntent.CanceledException ignored) {
                }
                endReply(et, true);
            }
        };
        send.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { doSend.run(); }
        });
        close.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { endReply(et, false); }
        });
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) { doSend.run(); return true; }
                return false;
            }
        });
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = px(14);
        col.addView(row, lp2);

        h.postDelayed(new Runnable() {
            @Override public void run() {
                et.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
            }
        }, 250);
    }

    private void endReply(EditText et, boolean sent) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(et.getWindowToken(), 0);
        replying = false;
        expandedKey = null;
        if (sent) notif = null;
        else bumpAutoClose();
        render();
    }

    // ------------------------------------------------------------------ media card

    private void buildMedia(LinearLayout col, final Item it) {
        final MediaController mc = it.controller;
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        int is = px(58);
        row.addView(bigIcon(it, is, px(16)), new LinearLayout.LayoutParams(is, is));
        LinearLayout txt = new LinearLayout(this);
        txt.setOrientation(LinearLayout.VERTICAL);
        txt.setPadding(px(14), 0, 0, 0);
        TextView title = text(17, 0xFFFFFFFF, true);
        title.setText(it.title != null ? it.title : it.app);
        title.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        title.setMarqueeRepeatLimit(-1);
        title.setSelected(true);
        txt.addView(title);
        if (it.text != null) {
            TextView ar = text(14, 0xFFBDBDC3, false);
            ar.setText(it.text);
            ar.setPadding(0, px(4), 0, 0);
            txt.addView(ar);
        }
        row.addView(txt, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        col.addView(row);

        final SeekBar seek = new SeekBar(this);
        seek.setMax(Math.max(1, it.max));
        seek.setProgressTintList(ColorStateList.valueOf(it.color));
        seek.setProgressBackgroundTintList(ColorStateList.valueOf(0x44FFFFFF));
        seek.setThumbTintList(ColorStateList.valueOf(0xFFFFFFFF));
        seek.setPadding(px(6), 0, px(6), 0);
        seek.setEnabled(it.max > 0);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(28));
        sp.topMargin = px(10);
        sp.leftMargin = sp.rightMargin = -px(6);
        col.addView(seek, sp);

        LinearLayout times = new LinearLayout(this);
        final TextView el = text(11, GREY, false);
        final TextView rem = text(11, GREY, false);
        times.addView(el);
        times.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        times.addView(rem);
        col.addView(times);

        final boolean[] dragging = {false};
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean user) {
                if (user) el.setText(fmt(p));
            }
            @Override public void onStartTrackingTouch(SeekBar s) { dragging[0] = true; h.removeCallbacks(autoClose); }
            @Override public void onStopTrackingTouch(SeekBar s) {
                dragging[0] = false;
                if (mc != null) mc.getTransportControls().seekTo(s.getProgress());
                bumpAutoClose();
            }
        });
        Runnable r = new Runnable() {
            @Override public void run() {
                if (dragging[0] || mc == null) return;
                long pos = position(mc);
                seek.setProgress((int) pos);
                el.setText(fmt(pos));
                rem.setText(it.max > 0 ? "-" + fmt(Math.max(0, it.max - pos)) : "");
            }
        };
        r.run();
        cardTickers.add(r);

        LinearLayout ctl = new LinearLayout(this);
        ctl.setGravity(Gravity.CENTER_VERTICAL);
        final Widgets.Glyph pp = new Widgets.Glyph(this, it.playing ? Widgets.Glyph.PAUSE : Widgets.Glyph.PLAY, 0xFF101012);
        ctl.addView(button(Widgets.Glyph.PREV, BTN, 0xFFFFFFFF, null, new Runnable() {
            @Override public void run() { if (mc != null) mc.getTransportControls().skipToPrevious(); }
        }), new LinearLayout.LayoutParams(0, px(50), 1));
        LinearLayout.LayoutParams mp = new LinearLayout.LayoutParams(0, px(50), 1.5f);
        mp.leftMargin = mp.rightMargin = px(10);
        ctl.addView(button(-1, it.color, 0, pp, new Runnable() {
            @Override public void run() {
                if (mc == null) return;
                PlaybackState st = mc.getPlaybackState();
                boolean playing = st != null && st.getState() == PlaybackState.STATE_PLAYING;
                if (playing) mc.getTransportControls().pause(); else mc.getTransportControls().play();
                pp.setKind(playing ? Widgets.Glyph.PLAY : Widgets.Glyph.PAUSE);
            }
        }), mp);
        ctl.addView(button(Widgets.Glyph.NEXT, BTN, 0xFFFFFFFF, null, new Runnable() {
            @Override public void run() { if (mc != null) mc.getTransportControls().skipToNext(); }
        }), new LinearLayout.LayoutParams(0, px(50), 1));
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        cl.topMargin = px(6);
        col.addView(ctl, cl);
    }

    private View button(int glyph, int bg, int fg, Widgets.Glyph custom, final Runnable action) {
        FrameLayout f = new FrameLayout(this);
        f.setBackground(Widgets.round(bg, px(25)));
        f.addView(custom != null ? custom : new Widgets.Glyph(this, glyph, fg));
        f.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                action.run();
                bumpAutoClose();
            }
        });
        return f;
    }

    private static long position(MediaController mc) {
        PlaybackState st = mc.getPlaybackState();
        if (st == null) return 0;
        long pos = st.getPosition();
        if (st.getState() == PlaybackState.STATE_PLAYING) {
            pos += (long) ((SystemClock.elapsedRealtime() - st.getLastPositionUpdateTime()) * st.getPlaybackSpeed());
        }
        return Math.max(0, pos);
    }

    private void open(Item it) {
        try {
            if (it.contentIntent != null) {
                it.contentIntent.send();
            } else {
                Intent i = getPackageManager().getLaunchIntentForPackage(it.pkg);
                if (i != null) startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        } catch (Exception ignored) {
        }
    }
}
