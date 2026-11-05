package com.listo.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/** A month grid, Sunday first: 6 rows of 7 days, with the selected day in a circle and a dot on busy days. */
public class MonthView extends View {

    public interface Listener {
        void onDaySelected(int y, int m, int d);
        void onMonthSwipe(int delta);
    }

    private static final String[] WEEK = {"Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"};

    public static final int ACCENT = 0xFF7B9EEA;

    private int year, month;
    private int selY, selM, selD;
    private Set<String> busy = new HashSet<String>();
    private Listener listener;

    private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private float downX, downY;

    public MonthView(Context c) {
        super(c);
        dp = c.getResources().getDisplayMetrics().density;
        text.setTextAlign(Paint.Align.CENTER);
    }

    public void setListener(Listener l) {
        listener = l;
    }

    public void show(int y, int m, int selY, int selM, int selD, Set<String> busy) {
        this.year = y;
        this.month = m;
        this.selY = selY;
        this.selM = selM;
        this.selD = selD;
        this.busy = busy;
        invalidate();
    }

    private float headH() { return 40 * dp; }
    private float rowH() { return 60 * dp; }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        setMeasuredDimension(w, (int) (headH() + rowH() * 6 + 4 * dp));
    }

    /** Calendar positioned at the first visible cell (a Sunday on or before the 1st). */
    private Calendar firstCell() {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month, 1);
        c.add(Calendar.DAY_OF_MONTH, -(c.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY));
        return c;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cw = getWidth() / 7f;
        text.setTypeface(Typeface.DEFAULT);
        text.setTextSize(14 * dp);
        text.setColor(0xFF9AA0AB);
        for (int i = 0; i < 7; i++) {
            canvas.drawText(WEEK[i], cw * i + cw / 2, headH() / 2 + 5 * dp, text);
        }

        Calendar today = Calendar.getInstance();
        int ty = today.get(Calendar.YEAR), tm = today.get(Calendar.MONTH), td = today.get(Calendar.DAY_OF_MONTH);

        Calendar c = firstCell();
        text.setTextSize(16 * dp);
        for (int i = 0; i < 42; i++) {
            int y = c.get(Calendar.YEAR), m = c.get(Calendar.MONTH), d = c.get(Calendar.DAY_OF_MONTH);
            float cx = cw * (i % 7) + cw / 2;
            float cy = headH() + rowH() * (i / 7) + rowH() / 2;
            boolean inMonth = m == month;
            boolean sel = y == selY && m == selM && d == selD;
            boolean isToday = y == ty && m == tm && d == td;

            if (sel) {
                fill.setColor(ACCENT);
                canvas.drawCircle(cx, cy, 20 * dp, fill);
            } else if (isToday) {
                fill.setColor(0x337B9EEA);
                canvas.drawCircle(cx, cy, 20 * dp, fill);
            }
            if (sel) text.setColor(0xFFFFFFFF);
            else if (isToday) text.setColor(0xFF4A6FD8);
            else text.setColor(inMonth ? 0xFF222222 : 0xFFB8BCC4);
            text.setTypeface(sel || isToday ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            canvas.drawText(String.valueOf(d), cx, cy + 5.5f * dp, text);

            if (busy.contains(Store.key(y, m, d))) {
                fill.setColor(sel ? 0xFFFFFFFF : (inMonth ? ACCENT : 0x887B9EEA));
                canvas.drawCircle(cx, cy + (sel ? 12 : 26) * dp, 3 * dp, fill);
            }
            c.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                return true;
            case MotionEvent.ACTION_UP:
                float dx = e.getX() - downX, dy = e.getY() - downY;
                if (Math.abs(dx) > 60 * dp && Math.abs(dx) > Math.abs(dy)) {
                    if (listener != null) listener.onMonthSwipe(dx < 0 ? 1 : -1);
                    return true;
                }
                if (Math.abs(dx) < 12 * dp && Math.abs(dy) < 12 * dp && e.getY() > headH()) {
                    int col = (int) (e.getX() / (getWidth() / 7f));
                    int row = (int) ((e.getY() - headH()) / rowH());
                    if (row > 5) row = 5;
                    Calendar c = firstCell();
                    c.add(Calendar.DAY_OF_MONTH, row * 7 + col);
                    if (listener != null) {
                        playSoundEffect(android.view.SoundEffectConstants.CLICK);
                        listener.onDaySelected(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                    }
                }
                return true;
        }
        return true;
    }
}
