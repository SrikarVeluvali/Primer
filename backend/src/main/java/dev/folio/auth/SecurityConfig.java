package dev.folio.auth;

import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService userDetailsService(AccountRepository accounts) {
        return email -> accounts.findByEmail(email).map(a -> User.withUsername(a.email).password(a.passwordHash).roles("USER").build())
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }
    @Bean AuthenticationManager authenticationManager(UserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users); provider.setPasswordEncoder(encoder);
        return new ProviderManager(provider);
    }
    @Bean SecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean SecurityFilterChain security(HttpSecurity http, SecurityContextRepository repository) throws Exception {
        http.authorizeHttpRequests(a -> a.requestMatchers("/", "/index.html", "/favicon.svg", "/assets/**", "/api/auth/session", "/api/auth/login", "/api/auth/register", "/error").permitAll().anyRequest().authenticated())
                .csrf(c -> c.csrfTokenRepository(new HttpSessionCsrfTokenRepository()).csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
                .securityContext(c -> c.securityContextRepository(repository))
                .requestCache(c -> c.disable())
                .formLogin(c -> c.disable()).httpBasic(c -> c.disable()).logout(c -> c.disable())
                .exceptionHandling(e -> e.authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(401); response.setContentType("application/json"); response.getWriter().write("{\"message\":\"Sign in to continue.\"}");
                }).accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(403); response.setContentType("application/json"); response.getWriter().write("{\"message\":\"Session security token expired. Refresh the page and try again.\"}");
                }));
        return http.build();
    }
}
