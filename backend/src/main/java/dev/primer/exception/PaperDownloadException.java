package dev.primer.exception;

public class PaperDownloadException extends RuntimeException {
    public PaperDownloadException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaperDownloadException(String message) {
        super(message);
    }
}
