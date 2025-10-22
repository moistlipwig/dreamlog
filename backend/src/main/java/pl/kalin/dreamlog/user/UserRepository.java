package pl.kalin.dreamlog.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.federatedIdentities LEFT JOIN FETCH u.localCredential WHERE u.email = :email")
    Optional<User> findByEmailWithCredentials(String email);

    boolean existsByEmail(String email);
}
