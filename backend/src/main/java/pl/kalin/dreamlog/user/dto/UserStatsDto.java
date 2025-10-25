package pl.kalin.dreamlog.user.dto;

import pl.kalin.dreamlog.dream.model.Mood;

/**
 * DTO for user statistics displayed on dashboard.
 * Aggregated data from user's dreams.
 */
public record UserStatsDto(
    long totalDreams,
    Mood mostCommonMood
) {
    /**
     * Factory method for creating stats with defaults when no dreams exist.
     * @return UserStatsDto with zero dreams and null mood
     */
    public static UserStatsDto empty() {
        return new UserStatsDto(0, null);
    }
}
