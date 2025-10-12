import {AsyncPipe} from '@angular/common';
import {Component, inject} from '@angular/core';
import {NavigationEnd, Router, RouterOutlet} from '@angular/router';
import {map, startWith} from 'rxjs';

import {AppShell} from '@core/app-shell';

@Component({
  selector: 'app-root',
  imports: [AppShell, RouterOutlet, AsyncPipe],
  standalone: true,
  template: `
    @if (shouldShowAppShell$ | async) {
      <app-app-shell/>
    } @else {
      <router-outlet/>
    }
  `,
  styleUrls: ['./app.scss'],
})
export class App {
  private readonly router = inject(Router);

  // Show AppShell only for routes starting with /app
  shouldShowAppShell$ = this.router.events.pipe(
    startWith(new NavigationEnd(0, this.router.url, this.router.url)),
    map((event) => {
      if (event instanceof NavigationEnd) {
        return event.url.startsWith('/app');
      }
      return false;
    }),
  );
}
