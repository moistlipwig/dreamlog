package pl.kalin.dreamlog.dream.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.kalin.dreamlog.dream.model.DreamEmbedding;

import java.util.UUID;

public interface DreamEmbeddingRepository extends JpaRepository<DreamEmbedding, UUID> {
}
