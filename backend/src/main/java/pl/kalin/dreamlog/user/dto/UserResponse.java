package pl.kalin.dreamlog.user.dto;

import java.util.List;
import java.util.UUID;

import pl.kalin.dreamlog.user.FederatedIdentity;
import pl.kalin.dreamlog.user.User;

public record UserResponse(
    UUID id,
    String email,
    String name,
    boolean emailVerified,
    List<String> providers, // List of linked OAuth providers
    boolean hasPassword // Whether user has local credentials
) {
    public static UserResponse from(User user) {
        List<String> providers = user.getFederatedIdentities().stream()
            .map(FederatedIdentity::getProvider)
            .toList();

        return new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getName(),
            user.getEmailVerified(),
            providers,
            user.hasPassword()
        );
    }
}
