package pl.kalin.dreamlog.user.dto;

import jakarta.validation.constraints.NotBlank;
import pl.kalin.dreamlog.user.validation.PasswordStrength;

/**
 * Request to set or change user password.
 */
public record SetPasswordRequest(
    @NotBlank(message = "Password is required")
    @PasswordStrength
    String password
) {}
