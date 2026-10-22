package com.xplay.pdfreader;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/** Read-only PDF viewer: open a file, scroll through its pages, pinch or double-tap to zoom. */
public class MainActivity extends Activity implements ZoomFrame.Listener {

    static final float MIN_ZOOM = 1f;
    static final float MAX_ZOOM = 4f;
    private static final int REQ_OPEN = 1;
    private static final int BAR = 0xFF202124;
    private static final int ACCENT = 0xFFE53935;

    private SharedPreferences prefs;
    private View topBar;
    private TextView title;
    private TextView indicator;
    private LinearLayout empty;
    private ZoomFrame frame;
    private HorizontalScrollView hscroll;
    private PageList list;
    private final PageAdapter adapter = new PageAdapter();

    private PdfDoc doc;
    private Uri uri;
    private float zoom = 1f;
    private int opening;
    private int pendingScrollX = -1;

    private final Runnable hideIndicator = new Runnable() {
        @Override public void run() {
            indicator.animate().alpha(0f).setDuration(300).withEndAction(new Runnable() {
                @Override public void run() {
                    indicator.setVisibility(View.GONE);
                }
            });
        }
    };

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("reader", MODE_PRIVATE);
        buildUi();

        Uri start = viewUri(getIntent());
        if (start == null) {
            String last = prefs.getString("last", null);
            if (last != null) start = Uri.parse(last);
        }
        if (start != null) open(start, false);
        else showEmpty();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Uri u = viewUri(intent);
        if (u != null) open(u, true);
    }

    @Override protected void onPause() {
        super.onPause();
        savePage();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (doc != null) doc.close();
    }

    @Override public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        // Keep the zoom level; the viewport width changes so lay out again.
        // The frame's layout listener resizes the list; keep the reading position.
        final int pos = currentPage();
        frame.post(new Runnable() {
            @Override public void run() {
                list.setSelection(pos);
            }
        });
    }

    private static Uri viewUri(Intent i) {
        return i != null && Intent.ACTION_VIEW.equals(i.getAction()) ? i.getData() : null;
    }

    // ---- UI ----

    private int dp(float v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics());
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setBackgroundColor(BAR);
        bar.setPadding(dp(16), 0, dp(4), 0);
        topBar = bar;

        title = new TextView(this);
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        title.setText(R.string.app_name);
        bar.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        ImageView openBtn = new ImageView(this);
        openBtn.setImageResource(R.drawable.ic_folder);
        openBtn.setScaleType(ImageView.ScaleType.CENTER);
        openBtn.setContentDescription("Abrir PDF");
        openBtn.setBackgroundResource(selectableBackground());
        openBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pickFile();
            }
        });
        bar.addView(openBtn, new LinearLayout.LayoutParams(dp(48), dp(48)));
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

        FrameLayout body = new FrameLayout(this);
        root.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        list = new PageList(this);
        list.setAdapter(adapter);
        list.setDivider(new ColorDrawable(Color.TRANSPARENT));
        list.setDividerHeight(dp(8));
        list.setSelector(new ColorDrawable(Color.TRANSPARENT));
        list.setPadding(0, dp(8), 0, dp(8));
        list.setClipToPadding(false);
        list.setFastScrollEnabled(true);
        list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override public void onScrollStateChanged(AbsListView v, int state) {}

            @Override public void onScroll(AbsListView v, int first, int visible, int total) {
                if (doc != null && visible > 0) showIndicator();
            }
        });

        list.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override public void onLayoutChange(View v, int l, int t, int r, int b,
                                                 int ol, int ot, int or, int ob) {
                if (pendingScrollX >= 0) {
                    hscroll.scrollTo(pendingScrollX, 0);
                    pendingScrollX = -1;
                }
            }
        });

        hscroll = new HorizontalScrollView(this);
        hscroll.setHorizontalScrollBarEnabled(true);
        hscroll.addView(list, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        frame = new ZoomFrame(this);
        frame.setContent(hscroll, this);
        frame.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override public void onLayoutChange(View v, int l, int t, int r, int b,
                                                 int ol, int ot, int or, int ob) {
                if (r - l != or - ol) list.setFixedWidth(Math.round((r - l) * zoom));
            }
        });
        body.addView(frame);

        indicator = new TextView(this);
        indicator.setTextColor(Color.WHITE);
        indicator.setTextSize(14);
        indicator.setPadding(dp(14), dp(6), dp(14), dp(6));
        GradientDrawable pill = new GradientDrawable();
        pill.setColor(0xCC000000);
        pill.setCornerRadius(dp(16));
        indicator.setBackground(pill);
        indicator.setVisibility(View.GONE);
        indicator.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                askPage();
            }
        });
        FrameLayout.LayoutParams ip = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        ip.bottomMargin = dp(20);
        body.addView(indicator, ip);

        empty = new LinearLayout(this);
        empty.setOrientation(LinearLayout.VERTICAL);
        empty.setGravity(Gravity.CENTER);
        empty.setBackgroundColor(0xFF525659);
        TextView msg = new TextView(this);
        msg.setText("Nenhum PDF aberto");
        msg.setTextColor(0xFFE0E0E0);
        msg.setTextSize(18);
        msg.setGravity(Gravity.CENTER);
        empty.addView(msg);
        TextView btn = new TextView(this);
        btn.setText("Abrir PDF");
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(16);
        btn.setTypeface(Typeface.DEFAULT_BOLD);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(dp(28), dp(12), dp(28), dp(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(ACCENT);
        bg.setCornerRadius(dp(24));
        btn.setBackground(bg);
        btn.setFocusable(true);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                pickFile();
            }
        });
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = dp(20);
        empty.addView(btn, bp);
        body.addView(empty);

        setContentView(root);
    }

    private int selectableBackground() {
        TypedValue v = new TypedValue();
        getTheme().resolveAttribute(android.R.attr.selectableItemBackgroundBorderless, v, true);
        return v.resourceId;
    }

    private void showEmpty() {
        title.setText(R.string.app_name);
        empty.setVisibility(View.VISIBLE);
        topBar.setVisibility(View.VISIBLE);
        indicator.removeCallbacks(hideIndicator);
        indicator.setVisibility(View.GONE);
    }

    private void showIndicator() {
        indicator.setText((currentPage() + 1) + " / " + doc.pageCount());
        indicator.animate().cancel();
        indicator.setAlpha(1f);
        indicator.setVisibility(View.VISIBLE);
        indicator.removeCallbacks(hideIndicator);
        indicator.postDelayed(hideIndicator, 1500);
    }

    /** The page covering the middle of the screen. */
    private int currentPage() {
        int mid = list.getHeight() / 2;
        for (int i = 0; i < list.getChildCount(); i++) {
            View c = list.getChildAt(i);
            if (c.getBottom() >= mid) return list.getFirstVisiblePosition() + i;
        }
        return list.getFirstVisiblePosition();
    }

    private void askPage() {
        if (doc == null) return;
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("1 – " + doc.pageCount());
        new AlertDialog.Builder(this)
                .setTitle("Ir para a página")
                .setView(input)
                .setPositiveButton("Ir", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        try {
                            int p = Integer.parseInt(input.getText().toString().trim()) - 1;
                            list.setSelection(Math.max(0, Math.min(doc.pageCount() - 1, p)));
                        } catch (NumberFormatException ignored) {
                        }
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // ---- Opening files ----

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("application/pdf");
        try {
            startActivityForResult(i, REQ_OPEN);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Nenhum gerenciador de arquivos encontrado", Toast.LENGTH_LONG).show();
        }
    }

    @Override protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req != REQ_OPEN || result != RESULT_OK || data == null || data.getData() == null) return;
        Uri u = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException ignored) {
            // Not persistable: it still opens now, just not again after a restart.
        }
        open(u, true);
    }

    private void open(final Uri u, final boolean userAction) {
        savePage();
        final int token = ++opening;
        title.setText("Abrindo…");
        new Thread(new Runnable() {
            @Override public void run() {
                PdfDoc d = null;
                String error = null;
                try {
                    d = PdfDoc.open(getApplicationContext(), u);
                } catch (SecurityException e) {
                    error = isLocked(e) ? "Este PDF é protegido por senha e não pode ser aberto"
                            : "Sem permissão para ler este arquivo";
                } catch (Exception e) {
                    error = "Não foi possível abrir o PDF";
                }
                final PdfDoc result = d;
                final String err = error;
                runOnUiThread(new Runnable() {
                    @Override public void run() {
                        if (token != opening || isDestroyed()) {
                            if (result != null) result.close();
                            return;
                        }
                        if (result == null) {
                            if (userAction) Toast.makeText(MainActivity.this, err, Toast.LENGTH_LONG).show();
                            else prefs.edit().remove("last").apply();
                            if (doc == null) showEmpty();
                            else title.setText(displayName(uri));
                            return;
                        }
                        show(u, result);
                    }
                });
            }
        }).start();
    }

    private static boolean isLocked(SecurityException e) {
        String m = e.getMessage();
        return m != null && m.toLowerCase().contains("password");
    }

    private void show(Uri u, PdfDoc d) {
        if (doc != null) doc.close();
        doc = d;
        uri = u;
        prefs.edit().putString("last", u.toString()).apply();
        empty.setVisibility(View.GONE);
        title.setText(displayName(u));
        zoom = 1f;
        list.setFixedWidth(frame.getWidth());
        hscroll.scrollTo(0, 0);
        adapter.notifyDataSetChanged();
        int page = prefs.getInt("page:" + u, 0);
        list.setSelection(Math.min(page, d.pageCount() - 1));
        list.post(new Runnable() {
            @Override public void run() {
                if (doc != null) showIndicator();
            }
        });
    }

    private void savePage() {
        if (doc != null && uri != null && list.getChildCount() > 0) {
            prefs.edit().putInt("page:" + uri, currentPage()).apply();
        }
    }

    private String displayName(Uri u) {
        String name = null;
        try {
            Cursor c = getContentResolver().query(u, new String[] {OpenableColumns.DISPLAY_NAME},
                    null, null, null);
            if (c != null) {
                try {
                    if (c.moveToFirst()) name = c.getString(0);
                } finally {
                    c.close();
                }
            }
        } catch (RuntimeException ignored) {
        }
        if (name == null) name = u.getLastPathSegment();
        return name != null ? name : getString(R.string.app_name);
    }

    // ---- Zoom ----

    @Override public float zoom() {
        return zoom;
    }

    @Override public void onDoubleTap(float x, float y) {
        if (doc == null) return;
        onZoom((zoom > 1.05f ? 1f : 2.5f) / zoom, x, y);
    }

    @Override public void onSingleTap() {
        if (doc == null) return;
        topBar.setVisibility(topBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    @Override public void onZoom(float factor, float fx, float fy) {
        if (doc == null || list.getChildCount() == 0) return;
        float z1 = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, zoom * factor));
        if (Math.abs(z1 - zoom) < 0.01f) return;
        final float applied = z1 / zoom;
        zoom = z1;

        // Keep the point under the fingers in place: find the page and spot within it.
        int idx = list.getChildCount() - 1;
        for (int i = 0; i < list.getChildCount(); i++) {
            if (list.getChildAt(i).getBottom() >= fy) {
                idx = i;
                break;
            }
        }
        View child = list.getChildAt(idx);
        int pos = list.getFirstVisiblePosition() + idx;
        float rel = child.getHeight() > 0 ? (fy - child.getTop()) / child.getHeight() : 0f;
        pendingScrollX = Math.max(0, Math.round((hscroll.getScrollX() + fx) * applied - fx));

        int width = Math.round(frame.getWidth() * zoom);
        list.setFixedWidth(width);
        int newH = Math.round(width * doc.ratio(pos));
        list.setSelectionFromTop(pos, Math.round(fy - rel * newH));
    }

    // ---- List ----

    /** ListView with a set width, since HorizontalScrollView measures children unbounded. */
    private static final class PageList extends ListView {
        private int fixedWidth;

        PageList(Context c) {
            super(c);
        }

        void setFixedWidth(int w) {
            if (w <= 0 || w == fixedWidth) return;
            fixedWidth = w;
            requestLayout();
        }

        @Override protected void onMeasure(int widthSpec, int heightSpec) {
            int w = fixedWidth > 0 ? fixedWidth : MeasureSpec.getSize(widthSpec);
            super.onMeasure(MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY), heightSpec);
        }
    }

    private final class PageAdapter extends BaseAdapter {
        @Override public int getCount() {
            return doc == null ? 0 : doc.pageCount();
        }

        @Override public Object getItem(int position) {
            return position;
        }

        @Override public long getItemId(int position) {
            return position;
        }

        @Override public View getView(int position, View convert, ViewGroup parent) {
            PageView v = convert instanceof PageView ? (PageView) convert : new PageView(MainActivity.this);
            v.bind(doc, position);
            return v;
        }
    }
}
