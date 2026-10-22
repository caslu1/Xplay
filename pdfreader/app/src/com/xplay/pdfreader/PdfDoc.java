package com.xplay.pdfreader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.LruCache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An open PDF. Pages are rendered on a single background thread (PdfRenderer
 * allows only one open page at a time) and kept in a memory-bounded cache.
 */
final class PdfDoc {

    private final ParcelFileDescriptor fd;
    private final PdfRenderer renderer;
    private final float[] ratios;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final LruCache<Integer, Bitmap> cache;
    private volatile boolean closed;

    /** Opens the document; must be called off the main thread. */
    static PdfDoc open(Context c, Uri uri) throws IOException {
        ParcelFileDescriptor fd = null;
        try {
            fd = c.getContentResolver().openFileDescriptor(uri, "r");
            if (fd == null) throw new IOException("Arquivo não encontrado");
            return new PdfDoc(fd);
        } catch (IOException e) {
            // Some providers hand out pipes, which PdfRenderer cannot seek: copy to cache.
            closeQuietly(fd);
            return new PdfDoc(copyToCache(c, uri));
        } catch (RuntimeException e) {
            closeQuietly(fd);
            throw e;
        }
    }

    private static ParcelFileDescriptor copyToCache(Context c, Uri uri) throws IOException {
        File f = new File(c.getCacheDir(), "current.pdf");
        InputStream in = c.getContentResolver().openInputStream(uri);
        if (in == null) throw new IOException("Arquivo não encontrado");
        try {
            OutputStream out = new FileOutputStream(f);
            try {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private PdfDoc(ParcelFileDescriptor fd) throws IOException {
        this.fd = fd;
        try {
            renderer = new PdfRenderer(fd);
        } catch (IOException | RuntimeException e) {
            closeQuietly(fd);
            throw e;
        }
        int n = renderer.getPageCount();
        ratios = new float[n];
        for (int i = 0; i < n; i++) {
            PdfRenderer.Page p = renderer.openPage(i);
            ratios[i] = p.getWidth() > 0 ? (float) p.getHeight() / p.getWidth() : 1.414f;
            p.close();
        }
        int max = (int) Math.min(Runtime.getRuntime().maxMemory() / 3, Integer.MAX_VALUE);
        cache = new LruCache<Integer, Bitmap>(max) {
            @Override protected int sizeOf(Integer key, Bitmap b) {
                return b.getByteCount();
            }
        };
    }

    int pageCount() {
        return ratios.length;
    }

    /** Page height divided by page width. */
    float ratio(int page) {
        return ratios[page];
    }

    Bitmap cached(int page) {
        return cache.get(page);
    }

    /** Renders {@code page} at {@code width} pixels and hands it to {@code v} if it still shows that page. */
    void render(final int page, final int width, final PageView v) {
        if (closed) return;
        exec.execute(new Runnable() {
            @Override public void run() {
                if (closed || v.page() != page) return;
                Bitmap have = cache.get(page);
                final Bitmap b = have != null && have.getWidth() >= width ? have : draw(page, width);
                if (b == null) return;
                main.post(new Runnable() {
                    @Override public void run() {
                        if (!closed && v.page() == page) v.setBitmap(b);
                    }
                });
            }
        });
    }

    private Bitmap draw(int page, int width) {
        for (int w = width; w >= 256; w /= 2) {
            try {
                Bitmap b = Bitmap.createBitmap(w, Math.max(1, Math.round(w * ratios[page])),
                        Bitmap.Config.ARGB_8888);
                b.eraseColor(Color.WHITE);
                PdfRenderer.Page p = renderer.openPage(page);
                try {
                    p.render(b, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                } finally {
                    p.close();
                }
                cache.put(page, b);
                return b;
            } catch (OutOfMemoryError e) {
                cache.evictAll();
            }
        }
        return null;
    }

    void close() {
        closed = true;
        exec.execute(new Runnable() {
            @Override public void run() {
                cache.evictAll();
                renderer.close();
                closeQuietly(fd);
            }
        });
        exec.shutdown();
    }

    private static void closeQuietly(ParcelFileDescriptor fd) {
        if (fd == null) return;
        try {
            fd.close();
        } catch (IOException ignored) {
        }
    }
}
