package com.xplay.mobile;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** "Quem está assistindo?": pick, add or edit profiles. */
public class ProfilesActivity extends Activity {

    static final String EXTRA_MANAGE = "manage";

    private static final String[] EMOJIS = {
            "🍿", "🎬", "🦁", "🐼", "🦊", "🐸", "🐱", "🐶", "🦄", "🚀", "⚽", "🎮", "👑", "🌟", "🍕", "🐙",
            "🤖", "👻", "🐵", "🐯", "🌈", "🎧", "🏀", "🦖",
    };

    private boolean manage;
    private GridLayout grid;
    private TextView manageButton, title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        if (!Repo.hasLogin()) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        Catalog.start();
        manage = getIntent().getBooleanExtra(EXTRA_MANAGE, false);
        setContentView(build());
        refresh();
        if (Repo.profiles().isEmpty()) edit(null);
    }

    private View build() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        FrameLayout center = new FrameLayout(this);
        scroll.addView(center);
        LinearLayout col = Ui.column(this);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Ui.dp(this, 24);
        col.setPadding(pad, pad, pad, pad);
        center.addView(col, Ui.frame(-1, -2, Gravity.CENTER));

        col.addView(Ui.logo(this, 34));
        title = Ui.text(this, "Quem está assistindo?", 22, 0xFFDDDDE0);
        col.addView(title, Ui.lp(-2, -2, 0, Ui.dp(this, 28), 0, Ui.dp(this, 40)));

        grid = new GridLayout(this);
        grid.setColumnCount(Ui.landscape(this) ? 5 : 2);
        col.addView(grid, Ui.lp(-2, -2));

        manageButton = Sheet.button(this, "", false);
        manageButton.setBackground(Ui.ripple(Ui.stroke(0, Ui.dp(this, 6), Ui.dp(this, 1), 0xAAFFFFFF),
                Ui.dp(this, 6)));
        manageButton.setPadding(Ui.dp(this, 44), Ui.dp(this, 14), Ui.dp(this, 44), Ui.dp(this, 14));
        manageButton.setTextSize(16);
        col.addView(manageButton, Ui.lp(-2, -2, 0, Ui.dp(this, 36), 0, 0));
        manageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                manage = !manage;
                refresh();
            }
        });
        return scroll;
    }

    private void refresh() {
        grid.removeAllViews();
        manageButton.setText(manage ? "Concluído" : "Gerenciar perfis");
        title.setText(manage ? "Gerenciar perfis" : "Quem está assistindo?");
        for (final Repo.Profile p : Repo.profiles()) {
            View tile = tile(p);
            tile.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    if (manage) edit(p);
                    else open(p);
                }
            });
            grid.addView(tile);
        }
        if (Repo.profiles().size() < 6) {
            View add = tile(null);
            add.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { edit(null); }
            });
            grid.addView(add);
        }
        manageButton.setVisibility(Repo.profiles().isEmpty() ? View.GONE : View.VISIBLE);
    }

    private View tile(Repo.Profile p) {
        int size = Ui.dp(this, 112);
        LinearLayout t = Ui.column(this);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        t.setClickable(true);
        t.setFocusable(true);
        t.setBackground(Ui.pressable(this, 0, Ui.dp(this, 16)));
        int m = Ui.dp(this, 8);
        t.setPadding(m, m, m, m);
        FrameLayout box;
        if (p == null) {
            box = new FrameLayout(this);
            box.setBackground(Ui.stroke(0xFF222226, Ui.dp(this, 12), Ui.dp(this, 2), 0xFF5A5A60));
            ImageView plus = Ui.icon(this, R.drawable.ic_add, Theme.TEXT);
            box.addView(plus, Ui.frame(Ui.dp(this, 58), Ui.dp(this, 58), Gravity.CENTER));
        } else {
            box = Ui.avatar(this, p, size);
            if (manage) {
                View shade = new View(this);
                shade.setBackground(Ui.round(0x99000000, size * 0.12f));
                box.addView(shade, Ui.frame(-1, -1, Gravity.CENTER));
                box.addView(Ui.icon(this, R.drawable.ic_edit, Theme.TEXT), Ui.frame(Ui.dp(this, 34), Ui.dp(this, 34),
                        Gravity.CENTER));
            }
        }
        t.addView(box, Ui.lp(size, size));
        TextView name = Ui.oneLine(Ui.text(this, p == null ? "Adicionar perfil" : p.name, 16, 0xFFB8B8BE, Ui.MEDIUM));
        name.setGravity(Gravity.CENTER);
        name.setMaxWidth(size + Ui.dp(this, 40));
        t.addView(name, Ui.lp(-2, -2, 0, Ui.dp(this, 12), 0, 0));
        GridLayout.LayoutParams gl = new GridLayout.LayoutParams();
        gl.setMargins(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 10));
        t.setLayoutParams(gl);
        return t;
    }

    private void open(Repo.Profile p) {
        Repo.selectProfile(p);
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    /** Add (p == null) or edit a profile: name, emoji and color. */
    private void edit(final Repo.Profile p) {
        final String[] emoji = {p == null ? EMOJIS[Repo.profiles().size() % EMOJIS.length] : p.emoji};
        final int[] color = {p == null ? Theme.AVATAR_BG[Repo.profiles().size() % Theme.AVATAR_BG.length] : p.color};

        LinearLayout col = Ui.column(this);
        int pad = Ui.dp(this, 22);
        col.setPadding(pad, pad, pad, Ui.dp(this, 16));
        col.addView(Ui.text(this, p == null ? "Novo perfil" : "Editar perfil", 19, Theme.TEXT, Ui.BOLD));

        LinearLayout top = Ui.row(this);
        final FrameLayout preview = new FrameLayout(this);
        top.addView(preview, Ui.lp(Ui.dp(this, 64), Ui.dp(this, 64)));
        final EditText name = Sheet.field(this, "Nome", p == null ? "" : p.name,
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        LinearLayout.LayoutParams nl = Ui.weight(1);
        nl.leftMargin = Ui.dp(this, 14);
        top.addView(name, nl);
        col.addView(top, Ui.lp(-1, -2, 0, Ui.dp(this, 18), 0, 0));

        final Runnable updatePreview = new Runnable() {
            public void run() {
                preview.removeAllViews();
                Repo.Profile fake = new Repo.Profile("", "", emoji[0], color[0]);
                preview.addView(Ui.avatar(ProfilesActivity.this, fake, Ui.dp(ProfilesActivity.this, 64)),
                        Ui.frame(-1, -1, Gravity.CENTER));
            }
        };
        updatePreview.run();

        col.addView(Ui.text(this, "Ícone", 14, Theme.TEXT_DIM), Ui.lp(-2, -2, 0, Ui.dp(this, 18), 0, Ui.dp(this, 6)));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout emojis = Ui.row(this);
        hs.addView(emojis);
        for (final String e : EMOJIS) {
            TextView t = Ui.text(this, e, 26, Theme.TEXT);
            t.setGravity(Gravity.CENTER);
            t.setClickable(true);
            t.setFocusable(true);
            t.setBackground(Ui.pressable(this, Theme.SURFACE_HI, Ui.dp(this, 12)));
            t.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    emoji[0] = e;
                    updatePreview.run();
                }
            });
            emojis.addView(t, Ui.lp(Ui.dp(this, 50), Ui.dp(this, 50), 0, 0, Ui.dp(this, 8), 0));
        }
        col.addView(hs, Ui.lp(-1, -2));

        col.addView(Ui.text(this, "Cor", 14, Theme.TEXT_DIM), Ui.lp(-2, -2, 0, Ui.dp(this, 16), 0, Ui.dp(this, 6)));
        LinearLayout colors = Ui.row(this);
        for (final int c : Theme.AVATAR_BG) {
            View v = new View(this);
            v.setClickable(true);
            v.setFocusable(true);
            v.setBackground(Ui.ripple(Ui.stroke(c, Ui.dp(this, 20), Ui.dp(this, 1), 0x55FFFFFF), Ui.dp(this, 20)));
            v.setOnClickListener(new View.OnClickListener() {
                public void onClick(View x) {
                    color[0] = c;
                    updatePreview.run();
                }
            });
            colors.addView(v, Ui.lp(Ui.dp(this, 38), Ui.dp(this, 38), 0, 0, Ui.dp(this, 10), 0));
        }
        col.addView(colors, Ui.lp(-1, -2));

        LinearLayout buttons = Ui.row(this);
        final Dialog[] d = new Dialog[1];
        if (p != null) {
            TextView del = Sheet.button(this, "Excluir", false);
            del.setTextColor(Theme.ERROR);
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Sheet.confirm(ProfilesActivity.this, "Excluir " + p.name + "?",
                            "Os favoritos e o histórico deste perfil serão apagados.", "Excluir", new Runnable() {
                                public void run() {
                                    d[0].dismiss();
                                    Repo.deleteProfile(p);
                                    if (Repo.profiles().isEmpty()) manage = false;
                                    refresh();
                                }
                            });
                }
            });
            buttons.addView(del);
        }
        View gap = new View(this);
        buttons.addView(gap, Ui.weight(1));
        TextView cancel = Sheet.button(this, "Cancelar", false);
        TextView save = Sheet.button(this, "Salvar", true);
        buttons.addView(cancel);
        LinearLayout.LayoutParams sl = Ui.lp(-2, -2);
        sl.leftMargin = Ui.dp(this, 10);
        buttons.addView(save, sl);
        col.addView(buttons, Ui.lp(-1, -2, 0, Ui.dp(this, 24), 0, 0));

        cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { d[0].dismiss(); }
        });
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String n = name.getText().toString().trim();
                if (n.isEmpty()) {
                    name.setError("Digite um nome");
                    return;
                }
                if (n.length() > 20) n = n.substring(0, 20);
                if (p == null) Repo.addProfile(n, emoji[0], color[0]);
                else Repo.updateProfile(p, n, emoji[0], color[0]);
                d[0].dismiss();
                refresh();
            }
        });
        ScrollView sv = new ScrollView(this);
        sv.addView(col);
        d[0] = Sheet.show(this, sv);
        d[0].getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
}
