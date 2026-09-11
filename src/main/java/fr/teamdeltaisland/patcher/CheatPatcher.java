package fr.teamdeltaisland.patcher;

import java.util.Arrays;
import java.util.Set;

public final class CheatPatcher {
    private CheatPatcher() { }

    public static void apply(byte[] rom, Set<Cheat> cheats) throws PatchException {
        for (Cheat cheat : cheats) for (Cheat.Edit edit : cheat.edits) {
            int end = edit.offset() + edit.expected().length;
            if (edit.offset() < 0 || end > rom.length)
                throw new PatchException(cheat.title + " : offset hors limites 0x" + Integer.toHexString(edit.offset()).toUpperCase() + ".");
            byte[] actual = Arrays.copyOfRange(rom, edit.offset(), end);
            if (!Arrays.equals(actual, edit.expected()))
                throw new PatchException(cheat.title + " : octets inattendus à l'offset 0x" + Integer.toHexString(edit.offset()).toUpperCase() + ". Patch annulé.");
            System.arraycopy(edit.replacement(), 0, rom, edit.offset(), edit.replacement().length);
        }
    }
}
