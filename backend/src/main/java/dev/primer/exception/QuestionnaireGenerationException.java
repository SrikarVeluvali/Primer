package dev.primer.exception;

public class QuestionnaireGenerationException extends RuntimeException {

    public QuestionnaireGenerationException(String message) {
        super(message);
    }

    public QuestionnaireGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
