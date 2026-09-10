package fr.teamdeltaisland.patcher;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Base64;

public final class TranslationPatch {
    private static final byte[] MAGIC = {'D', 'F', 'H', '2'};
    private static final String RESOURCE = "/patch/translation.dfhp.b64";

    private TranslationPatch() { }

    public static byte[] apply(byte[] original) throws PatchException {
        try (InputStream raw = TranslationPatch.class.getResourceAsStream(RESOURCE)) {
            if (raw == null) throw new PatchException("Ressource de traduction absente du patcher.");
            try (InputStream decoded = Base64.getMimeDecoder().wrap(raw);
                 DataInputStream in = new DataInputStream(new BufferedInputStream(decoded))) {
                byte[] magic = in.readNBytes(4);
                if (!Arrays.equals(magic, MAGIC)) throw new PatchException("Ressource de traduction invalide.");
                int targetSize = in.readInt();
                if (targetSize < original.length) throw new PatchException("Taille cible de traduction invalide.");
                byte[] result = Arrays.copyOf(original, targetSize);
                Arrays.fill(result, original.length, targetSize, (byte) 0xff);
                int records = in.readInt();
                for (int n = 0; n < records; n++) {
                    int offset = in.readInt();
                    int length = in.readInt();
                    if (offset < 0 || length < 1 || offset > result.length - length)
                        throw new PatchException("Bloc de traduction hors limites (bloc " + (n + 1) + ").");
                    byte[] expected = in.readNBytes(length);
                    if (expected.length != length) throw new EOFException();
                    if (!Arrays.equals(expected, Arrays.copyOfRange(result, offset, offset + length)))
                        throw new PatchException("Octets source inattendus à l'offset 0x" + Integer.toHexString(offset).toUpperCase() + ". Patch annulé.");
                    in.readFully(result, offset, length);
                }
                if (in.read() != -1) throw new PatchException("Données inattendues après le patch de traduction.");
                return result;
            }
        } catch (EOFException e) {
            throw new PatchException("Ressource de traduction tronquée.", e);
        } catch (IOException e) {
            throw new PatchException("Impossible de lire la ressource de traduction.", e);
        }
    }
}
