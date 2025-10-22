package pl.kalin.dreamlog.user.exception;

/**
 * Thrown when attempting to link an OAuth identity that is already linked to another user.
 */
public class FederatedIdentityAlreadyLinkedException extends RuntimeException {
    public FederatedIdentityAlreadyLinkedException(String provider) {
        super("This " + provider + " account is already linked to another user");
    }
}
