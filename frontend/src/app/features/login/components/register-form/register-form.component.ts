import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { Router } from '@angular/router';

import { AuthService } from '../../../../core/services/auth.service';

function passwordPolicy(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string;
  if (!value) return null;

  const hasLetter = /[a-zA-Z]/.test(value);
  const hasDigit = /\d/.test(value);

  return hasLetter && hasDigit ? null : { passwordPolicy: true };
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

  touchedFields = signal<Set<string>>(new Set());
  formSubmitted = signal(false);

  registerForm = new FormGroup({
    email: new FormControl('', {
      validators: [Validators.required, Validators.email],
      nonNullable: true,
    }),
    name: new FormControl('', { nonNullable: true }),
    password: new FormControl('', {
      validators: [Validators.required, Validators.minLength(8), passwordPolicy],
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
    } else {
      this.scrollToFirstError();
    }
  }

  private scrollToFirstError(): void {
    const firstInvalidControl = Object.keys(this.registerForm.controls).find((key) => this.registerForm.get(key)?.invalid);

    if (firstInvalidControl) {
      const element = document.querySelector(`[formControlName="${firstInvalidControl}"]`) as HTMLElement;
      element?.focus();
      element?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
  }
}
