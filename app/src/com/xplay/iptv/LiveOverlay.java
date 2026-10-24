package com.xplay.iptv;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fullscreen channel info: a bottom panel with number, logo, name and now/next, the channel's
 * qualities as chips (swipe sideways or ◀ ▶ to switch), and a clock. Used by both live players.
 */
public class LiveOverlay extends FrameLayout {

    public interface Listener {
        void onVariant(Channel c);
    }

    private static final Pattern QUALITY = Pattern.compile(
            "(?i)\\b(sd|hd|fhd|full ?hd|uhd|4k|8k|h\\.?265|hevc|1080p?|720p?|alt|backup)\\b");

    private final Activity a;
    private final Listener listener;
    private final SimpleDateFormat clock = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final TextView number, name, group, nowLabel, nowTitle, nowTime, nextTitle, typed, time, hint;
    private final ImageView logo;
    private final ProgressBar progress;
    private final LinearLayout chips;
    private final View chipsBlock;
    private List<Channel> variants = new ArrayList<Channel>();
    private Channel channel;
    /** The list ▲ ▼ zap through, to show which channels come before and after. */
    private List<Channel> zapList;
    private final TextView upNextChannel, downNextChannel, remaining;

    public LiveOverlay(Activity activity, Listener l) {
        super(activity);
        a = activity;
        listener = l;
        setClickable(true);

        // Clock and typed channel number, top right.
        LinearLayout topRight = Ui.column(a);
        topRight.setGravity(Gravity.END);
        time = Ui.text(a, "", 22, Palette.TEXT, true);
        time.setShadowLayer(8, 0, 1, Palette.BLACK);
        topRight.addView(time);
        typed = Ui.text(a, "", 40, Palette.BLACK, true);
        typed.setBackground(Ui.round(Palette.GOLD, Ui.dp(a, 12)));
        typed.setPadding(Ui.dp(a, 18), Ui.dp(a, 2), Ui.dp(a, 18), Ui.dp(a, 2));
        typed.setVisibility(GONE);
        LinearLayout.LayoutParams tl = Ui.lp(-2, -2);
        tl.topMargin = Ui.dp(a, 8);
        topRight.addView(typed, tl);
        FrameLayout.LayoutParams trl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
        trl.setMargins(0, Ui.dp(a, 20), Ui.dp(a, 28), 0);
        addView(topRight, trl);

        // Channels above and below, top left: what ▲ and ▼ will open.
        LinearLayout zap = Ui.column(a);
        zap.setBackground(Ui.round(0x99000000, Ui.dp(a, 12)));
        zap.setPadding(Ui.dp(a, 14), Ui.dp(a, 8), Ui.dp(a, 16), Ui.dp(a, 8));
        upNextChannel = Ui.text(a, "", 13, 0xFFDDDDDD, false);
        upNextChannel.setSingleLine(true);
        upNextChannel.setEllipsize(TextUtils.TruncateAt.END);
        zap.addView(upNextChannel);
        downNextChannel = Ui.text(a, "", 13, 0xFFDDDDDD, false);
        downNextChannel.setSingleLine(true);
        downNextChannel.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams dnl = Ui.lp(-2, -2);
        dnl.topMargin = Ui.dp(a, 4);
        zap.addView(downNextChannel, dnl);
        FrameLayout.LayoutParams zl = new FrameLayout.LayoutParams(Ui.dp(a, 300), -2, Gravity.TOP | Gravity.START);
        zl.setMargins(Ui.dp(a, 28), Ui.dp(a, 20), 0, 0);
        addView(zap, zl);

        LinearLayout bottom = Ui.column(a);
        bottom.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[] {0xF2000000, 0xCC000000, 0x00000000}));
        bottom.setPadding(Ui.dp(a, 28), Ui.dp(a, 56), Ui.dp(a, 28), Ui.dp(a, 22));
        addView(bottom, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));

        // Quality chips.
        LinearLayout chipsLine = Ui.row(a);
        chipsBlock = chipsLine;
        hint = Ui.text(a, "QUALIDADE", 12, Palette.TEXT_DIM, true);
        hint.setPadding(0, 0, Ui.dp(a, 12), 0);
        chipsLine.addView(hint);
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        chips = Ui.row(a);
        hs.addView(chips);
        chipsLine.addView(hs, Ui.weight(1));
        TextView swipe = Ui.text(a, "◀ ▶ qualidade", 12, Palette.TEXT_DIM, false);
        chipsLine.addView(swipe);
        LinearLayout.LayoutParams cl = Ui.lp(-1, -2);
        cl.bottomMargin = Ui.dp(a, 14);
        bottom.addView(chipsLine, cl);

        // Info panel: number | logo | name + programs.
        LinearLayout panel = Ui.row(a);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        bottom.addView(panel, Ui.lp(-1, -2));

        number = Ui.text(a, "", 28, Palette.BLACK, true);
        number.setGravity(Gravity.CENTER);
        number.setIncludeFontPadding(false);
        number.setBackground(Ui.round(Palette.GOLD, Ui.dp(a, 14)));
        panel.addView(number, Ui.lp(Ui.dp(a, 88), Ui.dp(a, 64)));

        FrameLayout logoBox = new FrameLayout(a);
        logoBox.setBackground(Ui.round(0x26FFFFFF, Ui.dp(a, 14)));
        logo = new ImageView(a);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int lp = Ui.dp(a, 10);
        logo.setPadding(lp, lp, lp, lp);
        logoBox.addView(logo, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams ll = Ui.lp(Ui.dp(a, 88), Ui.dp(a, 64));
        ll.leftMargin = Ui.dp(a, 12);
        panel.addView(logoBox, ll);

        LinearLayout info = Ui.column(a);
        info.setPadding(Ui.dp(a, 20), 0, 0, 0);
        LinearLayout nameLine = Ui.row(a);
        nameLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView liveBadge = Ui.text(a, "● AO VIVO", 11, Palette.TEXT, true);
        liveBadge.setBackground(Ui.round(Palette.HOT, Ui.dp(a, 5)));
        liveBadge.setPadding(Ui.dp(a, 7), Ui.dp(a, 2), Ui.dp(a, 7), Ui.dp(a, 2));
        nameLine.addView(liveBadge);
        name = Ui.text(a, "", 20, Palette.TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(Ui.dp(a, 10), 0, 0, 0);
        nameLine.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        group = Ui.text(a, "", 13, Palette.TEXT_DIM, true);
        group.setSingleLine(true);
        group.setPadding(Ui.dp(a, 12), 0, 0, 0);
        nameLine.addView(group);
        info.addView(nameLine, Ui.lp(-1, -2));

        LinearLayout nowLine = Ui.row(a);
        nowLabel = Ui.text(a, "AGORA", 12, Palette.BLACK, true);
        nowLabel.setBackground(Ui.round(Palette.GOLD, Ui.dp(a, 4)));
        nowLabel.setPadding(Ui.dp(a, 6), Ui.dp(a, 1), Ui.dp(a, 6), Ui.dp(a, 1));
        nowLine.addView(nowLabel);
        nowTitle = Ui.text(a, "", 16, Palette.TEXT, false);
        nowTitle.setSingleLine(true);
        nowTitle.setEllipsize(TextUtils.TruncateAt.END);
        nowTitle.setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 10), 0);
        nowLine.addView(nowTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        nowTime = Ui.text(a, "", 14, Palette.GOLD, true);
        nowLine.addView(nowTime);
        LinearLayout.LayoutParams nl = Ui.lp(-1, -2);
        nl.topMargin = Ui.dp(a, 8);
        info.addView(nowLine, nl);

        progress = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgressTintList(ColorStateList.valueOf(Palette.GOLD));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0x55FFFFFF));
        LinearLayout.LayoutParams pl = Ui.lp(-1, Ui.dp(a, 4));
        pl.topMargin = Ui.dp(a, 8);
        info.addView(progress, pl);

        LinearLayout nextLine = Ui.row(a);
        nextTitle = Ui.text(a, "", 14, Palette.TEXT_DIM, false);
        nextTitle.setSingleLine(true);
        nextTitle.setEllipsize(TextUtils.TruncateAt.END);
        nextLine.addView(nextTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        remaining = Ui.text(a, "", 12, Palette.TEXT_DIM, false);
        nextLine.addView(remaining);
        LinearLayout.LayoutParams xl = Ui.lp(-1, -2);
        xl.topMargin = Ui.dp(a, 6);
        info.addView(nextLine, xl);
        panel.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    public void setZapList(List<Channel> list) {
        zapList = list;
    }

    private void bindNeighbours(Channel c) {
        List<Channel> l = zapList;
        int i = l == null ? -1 : l.indexOf(c);
        View box = (View) upNextChannel.getParent();
        if (i < 0 || l.size() < 2) {
            box.setVisibility(GONE);
            return;
        }
        box.setVisibility(VISIBLE);
        int n = l.size();
        Channel up = l.get((i + 1) % n), down = l.get((i - 1 + n) % n);
        upNextChannel.setText("▲  " + Ui.pad3(Adult.number(up)) + "  " + Variants.displayName(up));
        downNextChannel.setText("▼  " + Ui.pad3(Adult.number(down)) + "  " + Variants.displayName(down));
    }

    public List<Channel> variants() {
        return variants;
    }

    /** Fills the panel for c; pool is where the other qualities are looked up. */
    public void bind(final Channel c, List<Channel> pool) {
        channel = c;
        time.setText(clock.format(new Date()));
        number.setText(Ui.pad3(Adult.number(c)));
        name.setText(Variants.displayName(c));
        group.setText(c.group == null ? "" : Variants.cleanGroup(c.group).toUpperCase(Locale.getDefault()));
        ImageLoader.load(c.logo, logo, Ui.dp(a, 88), R.drawable.logo_mark);
        nowLabel.setVisibility(GONE);
        nowTitle.setText("Sem programação disponível");
        nowTitle.setTextColor(Palette.TEXT_DIM);
        nowTime.setText("");
        nextTitle.setText("");
        remaining.setText("");
        progress.setVisibility(INVISIBLE);
        bindNeighbours(c);
        Epg.get(c, new Epg.Callback() {
            public void done(List<Xtream.Program> programs) {
                if (channel != c || programs.isEmpty()) return;
                Xtream.Program now = programs.get(0);
                nowLabel.setVisibility(VISIBLE);
                nowTitle.setText(now.title);
                nowTitle.setTextColor(Palette.TEXT);
                nowTime.setText(clock.format(new Date(now.start)) + " – " + clock.format(new Date(now.end)));
                progress.setProgress(Epg.progress(now));
                progress.setVisibility(VISIBLE);
                long left = (now.end - System.currentTimeMillis()) / 60000;
                remaining.setText(left > 0 ? "termina em " + left + " min" : "");
                if (programs.size() > 1) {
                    Xtream.Program next = programs.get(1);
                    nextTitle.setText("A SEGUIR  " + clock.format(new Date(next.start)) + "   " + next.title);
                }
            }
        });
        variants = Variants.of(c, pool);
        buildChips();
    }

    private void buildChips() {
        chips.removeAllViews();
        chipsBlock.setVisibility(variants.size() > 1 ? VISIBLE : GONE);
        if (variants.size() <= 1) return;
        for (final Channel v : variants) {
            boolean on = v == channel;
            TextView chip = Ui.text(a, quality(v), 14, on ? Palette.BLACK : Palette.TEXT, true);
            chip.setSingleLine(true);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setBackground(Ui.selectable(a, on ? Palette.GOLD : 0x33FFFFFF, on ? Palette.GOLD_LIGHT : 0x66FFFFFF,
                    Ui.dp(a, 16), Palette.GOLD));
            chip.setPadding(Ui.dp(a, 16), Ui.dp(a, 6), Ui.dp(a, 16), Ui.dp(a, 6));
            LinearLayout.LayoutParams lp = Ui.lp(-2, -2);
            lp.rightMargin = Ui.dp(a, 8);
            chips.addView(chip, lp);
            chip.setOnClickListener(new OnClickListener() {
                public void onClick(View view) { if (v != channel) listener.onVariant(v); }
            });
        }
    }

    /** "FHD", "4K"… taken from the name; the full name when it has no quality tag. */
    private static String quality(Channel c) {
        Matcher m = QUALITY.matcher(c.name);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            if (sb.length() > 0) sb.append(' ');
            sb.append(m.group(1).toUpperCase(Locale.ROOT));
        }
        return sb.length() > 0 ? sb.toString() : c.name;
    }

    /** The quality next to the current one (delta -1 / +1), or null when there is none. */
    public Channel neighbour(int delta) {
        if (variants.size() <= 1) return null;
        int i = variants.indexOf(channel);
        int n = variants.size();
        return variants.get(((i + delta) % n + n) % n);
    }

    public void showTyped(String digits) {
        typed.setText(digits);
        typed.setVisibility(digits == null || digits.isEmpty() ? GONE : VISIBLE);
    }
}
