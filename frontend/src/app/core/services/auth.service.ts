import {inject, Injectable} from '@angular/core';
import {Router} from '@angular/router';
import {BehaviorSubject, map, tap} from 'rxjs';

import {ApiHttp} from '../http/api-http';

export interface User {
  id: string;
  name: string;
  email: string;
}

@Injectable({providedIn: 'root'})
export class AuthService {
  private readonly api = inject(ApiHttp);
  private readonly router = inject(Router);

  private readonly userSubject = new BehaviorSubject<User | null>(null);
  readonly user$ = this.userSubject.asObservable();

  check() {
    return this.api.get<User>('/me').pipe(
      tap((user) => this.userSubject.next(user)),
      map((user) => !!user),
    );
  }

  logout() {
    return this.api.post<void>('/logout', {}).pipe(
      tap(() => {
        this.userSubject.next(null);
        void this.router.navigateByUrl('/login');
      }),
    );
  }
}
