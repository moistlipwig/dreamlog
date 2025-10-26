package pl.kalin.dreamlog.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.kalin.dreamlog.dream.model.Mood;
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.dto.UserStatsDto;

/**
 * Service for aggregating user statistics from dreams.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatsService {

    private final DreamEntryRepository dreamRepository;

    /**
     * Get statistics for the authenticated user.
     *
     * @param user the authenticated user
     * @return user statistics (total dreams, most common mood)
     */
    public UserStatsDto getUserStats(User user) {
        log.debug("Calculating stats for user: {}", user.getEmail());

        long totalDreams = dreamRepository.countByUserId(user.getId());

        if (totalDreams == 0) {
            return UserStatsDto.empty();
        }

        // Use optimized database query instead of loading all dreams into memory
        Mood mostCommonMood = dreamRepository.findMostCommonMoodByUserId(user.getId())
            .map(Mood::valueOf)
            .orElse(null);

        return new UserStatsDto(totalDreams, mostCommonMood);
    }
}
