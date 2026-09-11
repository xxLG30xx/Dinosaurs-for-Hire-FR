package fr.teamdeltaisland.patcher;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.imageio.ImageIO;

public final class PatchVerificationTest {
    private static final int FINAL_SIZE = 2_133_984;
    private static final int INTRO_EXIT_OFFSET = 0x20013E;
    private static final byte[] JMP_210 = {(byte) 0x4E, (byte) 0xF9, 0, 0, 2, 0x10};
    private static final byte[] JMP_200 = {(byte) 0x4E, (byte) 0xF9, 0, 0, 2, 0};

    public static void main(String[] args) throws Exception {
        if (args.length != 3) throw new AssertionError("Usage: test <ROM USA> <ROM FR> <JAR>");
        Path usa = Paths.get(args[0]);
        Path goldenPath = Paths.get(args[1]);
        Path jarPath = Paths.get(args[2]);
        byte[] original = RomValidator.readAndValidate(usa);
        byte[] golden = Files.readAllBytes(goldenPath);
        check(original.length == RomValidator.ORIGINAL_SIZE, "Taille de la ROM originale incorrecte");
        check(golden.length == FINAL_SIZE, "Taille du Golden Master incorrecte");
        check(PatchEngine.CLEAN_SHA1.equals(RomValidator.digest("SHA-1", golden)), "SHA-1 Golden Master incorrect");
        check(PatchEngine.CLEAN_SHA256.equals(RomValidator.digest("SHA-256", golden)), "SHA-256 Golden Master incorrect");

        byte[] generated = new PatchEngine().create(usa, EnumSet.noneOf(Cheat.class));
        check(generated.length == FINAL_SIZE, "Taille de la ROM produite incorrecte");
        check(Arrays.equals(golden, generated), "La ROM sans cheat diffère du Golden Master");
        check(PatchEngine.CLEAN_SHA1.equals(RomValidator.digest("SHA-1", generated)), "SHA-1 final incorrect");
        check(PatchEngine.CLEAN_SHA256.equals(RomValidator.digest("SHA-256", generated)), "SHA-256 final incorrect");
        checkChecksum(generated, "Golden Master");
        checkBytes(generated, INTRO_EXIT_OFFSET, JMP_210, "JMP $00000210 absent");
        check(!matches(generated, INTRO_EXIT_OFFSET, JMP_200), "Régression JMP $00000200");
        String region = new String(Arrays.copyOfRange(generated, 0x1F0, 0x200), StandardCharsets.US_ASCII).trim();
        check("JUE".equals(region), "Champ région incorrect : " + region);

        for (Cheat cheat : Cheat.values()) verifyCheat(usa, generated, cheat);
        byte[] corrupt = generated.clone();
        corrupt[0xE75C] ^= 1;
        try {
            CheatPatcher.apply(corrupt, EnumSet.of(Cheat.INFINITE_LIVES));
            throw new AssertionError("Les octets inattendus auraient dû être refusés");
        } catch (PatchException expected) { /* attendu */ }

        verifyJar(jarPath);
        Path verificationOutput = jarPath.toAbsolutePath().getParent().resolve("Dinosaurs_for_Hire_FR_verified.bin");
        Files.write(verificationOutput, generated);
        System.out.println("OK - ROM byte-for-byte, SHA-1/SHA-256, JMP $00000210, région JUE, 5 cheats, checksum et JAR Java 8 vérifiés");
    }

    private static void verifyCheat(Path usa, byte[] clean, Cheat cheat) throws Exception {
        for (Cheat.Edit edit : cheat.edits) checkBytes(clean, edit.offset(), edit.expected(), "Source cheat incorrecte : " + cheat);
        byte[] cheated = new PatchEngine().create(usa, EnumSet.of(cheat));
        Set<Integer> allowed = new HashSet<Integer>();
        allowed.add(0x18E); allowed.add(0x18F);
        for (Cheat.Edit edit : cheat.edits) {
            checkBytes(cheated, edit.offset(), edit.replacement(), "Cheat non appliqué : " + cheat);
            for (int i = 0; i < edit.replacement().length; i++) allowed.add(edit.offset() + i);
        }
        for (int i = 0; i < clean.length; i++)
            if (clean[i] != cheated[i]) check(allowed.contains(i), "Modification inattendue du cheat " + cheat + " à 0x" + Integer.toHexString(i));
        checkChecksum(cheated, cheat.toString());
    }

    private static void verifyJar(Path path) throws Exception {
        try (JarFile jar = new JarFile(path.toFile())) {
            Attributes attributes = jar.getManifest().getMainAttributes();
            check("fr.teamdeltaisland.patcher.PatcherApplication".equals(attributes.getValue(Attributes.Name.MAIN_CLASS)), "Main-Class incorrect");
            check(jar.getEntry("patch/translation.dfhp.b64") != null, "Patch différentiel absent du JAR");
            JarEntry imageEntry = jar.getJarEntry("Image.png");
            check(imageEntry != null, "Image.png absent du JAR");
            try (InputStream imageStream = jar.getInputStream(imageEntry)) {
                BufferedImage image = ImageIO.read(imageStream);
                check(image != null && image.getWidth() == 1584 && image.getHeight() == 993, "Dimensions Image.png incorrectes");
            }
            int classes = 0;
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                classes++;
                try (InputStream in = jar.getInputStream(entry)) {
                    byte[] header = new byte[8];
                    int position = 0;
                    while (position < header.length) {
                        int read = in.read(header, position, header.length - position);
                        if (read < 0) break;
                        position += read;
                    }
                    check(position == 8, "Classe tronquée : " + entry.getName());
                    int major = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
                    check(major == 52, "Classe non Java 8 : " + entry.getName() + " (major " + major + ")");
                }
            }
            check(classes > 0, "Aucune classe dans le JAR");
        }
    }

    private static void checkChecksum(byte[] rom, String label) throws Exception {
        int header = ((rom[0x18E] & 255) << 8) | (rom[0x18F] & 255);
        check(header == MegaDriveChecksum.calculate(rom), "Checksum Mega Drive incorrect : " + label);
    }

    private static void checkBytes(byte[] bytes, int offset, byte[] expected, String message) {
        check(matches(bytes, offset, expected), message);
    }

    private static boolean matches(byte[] bytes, int offset, byte[] expected) {
        if (offset < 0 || offset + expected.length > bytes.length) return false;
        for (int i = 0; i < expected.length; i++) if (bytes[offset + i] != expected[i]) return false;
        return true;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
