package com.xplay.iptv;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.util.LruCache;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Background loader for logos and posters with an in-memory cache, shared by all screens. */
public final class ImageLoader {

    private static final LruCache<String, Bitmap> CACHE = new LruCache<String, Bitmap>(
            (int) Math.min(Runtime.getRuntime().maxMemory() / 6, 48L * 1024 * 1024)) {
        @Override
        protected int sizeOf(String key, Bitmap value) {
            return value.getByteCount();
        }
    };
    private static final ExecutorService POOL = Executors.newFixedThreadPool(4);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private ImageLoader() {}

    /**
     * Loads url into view, decoded to roughly targetWidthPx. placeholderRes 0 leaves the view empty
     * until the image arrives.
     */
    public static void load(final String url, final ImageView view, final int targetWidthPx, int placeholderRes) {
        view.setTag(R.id.image_url, url);
        if (placeholderRes != 0) view.setImageResource(placeholderRes);
        else view.setImageDrawable(null);
        if (url == null || url.isEmpty()) return;
        final String key = url + "#" + targetWidthPx;
        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            view.setImageBitmap(cached);
            return;
        }
        POOL.execute(new Runnable() {
            public void run() {
                final Bitmap bmp = fetch(url, targetWidthPx);
                if (bmp == null) return;
                CACHE.put(key, bmp);
                MAIN.post(new Runnable() {
                    public void run() {
                        if (url.equals(view.getTag(R.id.image_url))) view.setImageBitmap(bmp);
                    }
                });
            }
        });
    }

    private static Bitmap fetch(String url, int targetWidth) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("User-Agent", "Xplay/2.0");
            InputStream in = conn.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            byte[] data = out.toByteArray();

            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 1;
            while (bounds.outWidth / (opts.inSampleSize * 2) >= targetWidth) opts.inSampleSize *= 2;
            return BitmapFactory.decodeByteArray(data, 0, data.length, opts);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
