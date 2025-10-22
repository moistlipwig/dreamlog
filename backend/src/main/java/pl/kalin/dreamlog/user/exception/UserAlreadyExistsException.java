package pl.kalin.dreamlog.user.exception;

/**
 * Thrown when attempting to register a user with an email that already exists.
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super("User with email '" + email + "' is already registered");
    }
}
