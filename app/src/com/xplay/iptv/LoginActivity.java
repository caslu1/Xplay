package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.concurrent.Callable;

/** Entry screen: Xtream login (DNS, user, password) or an M3U link. Auto-logs in when saved. */
public class LoginActivity extends Activity {

    private boolean xtreamTab = true;
    private TextView tabXtream, tabM3u, error, buttonLabel, heading;
    private LinearLayout xtreamForm, m3uForm, form;
    private EditText dns, user, pass, m3u;
    private ProgressBar buttonProgress, splash;
    private View button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        buildUi();
        if (Repo.hasLogin()) {
            xtreamTab = Repo.isXtream();
            updateTabs();
            form.setVisibility(View.GONE);
            splash.setVisibility(View.VISIBLE);
            heading.setText("Conectando…");
            connect();
        } else {
            xtreamTab = !Repo.MODE_M3U.equals(Repo.prefs().getString("last_mode", Repo.MODE_XTREAM));
            updateTabs();
        }
    }

    private void buildUi() {
        LinearLayout root = Ui.row(this);
        root.setBackgroundColor(Palette.BLACK);

        // Left: brand
        LinearLayout brand = Ui.column(this);
        brand.setGravity(Gravity.CENTER);
        brand.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[] {0xFF1A1508, Palette.BLACK}));
        brand.addView(Ui.logo(this, 44));
        View line = new View(this);
        line.setBackground(Ui.round(Palette.GOLD, Ui.dp(this, 2)));
        LinearLayout.LayoutParams ll = Ui.lp(Ui.dp(this, 56), Ui.dp(this, 4));
        ll.topMargin = Ui.dp(this, 18);
        brand.addView(line, ll);
        TextView subtitle = Ui.text(this, "TV ao vivo, filmes e séries\nem um só lugar", 18, Palette.TEXT_DIM, false);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams sp = Ui.lp(-2, -2);
        sp.topMargin = Ui.dp(this, 18);
        brand.addView(subtitle, sp);
        root.addView(brand, new LinearLayout.LayoutParams(0, -1, 1f));

        // Right: form
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout center = new FrameLayout(this);
        scroll.addView(center);
        root.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1f));

        LinearLayout box = Ui.column(this);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(this, 24);
        box.setPadding(pad, pad, pad, pad);
        center.addView(box, new FrameLayout.LayoutParams(
                Math.min(Ui.dp(this, 440), getResources().getDisplayMetrics().widthPixels / 2), -2, Gravity.CENTER));

        heading = Ui.text(this, "Entrar", 26, Palette.TEXT, true);
        LinearLayout.LayoutParams hp = Ui.lp(-1, -2);
        hp.bottomMargin = Ui.dp(this, 16);
        box.addView(heading, hp);

        splash = new ProgressBar(this);
        splash.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Palette.GOLD));
        splash.setVisibility(View.GONE);
        box.addView(splash, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));

        form = Ui.column(this);
        box.addView(form, Ui.lp(-1, -2));

        LinearLayout tabs = Ui.row(this);
        tabs.setBackground(Ui.round(Palette.SURFACE_HI, Ui.dp(this, 24)));
        tabs.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
        tabXtream = tab("Login Xtream");
        tabM3u = tab("Lista M3U");
        tabs.addView(tabXtream, Ui.weight(1));
        tabs.addView(tabM3u, Ui.weight(1));
        form.addView(tabs, Ui.lp(-1, -2));
        tabXtream.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { xtreamTab = true; updateTabs(); }
        });
        tabM3u.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { xtreamTab = false; updateTabs(); }
        });

        android.content.SharedPreferences p = Repo.prefs();
        xtreamForm = Ui.column(this);
        dns = field(xtreamForm, "DNS (ex.: http://servidor.com:8080)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, p.getString("xtream_dns", ""));
        user = field(xtreamForm, "Usuário", InputType.TYPE_CLASS_TEXT, p.getString("xtream_user", ""));
        pass = field(xtreamForm, "Senha",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, p.getString("xtream_pass", ""));
        form.addView(xtreamForm, Ui.lp(-1, -2));

        m3uForm = Ui.column(this);
        m3u = field(m3uForm, "Link da lista (http://.../lista.m3u)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, p.getString("playlist_url", ""));
        form.addView(m3uForm, Ui.lp(-1, -2));

        error = Ui.text(this, "", 14, Palette.ERROR, false);
        error.setGravity(Gravity.CENTER);
        error.setVisibility(View.GONE);
        LinearLayout.LayoutParams ep = Ui.lp(-1, -2);
        ep.topMargin = Ui.dp(this, 12);
        form.addView(error, ep);

        FrameLayout btn = new FrameLayout(this);
        btn.setFocusable(true);
        btn.setClickable(true);
        btn.setBackground(Ui.selectable(this, Palette.GOLD, 0xFFF5CD6A, Ui.dp(this, 28), Palette.TEXT));
        buttonLabel = Ui.text(this, "ENTRAR", 16, Palette.BLACK, true);
        btn.addView(buttonLabel, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        buttonProgress = new ProgressBar(this);
        buttonProgress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Palette.BLACK));
        buttonProgress.setVisibility(View.GONE);
        btn.addView(buttonProgress, new FrameLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 28), Gravity.CENTER));
        LinearLayout.LayoutParams btp = Ui.lp(-1, Ui.dp(this, 54));
        btp.topMargin = Ui.dp(this, 20);
        form.addView(btn, btp);
        button = btn;
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { submit(); }
        });

        setContentView(root);
    }

    private TextView tab(String label) {
        TextView t = Ui.text(this, label, 15, Palette.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        t.setFocusable(true);
        t.setClickable(true);
        return t;
    }

    private void updateTabs() {
        styleTab(tabXtream, xtreamTab);
        styleTab(tabM3u, !xtreamTab);
        xtreamForm.setVisibility(xtreamTab ? View.VISIBLE : View.GONE);
        m3uForm.setVisibility(xtreamTab ? View.GONE : View.VISIBLE);
        error.setVisibility(View.GONE);
    }

    private void styleTab(TextView t, boolean on) {
        t.setTextColor(on ? Palette.BLACK : Palette.TEXT_DIM);
        t.setBackground(Ui.selectable(this, on ? Palette.GOLD : 0, on ? 0xFFF5CD6A : 0x22FFFFFF,
                Ui.dp(this, 20), 0));
    }

    private EditText field(LinearLayout parent, String hint, int type, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setHintTextColor(0xFF6E6E6E);
        e.setTextColor(Palette.TEXT);
        e.setTextSize(16);
        e.setSingleLine(true);
        e.setInputType(type);
        e.setText(value);
        e.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        int h = Ui.dp(this, 16);
        e.setPadding(h, Ui.dp(this, 14), h, Ui.dp(this, 14));
        e.setBackground(Ui.selectable(this, Palette.SURFACE_HI, Palette.SURFACE_HI, Ui.dp(this, 14), Palette.GOLD));
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.topMargin = Ui.dp(this, 12);
        parent.addView(e, lp);
        return e;
    }

    private void submit() {
        if (xtreamTab) {
            String s = dns.getText().toString().trim();
            String u = user.getText().toString().trim();
            String p = pass.getText().toString().trim();
            if (s.isEmpty() || u.isEmpty() || p.isEmpty()) {
                showError("Preencha DNS, usuário e senha");
                return;
            }
            Repo.saveXtream(Xtream.normalizeServer(s), u, p);
        } else {
            String url = m3u.getText().toString().trim();
            if (url.isEmpty()) {
                showError("Informe o link da lista");
                return;
            }
            if (!url.contains("://")) url = "http://" + url;
            Repo.saveM3u(url);
        }
        Repo.prefs().edit().putString("last_mode", Repo.mode).apply();
        connect();
    }

    private void connect() {
        setBusy(true);
        Task.run(this, new Callable<Void>() {
            public Void call() throws Exception {
                if (Repo.isXtream()) {
                    Xtream.login();
                    return null;
                }
                // M3U links from Xtream servers carry the login; the API gives movies, series and EPG.
                String m3u = Repo.m3uUrl;
                String[] x = M3uCatalog.xtreamCredentials(m3u);
                if (x != null) {
                    Repo.saveXtream(x[0], x[1], x[2]);
                    try {
                        Xtream.login();
                        return null;
                    } catch (java.io.IOException e) {
                        Repo.saveM3u(m3u);
                    }
                }
                M3uCatalog.load(m3u);
                return null;
            }
        }, new Task.Done<Void>() {
            public void ok(Void v) {
                startActivity(new Intent(LoginActivity.this, LiveActivity.class));
                finish();
            }

            public void fail(Exception e) {
                setBusy(false);
                form.setVisibility(View.VISIBLE);
                splash.setVisibility(View.GONE);
                heading.setText("Entrar");
                Repo.logout();
                showError(Task.message(e));
            }
        });
    }

    private void setBusy(boolean busy) {
        button.setEnabled(!busy);
        buttonLabel.setVisibility(busy ? View.INVISIBLE : View.VISIBLE);
        buttonProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (busy) error.setVisibility(View.GONE);
    }

    private void showError(String msg) {
        error.setText(msg);
        error.setVisibility(View.VISIBLE);
    }
}
