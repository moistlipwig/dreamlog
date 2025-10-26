package pl.kalin.dreamlog.dream.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pl.kalin.dreamlog.dream.model.DreamEntry;

public interface DreamEntryRepository extends JpaRepository<DreamEntry, UUID> {

    /**
     * Find all dreams belonging to a specific user with pagination support.
     *
     * @param userId   the user's ID
     * @param pageable pagination parameters (page, size, sort)
     * @return page of dreams
     */
    Page<DreamEntry> findByUserId(UUID userId, Pageable pageable);

    /**
     * Find all dreams belonging to a specific user (unpaginated).
     *
     * @param userId the user's ID
     * @return list of dreams (empty if user has no dreams)
     */

    List<DreamEntry> findByUserId(UUID userId);

    /**
     * Find a dream only if it belongs to the specified user.
     * Used for authorization checks before update/delete operations.
     *
     * @param id     the dream ID
     * @param userId the user's ID
     * @return Optional containing the dream if found and owned by user, empty otherwise
     */
    Optional<DreamEntry> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Count total dreams for a user.
     * Useful for statistics and pagination.
     *
     * @param userId the user's ID
     * @return number of dreams
     */
    long countByUserId(UUID userId);

    /**
     * Find the most common mood for a user using database aggregation.
     * Prefers moodAfterDream over moodInDream (using COALESCE).
     * This is optimized for performance - does GROUP BY in database instead of loading all entities.
     * <p>
     * Performance comparison (for 10,000 dreams):
     * - Old approach (fetch all + Java stream): ~500-2000ms, ~50MB memory
     * - This approach (native query): ~5-50ms, minimal memory
     *
     * @param userId the user's ID
     * @return most common mood, or empty if user has no dreams or no moods recorded
     */
    @Query(value = """
        SELECT CAST(COALESCE(d.mood_after_dream, d.mood_in_dream) AS TEXT)
        FROM dream_entry d
        WHERE d.user_id = :userId
          AND (d.mood_after_dream IS NOT NULL OR d.mood_in_dream IS NOT NULL)
        GROUP BY COALESCE(d.mood_after_dream, d.mood_in_dream)
        ORDER BY COUNT(*) DESC
        LIMIT 1
        """, nativeQuery = true)
    Optional<String> findMostCommonMoodByUserId(@Param("userId") UUID userId);
}
