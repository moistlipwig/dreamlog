package pl.kalin.dreamlog.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Validates password strength.
 * Password must:
 * - Be between 8-100 characters
 * - Contain at least one letter
 * - Contain at least one digit
 */
@Documented
@Constraint(validatedBy = PasswordStrengthValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordStrength {
    String message() default "Password must be 8-100 characters and contain at least one letter and one digit";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
