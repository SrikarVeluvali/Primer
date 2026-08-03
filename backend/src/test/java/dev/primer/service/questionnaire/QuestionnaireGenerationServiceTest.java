package dev.primer.service.questionnaire;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import dev.primer.config.QuestionnaireProperties;
import dev.primer.entity.AnalysisStatus;
import dev.primer.entity.Paper;
import dev.primer.entity.PaperAnalysis;
import dev.primer.entity.ReadingStage;
import dev.primer.exception.QuestionnaireGenerationException;
import dev.primer.repository.PaperAnalysisRepository;
import dev.primer.repository.PaperRepository;
import dev.primer.service.ExtractedPage;
import dev.primer.service.PaperStorage;
import dev.primer.service.PdfExtractionResult;
import dev.primer.service.PdfExtractionService;

class QuestionnaireGenerationServiceTest {

    private final PaperRepository paperRepository = mock(PaperRepository.class);
    private final PaperAnalysisRepository analysisRepository = mock(PaperAnalysisRepository.class);
    private final PaperStorage paperStorage = mock(PaperStorage.class);
    private final PdfExtractionService pdfExtractionService = mock(PdfExtractionService.class);
    private final PaperContentAgent paperContentAgent = mock(PaperContentAgent.class);
    private final QuestioningAgent questioningAgent = mock(QuestioningAgent.class);
    private final QuestionnaireValidator validator = mock(QuestionnaireValidator.class);
    private final QuestionnaireProperties properties = new QuestionnaireProperties();

    private final UUID paperId = UUID.randomUUID();
    private Paper paper;
    private PaperAnalysis analysis;
    private QuestionnaireGenerationService service;

    private static final Questionnaire QUESTIONNAIRE = new Questionnaire("A paper", "A few questions.",
            List.of(new QuestionnaireQuestion("q1", "attention", QuestionType.SINGLE_CHOICE,
                    QuestionImportance.CORE, "How familiar are you?",
                    List.of(new QuestionOption("a", "Not at all"), new QuestionOption("b", "Very")))));

    private static final PaperContent CONTENT = new PaperContent("A paper", "It does something.",
            "machine learning", List.of("a result"), List.of("a method"), List.of("linear algebra"));

    @BeforeEach
    void setUp() {
        paper = new Paper(paperId, "1706.03762", "https://arxiv.org/pdf/1706.03762", "/papers/x.pdf");
        analysis = new PaperAnalysis(UUID.randomUUID(), paperId);
        service = new QuestionnaireGenerationService(paperRepository, analysisRepository, paperStorage,
                pdfExtractionService, paperContentAgent, questioningAgent, validator, properties,
                new ObjectMapper(), Runnable::run);

        when(paperRepository.findById(paperId)).thenReturn(Optional.of(paper));
        when(analysisRepository.findByPaperId(paperId)).thenReturn(Optional.of(analysis));
        when(paperStorage.resolve("/papers/x.pdf")).thenReturn(Path.of("/papers/x.pdf"));
        when(pdfExtractionService.extract(any())).thenReturn(
                new PdfExtractionResult(List.of(new ExtractedPage(1, "The paper text.", List.of()))));
        when(paperContentAgent.analyze(any())).thenReturn(CONTENT);
        when(questioningAgent.generate(any())).thenReturn(QUESTIONNAIRE);
    }

    private void claimSucceeds() {
        when(analysisRepository.claimForGeneration(paperId, properties.getMaxAttempts())).thenReturn(1);
    }

    @Test
    void doesNothingWhenAnotherRunAlreadyOwnsThePaper() {
        when(analysisRepository.claimForGeneration(paperId, properties.getMaxAttempts())).thenReturn(0);

        service.generateIfNeeded(paperId);

        verify(paperContentAgent, never()).analyze(any());
        verify(questioningAgent, never()).generate(any());
    }

    @Test
    void createsTheAnalysisRowBeforeClaimingIt() {
        when(analysisRepository.claimForGeneration(paperId, properties.getMaxAttempts())).thenReturn(0);

        service.generateIfNeeded(paperId);

        verify(analysisRepository).insertIfAbsent(any(UUID.class), any(UUID.class));
    }

    @Test
    void runsBothAgentsAndStoresWhatTheyProduce() {
        claimSucceeds();

        service.generateIfNeeded(paperId);

        verify(paperContentAgent).analyze("The paper text.");
        verify(questioningAgent).generate("The paper text.");
        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.READY);
        assertThat(analysis.getQuestionnaireJson()).contains("\"id\":\"q1\"");
        assertThat(analysis.getContentJson()).contains("linear algebra");
    }

    @Test
    void advancesTheReaderStageOnceTheQuestionnaireIsReady() {
        claimSucceeds();

        service.generateIfNeeded(paperId);

        assertThat(paper.getReadingStage()).isEqualTo(ReadingStage.QUESTIONNAIRE_READY);
        verify(paperRepository).save(paper);
    }

    @Test
    void resolvesThePdfThroughStorageSoThePathIsCheckedAgainstTheRoot() {
        claimSucceeds();

        service.generateIfNeeded(paperId);

        verify(paperStorage).resolve("/papers/x.pdf");
    }

    @Test
    void retriesOnceWhenTheFirstQuestionnaireIsUnusable() {
        claimSucceeds();
        doThrowThenPass();

        service.generateIfNeeded(paperId);

        verify(questioningAgent, times(2)).generate(any());
        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.READY);
    }

    @Test
    void failsTheAnalysisWhenBothAttemptsAreUnusable() {
        claimSucceeds();
        org.mockito.Mockito.doThrow(new QuestionnaireGenerationException("no options"))
                .when(validator).validate(any());

        service.generateIfNeeded(paperId);

        verify(questioningAgent, times(2)).generate(any());
        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getLastError()).contains("no options");
        assertThat(paper.getReadingStage()).isEqualTo(ReadingStage.INGESTED);
    }

    @Test
    void recordsTheUnderlyingReasonNotJustTheTopLevelMessage() {
        claimSucceeds();
        when(paperContentAgent.analyze(any())).thenThrow(new dev.primer.exception.AiProviderException(
                "Failed to call Groq model", new RuntimeException("model `qwen/nope` does not exist")));

        service.generateIfNeeded(paperId);

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getLastError())
                .contains("Failed to call Groq model")
                .contains("model `qwen/nope` does not exist");
    }

    @Test
    void failsTheAnalysisWhenThePaperHasNoReadableText() {
        claimSucceeds();
        when(pdfExtractionService.extract(any())).thenReturn(
                new PdfExtractionResult(List.of(new ExtractedPage(1, "   ", List.of()))));

        service.generateIfNeeded(paperId);

        assertThat(analysis.getStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(analysis.getLastError()).contains("No readable text");
        verify(questioningAgent, never()).generate(any());
    }

    @Test
    void truncatesPaperTextToTheConfiguredInputLimit() {
        claimSucceeds();
        properties.setMaxInputCharacters(10);
        when(pdfExtractionService.extract(any())).thenReturn(
                new PdfExtractionResult(List.of(new ExtractedPage(1, "0123456789abcdef", List.of()))));

        service.generateIfNeeded(paperId);

        ArgumentCaptor<String> text = ArgumentCaptor.forClass(String.class);
        verify(questioningAgent).generate(text.capture());
        assertThat(text.getValue()).isEqualTo("0123456789");
    }

    @Test
    void claimsWithTheConfiguredAttemptCeilingSoAFailingPaperStopsRetrying() {
        properties.setMaxAttempts(2);

        service.generateIfNeeded(paperId);

        verify(analysisRepository).claimForGeneration(paperId, 2);
    }

    private void doThrowThenPass() {
        org.mockito.Mockito.doThrow(new QuestionnaireGenerationException("no options"))
                .doNothing()
                .when(validator).validate(any());
    }
}
