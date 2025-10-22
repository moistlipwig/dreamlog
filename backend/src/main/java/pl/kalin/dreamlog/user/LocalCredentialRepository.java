package pl.kalin.dreamlog.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface LocalCredentialRepository extends JpaRepository<LocalCredential, UUID> {
    Optional<LocalCredential> findByUser(User user);

    Optional<LocalCredential> findByUserId(UUID userId);
}
