package com.xplay.dama;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Minimax com poda alfa-beta. A profundidade define a dificuldade. */
public class Ai {
    private static final int WIN = 100000;
    private final Random random = new Random();
    private final int depth;

    public Ai(int depth) {
        this.depth = depth;
    }

    public Board.Move choose(Board board) {
        List<Board.Move> moves = board.legalMoves();
        if (moves.isEmpty()) return null;
        Collections.shuffle(moves, random); // variedade entre lances de mesmo valor
        if (moves.size() == 1) return moves.get(0);
        Board.Move best = moves.get(0);
        int alpha = -WIN - 1;
        for (Board.Move m : moves) {
            Board next = board.copy();
            next.apply(m);
            int score = -search(next, depth - 1, -WIN - 1, -alpha);
            if (score > alpha) {
                alpha = score;
                best = m;
            }
        }
        return best;
    }

    private int search(Board b, int d, int alpha, int beta) {
        List<Board.Move> moves = b.legalMoves();
        if (moves.isEmpty()) return -WIN + (depth - d); // perder mais tarde é melhor
        if (b.quietKingMoves >= 20) return 0;
        // Não para no meio de uma sequência de capturas.
        if (d <= 0 && moves.get(0).captured.length == 0) return evaluate(b);
        if (d <= -6) return evaluate(b);
        for (Board.Move m : moves) {
            Board next = b.copy();
            next.apply(m);
            int score = -search(next, d - 1, -beta, -alpha);
            if (score > alpha) alpha = score;
            if (alpha >= beta) break;
        }
        return alpha;
    }

    /** Avaliação do ponto de vista de quem joga. */
    private int evaluate(Board b) {
        int score = 0;
        for (int r = 0; r < Board.N; r++)
            for (int c = 0; c < Board.N; c++) {
                int v = b.cells[r][c];
                if (v == 0) continue;
                int side = v > 0 ? Board.WHITE : Board.BLACK;
                int s;
                if (Math.abs(v) == 2) {
                    s = 300;
                } else {
                    int advance = side == Board.WHITE ? 7 - r : r;
                    s = 100 + advance * 4;
                    if (advance == 0) s += 6; // fileira de trás protege contra damas
                }
                if (c >= 2 && c <= 5 && r >= 2 && r <= 5) s += 5; // centro
                if (c == 0 || c == 7) s -= 3;
                score += s * side;
            }
        return score * b.turn + random.nextInt(3);
    }
}
