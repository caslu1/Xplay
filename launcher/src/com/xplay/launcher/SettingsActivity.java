package com.xplay.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Callable;

/** Launcher settings: device code and plan, panel sync, Android settings shortcuts and updates. */
public class SettingsActivity extends Activity {

    private LinearLayout list;

    /** Opens the settings, asking first for the PIN set in the panel (if any). */
    public static void open(final Activity a) {
        final String pin = Config.get(a).settings().optString("settingsPin", "");
        if (pin.isEmpty()) {
            a.startActivity(new Intent(a, SettingsActivity.class));
            return;
        }
        final EditText input = new EditText(a);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        input.setHint("PIN");
        new AlertDialog.Builder(a, R.style.DialogTheme)
                .setTitle("Digite o PIN")
                .setView(input)
                .setPositiveButton("Entrar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        if (pin.equals(input.getText().toString().trim())) {
                            a.startActivity(new Intent(a, SettingsActivity.class));
                        } else {
                            Toast.makeText(a, "PIN incorreto", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        Ui.applyTheme(Config.get(this));
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(0xFF05080F);
        int p = Ui.dp(this, 36);
        root.setPadding(p, Ui.dp(this, 16), p, 0);
        setContentView(root);

        LinearLayout header = Ui.row(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = Ui.iconButton(this, R.drawable.ic_back);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        header.addView(back, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        LinearLayout.LayoutParams hl = Ui.lp(-2, -2);
        hl.leftMargin = Ui.dp(this, 16);
        header.addView(Ui.text(this, "Configurações", 24, Ui.TEXT, true), hl);
        root.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 24));
        list = Ui.column(this);
        scroll.addView(list);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        fill();
    }

    private void fill() {
        list.removeAllViews();
        final Config cfg = Config.get(this);
        Device.Access access = Device.check(this, cfg);

        section("Aparelho");
        row("Código do aparelho", Device.id(this) + "  ·  cadastre este código no painel", null);
        String status;
        if (!access.allowed) status = "Bloqueado — " + access.reason.split("\n")[0];
        else if (access.entry != null) {
            JSONObject e = access.entry;
            String exp = e.optString("expires", "");
            status = "Ativo" + (e.optString("plan").isEmpty() ? "" : " · plano " + e.optString("plan"))
                    + (exp.isEmpty() ? "" : " · vence em " + Device.br(exp));
        } else status = "Liberado";
        row("Situação", status, null);

        section("Painel");
        String when = cfg.fetchedAt == 0 ? "nunca (usando a configuração de fábrica)"
                : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(cfg.fetchedAt));
        row("Sincronizar agora", "Última sincronização: " + when, new Runnable() {
            public void run() { sync(); }
        });
        row("Origem da configuração", Config.sourceLabel(this), new Runnable() {
            public void run() { editSource(); }
        });

        section("Sistema");
        row("Wi‑Fi e rede", "Conectar a uma rede", new Runnable() {
            public void run() { Actions.system(SettingsActivity.this, Settings.ACTION_WIFI_SETTINGS); }
        });
        row("Configurações do Android", "Tela, som, data e hora, contas…", new Runnable() {
            public void run() { Actions.system(SettingsActivity.this, Settings.ACTION_SETTINGS); }
        });
        row("Launcher padrão", "Escolher qual tela inicial o aparelho usa", new Runnable() {
            public void run() { Actions.system(SettingsActivity.this, Settings.ACTION_HOME_SETTINGS); }
        });
        row("Todos os apps", "Abrir, ver informações ou desinstalar", new Runnable() {
            public void run() { startActivity(new Intent(SettingsActivity.this, AppsActivity.class)); }
        });
        row("Limpar meus apps", "Remove da tela inicial os apps adicionados com +", new Runnable() {
            public void run() {
                Apps.clearPinned(SettingsActivity.this);
                Toast.makeText(SettingsActivity.this, "Apps removidos da tela inicial", Toast.LENGTH_SHORT).show();
            }
        });

        section("Sobre");
        row("Verificar atualização", cfg.name() + " versão " + Installer.currentName(this), new Runnable() {
            public void run() { Installer.checkUpdate(SettingsActivity.this, true); }
        });
        String support = cfg.settings().optString("support", "");
        if (!support.isEmpty()) row("Suporte", support, null);

        if (list.getChildCount() > 1) {
            for (int i = 0; i < list.getChildCount(); i++) {
                View v = list.getChildAt(i);
                if (v.isFocusable()) {
                    v.requestFocus();
                    break;
                }
            }
        }
    }

    private void section(String title) {
        TextView t = Ui.text(this, title.toUpperCase(), 12, Ui.MUTED, true);
        t.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams l = Ui.lp(-2, -2);
        l.topMargin = Ui.dp(this, 18);
        l.bottomMargin = Ui.dp(this, 6);
        list.addView(t, l);
    }

    private void row(String title, String subtitle, final Runnable action) {
        LinearLayout r = Ui.column(this);
        int h = Ui.dp(this, 18), v = Ui.dp(this, 12);
        r.setPadding(h, v, h, v);
        r.setBackground(Ui.card(this, 0xFF111827, Ui.dp(this, 12)));
        r.addView(Ui.text(this, title, 16, Ui.TEXT, true));
        r.addView(Ui.text(this, subtitle, 13, Ui.MUTED, false));
        r.setFocusable(true);
        if (action != null) {
            r.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) { action.run(); }
            });
        }
        LinearLayout.LayoutParams l = Ui.lp(-1, -2);
        l.bottomMargin = Ui.dp(this, 8);
        list.addView(r, l);
    }

    private void sync() {
        Toast.makeText(this, "Sincronizando…", Toast.LENGTH_SHORT).show();
        Task.run(this, new Callable<Config>() {
            public Config call() throws Exception {
                return Config.fetch(SettingsActivity.this);
            }
        }, new Task.Done<Config>() {
            public void ok(Config c) {
                Ui.applyTheme(c);
                fill();
                Toast.makeText(SettingsActivity.this, "Configuração atualizada", Toast.LENGTH_SHORT).show();
            }

            public void fail(Exception e) {
                Toast.makeText(SettingsActivity.this, "Falhou: " + Task.message(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void editSource() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(Config.customUrl(this));
        input.setHint("Vazio = repositório padrão");
        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle("Endereço do config.json")
                .setView(input)
                .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        Config.setCustomUrl(SettingsActivity.this, input.getText().toString());
                        sync();
                    }
                })
                .setNeutralButton("Padrão", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        Config.setCustomUrl(SettingsActivity.this, "");
                        sync();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
