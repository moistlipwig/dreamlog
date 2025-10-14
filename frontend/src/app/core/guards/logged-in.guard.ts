import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { catchError, map, of } from 'rxjs';

import { AuthService } from '../services/auth.service';

/**
 * Guard chroniący strony publiczne (landing, login) przed zalogowanymi userami.
 * Jeśli user jest zalogowany, przekierowuje na /app
 */
export const loggedInGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.check().pipe(
    map((isLoggedIn) => {
      if (isLoggedIn) {
        // User jest zalogowany, przekieruj do app
        void router.navigateByUrl('/app');
        return false;
      }
      // User nie jest zalogowany, pokaż stronę publiczną
      return true;
    }),
    catchError(() => {
      // Błąd = user nie jest zalogowany, można pokazać stronę
      return of(true);
    }),
  );
};
