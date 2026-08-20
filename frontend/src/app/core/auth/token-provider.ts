import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TokenProvider {
  private readonly token = signal<string | null>(null);
  getAccessToken(): string | null { return this.token(); }
  setAccessToken(token: string | null): void { this.token.set(token); }
}