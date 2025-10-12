import { ChangeDetectionStrategy, Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-google-login-button',
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './google-login-button.component.html',
  styleUrls: ['./google-login-button.component.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class GoogleLoginButtonComponent {}
