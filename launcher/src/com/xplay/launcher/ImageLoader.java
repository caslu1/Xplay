package com.xplay.launcher;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Loads logos, backgrounds, ads and icons: memory cache, then a disk cache that survives restarts, then the network.
 * Newest requests run first so what is on screen loads before rows scrolled past.
 */
public final class ImageLoader {

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(
            (int) Math.min(Runtime.getRuntime().maxMemory() / 5, 64L * 1024 * 1024)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(6, 6, 30, TimeUnit.SECONDS,
            new LinkedBlockingDeque<Runnable>() {
                @Override
                public boolean offer(Runnable r) {
                    return offerFirst(r);
                }
            });
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final int MAX_DISK_FILES = 400;
    private static File diskDir;

    private ImageLoader() {}

    public static void init(Context c) {
        if (diskDir != null) return;
        diskDir = new File(c.getCacheDir(), "img");
        diskDir.mkdirs();
        POOL.execute(new Runnable() {
            public void run() { trimDisk(); }
        });
    }

    /** Loads url into view, decoded to roughly targetWidthPx. placeholderRes 0 leaves the view empty. */
    public static void load(String url, ImageView view, int targetWidthPx, int placeholderRes) {
        load(url, view, targetWidthPx, placeholderRes, false);
    }

    /** Loads a tiny copy of the image and lets the view scale it up, which reads as a blur. */
    public static void loadBlurred(String url, ImageView view) {
        load(url, view, 24, 0, true);
    }

    private static void load(final String url, final ImageView view, final int targetWidthPx, int placeholderRes,
                             final boolean blur) {
        view.setTag(R.id.image_url, url);
        if (placeholderRes != 0) view.setImageResource(placeholderRes);
        else view.setImageDrawable(null);
        if (url == null || url.isEmpty()) return;
        final String key = url + "#" + targetWidthPx + (blur ? "b" : "");
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        POOL.execute(new Runnable() {
            public void run() {
                // Skip work for views that were already recycled for another image.
                if (!url.equals(view.getTag(R.id.image_url))) return;
                Bitmap bmp = decode(bytes(url), targetWidthPx);
                if (bmp == null) return;
                if (blur) bmp = Bitmap.createScaledBitmap(bmp, 48, Math.max(1, 48 * bmp.getHeight() / bmp.getWidth()), true);
                final Bitmap result = bmp;
                CACHE.put(key, result);
                MAIN.post(new Runnable() {
                    public void run() {
                        if (url.equals(view.getTag(R.id.image_url))) view.setImageBitmap(result);
                    }
                });
            }
        });
    }

    private static byte[] bytes(String url) {
        File file = diskDir == null ? null : new File(diskDir, Integer.toHexString(url.hashCode()) + "_" + url.length());
        if (file != null && file.exists()) {
            try {
                byte[] data = readAll(new FileInputStream(file));
                file.setLastModified(System.currentTimeMillis());
                return data;
            } catch (Exception ignored) {
                // Fall back to the network.
            }
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "XplayLauncher/1.0");
            byte[] data = readAll(conn.getInputStream());
            if (file != null && data.length > 0) {
                FileOutputStream out = new FileOutputStream(file);
                out.write(data);
                out.close();
            }
            return data;
        } catch (Exception e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private static byte[] readAll(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    private static Bitmap decode(byte[] data, int targetWidth) {
        if (data == null) return null;
        try {
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (bounds.outWidth / (opts.inSampleSize * 2) >= targetWidth) opts.inSampleSize *= 2;
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888; // logos and icons keep transparency
            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private static void trimDisk() {
        File[] files = diskDir.listFiles();
        if (files == null || files.length <= MAX_DISK_FILES) return;
        Arrays.sort(files, new Comparator<File>() {
            public int compare(File a, File b) { return Long.compare(a.lastModified(), b.lastModified()); }
        });
        for (int i = 0; i < files.length - MAX_DISK_FILES / 2; i++) files[i].delete();
    }
}
