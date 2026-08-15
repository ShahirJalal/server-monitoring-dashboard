import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { ToastService } from '../services/toast.service';

/**
 * Catches a 401 from any API call (session expired / never logged in on a
 * mutating action) and bounces to the login page, except for the auth
 * endpoints themselves -- /me is expected to 401 for a guest, and /login's
 * own 401 is handled inline by the login form.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {

  const router = inject(Router);
  const toastService = inject(ToastService);

  return next(req).pipe(
    catchError(error => {

      const isAuthEndpoint = req.url.includes('/auth/');

      if (error.status === 401 && !isAuthEndpoint) {
        toastService.info('Please log in to continue.');
        router.navigate(['/login']);
      }

      return throwError(() => error);
    })
  );
};
