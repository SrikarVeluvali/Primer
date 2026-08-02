package dev.primer.service;

import java.util.UUID;

/**
 * One path through the "curate summary" decision in the prerequisite-questionnaire flow:
 * either the user skipped the questionnaire (generic summary) or answered it (curated summary).
 * Implementations partition the input space via {@link #supports(String)} — for any given
 * {@code questionnaireResponseMarkdown}, exactly one implementation should return {@code true}.
 * {@link SummaryCurationService} picks whichever one matches and delegates to it; this class
 * knows nothing about which concrete strategies exist, so adding a third path later (e.g. a
 * "partially answered" strategy) means adding a new {@code @Component}, not touching this
 * interface or the service that resolves it.
 */
public interface SummaryCurationStrategy {

    /**
     * @param questionnaireResponseMarkdown the raw markdown the frontend sent back for the
     *                                       prerequisite questionnaire; {@code null} or blank
     *                                       means the user skipped it.
     */
    boolean supports(String questionnaireResponseMarkdown);

    SummaryCurationResult curate(UUID paperId, String questionnaireResponseMarkdown);
}
