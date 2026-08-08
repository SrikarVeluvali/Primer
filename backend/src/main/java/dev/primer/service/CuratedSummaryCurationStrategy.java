package dev.primer.service;

import java.util.UUID;

import org.springframework.stereotype.Component;

/**
 * Handles the "user answered the questionnaire" branch — the summary should be curated against
 * what they said they already know, per the flow shown in the questionnaire diagram.
 */
@Component
public class CuratedSummaryCurationStrategy implements SummaryCurationStrategy {

    private final KnowledgeCrafterAgent knowledgeCrafterAgent;

    public CuratedSummaryCurationStrategy(KnowledgeCrafterAgent knowledgeCrafterAgent) {
        this.knowledgeCrafterAgent = knowledgeCrafterAgent;
    }

    @Override
    public boolean supports(String questionnaireResponseMarkdown) {
        return questionnaireResponseMarkdown != null && !questionnaireResponseMarkdown.isBlank();
    }

    @Override
    public SummaryCurationResult curate(UUID paperId, String questionnaireResponseMarkdown) {
        UserKnowledgeUnderstanding understanding =
                knowledgeCrafterAgent.craftUnderstanding(questionnaireResponseMarkdown);

        // TODO: once paper text retrieval is wired in here, combine understanding.summary()
        // with the paper's extracted text into the actual curated-summary prompt via
        // AIService. For now this proves the knowledge-crafting step end-to-end and threads
        // its output through to where the real summary call will read it.
        return new SummaryCurationResult("curated",
                "Stub: would generate a curated summary for paper " + paperId
                        + " using this knowledge understanding: " + understanding.summary());
    }
}
