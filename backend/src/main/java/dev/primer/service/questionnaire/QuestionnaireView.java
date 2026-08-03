package dev.primer.service.questionnaire;

import dev.primer.entity.AnalysisStatus;

/**
 * What the reader can see of a paper's questionnaire right now. {@code questionnaireJson} is the
 * stored document, served back verbatim, and is only present once the status is
 * {@link AnalysisStatus#READY}.
 */
public record QuestionnaireView(AnalysisStatus status, String questionnaireJson, String error) {

    public boolean isReady() {
        return status == AnalysisStatus.READY && questionnaireJson != null;
    }
}
