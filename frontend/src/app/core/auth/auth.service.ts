import { Injectable, signal } from '@angular/core';
import { SessionService } from '../session/session.service';
import { TokenProvider } from './token-provider';
import { OidcAdapter } from './oidc-adapter';
import { LoginResponse } from 'angular-auth-oidc-client';
import { firstValueFrom } from 'rxjs';

export interface AuthState {
  status: 'UNAUTHENTICATED' | 'AUTHENTICATED' | 'REFRESHING';
  subject?: string;
  displayName?: string;
  username?: string;
  email?: string;
  expiresAt?: number;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly state = signal<AuthState>({ status: 'UNAUTHENTICATED', roles: [] });
  readonly isAuthenticated = signal(false);

  constructor(
    private readonly tokenProvider: TokenProvider,
    private readonly sessionService: SessionService,
    private readonly oidcAdapter: OidcAdapter,
  ) {}

  login(_returnUrl = '/'): void {
    this.oidcAdapter.authorize();
  }

  beginLogin(): void { this.login('/'); }

  prepareProviderToken(result: LoginResponse): void {
    const claims = (result.userData ?? {}) as Record<string, unknown>;
    const expiresAt = typeof claims['exp'] === 'number' ? claims['exp'] : undefined;
    this.tokenProvider.setToken({ accessToken: result.accessToken, expiresAt });
  }

  establishFromProvider(result: LoginResponse): void {
    const claims = (result.userData ?? {}) as Record<string, unknown>;
    const expiresAt = typeof claims['exp'] === 'number' ? claims['exp'] : undefined;
    this.establishSession({
      subject: typeof claims['sub'] === 'string' ? claims['sub'] : undefined,
      displayName: typeof claims['name'] === 'string' ? claims['name'] : undefined,
      username: typeof claims['preferred_username'] === 'string' ? claims['preferred_username'] : undefined,
      email: typeof claims['email'] === 'string' ? claims['email'] : undefined,
      expiresAt,
      roles: Array.isArray(claims['roles']) ? claims['roles'].filter((role): role is string => typeof role === 'string') : [],
    }, result.accessToken, expiresAt);
  }

  establishSession(state: Omit<AuthState, 'status'>, accessToken: string, expiresAt?: number): void {
    this.tokenProvider.setToken({ accessToken, expiresAt });
    this.state.set({ ...state, status: 'AUTHENTICATED', expiresAt, roles: [...state.roles] });
    this.isAuthenticated.set(true);
    this.sessionService.activate();
  }

  handleUnauthorized(): void {
    this.tokenProvider.clear();
    this.state.set({ status: 'UNAUTHENTICATED', roles: [] });
    this.isAuthenticated.set(false);
    this.sessionService.transition('EXPIRED');
  }

  logout(): void {
    this.clearLocalState();
    void firstValueFrom(this.oidcAdapter.logout()).catch(() => undefined);
  }

  private clearLocalState(): void {
    this.tokenProvider.clear();
    this.state.set({ status: 'UNAUTHENTICATED', roles: [] });
    this.isAuthenticated.set(false);
    this.sessionService.clear();
  }

}