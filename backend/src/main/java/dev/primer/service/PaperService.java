package dev.primer.service;

import java.nio.file.Path;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.primer.client.ArxivPdfClient;
import dev.primer.client.ArxivReference;
import dev.primer.client.ArxivReferenceParser;
import dev.primer.entity.Paper;
import dev.primer.entity.ReadingStage;
import dev.primer.exception.PaperNotFoundException;
import dev.primer.repository.PaperRepository;

@Service
public class PaperService {

    private final PaperRepository paperRepository;
    private final ArxivReferenceParser referenceParser;
    private final ArxivPdfClient arxivPdfClient;
    private final PaperStorage paperStorage;
    private final ApplicationEventPublisher eventPublisher;

    public PaperService(PaperRepository paperRepository, ArxivReferenceParser referenceParser,
                        ArxivPdfClient arxivPdfClient, PaperStorage paperStorage,
                        ApplicationEventPublisher eventPublisher) {
        this.paperRepository = paperRepository;
        this.referenceParser = referenceParser;
        this.arxivPdfClient = arxivPdfClient;
        this.paperStorage = paperStorage;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PaperPdf ingest(String rawReference) {
        ArxivReference reference = referenceParser.parse(rawReference);
        Paper existing = paperRepository.findByArxivId(reference.id()).orElse(null);
        if (existing != null && paperStorage.exists(existing.getStoragePath())) {
            return new PaperPdf(existing.getId(), paperStorage.resolve(existing.getStoragePath()));
        }

        UUID id = existing == null ? UUID.randomUUID() : existing.getId();
        Path temporary = paperStorage.temporaryPath(id);
        Path permanent = paperStorage.permanentPath(id);
        paperStorage.prepare(temporary);
        try {
            arxivPdfClient.download(reference, temporary);
            paperStorage.promote(temporary, permanent);
            Paper paper = existing == null
                    ? new Paper(id, reference.id(), reference.pdfUrl(), permanent.toString())
                    : existing;
            if (existing != null) {
                paper.markStored(permanent.toString());
            }
            paperRepository.save(paper);
            return new PaperPdf(id, permanent);
        } catch (RuntimeException exception) {
            paperStorage.deleteQuietly(temporary);
            paperStorage.deleteQuietly(permanent);
            throw exception;
        }
    }

    @Transactional
    public PaperPdf findPdf(UUID id) {
        Paper paper = paperRepository.findById(id)
                .orElseThrow(() -> new PaperNotFoundException("No paper exists with ID " + id + "."));
        if (!paperStorage.exists(paper.getStoragePath())) {
            paper.markFileMissing();
            throw new PaperNotFoundException("The PDF is no longer available for paper " + id + ".");
        }

        // Opening the paper is what tells us a reader has started on it, and it is the point at which
        // the questionnaire becomes worth generating. The event is handled after this transaction
        // commits so the generation never delays serving the PDF.
        paper.advanceTo(ReadingStage.ACCESSED);
        eventPublisher.publishEvent(new PaperAccessedEvent(paper.getId()));
        return new PaperPdf(paper.getId(), paperStorage.resolve(paper.getStoragePath()));
    }
}
