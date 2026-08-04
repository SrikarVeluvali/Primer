package dev.primer.service.questionnaire;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import dev.primer.config.QuestionnaireProperties;
import dev.primer.entity.Paper;
import dev.primer.entity.PaperAnalysis;
import dev.primer.entity.ReadingStage;
import dev.primer.exception.QuestionnaireGenerationException;
import dev.primer.repository.PaperAnalysisRepository;
import dev.primer.repository.PaperRepository;
import dev.primer.service.ExtractedPage;
import dev.primer.service.PaperStorage;
import dev.primer.service.PdfExtractionService;

/**
 * Runs the two analysis agents for a paper and stores what they produce.
 * <p>
 * Generation happens off the request thread because a full-paper LLM pass takes tens of seconds. Two
 * readers opening the same paper at once must still produce exactly one run, so ownership is taken
 * through a single conditional UPDATE rather than a read-then-write check.
 */
@Service
public class QuestionnaireGenerationService {

    private static final Logger log = LoggerFactory.getLogger(QuestionnaireGenerationService.class);
    private static final String PAGE_SEPARATOR = "\n\n";

    private final PaperRepository paperRepository;
    private final PaperAnalysisRepository analysisRepository;
    private final PaperStorage paperStorage;
    private final PdfExtractionService pdfExtractionService;
    private final PaperContentAgent paperContentAgent;
    private final QuestioningAgent questioningAgent;
    private final QuestionnaireValidator validator;
    private final QuestionnaireProperties properties;
    private final ObjectMapper objectMapper;
    private final Executor executor;

    public QuestionnaireGenerationService(PaperRepository paperRepository,
                                          PaperAnalysisRepository analysisRepository,
                                          PaperStorage paperStorage,
                                          PdfExtractionService pdfExtractionService,
                                          PaperContentAgent paperContentAgent,
                                          QuestioningAgent questioningAgent,
                                          QuestionnaireValidator validator,
                                          QuestionnaireProperties properties,
                                          ObjectMapper objectMapper,
                                          @Qualifier("agentExecutor") Executor executor) {
        this.paperRepository = paperRepository;
        this.analysisRepository = analysisRepository;
        this.paperStorage = paperStorage;
        this.pdfExtractionService = pdfExtractionService;
        this.paperContentAgent = paperContentAgent;
        this.questioningAgent = questioningAgent;
        this.validator = validator;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.executor = executor;
    }

    /**
     * Generates the analysis for a paper unless another caller already owns the run, or it has already
     * succeeded, or it has failed too many times. Safe to call on every PDF access.
     */
    public void generateIfNeeded(UUID paperId) {
        analysisRepository.insertIfAbsent(UUID.randomUUID(), paperId);
        if (analysisRepository.claimForGeneration(paperId, properties.getMaxAttempts()) == 0) {
            log.debug("Analysis for paper {} is already claimed, complete, or exhausted", paperId);
            return;
        }

        log.info("Generating analysis for paper {}", paperId);
        try {
            generate(paperId);
            log.info("Analysis for paper {} is ready", paperId);
        } catch (RuntimeException exception) {
            log.warn("Analysis for paper {} failed", paperId, exception);
            recordFailure(paperId, exception);
        }
    }

    private void generate(UUID paperId) {
        Paper paper = paperRepository.findById(paperId)
                .orElseThrow(() -> new QuestionnaireGenerationException("No paper exists with ID " + paperId + "."));
        String paperText = readPaperText(paper);

        // Both agents read the same parsed paper and neither needs the other's output, so they run
        // concurrently and the run costs one agent's latency rather than two.
        CompletableFuture<PaperContent> content =
                CompletableFuture.supplyAsync(() -> paperContentAgent.analyze(paperText), executor);
        CompletableFuture<Questionnaire> questionnaire =
                CompletableFuture.supplyAsync(() -> generateValidQuestionnaire(paperText), executor);

        try {
            CompletableFuture.allOf(content, questionnaire).join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();
            throw cause instanceof RuntimeException runtime ? runtime
                    : new QuestionnaireGenerationException("Analysis of paper " + paperId + " failed.", cause);
        }

        PaperAnalysis analysis = requireAnalysis(paperId);
        analysis.markReady(serialize(content.join()), serialize(questionnaire.join()));
        analysisRepository.save(analysis);

        paper.advanceTo(ReadingStage.QUESTIONNAIRE_READY);
        paperRepository.save(paper);
    }

    /**
     * Models drift from the brief occasionally — an empty option list, a duplicated ID. One retry
     * costs a single call and recovers most of those; a second failure means the paper is the problem.
     */
    private Questionnaire generateValidQuestionnaire(String paperText) {
        try {
            Questionnaire first = questioningAgent.generate(paperText);
            validator.validate(first);
            return first;
        } catch (QuestionnaireGenerationException exception) {
            log.info("Discarding unusable questionnaire and retrying once: {}", exception.getMessage());
        }
        Questionnaire retried = questioningAgent.generate(paperText);
        validator.validate(retried);
        return retried;
    }

    private String readPaperText(Paper paper) {
        Path path = paperStorage.resolve(paper.getStoragePath());
        String text = pdfExtractionService.extract(path).pages().stream()
                .map(ExtractedPage::text)
                .collect(Collectors.joining(PAGE_SEPARATOR));
        if (text.isBlank()) {
            throw new QuestionnaireGenerationException(
                    "No readable text was extracted from paper " + paper.getId() + ".");
        }
        int limit = properties.getMaxInputCharacters();
        return text.length() <= limit ? text : text.substring(0, limit);
    }

    /**
     * Flattens the cause chain into the stored message. The top-level message only names the provider
     * that failed; what a caller actually needs is the reason underneath it — a rejected model name, a
     * bad key, an exceeded context window — since the stored text is all the questionnaire endpoint
     * ever shows.
     */
    private static String describe(Throwable exception) {
        StringBuilder description = new StringBuilder();
        for (Throwable current = exception; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message == null || message.isBlank()) {
                message = current.getClass().getSimpleName();
            }
            if (description.indexOf(message) >= 0) {
                continue;
            }
            if (!description.isEmpty()) {
                description.append(": ");
            }
            description.append(message);
        }
        return description.toString();
    }

    private void recordFailure(UUID paperId, RuntimeException exception) {
        try {
            PaperAnalysis analysis = requireAnalysis(paperId);
            analysis.markFailed(describe(exception));
            analysisRepository.save(analysis);
        } catch (RuntimeException failure) {
            // The original generation error is the useful one; losing the bookkeeping only means the
            // row stays GENERATING and is retried on the next access.
            log.error("Could not record the analysis failure for paper {}", paperId, failure);
        }
    }

    private PaperAnalysis requireAnalysis(UUID paperId) {
        return analysisRepository.findByPaperId(paperId)
                .orElseThrow(() -> new QuestionnaireGenerationException(
                        "No analysis row exists for paper " + paperId + "."));
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new QuestionnaireGenerationException("Could not store the generated analysis.", exception);
        }
    }
}
