package com.movento.userservice.exception;

import com.movento.userservice.service.RefreshTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Consistent JSON errors ({"message": ...}) that the web BFF can show to users.
 * Without this, controller exceptions were forwarded to /error, which Spring Security protected,
 * so a duplicate email or a validation error surfaced as a misleading 401.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return body(HttpStatus.BAD_REQUEST, message.isBlank() ? "Invalid request" : message);
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> emailTaken(EmailAlreadyExistsException ex) {
        return body(HttpStatus.CONFLICT, "An account with this email already exists.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> badCredentials(AuthenticationException ex) {
        return body(HttpStatus.UNAUTHORIZED, "Incorrect email or password.");
    }

    @ExceptionHandler(RefreshTokenService.InvalidRefreshTokenException.class)
    public ResponseEntity<Map<String, Object>> invalidRefresh(RefreshTokenService.InvalidRefreshTokenException ex) {
        return body(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({NoSuchElementException.class, ResourceNotFoundException.class})
    public ResponseEntity<Map<String, Object>> notFound(RuntimeException ex) {
        return body(HttpStatus.NOT_FOUND, "Not found");
    }

    @ExceptionHandler(ProfileRuleException.class)
    public ResponseEntity<Map<String, Object>> profileRule(ProfileRuleException ex) {
        return body(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    private static ResponseEntity<Map<String, Object>> body(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status.value(), "message", message));
    }
}
