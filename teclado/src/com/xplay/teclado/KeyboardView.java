package com.xplay.teclado;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Draws the keys and turns touches into key presses. Layouts are plain strings, ten units wide. */
final class KeyboardView extends View {
    interface Listener {
        void onKey(int code);
        void onText(String text);
        void onCursor(int steps);
        void onLongSpace();
    }

    static final int SHIFT = -1, DELETE = -2, SYMBOLS = -3, LETTERS = -4, MORE_SYMBOLS = -5, ENTER = -6, EMOJI = -7;
    static final int LAYOUT_LETTERS = 0, LAYOUT_SYMBOLS = 1, LAYOUT_MORE = 2;

    private static final String[] TOP_ROW_DIGITS = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};

    static final class Key {
        final int code;
        final String label;
        final float width;
        String alts;
        String hint;
        final RectF rect = new RectF();

        Key(int code, String label, float width) {
            this.code = code;
            this.label = label;
            this.width = width;
        }

        boolean special() { return code < 0 || code == '\n'; }
    }

    private final Listener listener;
    private final Handler handler = new Handler();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float dp;
    private final Drawable icShift, icDelete, icEnter, icEmoji;

    private final List<List<Key>> rows = new ArrayList<>();
    private Themes.Theme theme = Themes.get(0);
    private float scale = 1f;
    private int layout = LAYOUT_LETTERS;
    private boolean numberRow;
    private int shift; // 0 off, 1 next letter, 2 caps lock
    private long lastShiftTap;
    private boolean vibrate = true;

    private Key pressed;
    private boolean longPressed;
    private float downX;
    private int cursorSteps;
    private List<String> popup;
    private final RectF popupRect = new RectF();
    private int popupSel;

    private final Runnable longPress = new Runnable() {
        @Override
        public void run() {
            Key k = pressed;
            if (k == null) return;
            if (k.code == DELETE) {
                longPressed = true;
                repeatDelete.run();
            } else if (k.code == ' ' && cursorSteps == 0) {
                longPressed = true;
                pressed = null;
                invalidate();
                listener.onLongSpace();
            } else if (k.alts != null) {
                longPressed = true;
                openPopup(k);
            }
        }
    };

    private final Runnable repeatDelete = new Runnable() {
        @Override
        public void run() {
            if (pressed == null || pressed.code != DELETE) return;
            listener.onKey(DELETE);
            handler.postDelayed(this, 55);
        }
    };

    KeyboardView(Context c, Listener listener) {
        super(c);
        this.listener = listener;
        dp = c.getResources().getDisplayMetrics().density;
        icShift = c.getResources().getDrawable(R.drawable.ic_shift).mutate();
        icDelete = c.getResources().getDrawable(R.drawable.ic_backspace).mutate();
        icEnter = c.getResources().getDrawable(R.drawable.ic_enter).mutate();
        icEmoji = c.getResources().getDrawable(R.drawable.ic_emoji).mutate();
        build();
    }

    // ---- State set by the service.

    void setTheme(Themes.Theme t) { theme = t; invalidate(); }

    void setScale(float s) {
        scale = s;
        requestLayout();
        invalidate();
    }

    void setVibrate(boolean on) { vibrate = on; }

    void setNumberRow(boolean on) {
        if (numberRow == on) return;
        numberRow = on;
        build();
    }

    void setLayout(int l) {
        if (layout == l) return;
        layout = l;
        build();
    }

    int layout() { return layout; }

    int shift() { return shift; }

    void setShift(int s) {
        if (shift == s) return;
        shift = s;
        invalidate();
    }

    /** Height for a given scale, so panels can match the keyboard exactly. */
    int heightFor(float s) {
        return Math.round(rowsCount() * 52 * dp * s + 8 * dp);
    }

    private int rowsCount() {
        return layout == LAYOUT_LETTERS && numberRow ? 5 : 4;
    }

    // ---- Layouts.

    private void build() {
        rows.clear();
        if (layout == LAYOUT_LETTERS) {
            if (numberRow) rows.add(chars("1234567890", null));
            List<Key> r1 = chars("qwertyuiop", null);
            if (!numberRow) {
                for (int i = 0; i < r1.size(); i++) {
                    Key k = r1.get(i);
                    k.hint = TOP_ROW_DIGITS[i];
                    k.alts = TOP_ROW_DIGITS[i] + (k.alts == null ? "" : k.alts);
                }
            }
            rows.add(r1);
            rows.add(chars("asdfghjkl", null));
            List<Key> r3 = new ArrayList<>();
            r3.add(new Key(SHIFT, "", 1.5f));
            r3.addAll(chars("zxcvbnm", null));
            r3.add(new Key(DELETE, "", 1.5f));
            rows.add(r3);
        } else if (layout == LAYOUT_SYMBOLS) {
            rows.add(chars("1234567890", null));
            rows.add(chars("@#$%&-+()/", null));
            List<Key> r3 = new ArrayList<>();
            r3.add(new Key(MORE_SYMBOLS, "=\\<", 1.5f));
            r3.addAll(chars("*\"':;!?", null));
            r3.add(new Key(DELETE, "", 1.5f));
            rows.add(r3);
        } else {
            rows.add(chars("~`|•√π÷×¶∆", null));
            rows.add(chars("£€¥^°={}[]", null));
            List<Key> r3 = new ArrayList<>();
            r3.add(new Key(SYMBOLS, "?123", 1.5f));
            r3.addAll(chars("\\©®™_<>", null));
            r3.add(new Key(DELETE, "", 1.5f));
            rows.add(r3);
        }
        List<Key> bottom = new ArrayList<>();
        bottom.add(layout == LAYOUT_LETTERS ? new Key(SYMBOLS, "?123", 1.5f) : new Key(LETTERS, "ABC", 1.5f));
        bottom.add(new Key(EMOJI, "", 1f));
        Key comma = new Key(',', ",", 1f);
        comma.alts = ",;:";
        bottom.add(comma);
        bottom.add(new Key(' ', "", 4f));
        Key dot = new Key('.', ".", 1f);
        dot.alts = ".!?…-@";
        bottom.add(dot);
        bottom.add(new Key(ENTER, "", 1.5f));
        rows.add(bottom);
        placeKeys();
        requestLayout();
        invalidate();
    }

    private static List<Key> chars(String s, String unused) {
        List<Key> row = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            Key k = new Key(ch, String.valueOf(ch), 1f);
            k.alts = alternates(ch);
            row.add(k);
        }
        return row;
    }

    private static String alternates(char ch) {
        switch (ch) {
            case 'a': return "áàâãäª";
            case 'e': return "éèêë€";
            case 'i': return "íìîï";
            case 'o': return "óòôõöº";
            case 'u': return "úùûü";
            case 'c': return "ç©";
            case 'n': return "ñ";
            case 's': return "ß$";
            case 'y': return "ý";
            case '-': return "_–—";
            case '$': return "€£¥¢";
            case '?': return "¿";
            case '!': return "¡";
            case '"': return "“”«»";
            case '\'': return "‘’";
            case '0': return "°";
            case '1': return "¹½";
            case '2': return "²";
            case '3': return "³";
            default: return null;
        }
    }

    // ---- Measure and draw.

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), heightFor(scale));
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        placeKeys();
    }

    private void placeKeys() {
        int w = getWidth();
        if (w == 0) return;
        float unit = w / 10f;
        float keyH = 52 * dp * scale;
        float top = 4 * dp;
        for (List<Key> row : rows) {
            float total = 0;
            for (Key k : row) total += k.width;
            float x = (10 - total) / 2 * unit;
            for (Key k : row) {
                k.rect.set(x, top, x + k.width * unit, top + keyH);
                x += k.width * unit;
            }
            top += keyH;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        c.drawColor(theme.bg);
        float gapX = 3 * dp, gapY = 4 * dp, radius = 7 * dp;
        RectF r = new RectF();
        for (List<Key> row : rows) {
            for (Key k : row) {
                r.set(k.rect.left + gapX, k.rect.top + gapY, k.rect.right - gapX, k.rect.bottom - gapY);
                boolean active = k.code == SHIFT && shift > 0;
                paint.setColor(k == pressed ? theme.pressed
                        : k.code == ENTER || active ? theme.accent
                        : k.special() ? theme.special : theme.key);
                c.drawRoundRect(r, radius, radius, paint);
                drawLabel(c, k, r);
            }
        }
        if (popup != null) drawPopup(c);
    }

    private void drawLabel(Canvas c, Key k, RectF r) {
        int fg = k.code == ENTER ? 0xFFFFFFFF : theme.keyText;
        switch (k.code) {
            case SHIFT:
                drawIcon(c, icShift, r, shift > 0 ? 0xFFFFFFFF : fg);
                if (shift == 2) {
                    paint.setColor(0xFFFFFFFF);
                    float cx = r.centerX(), y = r.centerY() + 13 * dp * Math.min(scale, 1f);
                    c.drawRect(cx - 7 * dp, y, cx + 7 * dp, y + 2 * dp, paint);
                }
                return;
            case DELETE: drawIcon(c, icDelete, r, fg); return;
            case ENTER: drawIcon(c, icEnter, r, fg); return;
            case EMOJI: drawIcon(c, icEmoji, r, fg); return;
            case ' ':
                paint.setColor(theme.hint);
                paint.setTextSize(12 * dp);
                paint.setTypeface(Typeface.DEFAULT);
                paint.setTextAlign(Paint.Align.CENTER);
                c.drawText("Teclado Xplay", r.centerX(), r.centerY() + 4 * dp, paint);
                return;
            default:
                break;
        }
        String label = shown(k.label);
        boolean small = label.length() > 1;
        paint.setColor(fg);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(small ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        paint.setTextSize((small ? 14 : 22) * dp * Math.min(scale, 1.15f));
        c.drawText(label, r.centerX(), r.centerY() - (paint.descent() + paint.ascent()) / 2, paint);
        if (k.hint != null) {
            paint.setColor(theme.hint);
            paint.setTextSize(10 * dp * Math.min(scale, 1.15f));
            paint.setTextAlign(Paint.Align.RIGHT);
            c.drawText(k.hint, r.right - 4 * dp, r.top + 12 * dp, paint);
        }
    }

    private void drawIcon(Canvas c, Drawable d, RectF r, int color) {
        int s = Math.round(22 * dp * Math.min(scale, 1.15f));
        int cx = Math.round(r.centerX()), cy = Math.round(r.centerY());
        d.setBounds(cx - s / 2, cy - s / 2, cx + s / 2, cy + s / 2);
        d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        d.draw(c);
    }

    private void drawPopup(Canvas c) {
        float radius = 8 * dp;
        paint.setColor(0x55000000);
        RectF shadow = new RectF(popupRect);
        shadow.offset(0, 2 * dp);
        c.drawRoundRect(shadow, radius, radius, paint);
        paint.setColor(theme.special);
        c.drawRoundRect(popupRect, radius, radius, paint);
        float cell = popupRect.width() / popup.size();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setTextSize(22 * dp * Math.min(scale, 1.15f));
        for (int i = 0; i < popup.size(); i++) {
            RectF cr = new RectF(popupRect.left + i * cell, popupRect.top, popupRect.left + (i + 1) * cell, popupRect.bottom);
            if (i == popupSel) {
                paint.setColor(theme.accent);
                c.drawRoundRect(cr, radius, radius, paint);
            }
            paint.setColor(i == popupSel ? 0xFFFFFFFF : theme.keyText);
            c.drawText(popup.get(i), cr.centerX(), cr.centerY() - (paint.descent() + paint.ascent()) / 2, paint);
        }
    }

    private String shown(String label) {
        if (layout != LAYOUT_LETTERS || shift == 0 || label.length() != 1) return label;
        return label.toUpperCase();
    }

    // ---- Touch.

    private Key keyAt(float x, float y) {
        if (rows.isEmpty()) return null;
        List<Key> row = rows.get(rows.size() - 1);
        for (List<Key> r : rows) {
            if (y < r.get(0).rect.bottom) { row = r; break; }
        }
        Key best = null;
        float bestDist = Float.MAX_VALUE;
        for (Key k : row) {
            if (x >= k.rect.left && x < k.rect.right) return k;
            float d = Math.min(Math.abs(x - k.rect.left), Math.abs(x - k.rect.right));
            if (d < bestDist) { bestDist = d; best = k; }
        }
        return best;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        int idx = e.getActionIndex();
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                // A second finger types the first key right away, so fast two-thumb typing keeps up.
                if (pressed != null && !longPressed) release(false);
                press(keyAt(e.getX(idx), e.getY(idx)), e.getX(idx));
                return true;
            case MotionEvent.ACTION_MOVE:
                if (pressed == null) return true;
                float x = e.getX(e.getPointerCount() - 1);
                if (popup != null) {
                    float cell = popupRect.width() / popup.size();
                    int sel = (int) Math.floor((x - popupRect.left) / cell);
                    sel = Math.max(0, Math.min(popup.size() - 1, sel));
                    if (sel != popupSel) { popupSel = sel; invalidate(); }
                } else if (pressed.code == ' ') {
                    int steps = (int) ((x - downX) / (14 * dp));
                    if (steps != cursorSteps) {
                        handler.removeCallbacks(longPress);
                        listener.onCursor(steps - cursorSteps);
                        cursorSteps = steps;
                    }
                }
                return true;
            case MotionEvent.ACTION_UP:
                if (pressed != null) release(true);
                return true;
            case MotionEvent.ACTION_CANCEL:
                cancel();
                return true;
            default:
                return true;
        }
    }

    private void press(Key k, float x) {
        pressed = k;
        longPressed = false;
        cursorSteps = 0;
        downX = x;
        if (k == null) return;
        if (vibrate) performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (k.code == DELETE) listener.onKey(DELETE);
        handler.postDelayed(longPress, k.code == DELETE ? 400 : 330);
        invalidate();
    }

    private void release(boolean up) {
        Key k = pressed;
        handler.removeCallbacks(longPress);
        handler.removeCallbacks(repeatDelete);
        if (popup != null) {
            String s = popup.get(popupSel);
            closePopup();
            pressed = null;
            listener.onText(s);
            invalidate();
            return;
        }
        pressed = null;
        invalidate();
        if (k == null || longPressed || cursorSteps != 0 || k.code == DELETE) return;
        fire(k);
    }

    void cancel() {
        handler.removeCallbacks(longPress);
        handler.removeCallbacks(repeatDelete);
        closePopup();
        pressed = null;
        invalidate();
    }

    private void fire(Key k) {
        switch (k.code) {
            case SHIFT:
                long now = System.currentTimeMillis();
                if (shift == 0) shift = now - lastShiftTap < 350 ? 2 : 1;
                else if (shift == 1 && now - lastShiftTap < 350) shift = 2;
                else shift = 0;
                lastShiftTap = now;
                invalidate();
                return;
            case SYMBOLS: setLayout(LAYOUT_SYMBOLS); return;
            case MORE_SYMBOLS: setLayout(LAYOUT_MORE); return;
            case LETTERS: setLayout(LAYOUT_LETTERS); return;
            case ENTER:
            case EMOJI:
            case ' ':
                listener.onKey(k.code == ENTER ? '\n' : k.code);
                return;
            default:
                listener.onText(shown(k.label));
        }
    }

    private void openPopup(Key k) {
        List<String> items = new ArrayList<>();
        String base = shown(k.label);
        String alts = k.alts;
        if (layout == LAYOUT_LETTERS && shift > 0) alts = alts.toUpperCase();
        for (int i = 0; i < alts.length(); i++) items.add(String.valueOf(alts.charAt(i)));
        if (!items.contains(base) && k.code != '.' && k.code != ',') items.add(base);
        float cell = k.rect.width();
        float w = cell * items.size();
        float left = Math.max(2 * dp, Math.min(getWidth() - w - 2 * dp, k.rect.centerX() - cell / 2));
        float h = k.rect.height();
        float top = k.rect.top - h + 2 * dp;
        if (top < 0) top = k.rect.top;
        popupRect.set(left, top, left + w, top + h - 4 * dp);
        popup = items;
        popupSel = Math.max(0, Math.min(items.size() - 1, (int) ((k.rect.centerX() - left) / cell)));
        if (vibrate) performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        invalidate();
    }

    private void closePopup() {
        popup = null;
    }
}
