package com.xplay.mobile;

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

/** Entry screen: Xtream login (server, user, password) or an M3U link. Skipped when a login is saved. */
public class LoginActivity extends Activity {

    private boolean xtreamTab = true;
    private TextView tabXtream, tabM3u, error, buttonLabel, status;
    private LinearLayout xtreamForm, m3uForm;
    private EditText dns, user, pass, m3u;
    private ProgressBar buttonProgress;
    private View button;
    private boolean busy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (Repo.hasLogin() && !getIntent().getBooleanExtra("edit", false)) {
            Catalog.start();
            startActivity(new Intent(this, ProfilesActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
            return;
        }
        setContentView(build());
        xtreamTab = !Repo.MODE_M3U.equals(Repo.prefs().getString("last_mode", Repo.MODE_XTREAM));
        updateTabs();
    }

    private View build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xFF1D0D16, Theme.BG, Theme.BG}));
        FrameLayout center = new FrameLayout(this);
        scroll.addView(center);

        LinearLayout box = Ui.column(this);
        box.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(this, 24);
        box.setPadding(pad, Ui.dp(this, 40), pad, pad);
        center.addView(box, new FrameLayout.LayoutParams(Math.min(Ui.dp(this, 460), Ui.screenWidth(this)), -2,
                Gravity.CENTER));

        box.addView(Ui.logo(this, 36));
        TextView sub = Ui.text(this, "Filmes, séries e TV ao vivo da sua lista", 16, Theme.TEXT_DIM);
        sub.setGravity(Gravity.CENTER);
        box.addView(sub, Ui.lp(-2, -2, 0, Ui.dp(this, 14), 0, Ui.dp(this, 32)));

        LinearLayout tabs = Ui.row(this);
        tabs.setBackground(Ui.round(Theme.SURFACE_HI, Ui.dp(this, 26)));
        int tp = Ui.dp(this, 4);
        tabs.setPadding(tp, tp, tp, tp);
        tabXtream = tab("Login Xtream");
        tabM3u = tab("Link M3U");
        tabs.addView(tabXtream, Ui.weight(1));
        tabs.addView(tabM3u, Ui.weight(1));
        box.addView(tabs, Ui.lp(-1, -2));
        tabXtream.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                xtreamTab = true;
                updateTabs();
            }
        });
        tabM3u.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                xtreamTab = false;
                updateTabs();
            }
        });

        android.content.SharedPreferences p = Repo.prefs();
        xtreamForm = Ui.column(this);
        dns = field(xtreamForm, "Servidor (ex.: http://servidor.com:8080)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, p.getString("xtream_dns", ""));
        user = field(xtreamForm, "Usuário", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS,
                p.getString("xtream_user", ""));
        pass = field(xtreamForm, "Senha", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD,
                p.getString("xtream_pass", ""));
        pass.setImeOptions(EditorInfo.IME_ACTION_DONE);
        box.addView(xtreamForm, Ui.lp(-1, -2));

        m3uForm = Ui.column(this);
        m3u = field(m3uForm, "Link da lista (http://…/get.php?…)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, p.getString("playlist_url", ""));
        m3u.setImeOptions(EditorInfo.IME_ACTION_DONE);
        TextView hint = Ui.text(this, "Links de servidores Xtream (get.php?username=…) abrem com capas, "
                + "sinopses e temporadas.", 13, Theme.TEXT_MUTED);
        m3uForm.addView(hint, Ui.lp(-1, -2, Ui.dp(this, 4), Ui.dp(this, 10), Ui.dp(this, 4), 0));
        box.addView(m3uForm, Ui.lp(-1, -2));

        TextView.OnEditorActionListener go = new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int id, android.view.KeyEvent e) {
                submit();
                return true;
            }
        };
        pass.setOnEditorActionListener(go);
        m3u.setOnEditorActionListener(go);

        error = Ui.text(this, "", 14, Theme.ERROR);
        error.setGravity(Gravity.CENTER);
        error.setVisibility(View.GONE);
        box.addView(error, Ui.lp(-1, -2, 0, Ui.dp(this, 14), 0, 0));

        FrameLayout btn = new FrameLayout(this);
        btn.setClickable(true);
        btn.setFocusable(true);
        btn.setBackground(Ui.ripple(Ui.round(0xFFFFFFFF, Ui.dp(this, 30)), Ui.dp(this, 30)));
        buttonLabel = Ui.text(this, "Entrar", 17, 0xFF111111, Ui.MEDIUM);
        btn.addView(buttonLabel, Ui.frame(-2, -2, Gravity.CENTER));
        buttonProgress = new ProgressBar(this);
        buttonProgress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(0xFF111111));
        buttonProgress.setVisibility(View.GONE);
        btn.addView(buttonProgress, Ui.frame(Ui.dp(this, 26), Ui.dp(this, 26), Gravity.CENTER));
        box.addView(btn, Ui.lp(-1, Ui.dp(this, 56), 0, Ui.dp(this, 20), 0, 0));
        button = btn;
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { submit(); }
        });

        status = Ui.text(this, "", 13, Theme.TEXT_MUTED);
        status.setGravity(Gravity.CENTER);
        box.addView(status, Ui.lp(-1, -2, 0, Ui.dp(this, 12), 0, 0));

        TextView version = Ui.text(this, "versão " + version(), 12, 0xFF55555C);
        box.addView(version, Ui.lp(-2, -2, 0, Ui.dp(this, 24), 0, 0));
        return scroll;
    }

    private String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    private TextView tab(String label) {
        TextView t = Ui.text(this, label, 15, Theme.TEXT, Ui.MEDIUM);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Ui.dp(this, 11), 0, Ui.dp(this, 11));
        t.setClickable(true);
        t.setFocusable(true);
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
        t.setTextColor(on ? 0xFF111111 : Theme.TEXT_DIM);
        t.setBackground(Ui.ripple(on ? Ui.round(0xFFFFFFFF, Ui.dp(this, 22)) : null, Ui.dp(this, 22)));
    }

    private EditText field(LinearLayout parent, String hint, int type, String value) {
        EditText e = Sheet.field(this, hint, value, type);
        e.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        parent.addView(e, Ui.lp(-1, -2, 0, Ui.dp(this, 12), 0, 0));
        return e;
    }

    private void submit() {
        if (busy) return;
        if (xtreamTab) {
            String s = dns.getText().toString().trim();
            String u = user.getText().toString().trim();
            String p = pass.getText().toString().trim();
            if (s.isEmpty() || u.isEmpty() || p.isEmpty()) {
                showError("Preencha servidor, usuário e senha");
                return;
            }
            String server = Xtream.normalizeServer(s);
            dns.setText(server);
            Repo.saveXtream(server, u, p);
        } else {
            String url = Http.extractUrl(m3u.getText().toString());
            if (url.isEmpty()) {
                showError("Informe o link da lista");
                return;
            }
            m3u.setText(url);
            Repo.saveM3u(url);
        }
        Repo.prefs().edit().putString("last_mode", Repo.mode).apply();
        hideKeyboard();
        setBusy(true);
        Catalog.onStatus(new Catalog.Progress() {
            public void step(String what) { status.setText(what); }
        });
        Catalog.reload();
        Catalog.whenLoaded(new Catalog.Listener() {
            public void loaded(Exception e) {
                if (isFinishing()) return;
                Catalog.onStatus(null);
                setBusy(false);
                if (e != null) {
                    Repo.logout();
                    showError(Task.message(e));
                    return;
                }
                Intent i = new Intent(LoginActivity.this, ProfilesActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(i);
                finish();
            }
        });
    }

    private void hideKeyboard() {
        View f = getCurrentFocus();
        if (f == null) return;
        android.view.inputmethod.InputMethodManager im =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (im != null) im.hideSoftInputFromWindow(f.getWindowToken(), 0);
    }

    private void setBusy(boolean b) {
        busy = b;
        button.setEnabled(!b);
        buttonLabel.setVisibility(b ? View.INVISIBLE : View.VISIBLE);
        buttonProgress.setVisibility(b ? View.VISIBLE : View.GONE);
        if (b) error.setVisibility(View.GONE);
        status.setText(b ? "Conectando…" : "");
    }

    private void showError(String msg) {
        error.setText(msg);
        error.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Catalog.onStatus(null);
    }
}
