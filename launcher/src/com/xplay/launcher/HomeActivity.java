package com.xplay.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * The home screen: background and logo from the panel, a rotating ad banner, the main buttons and a
 * row of apps (the panel's list plus the ones the user added with "+"). When the panel's device list
 * does not allow this box, a lock screen with the device code and the plans is shown instead.
 */
public class HomeActivity extends Activity {

    private static final int PICK_APP = 1;

    private final Handler handler = new Handler();
    private FrameLayout root;
    private LinearLayout appRow;
    private ImageView wifiIcon;
    private ImageView refreshButton;
    private String builtConfig = "";
    private String builtApps = "";
    private boolean fetching;

    private JSONArray ads = new JSONArray();
    private int adIndex;
    private ImageView adImage;
    private TextView adTitle;
    private LinearLayout adDots;
    private final Runnable nextAd = new Runnable() {
        public void run() {
            showAd(adIndex + 1);
        }
    };

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context c, Intent i) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(i.getAction())) {
                updateWifi();
                // A box that booted offline gets the panel config as soon as it connects.
                if (isOnline() && Config.get(HomeActivity.this).fetchedAt == 0) refresh(false);
            } else {
                buildAppRow(false);
            }
        }
    };

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        ImageLoader.init(this);
        root = new FrameLayout(this);
        setContentView(root);
        build();

        registerReceiver(receiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        IntentFilter pkgs = new IntentFilter();
        pkgs.addAction(Intent.ACTION_PACKAGE_ADDED);
        pkgs.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pkgs.addAction(Intent.ACTION_PACKAGE_CHANGED);
        pkgs.addDataScheme("package");
        registerReceiver(receiver, pkgs);

        refresh(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The settings screen may have synced a newer config.
        if (!Config.get(this).json.toString().equals(builtConfig)) build();
        buildAppRow(false);
        updateWifi();
        if (Config.get(this).isStale()) refresh(false);
        scheduleAd();
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(nextAd);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // HOME pressed while already home: go back to the first button.
        View first = root.findViewWithTag("first");
        if (first != null) first.requestFocus();
    }

    @Override
    public void onBackPressed() {
        // The launcher is the bottom of the stack; there is nowhere to go back to.
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            SettingsActivity.open(this);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == PICK_APP && result == RESULT_OK && data != null) {
            String pkg = data.getStringExtra("package");
            if (pkg != null) {
                Apps.pin(this, pkg);
                buildAppRow(true);
            }
        }
    }

    // --- Config ---------------------------------------------------------------------------------

    void refresh(final boolean manual) {
        if (fetching) return;
        fetching = true;
        if (refreshButton != null) {
            refreshButton.animate().rotationBy(720).setDuration(1400).setInterpolator(new LinearInterpolator()).start();
        }
        Task.run(this, new Callable<Config>() {
            public Config call() throws Exception {
                return Config.fetch(HomeActivity.this);
            }
        }, new Task.Done<Config>() {
            public void ok(Config cfg) {
                fetching = false;
                if (!cfg.json.toString().equals(builtConfig)) build();
                if (manual) Toast.makeText(HomeActivity.this, "Launcher atualizado", Toast.LENGTH_SHORT).show();
                Installer.checkOnStart(HomeActivity.this);
            }

            public void fail(Exception e) {
                fetching = false;
                if (manual) Toast.makeText(HomeActivity.this, "Sem conexão com o painel: " + Task.message(e),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void build() {
        Config cfg = Config.get(this);
        builtConfig = cfg.json.toString();
        builtApps = "";
        Ui.applyTheme(cfg);
        handler.removeCallbacks(nextAd);
        root.removeAllViews();
        adImage = null;

        JSONObject branding = cfg.branding();
        root.setBackgroundColor(Ui.color(branding.optString("backgroundColor"), 0xFF05080F));
        ImageView bg = new ImageView(this);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(bg, new FrameLayout.LayoutParams(-1, -1));
        ImageLoader.load(branding.optString("background", ""), bg, 1280, 0);
        View shade = new View(this);
        int overlay = Math.max(0, Math.min(90, branding.optInt("overlay", 45)));
        shade.setBackgroundColor((overlay * 255 / 100) << 24);
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        Device.Access access = Device.check(this, cfg);
        LinearLayout page = Ui.column(this);
        int h = Ui.dp(this, 40), v = Ui.dp(this, 18);
        page.setPadding(h, v, h, v);
        root.addView(page, new FrameLayout.LayoutParams(-1, -1));
        page.addView(topBar(cfg), Ui.lp(-1, Ui.dp(this, 52)));

        if (!access.allowed) {
            page.addView(lockView(cfg, access), new LinearLayout.LayoutParams(-1, 0, 1));
            View retry = root.findViewWithTag("first");
            if (retry != null) retry.requestFocus();
            return;
        }

        LinearLayout middle = Ui.row(this);
        LinearLayout.LayoutParams ml = new LinearLayout.LayoutParams(-1, 0, 1);
        ml.topMargin = Ui.dp(this, 14);
        page.addView(middle, ml);

        JSONObject adCfg = cfg.ads();
        ads = adCfg.optBoolean("enabled", true) && adCfg.optJSONArray("items") != null
                ? adCfg.optJSONArray("items") : new JSONArray();
        JSONArray buttons = cfg.list("buttons");
        if (ads.length() > 0) {
            middle.addView(adBanner(), Ui.weight(buttons.length() > 0 ? 1.25f : 1f));
        }
        if (buttons.length() > 0) {
            LinearLayout.LayoutParams bl = Ui.weight(1f);
            if (ads.length() > 0) bl.leftMargin = Ui.dp(this, 16);
            middle.addView(buttonGrid(buttons), bl);
        }

        String title = cfg.grid().optString("title", "Meus apps");
        TextView label = Ui.text(this, title, 15, Ui.TEXT, true);
        LinearLayout.LayoutParams ll = Ui.lp(-2, -2);
        ll.topMargin = Ui.dp(this, 12);
        ll.bottomMargin = Ui.dp(this, 6);
        page.addView(label, ll);

        HorizontalScrollView scroll = new HorizontalScrollView(this);
        scroll.setHorizontalScrollBarEnabled(false);
        scroll.setClipChildren(false);
        scroll.setClipToPadding(false);
        int sp = Ui.dp(this, 6);
        scroll.setPadding(sp, sp, sp, sp);
        appRow = Ui.row(this);
        appRow.setClipChildren(false);
        scroll.addView(appRow);
        page.addView(scroll, Ui.lp(-1, Ui.dp(this, 120)));
        buildAppRow(true);

        if (ads.length() > 0) showAd(0);
        View first = root.findViewWithTag("first");
        if (first != null) first.requestFocus();
    }

    // --- Top bar --------------------------------------------------------------------------------

    private View topBar(Config cfg) {
        LinearLayout bar = Ui.row(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);

        String logo = cfg.branding().optString("logo", "");
        if (!logo.isEmpty()) {
            ImageView img = new ImageView(this);
            img.setAdjustViewBounds(true);
            img.setScaleType(ImageView.ScaleType.FIT_START);
            img.setMaxWidth(Ui.dp(this, 260));
            bar.addView(img, Ui.lp(-2, Ui.dp(this, 46)));
            ImageLoader.load(logo, img, 520, 0);
        } else {
            bar.addView(Ui.text(this, cfg.name(), 22, Ui.TEXT, true));
        }
        bar.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));

        int size = Ui.dp(this, 42), gap = Ui.dp(this, 10);
        refreshButton = Ui.iconButton(this, R.drawable.ic_refresh);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { refresh(true); }
        });
        LinearLayout.LayoutParams il = Ui.lp(size, size);
        il.leftMargin = gap;
        bar.addView(refreshButton, il);

        ImageView settings = Ui.iconButton(this, R.drawable.ic_settings);
        settings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { SettingsActivity.open(HomeActivity.this); }
        });
        bar.addView(settings, new LinearLayout.LayoutParams(il));

        JSONObject s = cfg.settings();
        wifiIcon = Ui.iconButton(this, R.drawable.ic_wifi);
        wifiIcon.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { Actions.system(HomeActivity.this, Settings.ACTION_WIFI_SETTINGS); }
        });
        if (s.optBoolean("showWifi", true)) bar.addView(wifiIcon, new LinearLayout.LayoutParams(il));
        updateWifi();

        if (s.optBoolean("showClock", true)) {
            TextClock clock = new TextClock(this);
            clock.setFormat24Hour("HH:mm");
            clock.setFormat12Hour("HH:mm");
            clock.setTextSize(24);
            clock.setTextColor(Ui.TEXT);
            LinearLayout.LayoutParams cl = Ui.lp(-2, -2);
            cl.leftMargin = Ui.dp(this, 16);
            bar.addView(clock, cl);
        }
        return bar;
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo n = cm == null ? null : cm.getActiveNetworkInfo();
        return n != null && n.isConnected();
    }

    private void updateWifi() {
        if (wifiIcon == null) return;
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo n = cm == null ? null : cm.getActiveNetworkInfo();
        if (n == null || !n.isConnected()) wifiIcon.setImageResource(R.drawable.ic_wifi_off);
        else if (n.getType() == ConnectivityManager.TYPE_ETHERNET) wifiIcon.setImageResource(R.drawable.ic_ethernet);
        else wifiIcon.setImageResource(R.drawable.ic_wifi);
    }

    // --- Ads ------------------------------------------------------------------------------------

    private View adBanner() {
        final float r = Ui.dp(this, 18);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.round(Ui.CARD, r));
        box.setForeground(Ui.ring(this, r));
        box.setClipToOutline(true);
        Ui.focusable(box, 1.02f);

        adImage = new ImageView(this);
        adImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        box.addView(adImage, new FrameLayout.LayoutParams(-1, -1));

        adTitle = Ui.text(this, "", 20, 0xFFFFFFFF, true);
        adTitle.setShadowLayer(8, 0, 2, 0xCC000000);
        int p = Ui.dp(this, 18);
        adTitle.setPadding(p, p, p, p);
        box.addView(adTitle, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        adDots = Ui.row(this);
        FrameLayout.LayoutParams dl = new FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM | Gravity.END);
        dl.setMargins(p, p, p, p);
        box.addView(adDots, dl);

        box.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                JSONObject ad = ads.optJSONObject(adIndex);
                if (ad != null) Actions.run(HomeActivity.this, ad.optJSONObject("action"));
            }
        });
        box.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View view, int code, KeyEvent e) {
                if (e.getAction() != KeyEvent.ACTION_DOWN || ads.length() < 2) return false;
                // Inside the banner, left goes to the previous ad (right still moves to the buttons).
                if (code == KeyEvent.KEYCODE_DPAD_LEFT) {
                    showAd(adIndex - 1);
                    return true;
                }
                return false;
            }
        });
        return box;
    }

    private void showAd(int index) {
        if (adImage == null || ads.length() == 0) return;
        adIndex = (index % ads.length() + ads.length()) % ads.length();
        JSONObject ad = ads.optJSONObject(adIndex);
        if (ad == null) return;
        ImageLoader.load(ad.optString("image", ""), adImage, 1280, 0);
        adImage.setAlpha(0f);
        adImage.animate().alpha(1f).setDuration(400).start();
        String t = ad.optString("title", "");
        adTitle.setText(t);
        adTitle.setVisibility(t.isEmpty() ? View.GONE : View.VISIBLE);

        adDots.removeAllViews();
        if (ads.length() > 1) {
            for (int i = 0; i < ads.length(); i++) {
                View dot = new View(this);
                dot.setBackground(Ui.round(i == adIndex ? Ui.ACCENT : 0x88FFFFFF, Ui.dp(this, 4)));
                LinearLayout.LayoutParams l = Ui.lp(Ui.dp(this, i == adIndex ? 18 : 8), Ui.dp(this, 8));
                l.leftMargin = Ui.dp(this, 5);
                adDots.addView(dot, l);
            }
        }
        scheduleAd();
    }

    private void scheduleAd() {
        handler.removeCallbacks(nextAd);
        if (adImage == null || ads.length() < 2) return;
        int seconds = Math.max(3, Config.get(this).ads().optInt("interval", 8));
        handler.postDelayed(nextAd, seconds * 1000L);
    }

    // --- Main buttons ---------------------------------------------------------------------------

    private View buttonGrid(JSONArray buttons) {
        int n = buttons.length();
        int cols = n <= 2 ? 1 : n <= 4 ? 2 : 3;
        int rows = (n + cols - 1) / cols;
        int gap = Ui.dp(this, 14);
        LinearLayout grid = Ui.column(this);
        grid.setClipChildren(false);
        for (int r = 0; r < rows; r++) {
            LinearLayout row = Ui.row(this);
            row.setClipChildren(false);
            LinearLayout.LayoutParams rl = new LinearLayout.LayoutParams(-1, 0, 1);
            if (r > 0) rl.topMargin = gap;
            grid.addView(row, rl);
            for (int c = 0; c < cols; c++) {
                int i = r * cols + c;
                LinearLayout.LayoutParams cl = Ui.weight(1);
                if (c > 0) cl.leftMargin = gap;
                if (i < n) {
                    View b = mainButton(buttons.optJSONObject(i));
                    if (i == 0) b.setTag("first");
                    row.addView(b, cl);
                } else {
                    row.addView(new View(this), cl);
                }
            }
        }
        return grid;
    }

    private View mainButton(final JSONObject b) {
        if (b == null) return new View(this);
        float r = Ui.dp(this, 18);
        FrameLayout box = new FrameLayout(this);
        box.setBackground(Ui.round(Ui.color(b.optString("color"), Ui.ACCENT), r));
        box.setForeground(Ui.ring(this, r));
        box.setClipToOutline(true);
        Ui.focusable(box, 1.05f);

        String image = b.optString("image", "");
        if (!image.isEmpty()) {
            ImageView img = new ImageView(this);
            img.setScaleType(ImageView.ScaleType.CENTER_CROP);
            box.addView(img, new FrameLayout.LayoutParams(-1, -1));
            ImageLoader.load(image, img, 640, 0);
        }

        LinearLayout content = Ui.column(this);
        content.setGravity(Gravity.CENTER);
        int p = Ui.dp(this, 10);
        content.setPadding(p, p, p, p);
        ImageView icon = null;
        int res = Ui.iconRes(b.optString("icon", ""));
        String iconUrl = b.optString("icon", "");
        JSONObject action = b.optJSONObject("action");
        if (res != 0) {
            icon = Ui.icon(this, res, 0xFFFFFFFF);
        } else if (iconUrl.startsWith("http")) {
            icon = new ImageView(this);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            ImageLoader.load(iconUrl, icon, 160, 0);
        } else if (action != null && "app".equals(action.optString("type"))) {
            String pkg = Apps.resolve(this, action.optString("value"));
            Drawable d = pkg == null ? null : Apps.icon(this, pkg);
            if (d != null) {
                icon = new ImageView(this);
                icon.setImageDrawable(d);
            }
        }
        if (icon != null && image.isEmpty()) content.addView(icon, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        String label = b.optString("label", "");
        if (!label.isEmpty()) {
            TextView t = Ui.text(this, label, 17, 0xFFFFFFFF, true);
            t.setGravity(Gravity.CENTER);
            t.setMaxLines(2);
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setShadowLayer(6, 0, 2, 0x99000000);
            LinearLayout.LayoutParams tl = Ui.lp(-2, -2);
            tl.topMargin = Ui.dp(this, 8);
            content.addView(t, tl);
        }
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-1, -1);
        if (!image.isEmpty()) {
            cl = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
            content.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                    new int[] {0xCC000000, 0x00000000}));
        }
        box.addView(content, cl);
        box.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { Actions.run(HomeActivity.this, b.optJSONObject("action")); }
        });
        return box;
    }

    // --- App row --------------------------------------------------------------------------------

    /** One tile on the app row: either from the panel (pkgs may list fallbacks) or pinned by the user. */
    private static final class Tile {
        final String label;
        final String pkgs;
        final String icon;
        final boolean pinned;

        Tile(String label, String pkgs, String icon, boolean pinned) {
            this.label = label;
            this.pkgs = pkgs;
            this.icon = icon;
            this.pinned = pinned;
        }
    }

    private List<Tile> tiles() {
        Config cfg = Config.get(this);
        List<Tile> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        JSONArray apps = cfg.grid().optJSONArray("apps");
        if (apps != null) {
            for (int i = 0; i < apps.length(); i++) {
                JSONObject a = apps.optJSONObject(i);
                if (a == null) continue;
                String pkgs = a.optString("package", "");
                if (pkgs.isEmpty()) continue;
                if (a.optBoolean("hideIfMissing", false) && Apps.resolve(this, pkgs) == null) continue;
                out.add(new Tile(a.optString("label", ""), pkgs, a.optString("icon", ""), false));
                for (String p : pkgs.split(",")) seen.add(p.trim());
            }
        }
        if (cfg.grid().optBoolean("allowUserApps", true)) {
            for (String pkg : Apps.pinned(this)) {
                if (seen.contains(pkg) || !Apps.isInstalled(this, pkg)) continue;
                out.add(new Tile(Apps.label(this, pkg), pkg, "", true));
            }
        }
        return out;
    }

    private void buildAppRow(boolean force) {
        if (appRow == null) return;
        List<Tile> list = tiles();
        StringBuilder sig = new StringBuilder();
        for (Tile t : list) sig.append(t.pkgs).append('|').append(Apps.resolve(this, t.pkgs)).append(';');
        if (!force && sig.toString().equals(builtApps)) return;
        builtApps = sig.toString();

        View focused = getCurrentFocus();
        boolean rowHadFocus = focused != null && focused.getParent() == appRow;
        appRow.removeAllViews();
        for (Tile t : list) appRow.addView(appTile(t), tileParams());
        if (Config.get(this).grid().optBoolean("allowUserApps", true)) {
            appRow.addView(addTile(), tileParams());
        }
        if (rowHadFocus && appRow.getChildCount() > 0) appRow.getChildAt(0).requestFocus();
    }

    private LinearLayout.LayoutParams tileParams() {
        LinearLayout.LayoutParams l = Ui.lp(Ui.dp(this, 128), -1);
        l.rightMargin = Ui.dp(this, 12);
        return l;
    }

    private LinearLayout tileBox() {
        LinearLayout box = Ui.column(this);
        box.setGravity(Gravity.CENTER);
        int p = Ui.dp(this, 8);
        box.setPadding(p, p, p, p);
        box.setBackground(Ui.card(this, Ui.CARD, Ui.dp(this, 14)));
        Ui.focusable(box, 1.08f);
        return box;
    }

    private View appTile(final Tile t) {
        LinearLayout box = tileBox();
        final String pkg = Apps.resolve(this, t.pkgs);
        ImageView icon = new ImageView(this);
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        if (!t.icon.isEmpty()) {
            ImageLoader.load(t.icon, icon, 160, 0);
        } else {
            Drawable d = pkg == null ? null : Apps.icon(this, pkg);
            if (d != null) icon.setImageDrawable(d);
            else icon.setImageResource(R.drawable.ic_android);
        }
        box.addView(icon, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        String name = t.label.isEmpty() && pkg != null ? Apps.label(this, pkg) : t.label;
        TextView label = Ui.text(this, pkg == null ? name + "\n(instalar)" : name, 12, Ui.TEXT, false);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams ll = Ui.lp(-1, -2);
        ll.topMargin = Ui.dp(this, 6);
        box.addView(label, ll);
        if (pkg == null) box.setAlpha(0.55f);

        box.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { Actions.openApp(HomeActivity.this, t.pkgs); }
        });
        box.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                if (pkg != null) appMenu(pkg, t.pinned);
                return true;
            }
        });
        return box;
    }

    private View addTile() {
        LinearLayout box = tileBox();
        box.addView(Ui.icon(this, R.drawable.ic_add, Ui.TEXT), Ui.lp(Ui.dp(this, 40), Ui.dp(this, 40)));
        TextView label = Ui.text(this, "Adicionar", 12, Ui.MUTED, false);
        LinearLayout.LayoutParams ll = Ui.lp(-2, -2);
        ll.topMargin = Ui.dp(this, 6);
        box.addView(label, ll);
        box.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                startActivityForResult(new Intent(HomeActivity.this, AppsActivity.class)
                        .putExtra("pick", true), PICK_APP);
            }
        });
        return box;
    }

    private void appMenu(final String pkg, final boolean pinned) {
        List<String> items = new ArrayList<>();
        items.add("Abrir");
        if (pinned) items.add("Remover da tela inicial");
        items.add("Informações do app");
        items.add("Desinstalar");
        final String[] arr = items.toArray(new String[0]);
        new AlertDialog.Builder(this, R.style.DialogTheme)
                .setTitle(Apps.label(this, pkg))
                .setItems(arr, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        String choice = arr[which];
                        if (choice.startsWith("Abrir")) Apps.launch(HomeActivity.this, pkg);
                        else if (choice.startsWith("Remover")) {
                            Apps.unpin(HomeActivity.this, pkg);
                            buildAppRow(true);
                        } else if (choice.startsWith("Informações")) Apps.openInfo(HomeActivity.this, pkg);
                        else Apps.uninstall(HomeActivity.this, pkg);
                    }
                })
                .show();
    }

    // --- Lock screen ----------------------------------------------------------------------------

    private View lockView(Config cfg, Device.Access access) {
        LinearLayout box = Ui.column(this);
        box.setGravity(Gravity.CENTER);

        box.addView(Ui.icon(this, R.drawable.ic_lock, Ui.ACCENT), Ui.lp(Ui.dp(this, 52), Ui.dp(this, 52)));
        TextView title = Ui.text(this, "Aparelho não liberado", 26, Ui.TEXT, true);
        LinearLayout.LayoutParams tl = Ui.lp(-2, -2);
        tl.topMargin = Ui.dp(this, 10);
        box.addView(title, tl);
        TextView reason = Ui.text(this, access.reason, 15, Ui.MUTED, false);
        reason.setGravity(Gravity.CENTER);
        box.addView(reason, tl(6));

        LinearLayout code = Ui.column(this);
        code.setGravity(Gravity.CENTER);
        int p = Ui.dp(this, 14);
        code.setPadding(p * 2, p, p * 2, p);
        code.setBackground(Ui.round(Ui.CARD, Ui.dp(this, 14)));
        code.addView(Ui.text(this, "CÓDIGO DO APARELHO", 12, Ui.MUTED, true));
        TextView id = Ui.text(this, Device.id(this), 34, Ui.ACCENT, true);
        id.setLetterSpacing(0.08f);
        code.addView(id);
        box.addView(code, tl(16));

        JSONArray plans = cfg.list("plans");
        if (plans.length() > 0) {
            LinearLayout row = Ui.row(this);
            row.setGravity(Gravity.CENTER);
            for (int i = 0; i < plans.length() && i < 4; i++) {
                JSONObject plan = plans.optJSONObject(i);
                if (plan == null) continue;
                LinearLayout card = Ui.column(this);
                card.setGravity(Gravity.CENTER);
                card.setPadding(p, p, p, p);
                card.setBackground(Ui.round(Ui.CARD, Ui.dp(this, 14)));
                card.addView(Ui.text(this, plan.optString("name"), 14, Ui.TEXT, true));
                card.addView(Ui.text(this, plan.optString("price"), 20, Ui.ACCENT, true));
                String f = plan.optString("features", "");
                if (!f.isEmpty()) {
                    TextView ft = Ui.text(this, f, 11, Ui.MUTED, false);
                    ft.setGravity(Gravity.CENTER);
                    card.addView(ft);
                }
                LinearLayout.LayoutParams cl = Ui.lp(Ui.dp(this, 170), -2);
                cl.leftMargin = cl.rightMargin = Ui.dp(this, 6);
                row.addView(card, cl);
            }
            box.addView(row, tl(16));
        }

        String support = cfg.settings().optString("support", "");
        if (!support.isEmpty()) box.addView(Ui.text(this, support, 15, Ui.TEXT, false), tl(14));

        LinearLayout actions = Ui.row(this);
        TextView retry = pill("Tentar novamente");
        retry.setTag("first");
        retry.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { refresh(true); }
        });
        actions.addView(retry);
        TextView wifi = pill("Rede / Wi‑Fi");
        wifi.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) { Actions.system(HomeActivity.this, Settings.ACTION_WIFI_SETTINGS); }
        });
        LinearLayout.LayoutParams wl = Ui.lp(-2, -2);
        wl.leftMargin = Ui.dp(this, 12);
        actions.addView(wifi, wl);
        box.addView(actions, tl(16));
        return box;
    }

    private LinearLayout.LayoutParams tl(int topDp) {
        LinearLayout.LayoutParams l = Ui.lp(-2, -2);
        l.topMargin = Ui.dp(this, topDp);
        return l;
    }

    private TextView pill(String text) {
        TextView t = Ui.text(this, text, 15, Ui.TEXT, true);
        int h = Ui.dp(this, 22), v = Ui.dp(this, 10);
        t.setPadding(h, v, h, v);
        t.setBackground(Ui.card(this, Ui.ACCENT, Ui.dp(this, 22)));
        Ui.focusable(t, 1.06f);
        return t;
    }
}
