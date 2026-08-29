import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { CurrentUserApiService } from '../api/current-user-api.service';
import { RuntimeConfigurationService } from '../configuration/app-configuration';
import { AuthBootstrapService } from './auth-bootstrap.service';
import { AuthService } from './auth.service';
import { OidcAdapter } from './oidc-adapter';

const authenticatedResult = {
  isAuthenticated: true,
  accessToken: 'access-token',
  userData: { sub: 'subject', exp: 1_900_000_000 },
} as never;

const currentUser = {
  subject: 'subject',
  issuer: 'http://localhost:8081/realms/autovision',
  userRefId: 'user-ref',
  tenantId: 'tenant',
  externalUserId: 'external',
};

describe('AuthBootstrapService', () => {
  let events: string[];
  let auth: { prepareProviderToken: ReturnType<typeof vi.fn>; establishFromProvider: ReturnType<typeof vi.fn>; handleUnauthorized: ReturnType<typeof vi.fn>; markReady: ReturnType<typeof vi.fn> };
  let currentUserApi: { getCurrentUser: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    events = [];
    auth = {
      prepareProviderToken: vi.fn(() => events.push('prepare')),
      establishFromProvider: vi.fn(() => events.push('establish')),
      handleUnauthorized: vi.fn(() => events.push('unauthorized')),
      markReady: vi.fn(() => events.push('ready')),
    };
    currentUserApi = { getCurrentUser: vi.fn(() => of(currentUser)) };
    TestBed.configureTestingModule({
      providers: [
        AuthBootstrapService,
        { provide: RuntimeConfigurationService, useValue: { load: vi.fn(async () => undefined), configuration: () => ({ oidc: { issuer: 'issuer', clientId: 'client' } }) } },
        { provide: OidcAdapter, useValue: { checkAuth: () => of(authenticatedResult) } },
        { provide: AuthService, useValue: auth },
        { provide: CurrentUserApiService, useValue: currentUserApi },
      ],
    });
  });

  it('confirms Platform identity after token preparation and before session establishment', async () => {
    await TestBed.inject(AuthBootstrapService).initialize();

    expect(events).toEqual(['prepare', 'establish', 'ready']);
    expect(currentUserApi.getCurrentUser).toHaveBeenCalledOnce();
  });

  it('does not call the Platform API for an unauthenticated OIDC result', async () => {
    TestBed.overrideProvider(OidcAdapter, { useValue: { checkAuth: () => of({ isAuthenticated: false, accessToken: '' }) } });
    await TestBed.inject(AuthBootstrapService).initialize();

    expect(currentUserApi.getCurrentUser).not.toHaveBeenCalled();
    expect(auth.handleUnauthorized).toHaveBeenCalledOnce();
    expect(auth.markReady).toHaveBeenCalledOnce();
  });

  it('fails closed when Platform identity confirmation fails', async () => {
    currentUserApi.getCurrentUser.mockReturnValue(throwError(() => ({ status: 503 })));
    await TestBed.inject(AuthBootstrapService).initialize();

    expect(events).toEqual(['prepare', 'unauthorized', 'ready']);
    expect(auth.establishFromProvider).not.toHaveBeenCalled();
  });
});