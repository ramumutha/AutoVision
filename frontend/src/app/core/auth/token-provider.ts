import { Injectable, signal } from '@angular/core';

export interface AuthTokenSnapshot {
  accessToken: string;
  expiresAt?: number;
}

@Injectable({ providedIn: 'root' })
export class TokenProvider {
  private readonly token = signal<AuthTokenSnapshot | null>(null);

  getAccessToken(): string | null { return this.token()?.accessToken ?? null; }
  setToken(token: AuthTokenSnapshot | null): void { this.token.set(token); }
  clear(): void { this.token.set(null); }
}