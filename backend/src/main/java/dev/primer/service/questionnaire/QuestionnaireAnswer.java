package dev.primer.service.questionnaire;

import java.util.List;

/**
 * One reader's answer to one {@link QuestionnaireQuestion}, referenced by its stable
 * {@code questionId}.
 * <p>
 * For {@code SINGLE_CHOICE}/{@code MULTI_CHOICE} questions, {@code selectedOptionIds} holds the
 * chosen {@link QuestionOption} IDs and {@code freeText} is {@code null}. For {@code FREE_TEXT}
 * questions it's the reverse: {@code selectedOptionIds} is empty and {@code freeText} carries the
 * answer.
 */
public record QuestionnaireAnswer(String questionId, List<String> selectedOptionIds, String freeText) {
}
