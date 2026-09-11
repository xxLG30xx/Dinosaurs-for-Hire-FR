package fr.teamdeltaisland.patcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

public final class PatchEngine {
    public static final String OUTPUT_NAME = "Dinosaurs_for_Hire_FR.bin";
    public static final String CLEAN_SHA1 = "747737fd4b227b90ee4eaed7c9076bf848d92985";
    public static final String CLEAN_SHA256 = "25f3da3063b1e3b17829c6300e6b8135259a73c86415c29cf8929a1660234c45";

    public byte[] create(Path source, Set<Cheat> cheats) throws IOException, PatchException {
        byte[] result = TranslationPatch.apply(RomValidator.readAndValidate(source));
        CheatPatcher.apply(result, cheats);
        MegaDriveChecksum.update(result);
        if (cheats.isEmpty() && (!CLEAN_SHA1.equals(RomValidator.digest("SHA-1", result))
                || !CLEAN_SHA256.equals(RomValidator.digest("SHA-256", result))))
            throw new PatchException("La vérification finale SHA-1/SHA-256 de la traduction a échoué. Aucun fichier n'a été créé.");
        return result;
    }

    public void createFile(Path source, Path destination, Set<Cheat> cheats) throws IOException, PatchException {
        byte[] result = create(source, cheats);
        Path parent = destination.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".dinosaurs-patcher-", ".tmp");
        try {
            Files.write(temporary, result);
            Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } finally { Files.deleteIfExists(temporary); }
    }
}
