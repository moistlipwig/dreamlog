import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { firstValueFrom, of, type Observable } from 'rxjs';

import { authGuard } from './auth-guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  it('allows access when auth succeeds', async () => {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { check: () => of(true) } },
        { provide: Router, useValue: { navigateByUrl: () => Promise.resolve(true) } },
      ],
    });

    const result = await TestBed.runInInjectionContext(() =>
      firstValueFrom(
        authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot) as Observable<boolean>,
      ),
    );
    expect(result).toBe(true);
  });
});
