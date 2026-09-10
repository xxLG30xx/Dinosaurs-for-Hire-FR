package fr.teamdeltaisland.patcher;

import java.util.List;

public enum Cheat {
    INFINITE_LIVES("Vies infinies (J1 & J2)", "Le nombre de vies ne diminue plus.", List.of(
            p(0x00E75C, 0x3B40, 0x6002), p(0x00E7F2, 0x3B40, 0x6002))),
    INVINCIBILITY("Invincibilité (J1 & J2)", "Vous ne subissez aucun dégât.", List.of(
            p(0x007992, 0x670C, 0x4E75))),
    MAX_WEAPONS("Armes max (J1 & J2)", "Triple tir dès le début et conservé après une perte de vie.", List.of(
            p(0x0089B2, 0x0000, 0x0002), p(0x0089B8, 0x0000, 0x0002),
            p(0x00E74A, 0x0000, 0x0002), p(0x00E7E0, 0x0000, 0x0002))),
    INFINITE_BOMBS("Bombes infinies (J1 & J2)", "Stock de bombes illimité.", List.of(
            p(0x012E36, 0x532D, 0x6002), p(0x012E20, 0x532D, 0x6002))),
    LEVEL_SELECT("Sélection de niveau", "START → A → START = menu / START → START = pause normale", List.of(
            p(0x008FD8, 0x5240, 0x5C40), p(0x008FEE, 0x7205, 0x7200), p(0x00901C, 0x5846, 0x7C14)));

    public final String title;
    public final String description;
    final List<Edit> edits;

    Cheat(String title, String description, List<Edit> edits) {
        this.title = title; this.description = description; this.edits = edits;
    }

    private static Edit p(int offset, int expected, int replacement) {
        return new Edit(offset, new byte[]{(byte)(expected >>> 8), (byte)expected},
                new byte[]{(byte)(replacement >>> 8), (byte)replacement});
    }

    record Edit(int offset, byte[] expected, byte[] replacement) { }
}
