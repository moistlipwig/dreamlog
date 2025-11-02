package pl.kalin.dreamlog.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.exception.AuthenticationRequiredException;
import pl.kalin.dreamlog.user.exception.UserNotFoundException;
import pl.kalin.dreamlog.user.service.UserService;

@Component
@RequiredArgsConstructor
public class AuthenticationHelper {
    private final UserService userService;

    /**
     * Get current authenticated user from Spring Security Authentication.
     *
     * @param authentication Spring Security authentication object
     * @return User entity from database
     * @throws AuthenticationRequiredException if authentication is null or invalid (401)
     * @throws UserNotFoundException           if authenticated user doesn't exist in database (404)
     */
    public User getCurrentUser(Authentication authentication) {
        String email = extractEmail(authentication);
        return userService.findByEmailWithCredentials(email)
            .orElseThrow(() -> new UserNotFoundException(email));
    }

    /**
     * Extract email from Authentication principal.
     * Handles both UserDetails (form login) and OAuth2User (OAuth login).
     *
     * @param authentication Spring Security authentication object
     * @return email address
     * @throws AuthenticationRequiredException if authentication is null, not authenticated, or principal is invalid
     */
    public String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthenticationRequiredException("User not authenticated");
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            // Form-based login - username is email
            return userDetails.getUsername();
        }

        if (principal instanceof OAuth2User oAuth2User) {
            // OAuth2 login - email in attributes
            String email = oAuth2User.getAttribute("email");
            if (email == null) {
                throw new AuthenticationRequiredException("OAuth2 user has no email attribute");
            }
            return email;
        }

        throw new AuthenticationRequiredException("Unknown principal type: " + principal.getClass().getName());
    }
}
