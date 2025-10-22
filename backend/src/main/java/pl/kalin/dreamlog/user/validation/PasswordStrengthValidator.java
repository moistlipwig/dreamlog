package pl.kalin.dreamlog.user.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validator for {@link PasswordStrength} annotation.
 */
public class PasswordStrengthValidator implements ConstraintValidator<PasswordStrength, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return false;
        }

        // Length check
        if (password.length() < 8 || password.length() > 100) {
            return false;
        }

        // Must contain at least one letter
        boolean hasLetter = password.chars().anyMatch(Character::isLetter);

        // Must contain at least one digit
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);

        return hasLetter && hasDigit;
    }
}
