package com.ilha.notch;

import android.graphics.Bitmap;
import android.graphics.Color;

final class Colors {
    private Colors() {}

    /** The most colourful pixel group of a picture, made bright enough for a black island. */
    static int accentOf(Bitmap bmp) {
        try {
            Bitmap s = Bitmap.createScaledBitmap(bmp, 24, 24, true);
            float[] hsv = new float[3];
            float bestScore = -1;
            int best = 0xFFFF5A6E;
            for (int y = 0; y < 24; y += 2) {
                for (int x = 0; x < 24; x += 2) {
                    int c = s.getPixel(x, y);
                    Color.colorToHSV(c, hsv);
                    float score = hsv[1] * (0.35f + hsv[2]);
                    if (score > bestScore) {
                        bestScore = score;
                        best = c;
                    }
                }
            }
            if (bestScore < 0.15f) return 0xFFF2E6B8; // greyscale art: warm cream
            return forDark(best);
        } catch (Exception e) {
            return 0xFFFF5A6E;
        }
    }

    /** Keeps the hue, lifts the brightness so it reads on black. */
    static int forDark(int c) {
        float[] hsv = new float[3];
        Color.colorToHSV(c, hsv);
        hsv[1] = Math.min(hsv[1], 0.6f);
        hsv[2] = Math.max(hsv[2], 0.9f);
        return Color.HSVToColor(hsv);
    }

    static int alpha(int c, float a) {
        return (c & 0x00FFFFFF) | (Math.round(a * 255) << 24);
    }
}
