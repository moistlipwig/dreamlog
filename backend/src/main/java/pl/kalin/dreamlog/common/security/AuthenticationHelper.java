package pl.kalin.dreamlog.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import pl.kalin.dreamlog.user.User;
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
     * @throws IllegalArgumentException if user not found or authentication invalid
     */
    public User getCurrentUser(Authentication authentication) {
        String email = extractEmail(authentication);
        return userService.findByEmailWithCredentials(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));
    }

    /**
     * Extract email from Authentication principal.
     * Handles both UserDetails (form login) and OAuth2User (OAuth login).
     *
     * @param authentication Spring Security authentication object
     * @return email address
     * @throws IllegalArgumentException if principal type is unknown
     */
    public String extractEmail(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalArgumentException("User not authenticated");
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
                throw new IllegalArgumentException("OAuth2 user has no email attribute");
            }
            return email;
        }

        throw new IllegalArgumentException("Unknown principal type: " + principal.getClass().getName());
    }
}
