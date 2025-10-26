package pl.kalin.dreamlog.user.controller;

import jakarta.validation.Valid;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.dto.RegisterRequest;
import pl.kalin.dreamlog.user.dto.SetPasswordRequest;
import pl.kalin.dreamlog.user.dto.UserResponse;
import pl.kalin.dreamlog.user.service.UserService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;

    /**
     * Register new user with email and password.
     * GlobalExceptionHandler handles UserAlreadyExistsException.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.registerWithPassword(request);
        return ResponseEntity.ok(UserResponse.from(user));
    }

    /**
     * Set or change password for authenticated user.
     * Allows OAuth users to add local credentials.
     * Supports both form-based login (UserDetails) and OAuth2 login (OAuth2User).
     */
    @PostMapping("/set-password")
    public ResponseEntity<Map<String, Boolean>> setPassword(
        @Valid @RequestBody SetPasswordRequest request,
        Authentication authentication
    ) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String email = extractEmail(authentication);
        if (email == null) {
            return ResponseEntity.status(401).build();
        }

        User user = userService.findByEmailWithCredentials(email)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        userService.setPassword(user, request.password());
        return ResponseEntity.ok(Map.of("success", true));
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
