package com.xplay.launcher;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/**
 * Grid of installed apps. Opened with extra "pick" it is the "Selecionar App" dialog that returns the
 * chosen package to the home screen; otherwise it is the "Todos os apps" screen that opens apps.
 */
public class AppsActivity extends Activity {

    private List<Apps.App> apps;
    private boolean pick;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        pick = getIntent().getBooleanExtra("pick", false);
        Ui.applyTheme(Config.get(this));

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xEE000000);
        setContentView(root);

        LinearLayout panel = Ui.column(this);
        int p = Ui.dp(this, 24);
        panel.setPadding(p, p, p, p);
        android.graphics.drawable.GradientDrawable frame = Ui.round(0xFF050505, Ui.dp(this, 20));
        frame.setStroke(Ui.dp(this, 1), 0xFF2A2A2A);
        panel.setBackground(frame);
        FrameLayout.LayoutParams pl = new FrameLayout.LayoutParams(-1, -1);
        int m = Ui.dp(this, 36);
        pl.setMargins(m, Ui.dp(this, 28), m, Ui.dp(this, 28));
        root.addView(panel, pl);

        LinearLayout header = Ui.row(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(Ui.text(this, pick ? "Selecionar App" : "Todos os Apps", 22, Ui.TEXT, true),
                new LinearLayout.LayoutParams(0, -2, 1));
        ImageView close = Ui.iconButton(this, R.drawable.ic_close);
        close.setBackground(Ui.card(this, 0xFF1F1F1F, Ui.dp(this, 20)));
        close.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        header.addView(close, Ui.lp(Ui.dp(this, 38), Ui.dp(this, 38)));
        panel.addView(header);

        apps = Apps.installed(this);
        GridView grid = new GridView(this);
        grid.setNumColumns(7);
        grid.setHorizontalSpacing(Ui.dp(this, 12));
        grid.setVerticalSpacing(Ui.dp(this, 12));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(Ui.ring(this, Ui.dp(this, 14)));
        grid.setDrawSelectorOnTop(true);
        grid.setClipToPadding(false);
        int gp = Ui.dp(this, 4);
        grid.setPadding(gp, gp, gp, gp);
        grid.setAdapter(new Adapter());
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
                Apps.App app = apps.get(pos);
                if (pick) {
                    setResult(RESULT_OK, new Intent().putExtra("package", app.pkg));
                    finish();
                } else {
                    Apps.launch(AppsActivity.this, app.pkg);
                }
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                Apps.openInfo(AppsActivity.this, apps.get(pos).pkg);
                return true;
            }
        });
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(-1, 0, 1);
        gl.topMargin = Ui.dp(this, 18);
        panel.addView(grid, gl);
        if (apps.isEmpty()) {
            panel.addView(Ui.text(this, "Nenhum app encontrado", 16, Ui.MUTED, false));
        }
        grid.requestFocus();
    }

    private final class Adapter extends BaseAdapter {
        public int getCount() { return apps.size(); }
        public Object getItem(int i) { return apps.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout box;
            ImageView icon;
            TextView label;
            if (convert == null) {
                box = Ui.column(AppsActivity.this);
                box.setGravity(Gravity.CENTER);
                int p = Ui.dp(AppsActivity.this, 10);
                box.setPadding(p, p, p, p);
                box.setBackground(Ui.round(0xFF141414, Ui.dp(AppsActivity.this, 14)));
                box.setLayoutParams(new GridView.LayoutParams(-1, Ui.dp(AppsActivity.this, 112)));
                icon = new ImageView(AppsActivity.this);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                box.addView(icon, Ui.lp(Ui.dp(AppsActivity.this, 44), Ui.dp(AppsActivity.this, 44)));
                label = Ui.text(AppsActivity.this, "", 12, Ui.TEXT, false);
                label.setGravity(Gravity.CENTER);
                label.setSingleLine(true);
                label.setEllipsize(TextUtils.TruncateAt.END);
                label.setLetterSpacing(0.05f);
                LinearLayout.LayoutParams ll = Ui.lp(-1, -2);
                ll.topMargin = Ui.dp(AppsActivity.this, 14);
                box.addView(label, ll);
            } else {
                box = (LinearLayout) convert;
                icon = (ImageView) box.getChildAt(0);
                label = (TextView) box.getChildAt(1);
            }
            Apps.App app = apps.get(pos);
            icon.setImageDrawable(Apps.icon(AppsActivity.this, app.pkg));
            label.setText(app.label);
            return box;
        }
    }
}
