package dev.primer.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Handles the "user skipped the questionnaire" branch — no stated prerequisite knowledge to
 * curate against, so the summary is generated without any personalization input.
 */
@Component
public class GenericSummaryCurationStrategy implements SummaryCurationStrategy {

    @Override
    public boolean supports(String questionnaireResponseMarkdown) {
        return questionnaireResponseMarkdown == null || questionnaireResponseMarkdown.isBlank();
    }

    @Override
    public SummaryCurationResult curate(UUID paperId, String questionnaireResponseMarkdown) {
        // TODO: replace with a real AIService.process(...) call once the generic-summary
        // prompt is designed. For now this just proves the routing picked the right strategy.
        return new SummaryCurationResult("generic",
                "Stub: would generate a generic summary for paper " + paperId
                        + " (questionnaire was skipped, no stated prerequisite knowledge to curate against).");
    }
}
