import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenProvider } from '../auth/token-provider';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const token = inject(TokenProvider).getAccessToken();
  return token ? next(request.clone({ setHeaders: { Authorization: `Bearer ${token}` } })) : next(request);
};