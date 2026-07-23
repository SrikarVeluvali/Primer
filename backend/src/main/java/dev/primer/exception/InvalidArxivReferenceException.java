package dev.primer.exception;

public class InvalidArxivReferenceException extends RuntimeException {
    public InvalidArxivReferenceException(String message) {
        super(message);
    }
}
