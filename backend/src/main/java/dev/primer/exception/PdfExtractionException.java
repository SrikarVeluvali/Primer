package dev.primer.exception;

public class PdfExtractionException extends RuntimeException {
    public PdfExtractionException(String message, Throwable cause) {
        super(message, cause);
    }

    public PdfExtractionException(String message) {
        super(message);
    }
}
