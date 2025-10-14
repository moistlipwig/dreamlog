import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { BehaviorSubject, catchError, map, Observable, of, shareReplay, tap } from 'rxjs';

import { ApiHttp } from '../http/api-http';

export interface User {
  id: string;
  name: string;
  email: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiHttp);
  private readonly router = inject(Router);

  private readonly userSubject = new BehaviorSubject<User | null>(null);
  readonly user$ = this.userSubject.asObservable();

  // Cache dla auth check
  private authCheckCache$: Observable<boolean> | null = null;
  private cacheTimestamp = 0;
  private readonly CACHE_DURATION_MS = 60_000; // 1 minuta

  check(): Observable<boolean> {
    const now = Date.now();

    // Jeśli cache jest świeży, zwróć go
    if (this.authCheckCache$ && now - this.cacheTimestamp < this.CACHE_DURATION_MS) {
      return this.authCheckCache$;
    }

    // Cache wygasł lub nie istnieje - zrób nowy request
    this.cacheTimestamp = now;
    this.authCheckCache$ = this.api.get<User>('/me').pipe(
      tap((user) => this.userSubject.next(user)),
      map(() => true),
      catchError(() => {
        this.userSubject.next(null);
        return of(false);
      }),
      shareReplay(1), // Dziel jeden request między wiele subskrypcji
    );

    return this.authCheckCache$;
  }

  clearCache(): void {
    this.authCheckCache$ = null;
    this.cacheTimestamp = 0;
  }

  logout() {
    return this.api.post<void>('/logout', {}).pipe(
      tap(() => {
        this.userSubject.next(null);
        this.clearCache();
        void this.router.navigateByUrl('/login');
      }),
    );
  }
}
