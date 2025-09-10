import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot } from '@angular/router';
import { of } from 'rxjs';

import { authGuard } from './auth-guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  it('allows access when auth succeeds', (done) => {
    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: { check: () => of(true) } },
        { provide: Router, useValue: { navigateByUrl: jasmine.createSpy('navigateByUrl') } }
      ]
    });

    authGuard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot).subscribe((res) => {
      expect(res).toBeTrue();
      done();
    });
  });
});
