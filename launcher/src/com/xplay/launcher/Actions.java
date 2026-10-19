package com.xplay.launcher;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * What a main button, an ad or an app tile does when clicked. The panel stores it as
 * {"type": "...", "value": "..."}.
 */
public final class Actions {

    private Actions() {}

    public static void run(Activity a, JSONObject action) {
        if (action == null) return;
        String type = action.optString("type", "none");
        String value = action.optString("value", "").trim();
        switch (type) {
            case "app":
                openApp(a, value);
                break;
            case "url":
                if (!value.isEmpty()) openUri(a, value);
                break;
            case "store":
                a.startActivity(new Intent(a, StoreActivity.class));
                break;
            case "allapps":
                a.startActivity(new Intent(a, AppsActivity.class));
                break;
            case "settings":
                system(a, Settings.ACTION_SETTINGS);
                break;
            case "wifi":
                system(a, Settings.ACTION_WIFI_SETTINGS);
                break;
            case "launcher_settings":
                SettingsActivity.open(a);
                break;
            default:
                break;
        }
    }

    /** Opens the first installed package of a comma separated list, or offers to install it. */
    public static void openApp(Activity a, String pkgs) {
        String pkg = Apps.resolve(a, pkgs);
        if (pkg != null) {
            Apps.launch(a, pkg);
            return;
        }
        JSONArray store = Config.get(a).store().optJSONArray("apps");
        if (store != null) {
            for (String p : pkgs.split(",")) {
                for (int i = 0; i < store.length(); i++) {
                    JSONObject s = store.optJSONObject(i);
                    if (s != null && p.trim().equals(s.optString("package"))) {
                        Toast.makeText(a, "App não instalado. Instale pela loja.", Toast.LENGTH_SHORT).show();
                        a.startActivity(new Intent(a, StoreActivity.class).putExtra("package", p.trim()));
                        return;
                    }
                }
            }
        }
        String first = Apps.firstPackage(pkgs);
        if (first.isEmpty() || !openUri(a, "market://details?id=" + first)) {
            Toast.makeText(a, "App não instalado neste aparelho", Toast.LENGTH_SHORT).show();
        }
    }

    static boolean openUri(Activity a, String uri) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(i);
            return true;
        } catch (Exception e) {
            if (!uri.startsWith("market:")) Toast.makeText(a, "Nenhum app abre este link", Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    static void system(Activity a, String action) {
        try {
            a.startActivity(new Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            try {
                a.startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e2) {
                Toast.makeText(a, "Configurações indisponíveis", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
