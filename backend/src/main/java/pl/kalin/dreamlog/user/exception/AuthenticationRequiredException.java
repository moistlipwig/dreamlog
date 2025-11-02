package pl.kalin.dreamlog.user.exception;

/**
 * Thrown when a request requires authentication but the user is not authenticated
 * or the authentication is invalid.
 * Maps to HTTP 401 Unauthorized.
 */
public class AuthenticationRequiredException extends RuntimeException {
    public AuthenticationRequiredException(String message) {
        super(message);
    }
}
