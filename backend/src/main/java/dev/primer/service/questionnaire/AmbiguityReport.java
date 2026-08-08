package dev.primer.service.questionnaire;

import java.util.List;

/**
 * Output of {@link AnswerAmbiguityDetector#detect}. Empty {@code ambiguousAnswers} means every
 * answer clearly indicated the reader's knowledge one way or another — including a plain
 * "I don't know this," which is a clear answer, not an ambiguous one.
 */
public record AmbiguityReport(List<AmbiguousAnswer> ambiguousAnswers) {

    public boolean hasAmbiguity() {
        return ambiguousAnswers != null && !ambiguousAnswers.isEmpty();
    }
}
