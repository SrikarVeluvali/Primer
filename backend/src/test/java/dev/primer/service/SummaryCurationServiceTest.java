package dev.primer.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SummaryCurationServiceTest {

    private final SummaryCurationService service = new SummaryCurationService(
            List.of(new GenericSummaryCurationStrategy(), new CuratedSummaryCurationStrategy()));

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
        CuratedSummaryCurationStrategy curated = new CuratedSummaryCurationStrategy();

        for (String input : new String[]{null, "", "   ", "some content", "a"}) {
            boolean supportedByExactlyOne = generic.supports(input) ^ curated.supports(input);
            assertThat(supportedByExactlyOne)
                    .as("input %s should be handled by exactly one strategy", input)
                    .isTrue();
        }
    }
}
