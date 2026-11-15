import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth-service';

/** Pages behind this guard need a signed-in user; everyone else is sent to /login. */
export const authGuard: CanActivateFn = () => {
  const signedIn = inject(AuthService).currentUser() !== null;
  return signedIn || inject(Router).createUrlTree(['/login']);
};
