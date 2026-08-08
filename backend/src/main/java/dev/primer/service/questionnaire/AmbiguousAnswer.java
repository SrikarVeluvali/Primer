package dev.primer.service.questionnaire;

/**
 * One answer the model judged too ambiguous to confidently derive the reader's actual knowledge
 * from, and why.
 */
public record AmbiguousAnswer(String questionId, String reason) {
}
