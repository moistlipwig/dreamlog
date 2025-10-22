package pl.kalin.dreamlog.user.service;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import lombok.extern.slf4j.Slf4j;
import pl.kalin.dreamlog.user.User;

/**
 * Custom success handler for OAuth2 login (Google, Facebook, etc.)
 * Creates or updates User entity after successful OAuth authentication.
 */
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserService userService;

    public OAuth2SuccessHandler(UserService userService, String redirectUrl) {
        this.userService = userService;
        setDefaultTargetUrl(redirectUrl);
        setAlwaysUseDefaultTargetUrl(true);
        log.info("OAuth2SuccessHandler initialized with redirect URL: {}", redirectUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        log.debug("OAuth2 authentication success - URI: {}", request.getRequestURI());

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // Extract user info from OAuth2 provider
        String provider = extractProvider(authentication);
        String providerUserId = extractProviderUserId(provider, oAuth2User);
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        log.info("OAuth2 login - Provider: {}, Email: {}", provider, email);

        // Register or login user (creates User and FederatedIdentity if needed)
        User user = userService.registerOrLoginWithOAuth(provider, providerUserId, email, name);

        log.debug("User processed - Email: {}, ID: {}", user.getEmail(), user.getId());

        // Continue with default redirect behavior
        try {
            super.onAuthenticationSuccess(request, response, authentication);
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Extract OAuth provider name from Authentication object
     * Uses OAuth2AuthenticationToken's authorized client registration ID
     */
    private String extractProvider(Authentication authentication) {
        if (authentication instanceof org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken oauthToken) {
            return oauthToken.getAuthorizedClientRegistrationId(); // Returns "google", "facebook", etc.
        }

        log.warn("Cannot extract provider from authentication, defaulting to 'google'");
        return "google"; // Fallback for tests using mock authentication
    }

    /**
     * Extract provider-specific user ID from OAuth2User attributes.
     * Different providers use different attribute names:
     * - Google: "sub" (OpenID Connect standard)
     * - Facebook: "id" (Facebook's custom attribute)
     * - GitHub: "id"
     */
    private String extractProviderUserId(String provider, OAuth2User oAuth2User) {
        return switch (provider.toLowerCase()) {
            case "google" -> oAuth2User.getAttribute("sub");
            case "facebook", "github" -> {
                Object id = oAuth2User.getAttribute("id");
                yield id != null ? id.toString() : null;
            }
            default -> {
                // Fallback: try 'sub' first (OpenID Connect standard), then 'id'
                String sub = oAuth2User.getAttribute("sub");
                if (sub != null) {
                    yield sub;
                }
                Object id = oAuth2User.getAttribute("id");
                yield id != null ? id.toString() : null;
            }
        };
    }
}
