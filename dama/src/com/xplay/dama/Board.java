package com.xplay.dama;

import java.util.ArrayList;
import java.util.List;

/**
 * Damas brasileiras: tabuleiro 8x8, 12 peças por lado, captura obrigatória,
 * pedra captura para trás, dama voa e a lei da maioria (deve-se capturar o
 * maior número de peças possível).
 *
 * Valores das casas: 0 vazia, 1 pedra branca, 2 dama branca,
 * -1 pedra preta, -2 dama preta. As brancas começam embaixo e sobem.
 */
public class Board {
    public static final int N = 8;
    public static final int WHITE = 1, BLACK = -1;

    public final int[][] cells = new int[N][N];
    public int turn = WHITE;
    /** Lances seguidos só com damas e sem captura (empate em 20). */
    public int quietKingMoves = 0;

    private static final int[][] DIRS = {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}};

    public static class Move {
        /** Casas visitadas, codificadas como r * 8 + c; a primeira é a origem. */
        public final int[] path;
        /** Casas das peças capturadas, na ordem. */
        public final int[] captured;

        Move(int[] path, int[] captured) {
            this.path = path;
            this.captured = captured;
        }

        public int from() { return path[0]; }
        public int to() { return path[path.length - 1]; }
    }

    public Board() {}

    public static Board initial() {
        Board b = new Board();
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (isDark(r, c)) {
                    if (r < 3) b.cells[r][c] = BLACK;
                    else if (r > 4) b.cells[r][c] = WHITE;
                }
        return b;
    }

    public Board copy() {
        Board b = new Board();
        for (int r = 0; r < N; r++) System.arraycopy(cells[r], 0, b.cells[r], 0, N);
        b.turn = turn;
        b.quietKingMoves = quietKingMoves;
        return b;
    }

    public static boolean isDark(int r, int c) { return ((r + c) & 1) == 1; }

    static boolean inside(int r, int c) { return r >= 0 && r < N && c >= 0 && c < N; }

    public int count(int side) {
        int n = 0;
        for (int[] row : cells) for (int v : row) if (v * side > 0) n++;
        return n;
    }

    /** Todos os lances legais do lado da vez (já filtrados pela lei da maioria). */
    public List<Move> legalMoves() {
        List<Move> captures = new ArrayList<>();
        int best = 1;
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (cells[r][c] * turn > 0) {
                    List<Move> found = new ArrayList<>();
                    int[] path = new int[13];
                    int[] caps = new int[12];
                    path[0] = r * N + c;
                    boolean[] taken = new boolean[N * N];
                    int piece = cells[r][c];
                    cells[r][c] = 0; // a peça sai da origem durante a busca
                    searchCaptures(r, c, Math.abs(piece) == 2, path, 1, caps, 0, taken, found);
                    cells[r][c] = piece;
                    for (Move m : found) {
                        if (m.captured.length > best) {
                            best = m.captured.length;
                            captures.clear();
                        }
                        if (m.captured.length == best) captures.add(m);
                    }
                }
        if (!captures.isEmpty()) return captures;

        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                int v = cells[r][c];
                if (v * turn <= 0) continue;
                boolean king = Math.abs(v) == 2;
                for (int[] d : DIRS) {
                    if (!king && d[0] != -turn) continue; // pedra só anda para frente
                    int nr = r + d[0], nc = c + d[1];
                    while (inside(nr, nc) && cells[nr][nc] == 0) {
                        moves.add(new Move(new int[]{r * N + c, nr * N + nc}, new int[0]));
                        if (!king) break;
                        nr += d[0];
                        nc += d[1];
                    }
                }
            }
        return moves;
    }

    private void searchCaptures(int r, int c, boolean king, int[] path, int plen,
                                int[] caps, int clen, boolean[] taken, List<Move> out) {
        boolean extended = false;
        for (int[] d : DIRS) {
            int nr = r + d[0], nc = c + d[1];
            if (king) {
                while (inside(nr, nc) && cells[nr][nc] == 0) {
                    nr += d[0];
                    nc += d[1];
                }
            }
            if (!inside(nr, nc)) continue;
            int v = cells[nr][nc];
            if (v * turn >= 0 || taken[nr * N + nc]) continue; // precisa ser inimiga ainda não capturada
            int lr = nr + d[0], lc = nc + d[1];
            while (inside(lr, lc) && cells[lr][lc] == 0) {
                taken[nr * N + nc] = true;
                caps[clen] = nr * N + nc;
                path[plen] = lr * N + lc;
                searchCaptures(lr, lc, king, path, plen + 1, caps, clen + 1, taken, out);
                taken[nr * N + nc] = false;
                extended = true;
                if (!king) break;
                lr += d[0];
                lc += d[1];
            }
        }
        if (!extended && clen > 0) {
            int[] p = new int[plen];
            int[] k = new int[clen];
            System.arraycopy(path, 0, p, 0, plen);
            System.arraycopy(caps, 0, k, 0, clen);
            out.add(new Move(p, k));
        }
    }

    public void apply(Move m) {
        int fr = m.from() / N, fc = m.from() % N;
        int tr = m.to() / N, tc = m.to() % N;
        int piece = cells[fr][fc];
        cells[fr][fc] = 0;
        for (int sq : m.captured) cells[sq / N][sq % N] = 0;
        boolean wasKing = Math.abs(piece) == 2;
        if (!wasKing && ((piece > 0 && tr == 0) || (piece < 0 && tr == N - 1))) piece *= 2;
        cells[tr][tc] = piece;
        if (wasKing && m.captured.length == 0) quietKingMoves++;
        else quietKingMoves = 0;
        turn = -turn;
    }

    /** WHITE ou BLACK se alguém venceu, 0 se o jogo continua, 2 se empatou. */
    public int result() {
        if (legalMoves().isEmpty()) return -turn;
        if (quietKingMoves >= 20) return 2;
        return 0;
    }
}
