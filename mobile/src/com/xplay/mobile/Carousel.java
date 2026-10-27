package com.xplay.mobile;

import android.app.Activity;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Highlights at the top of the home: big poster cards that snap to the center, with the neighbours
 * peeking at the sides, page dots below, and an automatic turn every few seconds.
 */
public final class Carousel extends LinearLayout {

    private static final long AUTO_MS = 7000;

    private final Activity activity;
    private final Pager pager;
    private final LinearLayout strip;
    private final LinearLayout dots;
    private final List<View> cards = new ArrayList<View>();
    private final int itemWidth, gap;
    private int page;
    private final Runnable auto = new Runnable() {
        public void run() {
            if (cards.size() > 1 && isShown()) goTo((page + 1) % cards.size(), true);
            postDelayed(this, AUTO_MS);
        }
    };

    public Carousel(Activity a, List<VodItem> items) {
        super(a);
        activity = a;
        setOrientation(VERTICAL);
        int screen = Ui.screenWidth(a);
        boolean land = Ui.landscape(a);
        int screenH = a.getResources().getDisplayMetrics().heightPixels;
        itemWidth = land ? Math.min((int) (screen * 0.3f), (int) (screenH * 0.62f / 1.42f)) : (int) (screen * 0.665f);
        gap = Ui.dp(a, 14);
        int itemHeight = (int) (itemWidth * 1.42f);

        pager = new Pager(a);
        pager.setHorizontalScrollBarEnabled(false);
        pager.setOverScrollMode(OVER_SCROLL_NEVER);
        strip = Ui.row(a);
        int side = (screen - itemWidth) / 2;
        strip.setPadding(side, 0, side, 0);
        pager.addView(strip);
        addView(pager, Ui.lp(-1, itemHeight));

        for (VodItem v : items) {
            View c = card(v, itemWidth, itemHeight);
            LinearLayout.LayoutParams l = Ui.lp(itemWidth, itemHeight);
            l.setMargins(cards.isEmpty() ? 0 : gap, 0, 0, 0);
            strip.addView(c, l);
            cards.add(c);
        }

        dots = Ui.row(a);
        dots.setGravity(Gravity.CENTER);
        for (int i = 0; i < cards.size(); i++) {
            View d = new View(a);
            dots.addView(d);
        }
        addView(dots, Ui.lp(-1, -2, 0, Ui.dp(a, 18), 0, 0));
        updateDots();
        pager.post(new Runnable() {
            public void run() { applyScale(); }
        });
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        removeCallbacks(auto);
        postDelayed(auto, AUTO_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        removeCallbacks(auto);
    }

    private View card(final VodItem v, int w, int h) {
        final FrameLayout f = new FrameLayout(activity);
        f.setClickable(true);
        f.setFocusable(true);
        f.setBackground(Ui.stroke(Theme.SURFACE_HI, Ui.dp(activity, 22), Ui.dp(activity, 1), 0x33FFFFFF));
        Ui.clipRound(f, Ui.dp(activity, 22));
        ImageView img = new ImageView(activity);
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        f.addView(img, Ui.frame(-1, -1, Gravity.CENTER));
        ImageLoader.load(v.poster, img, w, 0);

        View shade = new View(activity);
        shade.setBackground(Ui.vertical(0x00000000, 0x00000000, 0x55000000, 0xE6000000, 0xF5000000));
        f.addView(shade, Ui.frame(-1, -1, Gravity.CENTER));

        LinearLayout info = Ui.column(activity);
        info.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = Ui.dp(activity, 14);
        info.setPadding(p, 0, p, Ui.dp(activity, 18));
        TextView title = Ui.text(activity, v.cleanName().toUpperCase(new java.util.Locale("pt", "BR")), 30, Theme.TEXT,
                Typeface.create("sans-serif-condensed", Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        title.setMaxLines(2);
        title.setEllipsize(android.text.TextUtils.TruncateAt.END);
        title.setLetterSpacing(-0.01f);
        title.setShadowLayer(12, 0, 2, 0xCC000000);
        title.setTextColor(Theme.PEACH);
        info.addView(title, Ui.lp(-1, -2));
        final TextView genres = Ui.oneLine(Ui.text(activity, "", 15, 0xFFD8D8DC));
        genres.setGravity(Gravity.CENTER);
        info.addView(genres, Ui.lp(-1, -2, 0, Ui.dp(activity, 14), 0, 0));
        final LinearLayout meta = Ui.row(activity);
        meta.setGravity(Gravity.CENTER);
        info.addView(meta, Ui.lp(-1, -2, 0, Ui.dp(activity, 10), 0, 0));
        f.addView(info, Ui.frame(-1, -2, Gravity.BOTTOM));

        fillMeta(v, InfoCache.peek(v), genres, meta);
        InfoCache.get(v, new InfoCache.Callback() {
            public void done(JSONObject answer) { fillMeta(v, answer, genres, meta); }
        });
        f.setOnClickListener(new OnClickListener() {
            public void onClick(View x) {
                int i = cards.indexOf(f);
                if (i != page) goTo(i, true);
                else Cards.openDetail(activity, v);
            }
        });
        return f;
    }

    private void fillMeta(VodItem v, JSONObject answer, TextView genres, LinearLayout meta) {
        List<String> g = Meta.genres(answer);
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < g.size() && i < 3; i++) s.append(i == 0 ? "" : "  •  ").append(g.get(i));
        genres.setText(s.length() > 0 ? s : (v.series ? "Série" : "Filme"));
        meta.removeAllViews();
        String date = Meta.shortDate(answer, v);
        if (!date.isEmpty()) meta.addView(Ui.text(activity, date, 15, 0xFFB8B8BE));
        double rating = Meta.rating(answer, v);
        if (rating > 0) {
            TextView imdb = Ui.text(activity, "IMDb", 12, 0xFF000000, Ui.BLACK);
            imdb.setBackground(Ui.round(Theme.IMDB, Ui.dp(activity, 4)));
            imdb.setPadding(Ui.dp(activity, 5), 0, Ui.dp(activity, 5), 0);
            meta.addView(imdb, Ui.lp(-2, -2, Ui.dp(activity, 14), 0, 0, 0));
            meta.addView(Ui.text(activity, String.format(java.util.Locale.US, "%.1f", rating), 16, Theme.TEXT, Ui.BOLD),
                    Ui.lp(-2, -2, Ui.dp(activity, 8), 0, 0, 0));
        }
    }

    private int pageAt(int scrollX) {
        int step = itemWidth + gap;
        return Math.max(0, Math.min(cards.size() - 1, Math.round(scrollX / (float) step)));
    }

    void goTo(int p, boolean smooth) {
        page = Math.max(0, Math.min(cards.size() - 1, p));
        int x = page * (itemWidth + gap);
        if (smooth) pager.smoothScrollTo(x, 0);
        else pager.scrollTo(x, 0);
        updateDots();
    }

    private void updateDots() {
        for (int i = 0; i < dots.getChildCount(); i++) {
            View d = dots.getChildAt(i);
            boolean on = i == page;
            int h = Ui.dp(activity, 6);
            LinearLayout.LayoutParams l = Ui.lp(on ? Ui.dp(activity, 26) : h, h);
            l.setMargins(Ui.dp(activity, 3), 0, Ui.dp(activity, 3), 0);
            d.setLayoutParams(l);
            d.setBackground(Ui.round(on ? 0xFFFFFFFF : 0x55FFFFFF, h));
        }
    }

    private void applyScale() {
        float center = pager.getScrollX() + pager.getWidth() / 2f;
        for (View c : cards) {
            float cc = c.getLeft() + c.getWidth() / 2f;
            float d = Math.min(1f, Math.abs(cc - center) / (itemWidth + gap));
            float s = 1f - 0.12f * d;
            c.setScaleX(s);
            c.setScaleY(s);
            c.setAlpha(1f - 0.35f * d);
        }
    }

    /** Scroll view that snaps one card at a time. */
    private final class Pager extends HorizontalScrollView {
        private boolean flung;
        private int downPage;

        Pager(Activity a) {
            super(a);
        }

        @Override
        public boolean onTouchEvent(MotionEvent ev) {
            int action = ev.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN) {
                downPage = page;
                removeCallbacks(auto);
            }
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                flung = false;
                boolean r = super.onTouchEvent(ev);
                if (!flung) goTo(pageAt(getScrollX()), true);
                Carousel.this.postDelayed(auto, AUTO_MS);
                return r;
            }
            return super.onTouchEvent(ev);
        }

        @Override
        public void fling(int velocityX) {
            flung = true;
            int dir = velocityX > 0 ? 1 : -1;
            int target = pageAt(getScrollX());
            // A quick swipe always moves one card, even a short one.
            if (target == downPage) target = downPage + dir;
            goTo(target, true);
        }

        @Override
        protected void onScrollChanged(int l, int t, int ol, int ot) {
            super.onScrollChanged(l, t, ol, ot);
            applyScale();
            int p = pageAt(l);
            if (p != page && !isPressed()) {
                // Keep the dots in step while dragging.
                page = p;
                updateDots();
            }
        }
    }
}
