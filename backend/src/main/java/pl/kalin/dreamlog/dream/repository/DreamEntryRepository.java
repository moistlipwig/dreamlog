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

    @Query(value = """
        SELECT d.*
        FROM dream_entry d
        WHERE d.user_id = :userId
          AND d.search_vector @@ websearch_to_tsquery('simple', unaccent(:searchQuery))
        ORDER BY ts_rank(d.search_vector, websearch_to_tsquery('simple', unaccent(:searchQuery))) DESC
        LIMIT 100
        """, nativeQuery = true)
    List<DreamEntry> searchByFullText(@Param("userId") UUID userId, @Param("searchQuery") String searchQuery);

    /**
     * Fuzzy search using PostgreSQL's trigram similarity (pg_trgm).
     * Fallback for FTS when no results found. Tolerates typos (e.g., "lucdi" → "lucid").
     * Uses similarity() function with threshold 0.2 (20% similarity required).
     * <p>
     * Performance note: Slower than FTS but more forgiving for typos.
     * Uses GIN indexes on title and content for optimization.
     *
     * @param userId      the user's ID (security filter)
     * @param searchQuery fuzzy query string
     * @return list of matching dreams ordered by similarity (max 100 results)
     */
    @Query(value = """
        SELECT d.*, GREATEST(
            similarity(d.title, :searchQuery),
            similarity(d.content, :searchQuery)
        ) AS similarity_score
        FROM dream_entry d
        WHERE d.user_id = :userId
          AND (
              similarity(d.title, :searchQuery) > 0.2
              OR similarity(d.content, :searchQuery) > 0.2
          )
        ORDER BY similarity_score DESC
        LIMIT 100
        """, nativeQuery = true)
    List<DreamEntry> searchByFuzzy(@Param("userId") UUID userId, @Param("searchQuery") String searchQuery);
}
