package pl.kalin.dreamlog.dream.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.kalin.dreamlog.dream.model.DreamAnalysis;

import java.util.Optional;
import java.util.UUID;

public interface DreamAnalysisRepository extends JpaRepository<DreamAnalysis, UUID> {

    /**
     * Find dream analysis by dream ID.
     * Used for idempotency checks in async tasks.
     */
    Optional<DreamAnalysis> findByDreamId(UUID dreamId);
}
