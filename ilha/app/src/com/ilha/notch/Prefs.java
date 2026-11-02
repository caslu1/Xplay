package com.ilha.notch;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

/** All user settings. Camera geometry is stored in pixels for this device. */
final class Prefs {
    static final String NAME = "ilha";

    private Prefs() {}

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    static boolean calibrated(Context c) { return get(c).getBoolean("calibrated", false); }
    /** Camera centre, horizontal offset from the screen centre (px). */
    static int camX(Context c) { return get(c).getInt("cam_x", 0); }
    /** Camera centre, distance from the top of the screen (px). */
    static int camY(Context c) { return get(c).getInt("cam_y", -1); }
    /** Camera diameter (px). */
    static int camD(Context c) { return get(c).getInt("cam_d", -1); }
    /** Extra width of the compact pill, in dp. */
    static int pillW(Context c) { return get(c).getInt("pill_w", 150); }

    static void setCamera(Context c, int x, int y, int d) {
        get(c).edit().putInt("cam_x", x).putInt("cam_y", y).putInt("cam_d", d)
                .putBoolean("calibrated", true).apply();
    }

    static boolean music(Context c) { return get(c).getBoolean("music", true); }
    static boolean notifs(Context c) { return get(c).getBoolean("notifs", true); }
    static boolean live(Context c) { return get(c).getBoolean("live", true); }
    static boolean calls(Context c) { return get(c).getBoolean("calls", true); }
    static boolean audio(Context c) { return get(c).getBoolean("audio", true); }
    static boolean vibrate(Context c) { return get(c).getBoolean("vibrate", false); }
    static boolean hideLandscape(Context c) { return get(c).getBoolean("hide_land", true); }
    /** Seconds a notification stays open on the island. */
    static int duration(Context c) { return get(c).getInt("duration", 5); }

    static boolean appAllowed(Context c, String pkg) {
        return !get(c).getStringSet("blocked", new HashSet<String>()).contains(pkg);
    }

    static void setAppAllowed(Context c, String pkg, boolean allowed) {
        Set<String> s = new HashSet<>(get(c).getStringSet("blocked", new HashSet<String>()));
        if (allowed) s.remove(pkg); else s.add(pkg);
        get(c).edit().putStringSet("blocked", s).apply();
    }

    static Set<String> seen(Context c) {
        return new HashSet<>(get(c).getStringSet("seen", new HashSet<String>()));
    }

    static void markSeen(Context c, String pkg) {
        Set<String> s = seen(c);
        if (s.add(pkg)) get(c).edit().putStringSet("seen", s).apply();
    }
}
