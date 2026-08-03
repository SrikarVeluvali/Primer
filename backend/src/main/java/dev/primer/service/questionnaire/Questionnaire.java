package dev.primer.service.questionnaire;

import java.util.List;

/**
 * The questionnaire served to the reader. Generated once per paper and then treated as immutable.
 */
public record Questionnaire(String paperTitle, String intro, List<QuestionnaireQuestion> questions) {
}
