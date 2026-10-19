package com.xplay.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;

/** Downloads APKs (store apps and launcher updates) and hands them to the system installer. */
public final class Installer {

    private static boolean checkedThisRun;

    private Installer() {}

    /** Downloads apkUrl with a progress dialog, then opens the installer. */
    public static void downloadAndInstall(final Activity a, final String apkUrl, final String name, String title) {
        if (apkUrl == null || apkUrl.trim().isEmpty()) {
            Toast.makeText(a, "Este app não tem link de download", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!canInstall(a)) return;

        LinearLayout box = Ui.column(a);
        int p = Ui.dp(a, 24);
        box.setPadding(p, Ui.dp(a, 12), p, 0);
        final TextView label = Ui.text(a, "Baixando… 0%", 15, 0xFFE5E7EB, false);
        box.addView(label);
        final ProgressBar bar = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressTintList(ColorStateList.valueOf(Ui.ACCENT));
        LinearLayout.LayoutParams bl = Ui.lp(-1, Ui.dp(a, 8));
        bl.topMargin = Ui.dp(a, 12);
        box.addView(bar, bl);
        final AlertDialog dialog = new AlertDialog.Builder(a, R.style.DialogTheme)
                .setTitle(title)
                .setView(box)
                .setCancelable(false)
                .show();

        final File target = ApkProvider.file(a, name);
        Task.run(a, new Callable<File>() {
            public File call() throws Exception {
                HttpURLConnection conn = Config.Net.open(apkUrl.trim(), null);
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
                    final long mb = done / (1024 * 1024);
                    if (pct != last) {
                        last = pct;
                        bar.post(new Runnable() {
                            public void run() {
                                if (pct >= 0) {
                                    bar.setProgress(pct);
                                    label.setText("Baixando… " + pct + "%");
                                } else {
                                    label.setText("Baixando… " + mb + " MB");
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

    /** Android 8+ asks once per app for permission to install APKs; send the user there first. */
    private static boolean canInstall(final Activity a) {
        if (Build.VERSION.SDK_INT < 26) return true;
        try {
            // PackageManager.canRequestPackageInstalls() is API 26; the build compiles against API 23.
            Object ok = android.content.pm.PackageManager.class.getMethod("canRequestPackageInstalls")
                    .invoke(a.getPackageManager());
            if (Boolean.TRUE.equals(ok)) return true;
        } catch (Exception e) {
            return true;
        }
        new AlertDialog.Builder(a, R.style.DialogTheme)
                .setTitle("Permitir instalação")
                .setMessage("Para instalar apps pela loja, permita que o " + Config.get(a).name()
                        + " instale apps desconhecidos. Depois volte e tente de novo.")
                .setPositiveButton("Abrir configuração", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        try {
                            a.startActivity(new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES",
                                    Uri.parse("package:" + a.getPackageName())));
                        } catch (Exception e) {
                            Actions.system(a, Settings.ACTION_SECURITY_SETTINGS);
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
        return false;
    }

    private static void install(Activity a, File apk) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        Uri uri = Build.VERSION.SDK_INT >= 24 ? ApkProvider.uri(apk) : Uri.fromFile(apk);
        i.setDataAndType(uri, "application/vnd.android.package-archive");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            a.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(a, "Não foi possível abrir o instalador", Toast.LENGTH_LONG).show();
        }
    }

    // --- Launcher self-update (releases/launcher/latest.json) -----------------------------------

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

    public static void checkOnStart(Activity a) {
        if (checkedThisRun) return;
        checkedThisRun = true;
        checkUpdate(a, false);
    }

    public static void checkUpdate(final Activity a, final boolean manual) {
        final String url = Config.get(a).settings().optString("launcherUpdateUrl", "");
        if (url.isEmpty()) {
            if (manual) Toast.makeText(a, "Atualização do launcher não configurada no painel", Toast.LENGTH_LONG).show();
            return;
        }
        if (manual) Toast.makeText(a, "Verificando atualizações…", Toast.LENGTH_SHORT).show();
        Task.run(a, new Callable<JSONObject>() {
            public JSONObject call() throws Exception {
                return new JSONObject(new String(Config.Net.get(url + "?t=" + System.currentTimeMillis(), null), "UTF-8"));
            }
        }, new Task.Done<JSONObject>() {
            public void ok(final JSONObject latest) {
                if (latest.optInt("versionCode", 0) <= currentCode(a)) {
                    if (manual) Toast.makeText(a, "O launcher já está na versão mais recente (" + currentName(a) + ")",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                String notes = latest.optString("notes", "");
                new AlertDialog.Builder(a, R.style.DialogTheme)
                        .setTitle("Nova versão do launcher")
                        .setMessage("Versão " + latest.optString("versionName") + " disponível."
                                + (notes.isEmpty() ? "" : "\n\n" + notes)
                                + "\n\nVersão instalada: " + currentName(a) + ".")
                        .setPositiveButton("Atualizar agora", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                downloadAndInstall(a, latest.optString("apk"), "launcher-update", "Atualizando o launcher");
                            }
                        })
                        .setNegativeButton("Depois", null)
                        .show();
            }

            public void fail(Exception e) {
                if (manual) Toast.makeText(a, "Não foi possível verificar: " + Task.message(e), Toast.LENGTH_LONG).show();
            }
        });
    }
}
