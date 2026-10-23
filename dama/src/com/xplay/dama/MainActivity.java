package com.xplay.dama;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements GameView.Listener {
    private static final int BG = 0xFF1B1B1B, GOLD = 0xFFFFC107;

    /** 0 = dois jogadores; senão, profundidade da IA (que joga com as pretas). */
    private int aiDepth;
    private Board board;
    private Board.Move lastMove;
    private final List<Board> history = new ArrayList<>();
    private final List<Board.Move> historyMoves = new ArrayList<>();
    private boolean inGame, thinking, gameOver;
    private int generation; // invalida respostas da IA de partidas anteriores

    private GameView gameView;
    private TextView status, whiteCount, blackCount;
    private Button undoButton;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getSharedPreferences("dama", MODE_PRIVATE);
        showMenu();
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        if (inGame) showGame();
        else showMenu();
    }

    @Override
    public void onBackPressed() {
        if (inGame) {
            generation++;
            if (gameView != null) gameView.cancelAnimation();
            thinking = false;
            inGame = false;
            showMenu();
        } else {
            super.onBackPressed();
        }
    }

    // ---------- menu ----------

    private void showMenu() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(BG);
        int pad = dp(24);
        root.setPadding(pad, pad, pad, pad);

        TextView title = text("Damas", 44, GOLD);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);
        TextView sub = text("Regras brasileiras", 16, 0xFFBBBBBB);
        sub.setPadding(0, 0, 0, dp(20));
        root.addView(sub);

        String[] labels = {"Contra o celular — Fácil", "Contra o celular — Médio",
                "Contra o celular — Difícil", "Dois jogadores"};
        final int[] depths = {2, 4, 6, 0};
        Button first = null;
        for (int i = 0; i < labels.length; i++) {
            final int depth = depths[i];
            Button b = button(labels[i]);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { startGame(depth); }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(300), ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.topMargin = dp(8);
            root.addView(b, lp);
            if (first == null) first = b;
        }

        int wins = prefs.getInt("wins", 0), losses = prefs.getInt("losses", 0);
        TextView score = text("Vitórias: " + wins + "   Derrotas: " + losses, 15, 0xFF999999);
        score.setPadding(0, dp(20), 0, 0);
        root.addView(score);

        setContentView(root);
        if (first != null) first.requestFocus();
    }

    // ---------- partida ----------

    private void startGame(int depth) {
        aiDepth = depth;
        board = Board.initial();
        lastMove = null;
        history.clear();
        historyMoves.clear();
        gameOver = false;
        thinking = false;
        generation++;
        inGame = true;
        showGame();
    }

    private void showGame() {
        Configuration cfg = getResources().getConfiguration();
        boolean landscape = cfg.orientation == Configuration.ORIENTATION_LANDSCAPE;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(BG);
        int pad = dp(12);
        root.setPadding(pad, pad, pad, pad);

        if (gameView != null) gameView.cancelAnimation();
        gameView = new GameView(this);
        gameView.setListener(this);

        LinearLayout side = new LinearLayout(this);
        side.setOrientation(LinearLayout.VERTICAL);
        side.setGravity(Gravity.CENTER);
        side.setPadding(landscape ? dp(20) : 0, landscape ? 0 : dp(16), 0, 0);

        status = text("", 20, Color.WHITE);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        side.addView(status);

        LinearLayout counts = new LinearLayout(this);
        counts.setGravity(Gravity.CENTER);
        whiteCount = text("", 16, 0xFFF0E6D0);
        blackCount = text("", 16, 0xFFAAAAAA);
        whiteCount.setPadding(0, dp(6), dp(16), dp(6));
        blackCount.setPadding(dp(16), dp(6), 0, dp(6));
        counts.addView(whiteCount);
        counts.addView(blackCount);
        side.addView(counts);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(landscape ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.CENTER);
        undoButton = button("Desfazer");
        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { undo(); }
        });
        Button newGame = button("Novo jogo");
        newGame.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { startGame(aiDepth); }
        });
        Button menu = button("Menu");
        menu.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onBackPressed(); }
        });
        for (Button b : new Button[]{undoButton, newGame, menu}) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    landscape ? dp(170) : ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(dp(4), dp(4), dp(4), dp(4));
            buttons.addView(b, lp);
        }
        side.addView(buttons);

        LinearLayout.LayoutParams boardLp = landscape
                ? new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f)
                : new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        root.addView(gameView, boardLp);
        root.addView(side, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(root);
        gameView.requestFocus();

        refresh();
        if (thinking) {
            // a IA continua pensando; a resposta chega em aiMove()
        } else if (!gameOver && isAiTurn()) {
            thinkAi();
        }
    }

    private boolean isAiTurn() {
        return aiDepth > 0 && board.turn == Board.BLACK;
    }

    private void refresh() {
        boolean human = !gameOver && !thinking && !isAiTurn();
        gameView.setBoard(board, lastMove, human);
        whiteCount.setText("● Brancas: " + board.count(Board.WHITE));
        blackCount.setText("● Pretas: " + board.count(Board.BLACK));
        undoButton.setEnabled(!history.isEmpty() && !thinking);
        int res = board.result();
        if (res == Board.WHITE) status.setText(aiDepth > 0 ? "Você venceu!" : "Brancas venceram!");
        else if (res == Board.BLACK) status.setText(aiDepth > 0 ? "O celular venceu" : "Pretas venceram!");
        else if (res == 2) status.setText("Empate");
        else if (aiDepth > 0) status.setText(board.turn == Board.WHITE ? "Sua vez" : "Pensando…");
        else status.setText(board.turn == Board.WHITE ? "Vez das brancas" : "Vez das pretas");
    }

    @Override
    public void onHumanMove(Board.Move move) {
        play(move);
    }

    private void play(final Board.Move move) {
        final int gen = generation;
        gameView.animateMove(move, new Runnable() {
            @Override public void run() {
                if (gen != generation) return;
                history.add(board.copy());
                historyMoves.add(lastMove);
                board.apply(move);
                lastMove = move;
                afterMove();
            }
        });
    }

    private void afterMove() {
        int res = board.result();
        if (res != 0) {
            gameOver = true;
            refresh();
            finishGame(res);
            return;
        }
        refresh();
        if (isAiTurn()) thinkAi();
    }

    private void thinkAi() {
        thinking = true;
        refresh();
        final int gen = generation;
        final Board snapshot = board.copy();
        final int depth = aiDepth;
        final long start = System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override public void run() {
                final Board.Move m = new Ai(depth).choose(snapshot);
                long wait = Math.max(0, 450 - (System.currentTimeMillis() - start));
                ui.postDelayed(new Runnable() {
                    @Override public void run() {
                        if (gen != generation || m == null) return;
                        thinking = false;
                        refresh();
                        gameView.setBoard(board, lastMove, false);
                        play(m);
                    }
                }, wait);
            }
        }).start();
    }

    private void undo() {
        if (thinking || history.isEmpty() || gameView.isAnimating()) return;
        int steps = 1;
        if (aiDepth > 0) {
            // volta até a última vez em que era a vez do jogador
            steps = board.turn == Board.WHITE ? 2 : 1;
            if (steps > history.size()) steps = history.size();
        }
        for (int i = 0; i < steps; i++) {
            board = history.remove(history.size() - 1);
            lastMove = historyMoves.remove(historyMoves.size() - 1);
        }
        gameOver = false;
        generation++;
        refresh();
        if (isAiTurn()) thinkAi();
    }

    private void finishGame(int res) {
        String msg;
        if (res == 2) {
            msg = "Empate: 20 lances seguidos só com damas.";
        } else if (aiDepth > 0) {
            boolean won = res == Board.WHITE;
            String key = won ? "wins" : "losses";
            prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply();
            msg = won ? "Parabéns, você venceu!" : "O celular venceu desta vez.";
        } else {
            msg = (res == Board.WHITE ? "As brancas" : "As pretas") + " venceram!";
        }
        new AlertDialog.Builder(this)
                .setTitle("Fim de jogo")
                .setMessage(msg)
                .setPositiveButton("Jogar de novo", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface d, int w) { startGame(aiDepth); }
                })
                .setNegativeButton("Fechar", null)
                .show();
    }

    // ---------- helpers de UI ----------

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }

    private TextView text(String s, int sp, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        b.setTextColor(Color.WHITE);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_pressed}, rounded(0xFF6D4C1F, GOLD));
        bg.addState(new int[]{android.R.attr.state_focused}, rounded(0xFF3A3A3A, GOLD));
        bg.addState(new int[]{-android.R.attr.state_enabled}, rounded(0xFF262626, 0xFF262626));
        bg.addState(new int[]{}, rounded(0xFF333333, 0xFF444444));
        b.setBackground(bg);
        b.setStateListAnimator(null);
        return b;
    }

    private GradientDrawable rounded(int fill, int border) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(10));
        g.setStroke(dp(2), border);
        return g;
    }
}
