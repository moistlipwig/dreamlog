import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import {
  Router,
  RouterOutlet,
  RouterLink,
  NavigationStart,
  NavigationEnd,
  NavigationCancel,
  NavigationError,
} from '@angular/router';
import { filter, map } from 'rxjs';

import { SearchBar } from './search-bar';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-app-shell',
  imports: [
    RouterOutlet,
    RouterLink,
    MatToolbarModule,
    MatButtonModule,
    MatIconModule,
    MatSidenavModule,
    MatListModule,
    MatProgressBarModule,
    SearchBar,
    AsyncPipe,
  ],
  templateUrl: './app-shell.html',
  styleUrls: ['./app-shell.scss'],
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AppShell {
  private readonly router = inject(Router);
  private readonly authService = inject(AuthService);

  loading$ = this.router.events.pipe(
    filter(
      (e) =>
        e instanceof NavigationStart ||
        e instanceof NavigationEnd ||
        e instanceof NavigationCancel ||
        e instanceof NavigationError,
    ),
    map((e) => e instanceof NavigationStart),
  );

  logout(): void {
    this.authService.logout().subscribe();
  }
}
