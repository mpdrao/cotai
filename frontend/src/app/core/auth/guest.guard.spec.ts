import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { guestGuard } from './guest.guard';

function runGuard(loggedIn: boolean) {
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: { isLoggedIn: () => loggedIn } }],
  });
  return TestBed.runInInjectionContext(() =>
    guestGuard({} as ActivatedRouteSnapshot, { url: '/login' } as RouterStateSnapshot),
  );
}

describe('guestGuard', () => {
  it('lets a visitor open login and sign up', () => {
    expect(runGuard(false)).toBe(true);
  });

  it('sends a logged in organizer to the dashboard', () => {
    const result = runGuard(true);
    expect(result instanceof UrlTree && result.toString()).toBe('/dashboard');
  });
});
