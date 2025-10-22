package pl.kalin.dreamlog.dream.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.kalin.dreamlog.dream.model.DreamEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DreamEntryRepository extends JpaRepository<DreamEntry, UUID> {

    /**
     * Find all dreams belonging to a specific user.
     * @param userId the user's ID
     * @return list of dreams (empty if user has no dreams)
     */
    List<DreamEntry> findByUserId(UUID userId);

    /**
     * Find a dream only if it belongs to the specified user.
     * Used for authorization checks before update/delete operations.
     * @param id the dream ID
     * @param userId the user's ID
     * @return Optional containing the dream if found and owned by user, empty otherwise
     */
    Optional<DreamEntry> findByIdAndUserId(UUID id, UUID userId);

    /**
     * Count total dreams for a user.
     * Useful for statistics and pagination.
     * @param userId the user's ID
     * @return number of dreams
     */
    long countByUserId(UUID userId);
}
