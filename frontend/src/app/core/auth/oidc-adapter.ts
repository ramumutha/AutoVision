import { Injectable, inject } from '@angular/core';
import { OidcSecurityService, LoginResponse } from 'angular-auth-oidc-client';
import { Observable } from 'rxjs';

export interface OidcSession {
  authenticated: boolean;
  accessToken?: string;
  claims?: Record<string, unknown>;
  expiresAt?: number;
}

@Injectable({ providedIn: 'root' })
export class OidcAdapter {
  private readonly oidc = inject(OidcSecurityService);

  checkAuth(): Observable<LoginResponse> { return this.oidc.checkAuth(); }
  authorize(): void { this.oidc.authorize(); }
  refresh(): Observable<LoginResponse> { return this.oidc.forceRefreshSession(); }
  logout(): Observable<unknown> { return this.oidc.logoffAndRevokeTokens(); }
  accessToken(): Observable<string> { return this.oidc.getAccessToken(); }
}