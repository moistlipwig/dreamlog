package pl.kalin.dreamlog.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import pl.kalin.dreamlog.user.validation.PasswordStrength;

/**
 * Request to register a new user with email and password.
 */
public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    String email,

    @NotBlank(message = "Password is required")
    @PasswordStrength
    String password,

    String name
) {}
