import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { catchError, of } from 'rxjs';
import { AuthService } from '../services/auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.check().pipe(
    catchError(() => {
      router.navigateByUrl('/login');
      return of(false);
    })
  );
};
