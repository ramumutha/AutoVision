import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { authInterceptor } from '../api/auth.interceptor';
import { OidcAdapter } from './oidc-adapter';
import { TokenProvider } from './token-provider';
import { AuthService } from './auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;
  let oidc: { accessToken: ReturnType<typeof vi.fn> };
  let auth: { handleUnauthorized: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    oidc = {
      accessToken: vi.fn().mockReturnValue(of('')),
    };
    auth = {
      handleUnauthorized: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: OidcAdapter, useValue: oidc },
        { provide: AuthService, useValue: auth },
      ],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('adds a bearer token only when one is available', () => {
    TestBed.inject(TokenProvider).setToken({ accessToken: 'token-1' });
    http.get('/api/resource').subscribe();
    const request = controller.expectOne('/api/resource');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-1');
    request.flush({});
  });

  it('uses the current provider token instead of a stale bootstrap snapshot', () => {
    TestBed.inject(TokenProvider).setToken({ accessToken: 'stale-token' });
    oidc.accessToken.mockReturnValue(of('current-token'));

    http.get('/api/resource').subscribe();

    const request = controller.expectOne('/api/resource');
    expect(request.request.headers.get('Authorization')).toBe('Bearer current-token');
    expect(TestBed.inject(TokenProvider).getAccessToken()).toBe('current-token');
    request.flush({});
  });

  it('does not invent a token when none is available', () => {
    http.get('/api/resource').subscribe();
    const request = controller.expectOne('/api/resource');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('clears authentication without retrying when an API request returns 401', () => {
    oidc.accessToken.mockReturnValue(of('current-token'));

    http.get('/api/resource').subscribe({ error: () => undefined });
    controller.expectOne('/api/resource').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(auth.handleUnauthorized).toHaveBeenCalledOnce();
    controller.expectNone('/api/resource');
  });

  it('does not resolve OIDC or attach authentication outside the API boundary', () => {
    TestBed.inject(TokenProvider).setToken({ accessToken: 'token-1' });

    http.get('/assets/app-config.json').subscribe();

    const request = controller.expectOne('/assets/app-config.json');
    expect(request.request.headers.has('Authorization')).toBe(false);
    expect(oidc.accessToken).not.toHaveBeenCalled();
    request.flush({});
  });
});