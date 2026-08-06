package dev.primer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import dev.primer.ai.AIRequest;
import dev.primer.ai.AIResponse;
import dev.primer.ai.AIService;

class SummaryCurationServiceTest {

    // CuratedSummaryCurationStrategy's curate() calls this for real, so it needs a stubbed
    // response even in tests that only care about routing, not about knowledge crafting itself.
    private final AIService aiService = mock(AIService.class);
    private final KnowledgeCrafterAgent knowledgeCrafterAgent = new KnowledgeCrafterAgent(aiService);

    private final SummaryCurationService service = new SummaryCurationService(
            List.of(new GenericSummaryCurationStrategy(), new CuratedSummaryCurationStrategy(knowledgeCrafterAgent)));

    @Test
    void routesNullResponseToGenericStrategy() {
        SummaryCurationResult result = service.curate(UUID.randomUUID(), null);

        assertThat(result.strategyUsed()).isEqualTo("generic");
    }

    @Test
    void routesBlankResponseToGenericStrategy() {
        SummaryCurationResult result = service.curate(UUID.randomUUID(), "   \n  ");

        assertThat(result.strategyUsed()).isEqualTo("generic");
    }

    @Test
    void routesNonBlankResponseToCuratedStrategy() {
        when(aiService.process(any(AIRequest.class)))
                .thenReturn(new AIResponse("knows linear algebra"));

        SummaryCurationResult result = service.curate(UUID.randomUUID(),
                "## Prerequisites\n- I already know linear algebra\n- I don't know attention mechanisms");

        assertThat(result.strategyUsed()).isEqualTo("curated");
    }

    @Test
    void failsClearlyIfNoStrategyMatches() {
        SummaryCurationService serviceWithNoStrategies = new SummaryCurationService(List.of());

        assertThatThrownBy(() -> serviceWithNoStrategies.curate(UUID.randomUUID(), "anything"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void genericAndCuratedStrategiesPartitionEveryInput() {
        GenericSummaryCurationStrategy generic = new GenericSummaryCurationStrategy();
        CuratedSummaryCurationStrategy curated = new CuratedSummaryCurationStrategy(knowledgeCrafterAgent);

        for (String input : new String[]{null, "", "   ", "some content", "a"}) {
            boolean supportedByExactlyOne = generic.supports(input) ^ curated.supports(input);
            assertThat(supportedByExactlyOne)
                    .as("input %s should be handled by exactly one strategy", input)
                    .isTrue();
        }
    }
}
