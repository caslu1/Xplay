package com.xplay.iptv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * In-app updates: reads releases/latest.json from the repository, offers the newer APK and hands
 * the download to the system installer. Updates install over the app because every build is signed
 * with the same key.
 */
public final class Updater {

    static final String LATEST_URL =
            "https://raw.githubusercontent.com/caslu1/Xplay/ccr-e02be4ba-gz9bjy/releases/latest.json";

    private static boolean checkedThisRun;

    private Updater() {}

    public static int currentCode(Activity a) {
        try {
            PackageInfo p = a.getPackageManager().getPackageInfo(a.getPackageName(), 0);
            return p.versionCode;
        } catch (Exception e) {
            return 0;
        }
    }

    public static String currentName(Activity a) {
        try {
            return a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }

    /** Checks once per app start; shows the update dialog only when a newer version exists. */
    public static void checkOnStart(Activity a) {
        if (checkedThisRun) return;
        checkedThisRun = true;
        check(a, false);
    }

    /** manual: also tells the user when the app is already up to date or the check failed. */
    public static void check(final Activity a, final boolean manual) {
        if (manual) Toast.makeText(a, "Verificando atualizações…", Toast.LENGTH_SHORT).show();
        Task.run(a, new Callable<JSONObject>() {
            public JSONObject call() throws Exception {
                return new JSONObject(new String(fetch(LATEST_URL + "?t=" + System.currentTimeMillis()), "UTF-8"));
            }
        }, new Task.Done<JSONObject>() {
            public void ok(JSONObject latest) {
                int code = latest.optInt("versionCode", 0);
                if (code > currentCode(a)) offer(a, latest);
                else if (manual) {
                    Toast.makeText(a, "Você já tem a versão mais recente (" + currentName(a) + ")",
                            Toast.LENGTH_LONG).show();
                }
            }

            public void fail(Exception e) {
                if (manual) Toast.makeText(a, "Não foi possível verificar: " + Task.message(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void offer(final Activity a, final JSONObject latest) {
        String notes = latest.optString("notes", "");
        new AlertDialog.Builder(a)
                .setTitle("Nova versão disponível")
                .setMessage("Xplay " + latest.optString("versionName") + " está disponível."
                        + (notes.isEmpty() ? "" : "\n\n" + notes)
                        + "\n\nVocê está usando a versão " + currentName(a) + ".")
                .setPositiveButton("Atualizar agora", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { download(a, latest.optString("apk")); }
                })
                .setNegativeButton("Depois", null)
                .show();
    }

    private static void download(final Activity a, final String apkUrl) {
        LinearLayout box = Ui.column(a);
        int p = Ui.dp(a, 24);
        box.setPadding(p, Ui.dp(a, 12), p, 0);
        final TextView label = Ui.text(a, "Baixando… 0%", 15, Palette.TEXT, false);
        box.addView(label);
        final ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressTintList(ColorStateList.valueOf(Palette.GOLD));
        LinearLayout.LayoutParams bl = Ui.lp(-1, Ui.dp(a, 8));
        bl.topMargin = Ui.dp(a, 12);
        box.addView(bar, bl);
        final AlertDialog dialog = new AlertDialog.Builder(a)
                .setTitle("Atualizando o Xplay")
                .setView(box)
                .setCancelable(false)
                .show();

        final File target = UpdateProvider.apkFile(a);
        Task.run(a, new Callable<File>() {
            public File call() throws Exception {
                HttpURLConnection conn = open(apkUrl);
                int total = conn.getContentLength();
                InputStream in = conn.getInputStream();
                target.getParentFile().mkdirs();
                FileOutputStream out = new FileOutputStream(target);
                byte[] buf = new byte[65536];
                long done = 0;
                int n, last = -1;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    done += n;
                    final int pct = total > 0 ? (int) (done * 100 / total) : -1;
                    if (pct != last) {
                        last = pct;
                        bar.post(new Runnable() {
                            public void run() {
                                if (pct >= 0) {
                                    bar.setProgress(pct);
                                    label.setText("Baixando… " + pct + "%");
                                }
                            }
                        });
                    }
                }
                out.close();
                in.close();
                conn.disconnect();
                target.setReadable(true, false);
                return target;
            }
        }, new Task.Done<File>() {
            public void ok(File apk) {
                dialog.dismiss();
                install(a, apk);
            }

            public void fail(Exception e) {
                dialog.dismiss();
                Toast.makeText(a, "Falha no download: " + Task.message(e), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static void install(Activity a, File apk) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        Uri uri = Build.VERSION.SDK_INT >= 24 ? UpdateProvider.uri(a) : Uri.fromFile(apk);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            a.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(a, "Não foi possível abrir o instalador", Toast.LENGTH_LONG).show();
        }
    }

    private static HttpURLConnection open(String address) throws java.io.IOException {
        URL url = new URL(address);
        for (int i = 0; i < 5; i++) {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent", "Xplay-Updater");
            int code = conn.getResponseCode();
            if (code >= 300 && code < 400 && conn.getHeaderField("Location") != null) {
                url = new URL(url, conn.getHeaderField("Location"));
                conn.disconnect();
                continue;
            }
            if (code != HttpURLConnection.HTTP_OK) {
                conn.disconnect();
                throw new java.io.IOException("Servidor respondeu HTTP " + code);
            }
            return conn;
        }
        throw new java.io.IOException("Redirecionamentos demais");
    }

    private static byte[] fetch(String address) throws java.io.IOException {
        HttpURLConnection conn = open(address);
        InputStream in = conn.getInputStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        conn.disconnect();
        return out.toByteArray();
    }
}
