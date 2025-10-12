import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

@Component({
  selector: 'app-login-form',
  imports: [ReactiveFormsModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  templateUrl: './login-form.component.html',
  styleUrls: ['./login-form.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginFormComponent {
  loginForm = new FormGroup({
    username: new FormControl('', {
      validators: [(control) => Validators.required(control)],
      nonNullable: true,
    }),
    password: new FormControl('', {
      validators: [(control) => Validators.required(control) ?? Validators.minLength(6)(control)],
      nonNullable: true,
    }),
  });

  isLoading = signal(false);

  onSubmit(): void {
    if (this.loginForm.valid) {
      this.isLoading.set(true);
      // TODO: Implement actual login logic with backend
      console.log('Login form submitted:', this.loginForm.value);

      // Simulate API call
      setTimeout(() => {
        this.isLoading.set(false);
        alert('Login with username/password not yet implemented. Please use Google login.');
      }, 1000);
    }
  }
}
