package dev.primer.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

/**
 * The routing module from the questionnaire-response diagram: given whatever the frontend sent
 * back for the prerequisite questionnaire, picks the one {@link SummaryCurationStrategy} that
 * applies and delegates to it.
 * <p>
 * Spring injects every {@code @Component} implementing {@link SummaryCurationStrategy} into this
 * list automatically — adding a new strategy later is a new class, not a change here.
 */
@Service
public class SummaryCurationService {

    private final List<SummaryCurationStrategy> strategies;

    public SummaryCurationService(List<SummaryCurationStrategy> strategies) {
        this.strategies = strategies;
    }

    public SummaryCurationResult curate(UUID paperId, String questionnaireResponseMarkdown) {
        return strategies.stream()
                .filter(strategy -> strategy.supports(questionnaireResponseMarkdown))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No SummaryCurationStrategy handles this questionnaire response"))
                .curate(paperId, questionnaireResponseMarkdown);
    }
}
