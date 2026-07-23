package dev.primer.exception;

public class PaperNotFoundException extends RuntimeException {
    public PaperNotFoundException(String message) {
        super(message);
    }
}
