package dev.folio.annotation;
import dev.folio.auth.AccountRepository;
import dev.folio.chat.ChatService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/papers/{paperId}/annotations")
public class AnnotationController {
    private final AnnotationService service; private final AccountRepository accounts;
    public AnnotationController(AnnotationService service,AccountRepository accounts) { this.service=service;this.accounts=accounts; }
    private UUID user(Authentication auth) { return accounts.findByEmail(auth.getName()).orElseThrow().id; }
    public record Model(String model) {}
    @GetMapping public List<AnnotationService.View> list(Authentication auth,@PathVariable UUID paperId) { return service.list(user(auth),paperId); }
    @PostMapping public AnnotationService.View add(Authentication auth,@PathVariable UUID paperId,@RequestBody AnnotationService.Input input) { return service.add(user(auth),paperId,input); }
    @DeleteMapping("/{id}") public void remove(Authentication auth,@PathVariable UUID paperId,@PathVariable UUID id) { service.delete(user(auth),paperId,id); }
    @PostMapping("/{id}/explain") public ChatService.TurnView explain(Authentication auth,@PathVariable UUID paperId,@PathVariable UUID id,@RequestBody Model model) { return service.explain(user(auth),paperId,id,model.model()); }
}
