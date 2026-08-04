package dev.primer.service.questionnaire;

import java.util.List;

/**
 * One question. {@code id} and the {@code options} IDs are the stable handles the reader's answers
 * refer back to, which is why a generated questionnaire is never regenerated once stored.
 */
public record QuestionnaireQuestion(
        String id,
        String concept,
        QuestionType type,
        QuestionImportance importance,
        String prompt,
        List<QuestionOption> options) {
}
