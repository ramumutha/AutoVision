import { Injectable, signal } from '@angular/core';

export interface SessionSummary { userDisplayName: string; tenantDisplayName?: string; }
export type SessionStatus = 'ACTIVE' | 'EXPIRING' | 'EXPIRED' | 'REVOKED' | 'CONFLICT' | 'OFFLINE_UNKNOWN';
export type ConcurrentSessionPolicy = 'ALLOW_MULTIPLE' | 'DENY_NEW' | 'REPLACE_OLDEST' | 'ASK_USER';
export interface SessionConflict { policy: 'ASK_USER'; existingDeviceLabel?: string; }

@Injectable({ providedIn: 'root' })
export class SessionService {
  readonly session = signal<SessionSummary | null>(null);
  readonly status = signal<SessionStatus>('EXPIRED');
  readonly conflict = signal<SessionConflict | null>(null);
  activate(summary?: SessionSummary): void { this.session.set(summary ?? null); this.status.set('ACTIVE'); }
  transition(status: SessionStatus): void { this.status.set(status); }
  requestConflict(existingDeviceLabel?: string): void {
    this.conflict.set({ policy: 'ASK_USER', existingDeviceLabel });
    this.status.set('CONFLICT');
  }
  clearConflict(): void { this.conflict.set(null); }
  clear(): void { this.session.set(null); this.conflict.set(null); this.status.set('EXPIRED'); }
}