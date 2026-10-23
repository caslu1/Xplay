package com.xplay.iptv;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.ColorDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

/** The app's own popup: a dark rounded card with gold buttons, an optional input or custom view. */
public final class Popup {

    private final Activity a;
    private final Dialog dialog;
    private final LinearLayout body;
    private final LinearLayout actions;
    private EditText input;
    private TextView primary;

    public Popup(Activity a, String title, String message) {
        this.a = a;
        dialog = new Dialog(a);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        body = Ui.column(a);
        body.setBackground(Ui.stroke(Palette.SURFACE, Ui.dp(a, 22), 1, 0x22FFFFFF));
        int p = Ui.dp(a, 26);
        body.setPadding(p, Ui.dp(a, 24), p, Ui.dp(a, 20));

        View bar = new View(a);
        bar.setBackground(Ui.round(Palette.GOLD, Ui.dp(a, 2)));
        LinearLayout.LayoutParams bl = Ui.lp(Ui.dp(a, 36), Ui.dp(a, 4));
        bl.bottomMargin = Ui.dp(a, 14);
        body.addView(bar, bl);

        if (title != null) body.addView(Ui.text(a, title, 21, Palette.TEXT, true));
        if (message != null) {
            TextView m = Ui.text(a, message, 15, Palette.TEXT_DIM, false);
            m.setLineSpacing(0, 1.2f);
            LinearLayout.LayoutParams ml = Ui.lp(-1, -2);
            ml.topMargin = Ui.dp(a, 8);
            body.addView(m, ml);
        }

        actions = Ui.row(a);
        actions.setGravity(Gravity.END);

        dialog.setContentView(body);
        Window w = dialog.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new ColorDrawable(0));
            w.setDimAmount(0.75f);
            w.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            int width = Math.min(Ui.dp(a, 460), (int) (a.getResources().getDisplayMetrics().widthPixels * 0.9f));
            w.setLayout(width, -2);
        }
    }

    /** Adds a text field; Enter on the keyboard presses the primary button. */
    public EditText input(String hint, int type, String value) {
        input = new EditText(a);
        input.setHint(hint);
        input.setHintTextColor(0xFF6E6E6E);
        input.setTextColor(Palette.TEXT);
        input.setTextSize(18);
        input.setSingleLine(true);
        input.setInputType(type);
        if (value != null) input.setText(value);
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        int h = Ui.dp(a, 16);
        input.setPadding(h, Ui.dp(a, 13), h, Ui.dp(a, 13));
        input.setBackground(Ui.selectable(a, Palette.SURFACE_HI, Palette.SURFACE_HI, Ui.dp(a, 14), Palette.GOLD));
        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int id, KeyEvent e) {
                if (primary != null) primary.performClick();
                return true;
            }
        });
        return add(input).input;
    }

    /** A numeric password field, centered, for PINs. */
    public EditText pinInput(String hint) {
        EditText e = input(hint, InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD, null);
        e.setGravity(Gravity.CENTER);
        e.setTextSize(26);
        e.setLetterSpacing(0.3f);
        return e;
    }

    public Popup add(View v) {
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = Ui.dp(a, 16);
        body.addView(v, lp);
        return this;
    }

    /** Adds a button; action may be null. The popup closes after the action unless keepOpen. */
    public Popup button(String label, boolean isPrimary, final Runnable action, final boolean keepOpen) {
        final TextView b = Ui.text(a, label, 15, isPrimary ? Palette.BLACK : Palette.TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        b.setPadding(Ui.dp(a, 22), Ui.dp(a, 12), Ui.dp(a, 22), Ui.dp(a, 12));
        float r = Ui.dp(a, 24);
        b.setBackground(isPrimary
                ? Ui.selectable(a, Palette.GOLD, Palette.GOLD_LIGHT, r, Palette.TEXT)
                : Ui.selectable(a, 0x1FFFFFFF, 0x33FFFFFF, r, Palette.GOLD));
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!keepOpen) dialog.dismiss();
                if (action != null) action.run();
            }
        });
        LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
        lp.leftMargin = Ui.dp(a, 10);
        actions.addView(b, lp);
        if (isPrimary) primary = b;
        return this;
    }

    public Popup button(String label, boolean isPrimary, Runnable action) {
        return button(label, isPrimary, action, false);
    }

    public Popup cancelable(boolean c) {
        dialog.setCancelable(c);
        return this;
    }

    public Popup onDismiss(final Runnable r) {
        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            public void onDismiss(android.content.DialogInterface d) { r.run(); }
        });
        return this;
    }

    public Popup show() {
        if (actions.getChildCount() > 0 && actions.getParent() == null) {
            LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
            lp.topMargin = Ui.dp(a, 22);
            body.addView(actions, lp);
        }
        if (a.isFinishing()) return this;
        dialog.show();
        if (input != null) {
            input.requestFocus();
            Window w = dialog.getWindow();
            if (w != null && !Ui.isTv(a)) w.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        } else if (primary != null) {
            primary.requestFocus();
        }
        return this;
    }

    public void dismiss() {
        if (dialog.isShowing()) dialog.dismiss();
    }

    public boolean isShowing() {
        return dialog.isShowing();
    }

    /** Shows an error/warning line inside the popup (e.g. wrong PIN) without closing it. */
    public void error(String msg) {
        if (input != null) {
            input.setText("");
            input.setHint(msg);
            input.setHintTextColor(Palette.ERROR);
        }
    }

    public static Popup message(Activity a, String title, String message) {
        return new Popup(a, title, message).button("OK", true, null).show();
    }

    public static Popup confirm(Activity a, String title, String message, String ok, Runnable onOk) {
        return new Popup(a, title, message).button("Cancelar", false, null).button(ok, true, onOk).show();
    }
}
