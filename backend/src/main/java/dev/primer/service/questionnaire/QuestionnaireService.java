package dev.primer.service.questionnaire;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.primer.entity.AnalysisStatus;
import dev.primer.entity.Paper;
import dev.primer.entity.PaperAnalysis;
import dev.primer.entity.ReadingStage;
import dev.primer.exception.PaperNotFoundException;
import dev.primer.repository.PaperAnalysisRepository;
import dev.primer.repository.PaperRepository;
import dev.primer.service.PaperAccessedEvent;

/**
 * The reader-facing side of the questionnaire: what state it is in, and the reader's choice to skip it.
 */
@Service
public class QuestionnaireService {

    private final PaperRepository paperRepository;
    private final PaperAnalysisRepository analysisRepository;
    private final ApplicationEventPublisher eventPublisher;

    public QuestionnaireService(PaperRepository paperRepository,
                                PaperAnalysisRepository analysisRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.paperRepository = paperRepository;
        this.analysisRepository = analysisRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public QuestionnaireView find(UUID paperId) {
        requirePaper(paperId);
        PaperAnalysis analysis = analysisRepository.findByPaperId(paperId).orElse(null);
        if (analysis == null) {
            // A reader can reach the questionnaire without ever hitting the PDF endpoint, so asking
            // for it starts generation too. The claim in the generation service keeps this from
            // duplicating a run that the PDF access already started.
            eventPublisher.publishEvent(new PaperAccessedEvent(paperId));
            return new QuestionnaireView(AnalysisStatus.PENDING, null, null);
        }
        return new QuestionnaireView(analysis.getStatus(), analysis.getQuestionnaireJson(), analysis.getLastError());
    }

    @Transactional
    public void skip(UUID paperId) {
        Paper paper = requirePaper(paperId);
        paper.advanceTo(ReadingStage.QUESTIONNAIRE_SKIPPED);
    }

    private Paper requirePaper(UUID paperId) {
        return paperRepository.findById(paperId)
                .orElseThrow(() -> new PaperNotFoundException("No paper exists with ID " + paperId + "."));
    }
}
