package pl.kalin.dreamlog.user;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FederatedIdentityRepository extends JpaRepository<FederatedIdentity, UUID> {
    Optional<FederatedIdentity> findByProviderAndProviderUserId(String provider, String providerUserId);
}
