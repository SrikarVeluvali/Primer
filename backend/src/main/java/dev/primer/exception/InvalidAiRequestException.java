package dev.primer.exception;

/**
 * Raised when an AI request is malformed: neither text nor image supplied, or an uploaded image
 * fails validation (size/content type). Mapped to {@code 400 Bad Request}.
 */
public class InvalidAiRequestException extends RuntimeException {

    public InvalidAiRequestException(String message) {
        super(message);
    }
}
