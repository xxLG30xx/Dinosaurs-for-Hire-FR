package fr.teamdeltaisland.patcher;

public final class MegaDriveChecksum {
    private static final int DATA_START = 0x200;
    private static final int CHECKSUM_OFFSET = 0x18E;
    private MegaDriveChecksum() { }

    public static int calculate(byte[] rom) throws PatchException {
        if (rom.length < DATA_START) throw new PatchException("ROM trop petite pour contenir un en-tête Mega Drive.");
        int sum = 0;
        for (int i = DATA_START; i + 1 < rom.length; i += 2)
            sum = (sum + ((rom[i] & 0xff) << 8) + (rom[i + 1] & 0xff)) & 0xffff;
        return sum;
    }

    public static void update(byte[] rom) throws PatchException {
        int checksum = calculate(rom);
        rom[CHECKSUM_OFFSET] = (byte)(checksum >>> 8);
        rom[CHECKSUM_OFFSET + 1] = (byte)checksum;
    }
}
