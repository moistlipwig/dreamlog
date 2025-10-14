import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';

import { GoogleLoginButtonComponent } from './components/google-login-button/google-login-button.component';
import { LoginFormComponent } from './components/login-form/login-form.component';

@Component({
  selector: 'app-login-page',
  imports: [
    MatCardModule,
    MatDividerModule,
    RouterLink,
    LoginFormComponent,
    GoogleLoginButtonComponent,
  ],
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LoginPageComponent {}
