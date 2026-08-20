import { Injectable, signal } from '@angular/core';

export interface SessionSummary { userDisplayName: string; tenantDisplayName?: string; }

@Injectable({ providedIn: 'root' })
export class SessionService {
  readonly session = signal<SessionSummary | null>(null);
  clear(): void { this.session.set(null); }
}