import { ChangeDetectionStrategy, Component, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';

import { GoogleLoginButtonComponent } from './components/google-login-button/google-login-button.component';
import { LoginFormComponent } from './components/login-form/login-form.component';
import { RegisterFormComponent } from './components/register-form/register-form.component';

@Component({
  selector: 'app-login-page',
  imports: [
    MatCardModule,
    MatDividerModule,
    MatButtonModule,
    LoginFormComponent,
    RegisterFormComponent,
    GoogleLoginButtonComponent,
  ],
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPageComponent {
  showRegister = signal(false);

  toggleMode(): void {
    this.showRegister.update((value) => !value);
  }
}
