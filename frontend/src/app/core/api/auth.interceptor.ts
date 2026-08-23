import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, of, switchMap, take, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { OidcAdapter } from '../auth/oidc-adapter';
import { TokenProvider } from '../auth/token-provider';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!request.url.startsWith('/api/')) return next(request);

  const tokenProvider = inject(TokenProvider);
  const oidcAdapter = inject(OidcAdapter);
  const authService = inject(AuthService);
  const providerToken = tokenProvider.getAccessToken();

  return oidcAdapter.accessToken().pipe(
    take(1),
    catchError(() => of(providerToken ?? '')),
    switchMap((currentToken) => {
      const accessToken = currentToken || providerToken;
      if (currentToken && currentToken !== providerToken) {
        tokenProvider.setToken({ accessToken: currentToken });
      }

      return next(withBearerToken(request, accessToken)).pipe(catchError((error) => {
        if (error.status === 401) authService.handleUnauthorized();
        return throwError(() => error);
      }));
    }),
  );
};

function withBearerToken<T>(request: HttpRequest<T>, accessToken: string | null): HttpRequest<T> {
  return accessToken
    ? request.clone({ setHeaders: { Authorization: `Bearer ${accessToken}` } })
    : request;
}