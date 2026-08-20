import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from '../api/auth.interceptor';
import { TokenProvider } from './token-provider';

describe('authInterceptor', () => {
  let http: HttpClient;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpClient);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('adds a bearer token only when one is available', () => {
    TestBed.inject(TokenProvider).setToken({ accessToken: 'token-1' });
    http.get('/resource').subscribe();
    const request = controller.expectOne('/resource');
    expect(request.request.headers.get('Authorization')).toBe('Bearer token-1');
    request.flush({});
  });

  it('does not invent a token when none is available', () => {
    http.get('/resource').subscribe();
    const request = controller.expectOne('/resource');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });
});