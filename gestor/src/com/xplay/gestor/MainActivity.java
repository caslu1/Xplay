package com.xplay.gestor;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.OutputStream;
import java.nio.charset.Charset;

/** Hosts the manager UI (assets/index.html); data lives in the WebView's localStorage. */
public class MainActivity extends Activity {
    private static final int PICK_FILE = 1;
    private static final int SAVE_FILE = 2;

    private WebView web;
    private ValueCallback<Uri[]> pendingChooser;
    private String pendingSave;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        web = new WebView(this);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        // Pages load from file:// and call the Supabase API.
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setTextZoom(100);
        web.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        web.addJavascriptInterface(new Bridge(), "Android");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("file:")) return false;
                openExternal(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                checkClipboard();
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (pendingChooser != null) pendingChooser.onReceiveValue(null);
                pendingChooser = callback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Selecionar arquivo"), PICK_FILE);
                } catch (Exception e) {
                    pendingChooser = null;
                    return false;
                }
                return true;
            }
        });
        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");
        setContentView(web);
    }

    private String lastClip;

    /** Android only lets the focused app read the clipboard, so check whenever the window gets focus. */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) checkClipboard();
    }

    private void checkClipboard() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm == null || !cm.hasPrimaryClip() || cm.getPrimaryClip().getItemCount() == 0) return;
            CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(this);
            if (t == null) return;
            String text = t.toString().trim();
            if (text.length() == 0 || text.length() > 40 || text.equals(lastClip)) return;
            lastClip = text;
            String js = "window.onClipboard && window.onClipboard(" + org.json.JSONObject.quote(text) + ")";
            web.evaluateJavascript(js, null);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onPause() {
        // The page writes to localStorage shortly after each change; make sure nothing is pending.
        web.evaluateJavascript("window.flushSave && flushSave()", null);
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (request == PICK_FILE && pendingChooser != null) {
            Uri uri = result == RESULT_OK && data != null ? data.getData() : null;
            pendingChooser.onReceiveValue(uri == null ? null : new Uri[]{uri});
            pendingChooser = null;
        } else if (request == SAVE_FILE) {
            String text = pendingSave;
            pendingSave = null;
            Uri uri = result == RESULT_OK && data != null ? data.getData() : null;
            if (uri == null || text == null) return;
            try {
                // "wt" truncates: without it, saving over a bigger file leaves its old tail behind.
                OutputStream out = getContentResolver().openOutputStream(uri, "wt");
                out.write(text.getBytes(Charset.forName("UTF-8")));
                out.close();
                Toast.makeText(this, "Arquivo salvo", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Não foi possível salvar: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        // The page closes its open sheet/menu first, or switches back to the dashboard.
        web.evaluateJavascript("window.onBack ? window.onBack() : false", new ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!"true".equals(value)) finish();
            }
        });
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Nenhum app para abrir este link", Toast.LENGTH_SHORT).show();
        }
    }

    private class Bridge {
        @JavascriptInterface
        public void open(final String url) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    openExternal(url);
                }
            });
        }

        @JavascriptInterface
        public void share(final String title, final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_SUBJECT, title);
                    i.putExtra(Intent.EXTRA_TEXT, text);
                    startActivity(Intent.createChooser(i, title));
                }
            });
        }

        /** Prints the current page; the system dialog offers "Salvar como PDF". */
        @JavascriptInterface
        public void print(final String name) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    PrintManager pm = (PrintManager) getSystemService(Context.PRINT_SERVICE);
                    PrintDocumentAdapter adapter = web.createPrintDocumentAdapter(name);
                    pm.print(name, adapter, new PrintAttributes.Builder()
                            .setMediaSize(PrintAttributes.MediaSize.ISO_A4).build());
                }
            });
        }

        /** Lets the user pick where to save (Downloads, Drive...) and writes the text there. */
        @JavascriptInterface
        public void saveFile(final String name, final String mime, final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    pendingSave = text;
                    Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                    i.addCategory(Intent.CATEGORY_OPENABLE);
                    i.setType(mime);
                    i.putExtra(Intent.EXTRA_TITLE, name);
                    try {
                        startActivityForResult(i, SAVE_FILE);
                    } catch (Exception e) {
                        pendingSave = null;
                        Toast.makeText(MainActivity.this, "Não foi possível abrir o seletor de arquivos", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }

        /** Inflates a raw-deflate ZIP entry (base64) for WebViews without DecompressionStream. */
        @JavascriptInterface
        public String inflate(String base64) {
            try {
                byte[] in = android.util.Base64.decode(base64, android.util.Base64.DEFAULT);
                java.util.zip.Inflater inf = new java.util.zip.Inflater(true);
                inf.setInput(in);
                java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream(in.length * 4);
                byte[] buf = new byte[65536];
                while (!inf.finished()) {
                    int n = inf.inflate(buf);
                    if (n == 0 && (inf.needsInput() || inf.needsDictionary())) break;
                    out.write(buf, 0, n);
                }
                inf.end();
                return new String(out.toByteArray(), Charset.forName("UTF-8"));
            } catch (Exception e) {
                return null;
            }
        }

        @JavascriptInterface
        public void copy(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setPrimaryClip(ClipData.newPlainText("codigo", text));
                }
            });
        }

        @JavascriptInterface
        public void setTheme(final boolean dark) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Window w = getWindow();
                    w.setStatusBarColor(dark ? 0xFF15102A : 0xFF140F14);
                    w.setNavigationBarColor(dark ? 0xFF1A1B2E : 0xFFFFFFFF);
                    web.setBackgroundColor(dark ? 0xFF0F1020 : 0xFFF4F6FB);
                }
            });
        }

        @JavascriptInterface
        public void toast(final String msg) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
