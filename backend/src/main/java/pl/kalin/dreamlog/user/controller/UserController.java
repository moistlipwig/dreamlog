package pl.kalin.dreamlog.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.dto.UserResponse;
import pl.kalin.dreamlog.user.exception.UserNotFoundException;
import pl.kalin.dreamlog.user.service.UserService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    /**
     * Get current authenticated user information.
     * Supports both form-based login (UserDetails) and OAuth2 login (OAuth2User).
     */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = extractEmail(authentication);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findByEmailWithCredentials(email)
            .orElseThrow(() -> new UserNotFoundException(email));

        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Extract email from either UserDetails (form login) or OAuth2User (OAuth login).
     */
    private String extractEmail(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails userDetails) {
            // Form-based login - username is email
            return userDetails.getUsername();
        }

        if (principal instanceof OAuth2User oAuth2User) {
            // OAuth2 login - email in attributes
            return oAuth2User.getAttribute("email");
        }

        return null;
    }
}
