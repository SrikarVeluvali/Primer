package dev.folio.learn;

import dev.folio.auth.AccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import static dev.folio.learn.LearningTypes.*;

@RestController
@RequestMapping("/api/papers/{paperId}/learn")
public class LearningController {
    private final LearningService service;
    private final AccountRepository accounts;
    public LearningController(LearningService service,AccountRepository accounts) { this.service=service; this.accounts=accounts; }
    private UUID user(Authentication auth) { return accounts.findByEmail(auth.getName()).orElseThrow().id; }
    public record Model(@Size(max=100) String model) {}
    public record PlanInput(Answers answers,@Size(max=100) String model) {}
    public record Progress(Set<String> completedSteps) {}
    @GetMapping public State get(@PathVariable UUID paperId,Authentication auth) { return service.get(user(auth),paperId); }
    @PostMapping("/assessment") public State assess(@PathVariable UUID paperId,@Valid @RequestBody Model input,Authentication auth) { return service.assess(user(auth),paperId,input.model()); }
    @PutMapping("/answers") public State answers(@PathVariable UUID paperId,@RequestBody Answers input,Authentication auth) { return service.answers(user(auth),paperId,input); }
    @PostMapping("/plan") public State plan(@PathVariable UUID paperId,@Valid @RequestBody PlanInput input,Authentication auth) { return service.plan(user(auth),paperId,input.answers(),input.model()); }
    @PutMapping("/progress") public State progress(@PathVariable UUID paperId,@RequestBody Progress input,Authentication auth) { return service.progress(user(auth),paperId,input.completedSteps()); }
    @PostMapping("/resources") public State resources(@PathVariable UUID paperId,Authentication auth) { return service.resources(user(auth),paperId); }
}
