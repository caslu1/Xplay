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
    private static final int LEARN_BOOST = 400; // learned words outrank the dictionary

    private final SharedPreferences prefs;
    private final Context ctx;
    private JSONObject words;

    /** The built-in dictionary: correct word -> rough frequency, plus an accent-free index. */
    private java.util.HashMap<String, Integer> dict;
    private java.util.HashMap<String, String> byNorm; // "voce" -> "você", "obrigado" -> "obrigado"

    Store(Context c) {
        ctx = c.getApplicationContext();
        prefs = c.getSharedPreferences("teclado", Context.MODE_PRIVATE);
    }

    // ---- Text helpers for accent-insensitive matching.

    /** Lowercase and strip accents, so "Você" and "voce" compare equal. */
    static String norm(String s) {
        String n = java.text.Normalizer.normalize(s.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD);
        return n.replaceAll("\\p{Mn}+", "");
    }

    /** Puts a dictionary word into the same case the user is typing in. */
    private static String withCase(String word, String typed) {
        if (typed.isEmpty()) return word;
        if (typed.length() > 1 && typed.equals(typed.toUpperCase(Locale.ROOT))) return word.toUpperCase(Locale.ROOT);
        if (Character.isUpperCase(typed.charAt(0))) return Character.toUpperCase(word.charAt(0)) + word.substring(1);
        return word;
    }

    private void ensureDict() {
        if (dict != null) return;
        dict = new java.util.HashMap<>();
        byNorm = new java.util.HashMap<>();
        try {
            java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(
                    ctx.getResources().openRawResource(R.raw.words), "UTF-8"));
            String line;
            int rank = 0, total = 2000;
            while ((line = r.readLine()) != null) {
                String w = line.trim();
                if (w.isEmpty()) continue;
                int freq = Math.max(10, total - rank);
                dict.put(w, freq);
                String nk = norm(w);
                // Keep the most frequent spelling for each accent-free form.
                String prev = byNorm.get(nk);
                if (prev == null || dict.get(prev) < freq) byNorm.put(nk, w);
                rank++;
            }
            r.close();
        } catch (Exception ignored) {
        }
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

    /** The combined vocabulary (dictionary + learned words) with a usable frequency each. */
    private java.util.HashMap<String, Integer> vocabulary() {
        ensureDict();
        java.util.HashMap<String, Integer> v = new java.util.HashMap<>(dict);
        JSONObject w = words();
        for (Iterator<String> it = w.keys(); it.hasNext(); ) {
            String k = it.next();
            Integer base = v.get(k);
            v.put(k, (base == null ? 0 : base) + LEARN_BOOST + w.optInt(k));
        }
        return v;
    }

    /**
     * Up to n suggestions for what is being typed. Matches ignore accents, so "voce" offers
     * "você"; words that merely start with the prefix come first, then close misspellings.
     */
    List<String> suggest(String prefix, int n) {
        List<String> out = new ArrayList<>();
        if (prefix.isEmpty()) return out;
        final String np = norm(prefix);
        final java.util.HashMap<String, Integer> vocab = vocabulary();

        final List<String> starts = new ArrayList<>();
        final List<String> near = new ArrayList<>();
        for (String word : vocab.keySet()) {
            String nw = norm(word);
            if (nw.equals(np)) continue;
            if (nw.startsWith(np)) starts.add(word);
            else if (np.length() >= 3 && Math.abs(nw.length() - np.length()) <= 1 && editDistance(np, nw, 1) <= 1) {
                near.add(word);
            }
        }
        final Comparator<String> byFreq = new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                int d = vocab.get(b) - vocab.get(a);
                return d != 0 ? d : a.length() - b.length();
            }
        };
        Collections.sort(starts, byFreq);
        Collections.sort(near, byFreq);

        // If what was typed is itself a correct word, keep it as one of the options too.
        boolean typedIsWord = byNorm.containsKey(np) && byNorm.get(np).equalsIgnoreCase(prefix);
        for (String s : starts) { if (out.size() < n) add(out, withCase(s, prefix)); }
        for (String s : near) { if (out.size() < n) add(out, withCase(s, prefix)); }
        if (!typedIsWord && byNorm.containsKey(np)) add(out, withCase(byNorm.get(np), prefix));
        return out.size() > n ? out.subList(0, n) : out;
    }

    /**
     * The autocorrected form of a finished word, or null to leave it alone. Fixes missing accents
     * ("voce" -> "você") and small typos ("qeuro" -> "quero"); never touches short words, words
     * with digits, or words the user has taught the keyboard.
     */
    String correct(String word) {
        if (word == null || word.length() < 3 || word.length() > 24) return null;
        for (int i = 0; i < word.length(); i++) {
            if (Character.isDigit(word.charAt(i))) return null;
        }
        ensureDict();
        JSONObject w = words();
        if (w.has(word.toLowerCase(Locale.ROOT))) return null; // the user types it on purpose
        String nw = norm(word);
        // Already a correct spelling (perhaps only the accents are missing).
        if (byNorm.containsKey(nw)) {
            String right = withCase(byNorm.get(nw), word);
            return right.equals(word) ? null : right;
        }
        // Otherwise look for the closest correct word.
        int max = word.length() <= 4 ? 1 : 2;
        String best = null;
        int bestDist = max + 1, bestFreq = -1;
        for (java.util.Map.Entry<String, String> e : byNorm.entrySet()) {
            String cand = e.getKey();
            if (Math.abs(cand.length() - nw.length()) > max) continue;
            if (cand.charAt(0) != nw.charAt(0)) continue; // typos rarely change the first letter
            int d = editDistance(nw, cand, max);
            if (d > max) continue;
            int freq = dict.get(e.getValue());
            if (d < bestDist || (d == bestDist && freq > bestFreq)) {
                bestDist = d; bestFreq = freq; best = e.getValue();
            }
        }
        return best == null ? null : withCase(best, word);
    }

    private static void add(List<String> out, String s) {
        if (!out.contains(s)) out.add(s);
    }

    /** Levenshtein distance, giving up (returns max+1) once it is clearly over the limit. */
    private static int editDistance(String a, String b, int max) {
        int la = a.length(), lb = b.length();
        if (Math.abs(la - lb) > max) return max + 1;
        int[] prev = new int[lb + 1];
        int[] cur = new int[lb + 1];
        for (int j = 0; j <= lb; j++) prev[j] = j;
        for (int i = 1; i <= la; i++) {
            cur[0] = i;
            int rowMin = cur[0];
            char ca = a.charAt(i - 1);
            for (int j = 1; j <= lb; j++) {
                int cost = ca == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(prev[j] + 1, cur[j - 1] + 1), prev[j - 1] + cost);
                rowMin = Math.min(rowMin, cur[j]);
            }
            if (rowMin > max) return max + 1;
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[lb];
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
