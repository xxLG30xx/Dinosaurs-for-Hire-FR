package fr.teamdeltaisland.patcher;

public final class PatchException extends Exception {
    public PatchException(String message) { super(message); }
    public PatchException(String message, Throwable cause) { super(message, cause); }
}
