package dev.folio.personal;

import dev.folio.auth.AccountRepository;
import dev.folio.paper.PaperService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.time.Instant;

@RestController
@RequestMapping("/api")
public class PersonalController {
    private final PersonalStore store;
    private final AccountRepository accounts;
    private final PaperService papers;
    public PersonalController(PersonalStore store,AccountRepository accounts,PaperService papers) { this.store=store; this.accounts=accounts; this.papers=papers; }
    private UUID user(Authentication auth) { return accounts.findByEmail(auth.getName()).orElseThrow().id; }
    public record Note(String text,Instant updatedAt) {}
    public record NoteInput(@NotNull @Size(max=100000) String text) {}
    public record Theme(String id,String name,String mode,String background,String surface,String panel,String text,String muted,String accent,String border) {}
    public record Preferences(String activeTheme,List<Theme> themes) {}
    @GetMapping("/papers/{id}/notes") public Note notes(@PathVariable UUID id,Authentication auth) { papers.get(id); return store.get(user(auth),"notes:"+id,Note.class,new Note("",null)); }
    @PutMapping("/papers/{id}/notes") public Note notes(@PathVariable UUID id,@Valid @RequestBody NoteInput body,Authentication auth) { papers.get(id); return store.put(user(auth),"notes:"+id,new Note(body.text(),Instant.now())); }
    @GetMapping("/preferences") public Preferences preferences(Authentication auth) { return store.get(user(auth),"preferences",Preferences.class,new Preferences("dark",List.of())); }
    @PutMapping("/preferences") public Preferences preferences(@RequestBody Preferences body,Authentication auth) {
        if(body==null || body.themes()==null || body.themes().size()>20) throw new IllegalArgumentException("Save up to 20 custom themes.");
        Set<String> ids=new HashSet<>();
        for(var theme:body.themes()) {
            if(theme==null || theme.id()==null || !theme.id().matches("[a-zA-Z0-9-]{1,60}") || Set.of("light","dark").contains(theme.id()) || !ids.add(theme.id())) throw new IllegalArgumentException("Theme identifiers must be unique.");
            if(theme.name()==null || theme.name().isBlank() || theme.name().length()>60 || theme.mode()==null || !Set.of("light","dark").contains(theme.mode())) throw new IllegalArgumentException("Give each theme a name and a base mode.");
            for(var color:new String[]{theme.background(),theme.surface(),theme.panel(),theme.text(),theme.muted(),theme.accent(),theme.border()}) if(color==null || !color.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException("Theme colors must be six-digit hex colors.");
        }
        if(body.activeTheme()==null || (!Set.of("light","dark").contains(body.activeTheme()) && !ids.contains(body.activeTheme()))) throw new IllegalArgumentException("Choose a saved theme.");
        return store.put(user(auth),"preferences",body);
    }
}
