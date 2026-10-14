package dev.folio.api;

import dev.folio.auth.AccountRepository;
import dev.folio.paper.*;
import dev.folio.chat.ChatService;
import dev.folio.ollama.OllamaClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.time.Instant;
import java.nio.file.Files;

@RestController
@RequestMapping("/api")
public class ApiController {
    private final PaperService service;
    private final PaperRepository papers;
    private final OllamaClient ollama;
    private final ChatService chats;
    private final AccountRepository accounts;
    public ApiController(PaperService service, PaperRepository papers, OllamaClient ollama, ChatService chats, AccountRepository accounts) {
        this.service = service; this.papers = papers; this.ollama = ollama; this.chats = chats; this.accounts = accounts;
    }
    public record PaperView(UUID id, String arxivId, String title, String authors, String abstractText, String published,
                            String status, String stage, String error, int pageCount, int chunkCount, Instant createdAt, int indexVersion, List<String> tags) {
        public static PaperView of(Paper p) { return new PaperView(p.id, p.arxivId, p.title, p.authors, p.abstractText, p.published, p.status, p.stage, p.error, p.pageCount, p.chunkCount, p.createdAt, p.indexVersion, List.copyOf(p.tags)); }
    }
    public record Import(@NotBlank @Size(max = 500) String url) {}
    public record Question(@NotBlank @Size(max = 4000) String question, @Size(max = 100) String model) {}
    public record Status(boolean connected, List<String> models, String chatModel, String embeddingModel, String message) {}
    @GetMapping("/status") public Status status() {
        try { return new Status(true, ollama.chatModels(), ollama.chatModel, ollama.embeddingModel, "Ollama is connected"); }
        catch (Exception e) { return new Status(false, List.of(), ollama.chatModel, ollama.embeddingModel, e.getMessage()); }
    }
    @GetMapping("/papers") public List<PaperView> list() { return papers.findAllByOrderByCreatedAtDesc().stream().map(PaperView::of).toList(); }
    @PostMapping("/papers") public ResponseEntity<PaperView> add(@Valid @RequestBody Import input) { return ResponseEntity.accepted().body(PaperView.of(service.add(input.url()))); }
    @GetMapping("/papers/{id}") public PaperView get(@PathVariable UUID id) { return PaperView.of(service.get(id)); }
    @PostMapping("/papers/{id}/retry") public PaperView retry(@PathVariable UUID id) { return PaperView.of(service.retry(id)); }
    @GetMapping("/papers/{id}/pdf") public ResponseEntity<Resource> pdf(@PathVariable UUID id) {
        service.get(id); var path = service.pdfPath(id);
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The PDF has not been downloaded yet.");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=paper.pdf").body(new FileSystemResource(path));
    }
    private UUID user(Authentication auth) { return accounts.findByEmail(auth.getName()).orElseThrow().id; }
    @GetMapping("/papers/{id}/chat") public List<ChatService.TurnView> history(@PathVariable UUID id, Authentication auth) { return chats.history(user(auth), id); }
    @PostMapping("/papers/{id}/chat") public ChatService.TurnView ask(@PathVariable UUID id, @Valid @RequestBody Question question, Authentication auth) { return chats.ask(user(auth), id, question.question().trim(), question.model()); }
    @DeleteMapping("/papers/{id}/chat") public void clear(@PathVariable UUID id, Authentication auth) { chats.clear(user(auth), id); }
}
