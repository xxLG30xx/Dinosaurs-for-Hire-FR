package fr.teamdeltaisland.patcher;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.net.URL;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.EnumSet;

public final class PatchVerificationTest {
    public static void main(String[] args) throws Exception {
        if (args.length != 2) throw new AssertionError("Usage: test <ROM USA> <ROM FR>");
        Path usa = Paths.get(args[0]); byte[] reference = Files.readAllBytes(Paths.get(args[1]));
        byte[] generated = new PatchEngine().create(usa, EnumSet.noneOf(Cheat.class));
        check(Arrays.equals(reference, generated), "La ROM sans cheat diffère de la référence finale");
        check(PatchEngine.CLEAN_SHA1.equals(RomValidator.digest("SHA-1", generated)), "SHA-1 final incorrect");
        int header = ((generated[0x18e]&255)<<8)|(generated[0x18f]&255);
        check(header == MegaDriveChecksum.calculate(generated), "Checksum Mega Drive incorrect");
        for (Cheat cheat : Cheat.values()) {
            byte[] cheated = new PatchEngine().create(usa, EnumSet.of(cheat));
            check(!Arrays.equals(generated, cheated), "Le cheat n'a rien modifié : " + cheat);
            for (Cheat.Edit edit : cheat.edits) {
                check(Arrays.equals(edit.replacement(), Arrays.copyOfRange(cheated, edit.offset(),
                                edit.offset() + edit.replacement().length)),
                        "Octets de remplacement incorrects : " + cheat + " à 0x" + Integer.toHexString(edit.offset()));
            }
            for (int offset = 0; offset < generated.length; offset++) {
                if (offset == 0x18e || offset == 0x18f || isCheatOffset(cheat, offset)) continue;
                check(generated[offset] == cheated[offset],
                        "Le cheat " + cheat + " a modifié un offset inattendu 0x" + Integer.toHexString(offset));
            }
            check((((cheated[0x18e]&255)<<8)|(cheated[0x18f]&255)) == MegaDriveChecksum.calculate(cheated), "Checksum incorrect : " + cheat);
        }
        byte[] corrupt = generated.clone(); corrupt[0xE75C] ^= 1;
        try { CheatPatcher.apply(corrupt, EnumSet.of(Cheat.INFINITE_LIVES)); throw new AssertionError("Les octets inattendus auraient dû être refusés"); }
        catch (PatchException expected) { /* attendu */ }
        URL imageResource = PatchVerificationTest.class.getResource("/Image.png");
        check(imageResource != null, "Image.png n'est pas embarqué dans les ressources");
        BufferedImage artwork = ImageIO.read(imageResource);
        check(artwork != null && artwork.getWidth() == 1584 && artwork.getHeight() == 993,
                "Les dimensions de l'interface officielle sont incorrectes");
        check(PatchVerificationTest.class.getResource("/patch/translation.dfhp.b64") != null,
                "translation.dfhp.b64 n'est pas embarqué dans les ressources");
        PatcherApplication.verifyOverlayComponents();
        System.out.println("OK - ROM générée identique, SHA-1=" + PatchEngine.CLEAN_SHA1 + ", cheats et checksum vérifiés");
    }
    private static boolean isCheatOffset(Cheat cheat, int offset) {
        return cheat.edits.stream().anyMatch(edit -> offset >= edit.offset()
                && offset < edit.offset() + edit.replacement().length);
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
