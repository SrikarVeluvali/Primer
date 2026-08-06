package dev.primer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import dev.primer.ai.AIRequest;
import dev.primer.ai.AIResponse;
import dev.primer.ai.AIService;

class KnowledgeCrafterAgentTest {

    private final AIService aiService = mock(AIService.class);
    private final KnowledgeCrafterAgent agent = new KnowledgeCrafterAgent(aiService);

    @Test
    void returnsModelOutputAsTheUnderstandingSummary() {
        when(aiService.process(any(AIRequest.class)))
                .thenReturn(new AIResponse("Knows linear algebra; unfamiliar with attention mechanisms."));

        UserKnowledgeUnderstanding understanding = agent.craftUnderstanding(
                "- I know linear algebra\n- I don't know attention mechanisms");

        assertThat(understanding.summary())
                .isEqualTo("Knows linear algebra; unfamiliar with attention mechanisms.");
    }

    @Test
    void sendsTheQuestionnaireContentAsTextOnlyInput() {
        when(aiService.process(any(AIRequest.class))).thenReturn(new AIResponse("summary"));

        agent.craftUnderstanding("I know linear algebra");

        var requestCaptor = org.mockito.ArgumentCaptor.forClass(AIRequest.class);
        verify(aiService).process(requestCaptor.capture());

        AIRequest sentRequest = requestCaptor.getValue();
        assertThat(sentRequest.image()).isNull();
        assertThat(sentRequest.text()).contains("I know linear algebra");
    }
}
