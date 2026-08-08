package dev.primer.service.questionnaire;

import java.util.List;

/**
 * A reader's complete submission for one {@link Questionnaire}. Question order/definitions live
 * on the {@code Questionnaire} itself; this only carries what the reader answered.
 */
public record QuestionnaireAnswers(List<QuestionnaireAnswer> answers) {
}
