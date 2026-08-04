package dev.primer.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;

import dev.primer.entity.AnalysisStatus;
import dev.primer.service.questionnaire.QuestionnaireView;

/**
 * The questionnaire, or an explanation of why it is not here yet.
 * <p>
 * {@code questionnaire} is embedded with {@link JsonRawValue} rather than deserialized and
 * re-serialized, so the document the reader receives is byte-for-byte the one that was generated and
 * stored. Answers will refer to its question and option IDs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuestionnaireResponse(
        AnalysisStatus status,
        @JsonRawValue String questionnaire,
        String error) {

    public static QuestionnaireResponse from(QuestionnaireView view) {
        return new QuestionnaireResponse(view.status(), view.questionnaireJson(), view.error());
    }
}
