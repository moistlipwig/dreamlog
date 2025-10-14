import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, of } from 'rxjs';

import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = (route) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.check().pipe(
    catchError((error: unknown) => {
      // Tylko auth errors (401/403) = redirect na login
      if (error instanceof HttpErrorResponse) {
        if (error.status === 401 || error.status === 403) {
          // Auth error - zapisz gdzie user chciał iść
          const returnUrl = '/' + route.url.map((segment) => segment.path).join('/');
          void router.navigate(['/login'], {
            queryParams: returnUrl !== '/' ? { returnUrl } : undefined,
          });
          return of(false);
        }
      }

      // Inne błędy (500, network) - nie przekierowuj, pokaż błąd
      // Error interceptor już pokaże snackbar
      return of(false);
    }),
  );
};
