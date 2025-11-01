package pl.kalin.dreamlog.dream.controller;

import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.kalin.dreamlog.common.dto.CreatedResponse;
import pl.kalin.dreamlog.dream.dto.DreamCreateRequest;
import pl.kalin.dreamlog.dream.dto.DreamResponse;
import pl.kalin.dreamlog.dream.dto.DreamUpdateRequest;
import pl.kalin.dreamlog.dream.service.DreamService;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.exception.UserNotFoundException;
import pl.kalin.dreamlog.user.service.UserService;

/**
 * REST controller for dream entry operations.
 * All endpoints require authentication and automatically filter by current user.
 */
@RestController
@RequestMapping("/api/dreams")
@RequiredArgsConstructor
public class DreamController {

    private final DreamService dreamService;
    private final UserService userService;

    /**
     * Get paginated dreams for the authenticated user.
     * Supports pagination and sorting via query parameters.
     *
     * @param page           zero-based page number (default: 0)
     * @param size           number of items per page (default: 20)
     * @param sort           sorting criteria in format "property,direction" (default: "date,desc")
     * @param authentication Spring Security authentication object
     * @return page of dreams with metadata (totalElements, totalPages, etc.)
     * @example GET /api/dreams?page=0&size=5&sort=date,desc
     */
    @GetMapping
    public ResponseEntity<Page<DreamResponse>> getUserDreams(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "date,desc") String sort,
        Authentication authentication) {
        User user = getCurrentUser(authentication);

        // Parse sort parameter (format: "property,direction")
        String[] sortParams = sort.split(",");
        String sortProperty = sortParams[0];
        Sort.Direction sortDirection = sortParams.length > 1 && sortParams[1].equalsIgnoreCase("asc")
            ? Sort.Direction.ASC
            : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortProperty));
        Page<DreamResponse> dreams = dreamService.getUserDreams(user, pageable);
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
     * Following CQRS: Returns only the ID with 201 Created and Location header.
     * Client should use the Location header or ID to fetch the full dream if needed.
     */
    @PostMapping
    public ResponseEntity<CreatedResponse> createDream(
        @Valid @RequestBody DreamCreateRequest request,
        Authentication authentication) {
        User user = getCurrentUser(authentication);
        UUID dreamId = dreamService.createDream(user, request);
        return ResponseEntity
            .created(URI.create("/api/dreams/" + dreamId))
            .body(new CreatedResponse(dreamId));
    }

    /**
     * Update an existing dream (PUT - full replacement).
     * Only the owner can update their dream.
     * Following CQRS: Returns 204 No Content.
     * Client should refetch the dream if they need the updated data.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateDream(
        @PathVariable UUID id,
        @Valid @RequestBody DreamUpdateRequest request,
        Authentication authentication) {
        User user = getCurrentUser(authentication);
        dreamService.updateDream(user, id, request);
        return ResponseEntity.noContent().build();
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
     * Search dreams by query string (full-text search).
     * Minimum 3 characters required in query.
     *
     * @param query          search query string
     * @param authentication Spring Security authentication object
     * @return list of matching dreams for the authenticated user
     * @example GET /api/dreams/search?query=lucid
     */
    @GetMapping("/search")
    public ResponseEntity<List<DreamResponse>> searchDreams(
        @RequestParam String query,
        Authentication authentication) {
        User user = getCurrentUser(authentication);

        // Validate minimum query length
        if (query == null || query.trim().length() < 3) {
            return ResponseEntity.ok(List.of());
        }

        List<DreamResponse> results = dreamService.searchDreams(user, query.trim());
        return ResponseEntity.ok(results);
    }

    /**
     * Helper method to get current authenticated user from database.
     * Supports both form login (UserDetails) and OAuth2 login (OAuth2User).
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

        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof org.springframework.security.oauth2.core.user.OAuth2User oAuth2User) {
            return oAuth2User.getAttribute("email");
        }

        throw new IllegalStateException("Unknown authentication principal type");
    }
}
