import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import {
  BehaviorSubject,
  catchError,
  map,
  Observable,
  of,
  shareReplay,
  tap,
  throwError,
} from 'rxjs';

import { ApiHttp } from '../http/api-http';

export interface User {
  id: string;
  name: string;
  email: string;
  emailVerified: boolean;
  hasPassword: boolean;
  providers: string[];
}

export interface LoginRequest {
  username: string; // email
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  name?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiHttp);
  private readonly router = inject(Router);

  private readonly userSubject = new BehaviorSubject<User | null>(null);
  readonly user$ = this.userSubject.asObservable();

  /**
   * Checks if user is authenticated by calling /api/me endpoint.
   * Uses shareReplay to ensure multiple guards share the same HTTP request during navigation.
   * No long-term caching - each navigation triggers a fresh check for security.
   */
  check(): Observable<boolean> {
    return this.api.get<User>('/me').pipe(
      tap((user) => this.userSubject.next(user)),
      map(() => true),
      catchError(() => {
        this.userSubject.next(null);
        return of(false);
      }),
      shareReplay({ bufferSize: 1, refCount: true }), // Share request, auto-cleanup when no subscribers
    );
  }

  /**
   * Login with email and password.
   * Backend expects form-encoded data at /api/auth/login.
   *
   * CSRF Flow:
   * 1. SpaCsrfTokenFilter on backend automatically ensures XSRF-TOKEN cookie is set
   * 2. Angular's HttpClient automatically reads XSRF-TOKEN cookie and sends X-XSRF-TOKEN header
   * 3. Backend validates CSRF token and processes login
   *
   * Note: withXsrfConfiguration in app.config.ts handles automatic cookie→header conversion.
   */
  login(email: string, password: string): Observable<boolean> {
    const formData = new URLSearchParams();
    formData.append('username', email);
    formData.append('password', password);

    return this.api.post<{ success: boolean }>('/auth/login', formData.toString(), {
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
      },
    }).pipe(
      tap(() => {
        // On success, fetch user info to populate userSubject
        this.check().subscribe();
      }),
      map(() => true),
      catchError((error: unknown) => {
        this.userSubject.next(null);
        return throwError(() => error);
      }),
    );
  }

  /**
   * Register new user with email and password.
   */
  register(request: RegisterRequest): Observable<User> {
    return this.api.post<User>('/auth/register', request).pipe(
      tap((user) => {
        this.userSubject.next(user);
        void this.router.navigateByUrl('/app');
      }),
      catchError((error: unknown) => {
        return throwError(() => error);
      }),
    );
  }

  /**
   * Logout user and redirect to login page.
   */
  logout(): Observable<void> {
    return this.api.post<void>('/auth/logout', {}).pipe(
      tap(() => {
        this.userSubject.next(null);
        void this.router.navigateByUrl('/login');
      }),
    );
  }
}
