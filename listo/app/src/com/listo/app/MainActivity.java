package com.listo.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity {

    private static final int ACCENT = MonthView.ACCENT;
    private static final int TEXT = 0xFF1F2329;
    private static final int MUTED = 0xFF8A909B;

    private static final String[] MONTHS_SHORT = {"JAN.", "FEV.", "MAR.", "ABR.", "MAIO", "JUN.",
            "JUL.", "AGO.", "SET.", "OUT.", "NOV.", "DEZ."};
    private static final String[] MONTHS = {"janeiro", "fevereiro", "março", "abril", "maio", "junho",
            "julho", "agosto", "setembro", "outubro", "novembro", "dezembro"};
    private static final String[] WEEKDAYS = {"Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"};

    static final String[] PRIORITY_NAMES = {"Nenhuma", "Baixa", "Média", "Alta"};
    static final int[] PRIORITY_COLORS = {0xFFB0B5BF, 0xFF6C8CF2, 0xFFF2A33A, 0xFFE5533D};
    static final int[] BAR_COLORS = {0xFFC9CDD6, 0xFF9B8CE8, 0xFFE59AB0, 0xFF9B8CE8};

    /** List themes: card background, badge colour, badge emoji. */
    static final int[] THEME_BG = {0xFFFFF3EA, 0xFFEFFCFC, 0xFFFDEBF0, 0xFFF1EEFD, 0xFFFFF8E0, 0xFFEDF9EF};
    static final int[] THEME_BADGE = {0xFFF7B48A, 0xFF6FD3CE, 0xFFF2A2BC, 0xFFA99BF0, 0xFFF3C766, 0xFF7FCB8E};
    static final String[] THEME_EMOJI = {"🎓", "🛒", "🎬", "📚", "⭐", "🌱"};

    private Store store;
    private float dp;

    private FrameLayout content;
    private View calendarPage, listsPage;
    private LinearLayout navCalendar, navLists;
    private int tab = 0;

    // calendar state
    private int viewY, viewM;
    private int selY, selM, selD;
    private boolean showDone = true;
    private TextView monthLabel, dayLabel;
    private Glyph todayIcon;
    private MonthView monthView;
    private LinearLayout taskBox;

    // lists state
    private LinearLayout listsBox;
    private final Map<Long, EditText> addInputs = new HashMap<Long, EditText>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        dp = getResources().getDisplayMetrics().density;
        store = new Store(this);

        Calendar now = Calendar.getInstance();
        selY = viewY = now.get(Calendar.YEAR);
        selM = viewM = now.get(Calendar.MONTH);
        selD = now.get(Calendar.DAY_OF_MONTH);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.WHITE);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        content = new FrameLayout(this);
        column.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        column.addView(buildNav(), new LinearLayout.LayoutParams(-1, px(60)));
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));

        calendarPage = buildCalendarPage();
        listsPage = buildListsPage();
        content.addView(calendarPage, new FrameLayout.LayoutParams(-1, -1));
        content.addView(listsPage, new FrameLayout.LayoutParams(-1, -1));

        root.addView(buildFab());
        setContentView(root);

        if (b != null) tab = b.getInt("tab", 0);
        selectTab(tab);
        refreshCalendar();
        refreshLists();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
    }

    // ---------------------------------------------------------------- helpers

    private int px(float v) {
        return Math.round(v * dp);
    }

    private GradientDrawable round(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusDp * dp);
        return g;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        return t;
    }

    private View clickable(View v) {
        TypedValueHolder.apply(this, v);
        v.setClickable(true);
        return v;
    }

    /** Ripple background from the theme. */
    static class TypedValueHolder {
        static void apply(Context c, View v) {
            android.util.TypedValue tv = new android.util.TypedValue();
            c.getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
            v.setBackgroundResource(tv.resourceId);
        }
    }

    private void setStatusBar(int color) {
        Window w = getWindow();
        w.setStatusBarColor(color);
        if (Build.VERSION.SDK_INT >= 23) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        } else {
            w.setStatusBarColor(0xFF9AA8C4);
        }
    }

    private void hideKeyboard(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    // ---------------------------------------------------------------- nav + fab

    private View buildNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.WHITE);
        nav.setElevation(px(8));
        navCalendar = navItem(Glyph.CALENDAR, "Calendário", 0);
        navLists = navItem(Glyph.LIST, "Listas", 1);
        nav.addView(navCalendar, new LinearLayout.LayoutParams(0, -1, 1));
        nav.addView(navLists, new LinearLayout.LayoutParams(0, -1, 1));
        return nav;
    }

    private LinearLayout navItem(int glyph, String label, final int index) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        Glyph g = new Glyph(this, glyph, MUTED);
        item.addView(g, new LinearLayout.LayoutParams(px(30), px(30)));
        TextView t = text(label, 11, MUTED, false);
        item.addView(t);
        clickable(item);
        item.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                selectTab(index);
            }
        });
        return item;
    }

    private void selectTab(int index) {
        tab = index;
        calendarPage.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        listsPage.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        paintNav(navCalendar, index == 0);
        paintNav(navLists, index == 1);
        setStatusBar(index == 0 ? 0xFFEAF2FE : 0xFFE3F0FC);
        hideKeyboard(content);
    }

    private void paintNav(LinearLayout item, boolean on) {
        int c = on ? ACCENT : MUTED;
        ((Glyph) item.getChildAt(0)).setColor(c);
        TextView t = (TextView) item.getChildAt(1);
        t.setTextColor(c);
        t.setTypeface(on ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
    }

    private View buildFab() {
        FrameLayout fab = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(ACCENT);
        fab.setBackground(bg);
        fab.setElevation(px(6));
        Glyph plus = new Glyph(this, Glyph.PLUS, Color.WHITE);
        fab.addView(plus, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(px(60), px(60), Gravity.BOTTOM | Gravity.END);
        lp.setMargins(0, 0, px(22), px(82));
        fab.setLayoutParams(lp);
        fab.setClickable(true);
        fab.setForeground(getDrawable(rippleRes()));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (tab == 0) editTask(null);
                else editList(null);
            }
        });
        return fab;
    }

    private int rippleRes() {
        android.util.TypedValue tv = new android.util.TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, tv, true);
        return tv.resourceId;
    }

    // ---------------------------------------------------------------- calendar page

    private View buildCalendarPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(col);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable grad = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFFEAF2FE, 0xFFF4F8FF, 0xFFFFFFFF});
        top.setBackground(grad);
        top.setPadding(px(6), px(8), px(6), px(6));
        col.addView(top);

        // header: ◀ NOV. 2026 ▶            [today] [⋮]
        LinearLayout head = new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        top.addView(head, new LinearLayout.LayoutParams(-1, px(52)));

        Glyph prev = new Glyph(this, Glyph.LEFT, 0xFF6B717C);
        clickable(prev);
        prev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { shiftMonth(-1); }
        });
        head.addView(prev, new LinearLayout.LayoutParams(px(36), px(44)));

        monthLabel = text("", 18, TEXT, false);
        monthLabel.setLetterSpacing(0.03f);
        head.addView(monthLabel);

        Glyph next = new Glyph(this, Glyph.RIGHT, 0xFF6B717C);
        clickable(next);
        next.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { shiftMonth(1); }
        });
        head.addView(next, new LinearLayout.LayoutParams(px(36), px(44)));

        head.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));

        todayIcon = new Glyph(this, Glyph.TODAY, 0xFF6B717C);
        clickable(todayIcon);
        todayIcon.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { goToday(); }
        });
        head.addView(todayIcon, new LinearLayout.LayoutParams(px(48), px(48)));

        final Glyph more = new Glyph(this, Glyph.MORE, 0xFF6B717C);
        clickable(more);
        more.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { calendarMenu(more); }
        });
        head.addView(more, new LinearLayout.LayoutParams(px(44), px(48)));

        monthView = new MonthView(this);
        monthView.setListener(new MonthView.Listener() {
            @Override public void onDaySelected(int y, int m, int d) {
                selY = y;
                selM = m;
                selD = d;
                viewY = y;
                viewM = m;
                refreshCalendar();
            }

            @Override public void onMonthSwipe(int delta) {
                shiftMonth(delta);
            }
        });
        top.addView(monthView, new LinearLayout.LayoutParams(-1, -2));

        dayLabel = text("", 14, MUTED, true);
        dayLabel.setPadding(px(22), px(10), px(22), px(6));
        col.addView(dayLabel);

        taskBox = new LinearLayout(this);
        taskBox.setOrientation(LinearLayout.VERTICAL);
        taskBox.setPadding(px(16), 0, px(16), px(100));
        col.addView(taskBox);
        return scroll;
    }

    private void shiftMonth(int delta) {
        viewM += delta;
        while (viewM < 0) { viewM += 12; viewY--; }
        while (viewM > 11) { viewM -= 12; viewY++; }
        refreshCalendar();
    }

    private void goToday() {
        Calendar now = Calendar.getInstance();
        selY = viewY = now.get(Calendar.YEAR);
        selM = viewM = now.get(Calendar.MONTH);
        selD = now.get(Calendar.DAY_OF_MONTH);
        refreshCalendar();
    }

    private void calendarMenu(View anchor) {
        choose(new String[]{showDone ? "Ocultar concluídas" : "Mostrar concluídas",
                "Excluir concluídas deste dia", "Ir para hoje"}, new Choice() {
            @Override public void chosen(int which) {
                if (which == 0) {
                    showDone = !showDone;
                } else if (which == 1) {
                    String k = Store.key(selY, selM, selD);
                    for (int i = store.tasks.size() - 1; i >= 0; i--) {
                        Store.Task t = store.tasks.get(i);
                        if (t.done && k.equals(t.date)) store.tasks.remove(i);
                    }
                    store.save();
                } else {
                    goToday();
                    return;
                }
                refreshCalendar();
            }
        });
    }

    interface Choice {
        void chosen(int which);
    }

    private void choose(String[] items, final Choice c) {
        new AlertDialog.Builder(this).setItems(items, new DialogInterface.OnClickListener() {
            @Override public void onClick(DialogInterface d, int which) {
                c.chosen(which);
            }
        }).show();
    }

    private void refreshCalendar() {
        monthLabel.setText(MONTHS_SHORT[viewM] + " " + viewY);
        todayIcon.setLabel(String.valueOf(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
        monthView.show(viewY, viewM, selY, selM, selD, store.busyDates());

        Calendar sel = Calendar.getInstance();
        sel.clear();
        sel.set(selY, selM, selD);
        Calendar now = Calendar.getInstance();
        String prefix = "";
        if (Store.key(sel).equals(Store.key(now))) prefix = "Hoje · ";
        else {
            now.add(Calendar.DAY_OF_MONTH, 1);
            if (Store.key(sel).equals(Store.key(now))) prefix = "Amanhã · ";
        }
        dayLabel.setText(prefix + WEEKDAYS[sel.get(Calendar.DAY_OF_WEEK) - 1] + ", " + selD + " de " + MONTHS[selM]);

        taskBox.removeAllViews();
        List<Store.Task> list = store.tasksOn(Store.key(selY, selM, selD));
        int shown = 0;
        for (Store.Task t : list) {
            if (t.done && !showDone) continue;
            taskBox.addView(taskCard(t));
            shown++;
        }
        if (shown == 0) {
            TextView empty = text("Nenhuma tarefa neste dia.\nToque em + para adicionar.", 15, 0xFFA5ABB5, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, px(36), 0, 0);
            taskBox.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private View taskCard(final Store.Task t) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(round(0xFFF7F8FA, 10));
        card.setClipToOutline(true);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = px(10);
        card.setLayoutParams(lp);
        card.setMinimumHeight(px(68));

        View bar = new View(this);
        bar.setBackgroundColor(t.done ? 0xFFDADDE3 : BAR_COLORS[t.priority]);
        card.addView(bar, new LinearLayout.LayoutParams(px(5), -1));

        final Glyph check = new Glyph(this, Glyph.CHECKBOX, t.done ? 0xFFB9BEC8 : 0xFF9AA0AB);
        check.setFilled(t.done);
        clickable(check);
        check.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                t.done = !t.done;
                store.save();
                refreshCalendar();
            }
        });
        card.addView(check, new LinearLayout.LayoutParams(px(48), px(48)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(0, px(12), 0, px(12));
        TextView title = text(t.title, 17, t.done ? 0xFFA5ABB5 : TEXT, false);
        if (t.done) title.setPaintFlags(title.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        texts.addView(title);
        if (!t.time.isEmpty()) {
            TextView time = text(t.time, 13, MUTED, false);
            time.setPadding(0, px(3), 0, 0);
            texts.addView(time);
        }
        card.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));

        Glyph flag = new Glyph(this, Glyph.FLAG, t.done ? 0xFFC9CDD6 : PRIORITY_COLORS[t.priority]);
        flag.setFilled(t.priority > 0 && !t.done);
        card.addView(flag, new LinearLayout.LayoutParams(px(52), px(52)));

        card.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x22000000), round(0xFFF7F8FA, 10), round(Color.BLACK, 10)));
        card.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { editTask(t); }
        });
        card.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                confirmDeleteTask(t);
                return true;
            }
        });
        return card;
    }

    private void confirmDeleteTask(final Store.Task t) {
        new AlertDialog.Builder(this)
                .setTitle("Excluir tarefa?")
                .setMessage(t.title)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        store.tasks.remove(t);
                        store.save();
                        refreshCalendar();
                    }
                }).show();
    }

    /** Create (t == null) or edit a task. */
    private void editTask(final Store.Task existing) {
        final Store.Task draft = new Store.Task();
        if (existing != null) {
            draft.title = existing.title;
            draft.date = existing.date;
            draft.time = existing.time;
            draft.priority = existing.priority;
        } else {
            draft.title = "";
            draft.date = Store.key(selY, selM, selD);
            draft.time = "";
            draft.priority = 0;
        }

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(px(22), px(8), px(22), 0);

        final EditText title = new EditText(this);
        title.setHint("O que você precisa fazer?");
        title.setText(draft.title);
        title.setSingleLine(true);
        title.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        title.setSelection(title.getText().length());
        form.addView(title, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout when = new LinearLayout(this);
        when.setOrientation(LinearLayout.HORIZONTAL);
        when.setPadding(0, px(12), 0, px(4));
        final TextView dateBtn = chip("");
        final TextView timeBtn = chip("");
        when.addView(dateBtn);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(-2, -2);
        tlp.leftMargin = px(10);
        when.addView(timeBtn, tlp);
        form.addView(when);

        final Runnable paintWhen = new Runnable() {
            @Override public void run() {
                String[] p = draft.date.split("-");
                dateBtn.setText("📅  " + Integer.parseInt(p[2]) + " " + MONTHS_SHORT[Integer.parseInt(p[1]) - 1].toLowerCase(Locale.ROOT) + " " + p[0]);
                timeBtn.setText(draft.time.isEmpty() ? "🕒  Sem horário" : "🕒  " + draft.time);
            }
        };
        paintWhen.run();

        dateBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String[] p = draft.date.split("-");
                new DatePickerDialog(MainActivity.this, new DatePickerDialog.OnDateSetListener() {
                    @Override public void onDateSet(DatePicker view, int y, int m, int d) {
                        draft.date = Store.key(y, m, d);
                        paintWhen.run();
                    }
                }, Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2])).show();
            }
        });
        timeBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                int h = 9, m = 0;
                if (!draft.time.isEmpty()) {
                    h = Integer.parseInt(draft.time.substring(0, 2));
                    m = Integer.parseInt(draft.time.substring(3, 5));
                }
                TimePickerDialog tp = new TimePickerDialog(MainActivity.this, new TimePickerDialog.OnTimeSetListener() {
                    @Override public void onTimeSet(TimePicker view, int hh, int mm) {
                        draft.time = String.format(Locale.US, "%02d:%02d", hh, mm);
                        paintWhen.run();
                    }
                }, h, m, true);
                tp.setButton(DialogInterface.BUTTON_NEUTRAL, "Sem horário", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        draft.time = "";
                        paintWhen.run();
                    }
                });
                tp.show();
            }
        });

        TextView pl = text("Prioridade", 13, MUTED, false);
        pl.setPadding(0, px(14), 0, px(6));
        form.addView(pl);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        final LinearLayout prios = new LinearLayout(this);
        prios.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(prios);
        form.addView(hs);
        final TextView[] pchips = new TextView[4];
        final Runnable paintPrio = new Runnable() {
            @Override public void run() {
                for (int i = 0; i < 4; i++) {
                    boolean on = draft.priority == i;
                    GradientDrawable g = round(on ? PRIORITY_COLORS[i] : 0xFFF2F3F6, 18);
                    pchips[i].setBackground(g);
                    pchips[i].setTextColor(on ? Color.WHITE : 0xFF4A505A);
                }
            }
        };
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            TextView c = chip((i == 0 ? "" : "⚑ ") + PRIORITY_NAMES[i]);
            c.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    draft.priority = idx;
                    paintPrio.run();
                }
            });
            pchips[i] = c;
            LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(-2, -2);
            clp.rightMargin = px(8);
            prios.addView(c, clp);
        }
        paintPrio.run();

        AlertDialog.Builder b = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Nova tarefa" : "Editar tarefa")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null);
        if (existing != null) {
            b.setNeutralButton("Excluir", new DialogInterface.OnClickListener() {
                @Override public void onClick(DialogInterface d, int w) {
                    store.tasks.remove(existing);
                    store.save();
                    refreshCalendar();
                }
            });
        }
        final AlertDialog dlg = b.create();
        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface d) {
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String s = title.getText().toString().trim();
                        if (s.isEmpty()) {
                            title.setError("Digite um título");
                            return;
                        }
                        Store.Task t = existing;
                        if (t == null) {
                            t = new Store.Task();
                            t.id = store.newId();
                            store.tasks.add(t);
                        }
                        t.title = s;
                        t.date = draft.date;
                        t.time = draft.time;
                        t.priority = draft.priority;
                        store.save();
                        String[] p = t.date.split("-");
                        selY = viewY = Integer.parseInt(p[0]);
                        selM = viewM = Integer.parseInt(p[1]) - 1;
                        selD = Integer.parseInt(p[2]);
                        refreshCalendar();
                        dlg.dismiss();
                    }
                });
            }
        });
        if (existing == null) {
            dlg.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dlg.show();
    }

    private TextView chip(String s) {
        TextView c = text(s, 14, 0xFF4A505A, false);
        c.setBackground(round(0xFFF2F3F6, 18));
        c.setPadding(px(14), px(8), px(14), px(8));
        c.setClickable(true);
        return c;
    }

    // ---------------------------------------------------------------- lists page

    private View buildListsPage() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFE3F0FC);
        scroll.setFillViewport(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(20), px(18), px(20), px(110));
        scroll.addView(col);

        TextView h = text("Minhas listas", 30, 0xFF111111, true);
        col.addView(h);
        TextView sub = text("Organize estudos, compras, filmes e o que mais quiser.", 14, 0xFF6B7684, false);
        sub.setPadding(0, px(4), 0, px(6));
        col.addView(sub);

        listsBox = new LinearLayout(this);
        listsBox.setOrientation(LinearLayout.VERTICAL);
        col.addView(listsBox);
        return scroll;
    }

    private void refreshLists() {
        listsBox.removeAllViews();
        addInputs.clear();
        for (Store.TaskList l : store.lists) listsBox.addView(listCard(l));
        if (store.lists.isEmpty()) {
            TextView empty = text("Nenhuma lista ainda.\nToque em + para criar a primeira.", 15, 0xFF7C8897, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, px(60), 0, 0);
            listsBox.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        }
    }

    private View listCard(final Store.TaskList l) {
        final int theme = l.theme % THEME_BG.length;

        FrameLayout wrap = new FrameLayout(this);
        wrap.setClipChildren(false);
        wrap.setClipToPadding(false);
        wrap.setPadding(0, px(22), 0, 0);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(THEME_BG[theme], 22));
        card.setElevation(px(5));
        card.setPadding(px(18), px(16), px(10), px(12));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(-1, -2);
        clp.rightMargin = px(8);
        wrap.addView(card, clp);

        LinearLayout titleRow = new LinearLayout(this);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text(l.name, 19, 0xFF111111, true);
        title.setPadding(0, px(4), px(70), px(6));
        titleRow.addView(title, new LinearLayout.LayoutParams(0, -2, 1));
        card.addView(titleRow);

        int done = 0;
        for (final Store.Item it : l.items) {
            if (it.done) done++;
            card.addView(itemRow(l, it, theme));
        }

        final EditText add = new EditText(this);
        add.setHint("+  Adicionar item");
        add.setHintTextColor(0xFF9AA3AE);
        add.setTextColor(TEXT);
        add.setTextSize(16);
        add.setBackground(null);
        add.setSingleLine(true);
        add.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        add.setImeOptions(EditorInfo.IME_ACTION_DONE);
        add.setPadding(px(6), px(10), px(6), px(8));
        add.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override public boolean onEditorAction(TextView v, int actionId, KeyEvent e) {
                String s = add.getText().toString().trim();
                if (s.isEmpty()) {
                    hideKeyboard(add);
                    add.clearFocus();
                    return true;
                }
                Store.Item it = new Store.Item();
                it.id = store.newId();
                it.text = s;
                l.items.add(it);
                store.save();
                refreshLists();
                EditText again = addInputs.get(l.id);
                if (again != null) again.requestFocus();
                return true;
            }
        });
        addInputs.put(l.id, add);
        card.addView(add, new LinearLayout.LayoutParams(-1, -2));

        if (!l.items.isEmpty()) {
            TextView count = text(done + " de " + l.items.size() + " concluídos", 12, 0xFF8C96A3, false);
            count.setPadding(px(6), 0, 0, px(2));
            card.addView(count);
        }

        // badge in the top-right corner, half outside the card
        TextView badge = text(THEME_EMOJI[theme], 26, Color.WHITE, false);
        badge.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(THEME_BADGE[theme]);
        badge.setBackground(bg);
        badge.setElevation(px(7));
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(px(64), px(64), Gravity.TOP | Gravity.END);
        blp.topMargin = 0;
        wrap.addView(badge, blp);

        View.OnClickListener menu = new View.OnClickListener() {
            @Override public void onClick(View v) { listMenu(v, l); }
        };
        badge.setOnClickListener(menu);
        title.setOnClickListener(menu);

        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(-1, -2);
        wlp.topMargin = px(10);
        wrap.setLayoutParams(wlp);
        return wrap;
    }

    private View itemRow(final Store.TaskList l, final Store.Item it, int theme) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(px(50));

        Glyph check = new Glyph(this, Glyph.CHECKBOX, it.done ? THEME_BADGE[theme] : 0xFF8F959E);
        check.setFilled(it.done);
        row.addView(check, new LinearLayout.LayoutParams(px(40), px(44)));

        TextView t = text(it.text, 16, it.done ? 0xFFA0A6AF : 0xFF2A2E35, false);
        if (it.done) t.setPaintFlags(t.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
        t.setPadding(px(6), px(8), px(6), px(8));
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setMaxLines(2);
        row.addView(t, new LinearLayout.LayoutParams(0, -2, 1));

        Glyph x = new Glyph(this, Glyph.CLOSE, 0xFF9AA0AB);
        clickable(x);
        x.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                l.items.remove(it);
                store.save();
                refreshLists();
            }
        });
        row.addView(x, new LinearLayout.LayoutParams(px(44), px(44)));

        row.setClickable(true);
        row.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                it.done = !it.done;
                store.save();
                refreshLists();
            }
        });
        row.setOnLongClickListener(new View.OnLongClickListener() {
            @Override public boolean onLongClick(View v) {
                prompt("Editar item", it.text, new Done() {
                    @Override public void done(String s) {
                        it.text = s;
                        store.save();
                        refreshLists();
                    }
                });
                return true;
            }
        });
        return row;
    }

    private void listMenu(View anchor, final Store.TaskList l) {
        choose(new String[]{"Editar lista", "Desmarcar todos", "Remover concluídos", "Excluir lista"}, new Choice() {
            @Override public void chosen(int which) {
                switch (which) {
                    case 0:
                        editList(l);
                        return;
                    case 1:
                        for (Store.Item i : l.items) i.done = false;
                        break;
                    case 2:
                        for (int i = l.items.size() - 1; i >= 0; i--) if (l.items.get(i).done) l.items.remove(i);
                        break;
                    case 3:
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Excluir \"" + l.name + "\"?")
                                .setMessage("A lista e todos os itens serão apagados.")
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Excluir", new DialogInterface.OnClickListener() {
                                    @Override public void onClick(DialogInterface d, int w) {
                                        store.lists.remove(l);
                                        store.save();
                                        refreshLists();
                                    }
                                }).show();
                        return;
                }
                store.save();
                refreshLists();
            }
        });
    }

    /** Create (l == null) or edit a list: name and colour theme. */
    private void editList(final Store.TaskList existing) {
        final int[] theme = {existing != null ? existing.theme % THEME_BG.length : store.lists.size() % THEME_BG.length};

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(px(22), px(8), px(22), 0);
        final EditText name = new EditText(this);
        name.setHint("Nome da lista");
        name.setSingleLine(true);
        name.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        if (existing != null) name.setText(existing.name);
        name.setSelection(name.getText().length());
        form.addView(name);

        TextView tl = text("Estilo", 13, MUTED, false);
        tl.setPadding(0, px(14), 0, px(8));
        form.addView(tl);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        form.addView(row);
        final TextView[] dots = new TextView[THEME_BG.length];
        final Runnable paint = new Runnable() {
            @Override public void run() {
                for (int i = 0; i < dots.length; i++) {
                    GradientDrawable g = new GradientDrawable();
                    g.setShape(GradientDrawable.OVAL);
                    g.setColor(THEME_BADGE[i]);
                    if (i == theme[0]) g.setStroke(px(3), 0xFF333A45);
                    dots[i].setBackground(g);
                }
            }
        };
        for (int i = 0; i < THEME_BG.length; i++) {
            final int idx = i;
            TextView d = text(THEME_EMOJI[i], 18, Color.WHITE, false);
            d.setGravity(Gravity.CENTER);
            d.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    theme[0] = idx;
                    paint.run();
                }
            });
            dots[i] = d;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, px(42), 1);
            lp.setMargins(px(3), 0, px(3), 0);
            row.addView(d, lp);
        }
        paint.run();

        final AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle(existing == null ? "Nova lista" : "Editar lista")
                .setView(form)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", null)
                .create();
        dlg.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface d) {
                dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        String s = name.getText().toString().trim();
                        if (s.isEmpty()) {
                            name.setError("Digite um nome");
                            return;
                        }
                        Store.TaskList l = existing;
                        if (l == null) {
                            l = new Store.TaskList();
                            l.id = store.newId();
                            store.lists.add(l);
                        }
                        l.name = s;
                        l.theme = theme[0];
                        store.save();
                        refreshLists();
                        dlg.dismiss();
                    }
                });
            }
        });
        if (existing == null) {
            dlg.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }
        dlg.show();
    }

    interface Done {
        void done(String s);
    }

    private void prompt(String title, String value, final Done done) {
        final EditText in = new EditText(this);
        in.setText(value);
        in.setSingleLine(true);
        in.setSelection(in.getText().length());
        FrameLayout box = new FrameLayout(this);
        box.setPadding(px(22), px(8), px(22), 0);
        box.addView(in);
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setView(box)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String s = in.getText().toString().trim();
                        if (!s.isEmpty()) done.done(s);
                    }
                }).show();
    }
}
