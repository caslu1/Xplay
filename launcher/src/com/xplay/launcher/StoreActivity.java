package com.xplay.launcher;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** The app store: APKs listed in the panel's "Loja de Apps", installed or updated with one click. */
public class StoreActivity extends Activity {

    private final List<JSONObject> items = new ArrayList<>();
    private Adapter adapter;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        ImageLoader.init(this);
        Config cfg = Config.get(this);
        Ui.applyTheme(cfg);
        JSONObject store = cfg.store();

        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(0xFF000000);
        int p = Ui.dp(this, 36);
        root.setPadding(p, Ui.dp(this, 16), p, Ui.dp(this, 16));
        setContentView(root);

        LinearLayout header = Ui.row(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        ImageView back = Ui.iconButton(this, R.drawable.ic_back);
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        header.addView(back, Ui.lp(Ui.dp(this, 44), Ui.dp(this, 44)));
        LinearLayout titles = Ui.column(this);
        titles.addView(Ui.text(this, store.optString("title", "LOJA DE APPS"), 26, Ui.TEXT, true));
        String sub = store.optString("subtitle", "");
        if (!sub.isEmpty()) {
            TextView st = Ui.text(this, sub, 12, Ui.ACCENT, true);
            st.setLetterSpacing(0.1f);
            titles.addView(st);
        }
        LinearLayout.LayoutParams tl = Ui.lp(-2, -2);
        tl.leftMargin = Ui.dp(this, 40);
        header.addView(titles, tl);
        root.addView(header);

        JSONArray apps = store.optJSONArray("apps");
        if (apps != null) {
            for (int i = 0; i < apps.length(); i++) {
                JSONObject o = apps.optJSONObject(i);
                if (o != null) items.add(o);
            }
        }

        final GridView grid = new GridView(this);
        grid.setNumColumns(5);
        grid.setHorizontalSpacing(Ui.dp(this, 16));
        grid.setVerticalSpacing(Ui.dp(this, 16));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setSelector(Ui.ring(this, Ui.dp(this, 18)));
        grid.setDrawSelectorOnTop(true);
        int gp = Ui.dp(this, 4);
        grid.setPadding(gp, gp, gp, gp);
        grid.setClipToPadding(false);
        adapter = new Adapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int pos, long id) { click(items.get(pos)); }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int pos, long id) {
                String pkg = items.get(pos).optString("package");
                if (Apps.isInstalled(StoreActivity.this, pkg)) Apps.openInfo(StoreActivity.this, pkg);
                return true;
            }
        });
        LinearLayout.LayoutParams gl = new LinearLayout.LayoutParams(-1, 0, 1);
        gl.topMargin = Ui.dp(this, 24);
        root.addView(grid, gl);
        if (items.isEmpty()) {
            root.addView(Ui.text(this, "Nenhum app na loja ainda. Adicione pelo painel.", 16, Ui.MUTED, false));
        }

        String wanted = getIntent().getStringExtra("package");
        int sel = 0;
        for (int i = 0; wanted != null && i < items.size(); i++) {
            if (wanted.equals(items.get(i).optString("package"))) sel = i;
        }
        grid.setSelection(sel);
        grid.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Back from the installer: refresh the Instalado / Atualizar labels.
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private int state(JSONObject app) {
        String pkg = app.optString("package", "");
        int installed = pkg.isEmpty() ? -1 : Apps.versionCode(this, pkg);
        if (installed < 0) return 0;               // not installed
        if (installed < app.optInt("versionCode", 0)) return 1; // update available
        return 2;                                   // up to date
    }

    private void click(JSONObject app) {
        String name = app.optString("name", "app");
        if (state(app) == 2) {
            Apps.launch(this, app.optString("package"));
            return;
        }
        Installer.downloadAndInstall(this, app.optString("apk"), app.optString("package", name), "Instalando " + name);
    }

    private final class Adapter extends BaseAdapter {
        public int getCount() { return items.size(); }
        public Object getItem(int i) { return items.get(i); }
        public long getItemId(int i) { return i; }

        public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout box;
            if (convert == null) {
                box = Ui.column(StoreActivity.this);
                box.setGravity(Gravity.CENTER);
                int p = Ui.dp(StoreActivity.this, 14);
                box.setPadding(p, p, p, p);
                box.setBackground(Ui.round(0xFF141414, Ui.dp(StoreActivity.this, 18)));
                box.setLayoutParams(new GridView.LayoutParams(-1, Ui.dp(StoreActivity.this, 176)));
                ImageView icon = new ImageView(StoreActivity.this);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                box.addView(icon, Ui.lp(Ui.dp(StoreActivity.this, 76), Ui.dp(StoreActivity.this, 76)));
                TextView name = Ui.text(StoreActivity.this, "", 16, Ui.TEXT, true);
                name.setSingleLine(true);
                name.setEllipsize(TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams nl = Ui.lp(-2, -2);
                nl.topMargin = Ui.dp(StoreActivity.this, 12);
                box.addView(name, nl);
                TextView status = Ui.text(StoreActivity.this, "", 12, Ui.ACCENT, true);
                LinearLayout.LayoutParams sl = Ui.lp(-2, -2);
                sl.topMargin = Ui.dp(StoreActivity.this, 6);
                box.addView(status, sl);
            } else {
                box = (LinearLayout) convert;
            }
            JSONObject app = items.get(pos);
            ImageView icon = (ImageView) box.getChildAt(0);
            ImageLoader.load(app.optString("icon", ""), icon, 200, R.drawable.ic_android);
            ((TextView) box.getChildAt(1)).setText(app.optString("name"));
            TextView status = (TextView) box.getChildAt(2);
            String version = app.optString("versionName", "");
            switch (state(app)) {
                case 0:
                    status.setText("INSTALAR" + (version.isEmpty() ? "" : "  v" + version));
                    status.setTextColor(Ui.ACCENT);
                    break;
                case 1:
                    status.setText("ATUALIZAR" + (version.isEmpty() ? "" : " para v" + version));
                    status.setTextColor(0xFFF59E0B);
                    break;
                default:
                    status.setText("INSTALADO · ABRIR");
                    status.setTextColor(0xFF22C55E);
                    break;
            }
            return box;
        }
    }
}
