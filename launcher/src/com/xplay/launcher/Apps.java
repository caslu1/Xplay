package com.xplay.launcher;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.widget.Toast;

import org.json.JSONArray;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Installed apps: listing, launching and the apps the user pinned to the home screen. */
public final class Apps {

    public static final class App {
        public final String label;
        public final String pkg;

        App(String label, String pkg) {
            this.label = label;
            this.pkg = pkg;
        }
    }

    private Apps() {}

    /** Every app that can be opened from a launcher (phone or TV), sorted by name. */
    public static List<App> installed(Context c) {
        PackageManager pm = c.getPackageManager();
        Map<String, App> byPkg = new LinkedHashMap<>();
        for (String category : new String[] {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER}) {
            Intent i = new Intent(Intent.ACTION_MAIN).addCategory(category);
            for (ResolveInfo r : pm.queryIntentActivities(i, 0)) {
                String pkg = r.activityInfo.packageName;
                if (pkg.equals(c.getPackageName()) || byPkg.containsKey(pkg)) continue;
                byPkg.put(pkg, new App(String.valueOf(r.loadLabel(pm)), pkg));
            }
        }
        List<App> list = new ArrayList<>(byPkg.values());
        final Collator collator = Collator.getInstance();
        Collections.sort(list, new Comparator<App>() {
            public int compare(App a, App b) { return collator.compare(a.label, b.label); }
        });
        return list;
    }

    public static boolean isInstalled(Context c, String pkg) {
        try {
            c.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static int versionCode(Context c, String pkg) {
        try {
            PackageInfo p = c.getPackageManager().getPackageInfo(pkg, 0);
            return p.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    /**
     * The panel may list several package names separated by commas (for example the TV and the phone
     * build of YouTube); the first one installed wins. Returns null when none is installed.
     */
    public static String resolve(Context c, String pkgs) {
        if (pkgs == null) return null;
        for (String p : pkgs.split(",")) {
            p = p.trim();
            if (!p.isEmpty() && isInstalled(c, p)) return p;
        }
        return null;
    }

    public static String firstPackage(String pkgs) {
        if (pkgs == null) return "";
        String p = pkgs.split(",")[0].trim();
        return p;
    }

    public static Drawable icon(Context c, String pkg) {
        PackageManager pm = c.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            return info.loadIcon(pm);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static String label(Context c, String pkg) {
        PackageManager pm = c.getPackageManager();
        try {
            return String.valueOf(pm.getApplicationInfo(pkg, 0).loadLabel(pm));
        } catch (PackageManager.NameNotFoundException e) {
            return pkg;
        }
    }

    public static boolean launch(Activity a, String pkg) {
        PackageManager pm = a.getPackageManager();
        Intent i = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (i == null) i = pm.getLaunchIntentForPackage(pkg);
        if (i == null) {
            Toast.makeText(a, "Não foi possível abrir " + label(a, pkg), Toast.LENGTH_SHORT).show();
            return false;
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            a.startActivity(i);
            return true;
        } catch (Exception e) {
            Toast.makeText(a, "Não foi possível abrir " + label(a, pkg), Toast.LENGTH_SHORT).show();
            return false;
        }
    }

    public static void openInfo(Activity a, String pkg) {
        try {
            a.startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + pkg)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, "Tela de informações indisponível", Toast.LENGTH_SHORT).show();
        }
    }

    public static void uninstall(Activity a, String pkg) {
        try {
            a.startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + pkg)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(a, "Desinstalação indisponível", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Apps the user added to the home screen with the "+" tile -------------------------------

    public static List<String> pinned(Context c) {
        List<String> out = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(Config.prefs(c).getString("pinned", "[]"));
            for (int i = 0; i < a.length(); i++) out.add(a.getString(i));
        } catch (Exception ignored) {
            // Start over with no pinned apps.
        }
        return out;
    }

    private static void savePinned(Context c, List<String> list) {
        Config.prefs(c).edit().putString("pinned", new JSONArray(list).toString()).apply();
    }

    public static void pin(Context c, String pkg) {
        List<String> list = pinned(c);
        if (!list.contains(pkg)) list.add(pkg);
        savePinned(c, list);
    }

    public static void unpin(Context c, String pkg) {
        List<String> list = pinned(c);
        list.remove(pkg);
        savePinned(c, list);
    }

    public static void clearPinned(Context c) {
        savePinned(c, new ArrayList<String>());
    }
}
