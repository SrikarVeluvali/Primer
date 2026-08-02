package dev.primer.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Handles the "user answered the questionnaire" branch — the summary should be curated against
 * what they said they already know, per the flow shown in the questionnaire diagram.
 */
@Component
public class CuratedSummaryCurationStrategy implements SummaryCurationStrategy {

    @Override
    public boolean supports(String questionnaireResponseMarkdown) {
        return questionnaireResponseMarkdown != null && !questionnaireResponseMarkdown.isBlank();
    }

    @Override
    public SummaryCurationResult curate(UUID paperId, String questionnaireResponseMarkdown) {
        // TODO: replace with a real AIService.process(...) call once the curated-summary prompt
        // is designed — it should fold questionnaireResponseMarkdown into the prompt so the
        // summary skips/expands concepts based on what the user said they already know.
        return new SummaryCurationResult("curated",
                "Stub: would generate a curated summary for paper " + paperId
                        + " using " + questionnaireResponseMarkdown.length()
                        + " characters of questionnaire answers.");
    }
}
