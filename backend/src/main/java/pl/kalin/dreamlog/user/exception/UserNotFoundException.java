package pl.kalin.dreamlog.user.exception;

/**
 * Thrown when a user cannot be found.
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String email) {
        super("User not found: " + email);
    }
}
