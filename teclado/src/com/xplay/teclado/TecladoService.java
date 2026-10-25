package com.xplay.teclado;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.inputmethodservice.InputMethodService;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * The keyboard. The top bar always has the clipboard, the microphone and the resize button; between
 * them it shows word suggestions while typing, or the history and theme buttons otherwise.
 */
public class TecladoService extends InputMethodService
        implements KeyboardView.Listener, ClipboardManager.OnPrimaryClipChangedListener, RecognitionListener {

    private static final int KEYS = 0, VOICE = 1, RESIZE = 2, CLIP = 3, HISTORY = 4, THEMES = 5, EMOJI = 6;

    private static final String EMOJIS = "😀 😃 😄 😁 😆 😅 😂 🤣 😊 😇 🙂 😉 😌 😍 🥰 😘 😗 😙 😚 😋 😛 😜 🤪 😝 🤑"
            + " 🤗 🤭 🤫 🤔 🤐 🤨 😐 😑 😶 😏 😒 🙄 😬 😮‍💨 🤥 😔 😪 🤤 😴 😷 🤒 🤕 🤢 🤮 🥵 🥶 🥴 😵 🤯 🤠"
            + " 🥳 😎 🤓 🧐 😕 😟 🙁 😮 😯 😲 😳 🥺 😦 😧 😨 😰 😥 😢 😭 😱 😖 😣 😞 😓 😩 😫 🥱 😤 😡 😠"
            + " 🤬 😈 👿 💀 💩 🤡 👻 👽 🤖 😺 😸 😹 😻 😼 😽 🙀 😿 😾 🙈 🙉 🙊 💋 💌 💘 💝 💖 💗 💓 💞 💕"
            + " 💟 ❣️ 💔 ❤️ 🧡 💛 💚 💙 💜 🤎 🖤 🤍 💯 💢 💥 💫 💦 💨 💬 💭 💤 👋 🤚 🖐️ ✋ 🖖 👌 🤌 🤏 ✌️"
            + " 🤞 🤟 🤘 🤙 👈 👉 👆 👇 ☝️ 👍 👎 ✊ 👊 🤛 🤜 👏 🙌 👐 🤲 🤝 🙏 💪 🧠 👀 👁️ 👅 👄 👶 🧒 👦"
            + " 👧 🧑 👨 👩 🧓 👴 👵 🙋 🤷 🤦 💁 🙆 🙅 🎉 🎊 🎁 🎂 🎈 ✨ 🔥 ⭐ 🌟 ☀️ 🌙 ⚡ 🌈 ☔ ❄️ 🌹 🌻"
            + " 🌸 🍀 🌴 🐶 🐱 🐭 🐰 🦊 🐻 🐼 🐨 🐯 🦁 🐮 🐷 🐸 🐵 🐔 🐧 🐦 🦋 🐝 🍕 🍔 🍟 🌭 🍿 🥤 🍺 🍻"
            + " 🍷 ☕ 🍰 🍫 🍓 🍉 🍌 🍎 ⚽ 🏀 🏆 🎮 🎧 🎵 🎶 📱 💻 📺 📷 💡 💰 💸 💳 📌 📍 ✅ ❌ ⚠️ ❓ ❗ 🆗 🆕 🔝"
            + " 🚗 ✈️ 🏠 🇧🇷";

    private Store store;
    private Themes.Theme theme;
    private float dp;
    private final Handler handler = new Handler();

    private LinearLayout root;
    private LinearLayout bar;
    private LinearLayout suggestBar;
    private FrameLayout content;
    private KeyboardView kb;
    private int mode = KEYS;
    private float scale;

    private ClipboardManager clipboard;
    private SpeechRecognizer recognizer;
    private TextView voiceText;

    private final StringBuilder typed = new StringBuilder();
    private boolean privateField;
    private boolean noSuggestions;
    private String prefix = "";
    private List<String> suggestions = new ArrayList<>();
    private long confirmUntil;

    @Override
    public void onCreate() {
        super.onCreate();
        store = new Store(this);
        dp = getResources().getDisplayMetrics().density;
        theme = Themes.get(store.theme());
        scale = store.scale();
        clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clipboard.addPrimaryClipChangedListener(this);
    }

    @Override
    public void onDestroy() {
        clipboard.removePrimaryClipChangedListener(this);
        stopVoice();
        super.onDestroy();
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public View onCreateInputView() {
        kb = new KeyboardView(this, this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER);
        root.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(44 * dp)));
        suggestBar = new LinearLayout(this);
        suggestBar.setOrientation(LinearLayout.HORIZONTAL);
        suggestBar.setGravity(Gravity.CENTER_VERTICAL);
        root.addView(suggestBar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.round(42 * dp)));
        content = new FrameLayout(this);
        root.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        theme = Themes.get(store.theme());
        scale = store.scale();
        kb.setTheme(theme);
        kb.setScale(scale);
        kb.setVibrate(store.vibrate());
        kb.setNumberRow(store.numberRow());
        render();
        return root;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        int variation = info.inputType & InputType.TYPE_MASK_VARIATION;
        privateField = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                || (cls == InputType.TYPE_CLASS_NUMBER && variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD)
                || (info.imeOptions & 0x1000000) != 0; // IME_FLAG_NO_PERSONALIZED_LEARNING
        noSuggestions = privateField || cls != InputType.TYPE_CLASS_TEXT
                || variation == InputType.TYPE_TEXT_VARIATION_URI
                || variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                || variation == InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS;
        typed.setLength(0);
        theme = Themes.get(store.theme());
        scale = store.scale();
        kb.setTheme(theme);
        kb.setScale(scale);
        kb.setVibrate(store.vibrate());
        kb.setNumberRow(store.numberRow());
        kb.setLayout(cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE
                || cls == InputType.TYPE_CLASS_DATETIME ? KeyboardView.LAYOUT_SYMBOLS : KeyboardView.LAYOUT_LETTERS);
        kb.setShift(0);
        kb.cancel();
        readClip();
        mode = KEYS;
        updateShift();
        updateSuggestions();
        render();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        stopVoice();
        if (mode == RESIZE) store.setScale(scale);
        mode = KEYS;
        if (kb != null) kb.cancel();
        super.onFinishInputView(finishingInput);
    }

    @Override
    public void onFinishInput() {
        flushTyped();
        super.onFinishInput();
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd,
                                  int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
        // The app emptied the box (the message was sent): that is the end of one entry in the history.
        if (newSelStart == 0 && newSelEnd == 0 && typed.length() > 0) {
            InputConnection ic = getCurrentInputConnection();
            CharSequence after = ic == null ? null : ic.getTextAfterCursor(1, 0);
            if (after != null && after.length() == 0) {
                flushTyped();
                updateShift();
            }
        }
        if (mode == KEYS) updateSuggestions();
    }

    // ---- Typing.

    @Override
    public void onText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.commitText(text, 1);
        record(text);
        updateShift();
        updateSuggestions();
    }

    @Override
    public void onKey(int code) {
        InputConnection ic = getCurrentInputConnection();
        if (code == KeyboardView.EMOJI) {
            setMode(EMOJI);
            return;        }
        if (ic == null) return;
        if (code == KeyboardView.DELETE) {
            CharSequence sel = ic.getSelectedText(0);
            if (!TextUtils.isEmpty(sel)) ic.commitText("", 1);
            else ic.deleteSurroundingText(1, 0);
            if (typed.length() > 0) typed.deleteCharAt(typed.length() - 1);
            updateShift();
            updateSuggestions();
            return;
        }
        if (code == '\n') {
            if (sendAction(ic)) return;
            ic.commitText("\n", 1);
            record("\n");
            updateShift();
            updateSuggestions();
            return;
        }
        if (code == ' ') {
            ic.commitText(" ", 1);
            record(" ");
            updateShift();
            updateSuggestions();
        }
    }

    @Override
    public void onCursor(int steps) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || steps == 0) return;
        CharSequence before = ic.getTextBeforeCursor(10000, 0);
        CharSequence after = ic.getTextAfterCursor(10000, 0);
        int pos = before == null ? 0 : before.length();
        int len = pos + (after == null ? 0 : after.length());
        int target = Math.max(0, Math.min(len, pos + steps));
        ic.setSelection(target, target);
    }

    @Override
    public void onLongSpace() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showInputMethodPicker();
    }

    /** Fires the editor's own Go/Search/Send/Done action when the field asks for one. */
    private boolean sendAction(InputConnection ic) {
        EditorInfo info = getCurrentInputEditorInfo();
        if (info == null) return false;
        int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        if (action == EditorInfo.IME_ACTION_NONE || action == EditorInfo.IME_ACTION_UNSPECIFIED
                || (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0) {
            return false;
        }
        flushTyped();
        ic.performEditorAction(action);
        return true;
    }

    // ---- Remembering what was typed (locally, on this phone only).

    private void record(String text) {
        if (!store.recording() || privateField) return;
        if ("\n".equals(text)) { flushTyped(); return; }
        typed.append(text);
    }

    private void flushTyped() {
        String s = typed.toString().trim();
        typed.setLength(0);
        if (!s.isEmpty() && store.recording() && !privateField) store.addHistory(s);
    }

    // ---- Shift and word suggestions.

    private void updateShift() {
        if (kb.shift() == 2 || kb.layout() != KeyboardView.LAYOUT_LETTERS) return;
        InputConnection ic = getCurrentInputConnection();
        boolean start = true;
        if (ic != null) {
            CharSequence b = ic.getTextBeforeCursor(2, 0);
            if (b != null && b.length() > 0) {
                char last = b.charAt(b.length() - 1);
                start = last == '\n' || ((last == ' ') && b.length() >= 2
                        && ".!?".indexOf(b.charAt(b.length() - 2)) >= 0);
                if (b.length() == 1 && last == ' ') start = false;
            }
        }
        kb.setShift(start ? 1 : 0);
    }

    private void updateSuggestions() {
        if (noSuggestions || mode != KEYS) { prefix = ""; suggestions = new ArrayList<>(); if (mode == KEYS) renderSuggest(); return; }
        InputConnection ic = getCurrentInputConnection();
        String word = "";
        if (ic != null) {
            CharSequence b = ic.getTextBeforeCursor(40, 0);
            if (b != null) {
                int i = b.length();
                while (i > 0 && isWordChar(b.charAt(i - 1))) i--;
                word = b.subSequence(i, b.length()).toString();
            }
        }
        if (!word.equals(prefix)) {
            prefix = word;
            suggestions = store.suggest(word, 3);
            renderSuggest();
        }
    }

    private static boolean isWordChar(char c) {
        return Character.isLetter(c) || c == '\'' || c == '-';
    }

    private void applySuggestion(String word) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (!prefix.isEmpty()) ic.deleteSurroundingText(prefix.length(), 0);
        ic.commitText(word + " ", 1);
        ic.endBatchEdit();
        typed.append(word.substring(Math.min(prefix.length(), word.length()))).append(" ");
        prefix = "";
        suggestions = new ArrayList<>();
        updateShift();
        renderSuggest();
    }

    // ---- Clipboard.

    @Override
    public void onPrimaryClipChanged() {
        if (store.recording()) readClip();
        if (mode == CLIP) renderPanel();
    }

    private void readClip() {
        if (privateField) return;
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;
        CharSequence t = clip.getItemAt(0).coerceToText(this);
        if (t != null && t.length() > 0 && t.length() < 5000) store.addClip(t.toString());
    }

    private void paste(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.commitText(text, 1);
        record(text);
        setMode(KEYS);
    }

    // ---- Voice dictation (the phone's own speech recognizer).

    private void toggleVoice() {
        if (recognizer != null) { stopVoice(); setMode(KEYS); return; }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            toast("Reconhecimento de voz indisponível neste aparelho");
            return;
        }
        if (Build.VERSION.SDK_INT >= 23
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Intent i = new Intent(this, SetupActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            i.putExtra("askMic", true);
            startActivity(i);
            toast("Permita o microfone e toque no microfone de novo");
            return;
        }
        setMode(VOICE);
        startVoice();
    }

    private void startVoice() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(this);
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "pt-BR");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        i.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());
        try {
            recognizer.startListening(i);
            if (voiceText != null) voiceText.setText("Ouvindo… fale agora");
        } catch (Exception e) {
            toast("Não foi possível iniciar o microfone");
            stopVoice();
            setMode(KEYS);
        }
    }

    private void stopVoice() {
        if (recognizer != null) {
            try { recognizer.destroy(); } catch (Exception ignored) {}
            recognizer = null;
        }
    }

    @Override public void onReadyForSpeech(Bundle p) {}
    @Override public void onBeginningOfSpeech() {}
    @Override public void onRmsChanged(float db) {}
    @Override public void onBufferReceived(byte[] b) {}
    @Override public void onEndOfSpeech() { if (voiceText != null) voiceText.setText("Processando…"); }

    @Override
    public void onPartialResults(Bundle results) {
        List<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (voiceText != null && list != null && !list.isEmpty() && !list.get(0).isEmpty()) {
            voiceText.setText(list.get(0));
        }
    }

    @Override
    public void onResults(Bundle results) {
        List<String> list = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (list != null && !list.isEmpty() && !list.get(0).isEmpty()) {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                String text = list.get(0);
                CharSequence before = ic.getTextBeforeCursor(1, 0);
                if (before != null && before.length() > 0 && before.charAt(0) != ' ' && before.charAt(0) != '\n') {
                    text = " " + text;
                }
                ic.commitText(text, 1);
                record(text);
            }
        }
        stopVoice();
        setMode(KEYS);
        updateShift();
    }

    @Override
    public void onError(int error) {
        if (voiceText != null) {
            voiceText.setText(error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                    ? "Não entendi. Toque para tentar de novo." : "Erro no microfone. Toque para tentar de novo.");
        }
        stopVoice();
    }

    @Override public void onEvent(int type, Bundle params) {}

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ---- Switching between the keyboard and the panels.

    private void setMode(int m) {
        if (mode == RESIZE && m != RESIZE) store.setScale(scale);
        if (mode == VOICE && m != VOICE) stopVoice();
        mode = m;
        render();
    }

    private void render() {
        renderBar();
        renderSuggest();
        renderPanel();
    }

    private void renderPanel() {
        content.removeAllViews();
        voiceText = null;
        switch (mode) {
            case VOICE: content.addView(voicePanel()); break;
            case RESIZE: content.addView(resizePanel()); break;
            case CLIP: content.addView(listPanel(true)); break;
            case HISTORY: content.addView(listPanel(false)); break;
            case THEMES: content.addView(themesPanel()); break;
            case EMOJI: content.addView(emojiPanel()); break;
            default:
                if (kb.getParent() != null) ((ViewGroup) kb.getParent()).removeView(kb);
                content.addView(kb, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        root.setBackgroundColor(theme.bg);
    }

    // ---- The icon toolbar (top row). The icons are evenly spread and centered,
    //      with the clipboard ("copiar") in the exact middle, like the reference keyboard.

    private void renderBar() {
        if (bar == null) return;
        bar.removeAllViews();
        bar.setBackgroundColor(blend(theme.bg, theme.key, 0.35f));

        if (mode != KEYS && mode != RESIZE) {
            bar.setWeightSum(0);
            bar.addView(barButton(R.drawable.ic_back, theme.keyText, "voltar", new Runnable() {
                public void run() { setMode(KEYS); }
            }));
            bar.addView(title(titleFor(mode)));
            if (mode == CLIP) {
                bar.addView(barButton(R.drawable.ic_history, theme.keyText, "histórico", new Runnable() {
                    public void run() { setMode(HISTORY); }
                }));
            }
            if (mode == CLIP || mode == HISTORY) {
                bar.addView(barButton(R.drawable.ic_delete, theme.keyText, "limpar", new Runnable() {
                    public void run() {
                        if (mode == CLIP) store.clearClips(); else store.clearHistory();
                        renderPanel();
                    }
                }));
            }
            return;
        }

        if (mode == RESIZE) {
            bar.setWeightSum(0);
            bar.addView(barButton(R.drawable.ic_mic, theme.keyText, "microfone", new Runnable() {
                public void run() { toggleVoice(); }
            }));
            bar.addView(title("Tamanho do teclado"));
            bar.addView(barButton(R.drawable.ic_check, theme.accent, "pronto", new Runnable() {
                public void run() { setMode(KEYS); }
            }));
            return;
        }

        boolean rec = recognizer != null;
        bar.setWeightSum(7f);
        barIcon(R.drawable.ic_emoji, theme.keyText, "emojis", new Runnable() {
            public void run() { setMode(EMOJI); }
        });
        barIcon(R.drawable.ic_mic, rec ? theme.accent : theme.keyText, "microfone", new Runnable() {
            public void run() { toggleVoice(); }
        });
        barIcon(R.drawable.ic_history, theme.keyText, "histórico", new Runnable() {
            public void run() { setMode(HISTORY); }
        });
        barIcon(R.drawable.ic_clip, theme.keyText, "área de transferência", new Runnable() {
            public void run() { setMode(CLIP); }
        });
        barIcon(R.drawable.ic_palette, theme.keyText, "temas", new Runnable() {
            public void run() { setMode(THEMES); }
        });
        barIcon(R.drawable.ic_resize, theme.keyText, "redimensionar", new Runnable() {
            public void run() { setMode(RESIZE); }
        });
        barIcon(R.drawable.ic_more, theme.keyText, "ajustes", new Runnable() {
            public void run() { openSettings(); }
        });
    }

    /** One evenly-weighted centered icon in the toolbar. */
    private void barIcon(int icon, int color, String desc, final Runnable onTap) {
        ImageView v = new ImageView(this);
        v.setImageResource(icon);
        v.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        v.setContentDescription(desc);
        int p = Math.round(10 * dp);
        v.setPadding(p, p, p, p);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        tap(v, onTap);
        bar.addView(v);
    }

    private void openSettings() {
        Intent i = new Intent(this, SetupActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    // ---- The suggestions row (just above the keys): the words for what is being typed.

    private void renderSuggest() {
        if (suggestBar == null) return;
        suggestBar.removeAllViews();
        boolean show = mode == KEYS || mode == RESIZE;
        suggestBar.setVisibility(show ? View.VISIBLE : View.GONE);
        if (!show) return;
        suggestBar.setBackgroundColor(theme.bg);
        for (int i = 0; i < 3; i++) {
            if (i > 0) suggestBar.addView(divider());
            String word = i < suggestions.size() ? suggestions.get(i) : null;
            suggestBar.addView(suggestionChip(word, i == 0 && suggestions.size() > 1));
        }
    }

    private View suggestionChip(final String word, boolean strong) {
        TextView t = new TextView(this);
        t.setText(word == null ? "" : word);
        t.setTextColor(strong ? theme.accent : theme.keyText);
        t.setTextSize(16);
        t.setTypeface(strong ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        t.setGravity(Gravity.CENTER);
        t.setMaxLines(1);
        t.setEllipsize(TextUtils.TruncateAt.END);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        if (word != null) tap(t, new Runnable() { public void run() { applySuggestion(word); } });
        return t;
    }

    private View divider() {
        View v = new View(this);
        v.setBackgroundColor(theme.hint & 0x55FFFFFF);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Math.round(1 * dp), Math.round(20 * dp));
        v.setLayoutParams(lp);
        return v;
    }

    private TextView title(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(theme.keyText);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(Math.round(6 * dp), 0, 0, 0);
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        return t;
    }

    private View barButton(int icon, int color, String desc, final Runnable onTap) {
        ImageView v = new ImageView(this);
        v.setImageResource(icon);
        v.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        v.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        v.setContentDescription(desc);
        int s = Math.round(46 * dp);
        v.setPadding(Math.round(12 * dp), Math.round(12 * dp), Math.round(12 * dp), Math.round(12 * dp));
        v.setLayoutParams(new LinearLayout.LayoutParams(s, ViewGroup.LayoutParams.MATCH_PARENT));
        tap(v, onTap);
        return v;
    }

    private String titleFor(int m) {
        switch (m) {
            case CLIP: return "Área de transferência";
            case HISTORY: return "Histórico";
            case THEMES: return "Temas";
            case EMOJI: return "Emojis";
            default: return "";
        }
    }

    // ---- Panels. They all stand as tall as the keyboard so the field never jumps.

    private int panelHeight() {
        return kb.heightFor(scale) + Math.round(8 * dp);
    }

    private View voicePanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setBackgroundColor(theme.bg);
        box.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight()));

        ImageView mic = new ImageView(this);
        mic.setImageResource(R.drawable.ic_mic);
        mic.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
        int d = Math.round(56 * dp);
        mic.setPadding(Math.round(16 * dp), Math.round(16 * dp), Math.round(16 * dp), Math.round(16 * dp));
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(theme.accent);
        mic.setBackground(circle);
        LinearLayout.LayoutParams mlp = new LinearLayout.LayoutParams(d, d);
        box.addView(mic, mlp);
        pulse(mic);

        voiceText = new TextView(this);
        voiceText.setText("Ouvindo… fale agora");
        voiceText.setTextColor(theme.keyText);
        voiceText.setTextSize(16);
        voiceText.setGravity(Gravity.CENTER);
        voiceText.setPadding(Math.round(24 * dp), Math.round(16 * dp), Math.round(24 * dp), Math.round(4 * dp));
        box.addView(voiceText);

        TextView hint = new TextView(this);
        hint.setText("Toque no microfone para ouvir de novo  ·  voltar para fechar");
        hint.setTextColor(theme.hint);
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, Math.round(8 * dp), 0, 0);
        box.addView(hint);

        tap(mic, new Runnable() { public void run() { stopVoice(); startVoice(); } });
        return box;
    }

    private void pulse(View v) {
        AlphaAnimation a = new AlphaAnimation(1f, 0.4f);
        a.setDuration(700);
        a.setRepeatMode(Animation.REVERSE);
        a.setRepeatCount(Animation.INFINITE);
        v.startAnimation(a);
    }

    private View resizePanel() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(theme.bg);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER);
        controls.setPadding(0, Math.round(10 * dp), 0, Math.round(10 * dp));
        controls.addView(roundButton(R.drawable.ic_minus, new Runnable() {
            public void run() { changeScale(-0.1f); }
        }));
        final TextView pct = new TextView(this);
        pct.setTextColor(theme.keyText);
        pct.setTextSize(18);
        pct.setTypeface(Typeface.DEFAULT_BOLD);
        pct.setGravity(Gravity.CENTER);
        pct.setText(Math.round(scale * 100) + "%");
        pct.setMinWidth(Math.round(80 * dp));
        controls.addView(pct);
        controls.addView(roundButton(R.drawable.ic_plus, new Runnable() {
            public void run() { changeScale(0.1f); }
        }));
        box.addView(controls);

        if (kb.getParent() != null) ((ViewGroup) kb.getParent()).removeView(kb);
        box.addView(kb, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        pctRef = pct;
        return box;
    }

    private TextView pctRef;

    private void changeScale(float d) {
        scale = Store.clampScale(Math.round((scale + d) * 10) / 10f);
        store.setScale(scale);
        kb.setScale(scale);
        if (pctRef != null) pctRef.setText(Math.round(scale * 100) + "%");
    }

    private View roundButton(int icon, Runnable onTap) {
        ImageView v = new ImageView(this);
        v.setImageResource(icon);
        v.setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_IN);
        int d = Math.round(44 * dp);
        v.setPadding(Math.round(10 * dp), Math.round(10 * dp), Math.round(10 * dp), Math.round(10 * dp));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.OVAL);
        bg.setColor(theme.special);
        v.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(d, d);
        lp.setMargins(Math.round(18 * dp), 0, Math.round(18 * dp), 0);
        v.setLayoutParams(lp);
        tap(v, onTap);
        return v;
    }

    private View listPanel(final boolean clips) {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(theme.bg);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight()));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Math.round(8 * dp), Math.round(6 * dp), Math.round(8 * dp), Math.round(6 * dp));
        scroll.addView(list);

        List<Store.Item> items = clips ? Store.sortedClips(store.clips()) : store.history();
        if (items.isEmpty()) {
            list.addView(empty(clips
                    ? "Nada copiado ainda. O que você copiar aparece aqui."
                    : (store.recording() ? "Nada por aqui ainda." : "A gravação está desligada (toque no engrenagem no app).")));
            return scroll;
        }
        SimpleDateFormat fmt = new SimpleDateFormat("dd/MM HH:mm", new Locale("pt", "BR"));
        for (final Store.Item item : items) list.addView(row(item, clips, fmt));
        return scroll;
    }

    private View row(final Store.Item item, final boolean clips, SimpleDateFormat fmt) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(theme.key);
        bg.setCornerRadius(10 * dp);
        r.setBackground(bg);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, Math.round(3 * dp), 0, Math.round(3 * dp));
        r.setLayoutParams(rlp);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.setPadding(Math.round(12 * dp), Math.round(10 * dp), Math.round(8 * dp), Math.round(10 * dp));
        texts.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView t = new TextView(this);
        t.setText(item.text.replace("\n", " ").trim());
        t.setTextColor(theme.keyText);
        t.setTextSize(15);
        t.setMaxLines(2);
        t.setEllipsize(TextUtils.TruncateAt.END);
        texts.addView(t);
        if (item.time > 0) {
            TextView when = new TextView(this);
            when.setText((clips && item.pinned ? "📌 " : "") + fmt.format(new Date(item.time)));
            when.setTextColor(theme.hint);
            when.setTextSize(11);
            texts.addView(when);
        }
        r.addView(texts);
        tap(texts, new Runnable() { public void run() { paste(item.text); } });

        if (clips) {
            r.addView(rowIcon(item.pinned ? R.drawable.ic_pin : R.drawable.ic_pin,
                    item.pinned ? theme.accent : theme.hint, new Runnable() {
                public void run() { store.togglePin(item); renderPanel(); }
            }));
        }
        r.addView(rowIcon(R.drawable.ic_close, theme.hint, new Runnable() {
            public void run() {
                if (clips) store.removeClip(item); else store.removeHistory(item);
                renderPanel();
            }
        }));
        return r;
    }

    private View rowIcon(int icon, int color, Runnable onTap) {
        ImageView v = new ImageView(this);
        v.setImageResource(icon);
        v.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        int s = Math.round(42 * dp);
        v.setPadding(Math.round(10 * dp), Math.round(10 * dp), Math.round(10 * dp), Math.round(10 * dp));
        v.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        tap(v, onTap);
        return v;
    }

    private View empty(String msg) {
        TextView t = new TextView(this);
        t.setText(msg);
        t.setTextColor(theme.hint);
        t.setTextSize(14);
        t.setGravity(Gravity.CENTER);
        t.setPadding(Math.round(24 * dp), Math.round(40 * dp), Math.round(24 * dp), Math.round(40 * dp));
        return t;
    }

    private View themesPanel() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(theme.bg);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight()));
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(Math.round(10 * dp), Math.round(8 * dp), Math.round(10 * dp), Math.round(8 * dp));
        scroll.addView(list);
        for (int i = 0; i < Themes.ALL.length; i++) list.addView(themeRow(i));
        return scroll;
    }

    private View themeRow(final int index) {
        final Themes.Theme t = Themes.get(index);
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(Math.round(12 * dp), Math.round(10 * dp), Math.round(12 * dp), Math.round(10 * dp));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(t.bg);
        bg.setCornerRadius(12 * dp);
        bg.setStroke(Math.round((index == store.theme() ? 3 : 1) * dp), index == store.theme() ? t.accent : t.hint);
        r.setBackground(bg);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rlp.setMargins(0, Math.round(4 * dp), 0, Math.round(4 * dp));
        r.setLayoutParams(rlp);

        for (int c = 0; c < 3; c++) {
            View sw = new View(this);
            GradientDrawable d = new GradientDrawable();
            d.setShape(GradientDrawable.OVAL);
            d.setColor(c == 0 ? t.key : c == 1 ? t.accent : t.special);
            sw.setBackground(d);
            int s = Math.round(22 * dp);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(s, s);
            lp.setMargins(0, 0, Math.round(6 * dp), 0);
            r.addView(sw, lp);
        }
        TextView name = new TextView(this);
        name.setText(t.name);
        name.setTextColor(t.keyText);
        name.setTextSize(16);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setPadding(Math.round(8 * dp), 0, 0, 0);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        r.addView(name);
        if (index == store.theme()) {
            ImageView check = new ImageView(this);
            check.setImageResource(R.drawable.ic_check);
            check.setColorFilter(t.accent, PorterDuff.Mode.SRC_IN);
            r.addView(check, new LinearLayout.LayoutParams(Math.round(26 * dp), Math.round(26 * dp)));
        }
        tap(r, new Runnable() {
            public void run() {
                store.setTheme(index);
                theme = Themes.get(index);
                kb.setTheme(theme);
                render();
            }
        });
        return r;
    }

    private View emojiPanel() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(theme.bg);
        scroll.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelHeight()));
        final int cols = Math.max(6, getResources().getDisplayMetrics().widthPixels / Math.round(48 * dp));
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(Math.round(4 * dp), Math.round(4 * dp), Math.round(4 * dp), Math.round(4 * dp));
        scroll.addView(grid);
        String[] all = EMOJIS.split(" ");
        LinearLayout rowView = null;
        for (int i = 0; i < all.length; i++) {
            if (i % cols == 0) {
                rowView = new LinearLayout(this);
                rowView.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                grid.addView(rowView);
            }
            rowView.addView(emojiCell(all[i]));
        }
        return scroll;
    }

    private View emojiCell(final String e) {
        TextView t = new TextView(this);
        t.setText(e);
        t.setTextSize(24);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, Math.round(8 * dp), 0, Math.round(8 * dp));
        t.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        tap(t, new Runnable() {
            public void run() {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) { ic.commitText(e, 1); record(e); }
            }
        });
        return t;
    }

    // ---- A touch helper that gives a quick press highlight without pulling in extra resources.

    private void tap(final View v, final Runnable onTap) {
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        view.setAlpha(0.5f);
                        return true;
                    case MotionEvent.ACTION_UP:
                        view.setAlpha(1f);
                        float x = e.getX(), y = e.getY();
                        if (x >= 0 && y >= 0 && x <= view.getWidth() && y <= view.getHeight()) {
                            if (store.vibrate()) view.performHapticFeedback(
                                    android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                            onTap.run();
                        }
                        return true;
                    case MotionEvent.ACTION_CANCEL:
                        view.setAlpha(1f);
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private static int blend(int a, int b, float t) {
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = Math.round(ar + (br - ar) * t), g = Math.round(ag + (bg - ag) * t), bl = Math.round(ab + (bb - ab) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
