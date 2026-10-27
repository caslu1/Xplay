package com.xplay.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

/** Dark rounded dialogs: option lists (bottom sheet on phones), confirmations and text prompts. */
public final class Sheet {

    public interface Pick {
        void picked(int index);
    }

    public interface Input {
        void entered(String text);
    }

    private Sheet() {}

    /** A dialog holding content; bottom-anchored in portrait, centered in landscape. */
    public static Dialog show(Activity a, View content) {
        final Dialog d = new Dialog(a, android.R.style.Theme_Material_Dialog_NoActionBar);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        boolean land = Ui.landscape(a);
        FrameLayout box = new FrameLayout(a);
        float r = Ui.dp(a, 22);
        if (land) {
            box.setBackground(Ui.round(Theme.SURFACE, r));
        } else {
            android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
            g.setColor(Theme.SURFACE);
            g.setCornerRadii(new float[] {r, r, r, r, 0, 0, 0, 0});
            box.setBackground(g);
        }
        box.addView(content);
        d.setContentView(box);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            WindowManager.LayoutParams p = w.getAttributes();
            if (land) {
                p.width = Math.min(Ui.dp(a, 420), (int) (Ui.screenWidth(a) * 0.8f));
                p.gravity = Gravity.CENTER;
            } else {
                p.width = ViewGroup.LayoutParams.MATCH_PARENT;
                p.gravity = Gravity.BOTTOM;
            }
            w.setAttributes(p);
            w.setDimAmount(0.6f);
        }
        d.show();
        return d;
    }

    /** Options with a check mark on selected (-1 for none). */
    public static Dialog options(Activity a, String title, List<String> items, int selected, final Pick pick) {
        LinearLayout col = Ui.column(a);
        int pad = Ui.dp(a, 8);
        col.setPadding(0, Ui.dp(a, 18), 0, Ui.dp(a, 12));
        TextView t = Ui.text(a, title, 18, Theme.TEXT, Ui.BOLD);
        t.setPadding(Ui.dp(a, 24), 0, Ui.dp(a, 24), Ui.dp(a, 10));
        col.addView(t);
        LinearLayout list = Ui.column(a);
        list.setPadding(pad, 0, pad, 0);
        ScrollView scroll = new ScrollView(a);
        scroll.addView(list);
        int max = (int) (a.getResources().getDisplayMetrics().heightPixels * (Ui.landscape(a) ? 0.62f : 0.55f));
        col.addView(scroll, Ui.lp(-1, -2));
        final Dialog[] d = new Dialog[1];
        for (int i = 0; i < items.size(); i++) {
            final int index = i;
            LinearLayout row = Ui.row(a);
            row.setPadding(Ui.dp(a, 16), Ui.dp(a, 14), Ui.dp(a, 16), Ui.dp(a, 14));
            row.setBackground(Ui.pressable(a, 0, Ui.dp(a, 14)));
            row.setClickable(true);
            row.setFocusable(true);
            TextView label = Ui.text(a, items.get(i), 16, i == selected ? Theme.PINK : Theme.TEXT,
                    i == selected ? Ui.MEDIUM : null);
            row.addView(label, Ui.weight(1));
            if (i == selected) row.addView(Ui.icon(a, R.drawable.ic_check, Theme.PINK), Ui.lp(Ui.dp(a, 22), Ui.dp(a, 22)));
            row.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    d[0].dismiss();
                    pick.picked(index);
                }
            });
            list.addView(row, Ui.lp(-1, -2));
        }
        d[0] = show(a, col);
        // Cap the height so long lists scroll inside the sheet.
        scroll.measure(View.MeasureSpec.makeMeasureSpec(Ui.screenWidth(a), View.MeasureSpec.AT_MOST),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        if (scroll.getMeasuredHeight() > max) scroll.getLayoutParams().height = max;
        if (selected > 2) {
            final ScrollView s = scroll;
            final int sel = selected;
            final LinearLayout l = list;
            scroll.post(new Runnable() {
                public void run() {
                    View v = l.getChildAt(sel);
                    if (v != null) s.scrollTo(0, Math.max(0, v.getTop() - s.getHeight() / 3));
                }
            });
        }
        return d[0];
    }

    public static void confirm(Activity a, String title, String message, String yes, final Runnable onYes) {
        LinearLayout col = Ui.column(a);
        int p = Ui.dp(a, 24);
        col.setPadding(p, p, p, Ui.dp(a, 16));
        col.addView(Ui.text(a, title, 19, Theme.TEXT, Ui.BOLD));
        if (message != null) {
            TextView m = Ui.text(a, message, 15, Theme.TEXT_DIM);
            m.setLineSpacing(0, 1.15f);
            col.addView(m, Ui.lp(-1, -2, 0, Ui.dp(a, 10), 0, 0));
        }
        LinearLayout buttons = Ui.row(a);
        buttons.setGravity(Gravity.END);
        final Dialog[] d = new Dialog[1];
        TextView no = button(a, "Cancelar", false);
        TextView ok = button(a, yes, true);
        buttons.addView(no);
        LinearLayout.LayoutParams okl = Ui.lp(-2, -2);
        okl.leftMargin = Ui.dp(a, 10);
        buttons.addView(ok, okl);
        col.addView(buttons, Ui.lp(-1, -2, 0, Ui.dp(a, 22), 0, 0));
        no.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d[0].dismiss(); }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                d[0].dismiss();
                onYes.run();
            }
        });
        d[0] = show(a, col);
    }

    public static void message(Activity a, String title, String message) {
        LinearLayout col = Ui.column(a);
        int p = Ui.dp(a, 24);
        col.setPadding(p, p, p, Ui.dp(a, 16));
        col.addView(Ui.text(a, title, 19, Theme.TEXT, Ui.BOLD));
        TextView m = Ui.text(a, message, 15, Theme.TEXT_DIM);
        m.setLineSpacing(0, 1.15f);
        col.addView(m, Ui.lp(-1, -2, 0, Ui.dp(a, 10), 0, 0));
        final Dialog[] d = new Dialog[1];
        TextView ok = button(a, "OK", true);
        LinearLayout.LayoutParams l = Ui.lp(-2, -2, 0, Ui.dp(a, 22), 0, 0);
        l.gravity = Gravity.END;
        col.addView(ok, l);
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d[0].dismiss(); }
        });
        d[0] = show(a, col);
    }

    public static void prompt(Activity a, String title, String hint, String value, boolean secret, final Input done) {
        LinearLayout col = Ui.column(a);
        int p = Ui.dp(a, 24);
        col.setPadding(p, p, p, Ui.dp(a, 16));
        col.addView(Ui.text(a, title, 19, Theme.TEXT, Ui.BOLD));
        final EditText e = field(a, hint, value, secret ? InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD
                : InputType.TYPE_CLASS_TEXT);
        col.addView(e, Ui.lp(-1, -2, 0, Ui.dp(a, 16), 0, 0));
        LinearLayout buttons = Ui.row(a);
        buttons.setGravity(Gravity.END);
        final Dialog[] d = new Dialog[1];
        TextView no = button(a, "Cancelar", false);
        TextView ok = button(a, "OK", true);
        buttons.addView(no);
        LinearLayout.LayoutParams okl = Ui.lp(-2, -2);
        okl.leftMargin = Ui.dp(a, 10);
        buttons.addView(ok, okl);
        col.addView(buttons, Ui.lp(-1, -2, 0, Ui.dp(a, 22), 0, 0));
        no.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d[0].dismiss(); }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                d[0].dismiss();
                done.entered(e.getText().toString().trim());
            }
        });
        d[0] = show(a, col);
        d[0].getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        e.requestFocus();
    }

    public static EditText field(Activity a, String hint, String value, int type) {
        EditText e = new EditText(a);
        e.setHint(hint);
        e.setHintTextColor(Theme.TEXT_MUTED);
        e.setTextColor(Theme.TEXT);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setInputType(type);
        if (value != null) e.setText(value);
        int h = Ui.dp(a, 16);
        e.setPadding(h, Ui.dp(a, 15), h, Ui.dp(a, 15));
        android.graphics.drawable.StateListDrawable s = new android.graphics.drawable.StateListDrawable();
        s.addState(new int[] {android.R.attr.state_focused},
                Ui.stroke(Theme.SURFACE_HI, Ui.dp(a, 14), Ui.dp(a, 1.5f), Theme.PINK));
        s.addState(new int[] {}, Ui.stroke(Theme.SURFACE_HI, Ui.dp(a, 14), Ui.dp(a, 1), Theme.STROKE));
        e.setBackground(s);
        return e;
    }

    /** Pill button: filled white (primary) or outlined. */
    public static TextView button(Activity a, String label, boolean primary) {
        TextView t = Ui.text(a, label, 15, primary ? 0xFF111111 : Theme.TEXT, Ui.MEDIUM);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Ui.dp(a, 20), Ui.dp(a, 11), Ui.dp(a, 20), Ui.dp(a, 11));
        t.setClickable(true);
        t.setFocusable(true);
        t.setBackground(Ui.ripple(primary ? Ui.round(0xFFFFFFFF, Ui.dp(a, 30))
                : Ui.stroke(0, Ui.dp(a, 30), Ui.dp(a, 1), 0x66FFFFFF), Ui.dp(a, 30)));
        return t;
    }

    /** Row for settings-like lists: icon, title, optional detail and chevron. */
    public static LinearLayout listRow(Activity a, int icon, String title, String detail) {
        LinearLayout row = Ui.row(a);
        row.setPadding(Ui.dp(a, 16), Ui.dp(a, 14), Ui.dp(a, 12), Ui.dp(a, 14));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(Ui.pressable(a, 0, Ui.dp(a, 14)));
        if (icon != 0) {
            ImageView i = Ui.icon(a, icon, Theme.TEXT_DIM);
            row.addView(i, Ui.lp(Ui.dp(a, 22), Ui.dp(a, 22)));
        }
        LinearLayout texts = Ui.column(a);
        texts.addView(Ui.text(a, title, 16, Theme.TEXT));
        if (detail != null && !detail.isEmpty()) {
            texts.addView(Ui.oneLine(Ui.text(a, detail, 13, Theme.TEXT_MUTED)));
        }
        LinearLayout.LayoutParams tl = Ui.weight(1);
        tl.leftMargin = icon != 0 ? Ui.dp(a, 16) : 0;
        row.addView(texts, tl);
        return row;
    }
}
