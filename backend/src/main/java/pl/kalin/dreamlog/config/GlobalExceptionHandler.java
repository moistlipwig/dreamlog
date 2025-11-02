package pl.kalin.dreamlog.config;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pl.kalin.dreamlog.user.exception.AuthenticationRequiredException;
import pl.kalin.dreamlog.user.exception.FederatedIdentityAlreadyLinkedException;
import pl.kalin.dreamlog.user.exception.UserAlreadyExistsException;
import pl.kalin.dreamlog.user.exception.UserNotFoundException;

/**
 * Global exception handler for REST API endpoints.
 * Converts domain exceptions into appropriate HTTP responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle validation errors from @Valid annotations.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));

        return ResponseEntity
            .badRequest()
            .body(Map.of("error", errorMessage));
    }

    /**
     * Handle user already exists (duplicate registration).
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity
            .badRequest()
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handle authentication required (401).
     * Triggered when user is not authenticated or authentication is invalid.
     */
    @ExceptionHandler(AuthenticationRequiredException.class)
    public ResponseEntity<Map<String, String>> handleAuthenticationRequired(AuthenticationRequiredException ex) {
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handle user not found (404).
     * Triggered when authenticated session points to deleted user.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(Map.of("error", ex.getMessage()));
    }

    /**
     * Handle federated identity already linked to another user.
     */
    @ExceptionHandler(FederatedIdentityAlreadyLinkedException.class)
    public ResponseEntity<Map<String, String>> handleFederatedIdentityAlreadyLinked(
        FederatedIdentityAlreadyLinkedException ex
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(Map.of("error", ex.getMessage()));
    }

}
