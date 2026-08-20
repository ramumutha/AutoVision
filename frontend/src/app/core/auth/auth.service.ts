import { Injectable, signal } from '@angular/core';
import { SessionService } from '../session/session.service';
import { TokenProvider } from './token-provider';

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
  ) {}

  beginLogin(): void {}

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
    this.tokenProvider.clear();
    this.state.set({ status: 'UNAUTHENTICATED', roles: [] });
    this.isAuthenticated.set(false);
    this.sessionService.clear();
  }
}