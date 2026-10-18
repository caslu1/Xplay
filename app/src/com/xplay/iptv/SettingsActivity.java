package com.xplay.iptv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

/** Settings: account, parental control, connection test, updates and about, one section at a time. */
public class SettingsActivity extends Activity implements TopBar.Listener {

    private static final String[] SECTIONS = {"Conta", "Controle parental", "Teste de conexão", "Atualizações", "Sobre"};
    private static final int[] ICONS = {R.drawable.ic_person, R.drawable.ic_heart, R.drawable.ic_refresh,
            R.drawable.ic_arrow_right, R.drawable.ic_settings};

    private final List<TextView> menuItems = new ArrayList<TextView>();
    private LinearLayout content;
    private ScrollView contentScroll;
    private int section = -1;
    private boolean testing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (!Repo.hasLogin() || Repo.channels == null) {
            finish();
            return;
        }
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Palette.BLACK);
        TopBar top = new TopBar(this, Repo.hasVod(), this);
        top.select(TopBar.ACCOUNT);
        root.addView(top, Ui.lp(-1, -2));

        LinearLayout body = Ui.row(this);
        body.setGravity(Gravity.TOP);
        body.setPadding(Ui.dp(this, 24), Ui.dp(this, 8), Ui.dp(this, 24), Ui.dp(this, 16));
        root.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        LinearLayout menu = Ui.column(this);
        TextView title = Ui.text(this, "Configurações", 24, Palette.TEXT, true);
        title.setPadding(Ui.dp(this, 8), 0, 0, Ui.dp(this, 14));
        menu.addView(title);
        for (int i = 0; i < SECTIONS.length; i++) {
            final int which = i;
            TextView item = Ui.text(this, SECTIONS[i], 16, Palette.TEXT_DIM, true);
            item.setSingleLine(true);
            item.setFocusable(true);
            item.setClickable(true);
            item.setGravity(Gravity.CENTER_VERTICAL);
            android.graphics.drawable.Drawable icon = getResources().getDrawable(ICONS[i]).mutate();
            icon.setBounds(0, 0, Ui.dp(this, 20), Ui.dp(this, 20));
            item.setCompoundDrawables(icon, null, null, null);
            item.setCompoundDrawablePadding(Ui.dp(this, 14));
            item.setPadding(Ui.dp(this, 16), Ui.dp(this, 14), Ui.dp(this, 16), Ui.dp(this, 14));
            LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
            lp.bottomMargin = Ui.dp(this, 4);
            menu.addView(item, lp);
            menuItems.add(item);
            item.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { show(which); }
            });
            item.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                public void onFocusChange(View v, boolean has) { if (has) show(which); }
            });
        }
        body.addView(menu, Ui.lp(Ui.dp(this, 250), -1));

        contentScroll = new ScrollView(this);
        contentScroll.setBackground(Ui.round(Palette.SURFACE, Ui.dp(this, 18)));
        content = Ui.column(this);
        int p = Ui.dp(this, 28);
        content.setPadding(p, Ui.dp(this, 24), p, p);
        contentScroll.addView(content);
        LinearLayout.LayoutParams cl = new LinearLayout.LayoutParams(0, -1, 1f);
        cl.leftMargin = Ui.dp(this, 20);
        body.addView(contentScroll, cl);

        setContentView(root);
        show(0);
        menuItems.get(0).requestFocus();
    }

    public void onTopBar(int item) {
        if (item != TopBar.ACCOUNT) TopBar.open(this, item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(0, 0);
    }

    private void show(int which) {
        if (which == section) return;
        section = which;
        float r = Ui.dp(this, 12);
        for (int i = 0; i < menuItems.size(); i++) {
            boolean on = i == which;
            TextView t = menuItems.get(i);
            t.setTextColor(on ? Palette.TEXT : Palette.TEXT_DIM);
            t.setBackground(Ui.selectable(this, on ? 0x26E8B84A : 0, on ? 0x40E8B84A : 0x1AFFFFFF, r, Palette.GOLD));
            t.getCompoundDrawables()[0].setTint(on ? Palette.GOLD : Palette.TEXT_DIM);
        }
        content.removeAllViews();
        contentScroll.scrollTo(0, 0);
        switch (which) {
            case 0: account(); break;
            case 1: parental(); break;
            case 2: connection(); break;
            case 3: updates(); break;
            default: about(); break;
        }
    }

    // ---------------------------------------------------------------- building blocks

    private void heading(String title, String subtitle) {
        content.addView(Ui.text(this, title, 22, Palette.TEXT, true));
        if (subtitle != null) {
            TextView s = Ui.text(this, subtitle, 14, Palette.TEXT_DIM, false);
            s.setPadding(0, Ui.dp(this, 4), 0, 0);
            content.addView(s);
        }
        View gap = new View(this);
        content.addView(gap, Ui.lp(1, Ui.dp(this, 18)));
    }

    private LinearLayout card() {
        LinearLayout c = Ui.column(this);
        c.setBackground(Ui.round(Palette.SURFACE_HI, Ui.dp(this, 14)));
        c.setPadding(Ui.dp(this, 20), Ui.dp(this, 6), Ui.dp(this, 20), Ui.dp(this, 6));
        LinearLayout.LayoutParams lp = Ui.lp(-1, -2);
        lp.bottomMargin = Ui.dp(this, 16);
        content.addView(c, lp);
        return c;
    }

    private TextView row(LinearLayout card, String label, String value) {
        if (card.getChildCount() > 0) {
            View line = new View(this);
            line.setBackgroundColor(0x14FFFFFF);
            card.addView(line, Ui.lp(-1, 1));
        }
        LinearLayout r = Ui.row(this);
        r.setPadding(0, Ui.dp(this, 13), 0, Ui.dp(this, 13));
        r.addView(Ui.text(this, label, 15, Palette.TEXT_DIM, false), Ui.weight(1));
        TextView v = Ui.text(this, value == null || value.isEmpty() ? "-" : value, 15, Palette.TEXT, true);
        v.setGravity(Gravity.END);
        v.setSingleLine(true);
        v.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        r.addView(v, Ui.weight(1.6f));
        card.addView(r, Ui.lp(-1, -2));
        return v;
    }

    private TextView button(String label, boolean primary) {
        TextView b = Ui.text(this, label, 15, primary ? Palette.BLACK : Palette.TEXT, true);
        b.setGravity(Gravity.CENTER);
        b.setFocusable(true);
        b.setClickable(true);
        b.setPadding(Ui.dp(this, 24), Ui.dp(this, 13), Ui.dp(this, 24), Ui.dp(this, 13));
        float r = Ui.dp(this, 24);
        b.setBackground(primary
                ? Ui.selectable(this, Palette.GOLD, Palette.GOLD_LIGHT, r, Palette.TEXT)
                : Ui.selectable(this, 0x1FFFFFFF, 0x33FFFFFF, r, Palette.GOLD));
        return b;
    }

    private LinearLayout buttons(View... views) {
        LinearLayout r = Ui.row(this);
        for (View v : views) {
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.rightMargin = Ui.dp(this, 12);
            r.addView(v, lp);
        }
        content.addView(r, Ui.lp(-1, -2));
        return r;
    }

    private static String date(String unix) {
        try {
            long t = Long.parseLong(unix.trim());
            if (t <= 0) return "Ilimitado";
            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date(t * 1000L));
        } catch (NumberFormatException e) {
            return unix.isEmpty() || unix.equals("null") ? "Ilimitado" : unix;
        }
    }

    // ---------------------------------------------------------------- sections

    private void account() {
        heading("Conta", Repo.isXtream() ? "Login Xtream" : "Lista M3U");
        LinearLayout profile = Ui.row(this);
        ImageView avatar = Ui.icon(this, R.drawable.ic_person, Palette.BLACK);
        avatar.setBackground(Ui.round(Palette.GOLD, Ui.dp(this, 30)));
        int ap = Ui.dp(this, 14);
        avatar.setPadding(ap, ap, ap, ap);
        profile.addView(avatar, Ui.lp(Ui.dp(this, 60), Ui.dp(this, 60)));
        LinearLayout names = Ui.column(this);
        names.setPadding(Ui.dp(this, 16), 0, 0, 0);
        names.addView(Ui.text(this, Repo.isXtream() ? Repo.user : "Minha lista", 20, Palette.TEXT, true));
        org.json.JSONObject a = Repo.account;
        String status = a == null ? "" : a.optString("status", "");
        boolean active = status.isEmpty() || status.equalsIgnoreCase("Active");
        TextView chip = Ui.text(this, active ? "● Conta ativa" : status, 13, active ? 0xFF4CD787 : Palette.ERROR, true);
        names.addView(chip);
        profile.addView(names, Ui.weight(1));
        LinearLayout.LayoutParams pl = Ui.lp(-1, -2);
        pl.bottomMargin = Ui.dp(this, 18);
        content.addView(profile, pl);

        LinearLayout info = card();
        if (Repo.isXtream()) {
            row(info, "Servidor", Repo.server);
            if (a != null) {
                row(info, "Vencimento", date(a.optString("exp_date", "")));
                row(info, "Conexões", a.optString("active_cons", "0") + " de " + a.optString("max_connections", "-"));
                row(info, "Criada em", date(a.optString("created_at", "")));
            }
        } else {
            row(info, "Lista", Repo.m3uUrl);
        }
        LinearLayout counts = card();
        row(counts, "Canais", String.valueOf(Repo.channels == null ? 0 : Repo.channels.size()));
        row(counts, "Filmes", Repo.movies == null ? "carregando…" : String.valueOf(Repo.movies.size()));
        row(counts, "Séries", Repo.series == null ? "carregando…" : String.valueOf(Repo.series.size()));

        TextView refresh = button("Atualizar listas", true);
        TextView logout = button("Sair da conta", false);
        logout.setTextColor(Palette.ERROR);
        buttons(refresh, logout);
        refresh.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { refreshAll(); }
        });
        logout.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { confirmLogout(); }
        });
    }

    private void parental() {
        heading("Controle parental", "Pastas e categorias adultas (+18) pedem senha. Senha padrão: 0000.");
        LinearLayout c = card();
        final TextView state = row(c, "Conteúdo adulto", Adult.hidden() ? "Oculto" : "Visível com senha");
        state.setTextColor(Adult.hidden() ? Palette.ERROR : 0xFF4CD787);
        row(c, "Senha", "••••");
        final TextView toggle = button(Adult.hidden() ? "Mostrar conteúdo adulto" : "Ocultar conteúdo adulto", true);
        TextView pin = button("Alterar senha", false);
        buttons(toggle, pin);
        toggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final boolean hide = !Adult.hidden();
                Adult.askPin(SettingsActivity.this, hide ? "Ocultar conteúdo adulto" : "Mostrar conteúdo adulto",
                        new Runnable() {
                            public void run() {
                                Adult.setHidden(hide);
                                section = -1;
                                show(1);
                                Toast.makeText(SettingsActivity.this, hide ? "Conteúdo adulto oculto em todo o app"
                                        : "Conteúdo adulto visível com senha", Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
        pin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { changePin(); }
        });
    }

    private void updates() {
        heading("Atualizações", "O app avisa sozinho ao abrir quando existe uma versão nova.");
        LinearLayout c = card();
        row(c, "Versão instalada", Updater.currentName(this));
        TextView check = button("Verificar atualizações", true);
        buttons(check);
        check.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { Updater.check(SettingsActivity.this, true); }
        });
    }

    private void about() {
        heading("Sobre", null);
        LinearLayout c = card();
        row(c, "Aplicativo", "Xplay " + Updater.currentName(this));
        row(c, "Aparelho", Build.MANUFACTURER + " " + Build.MODEL);
        row(c, "Android", Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
        DisplayMetrics m = getResources().getDisplayMetrics();
        row(c, "Tela", m.widthPixels + " × " + m.heightPixels);
        row(c, "Modo", Ui.isTv(this) ? "TV (controle remoto)" : "Celular / tablet");
    }

    // ---------------------------------------------------------------- connection test

    private void connection() {
        heading("Teste de conexão", "Mede sua internet e a resposta do servidor da sua lista.");
        final LinearLayout c = card();
        final TextView internet = row(c, "Internet", "—");
        final TextView server = row(c, "Servidor", "—");
        final TextView login = row(c, "Login / lista", "—");
        final TextView speed = row(c, "Velocidade de download", "—");
        final TextView verdict = Ui.text(this, "", 15, Palette.TEXT, false);
        verdict.setLineSpacing(0, 1.2f);
        LinearLayout.LayoutParams vl = Ui.lp(-1, -2);
        vl.bottomMargin = Ui.dp(this, 16);
        final ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setIndeterminate(true);
        bar.setIndeterminateTintList(android.content.res.ColorStateList.valueOf(Palette.GOLD));
        bar.setVisibility(View.GONE);
        content.addView(bar, Ui.lp(-1, Ui.dp(this, 6)));
        content.addView(verdict, vl);
        final TextView start = button("Iniciar teste", true);
        buttons(start);
        start.requestFocus();
        start.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (testing) return;
                testing = true;
                start.setText("Testando…");
                bar.setVisibility(View.VISIBLE);
                verdict.setText("");
                for (TextView t : new TextView[] {internet, server, login, speed}) {
                    t.setText("testando…");
                    t.setTextColor(Palette.TEXT_DIM);
                }
                runTest(internet, server, login, speed, verdict, start, bar);
            }
        });
    }

    private void runTest(final TextView internet, final TextView server, final TextView login, final TextView speed,
                         final TextView verdict, final TextView start, final ProgressBar bar) {
        new Thread(new Runnable() {
            public void run() {
                final long net = ping("http://connectivitycheck.gstatic.com/generate_204");
                post(internet, net >= 0 ? net + " ms" : "Sem acesso", net >= 0);
                String target = Repo.isXtream() ? Repo.server + "/player_api.php" : Repo.m3uUrl;
                final long srv = ping(target);
                post(server, srv >= 0 ? srv + " ms" : "Sem resposta", srv >= 0);
                boolean ok;
                String msg;
                try {
                    if (Repo.isXtream()) {
                        Xtream.login();
                        ok = true;
                        msg = "OK";
                    } else {
                        ok = srv >= 0;
                        msg = ok ? "OK" : "Lista inacessível";
                    }
                } catch (Exception e) {
                    ok = false;
                    msg = Task.message(e);
                }
                post(login, msg, ok);
                final double mbps = downloadMbps();
                post(speed, mbps > 0 ? String.format(Locale.US, "%.1f Mbps", mbps) : "Não foi possível medir", mbps > 0);
                final boolean loginOk = ok;
                runOnUiThread(new Runnable() {
                    public void run() {
                        testing = false;
                        bar.setVisibility(View.GONE);
                        start.setText("Testar novamente");
                        verdict.setText(summary(net, srv, loginOk, mbps));
                    }
                });
            }
        }, "connection-test").start();
    }

    private void post(final TextView v, final String text, final boolean good) {
        runOnUiThread(new Runnable() {
            public void run() {
                v.setText(text);
                v.setTextColor(good ? 0xFF4CD787 : Palette.ERROR);
            }
        });
    }

    private static String summary(long net, long srv, boolean login, double mbps) {
        if (net < 0) return "Sem internet. Verifique o Wi‑Fi ou o cabo de rede.";
        if (srv < 0) return "Sua internet funciona, mas o servidor da lista não responde. Fale com o seu provedor da lista.";
        if (!login) return "O servidor responde, mas o login falhou. Confira usuário, senha ou se a conta venceu.";
        String q;
        if (mbps <= 0) q = "Não deu para medir a velocidade, mas a conexão com o servidor está funcionando.";
        else if (mbps >= 25) q = "Conexão ótima: assiste até 4K.";
        else if (mbps >= 12) q = "Conexão boa: Full HD sem travar.";
        else if (mbps >= 6) q = "Conexão razoável: prefira canais HD.";
        else q = "Conexão fraca: prefira canais SD para evitar travamentos.";
        return q + (srv > 800 ? "\nO servidor está demorando para responder (" + srv + " ms)." : "");
    }

    /** Round trip of a small request in ms, or -1 when unreachable. */
    private static long ping(String address) {
        HttpURLConnection conn = null;
        try {
            long t0 = System.currentTimeMillis();
            conn = (HttpURLConnection) new URL(address).openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(6000);
            conn.setRequestProperty("User-Agent", "Xplay/2.0");
            conn.getResponseCode();
            return System.currentTimeMillis() - t0;
        } catch (Exception e) {
            return -1;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    /** Downloads for up to ~8 s (or 20 MB) and returns the speed in Mbps, or -1. */
    private static double downloadMbps() {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL("https://speed.cloudflare.com/__down?bytes=25000000").openConnection();
            conn.setConnectTimeout(6000);
            conn.setReadTimeout(8000);
            InputStream in = conn.getInputStream();
            byte[] buf = new byte[65536];
            long bytes = 0;
            long t0 = System.currentTimeMillis();
            int n;
            while ((n = in.read(buf)) > 0) {
                bytes += n;
                if (System.currentTimeMillis() - t0 > 8000 || bytes > 20000000) break;
            }
            in.close();
            double secs = Math.max(0.2, (System.currentTimeMillis() - t0) / 1000.0);
            return bytes * 8 / secs / 1000000.0;
        } catch (Exception e) {
            return -1;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    // ---------------------------------------------------------------- actions

    private void refreshAll() {
        Toast.makeText(this, "Atualizando listas…", Toast.LENGTH_SHORT).show();
        Task.run(this, new Callable<Void>() {
            public Void call() throws Exception {
                if (!Repo.isXtream()) {
                    M3uCatalog.load(Repo.m3uUrl);
                    return null;
                }
                Xtream.loadLive();
                Repo.movies = null;
                Repo.series = null;
                Xtream.loadMovies();
                Xtream.loadSeries();
                return null;
            }
        }, new Task.Done<Void>() {
            public void ok(Void v) {
                Toast.makeText(SettingsActivity.this, "Listas atualizadas", Toast.LENGTH_SHORT).show();
                section = -1;
                show(0);
            }

            public void fail(Exception e) {
                Toast.makeText(SettingsActivity.this, "Erro: " + Task.message(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle("Sair da conta?")
                .setMessage("Você poderá entrar com outro login ou lista.")
                .setPositiveButton("Sair", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        Repo.logout();
                        Intent i = new Intent(SettingsActivity.this, LoginActivity.class);
                        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(i);
                        finish();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void changePin() {
        Adult.askPin(this, "Senha atual", new Runnable() {
            public void run() {
                final EditText input = new EditText(SettingsActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
                input.setGravity(Gravity.CENTER);
                input.setTextSize(24);
                LinearLayout box = new LinearLayout(SettingsActivity.this);
                box.setPadding(Ui.dp(SettingsActivity.this, 24), Ui.dp(SettingsActivity.this, 8),
                        Ui.dp(SettingsActivity.this, 24), 0);
                box.addView(input, new LinearLayout.LayoutParams(-1, -2));
                new AlertDialog.Builder(SettingsActivity.this)
                        .setTitle("Nova senha (4 números ou mais)")
                        .setView(box)
                        .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                String pin = input.getText().toString();
                                if (pin.length() < 4) {
                                    Toast.makeText(SettingsActivity.this, "Use pelo menos 4 números", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                Adult.setPin(pin);
                                Toast.makeText(SettingsActivity.this, "Senha alterada", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
        });
    }
}
