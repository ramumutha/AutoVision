import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { TokenProvider } from '../auth/token-provider';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(TokenProvider).getAccessToken();
  const authService = inject(AuthService);
  const authorized = token
    ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : request;
  return next(authorized).pipe(catchError((error) => {
    if (error.status === 401) authService.handleUnauthorized();
    return throwError(() => error);
  }));
};