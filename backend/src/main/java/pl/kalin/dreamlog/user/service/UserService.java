package pl.kalin.dreamlog.user.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.kalin.dreamlog.user.FederatedIdentity;
import pl.kalin.dreamlog.user.FederatedIdentityRepository;
import pl.kalin.dreamlog.user.LocalCredential;
import pl.kalin.dreamlog.user.LocalCredentialRepository;
import pl.kalin.dreamlog.user.User;
import pl.kalin.dreamlog.user.UserRepository;
import pl.kalin.dreamlog.user.dto.RegisterRequest;
import pl.kalin.dreamlog.user.exception.FederatedIdentityAlreadyLinkedException;
import pl.kalin.dreamlog.user.exception.UserAlreadyExistsException;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final FederatedIdentityRepository federatedIdentityRepository;
    private final LocalCredentialRepository localCredentialRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Register new user with email/password (manual registration)
     */
    public User registerWithPassword(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(request.email());
        }

        User user = User.builder()
            .email(request.email())
            .name(request.name())
            .emailVerified(false) // Will be verified in Phase 6
            .build();

        user = userRepository.save(user);

        // Create local credentials
        LocalCredential credential = LocalCredential.builder()
            .user(user)
            .passwordHash(passwordEncoder.encode(request.password()))
            .build();

        localCredentialRepository.save(credential);
        user.setLocalCredential(credential);

        log.info("User registered with email: {}", user.getEmail());
        return user;
    }

    /**
     * Register or login user via OAuth provider (Google, Facebook, etc.)
     */
    public User registerOrLoginWithOAuth(String provider, String providerUserId, String email, String name) {
        // Check if federated identity already exists
        Optional<FederatedIdentity> existingIdentity =
            federatedIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId);

        if (existingIdentity.isPresent()) {
            // User already registered with this OAuth provider
            User user = existingIdentity.get().getUser();
            user.setLastLoginAt(Instant.now());
            log.info("User logged in via {}: {}", provider, user.getEmail());
            return userRepository.save(user);
        }

        // Check if user exists with this email (account linking scenario)
        Optional<User> existingUser = userRepository.findByEmail(email);

        if (existingUser.isPresent()) {
            // Link OAuth provider to existing account
            User user = existingUser.get();
            linkOAuthProvider(user, provider, providerUserId);
            user.setEmailVerified(true); // OAuth providers verify emails
            user.setLastLoginAt(Instant.now());
            log.info("Linked {} to existing user: {}", provider, user.getEmail());
            return userRepository.save(user);
        }

        // New user - create account with OAuth
        User user = User.builder()
            .email(email)
            .name(name)
            .emailVerified(true) // OAuth providers verify emails
            .lastLoginAt(Instant.now()) // Set login timestamp
            .build();

        user = userRepository.save(user);

        FederatedIdentity identity = FederatedIdentity.builder()
            .user(user)
            .provider(provider)
            .providerUserId(providerUserId)
            .build();

        federatedIdentityRepository.save(identity);
        user.getFederatedIdentities().add(identity);

        log.info("New user registered via {}: {}", provider, user.getEmail());
        return user;
    }

    /**
     * Link OAuth provider to existing user account
     */
    public void linkOAuthProvider(User user, String provider, String providerUserId) {
        // Check if this OAuth identity is already linked to another user
        Optional<FederatedIdentity> existingIdentity =
            federatedIdentityRepository.findByProviderAndProviderUserId(provider, providerUserId);

        if (existingIdentity.isPresent()) {
            if (!existingIdentity.get().getUser().getId().equals(user.getId())) {
                throw new FederatedIdentityAlreadyLinkedException(provider);
            }
            // Already linked to this user, nothing to do
            return;
        }

        FederatedIdentity identity = FederatedIdentity.builder()
            .user(user)
            .provider(provider)
            .providerUserId(providerUserId)
            .build();

        federatedIdentityRepository.save(identity);
        user.getFederatedIdentities().add(identity);
        log.info("Linked {} provider to user: {}", provider, user.getEmail());
    }

    /**
     * Set or update password for user (for OAuth users who want to add local credentials)
     */
    public void setPassword(User user, String password) {
        LocalCredential credential = user.getLocalCredential();

        if (credential == null) {
            // User doesn't have local credentials yet - create them
            credential = LocalCredential.builder()
                .user(user)
                .passwordHash(passwordEncoder.encode(password))
                .build();

            localCredentialRepository.save(credential);
            user.setLocalCredential(credential);
            log.info("Password set for user: {}", user.getEmail());
        } else {
            // Update existing password
            credential.setPasswordHash(passwordEncoder.encode(password));
            credential.setPasswordChangedAt(Instant.now());
            localCredentialRepository.save(credential);
            log.info("Password updated for user: {}", user.getEmail());
        }
    }

    /**
     * Find user by email with all credentials loaded
     */
    @Transactional(readOnly = true)
    public Optional<User> findByEmailWithCredentials(String email) {
        return userRepository.findByEmailWithCredentials(email);
    }

    /**
     * Find user by ID
     */
    @Transactional(readOnly = true)
    public Optional<User> findById(UUID id) {
        return userRepository.findById(id);
    }

    /**
     * Update last login timestamp
     */
    public void updateLastLogin(User user) {
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
    }
}
