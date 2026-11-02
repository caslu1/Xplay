package com.minhacasa.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.view.HapticFeedbackConstants;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Casca nativa: mostra o app (assets/index.html) e faz as chamadas HTTP ao SmartThings. */
public class MainActivity extends Activity {
    private static final String ALLOWED_HOST = "api.smartthings.com";
    private static final String AI_URL = "https://api.anthropic.com/v1/messages";
    private static final int PICK_PHOTO = 7;

    private ValueCallback<Uri[]> fileCallback;
    private Uri cameraUri;

    private WebView web;
    private final ExecutorService pool = Executors.newFixedThreadPool(6);

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        web = new WebView(this);
        web.setBackgroundColor(Color.TRANSPARENT);
        web.setOverScrollMode(WebView.OVER_SCROLL_NEVER);
        web.setVerticalScrollBarEnabled(false);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setTextZoom(100);
        web.addJavascriptInterface(new Bridge(), "Native");
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url.startsWith("file:")) return false;
                openExternal(url);
                return true;
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                return openPhotoPicker(callback);
            }
        });
        PhotoProvider.cleanup(this);
        setContentView(web);
        if (state != null) web.restoreState(state);
        else web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        web.evaluateJavascript("window.onAppResume && window.onAppResume()", null);
    }

    @Override
    protected void onPause() {
        web.onPause();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        web.evaluateJavascript("window.onBack ? String(window.onBack()) : 'false'", new android.webkit.ValueCallback<String>() {
            @Override
            public void onReceiveValue(String value) {
                if (!"\"true\"".equals(value) && !"true".equals(value)) MainActivity.super.onBackPressed();
            }
        });
    }

    @Override
    protected void onDestroy() {
        pool.shutdownNow();
        web.destroy();
        super.onDestroy();
    }

    /** Escolher foto: galeria ou câmera (a câmera grava pelo PhotoProvider, sem precisar de permissão). */
    private boolean openPhotoPicker(ValueCallback<Uri[]> callback) {
        if (fileCallback != null) fileCallback.onReceiveValue(null);
        fileCallback = callback;
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
        pick.addCategory(Intent.CATEGORY_OPENABLE);
        pick.setType("image/*");
        pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        cameraUri = PhotoProvider.newPhotoUri();
        Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cam.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
        cam.setClipData(ClipData.newRawUri("", cameraUri));
        cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(pick, "Adicionar foto do cômodo");
        if (cam.resolveActivity(getPackageManager()) != null) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cam});
        try {
            startActivityForResult(chooser, PICK_PHOTO);
            return true;
        } catch (Exception e) {
            fileCallback = null;
            callback.onReceiveValue(null);
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != PICK_PHOTO) {
            super.onActivityResult(requestCode, resultCode, data);
            return;
        }
        Uri[] result = null;
        if (resultCode == RESULT_OK) {
            if (data != null && data.getClipData() != null && data.getClipData().getItemCount() > 0) {
                ClipData clip = data.getClipData();
                java.util.ArrayList<Uri> list = new java.util.ArrayList<>();
                for (int i = 0; i < clip.getItemCount(); i++) {
                    Uri u = clip.getItemAt(i).getUri();
                    if (u != null && !u.equals(cameraUri)) list.add(u);
                }
                if (!list.isEmpty()) result = list.toArray(new Uri[0]);
            }
            if (result == null && data != null && data.getData() != null) result = new Uri[]{data.getData()};
            if (result == null && cameraUri != null && PhotoProvider.exists(this, cameraUri)) result = new Uri[]{cameraUri};
        }
        if (fileCallback != null) fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    private void openExternal(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception ignored) {
        }
    }

    private void deliver(final String id, final int status, final String body) {
        final String js = "window.__nativeCb && window.__nativeCb(" + JSONObject.quote(id) + "," + status + "," + JSONObject.quote(body) + ")";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing()) web.evaluateJavascript(js, null);
            }
        });
    }

    private class Bridge {
        @JavascriptInterface
        public void http(final String id, final String method, final String url, final String token, final String body) {
            pool.execute(new Runnable() {
              @Override
              public void run() {
                HttpURLConnection c = null;
                try {
                    URL u = new URL(url);
                    if (!"https".equals(u.getProtocol()) || !ALLOWED_HOST.equals(u.getHost())) {
                        deliver(id, 0, "host not allowed");
                        return;
                    }
                    c = (HttpURLConnection) u.openConnection();
                    c.setConnectTimeout(15000);
                    c.setReadTimeout(20000);
                    c.setRequestMethod(method);
                    c.setRequestProperty("Authorization", "Bearer " + token);
                    c.setRequestProperty("Accept", "application/json");
                    if (body != null && !body.isEmpty()) {
                        c.setDoOutput(true);
                        c.setRequestProperty("Content-Type", "application/json");
                        OutputStream os = c.getOutputStream();
                        os.write(body.getBytes("UTF-8"));
                        os.close();
                    }
                    int code = c.getResponseCode();
                    InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    String resp = "";
                    if (in != null) {
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                        in.close();
                        resp = bo.toString("UTF-8");
                    }
                    deliver(id, code, resp);
                } catch (Exception e) {
                    deliver(id, 0, String.valueOf(e.getMessage()));
                } finally {
                    if (c != null) c.disconnect();
                }
              }
            });
        }

        /** Chamada à API do Claude (análise das fotos dos cômodos). */
        @JavascriptInterface
        public void ai(final String id, final String apiKey, final String body) {
            pool.execute(new Runnable() {
              @Override
              public void run() {
                HttpURLConnection c = null;
                try {
                    c = (HttpURLConnection) new URL(AI_URL).openConnection();
                    c.setConnectTimeout(20000);
                    c.setReadTimeout(240000);
                    c.setRequestMethod("POST");
                    c.setDoOutput(true);
                    c.setRequestProperty("content-type", "application/json");
                    c.setRequestProperty("x-api-key", apiKey);
                    c.setRequestProperty("anthropic-version", "2026-10-21");
                    c.setRequestProperty("anthropic-beta", "server-side-fallback-2026-07-01");
                    OutputStream os = c.getOutputStream();
                    os.write(body.getBytes("UTF-8"));
                    os.close();
                    int code = c.getResponseCode();
                    InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    String resp = "";
                    if (in != null) {
                        ByteArrayOutputStream bo = new ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
                        in.close();
                        resp = bo.toString("UTF-8");
                    }
                    deliver(id, code, resp);
                } catch (Exception e) {
                    deliver(id, 0, String.valueOf(e.getMessage()));
                } finally {
                    if (c != null) c.disconnect();
                }
              }
            });
        }

        @JavascriptInterface
        public void haptic() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    web.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY,
                            HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
                }
            });
        }

        @JavascriptInterface
        public void openUrl(final String url) {
            if (url != null && url.startsWith("https://")) runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    openExternal(url);
                }
            });
        }

        @JavascriptInterface
        public void setBars(final String status, final String nav) {
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                try {
                    getWindow().setStatusBarColor(Color.parseColor(status));
                    getWindow().setNavigationBarColor(Color.parseColor(nav));
                } catch (Exception ignored) {
                }
              }
            });
        }
    }
}
