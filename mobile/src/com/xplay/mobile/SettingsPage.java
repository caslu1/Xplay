package com.xplay.mobile;

import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/** Ajustes: profile, account, lists, player and about. */
public final class SettingsPage extends FrameLayout {

    static final String[] ASPECTS = {"Automático", "Preencher", "Esticar", "16:9", "4:3"};

    private final MainActivity a;
    private final LinearLayout col;

    public SettingsPage(MainActivity activity) {
        super(activity);
        a = activity;
        ScrollView scroll = new ScrollView(a);
        scroll.setClipToPadding(false);
        scroll.setPadding(0, 0, 0, a.navSpace());
        scroll.setVerticalScrollBarEnabled(false);
        col = Ui.column(a);
        int p = Ui.dp(a, 12);
        col.setPadding(p, 0, p, 0);
        scroll.addView(col);
        addView(scroll, Ui.frame(-1, -1, Gravity.TOP));
    }

    void refresh() {
        col.removeAllViews();
        TextView title = Ui.text(a, "Ajustes", 28, Theme.TEXT, Ui.BOLD);
        title.setPadding(Ui.dp(a, 8), Ui.dp(a, 22), 0, Ui.dp(a, 14));
        col.addView(title);

        // Profile card
        Repo.Profile p = Repo.profile();
        LinearLayout prof = Ui.row(a);
        prof.setBackground(Ui.round(Theme.SURFACE, Ui.dp(a, 18)));
        prof.setPadding(Ui.dp(a, 14), Ui.dp(a, 14), Ui.dp(a, 14), Ui.dp(a, 14));
        int s = Ui.dp(a, 56);
        prof.addView(Ui.avatar(a, p, s), Ui.lp(s, s));
        LinearLayout pt = Ui.column(a);
        pt.addView(Ui.text(a, p == null ? "Perfil" : p.name, 18, Theme.TEXT, Ui.MEDIUM));
        pt.addView(Ui.text(a, "Perfil atual", 13, Theme.TEXT_MUTED));
        LinearLayout.LayoutParams ptl = Ui.weight(1);
        ptl.leftMargin = Ui.dp(a, 14);
        prof.addView(pt, ptl);
        TextView sw = Sheet.button(a, "Trocar", false);
        sw.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { a.switchProfile(); }
        });
        prof.addView(sw);
        col.addView(prof, Ui.lp(-1, -2));

        section("Conta");
        LinearLayout box = group();
        if (Repo.isXtream()) {
            info(box, "Servidor", Repo.server);
            info(box, "Usuário", Repo.user);
            JSONObject acc = Repo.account;
            if (acc != null) {
                String exp = acc.optString("exp_date", "");
                if (exp.matches("\\d+")) {
                    info(box, "Vencimento", new SimpleDateFormat("dd/MM/yyyy", Locale.US)
                            .format(new Date(Long.parseLong(exp) * 1000L)));
                } else {
                    info(box, "Vencimento", "Sem vencimento");
                }
                String max = acc.optString("max_connections", "");
                if (!max.isEmpty() && !max.equals("null")) {
                    info(box, "Telas", acc.optString("active_cons", "0") + " em uso de " + max);
                }
                info(box, "Situação", "Active".equalsIgnoreCase(acc.optString("status")) ? "Ativa" : acc.optString("status"));
            }
        } else {
            info(box, "Lista M3U", Repo.m3uUrl);
        }
        info(box, "Conteúdo", count(Repo.channels) + " canais · " + count(Repo.movies) + " filmes · "
                + count(Repo.series) + " séries");

        section("Listas");
        LinearLayout lists = group();
        action(lists, R.drawable.ic_refresh, "Atualizar listas", "Baixa canais, filmes e séries de novo", new Runnable() {
            public void run() { a.reloadAll(); }
        });
        action(lists, R.drawable.ic_swap, "Trocar login / lista", "Entrar com outro Xtream ou link M3U", new Runnable() {
            public void run() { a.editLogin(); }
        });

        section("Perfis");
        LinearLayout profiles = group();
        action(profiles, R.drawable.ic_person, "Trocar de perfil", null, new Runnable() {
            public void run() { a.switchProfile(); }
        });
        action(profiles, R.drawable.ic_edit, "Gerenciar perfis", "Nomes, ícones e cores", new Runnable() {
            public void run() {
                android.content.Intent i = new android.content.Intent(a, ProfilesActivity.class);
                i.putExtra(ProfilesActivity.EXTRA_MANAGE, true);
                a.startActivity(i);
                a.finish();
            }
        });
        action(profiles, R.drawable.ic_close, "Limpar \"Continuar assistindo\"", "Só deste perfil", new Runnable() {
            public void run() {
                Sheet.confirm(a, "Limpar o histórico?", "\"Continuar assistindo\" deste perfil fica vazio.", "Limpar",
                        new Runnable() {
                            public void run() {
                                for (JSONObject e : Repo.continueList()) Repo.removeProgress(e.optString("key"));
                                a.refreshHome();
                                Ui.toast(a, "Histórico limpo");
                            }
                        });
            }
        });

        section("Reprodução");
        LinearLayout play = group();
        action(play, R.drawable.ic_aspect, "Formato da tela", ASPECTS[Repo.aspectMode()], new Runnable() {
            public void run() {
                Sheet.options(a, "Formato da tela", Arrays.asList(ASPECTS), Repo.aspectMode(), new Sheet.Pick() {
                    public void picked(int i) {
                        Repo.setAspectMode(i);
                        refresh();
                    }
                });
            }
        });
        action(play, R.drawable.ic_lock, "Conteúdo adulto", Repo.showAdult() ? "Visível" : "Oculto", new Runnable() {
            public void run() {
                if (Repo.showAdult()) {
                    Repo.setShowAdult(false);
                    refresh();
                    a.reloadPages();
                    return;
                }
                Sheet.confirm(a, "Mostrar conteúdo adulto?", "Categorias adultas passam a aparecer em Filmes, "
                        + "Séries e Ao vivo.", "Mostrar", new Runnable() {
                            public void run() {
                                Repo.setShowAdult(true);
                                refresh();
                                a.reloadPages();
                            }
                        });
            }
        });

        section("Sobre");
        LinearLayout about = group();
        String version = "";
        try {
            version = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
        } catch (Exception ignored) {
            // Leave blank.
        }
        info(about, "Versão", version);
        action(about, R.drawable.ic_logout, "Sair", "Volta para a tela de login", new Runnable() {
            public void run() {
                Sheet.confirm(a, "Sair da conta?", "Os perfis e favoritos ficam salvos neste aparelho.", "Sair",
                        new Runnable() {
                            public void run() {
                                Repo.logout();
                                android.content.Intent i = new android.content.Intent(a, LoginActivity.class);
                                i.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                                        | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                a.startActivity(i);
                            }
                        });
            }
        });
    }

    private static int count(java.util.List<?> l) {
        return l == null ? 0 : l.size();
    }

    private void section(String name) {
        TextView t = Ui.text(a, name.toUpperCase(new Locale("pt", "BR")), 12, Theme.TEXT_MUTED, Ui.MEDIUM);
        t.setLetterSpacing(0.08f);
        t.setPadding(Ui.dp(a, 10), Ui.dp(a, 24), 0, Ui.dp(a, 8));
        col.addView(t);
    }

    private LinearLayout group() {
        LinearLayout g = Ui.column(a);
        g.setBackground(Ui.round(Theme.SURFACE, Ui.dp(a, 18)));
        g.setPadding(0, Ui.dp(a, 4), 0, Ui.dp(a, 4));
        col.addView(g, Ui.lp(-1, -2));
        return g;
    }

    private void info(LinearLayout box, String label, String value) {
        LinearLayout row = Ui.row(a);
        row.setPadding(Ui.dp(a, 16), Ui.dp(a, 12), Ui.dp(a, 16), Ui.dp(a, 12));
        row.addView(Ui.text(a, label, 15, Theme.TEXT_DIM));
        TextView v = Ui.oneLine(Ui.text(a, value == null ? "" : value, 15, Theme.TEXT));
        v.setGravity(Gravity.END);
        v.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        LinearLayout.LayoutParams l = Ui.weight(1);
        l.leftMargin = Ui.dp(a, 16);
        row.addView(v, l);
        box.addView(row);
    }

    private void action(LinearLayout box, int icon, String title, String detail, final Runnable r) {
        LinearLayout row = Sheet.listRow(a, icon, title, detail);
        row.addView(Ui.icon(a, R.drawable.ic_chevron_right, Theme.TEXT_MUTED), Ui.lp(Ui.dp(a, 22), Ui.dp(a, 22)));
        row.setOnClickListener(new OnClickListener() {
            public void onClick(View v) { r.run(); }
        });
        box.addView(row);
    }
}
