package com.ilha.notch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private static final int BG = 0xFF0E0E12, CARD = 0xFF1A1A21, ACCENT = 0xFF7CF2C4, GREY = 0xFF9C9CA6;

    private float dp;
    private LinearLayout list;
    private TextView notifStatus, a11yStatus;
    private View notifBtn, a11yBtn;
    private LinearLayout appsBox;
    private final List<Runnable> calRefresh = new ArrayList<>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(px(18), px(28), px(18), px(40));
        sv.addView(list);
        setContentView(sv);

        header();
        permissions();
        calibration();
        features();
        apps();
        TextView foot = text("Ilha não envia nada para a internet: tudo fica no seu aparelho.", 12, GREY, false);
        foot.setGravity(Gravity.CENTER);
        foot.setPadding(0, px(20), 0, 0);
        list.addView(foot);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
        refreshApps();
        if (Hub.island != null) Hub.island.setCalibrating(true);
        for (Runnable r : calRefresh) r.run();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (Hub.island != null) Hub.island.setCalibrating(false);
    }

    // ------------------------------------------------------------------ sections

    private void header() {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        ImageView ic = new ImageView(this);
        ic.setImageResource(R.drawable.ic_launcher);
        row.addView(ic, new LinearLayout.LayoutParams(px(52), px(52)));
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setPadding(px(14), 0, 0, 0);
        t.addView(text("Ilha", 28, 0xFFFFFFFF, true));
        t.addView(text("Música, chamadas e mensagens ao redor da câmera.", 13.5f, GREY, false));
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        list.addView(row);
    }

    private void permissions() {
        LinearLayout c = card("1. Permissões");
        c.addView(text("O Ilha precisa de duas permissões. Nada sai do seu celular.", 13, GREY, false));

        notifStatus = text("", 14, 0xFFFFFFFF, true);
        notifBtn = pill("Ativar", true, new Runnable() {
            @Override public void run() {
                startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
            }
        });
        c.addView(statusRow(notifStatus, "Acesso às notificações: para ler mensagens, músicas e chamadas.", notifBtn));

        a11yStatus = text("", 14, 0xFFFFFFFF, true);
        a11yBtn = pill("Ativar", true, new Runnable() {
            @Override public void run() {
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                Toast.makeText(MainActivity.this, "Toque em \"Ilha\" (em Apps instalados) e ative.", Toast.LENGTH_LONG).show();
            }
        });
        c.addView(statusRow(a11yStatus, "Acessibilidade: só para desenhar a ilha por cima da barra de status.", a11yBtn));

        TextView tip = text("Opção cinza ou bloqueada? No Android 13 ou superior, abra \"Info do app\" → menu ⋮ → \"Permitir configurações restritas\" e tente de novo.", 12.5f, GREY, false);
        tip.setSingleLine(false);
        tip.setPadding(0, px(12), 0, px(8));
        c.addView(tip);
        LinearLayout row = new LinearLayout(this);
        row.addView(pill("Info do app", false, new Runnable() {
            @Override public void run() {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
            }
        }));
        View gap = new View(this);
        row.addView(gap, new LinearLayout.LayoutParams(px(8), 1));
        row.addView(pill("Testar a ilha", true, new Runnable() {
            @Override public void run() {
                if (Hub.island == null) {
                    Toast.makeText(MainActivity.this, "Ative a acessibilidade primeiro.", Toast.LENGTH_SHORT).show();
                } else {
                    Hub.island.showTest();
                }
            }
        }));
        c.addView(row);
    }

    private void calibration() {
        LinearLayout c = card("2. Encaixe na câmera");
        TextView t = text("Enquanto esta tela estiver aberta, a ilha aparece com um anel verde: ajuste até o anel ficar em volta da câmera.", 13, GREY, false);
        t.setSingleLine(false);
        c.addView(t);
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, px(12), 0, px(4));
        row.addView(pill("Detectar automaticamente", true, new Runnable() {
            @Override public void run() {
                if (Hub.island == null) {
                    Toast.makeText(MainActivity.this, "Ative a acessibilidade primeiro.", Toast.LENGTH_SHORT).show();
                } else if (Hub.island.detectCutout()) {
                    Toast.makeText(MainActivity.this, "Câmera encontrada!", Toast.LENGTH_SHORT).show();
                    for (Runnable r : calRefresh) r.run();
                } else {
                    Toast.makeText(MainActivity.this, "Não deu para detectar. Ajuste manualmente.", Toast.LENGTH_LONG).show();
                }
            }
        }));
        c.addView(row);

        DisplayMetrics m = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(m);
        final int half = m.widthPixels / 2;
        final int sb = statusBar();

        slider(c, "Posição horizontal", -half, half, new Getter() {
            public int get() { return Prefs.camX(MainActivity.this); }
        }, new Setter() {
            public void set(int v) { saveCam(v, camY(sb), camD(sb)); }
        });
        slider(c, "Posição vertical", 0, Math.max(sb * 2, px(80)), new Getter() {
            public int get() { return camY(sb); }
        }, new Setter() {
            public void set(int v) { saveCam(Prefs.camX(MainActivity.this), v, camD(sb)); }
        });
        slider(c, "Tamanho da câmera", px(6), px(70), new Getter() {
            public int get() { return camD(sb); }
        }, new Setter() {
            public void set(int v) { saveCam(Prefs.camX(MainActivity.this), camY(sb), v); }
        });
        slider(c, "Largura da ilha", 90, 320, new Getter() {
            public int get() { return Prefs.pillW(MainActivity.this); }
        }, new Setter() {
            public void set(int v) {
                Prefs.get(MainActivity.this).edit().putInt("pill_w", v).apply();
                if (Hub.island != null) Hub.island.reloadGeometry();
            }
        });
    }

    private int camY(int sb) { int y = Prefs.camY(this); return y >= 0 ? y : sb / 2; }
    private int camD(int sb) { int d = Prefs.camD(this); return d > 0 ? d : Math.round(sb * 0.55f); }

    private void saveCam(int x, int y, int d) {
        Prefs.setCamera(this, x, y, d);
        if (Hub.island != null) Hub.island.reloadGeometry();
    }

    private void features() {
        LinearLayout c = card("3. O que mostrar");
        toggle(c, "Música", "Capa, barras animadas e controles.", "music", true);
        toggle(c, "Notificações", "Mensagens com resposta rápida.", "notifs", true);
        toggle(c, "Atividades ao vivo", "Timers, downloads, navegação, entregas.", "live", true);
        toggle(c, "Chamadas", "Atender e recusar pela ilha.", "calls", true);
        toggle(c, "Vibrar", "Vibração curta a cada notificação.", "vibrate", false);
        toggle(c, "Esconder na horizontal", "Some em vídeos e jogos deitados.", "hide_land", true);
        slider(c, "Tempo aberta (segundos)", 2, 15, new Getter() {
            public int get() { return Prefs.duration(MainActivity.this); }
        }, new Setter() {
            public void set(int v) { Prefs.get(MainActivity.this).edit().putInt("duration", v).apply(); }
        });
        TextView t = text("Toque na ilha para abrir. Deslize para cima para fechar. Toque e segure para abrir o app. Com duas atividades, toque na bolinha ao lado para trocar.", 12.5f, GREY, false);
        t.setSingleLine(false);
        t.setPadding(0, px(10), 0, 0);
        c.addView(t);
    }

    private void apps() {
        LinearLayout c = card("4. Apps");
        TextView t = text("Escolha quais apps aparecem na ilha. Para não ver a mesma notificação duas vezes, desligue o pop-up do próprio sistema: toque em \"Pop-up\" e desative \"Mostrar como pop-up\" / \"Notificações flutuantes\".", 13, GREY, false);
        t.setSingleLine(false);
        c.addView(t);
        appsBox = new LinearLayout(this);
        appsBox.setOrientation(LinearLayout.VERTICAL);
        c.addView(appsBox);
    }

    private void refreshApps() {
        appsBox.removeAllViews();
        final PackageManager pm = getPackageManager();
        List<String> pkgs = new ArrayList<>(Prefs.seen(this));
        final List<String> labels = new ArrayList<>();
        Collections.sort(pkgs, new Comparator<String>() {
            @Override public int compare(String a, String b) { return label(pm, a).compareToIgnoreCase(label(pm, b)); }
        });
        if (pkgs.isEmpty()) {
            TextView e = text("Os apps aparecem aqui conforme mandam notificações.", 13, 0xFFFFFFFF, false);
            e.setSingleLine(false);
            e.setPadding(0, px(12), 0, 0);
            appsBox.addView(e);
            return;
        }
        for (final String pkg : pkgs) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, px(12), 0, 0);
            ImageView ic = new ImageView(this);
            try { ic.setImageDrawable(pm.getApplicationIcon(pkg)); } catch (Exception ignored) {}
            row.addView(ic, new LinearLayout.LayoutParams(px(30), px(30)));
            TextView name = text(label(pm, pkg), 14.5f, 0xFFFFFFFF, false);
            name.setPadding(px(12), 0, px(8), 0);
            row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            row.addView(pill("Pop-up", false, new Runnable() {
                @Override public void run() { openNotifSettings(pkg); }
            }));
            Switch sw = new Switch(this);
            tint(sw);
            sw.setChecked(Prefs.appAllowed(this, pkg));
            sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override public void onCheckedChanged(CompoundButton b, boolean on) {
                    Prefs.setAppAllowed(MainActivity.this, pkg, on);
                    Hub.requestResync();
                }
            });
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sp.leftMargin = px(8);
            row.addView(sw, sp);
            appsBox.addView(row);
        }
    }

    private void openNotifSettings(String pkg) {
        Intent i = new Intent("android.settings.APP_NOTIFICATION_SETTINGS");
        i.putExtra("android.provider.extra.APP_PACKAGE", pkg);
        i.putExtra("app_package", pkg);
        try {
            ApplicationInfo ai = getPackageManager().getApplicationInfo(pkg, 0);
            i.putExtra("app_uid", ai.uid);
        } catch (Exception ignored) {
        }
        try {
            startActivity(i);
        } catch (Exception e) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + pkg)));
        }
    }

    private static String label(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private void refreshStatus() {
        boolean n = notifEnabled(), a = a11yEnabled();
        notifStatus.setText(n ? "✓ Notificações" : "Notificações");
        notifStatus.setTextColor(n ? ACCENT : 0xFFFFFFFF);
        notifBtn.setVisibility(n ? View.GONE : View.VISIBLE);
        a11yStatus.setText(a ? "✓ Acessibilidade" : "Acessibilidade");
        a11yStatus.setTextColor(a ? ACCENT : 0xFFFFFFFF);
        a11yBtn.setVisibility(a ? View.GONE : View.VISIBLE);
    }

    private boolean notifEnabled() {
        String s = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(new ComponentName(this, NotifService.class).flattenToString());
    }

    private boolean a11yEnabled() {
        String s = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return s != null && s.contains(getPackageName() + "/");
    }

    private int statusBar() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : px(24);
    }

    // ------------------------------------------------------------------ building blocks

    interface Getter { int get(); }
    interface Setter { void set(int v); }

    private int px(float v) { return Math.round(v * dp); }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        t.setLineSpacing(px(2), 1);
        return t;
    }

    private LinearLayout card(String title) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(Widgets.round(CARD, px(22)));
        c.setPadding(px(18), px(16), px(18), px(18));
        c.addView(text(title, 17, 0xFFFFFFFF, true), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((LinearLayout.LayoutParams) c.getChildAt(0).getLayoutParams()).bottomMargin = px(6);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p.topMargin = px(18);
        list.addView(c, p);
        return c;
    }

    private TextView pill(String label, boolean primary, final Runnable r) {
        TextView b = text(label, 13.5f, primary ? 0xFF0E0E12 : 0xFFFFFFFF, true);
        b.setGravity(Gravity.CENTER);
        b.setBackground(Widgets.round(primary ? ACCENT : 0x22FFFFFF, px(20)));
        b.setPadding(px(16), px(9), px(16), px(9));
        b.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { r.run(); }
        });
        return b;
    }

    private View statusRow(TextView status, String desc, View btn) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, px(14), 0, 0);
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(status);
        TextView d = text(desc, 12.5f, GREY, false);
        d.setPadding(0, px(2), px(10), 0);
        t.addView(d);
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(btn);
        return row;
    }

    private void tint(Switch sw) {
        int[][] states = {{android.R.attr.state_checked}, {}};
        sw.setThumbTintList(new ColorStateList(states, new int[]{ACCENT, 0xFFBDBDC3}));
        sw.setTrackTintList(new ColorStateList(states, new int[]{0x887CF2C4, 0x44FFFFFF}));
    }

    private void toggle(LinearLayout c, String title, String desc, final String key, boolean def) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, px(12), 0, 0);
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.addView(text(title, 15, 0xFFFFFFFF, false));
        t.addView(text(desc, 12.5f, GREY, false));
        row.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Switch sw = new Switch(this);
        tint(sw);
        sw.setChecked(Prefs.get(this).getBoolean(key, def));
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override public void onCheckedChanged(CompoundButton b, boolean on) {
                Prefs.get(MainActivity.this).edit().putBoolean(key, on).apply();
                Hub.requestResync();
                if (Hub.island != null) Hub.island.reloadGeometry();
            }
        });
        row.addView(sw);
        c.addView(row);
    }

    private void slider(LinearLayout c, String title, final int min, int max, final Getter g, final Setter s) {
        LinearLayout row = new LinearLayout(this);
        row.setPadding(0, px(12), 0, 0);
        row.addView(text(title, 14.5f, 0xFFFFFFFF, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        final TextView val = text("", 13, ACCENT, true);
        row.addView(val);
        c.addView(row);

        LinearLayout ctl = new LinearLayout(this);
        ctl.setGravity(Gravity.CENTER_VERTICAL);
        final SeekBar sb = new SeekBar(this);
        sb.setMax(max - min);
        sb.setProgressTintList(ColorStateList.valueOf(ACCENT));
        sb.setThumbTintList(ColorStateList.valueOf(ACCENT));
        sb.setProgressBackgroundTintList(ColorStateList.valueOf(0x44FFFFFF));
        TextView minus = pill("−", false, new Runnable() {
            @Override public void run() { sb.setProgress(sb.getProgress() - 1); s.set(sb.getProgress() + min); }
        });
        TextView plus = pill("+", false, new Runnable() {
            @Override public void run() { sb.setProgress(sb.getProgress() + 1); s.set(sb.getProgress() + min); }
        });
        ctl.addView(minus);
        ctl.addView(sb, new LinearLayout.LayoutParams(0, px(36), 1));
        ctl.addView(plus);
        c.addView(ctl);

        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int p, boolean user) {
                val.setText(String.valueOf(p + min));
                if (user) s.set(p + min);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) {}
            @Override public void onStopTrackingTouch(SeekBar bar) {}
        });
        Runnable refresh = new Runnable() {
            @Override public void run() {
                sb.setProgress(g.get() - min);
                val.setText(String.valueOf(g.get()));
            }
        };
        refresh.run();
        calRefresh.add(refresh);
    }
}
