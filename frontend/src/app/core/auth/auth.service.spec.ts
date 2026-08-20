import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { TokenProvider } from './token-provider';
import { SessionService } from '../session/session.service';

describe('AuthService', () => {
  it('starts unauthenticated and clears session state on logout', () => {
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