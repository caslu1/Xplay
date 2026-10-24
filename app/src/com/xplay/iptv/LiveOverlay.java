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

        // Top: channels above/below (what ▲ ▼ open) on the left, clock and typed number on the right.
        LinearLayout topLine = Ui.row(a);
        topLine.setGravity(Gravity.TOP);
        topLine.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {0xB3000000, 0x00000000}));
        topLine.setPadding(Ui.dp(a, 28), Ui.dp(a, 18), Ui.dp(a, 28), Ui.dp(a, 40));
        LinearLayout zap = Ui.column(a);
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
        topLine.addView(zap, Ui.weight(1));
        LinearLayout topRight = Ui.column(a);
        topRight.setGravity(Gravity.END);
        time = Ui.text(a, "", 22, Palette.TEXT, true);
        time.setShadowLayer(8, 0, 1, Palette.BLACK);
        topRight.addView(time);
        typed = Ui.text(a, "", 36, Palette.BLACK, true);
        typed.setBackground(Ui.round(Palette.GOLD, Ui.dp(a, 12)));
        typed.setPadding(Ui.dp(a, 16), Ui.dp(a, 2), Ui.dp(a, 16), Ui.dp(a, 2));
        typed.setVisibility(GONE);
        LinearLayout.LayoutParams tl = Ui.lp(-2, -2);
        tl.topMargin = Ui.dp(a, 8);
        topRight.addView(typed, tl);
        topLine.addView(topRight);
        addView(topLine, new FrameLayout.LayoutParams(-1, -2, Gravity.TOP));

        // Bottom: a floating card.
        LinearLayout card = Ui.row(a);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setBackground(Ui.stroke(0xEB0E0E0E, Ui.dp(a, 20), 1, 0x22FFFFFF));
        int cp = Ui.dp(a, 18);
        card.setPadding(cp, cp, Ui.dp(a, 22), cp);
        FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        cl.setMargins(Ui.dp(a, 24), 0, Ui.dp(a, 24), Ui.dp(a, 20));
        addView(card, cl);

        LinearLayout left = Ui.column(a);
        left.setGravity(Gravity.CENTER_HORIZONTAL);
        FrameLayout logoBox = new FrameLayout(a);
        logoBox.setBackground(Ui.round(0x1FFFFFFF, Ui.dp(a, 12)));
        logo = new ImageView(a);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        int lp = Ui.dp(a, 8);
        logo.setPadding(lp, lp, lp, lp);
        logoBox.addView(logo, new FrameLayout.LayoutParams(-1, -1));
        left.addView(logoBox, Ui.lp(Ui.dp(a, 96), Ui.dp(a, 60)));
        number = Ui.text(a, "", 15, Palette.GOLD, true);
        number.setGravity(Gravity.CENTER);
        number.setLetterSpacing(0.08f);
        LinearLayout.LayoutParams numl = Ui.lp(-2, -2);
        numl.topMargin = Ui.dp(a, 6);
        left.addView(number, numl);
        card.addView(left);

        LinearLayout info = Ui.column(a);
        info.setPadding(Ui.dp(a, 18), 0, 0, 0);
        LinearLayout nameLine = Ui.row(a);
        nameLine.setGravity(Gravity.CENTER_VERTICAL);
        TextView liveBadge = Ui.text(a, "● AO VIVO", 10, Palette.TEXT, true);
        liveBadge.setBackground(Ui.round(Palette.HOT, Ui.dp(a, 5)));
        liveBadge.setPadding(Ui.dp(a, 7), Ui.dp(a, 2), Ui.dp(a, 7), Ui.dp(a, 2));
        nameLine.addView(liveBadge);
        name = Ui.text(a, "", 19, Palette.TEXT, true);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        name.setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 10), 0);
        nameLine.addView(name, new LinearLayout.LayoutParams(0, -2, 1f));
        group = Ui.text(a, "", 11, Palette.TEXT_DIM, true);
        group.setSingleLine(true);
        group.setLetterSpacing(0.06f);
        nameLine.addView(group);
        info.addView(nameLine, Ui.lp(-1, -2));

        LinearLayout nowLine = Ui.row(a);
        nowLine.setGravity(Gravity.CENTER_VERTICAL);
        nowLabel = Ui.text(a, "AGORA", 10, Palette.BLACK, true);
        nowLabel.setBackground(Ui.round(Palette.GOLD, Ui.dp(a, 4)));
        nowLabel.setPadding(Ui.dp(a, 6), Ui.dp(a, 1), Ui.dp(a, 6), Ui.dp(a, 1));
        nowLine.addView(nowLabel);
        nowTitle = Ui.text(a, "", 14, Palette.TEXT, false);
        nowTitle.setSingleLine(true);
        nowTitle.setEllipsize(TextUtils.TruncateAt.END);
        nowTitle.setPadding(Ui.dp(a, 10), 0, Ui.dp(a, 10), 0);
        nowLine.addView(nowTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        nowTime = Ui.text(a, "", 12, Palette.GOLD, true);
        nowLine.addView(nowTime);
        LinearLayout.LayoutParams nl = Ui.lp(-1, -2);
        nl.topMargin = Ui.dp(a, 8);
        info.addView(nowLine, nl);

        progress = new ProgressBar(a, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(1000);
        progress.setProgressTintList(ColorStateList.valueOf(Palette.GOLD));
        progress.setProgressBackgroundTintList(ColorStateList.valueOf(0x40FFFFFF));
        LinearLayout.LayoutParams pl = Ui.lp(-1, Ui.dp(a, 3));
        pl.topMargin = Ui.dp(a, 8);
        info.addView(progress, pl);

        LinearLayout nextLine = Ui.row(a);
        nextLine.setGravity(Gravity.CENTER_VERTICAL);
        nextTitle = Ui.text(a, "", 12, Palette.TEXT_DIM, false);
        nextTitle.setSingleLine(true);
        nextTitle.setEllipsize(TextUtils.TruncateAt.END);
        nextLine.addView(nextTitle, new LinearLayout.LayoutParams(0, -2, 1f));
        remaining = Ui.text(a, "", 11, Palette.TEXT_DIM, false);
        nextLine.addView(remaining);
        LinearLayout.LayoutParams xl = Ui.lp(-1, -2);
        xl.topMargin = Ui.dp(a, 6);
        info.addView(nextLine, xl);

        // Qualities, under the programs.
        LinearLayout chipsLine = Ui.row(a);
        chipsLine.setGravity(Gravity.CENTER_VERTICAL);
        chipsBlock = chipsLine;
        hint = Ui.text(a, "QUALIDADE  ◀ ▶", 10, Palette.TEXT_DIM, true);
        hint.setPadding(0, 0, Ui.dp(a, 10), 0);
        chipsLine.addView(hint);
        HorizontalScrollView hs = new HorizontalScrollView(a);
        hs.setHorizontalScrollBarEnabled(false);
        chips = Ui.row(a);
        hs.addView(chips);
        chipsLine.addView(hs, Ui.weight(1));
        LinearLayout.LayoutParams chl = Ui.lp(-1, -2);
        chl.topMargin = Ui.dp(a, 10);
        info.addView(chipsLine, chl);
        card.addView(info, new LinearLayout.LayoutParams(0, -2, 1f));
    }

    private static final Pattern TIME_TAG = Pattern.compile("\\s*[-|•]?\\s*\\b([01]?\\d|2[0-3])\\s*[:hH]\\s*[0-5]\\d\\b\\s*");

    /** Name without quality tags or a trailing kick-off time ("… x … - 18:30"). */
    private static String cleanName(Channel c) {
        String n = TIME_TAG.matcher(Variants.displayName(c)).replaceAll(" ").trim();
        return n.isEmpty() ? c.name : n;
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
        upNextChannel.setText("▲  " + Ui.pad3(Adult.number(up)) + "   " + cleanName(up));
        downNextChannel.setText("▼  " + Ui.pad3(Adult.number(down)) + "   " + cleanName(down));
    }

    public List<Channel> variants() {
        return variants;
    }

    /** Fills the panel for c; pool is where the other qualities are looked up. */
    public void bind(final Channel c, List<Channel> pool) {
        channel = c;
        time.setText(clock.format(new Date()));
        number.setText("CANAL " + Ui.pad3(Adult.number(c)));
        name.setText(cleanName(c));
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
                // Guides with a wrong time zone give silly values; only show what makes sense.
                remaining.setText(left > 0 && left <= 180 && now.start <= System.currentTimeMillis()
                        ? "termina em " + left + " min" : "");
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
            TextView chip = Ui.text(a, quality(v), 12, on ? Palette.BLACK : Palette.TEXT, true);
            chip.setSingleLine(true);
            chip.setFocusable(true);
            chip.setClickable(true);
            chip.setBackground(Ui.selectable(a, on ? Palette.GOLD : 0x33FFFFFF, on ? Palette.GOLD_LIGHT : 0x66FFFFFF,
                    Ui.dp(a, 16), Palette.GOLD));
            chip.setPadding(Ui.dp(a, 12), Ui.dp(a, 4), Ui.dp(a, 12), Ui.dp(a, 4));
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
