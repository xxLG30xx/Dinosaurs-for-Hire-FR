package fr.teamdeltaisland.patcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Set;

public final class PatchEngine {
    public static final String OUTPUT_NAME = "Dinosaurs_for_Hire_FR.bin";
    public static final String CLEAN_SHA1 = "1af0132f63e16f69d7a6366df27610fad7339ba4";

    public byte[] create(Path source, Set<Cheat> cheats) throws IOException, PatchException {
        byte[] result = TranslationPatch.apply(RomValidator.readAndValidate(source));
        CheatPatcher.apply(result, cheats);
        MegaDriveChecksum.update(result);
        if (cheats.isEmpty() && !CLEAN_SHA1.equals(RomValidator.digest("SHA-1", result)))
            throw new PatchException("La vérification finale SHA-1 de la traduction a échoué. Aucun fichier n'a été créé.");
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
