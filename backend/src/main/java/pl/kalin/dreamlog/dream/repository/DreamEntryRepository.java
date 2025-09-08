package pl.kalin.dreamlog.dream.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.kalin.dreamlog.dream.model.DreamEntry;

import java.util.UUID;

public interface DreamEntryRepository extends JpaRepository<DreamEntry, UUID> {
}
