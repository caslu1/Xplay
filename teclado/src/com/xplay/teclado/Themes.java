package com.xplay.teclado;

/** The keyboard color themes. Index is what Store keeps. */
final class Themes {
    static final class Theme {
        final String name;
        final int bg, key, keyText, special, pressed, accent, hint;

        Theme(String name, int bg, int key, int keyText, int special, int pressed, int accent, int hint) {
            this.name = name;
            this.bg = bg;
            this.key = key;
            this.keyText = keyText;
            this.special = special;
            this.pressed = pressed;
            this.accent = accent;
            this.hint = hint;
        }
    }

    static final Theme[] ALL = {
        new Theme("Escuro", 0xFF1B1B1D, 0xFF2E2E33, 0xFFFFFFFF, 0xFF242428, 0xFF4A4A52, 0xFFE84A6A, 0xFF9A9AA2),
        new Theme("Claro", 0xFFE6E8EC, 0xFFFFFFFF, 0xFF1D1D1F, 0xFFCDD1D8, 0xFFB8BDC6, 0xFF1A73E8, 0xFF7A7F88),
        new Theme("Oceano", 0xFF0A2238, 0xFF143A60, 0xFFE6F4FF, 0xFF0F2E4E, 0xFF1E5488, 0xFF29B6F6, 0xFF7FA7C9),
        new Theme("Neon Roxo", 0xFF12061C, 0xFF2A0F40, 0xFFF6E8FF, 0xFF1D0A2E, 0xFF45196A, 0xFFD500F9, 0xFFA77FC4),
        new Theme("Floresta", 0xFF0F221A, 0xFF1C3A2B, 0xFFE8F5E9, 0xFF153024, 0xFF2B5A41, 0xFF66BB6A, 0xFF86A894),
        new Theme("Pôr do Sol", 0xFF2A130F, 0xFF4A2219, 0xFFFFF3E0, 0xFF3A1B14, 0xFF6E3324, 0xFFFF7043, 0xFFC79A86),
    };

    static Theme get(int i) {
        return ALL[i >= 0 && i < ALL.length ? i : 0];
    }

    private Themes() {}
}
