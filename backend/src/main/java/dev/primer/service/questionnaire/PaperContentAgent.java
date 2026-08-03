package dev.primer.service.questionnaire;

import org.springframework.stereotype.Component;

import dev.primer.ai.AIRequest;
import dev.primer.ai.AIService;

/**
 * The "paper content analyzer" from the design flow. Reduces the extracted paper text to a compact
 * structured reading of what the paper is and what a reader needs to know to follow it.
 */
@Component
public class PaperContentAgent {

    private static final String PROMPT = """
            You are analysing a research paper so a reading assistant can guide a newcomer through it.

            Read the paper text below and produce a structured summary of it.
              - title: the paper's title exactly as written.
              - summary: three to five sentences on what the paper does and why it matters.
              - domain: the research area, e.g. "natural language processing" or "condensed matter physics".
              - contributions: the specific claims or artifacts the paper contributes.
              - methods: the techniques, architectures, or experimental approaches it uses.
              - prerequisiteConcepts: the concepts a reader must already understand to follow this
                paper, most fundamental first. Name concepts, not sections — "variational inference",
                not "the background section". Do not include concepts the paper itself introduces.

            Base everything strictly on the text provided. Do not speculate about content you cannot see.

            PAPER TEXT:
            %s
            """;

    private final AIService aiService;

    public PaperContentAgent(AIService aiService) {
        this.aiService = aiService;
    }

    public PaperContent analyze(String paperText) {
        AIRequest request = new AIRequest(PROMPT.formatted(paperText), null, null);
        return aiService.processStructured(request, PaperContent.class);
    }
}
