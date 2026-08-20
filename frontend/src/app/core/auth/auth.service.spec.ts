import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { TokenProvider } from './token-provider';
import { SessionService } from '../session/session.service';
import { OidcAdapter } from './oidc-adapter';
import { describe, expect, it, vi } from 'vitest';

describe('AuthService', () => {
  it('starts login without overriding the OIDC transaction state', () => {
    const authorize = vi.fn();
    TestBed.configureTestingModule({
      providers: [{ provide: OidcAdapter, useValue: { authorize, logout: () => ({ subscribe: () => undefined }) } }],
    });
    const service = TestBed.inject(AuthService);

    service.login('/');

    expect(authorize).toHaveBeenCalledOnce();
    expect(authorize).toHaveBeenCalledWith();
  });

  it('starts unauthenticated and clears session state on logout', () => {
    TestBed.configureTestingModule({
      providers: [{ provide: OidcAdapter, useValue: { logout: () => ({ subscribe: () => undefined }) } }],
    });
    const service = TestBed.inject(AuthService);
    const session = TestBed.inject(SessionService);
    service.establishSession({ subject: 'user-1', roles: [] }, 'token-1');
    service.logout();

    expect(service.state().status).toBe('UNAUTHENTICATED');
    expect(service.isAuthenticated()).toBe(false);
    expect(session.status()).toBe('EXPIRED');
    expect(TestBed.inject(TokenProvider).getAccessToken()).toBeNull();
  });
});