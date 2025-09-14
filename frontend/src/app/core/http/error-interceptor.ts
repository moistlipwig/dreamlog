import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snack = inject(MatSnackBar);
  return next(req).pipe(
    catchError((err: unknown) => {
      const message = (err as { statusText?: string }).statusText ?? 'Error';
      snack.open(message, 'Close', { duration: 3000 });
      return throwError(() => err);
    })
  );
};
