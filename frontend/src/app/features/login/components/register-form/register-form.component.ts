import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';

/**
 * Custom validator to check if password contains at least one letter and one digit
 */
function passwordStrengthValidator(control: FormControl): {[key: string]: boolean} | null {
  const value = control.value as string;
  if (!value) {
    return null; // Let required validator handle empty values
  }

  const hasLetter = /[a-zA-Z]/.test(value);
  const hasDigit = /\d/.test(value);

  if (!hasLetter || !hasDigit) {
    return { passwordStrength: true };
  }

  return null;
}

@Component({
  selector: 'app-register-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './register-form.component.html',
  styleUrls: ['./register-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class RegisterFormComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  registerForm = new FormGroup({
    email: new FormControl('', {
      validators: [Validators.required, Validators.email],
      nonNullable: true,
    }),
    name: new FormControl('', {
      nonNullable: true,
    }),
    password: new FormControl('', {
      validators: [
        Validators.required,
        Validators.minLength(8),
        Validators.maxLength(100),
        passwordStrengthValidator,
      ],
      nonNullable: true,
    }),
  });

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  onSubmit(): void {
    if (this.registerForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set(null);

      const formValue = this.registerForm.getRawValue();

      this.authService.register(formValue).subscribe({
        next: () => {
          this.isLoading.set(false);
          void this.router.navigateByUrl('/app');
        },
        error: (error) => {
          this.isLoading.set(false);
          const message = error.error?.error || 'Registration failed. Please try again.';
          this.errorMessage.set(message);
        },
      });
    }
  }
}
