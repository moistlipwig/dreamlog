import {HttpErrorResponse, HttpInterceptorFn} from '@angular/common/http';
import {inject} from '@angular/core';
import {MatSnackBar} from '@angular/material/snack-bar';
import {catchError, throwError} from 'rxjs';

/**
 * Global error interceptor that shows user-friendly error messages.
 *
 * Silent errors (no snackbar):
 * - 401/403 (handled by auth guard)
 * - Network errors on /api/me endpoint (auth check when backend offline)
 *
 * Visible errors (shows snackbar):
 * - 404, 500, etc. on regular endpoints
 * - Network errors on user actions (saves, fetches, etc.)
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const snack = inject(MatSnackBar);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (!(error instanceof HttpErrorResponse)) {
        return throwError(() => error);
      }

      // Silent errors - don't show snackbar
      const isSilentError =
        error.status === 200 ||
        error.status === 401 ||
        (error.status === 0 && req.url.endsWith('/api/me'));

      if (isSilentError) {
        return throwError(() => error);
      }

      // Show snackbar for actual errors
      let message: string;
      const errPayload = (error as HttpErrorResponse).error;
      if (errPayload && typeof errPayload === 'object' && 'message' in errPayload && typeof (errPayload as { message?: unknown }).message === 'string') {
        message = (errPayload as { message: string }).message;
      } else {
        message = (error.statusText ?? 'An error occurred');
      }
      snack.open(message, 'Close', { duration: 5000 });
      return throwError(() => error);
    }),
  );
};
