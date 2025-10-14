import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { ResolveFn, Router } from '@angular/router';
import { catchError, EMPTY } from 'rxjs';

import { Dream } from '../models/dream';
import { DreamsService } from '../services/dreams.service';

export const dreamResolver: ResolveFn<Dream | null> = (route) => {
  const dreams = inject(DreamsService);
  const router = inject(Router);
  const id = route.paramMap.get('id');

  if (!id) {
    void router.navigateByUrl('/app/dreams');
    return EMPTY;
  }

  return dreams.get(id).pipe(
    catchError((err: unknown) => {
      if (err instanceof HttpErrorResponse && err.status === 404) {
        void router.navigateByUrl('/app/not-found');
      } else {
        void router.navigateByUrl('/app/dreams');
      }
      return EMPTY;
    }),
  );
};
