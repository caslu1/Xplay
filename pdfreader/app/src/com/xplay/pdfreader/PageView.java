package com.xplay.pdfreader;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;

/** One page of the document, sized to the list width and rendered on demand. */
final class PageView extends View {

    /** Largest bitmap width we render; wider zoom levels are upscaled. */
    static final int MAX_RENDER_WIDTH = 2048;

    private final Paint bitmapPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect dst = new Rect();
    private PdfDoc doc;
    private volatile int page = -1;
    private float ratio = 1.414f;
    private Bitmap bitmap;
    private int requestedWidth;

    PageView(Context c) {
        super(c);
        textPaint.setColor(Color.GRAY);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(18 * c.getResources().getDisplayMetrics().scaledDensity);
    }

    int page() {
        return page;
    }

    void bind(PdfDoc doc, int page) {
        if (this.doc == doc && this.page == page) return;
        this.doc = doc;
        this.page = page;
        ratio = doc.ratio(page);
        bitmap = doc.cached(page);
        requestedWidth = 0;
        requestLayout();
        invalidate();
    }

    void setBitmap(Bitmap b) {
        bitmap = b;
        invalidate();
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, Math.round(w * ratio));
    }

    @Override protected void onDraw(Canvas canvas) {
        canvas.drawColor(Color.WHITE);
        int w = getWidth();
        if (bitmap != null) {
            dst.set(0, 0, w, getHeight());
            canvas.drawBitmap(bitmap, null, dst, bitmapPaint);
        } else {
            canvas.drawText(String.valueOf(page + 1), w / 2f, getHeight() / 2f, textPaint);
        }
        int want = Math.min(w, MAX_RENDER_WIDTH);
        if (doc != null && want > 0 && want > requestedWidth
                && (bitmap == null || bitmap.getWidth() < want * 0.9f)) {
            requestedWidth = want;
            doc.render(page, want, this);
        }
    }
}
