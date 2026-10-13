package dev.folio.api;

import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.DataIntegrityViolationException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<?> badInput(IllegalArgumentException e) { return error(HttpStatus.BAD_REQUEST, e.getMessage()); }
    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<?> state(IllegalStateException e) { return error(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage()); }
    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<?> credentials(AuthenticationException e) { return error(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."); }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<?> validation(MethodArgumentNotValidException e) {
        var field = e.getBindingResult().getFieldErrors().getFirst();
        return error(HttpStatus.BAD_REQUEST, field.getField() + " " + field.getDefaultMessage());
    }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<?> status(ResponseStatusException e) { return ResponseEntity.status(e.getStatusCode()).body(Map.of("message", e.getReason() == null ? "Request failed." : e.getReason())); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<?> conflict(DataIntegrityViolationException e) { return error(HttpStatus.CONFLICT, "This item already exists. Refresh and try again."); }
    @ExceptionHandler(Exception.class)
    ResponseEntity<?> unexpected(Exception e) { return error(HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be completed. Check the server log and try again."); }
    private ResponseEntity<?> error(HttpStatus status, String message) { return ResponseEntity.status(status).body(Map.of("message", message)); }
}
