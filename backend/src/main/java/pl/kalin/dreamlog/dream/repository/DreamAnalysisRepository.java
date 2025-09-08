package pl.kalin.dreamlog.dream.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.kalin.dreamlog.dream.model.DreamAnalysis;

import java.util.UUID;

public interface DreamAnalysisRepository extends JpaRepository<DreamAnalysis, UUID> {
}
