package com.xplay.iptv;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

/** Grid with every title of a category or row. */
public class CategoryActivity extends Activity {

    private static String pendingTitle;
    private static List<VodItem> pendingItems;

    private List<VodItem> items;
    private BaseAdapter adapter;

    public static void start(Activity from, String title, List<VodItem> items) {
        pendingTitle = title;
        pendingItems = items;
        from.startActivity(new Intent(from, CategoryActivity.class));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Repo.init(this);
        items = pendingItems;
        if (items == null) {
            finish();
            return;
        }
        LinearLayout root = Ui.column(this);
        root.setBackgroundColor(Palette.BLACK);

        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 8), Ui.dp(this, 12), Ui.dp(this, 16), Ui.dp(this, 4));
        ImageView back = Ui.iconButton(this, R.drawable.ic_back, Palette.TEXT, Palette.GOLD);
        back.setContentDescription("Voltar");
        top.addView(back, Ui.lp(Ui.dp(this, 48), Ui.dp(this, 48)));
        back.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { finish(); }
        });
        TextView title = Ui.text(this, pendingTitle, 22, Palette.TEXT, true);
        title.setSingleLine(true);
        title.setEllipsize(TextUtils.TruncateAt.END);
        title.setPadding(Ui.dp(this, 8), 0, 0, 0);
        top.addView(title, Ui.weight(1));
        top.addView(Ui.text(this, items.size() + " títulos", 14, Palette.TEXT_DIM, false));
        root.addView(top, Ui.lp(-1, -2));

        GridView grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(Ui.dp(this, Ui.posterDp(this) + 12));
        grid.setStretchMode(GridView.STRETCH_SPACING_UNIFORM);
        grid.setVerticalSpacing(Ui.dp(this, 12));
        grid.setSelector(new android.graphics.drawable.ColorDrawable(0));
        grid.setPadding(Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 20));
        grid.setClipToPadding(false);
        adapter = new BaseAdapter() {
            public int getCount() { return items.size(); }
            public Object getItem(int i) { return items.get(i); }
            public long getItemId(int i) { return i; }
            public View getView(int i, View convert, ViewGroup parent) {
                return Posters.card(CategoryActivity.this, items.get(i), Ui.posterDp(CategoryActivity.this));
            }
        };
        grid.setAdapter(adapter);
        root.addView(grid, new LinearLayout.LayoutParams(-1, 0, 1f));
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) adapter.notifyDataSetChanged();
    }
}
