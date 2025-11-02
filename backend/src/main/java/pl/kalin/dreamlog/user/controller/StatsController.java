package pl.kalin.dreamlog.user.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import pl.kalin.dreamlog.common.security.AuthenticationHelper;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.dto.UserStatsDto;
import pl.kalin.dreamlog.user.service.StatsService;

/**
 * REST controller for user statistics.
 * Provides aggregated data for dashboard and analytics.
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;
    private final AuthenticationHelper authHelper;

    /**
     * Get statistics for the authenticated user.
     * Includes total dreams count and most common mood.
     */
    @GetMapping("/me")
    public ResponseEntity<UserStatsDto> getMyStats(Authentication authentication) {
        User user = authHelper.getCurrentUser(authentication);
        UserStatsDto stats = statsService.getUserStats(user);
        return ResponseEntity.ok(stats);
    }
}
