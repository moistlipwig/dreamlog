package pl.kalin.dreamlog.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.dto.UserStatsDto;
import pl.kalin.dreamlog.user.exception.UserNotFoundException;
import pl.kalin.dreamlog.user.service.StatsService;
import pl.kalin.dreamlog.user.service.UserService;

/**
 * REST controller for user statistics.
 * Provides aggregated data for dashboard and analytics.
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final UserService userService;

    /**
     * Get statistics for the authenticated user.
     * Includes total dreams count and most common mood.
     *
     * @param authentication Spring Security authentication object
     * @return user statistics
     *
     * @example GET /api/stats/me
     */
    @GetMapping("/me")
    public ResponseEntity<UserStatsDto> getMyStats(Authentication authentication) {
        User user = getCurrentUser(authentication);
        UserStatsDto stats = statsService.getUserStats(user);
        return ResponseEntity.ok(stats);
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
            return userDetails.getUsername();
        }

        if (principal instanceof OAuth2User oAuth2User) {
            return oAuth2User.getAttribute("email");
        }

        throw new IllegalStateException("Unknown authentication principal type");
    }
}
