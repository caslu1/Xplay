package com.xplay.teclado;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** The launcher screen: turn the keyboard on, pick it, and the on/off switches it keeps. */
public class SetupActivity extends Activity {

    private static final int ACCENT = 0xFFE84A6A, BG = 0xFF14141A, CARD = 0xFF20202A, TEXT = 0xFFF2F2F5, DIM = 0xFF9A9AA6;
    private Store store;
    private LinearLayout steps;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        getWindow().getDecorView().setBackgroundColor(BG);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(28), pad, dp(28));
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("Teclado Xplay");
        title.setTextColor(TEXT);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Microfone para ditar, área de transferência, tamanho ajustável e 5 temas. Tudo fica só no seu aparelho.");
        sub.setTextColor(DIM);
        sub.setTextSize(14);
        sub.setPadding(0, dp(6), 0, dp(20));
        root.addView(sub);

        steps = new LinearLayout(this);
        steps.setOrientation(LinearLayout.VERTICAL);
        root.addView(steps);

        LinearLayout opts = card();
        opts.addView(sectionTitle("Preferências"));
        opts.addView(toggle("Gravar o que digito e copio", "Guarda o histórico e a área de transferência neste aparelho.",
                store.recording(), new OnSet() { public void set(boolean v) { store.setRecording(v); } }));
        opts.addView(toggle("Vibrar ao tocar", null, store.vibrate(),
                new OnSet() { public void set(boolean v) { store.setVibrate(v); } }));
        opts.addView(toggle("Linha de números em cima", null, store.numberRow(),
                new OnSet() { public void set(boolean v) { store.setNumberRow(v); } }));
        root.addView(opts);

        TextView foot = new TextView(this);
        foot.setText("Dica: segure a tecla de espaço para trocar de teclado. Segure as letras para acentos.");
        foot.setTextColor(DIM);
        foot.setTextSize(12);
        foot.setPadding(dp(4), dp(18), dp(4), 0);
        root.addView(foot);

        // Ask for the microphone up front, like the official keyboards do, so dictation just works.
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildSteps();
    }

    private void buildSteps() {
        steps.removeAllViews();
        final boolean enabled = isEnabled();
        final boolean picked = isPicked();

        LinearLayout c1 = card();
        c1.addView(stepTitle("1", "Ligar o teclado nos ajustes", enabled));
        if (!enabled) {
            c1.addView(hint("Na lista de teclados, ative o \"Teclado Xplay\"."));
            c1.addView(button("Abrir ajustes de teclado", new Runnable() {
                public void run() {
                    startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
                }
            }));
        } else {
            c1.addView(hint("Pronto, o teclado está ligado."));
        }
        steps.addView(c1);

        LinearLayout c2 = card();
        c2.addView(stepTitle("2", "Escolher como teclado atual", picked));
        if (!picked) {
            c2.addView(hint("Toque abaixo e selecione o \"Teclado Xplay\"."));
            c2.addView(button(enabled ? "Escolher teclado agora" : "Ligue o passo 1 primeiro", new Runnable() {
                public void run() {
                    InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                    if (imm != null) imm.showInputMethodPicker();
                }
            }));
        } else {
            c2.addView(hint("O Teclado Xplay está selecionado."));
        }
        steps.addView(c2);

        LinearLayout c3 = card();
        c3.addView(sectionTitle("Experimente aqui"));
        EditText test = new EditText(this);
        test.setHint("Toque e digite para testar…");
        test.setHintTextColor(DIM);
        test.setTextColor(TEXT);
        test.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        test.setMinLines(2);
        test.setGravity(Gravity.TOP);
        GradientDrawable box = new GradientDrawable();
        box.setColor(0xFF2C2C38);
        box.setCornerRadius(dp(10));
        test.setBackground(box);
        test.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(8);
        c3.addView(test, lp);
        steps.addView(c3);
    }

    private boolean isEnabled() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm == null) return false;
        for (Object info : imm.getEnabledInputMethodList()) {
            if (info.toString().contains(getPackageName())) return true;
        }
        return false;
    }

    private boolean isPicked() {
        String id = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        return id != null && id.contains(getPackageName());
    }

    // ---- Small view builders.

    private interface OnSet { void set(boolean value); }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(dp(16));
        c.setBackground(bg);
        c.setPadding(dp(16), dp(14), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = dp(14);
        c.setLayoutParams(lp);
        return c;
    }

    private View sectionTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(TEXT);
        t.setTextSize(17);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private View stepTitle(String n, String s, boolean done) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        TextView badge = new TextView(this);
        badge.setText(done ? "✓" : n);
        badge.setTextColor(Color.WHITE);
        badge.setTextSize(15);
        badge.setTypeface(Typeface.DEFAULT_BOLD);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.OVAL);
        d.setColor(done ? 0xFF3DBB6B : ACCENT);
        badge.setBackground(d);
        int s1 = dp(28);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(s1, s1);
        blp.rightMargin = dp(10);
        r.addView(badge, blp);
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(TEXT);
        t.setTextSize(16);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        r.addView(t);
        return r;
    }

    private View hint(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(DIM);
        t.setTextSize(13);
        t.setPadding(dp(38), dp(4), 0, 0);
        return t;
    }

    private Button button(String s, final Runnable onClick) {
        Button btn = new Button(this);
        btn.setText(s);
        btn.setTextColor(Color.WHITE);
        btn.setAllCaps(false);
        btn.setTextSize(15);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ACCENT);
        bg.setCornerRadius(dp(12));
        btn.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { onClick.run(); }
        });
        return btn;
    }

    private View toggle(String label, String desc, boolean on, final OnSet onSet) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, dp(10), 0, dp(2));
        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(TEXT);
        t.setTextSize(15);
        texts.addView(t);
        if (desc != null) {
            TextView d = new TextView(this);
            d.setText(desc);
            d.setTextColor(DIM);
            d.setTextSize(12);
            texts.addView(d);
        }
        r.addView(texts);
        CheckBox cb = new CheckBox(this);
        cb.setChecked(on);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton v, boolean checked) { onSet.set(checked); }
        });
        r.addView(cb);
        return r;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
