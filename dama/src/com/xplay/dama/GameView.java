package com.xplay.dama;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import java.util.ArrayList;
import java.util.List;

/** Desenha o tabuleiro e trata toques e o controle remoto (D-pad). */
public class GameView extends View {
    public interface Listener {
        /** Chamado quando o jogador escolhe um lance completo. */
        void onHumanMove(Board.Move move);
    }

    private static final int N = Board.N;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Path crown = new Path();

    private Board board = Board.initial();
    private List<Board.Move> legal = new ArrayList<>();
    private final List<Integer> selection = new ArrayList<>();
    private boolean inputEnabled = true;
    private Board.Move lastMove;
    private Listener listener;

    private Board.Move animMove;
    private float animPos;
    private ValueAnimator animator;

    private int cursor = 5 * N + 0;
    private boolean cursorVisible;

    private float cell, left, top;

    public GameView(Context context) {
        super(context);
        setFocusable(true);
        setFocusableInTouchMode(true);
        stroke.setStyle(Paint.Style.STROKE);
    }

    public void setListener(Listener l) { listener = l; }

    public void setBoard(Board b, Board.Move last, boolean humanTurn) {
        board = b;
        lastMove = last;
        inputEnabled = humanTurn;
        legal = humanTurn ? b.legalMoves() : new ArrayList<Board.Move>();
        selection.clear();
        invalidate();
    }

    public boolean isAnimating() { return animMove != null; }

    /** Anima a peça percorrendo o caminho e chama {@code done} ao terminar. */
    public void animateMove(Board.Move m, final Runnable done) {
        if (animator != null) animator.cancel();
        animMove = m;
        animPos = 0;
        selection.clear();
        inputEnabled = false;
        int steps = m.path.length - 1;
        animator = ValueAnimator.ofFloat(0, steps);
        animator.setDuration(Math.min(220 + 260L * steps, 1400));
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override public void onAnimationUpdate(ValueAnimator a) {
                animPos = (Float) a.getAnimatedValue();
                invalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            boolean cancelled;
            @Override public void onAnimationCancel(Animator a) { cancelled = true; }
            @Override public void onAnimationEnd(Animator a) {
                animMove = null;
                animator = null;
                if (!cancelled) done.run();
            }
        });
        animator.start();
    }

    public void cancelAnimation() {
        if (animator != null) animator.cancel();
        animMove = null;
    }

    // ---------- desenho ----------

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec), h = MeasureSpec.getSize(hSpec);
        int s = Math.min(w, h);
        if (MeasureSpec.getMode(wSpec) == MeasureSpec.UNSPECIFIED) s = h;
        if (MeasureSpec.getMode(hSpec) == MeasureSpec.UNSPECIFIED) s = w;
        setMeasuredDimension(s, s);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float size = Math.min(getWidth(), getHeight());
        float border = size * 0.035f;
        cell = (size - 2 * border) / N;
        left = (getWidth() - cell * N) / 2f;
        top = (getHeight() - cell * N) / 2f;

        // moldura de madeira
        paint.setShader(null);
        paint.setColor(0xFF5D3A1A);
        rect.set(left - border, top - border, left + cell * N + border, top + cell * N + border);
        canvas.drawRoundRect(rect, border, border, paint);

        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                paint.setColor(Board.isDark(r, c) ? 0xFF7A4B2A : 0xFFF0D9B5);
                canvas.drawRect(x(c), y(r), x(c) + cell, y(r) + cell, paint);
            }

        if (lastMove != null && animMove == null) {
            paint.setColor(0x55FFD54F);
            for (int sq : lastMove.path) drawCell(canvas, sq);
        }

        List<Board.Move> candidates = candidates();
        if (inputEnabled && animMove == null) {
            if (selection.isEmpty()) {
                // marca as peças que podem jogar
                stroke.setColor(0xCCFFD54F);
                stroke.setStrokeWidth(cell * 0.06f);
                List<Integer> seen = new ArrayList<>();
                for (Board.Move m : legal)
                    if (!seen.contains(m.from())) {
                        seen.add(m.from());
                        canvas.drawCircle(cx(m.from()), cy(m.from()), cell * 0.44f, stroke);
                    }
            } else {
                paint.setColor(0x6642A5F5);
                for (int sq : selection) drawCell(canvas, sq);
                int k = selection.size();
                for (Board.Move m : candidates) {
                    paint.setColor(0xCC42A5F5);
                    canvas.drawCircle(cx(m.path[k]), cy(m.path[k]), cell * 0.16f, paint);
                    if (m.path.length > k + 1) {
                        paint.setColor(0x6642A5F5);
                        canvas.drawCircle(cx(m.to()), cy(m.to()), cell * 0.12f, paint);
                    }
                    paint.setColor(0x88E53935);
                    for (int i = k - 1; i < m.captured.length; i++)
                        if (i >= 0) canvas.drawCircle(cx(m.captured[i]), cy(m.captured[i]), cell * 0.1f, paint);
                }
            }
        }

        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                int v = board.cells[r][c];
                if (v == 0) continue;
                int sq = r * N + c;
                if (animMove != null) {
                    if (sq == animMove.from()) continue;
                    int idx = indexOf(animMove.captured, sq);
                    if (idx >= 0 && animPos >= idx + 1) continue; // já foi capturada
                    if (idx >= 0 && animPos > idx + 0.5f) {
                        drawPiece(canvas, x(c) + cell / 2, y(r) + cell / 2, v, 1f - (animPos - idx - 0.5f) * 2);
                        continue;
                    }
                }
                drawPiece(canvas, x(c) + cell / 2, y(r) + cell / 2, v, 1f);
            }

        if (animMove != null) {
            int i = Math.min((int) animPos, animMove.path.length - 2);
            float t = animPos - i;
            int a = animMove.path[i], b = animMove.path[i + 1];
            float px = cx(a) + (cx(b) - cx(a)) * t;
            float py = cy(a) + (cy(b) - cy(a)) * t;
            float lift = (float) Math.sin(Math.PI * t) * cell * 0.12f;
            int fr = animMove.from() / N, fc = animMove.from() % N;
            drawPiece(canvas, px, py - lift, board.cells[fr][fc], 1f);
        }

        if (cursorVisible) {
            stroke.setColor(Color.WHITE);
            stroke.setStrokeWidth(cell * 0.07f);
            int r = cursor / N, c = cursor % N;
            canvas.drawRect(x(c) + cell * 0.04f, y(r) + cell * 0.04f,
                    x(c) + cell * 0.96f, y(r) + cell * 0.96f, stroke);
        }
    }

    private void drawCell(Canvas canvas, int sq) {
        int r = sq / N, c = sq % N;
        canvas.drawRect(x(c), y(r), x(c) + cell, y(r) + cell, paint);
    }

    private void drawPiece(Canvas canvas, float px, float py, int v, float alpha) {
        boolean white = v > 0;
        float rad = cell * 0.38f;
        int a = (int) (255 * Math.max(0, Math.min(1, alpha)));

        paint.setShader(null);
        paint.setColor(Color.argb(a * 90 / 255, 0, 0, 0));
        canvas.drawCircle(px + rad * 0.08f, py + rad * 0.14f, rad, paint);

        int light = white ? 0xFFFFFBF0 : 0xFF5A5A5A;
        int dark = white ? 0xFFD8CBB0 : 0xFF151515;
        paint.setShader(new RadialGradient(px - rad * 0.3f, py - rad * 0.3f, rad * 1.3f,
                light, dark, Shader.TileMode.CLAMP));
        paint.setAlpha(a);
        canvas.drawCircle(px, py, rad, paint);
        paint.setShader(null);

        stroke.setStrokeWidth(rad * 0.08f);
        stroke.setColor(white ? 0xFFB8A888 : 0xFF000000);
        stroke.setAlpha(a);
        canvas.drawCircle(px, py, rad * 0.72f, stroke);
        canvas.drawCircle(px, py, rad, stroke);

        if (Math.abs(v) == 2) {
            float w = rad * 0.55f, h = rad * 0.4f;
            crown.reset();
            crown.moveTo(px - w, py + h * 0.6f);
            crown.lineTo(px - w, py - h * 0.5f);
            crown.lineTo(px - w * 0.5f, py);
            crown.lineTo(px, py - h);
            crown.lineTo(px + w * 0.5f, py);
            crown.lineTo(px + w, py - h * 0.5f);
            crown.lineTo(px + w, py + h * 0.6f);
            crown.close();
            paint.setColor(0xFFFFC107);
            paint.setAlpha(a);
            canvas.drawPath(crown, paint);
        }
    }

    private float x(int c) { return left + c * cell; }
    private float y(int r) { return top + r * cell; }
    private float cx(int sq) { return x(sq % N) + cell / 2; }
    private float cy(int sq) { return y(sq / N) + cell / 2; }

    private static int indexOf(int[] a, int v) {
        for (int i = 0; i < a.length; i++) if (a[i] == v) return i;
        return -1;
    }

    // ---------- entrada ----------

    private List<Board.Move> candidates() {
        List<Board.Move> out = new ArrayList<>();
        if (selection.isEmpty()) return out;
        for (Board.Move m : legal) {
            if (m.path.length <= selection.size()) continue;
            boolean ok = true;
            for (int i = 0; i < selection.size() && ok; i++) ok = m.path[i] == selection.get(i);
            if (ok) out.add(m);
        }
        return out;
    }

    private boolean hasMoveFrom(int sq) {
        for (Board.Move m : legal) if (m.from() == sq) return true;
        return false;
    }

    private void tap(int sq) {
        if (!inputEnabled || animMove != null) return;
        if (selection.size() == 1 && selection.get(0) == sq) {
            selection.clear();
            invalidate();
            return;
        }
        if (selection.isEmpty() || (selection.size() == 1 && hasMoveFrom(sq))) {
            selection.clear();
            if (hasMoveFrom(sq)) selection.add(sq);
            invalidate();
            return;
        }
        List<Board.Move> cand = candidates();
        int k = selection.size();
        List<Board.Move> next = new ArrayList<>();
        for (Board.Move m : cand) if (m.path[k] == sq) next.add(m);
        if (!next.isEmpty()) {
            selection.add(sq);
            for (Board.Move m : next)
                if (m.path.length == selection.size()) {
                    commit(m);
                    return;
                }
            invalidate();
            return;
        }
        // toque direto no destino final de uma captura múltipla
        Board.Move only = null;
        int found = 0;
        for (Board.Move m : cand)
            if (m.to() == sq) {
                only = m;
                found++;
            }
        if (found == 1) {
            commit(only);
            return;
        }
        if (selection.size() == 1) {
            selection.clear();
            invalidate();
        }
    }

    private void commit(Board.Move m) {
        selection.clear();
        inputEnabled = false;
        if (listener != null) listener.onHumanMove(m);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            cursorVisible = false;
            return true;
        }
        if (e.getAction() == MotionEvent.ACTION_UP) {
            int c = (int) Math.floor((e.getX() - left) / cell);
            int r = (int) Math.floor((e.getY() - top) / cell);
            if (Board.inside(r, c)) {
                cursor = r * N + c;
                tap(cursor);
            }
            performClick();
            return true;
        }
        return super.onTouchEvent(e);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        int r = cursor / N, c = cursor % N;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_UP: r = Math.max(0, r - 1); break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (r == N - 1) return super.onKeyDown(keyCode, event); // sai para os botões
                r++;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (c == 0) return super.onKeyDown(keyCode, event);
                c--;
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (c == N - 1) return super.onKeyDown(keyCode, event);
                c++;
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                cursorVisible = true;
                tap(cursor);
                invalidate();
                return true;
            default:
                return super.onKeyDown(keyCode, event);
        }
        cursor = r * N + c;
        cursorVisible = true;
        invalidate();
        return true;
    }

    @Override
    protected void onFocusChanged(boolean gained, int direction, android.graphics.Rect previous) {
        super.onFocusChanged(gained, direction, previous);
        if (!gained) cursorVisible = false;
        invalidate();
    }
}
