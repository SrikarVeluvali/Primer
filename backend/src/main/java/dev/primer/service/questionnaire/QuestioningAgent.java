package dev.primer.service.questionnaire;

import org.springframework.stereotype.Component;

import dev.primer.ai.AIRequest;
import dev.primer.ai.AIService;
import dev.primer.config.QuestionnaireProperties;

/**
 * The "questioning agent" from the design flow. Reads the paper and produces the questionnaire that
 * establishes what the reader already knows, so the reading path can be built around their gaps.
 */
@Component
public class QuestioningAgent {

    private static final String PROMPT = """
            You are preparing a reader to understand a research paper. Before they read it, you need to
            find out which of its prerequisite concepts they already know.

            Write between %d and %d questions that probe the reader's existing knowledge.

            Rules:
              - Ask about the reader's familiarity with prerequisite concepts. Never quiz them on the
                paper's own findings — they have not read it yet.
              - Each question targets exactly one concept, named in the "concept" field.
              - Order questions from the most fundamental concept to the most specialised.
              - Set importance to CORE when the paper is unreadable without the concept, HELPFUL when it
                smooths the reading, PERIPHERAL when it only enriches it. Include at least two CORE.
              - For SINGLE_CHOICE and MULTI_CHOICE, give three to five options that describe graded
                levels of familiarity, from no exposure to working fluency. Never write options that
                shame the reader for not knowing something.
              - Use FREE_TEXT for at most one question, asking what the reader wants from the paper.
                FREE_TEXT questions must have an empty options list.
              - IDs must be unique within the questionnaire: "q1", "q2", ... for questions, and
                "a", "b", "c", ... for the options within each question.
              - intro: one friendly sentence explaining why you are asking.

            PAPER:
            %s
            """;

    private final AIService aiService;
    private final QuestionnaireProperties properties;

    public QuestioningAgent(AIService aiService, QuestionnaireProperties properties) {
        this.aiService = aiService;
        this.properties = properties;
    }

    public Questionnaire generate(String paperText) {
        String prompt = PROMPT.formatted(properties.getMinQuestions(), properties.getMaxQuestions(), paperText);
        return aiService.processStructured(new AIRequest(prompt, null, null), Questionnaire.class);
    }
}
