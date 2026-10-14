package dev.folio.auth;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountRepository accounts;
    private final org.springframework.security.crypto.password.PasswordEncoder encoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contexts;
    public AuthController(AccountRepository accounts, org.springframework.security.crypto.password.PasswordEncoder encoder,
                          AuthenticationManager manager, SecurityContextRepository contexts) {
        this.accounts = accounts; this.encoder = encoder; this.authenticationManager = manager; this.contexts = contexts;
    }
    public record UserView(UUID id, String name, String email) {}
    public record SessionView(UserView user, String csrfToken) {}
    public record Login(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {}
    public record Register(@NotBlank @Size(max = 80) String name, @NotBlank @Email @Size(max = 254) String email,
                           @NotBlank @Size(min = 10, max = 72) String password) {}
    private UserView view(Account account) { return new UserView(account.id, account.name, account.email); }
    @GetMapping("/session")
    public SessionView session(Authentication auth, CsrfToken token) {
        var user = auth == null ? null : accounts.findByEmail(auth.getName()).map(this::view).orElse(null);
        return new SessionView(user, token.getToken());
    }
    @PostMapping("/register")
    public UserView register(@Valid @RequestBody Register input, HttpServletRequest request, HttpServletResponse response) {
        String email = input.email().trim().toLowerCase(Locale.ROOT);
        if (input.password().getBytes(StandardCharsets.UTF_8).length > 72) throw new IllegalArgumentException("Password must be at most 72 UTF-8 bytes.");
        if (accounts.findByEmail(email).isPresent()) throw new ResponseStatusException(HttpStatus.CONFLICT, "An account with this email already exists. Sign in instead.");
        Account account = accounts.saveAndFlush(new Account(email, input.name().trim(), encoder.encode(input.password())));
        signIn(email, input.password(), request, response); return view(account);
    }
    @PostMapping("/login")
    public UserView login(@Valid @RequestBody Login input, HttpServletRequest request, HttpServletResponse response) {
        String email = input.email().trim().toLowerCase(Locale.ROOT);
        signIn(email, input.password(), request, response); return view(accounts.findByEmail(email).orElseThrow());
    }
    private void signIn(String email, String password, HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, password));
        if (request.getSession(false) != null) request.changeSessionId();
        var context = SecurityContextHolder.createEmptyContext(); context.setAuthentication(auth);
        SecurityContextHolder.setContext(context); contexts.saveContext(context, request, response);
        new HttpSessionCsrfTokenRepository().saveToken(null, request, response);
    }
    @PostMapping("/logout")
    public void logout(HttpServletRequest request) {
        if (request.getSession(false) != null) request.getSession(false).invalidate();
        SecurityContextHolder.clearContext();
    }
}
