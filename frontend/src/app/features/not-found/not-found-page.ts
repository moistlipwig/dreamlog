import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { RouterLink } from '@angular/router';

import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-not-found-page',
  imports: [RouterLink, MatCardModule, MatButtonModule],
  templateUrl: './not-found-page.html',
  styleUrls: ['./not-found-page.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotFoundPage {
  private auth = inject(AuthService);

  // Smart home - zalogowany → /app, niezalogowany → /
  user = toSignal(this.auth.user$);
  homeLink = computed(() => (this.user() ? '/app' : '/'));
}
