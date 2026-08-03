package dev.primer.service.questionnaire;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import dev.primer.service.PaperAccessedEvent;

/**
 * Starts analysis when a reader opens a paper and has not answered the questionnaire yet.
 * <p>
 * Firing after commit is load-bearing: the listener runs on another thread with its own transaction,
 * so a listener that ran before the publisher committed could read a paper row that is not visible to
 * it yet.
 */
@Component
public class PaperAccessedListener {

    private final QuestionnaireGenerationService generationService;

    public PaperAccessedListener(QuestionnaireGenerationService generationService) {
        this.generationService = generationService;
    }

    @Async("analysisExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaperAccessed(PaperAccessedEvent event) {
        generationService.generateIfNeeded(event.paperId());
    }
}
