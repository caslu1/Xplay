package com.xplay.iptv;

import android.app.Activity;
import android.text.InputType;
import android.view.Gravity;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Adult content (+18): such groups and categories ask for a PIN (default 0000) before opening, and
 * the "hide adult content" setting removes them from every list.
 */
public final class Adult {

    public static final String DEFAULT_PIN = "0000";

    private static final Pattern ADULT = Pattern.compile(
            "adult|adulto|\\+ ?18|18 ?\\+|xxx|porn|sexy|sexo|erot|hentai|playboy|hot ?club|onlyfans",
            Pattern.CASE_INSENSITIVE);

    /** Unlocked with the PIN for the rest of this app session. */
    private static boolean unlocked;

    private static List<Channel> liveSource;
    private static boolean liveVisible;
    private static List<Channel> liveFiltered;
    private static Map<Channel, Integer> numbers;
    private static List<Channel> numbered;

    private Adult() {}

    public static boolean isAdultName(String s) {
        return s != null && ADULT.matcher(s).find();
    }

    public static boolean isAdult(Channel c) {
        return isAdultName(c.group) || isAdultName(c.name);
    }

    public static boolean isAdult(VodItem v) {
        java.util.Map<String, String> cats = v.series ? Repo.seriesCategories : Repo.movieCategories;
        String cat = cats == null || v.categoryId == null ? null : cats.get(v.categoryId);
        return isAdultName(cat) || isAdultName(v.name);
    }

    public static boolean hidden() {
        return Repo.prefs().getBoolean("hide_adult", false);
    }

    public static void setHidden(boolean hide) {
        Repo.prefs().edit().putBoolean("hide_adult", hide).apply();
        if (hide) unlocked = false;
    }

    /** True when adult items may appear in mixed lists (all channels, rows, search). */
    public static boolean visible() {
        return !hidden() && unlocked;
    }

    /** Whether adult groups/categories are listed at all (locked or not). */
    public static boolean listed() {
        return !hidden();
    }

    /** Live channels to show in mixed lists. */
    public static List<Channel> channels() {
        List<Channel> all = Repo.channels;
        if (all == null) return new ArrayList<Channel>();
        boolean vis = visible();
        if (all == liveSource && vis == liveVisible && liveFiltered != null) return liveFiltered;
        List<Channel> out = vis ? all : new ArrayList<Channel>();
        if (!vis) for (Channel c : all) if (!isAdult(c)) out.add(c);
        liveSource = all;
        liveVisible = vis;
        liveFiltered = out;
        return out;
    }

    public static List<VodItem> filter(List<VodItem> items) {
        if (items == null) return null;
        if (visible()) return items;
        List<VodItem> out = new ArrayList<VodItem>(items.size());
        for (VodItem v : items) if (!isAdult(v)) out.add(v);
        return out;
    }

    /** Channel number: position in the full list, so numbers never change with filters. */
    public static int number(Channel c) {
        List<Channel> all = Repo.channels;
        if (all == null) return 0;
        if (all != numbered) {
            numbers = new IdentityHashMap<Channel, Integer>();
            for (int i = 0; i < all.size(); i++) numbers.put(all.get(i), i + 1);
            numbered = all;
        }
        Integer n = numbers.get(c);
        return n == null ? 0 : n;
    }

    /** Runs action right away when allowed, otherwise asks for the PIN first. */
    public static void unlock(final Activity a, final Runnable action) {
        if (unlocked) {
            action.run();
            return;
        }
        askPin(a, "Conteúdo adulto (+18)", new Runnable() {
            public void run() {
                unlocked = true;
                action.run();
            }
        });
    }

    /** Asks for the PIN and runs onOk when it is right. */
    public static void askPin(final Activity a, String title, final Runnable onOk) {
        final Popup p = new Popup(a, title, "Digite a senha para continuar.");
        final EditText input = p.pinInput("Senha");
        p.button("Cancelar", false, null).button("OK", true, new Runnable() {
            public void run() {
                if (input.getText().toString().equals(pin())) {
                    p.dismiss();
                    onOk.run();
                } else {
                    p.error("Senha incorreta");
                }
            }
        }, true).show();
    }

    public static String pin() {
        return Repo.prefs().getString("adult_pin", DEFAULT_PIN);
    }

    public static void setPin(String pin) {
        Repo.prefs().edit().putString("adult_pin", pin).apply();
    }
}
