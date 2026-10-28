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

/**
 * Entry screen. With the Xplay panel configured the client types the 9-digit code from their recharge,
 * or opens the manual form to sign in with a list URL; otherwise only the manual form (Xtream login or
 * M3U link). Auto-logs in when saved.
 */
public class LoginActivity extends Activity {

    private boolean xtreamTab = true;
    private TextView tabXtream, tabM3u, error, buttonLabel, heading;
    private LinearLayout xtreamForm, m3uForm, form;
    private EditText dns, user, pass, m3u;
    private ProgressBar buttonProgress, splash;
    private View formRoot;
    private TextView splashStatus;
    private View button;
    private EditText code;
    private TextView codeError, codeLabel;
    private ProgressBar codeProgress;
    private View codeButton, codeRoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        Ui.fit(this);
        new Thread(new Runnable() {
            public void run() { Backend.registerDevice(); }
        }, "register-device").start();
        if (Backend.configured()) {
            buildCodeUi();
            String saved = Backend.savedCode();
            if (saved != null) {
                code.setText(saved);
                setContentView(buildSplash());
                loginWithCode(saved, true);
            } else if (Repo.hasLogin()) {
                // Signed in with a list URL instead of a code.
                buildUi();
                xtreamTab = Repo.isXtream();
                updateTabs();
                setContentView(buildSplash());
                connect();
            } else {
                setContentView(codeRoot);
            }
            return;
        }
        buildUi();
        if (Repo.hasLogin()) {
            xtreamTab = Repo.isXtream();
            updateTabs();
            setContentView(buildSplash());
            connect();
        } else {
            xtreamTab = !Repo.MODE_M3U.equals(Repo.prefs().getString("last_mode", Repo.MODE_XTREAM));
            updateTabs();
            setContentView(formRoot);
        }
    }

    /** Loading screen shown while signing in with the saved login. */
    private View buildSplash() {
        FrameLayout root = new FrameLayout(this);
        root.setBackground(new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
                new int[] {0xFF1A1508, Palette.BLACK, Palette.BLACK}));
        LinearLayout c = Ui.column(this);
        c.setGravity(Gravity.CENTER_HORIZONTAL);
        root.addView(c, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));

        View logo = Ui.logo(this, 52);
        c.addView(logo);
        logo.setAlpha(0f);
        logo.setScaleX(0.9f);
        logo.setScaleY(0.9f);
        logo.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(450).start();

        TextView tagline = Ui.text(this, "TV ao vivo, filmes e séries", 17, Palette.TEXT_DIM, false);
        LinearLayout.LayoutParams tl = Ui.lp(-2, -2);
        tl.topMargin = Ui.dp(this, 10);
        c.addView(tagline, tl);

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setIndeterminate(true);
        bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Palette.GOLD));
        LinearLayout.LayoutParams bl = Ui.lp(Ui.dp(this, 220), Ui.dp(this, 6));
        bl.topMargin = Ui.dp(this, 36);
        c.addView(bar, bl);

        splashStatus = Ui.text(this, "Conectando ao servidor…", 14, Palette.TEXT_DIM, false);
        LinearLayout.LayoutParams sl = Ui.lp(-2, -2);
        sl.topMargin = Ui.dp(this, 14);
        c.addView(splashStatus, sl);

        TextView version = Ui.text(this, "versão " + Updater.currentName(this), 12, 0xFF555555, false);
        FrameLayout.LayoutParams vl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        vl.bottomMargin = Ui.dp(this, 20);
        root.addView(version, vl);
        return root;
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

        if (Backend.configured()) {
            View back = secondaryButton("ENTRAR COM CÓDIGO");
            LinearLayout.LayoutParams bp = Ui.lp(-1, Ui.dp(this, 48));
            bp.topMargin = Ui.dp(this, 12);
            form.addView(back, bp);
            back.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { showCodeForm(); }
            });
        }

        formRoot = root;
    }

    /** Outlined button for switching between the code and the list URL forms. */
    private TextView secondaryButton(String label) {
        TextView t = Ui.text(this, label, 15, Palette.TEXT, true);
        t.setGravity(Gravity.CENTER);
        t.setFocusable(true);
        t.setClickable(true);
        t.setBackground(Ui.selectable(this, Palette.SURFACE_HI, 0x33FFFFFF, Ui.dp(this, 24), Palette.GOLD));
        return t;
    }

    /** From the code screen: sign in with a list URL (M3U link or Xtream login) instead. */
    private void showManualForm() {
        if (formRoot == null) buildUi();
        xtreamTab = false;
        updateTabs();
        setContentView(formRoot);
        m3u.requestFocus();
    }

    private void showCodeForm() {
        setContentView(codeRoot);
        code.requestFocus();
    }

    @Override
    public void onBackPressed() {
        if (codeRoot != null && formRoot != null && formRoot.getParent() != null && form.getVisibility() == View.VISIBLE) {
            showCodeForm();
            return;
        }
        super.onBackPressed();
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
                openSavedLogin();
                return null;
            }
        }, new Task.Done<Void>() {
            public void ok(Void v) { openChannels(); }

            public void fail(Exception e) {
                setBusy(false);
                if (formRoot.getParent() == null) setContentView(formRoot);
                form.setVisibility(View.VISIBLE);
                splash.setVisibility(View.GONE);
                heading.setText("Entrar");
                Repo.logout();
                showError(Task.message(e));
            }
        });
    }

    /** Signs in with the saved Xtream login or M3U list. Worker thread. */
    private static void openSavedLogin() throws Exception {
        if (Repo.isXtream()) {
            Xtream.login();
            return;
        }
        // M3U links from Xtream servers carry the login; the API gives movies, series and EPG.
        String m3u = Repo.m3uUrl;
        String[] x = M3uCatalog.xtreamCredentials(m3u);
        if (x != null) {
            Repo.saveXtream(x[0], x[1], x[2]);
            try {
                Xtream.login();
                return;
            } catch (java.io.IOException e) {
                Repo.saveM3u(m3u);
            }
        }
        M3uCatalog.load(m3u);
    }

    private void openChannels() {
        if (splashStatus != null) splashStatus.setText("Abrindo canais…");
        startActivity(new Intent(LoginActivity.this, LiveActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ---------------------------------------------------------------- code login (Xplay panel)

    private void buildCodeUi() {
        LinearLayout root = Ui.row(this);
        root.setBackgroundColor(Palette.BLACK);

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

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout center = new FrameLayout(this);
        scroll.addView(center);
        root.addView(scroll, new LinearLayout.LayoutParams(0, -1, 1f));

        LinearLayout box = Ui.column(this);
        int pad = Ui.dp(this, 24);
        box.setPadding(pad, pad, pad, pad);
        center.addView(box, new FrameLayout.LayoutParams(
                Math.min(Ui.dp(this, 440), getResources().getDisplayMetrics().widthPixels / 2), -2, Gravity.CENTER));

        box.addView(Ui.text(this, "Entrar", 26, Palette.TEXT, true));
        TextView hint = Ui.text(this, "Digite o código de 9 dígitos da sua recarga.", 15, Palette.TEXT_DIM, false);
        LinearLayout.LayoutParams hl = Ui.lp(-1, -2);
        hl.topMargin = Ui.dp(this, 6);
        box.addView(hint, hl);

        code = new EditText(this);
        code.setHint("000 000 000");
        code.setHintTextColor(0xFF4A4A4A);
        code.setTextColor(Palette.TEXT);
        code.setTextSize(28);
        code.setLetterSpacing(0.25f);
        code.setGravity(Gravity.CENTER);
        code.setSingleLine(true);
        code.setInputType(InputType.TYPE_CLASS_NUMBER);
        code.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(9)});
        code.setImeOptions(EditorInfo.IME_ACTION_DONE);
        code.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
        code.setBackground(Ui.selectable(this, Palette.SURFACE_HI, Palette.SURFACE_HI, Ui.dp(this, 14), Palette.GOLD));
        code.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int id, android.view.KeyEvent e) {
                submitCode();
                return true;
            }
        });
        LinearLayout.LayoutParams cl = Ui.lp(-1, -2);
        cl.topMargin = Ui.dp(this, 20);
        box.addView(code, cl);

        codeError = Ui.text(this, "", 14, Palette.ERROR, false);
        codeError.setGravity(Gravity.CENTER);
        codeError.setVisibility(View.GONE);
        LinearLayout.LayoutParams ep = Ui.lp(-1, -2);
        ep.topMargin = Ui.dp(this, 12);
        box.addView(codeError, ep);

        FrameLayout btn = new FrameLayout(this);
        btn.setFocusable(true);
        btn.setClickable(true);
        btn.setBackground(Ui.selectable(this, Palette.GOLD, Palette.GOLD_LIGHT, Ui.dp(this, 28), Palette.TEXT));
        codeLabel = Ui.text(this, "ENTRAR", 16, Palette.BLACK, true);
        btn.addView(codeLabel, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        codeProgress = new ProgressBar(this);
        codeProgress.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Palette.BLACK));
        codeProgress.setVisibility(View.GONE);
        btn.addView(codeProgress, new FrameLayout.LayoutParams(Ui.dp(this, 28), Ui.dp(this, 28), Gravity.CENTER));
        LinearLayout.LayoutParams btp = Ui.lp(-1, Ui.dp(this, 54));
        btp.topMargin = Ui.dp(this, 20);
        box.addView(btn, btp);
        codeButton = btn;
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { submitCode(); }
        });

        View byUrl = secondaryButton("ENTRAR COM LINK (URL)");
        LinearLayout.LayoutParams ul = Ui.lp(-1, Ui.dp(this, 48));
        ul.topMargin = Ui.dp(this, 12);
        box.addView(byUrl, ul);
        byUrl.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showManualForm(); }
        });

        // The install's own user: the reseller links it to the client's recharges.
        LinearLayout device = Ui.row(this);
        device.setGravity(Gravity.CENTER_VERTICAL);
        device.setBackground(Ui.round(0x14E8B84A, Ui.dp(this, 14)));
        device.setPadding(Ui.dp(this, 16), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 12));
        LinearLayout dl = Ui.column(this);
        dl.addView(Ui.text(this, "Usuário deste aparelho", 13, Palette.TEXT_DIM, false));
        dl.addView(Ui.text(this, "Informe ao seu revendedor para recarregar", 12, 0xFF6E6E6E, false));
        device.addView(dl, Ui.weight(1));
        TextView user = Ui.text(this, Backend.deviceUser(), 22, Palette.GOLD, true);
        user.setLetterSpacing(0.12f);
        device.addView(user);
        LinearLayout.LayoutParams dp = Ui.lp(-1, -2);
        dp.topMargin = Ui.dp(this, 24);
        box.addView(device, dp);

        TextView version = Ui.text(this, "versão " + Updater.currentName(this), 12, 0xFF555555, false);
        version.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams vp = Ui.lp(-1, -2);
        vp.topMargin = Ui.dp(this, 16);
        box.addView(version, vp);

        codeRoot = root;
    }

    private void submitCode() {
        String c = code.getText().toString().replaceAll("\\D", "");
        if (c.length() != 9) {
            showCodeError("O código tem 9 números");
            return;
        }
        loginWithCode(c, false);
    }

    /** Checks the code on the panel, then opens the client's list. auto: started from the saved code. */
    private void loginWithCode(final String c, final boolean auto) {
        setCodeBusy(true);
        Task.run(this, new Callable<Void>() {
            public Void call() throws Exception {
                String m3u;
                try {
                    m3u = Backend.login(c);
                } catch (Backend.Refused e) {
                    throw e;
                } catch (Exception e) {
                    // Panel unreachable: keep watching with the list from the last successful login.
                    if (!auto || !Repo.hasLogin()) throw e;
                    openSavedLogin();
                    return null;
                }
                Repo.saveM3u(m3u);
                openSavedLogin();
                return null;
            }
        }, new Task.Done<Void>() {
            public void ok(Void v) { openChannels(); }

            public void fail(Exception e) {
                setCodeBusy(false);
                if (e instanceof Backend.Refused) {
                    Backend.clearCode();
                    Repo.logout();
                }
                if (codeRoot.getParent() == null) setContentView(codeRoot);
                if (e instanceof Backend.Refused && auto) {
                    Popup.message(LoginActivity.this, "Acesso suspenso", Task.message(e));
                }
                showCodeError(Task.message(e));
                code.requestFocus();
            }
        });
    }

    private void setCodeBusy(boolean busy) {
        codeButton.setEnabled(!busy);
        codeLabel.setVisibility(busy ? View.INVISIBLE : View.VISIBLE);
        codeProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        if (busy) codeError.setVisibility(View.GONE);
    }

    private void showCodeError(String msg) {
        codeError.setText(msg);
        codeError.setVisibility(View.VISIBLE);
    }

    // ---------------------------------------------------------------- manual login

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
