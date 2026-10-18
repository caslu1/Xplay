package com.xplay.iptv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;

public class MainActivity extends Activity {

    private static final String PREFS = "xplay";
    private static final String KEY_MODE = "mode";
    private static final String MODE_M3U = "m3u";
    private static final String MODE_XTREAM = "xtream";
    private static final String KEY_URL = "playlist_url";
    private static final String KEY_DNS = "xtream_dns";
    private static final String KEY_USER = "xtream_user";
    private static final String KEY_PASS = "xtream_pass";
    private static final String KEY_FAVS = "favorites";
    private static final String ALL = "Todos os canais";
    private static final String FAVS = "★ Favoritos";

    /** Channels currently shown in the list; read by PlayerActivity for zapping. */
    static List<Channel> visible = new ArrayList<Channel>();

    private final Handler main = new Handler(Looper.getMainLooper());
    private final ImageLoader images = new ImageLoader();

    private SharedPreferences prefs;
    private List<Channel> all = new ArrayList<Channel>();
    private Set<String> favorites;

    private Spinner groupSpinner;
    private EditText search;
    private ListView list;
    private TextView status;
    private ProgressBar progress;
    private ChannelAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        favorites = new HashSet<String>(prefs.getStringSet(KEY_FAVS, new HashSet<String>()));
        buildUi();

        if (!loadSaved()) askForSource();
    }

    /** Reloads the saved source; returns false when nothing is configured yet. */
    private boolean loadSaved() {
        if (MODE_XTREAM.equals(prefs.getString(KEY_MODE, MODE_M3U))) {
            String dns = prefs.getString(KEY_DNS, null);
            if (dns == null) return false;
            loadXtream(dns, prefs.getString(KEY_USER, ""), prefs.getString(KEY_PASS, ""));
        } else {
            String url = prefs.getString(KEY_URL, null);
            if (url == null) return false;
            loadPlaylist(url);
        }
        return true;
    }

    private boolean hasSource() {
        return prefs.contains(KEY_URL) || prefs.contains(KEY_DNS);
    }

    private void buildUi() {
        int pad = dp(12);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, 0);

        groupSpinner = new Spinner(this);
        root.addView(groupSpinner, new LinearLayout.LayoutParams(-1, -2));

        search = new EditText(this);
        search.setHint("Buscar canal");
        search.setSingleLine(true);
        root.addView(search, new LinearLayout.LayoutParams(-1, -2));

        progress = new ProgressBar(this);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(-2, -2);
        pp.gravity = Gravity.CENTER_HORIZONTAL;
        pp.topMargin = dp(24);
        root.addView(progress, pp);

        status = new TextView(this);
        status.setGravity(Gravity.CENTER);
        status.setPadding(pad, pad, pad, pad);
        root.addView(status, new LinearLayout.LayoutParams(-1, -2));

        list = new ListView(this);
        list.setFastScrollEnabled(true);
        adapter = new ChannelAdapter();
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1f));

        setContentView(root);
        progress.setVisibility(View.GONE);

        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) { applyFilter(); }
        });
        groupSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> p, View v, int pos, long id) { applyFilter(); }
            public void onNothingSelected(AdapterView<?> p) {}
        });
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Intent i = new Intent(MainActivity.this, PlayerActivity.class);
                i.putExtra(PlayerActivity.EXTRA_INDEX, pos);
                startActivity(i);
            }
        });
        list.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                toggleFavorite(visible.get(pos));
                return true;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "Recarregar lista");
        menu.add(0, 2, 1, "Trocar lista / login");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            if (!loadSaved()) askForSource();
            return true;
        }
        if (item.getItemId() == 2) {
            askForSource();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void askForSource() {
        new AlertDialog.Builder(this)
                .setTitle("Como deseja entrar?")
                .setItems(new String[] {"Login Xtream (DNS, usuário e senha)", "Link da lista M3U"},
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int which) {
                                if (which == 0) askForXtream();
                                else askForPlaylist();
                            }
                        })
                .show();
    }

    private void askForXtream() {
        final EditText dns = field("DNS (ex.: http://servidor.com:8080)",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, KEY_DNS);
        final EditText user = field("Usuário", InputType.TYPE_CLASS_TEXT, KEY_USER);
        final EditText pass = field("Senha",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, KEY_PASS);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(dns, new LinearLayout.LayoutParams(-1, -2));
        box.addView(user, new LinearLayout.LayoutParams(-1, -2));
        box.addView(pass, new LinearLayout.LayoutParams(-1, -2));

        new AlertDialog.Builder(this)
                .setTitle("Login Xtream")
                .setView(box)
                .setPositiveButton("Entrar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        String s = dns.getText().toString().trim();
                        String u = user.getText().toString().trim();
                        String p = pass.getText().toString().trim();
                        if (s.isEmpty() || u.isEmpty() || p.isEmpty()) {
                            showStatus("Preencha DNS, usuário e senha pelo menu ⋮");
                            return;
                        }
                        s = XtreamClient.normalizeServer(s);
                        prefs.edit().putString(KEY_MODE, MODE_XTREAM).putString(KEY_DNS, s)
                                .putString(KEY_USER, u).putString(KEY_PASS, p).apply();
                        loadXtream(s, u, p);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private EditText field(String hint, int inputType, String prefKey) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setSingleLine(true);
        e.setInputType(inputType);
        e.setText(prefs.getString(prefKey, ""));
        return e;
    }

    private void askForPlaylist() {
        final EditText input = new EditText(this);
        input.setHint("http://servidor/lista.m3u");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(prefs.getString(KEY_URL, ""));
        LinearLayout box = new LinearLayout(this);
        box.setPadding(dp(20), dp(8), dp(20), 0);
        box.addView(input, new LinearLayout.LayoutParams(-1, -2));

        new AlertDialog.Builder(this)
                .setTitle("Link da lista IPTV (M3U)")
                .setView(box)
                .setPositiveButton("Carregar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        String url = input.getText().toString().trim();
                        if (url.isEmpty()) {
                            showStatus("Nenhum link informado. Use o menu para adicionar.");
                            return;
                        }
                        if (!url.contains("://")) url = "http://" + url;
                        prefs.edit().putString(KEY_MODE, MODE_M3U).putString(KEY_URL, url).apply();
                        loadPlaylist(url);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void loadPlaylist(final String url) {
        load("Carregando lista…", new Callable<List<Channel>>() {
            public List<Channel> call() throws Exception { return M3uParser.download(url); }
        });
    }

    private void loadXtream(final String dns, final String user, final String pass) {
        load("Entrando no servidor…", new Callable<List<Channel>>() {
            public List<Channel> call() throws Exception { return XtreamClient.load(dns, user, pass); }
        });
    }

    private void load(String message, final Callable<List<Channel>> loader) {
        progress.setVisibility(View.VISIBLE);
        showStatus(message);
        new Thread(new Runnable() {
            public void run() {
                try {
                    final List<Channel> result = loader.call();
                    main.post(new Runnable() {
                        public void run() { onPlaylistLoaded(result); }
                    });
                } catch (final Exception e) {
                    main.post(new Runnable() {
                        public void run() {
                            progress.setVisibility(View.GONE);
                            showStatus("Erro ao carregar:\n" + e.getMessage()
                                    + "\n\nConfira os dados no menu ⋮");
                        }
                    });
                }
            }
        }).start();
    }

    private void onPlaylistLoaded(List<Channel> channels) {
        progress.setVisibility(View.GONE);
        all = channels;
        Set<String> groups = new LinkedHashSet<String>();
        groups.add(ALL);
        groups.add(FAVS);
        for (Channel c : channels) groups.add(c.group);
        ArrayAdapter<String> ga = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, new ArrayList<String>(groups));
        groupSpinner.setAdapter(ga);
        applyFilter();
        Toast.makeText(this, channels.size() + " canais carregados", Toast.LENGTH_SHORT).show();
    }

    private void applyFilter() {
        Object sel = groupSpinner.getSelectedItem();
        String group = sel == null ? ALL : sel.toString();
        String q = search.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<Channel> out = new ArrayList<Channel>();
        for (Channel c : all) {
            if (group.equals(FAVS)) {
                if (!favorites.contains(c.url)) continue;
            } else if (!group.equals(ALL) && !group.equals(c.group)) {
                continue;
            }
            if (!q.isEmpty() && !c.name.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(c);
        }
        visible = out;
        adapter.notifyDataSetChanged();
        if (all.isEmpty()) showStatus(hasSource()
                ? "A lista não tem canais." : "Adicione sua lista ou login pelo menu ⋮");
        else if (out.isEmpty()) showStatus("Nenhum canal encontrado.");
        else status.setVisibility(View.GONE);
    }

    private void toggleFavorite(Channel c) {
        boolean added;
        if (favorites.contains(c.url)) {
            favorites.remove(c.url);
            added = false;
        } else {
            favorites.add(c.url);
            added = true;
        }
        prefs.edit().putStringSet(KEY_FAVS, new HashSet<String>(favorites)).apply();
        Toast.makeText(this, added ? "Adicionado aos favoritos" : "Removido dos favoritos",
                Toast.LENGTH_SHORT).show();
        applyFilter();
    }

    private void showStatus(String text) {
        status.setText(text);
        status.setVisibility(View.VISIBLE);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private class ChannelAdapter extends BaseAdapter {
        public int getCount() { return visible.size(); }
        public Object getItem(int pos) { return visible.get(pos); }
        public long getItemId(int pos) { return pos; }

        public View getView(int pos, View convert, ViewGroup parent) {
            LinearLayout row;
            ImageView logo;
            TextView name;
            TextView star;
            if (convert == null) {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(4), dp(8), dp(4), dp(8));
                logo = new ImageView(MainActivity.this);
                logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
                row.addView(logo, new LinearLayout.LayoutParams(dp(48), dp(36)));
                name = new TextView(MainActivity.this);
                name.setTextSize(16);
                name.setSingleLine(true);
                name.setPadding(dp(12), 0, dp(8), 0);
                row.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
                star = new TextView(MainActivity.this);
                star.setTextColor(Color.parseColor("#F9C74F"));
                star.setTextSize(18);
                row.addView(star, new LinearLayout.LayoutParams(-2, -2));
            } else {
                row = (LinearLayout) convert;
                logo = (ImageView) row.getChildAt(0);
                name = (TextView) row.getChildAt(1);
                star = (TextView) row.getChildAt(2);
            }
            Channel c = visible.get(pos);
            name.setText(c.name);
            star.setText(favorites.contains(c.url) ? "★" : "");
            images.load(c.logo, logo);
            return row;
        }
    }
}
