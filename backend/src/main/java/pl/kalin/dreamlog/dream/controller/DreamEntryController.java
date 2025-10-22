package pl.kalin.dreamlog.dream.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import pl.kalin.dreamlog.dream.dto.DreamCreateRequest;
import pl.kalin.dreamlog.dream.dto.DreamResponse;
import pl.kalin.dreamlog.dream.dto.DreamUpdateRequest;
import pl.kalin.dreamlog.dream.service.DreamService;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.exception.UserNotFoundException;
import pl.kalin.dreamlog.user.service.UserService;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for dream entry operations.
 * All endpoints require authentication and automatically filter by current user.
 */
@RestController
@RequestMapping("/api/dreams")
@RequiredArgsConstructor
public class DreamEntryController {

    private final DreamService dreamService;
    private final UserService userService;

    /**
     * Get all dreams for the authenticated user.
     */
    @GetMapping
    public ResponseEntity<List<DreamResponse>> getUserDreams(Authentication authentication) {
        User user = getCurrentUser(authentication);
        List<DreamResponse> dreams = dreamService.getUserDreams(user);
        return ResponseEntity.ok(dreams);
    }

    /**
     * Get a single dream by ID (only if it belongs to authenticated user).
     */
    @GetMapping("/{id}")
    public ResponseEntity<DreamResponse> getDreamById(
            @PathVariable UUID id,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        DreamResponse dream = dreamService.getDreamById(user, id);
        return ResponseEntity.ok(dream);
    }

    /**
     * Create a new dream entry for the authenticated user.
     */
    @PostMapping
    public ResponseEntity<DreamResponse> createDream(
            @Valid @RequestBody DreamCreateRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        DreamResponse created = dreamService.createDream(user, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing dream (PUT - full replacement).
     * Only the owner can update their dream.
     */
    @PutMapping("/{id}")
    public ResponseEntity<DreamResponse> updateDream(
            @PathVariable UUID id,
            @Valid @RequestBody DreamUpdateRequest request,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        DreamResponse updated = dreamService.updateDream(user, id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a dream entry.
     * Only the owner can delete their dream.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDream(
            @PathVariable UUID id,
            Authentication authentication) {
        User user = getCurrentUser(authentication);
        dreamService.deleteDream(user, id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Helper method to get current authenticated user from database.
     * Supports both form login (UserDetails) and OAuth2 login (OAuth2User).
     * @throws UserNotFoundException if authenticated user not found in database
     */
    private User getCurrentUser(Authentication authentication) {
        String email = extractEmail(authentication);
        return userService.findByEmailWithCredentials(email)
            .orElseThrow(() -> new UserNotFoundException(email));
    }

    /**
     * Extract email from Authentication principal.
     * Supports both UserDetails (form login) and OAuth2User (OAuth login).
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

        throw new IllegalStateException("Unknown authentication principal type");
    }
}
