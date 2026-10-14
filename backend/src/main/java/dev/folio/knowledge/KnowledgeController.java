package dev.folio.knowledge;

import dev.folio.auth.AccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api")
public class KnowledgeController {
    private final KnowledgeService service;
    private final AccountRepository accounts;
    public KnowledgeController(KnowledgeService service,AccountRepository accounts) { this.service=service; this.accounts=accounts; }
    private UUID user(Authentication auth) { return accounts.findByEmail(auth.getName()).orElseThrow().id; }
    public record TagInput(String name,int level,boolean interested) {}
    public record PaperTags(List<String> tags) {}
    public record Model(String model) {}
    @GetMapping("/knowledge") public KnowledgeService.Profile get(Authentication auth) { return service.get(user(auth)); }
    @GetMapping("/topics") public List<KnowledgeService.CatalogTag> topics() { return service.catalog(); }
    @PostMapping("/knowledge/tags") public KnowledgeService.Profile update(Authentication auth,@RequestBody TagInput input) { return service.update(user(auth),input.name(),input.level(),input.interested()); }
    @DeleteMapping("/knowledge/tags/{id}") public KnowledgeService.Profile remove(Authentication auth,@PathVariable String id) { return service.remove(user(auth),id); }
    @GetMapping("/recommendations") public List<KnowledgeService.Recommendation> recommend(Authentication auth) { return service.recommendations(user(auth)); }
    @PutMapping("/papers/{id}/tags") public PaperTags tags(@PathVariable UUID id,@RequestBody PaperTags input) { return new PaperTags(service.paperTags(id,input.tags())); }
    @PostMapping("/papers/{id}/tags/suggest") public PaperTags suggest(@PathVariable UUID id,@RequestBody Model model) { return new PaperTags(service.suggest(id,model.model())); }
}
