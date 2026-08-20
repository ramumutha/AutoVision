import { Injectable, signal } from '@angular/core';
import { TokenProvider } from './token-provider';

@Injectable({ providedIn: 'root' })
export class AuthService {
  readonly isAuthenticated = signal(false);
  constructor(private readonly tokenProvider: TokenProvider) {}
  beginLogin(): void {}
  logout(): void { this.tokenProvider.setAccessToken(null); this.isAuthenticated.set(false); }
}