package pl.kalin.dreamlog.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.kalin.dreamlog.dream.model.DreamEntry;
import pl.kalin.dreamlog.dream.model.Mood;
import pl.kalin.dreamlog.dream.repository.DreamEntryRepository;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.dto.UserStatsDto;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * @param user the authenticated user
     * @return user statistics (total dreams, most common mood)
     */
    public UserStatsDto getUserStats(User user) {
        log.debug("Calculating stats for user: {}", user.getEmail());

        long totalDreams = dreamRepository.countByUserId(user.getId());

        if (totalDreams == 0) {
            return UserStatsDto.empty();
        }

        // Find most common mood by fetching all dreams (we can optimize later with custom query)
        List<DreamEntry> dreams = dreamRepository.findByUserId(user.getId());
        Mood mostCommonMood = calculateMostCommonMood(dreams);

        return new UserStatsDto(totalDreams, mostCommonMood);
    }

    /**
     * Calculate the most common mood from user's dreams.
     * Uses moodAfterDream if available, falls back to moodInDream.
     * @param dreams list of dream entries
     * @return most common mood or null if no moods recorded
     */
    private Mood calculateMostCommonMood(List<DreamEntry> dreams) {
        Map<Mood, Long> moodCounts = dreams.stream()
            .map(dream -> dream.getMoodAfterDream() != null ? dream.getMoodAfterDream() : dream.getMoodInDream())
            .filter(mood -> mood != null)
            .collect(Collectors.groupingBy(mood -> mood, Collectors.counting()));

        if (moodCounts.isEmpty()) {
            return null;
        }

        return moodCounts.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(null);
    }
}
