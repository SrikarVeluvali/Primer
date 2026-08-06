package dev.primer.service;

import org.springframework.stereotype.Service;

import dev.primer.ai.AIRequest;
import dev.primer.ai.AIResponse;
import dev.primer.ai.AIService;

/**
 * Turns a user's raw prerequisite-questionnaire answers into a plain-language summary of what
 * they already know and what they likely don't — the "knowledge crafter agent" from the
 * questionnaire flow. Reuses {@link AIService} for the actual model call, so it's agnostic to
 * which provider (Gemini, Groq, ...) is configured.
 * <p>
 * This is one step, not the whole curated-summary pipeline: its output is meant to be threaded
 * into a later prompt (alongside the paper's own text) that produces the actual curated summary,
 * not to be shown to the user directly.
 */
@Service
public class KnowledgeCrafterAgent {

    private static final String PROMPT_TEMPLATE = """
            A user is about to read a research paper. Before reading, they answered a short \
            questionnaire about which prerequisite concepts they already understand.

            Based only on what they wrote below, summarize in a few plain sentences what this \
            user already knows and what they likely do NOT know. Name specific concepts where \
            the response mentions them. Do not invent knowledge they didn't state, and if the \
            response doesn't give enough signal about a concept, say that rather than guessing.

            Questionnaire response:
            %s
            """;

    private final AIService aiService;

    public KnowledgeCrafterAgent(AIService aiService) {
        this.aiService = aiService;
    }

    public UserKnowledgeUnderstanding craftUnderstanding(String questionnaireResponseMarkdown) {
        String prompt = PROMPT_TEMPLATE.formatted(questionnaireResponseMarkdown);
        AIResponse response = aiService.process(new AIRequest(prompt, null, null));
        return new UserKnowledgeUnderstanding(response.text());
    }
}
