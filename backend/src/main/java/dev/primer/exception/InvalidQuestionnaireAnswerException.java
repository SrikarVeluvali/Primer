package dev.primer.exception;

/**
 * Raised when a submitted {@code QuestionnaireAnswers} doesn't line up with the
 * {@code Questionnaire} it's supposedly answering — a question ID that doesn't exist, an option
 * ID the question never offered, or a single-choice question answered with anything other than
 * exactly one option. These are hard usage errors, not judgment calls: unlike ambiguity (which
 * {@code AnswerAmbiguityDetector} asks the model about), the schema alone is enough to know
 * something is wrong here, so this fails fast before spending an AI call on garbage input.
 */
public class InvalidQuestionnaireAnswerException extends RuntimeException {

    public InvalidQuestionnaireAnswerException(String message) {
        super(message);
    }
}
