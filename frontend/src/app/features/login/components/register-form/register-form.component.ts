import {HttpErrorResponse} from '@angular/common/http';
import {ChangeDetectionStrategy, Component, inject, signal} from '@angular/core';
import {
  AbstractControl,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import {MatButtonModule} from '@angular/material/button';
import {MatFormFieldModule} from '@angular/material/form-field';
import {MatInputModule} from '@angular/material/input';
import {MatSnackBar} from '@angular/material/snack-bar';
import {Router} from '@angular/router';

import {AuthService} from '../../../../core/services/auth.service';

function passwordPolicy(control: AbstractControl): ValidationErrors | null {
  const value = control.value as string;
  if (!value) return null;

  const hasLetter = /[a-zA-Z]/.test(value);
  const hasDigit = /\d/.test(value);

  return hasLetter && hasDigit ? null : {passwordPolicy: true};
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
  private readonly snackBar = inject(MatSnackBar);

  touchedFields = signal<Set<string>>(new Set());
  formSubmitted = signal(false);

  registerForm = new FormGroup({
    email: new FormControl('', {
      validators: [
        (control) => Validators.required(control),
        (control) => Validators.email(control),
      ],
      nonNullable: true,
    }),
    name: new FormControl('', {nonNullable: true}),
    password: new FormControl('', {
      validators: [
        (control) => Validators.required(control),
        (control) => Validators.minLength(8)(control),
        passwordPolicy,
      ],
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
        next: (user) => {
          // Show success message with user's name
          const welcomeMessage = user.name
            ? `Welcome ${user.name}! Your account has been created.`
            : 'Account created successfully! Welcome to DreamLog.';

          this.snackBar.open(welcomeMessage, 'OK', {
            duration: 5000,
            horizontalPosition: 'center',
            verticalPosition: 'top',
          });

          // Verify session is established before navigating
          // This ensures auth guard will pass
          this.authService.check().subscribe({
            next: (authenticated) => {
              this.isLoading.set(false);
              if (authenticated) {
                void this.router.navigateByUrl('/app');
              } else {
                // Session not established - show error
                this.errorMessage.set(
                  'Registration succeeded but login failed. Please try logging in manually.',
                );
              }
            },
            error: () => {
              this.isLoading.set(false);
              this.errorMessage.set(
                'Registration succeeded but login failed. Please try logging in manually.',
              );
            },
          });
        },
        error: (error: unknown) => {
          this.isLoading.set(false);
          let message = 'Registration failed. Please try again.';
          if (error instanceof HttpErrorResponse) {
            const errorBody = error.error as { error?: string } | null;
            if (errorBody && typeof errorBody.error === 'string') {
              message = errorBody.error;
            }
          }
          this.errorMessage.set(message);
        },
      });
    } else {
      this.scrollToFirstError();
    }
  }

  private scrollToFirstError(): void {
    const firstInvalidControl = Object.keys(this.registerForm.controls).find(
      (key) => this.registerForm.get(key)?.invalid,
    );

    if (firstInvalidControl) {
      const element = document.querySelector(
        `[formControlName="${firstInvalidControl}"]`,
      ) as HTMLElement;
      element?.focus();
      element?.scrollIntoView({behavior: 'smooth', block: 'center'});
    }
  }
}
