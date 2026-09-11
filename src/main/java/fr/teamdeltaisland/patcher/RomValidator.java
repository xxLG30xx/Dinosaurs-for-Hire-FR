package fr.teamdeltaisland.patcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RomValidator {
    public static final int ORIGINAL_SIZE = 1_048_576;
    public static final String MD5 = "e4c6cbf1a2ea36404fb69667cd080b4f";
    public static final String SHA1 = "d006efbf1d811e018271745925fe00ca6d93f24f";

    private RomValidator() { }

    public static byte[] readAndValidate(Path path) throws IOException, PatchException {
        byte[] rom = Files.readAllBytes(path);
        if (rom.length != ORIGINAL_SIZE) {
            throw new PatchException("ROM incompatible : taille attendue 1 048 576 octets, taille trouvée " + rom.length + ".");
        }
        if (!MD5.equals(digest("MD5", rom)) || !SHA1.equals(digest("SHA-1", rom))) {
            throw new PatchException("ROM incompatible : les empreintes MD5/SHA-1 ne correspondent pas à la ROM USA officielle.");
        }
        return rom;
    }

    public static String digest(String algorithm, byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance(algorithm).digest(bytes);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte value : digest) hex.append(String.format("%02x", value & 0xff));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithme indisponible : " + algorithm, e);
        }
    }
}
