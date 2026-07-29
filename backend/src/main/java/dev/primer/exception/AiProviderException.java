package dev.primer.exception;

/**
 * Raised when the configured AI provider fails to fulfil a request (network failure, timeout,
 * upstream error, or an empty/unusable response). Mapped to {@code 502 Bad Gateway}.
 */
public class AiProviderException extends RuntimeException {

    public AiProviderException(String message) {
        super(message);
    }

    public AiProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
