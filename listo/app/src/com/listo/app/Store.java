package com.listo.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** All app data, kept as one JSON document in SharedPreferences. */
public class Store {

    public static class Item {
        public long id;
        public String text;
        public boolean done;
    }

    public static class TaskList {
        public long id;
        public String name;
        public int theme;
        public List<Item> items = new ArrayList<Item>();
    }

    public static class Task {
        public long id;
        public String title;
        public String date;   // yyyy-MM-dd
        public String time;   // HH:mm or ""
        public int priority;  // 0 none, 1 low, 2 medium, 3 high
        public boolean done;
    }

    public final List<TaskList> lists = new ArrayList<TaskList>();
    public final List<Task> tasks = new ArrayList<Task>();

    private final SharedPreferences prefs;
    private long nextId = 1;

    public Store(Context ctx) {
        prefs = ctx.getSharedPreferences("listo", Context.MODE_PRIVATE);
        String raw = prefs.getString("data", null);
        if (raw == null) {
            seed();
            save();
        } else {
            load(raw);
        }
    }

    public long newId() {
        return nextId++;
    }

    public static String key(int y, int m, int d) {
        return String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d);
    }

    public static String key(Calendar c) {
        return key(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
    }

    public List<Task> tasksOn(String date) {
        List<Task> out = new ArrayList<Task>();
        for (Task t : tasks) if (date.equals(t.date)) out.add(t);
        Collections.sort(out, new Comparator<Task>() {
            @Override public int compare(Task a, Task b) {
                if (a.done != b.done) return a.done ? 1 : -1;
                String ta = a.time.isEmpty() ? "99" : a.time;
                String tb = b.time.isEmpty() ? "99" : b.time;
                int c = ta.compareTo(tb);
                return c != 0 ? c : b.priority - a.priority;
            }
        });
        return out;
    }

    /** Dates that still have unfinished tasks. */
    public Set<String> busyDates() {
        Set<String> out = new HashSet<String>();
        for (Task t : tasks) if (!t.done) out.add(t.date);
        return out;
    }

    public void save() {
        try {
            JSONObject root = new JSONObject();
            root.put("nextId", nextId);
            JSONArray ls = new JSONArray();
            for (TaskList l : lists) {
                JSONObject o = new JSONObject();
                o.put("id", l.id);
                o.put("name", l.name);
                o.put("theme", l.theme);
                JSONArray items = new JSONArray();
                for (Item i : l.items) {
                    JSONObject io = new JSONObject();
                    io.put("id", i.id);
                    io.put("text", i.text);
                    io.put("done", i.done);
                    items.put(io);
                }
                o.put("items", items);
                ls.put(o);
            }
            root.put("lists", ls);
            JSONArray ts = new JSONArray();
            for (Task t : tasks) {
                JSONObject o = new JSONObject();
                o.put("id", t.id);
                o.put("title", t.title);
                o.put("date", t.date);
                o.put("time", t.time);
                o.put("priority", t.priority);
                o.put("done", t.done);
                ts.put(o);
            }
            root.put("tasks", ts);
            prefs.edit().putString("data", root.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    private void load(String raw) {
        try {
            JSONObject root = new JSONObject(raw);
            nextId = root.optLong("nextId", 1);
            JSONArray ls = root.optJSONArray("lists");
            for (int i = 0; ls != null && i < ls.length(); i++) {
                JSONObject o = ls.getJSONObject(i);
                TaskList l = new TaskList();
                l.id = o.optLong("id");
                l.name = o.optString("name");
                l.theme = o.optInt("theme");
                JSONArray items = o.optJSONArray("items");
                for (int j = 0; items != null && j < items.length(); j++) {
                    JSONObject io = items.getJSONObject(j);
                    Item it = new Item();
                    it.id = io.optLong("id");
                    it.text = io.optString("text");
                    it.done = io.optBoolean("done");
                    l.items.add(it);
                }
                lists.add(l);
            }
            JSONArray ts = root.optJSONArray("tasks");
            for (int i = 0; ts != null && i < ts.length(); i++) {
                JSONObject o = ts.getJSONObject(i);
                Task t = new Task();
                t.id = o.optLong("id");
                t.title = o.optString("title");
                t.date = o.optString("date");
                t.time = o.optString("time");
                t.priority = o.optInt("priority");
                t.done = o.optBoolean("done");
                tasks.add(t);
            }
        } catch (Exception e) {
            lists.clear();
            tasks.clear();
            seed();
        }
    }

    private TaskList addList(String name, int theme, String... items) {
        TaskList l = new TaskList();
        l.id = newId();
        l.name = name;
        l.theme = theme;
        for (String s : items) {
            Item it = new Item();
            it.id = newId();
            it.text = s;
            l.items.add(it);
        }
        lists.add(l);
        return l;
    }

    private void addTask(Calendar c, String title, String time, int priority) {
        Task t = new Task();
        t.id = newId();
        t.title = title;
        t.date = key(c);
        t.time = time;
        t.priority = priority;
        tasks.add(t);
    }

    private void seed() {
        addList("Lista de estudos", 0, "🌍 Aula de francês", "📒 Aula de matemática", "🌿 Lição de biologia");
        addList("Lista de compras", 1, "🍅 Tomate", "🥦 Brócolis", "🍫 Chocolate");
        addList("Lista de filmes", 2, "Peixe Grande", "Nomadland", "Raya e o Último Dragão");

        Calendar c = Calendar.getInstance();
        addTask(c, "Apresentação", "07:00", 3);
        addTask(c, "Comprar frutas", "", 1);
        addTask(c, "Compras no mercado", "07:00", 2);
        c.add(Calendar.DAY_OF_MONTH, 2);
        addTask(c, "Consulta no dentista", "14:30", 2);
        c.add(Calendar.DAY_OF_MONTH, 5);
        addTask(c, "Aniversário da Ana 🎂", "", 1);
    }
}
