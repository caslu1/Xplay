package com.xplay.teclado;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/** Everything the keyboard remembers, kept in one SharedPreferences file on the phone. */
final class Store {
    static final class Item {
        String text;
        long time;
        boolean pinned;

        Item(String text, long time, boolean pinned) {
            this.text = text;
            this.time = time;
            this.pinned = pinned;
        }
    }

    static final float MIN_SCALE = 0.6f, MAX_SCALE = 1.6f;
    private static final int MAX_CLIPS = 60, MAX_HISTORY = 1000, MAX_WORDS = 5000;

    /** Seeds the suggestions before the keyboard has learned anything. */
    private static final String COMMON = "de a o que e do da em um para é com não uma os no se na por mais as dos como"
            + " mas foi ao ele das tem à seu sua ou ser quando muito há nos já está eu também só pelo pela até isso ela"
            + " entre era depois sem mesmo aos ter seus quem nas me esse eles estão você tinha foram essa num nem suas"
            + " meu às minha têm numa pelos elas havia seja qual será nós tenho lhe deles essas esses pelas este fosse"
            + " dele tu te vocês vos lhes meus minhas teu tua teus tuas nosso nossa nossos nossas obrigado obrigada"
            + " bom boa dia tarde noite tudo bem beleza certo ok sim valeu amanhã hoje agora onde porque pode fazer"
            + " quero preciso vamos aqui ali então ainda sempre nunca casa trabalho mensagem favor desculpa abraço";

    private final SharedPreferences prefs;
    private JSONObject words;

    Store(Context c) {
        prefs = c.getSharedPreferences("teclado", Context.MODE_PRIVATE);
    }

    int theme() { return prefs.getInt("theme", 0); }
    void setTheme(int i) { prefs.edit().putInt("theme", i).apply(); }

    float scale() { return prefs.getFloat("scale", 1f); }
    void setScale(float s) { prefs.edit().putFloat("scale", clampScale(s)).apply(); }
    static float clampScale(float s) { return Math.max(MIN_SCALE, Math.min(MAX_SCALE, s)); }

    boolean recording() { return prefs.getBoolean("recording", true); }
    void setRecording(boolean on) { prefs.edit().putBoolean("recording", on).apply(); }

    boolean vibrate() { return prefs.getBoolean("vibrate", true); }
    void setVibrate(boolean on) { prefs.edit().putBoolean("vibrate", on).apply(); }

    boolean numberRow() { return prefs.getBoolean("numberRow", false); }
    void setNumberRow(boolean on) { prefs.edit().putBoolean("numberRow", on).apply(); }

    // ---- Clipboard: pinned first, then newest first.

    List<Item> clips() { return load("clips"); }

    void addClip(String text) {
        if (text == null || text.trim().isEmpty()) return;
        List<Item> list = clips();
        boolean pinned = false;
        for (Iterator<Item> it = list.iterator(); it.hasNext(); ) {
            Item i = it.next();
            if (i.text.equals(text)) { pinned = i.pinned; it.remove(); }
        }
        list.add(0, new Item(text, System.currentTimeMillis(), pinned));
        int unpinned = 0;
        for (Iterator<Item> it = list.iterator(); it.hasNext(); ) {
            if (!it.next().pinned && ++unpinned > MAX_CLIPS) it.remove();
        }
        save("clips", list);
    }

    void togglePin(Item item) {
        List<Item> list = clips();
        for (Item i : list) if (i.text.equals(item.text)) i.pinned = !i.pinned;
        save("clips", list);
    }

    void removeClip(Item item) { remove("clips", item); }

    void clearClips() {
        List<Item> list = clips();
        for (Iterator<Item> it = list.iterator(); it.hasNext(); ) if (!it.next().pinned) it.remove();
        save("clips", list);
    }

    static List<Item> sortedClips(List<Item> list) {
        List<Item> out = new ArrayList<>();
        for (Item i : list) if (i.pinned) out.add(i);
        for (Item i : list) if (!i.pinned) out.add(i);
        return out;
    }

    // ---- History of everything typed or dictated, newest first.

    List<Item> history() { return load("history"); }

    void addHistory(String text) {
        text = text.trim();
        if (text.isEmpty()) return;
        List<Item> list = history();
        list.add(0, new Item(text, System.currentTimeMillis(), false));
        while (list.size() > MAX_HISTORY) list.remove(list.size() - 1);
        save("history", list);
        learn(text);
    }

    void removeHistory(Item item) { remove("history", item); }
    void clearHistory() { prefs.edit().remove("history").apply(); }

    // ---- Word suggestions.

    void learn(String text) {
        JSONObject w = words();
        for (String word : text.toLowerCase(Locale.ROOT).split("[^\\p{L}']+")) {
            if (word.length() < 2 || word.length() > 30) continue;
            try {
                w.put(word, w.optInt(word) + 3);
            } catch (JSONException ignored) {
            }
        }
        if (w.length() > MAX_WORDS) trimWords(w);
        prefs.edit().putString("words", w.toString()).apply();
    }

    /** Up to n known words starting with prefix, most used first, in the prefix's case. */
    List<String> suggest(String prefix, int n) {
        List<String> out = new ArrayList<>();
        if (prefix.isEmpty()) return out;
        String p = prefix.toLowerCase(Locale.ROOT);
        final JSONObject w = words();
        List<String> hits = new ArrayList<>();
        for (Iterator<String> it = w.keys(); it.hasNext(); ) {
            String k = it.next();
            if (k.startsWith(p) && !k.equals(p)) hits.add(k);
        }
        for (String k : COMMON.split(" ")) {
            if (k.startsWith(p) && !k.equals(p) && !w.has(k)) hits.add(k);
        }
        Collections.sort(hits, new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                int d = w.optInt(b) - w.optInt(a);
                return d != 0 ? d : a.length() - b.length();
            }
        });
        boolean upper = Character.isUpperCase(prefix.charAt(0));
        boolean allUpper = prefix.length() > 1 && prefix.equals(prefix.toUpperCase(Locale.ROOT));
        for (int i = 0; i < hits.size() && out.size() < n; i++) {
            String s = hits.get(i);
            if (allUpper) s = s.toUpperCase(Locale.ROOT);
            else if (upper) s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
            out.add(s);
        }
        return out;
    }

    private JSONObject words() {
        if (words == null) {
            try {
                words = new JSONObject(prefs.getString("words", "{}"));
            } catch (JSONException e) {
                words = new JSONObject();
            }
        }
        return words;
    }

    private static void trimWords(JSONObject w) {
        List<String> drop = new ArrayList<>();
        for (Iterator<String> it = w.keys(); it.hasNext(); ) {
            String k = it.next();
            if (w.optInt(k) <= 3) drop.add(k);
        }
        for (String k : drop) w.remove(k);
    }

    // ---- JSON list helpers.

    private List<Item> load(String key) {
        List<Item> list = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs.getString(key, "[]"));
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                list.add(new Item(o.getString("t"), o.optLong("at"), o.optBoolean("pin")));
            }
        } catch (JSONException ignored) {
        }
        return list;
    }

    private void save(String key, List<Item> list) {
        JSONArray a = new JSONArray();
        try {
            for (Item i : list) a.put(new JSONObject().put("t", i.text).put("at", i.time).put("pin", i.pinned));
        } catch (JSONException ignored) {
        }
        prefs.edit().putString(key, a.toString()).apply();
    }

    private void remove(String key, Item item) {
        List<Item> list = load(key);
        for (Iterator<Item> it = list.iterator(); it.hasNext(); ) {
            Item i = it.next();
            if (i.text.equals(item.text) && i.time == item.time) { it.remove(); break; }
        }
        save(key, list);
    }
}
