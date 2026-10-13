package dev.folio.paper;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.folio.ollama.OllamaClient;
import dev.folio.retrieval.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import jakarta.annotation.PreDestroy;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

@Service
public class PaperService {
    public static final int INDEX_VERSION = 1;
    private final PaperRepository papers;
    private final ChunkRepository chunks;
    private final ArxivClient arxiv;
    private final OllamaClient ollama;
    private final TextChunker chunker;
    private final ObjectMapper json;
    private final Path directory;
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(20));
    public PaperService(PaperRepository papers, ChunkRepository chunks, ArxivClient arxiv, OllamaClient ollama,
                        TextChunker chunker, ObjectMapper json, @Value("${folio.data-directory}") String directory) {
        this.papers = papers; this.chunks = chunks; this.arxiv = arxiv; this.ollama = ollama; this.chunker = chunker; this.json = json; this.directory = Path.of(directory).toAbsolutePath();
    }
    @EventListener(ApplicationReadyEvent.class)
    public void recover() {
        papers.findAll().stream().filter(p -> !Set.of("READY", "FAILED").contains(p.status)).forEach(p -> {
            p.status = "FAILED"; p.error = "Indexing was interrupted when the server stopped. Retry indexing."; p.stage = "Indexing interrupted"; papers.save(p);
        });
        var outdated = papers.findAll().stream().filter(p -> p.status.equals("READY") && p.indexVersion < INDEX_VERSION).toList();
        for (var paper : outdated) { paper.status = "QUEUED"; paper.stage = "Updating the paper index"; papers.save(paper); }
        if (!outdated.isEmpty()) worker.execute(() -> outdated.forEach(p -> ingest(p.id)));
    }
    @PreDestroy public void stop() { worker.shutdownNow(); }
    public Paper get(UUID id) { return papers.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paper not found.")); }
    public synchronized Paper add(String link) {
        String id = ArxivClient.parseId(link);
        var existing = papers.findByArxivId(id);
        if (existing.isPresent()) return existing.get();
        if (worker.getQueue().remainingCapacity() == 0) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "The import queue is full. Try again shortly.");
        var paper = papers.save(new Paper(id, ollama.embeddingModel));
        worker.execute(() -> ingest(paper.id)); return paper;
    }
    public synchronized Paper retry(UUID id) {
        Paper paper = get(id);
        if (!paper.status.equals("FAILED")) throw new IllegalArgumentException("Only failed imports can be retried.");
        if (worker.getQueue().remainingCapacity() == 0) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "The import queue is full. Try again shortly.");
        paper.status = "QUEUED"; paper.stage = "Waiting to index"; paper.error = null; paper.embeddingModel = ollama.embeddingModel;
        papers.save(paper); worker.execute(() -> ingest(id)); return paper;
    }
    private void stage(Paper p, String message) { p.stage = message; papers.save(p); }
    private void ingest(UUID id) {
        Paper p = get(id);
        try {
            p.status = "INDEXING";
            byte[] pdf;
            if (Files.isRegularFile(pdfPath(id)) && !p.abstractText.isBlank()) {
                stage(p, "Reading the saved paper"); pdf = Files.readAllBytes(pdfPath(id));
            } else {
                stage(p, "Fetching paper details"); arxiv.metadata(p);
                stage(p, "Downloading the paper"); pdf = arxiv.pdf(p.arxivId);
                Files.createDirectories(directory); Files.write(pdfPath(id), pdf);
            }
            stage(p, "Reading pages"); chunks.deleteByPaperId(id);
            List<Chunk> extracted = new ArrayList<>();
            try (var document = Loader.loadPDF(pdf)) {
                p.pageCount = document.getNumberOfPages();
                if (p.pageCount > 300) throw new IllegalArgumentException("Papers over 300 pages are not supported yet.");
                var stripper = new PDFTextStripper(); stripper.setSortByPosition(true);
                for (int page = 1; page <= p.pageCount; page++) {
                    stripper.setStartPage(page); stripper.setEndPage(page);
                    String cleaned = PassageCleaner.clean(stripper.getText(document));
                    for (String text : chunker.split(cleaned)) {
                        if (PassageCleaner.readable(text)) extracted.add(new Chunk(id, page, extracted.size(), text, null));
                    }
                }
            }
            if (extracted.isEmpty()) throw new IllegalArgumentException("This PDF has no readable text. Scanned papers require OCR, which is not supported yet.");
            for (int start = 0; start < extracted.size(); start += 12) {
                stage(p, "Indexing passages " + Math.min(start + 12, extracted.size()) + " of " + extracted.size());
                var batch = extracted.subList(start, Math.min(start + 12, extracted.size()));
                var vectors = ollama.embed(p.embeddingModel, batch.stream().map(c -> c.text).toList());
                for (int i = 0; i < batch.size(); i++) batch.get(i).embedding = json.writeValueAsString(vectors.get(i));
                chunks.saveAll(batch);
            }
            p.chunkCount = extracted.size(); p.indexVersion = INDEX_VERSION; p.status = "READY"; p.error = null; stage(p, "Ready to explore");
        } catch (Exception e) {
            p.status = "FAILED"; p.error = e.getMessage() == null ? "Import failed. Try again." : e.getMessage();
            stage(p, "Import needs attention");
        }
    }
    public Path pdfPath(UUID id) { return directory.resolve(id + ".pdf"); }
}
