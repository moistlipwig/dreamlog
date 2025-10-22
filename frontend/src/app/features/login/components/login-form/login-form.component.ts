import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';

@Component({
  selector: 'app-login-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginFormComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Track which fields have been touched (blurred)
  touchedFields = signal<Set<string>>(new Set());
  formSubmitted = signal(false);

  loginForm = new FormGroup({
    username: new FormControl('', {
      validators: [Validators.required, Validators.email],
      nonNullable: true,
    }),
    password: new FormControl('', {
      validators: [Validators.required, Validators.minLength(8)],
      nonNullable: true,
    }),
  });

  isLoading = signal(false);
  errorMessage = signal<string | null>(null);

  onFieldBlur(fieldName: string): void {
    const touched = new Set(this.touchedFields());
    touched.add(fieldName);
    this.touchedFields.set(touched);
  }

  shouldShowError(fieldName: string): boolean {
    return this.formSubmitted() || this.touchedFields().has(fieldName);
  }

  onSubmit(): void {
    this.formSubmitted.set(true);

    if (this.loginForm.valid) {
      this.isLoading.set(true);
      this.errorMessage.set(null);

      const { username, password } = this.loginForm.getRawValue();

      this.authService.login(username, password).subscribe({
        next: () => {
          this.isLoading.set(false);
          void this.router.navigateByUrl('/app');
        },
        error: (error) => {
          this.isLoading.set(false);
          const message = error.error?.error || 'Login failed. Please check your credentials.';
          this.errorMessage.set(message);
        },
      });
    } else {
      // Scroll to first invalid field
      this.scrollToFirstError();
    }
  }

  private scrollToFirstError(): void {
    const firstInvalidControl = Object.keys(this.loginForm.controls).find((key) => this.loginForm.get(key)?.invalid);

    if (firstInvalidControl) {
      const element = document.querySelector(`[formControlName="${firstInvalidControl}"]`) as HTMLElement;
      element?.focus();
      element?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }
}
