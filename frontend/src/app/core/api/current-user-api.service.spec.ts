import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { describe, expect, it } from 'vitest';
import { CurrentUserApiService } from './current-user-api.service';

describe('CurrentUserApiService', () => {
  it('calls the relative current-user endpoint', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(CurrentUserApiService);
    const controller = TestBed.inject(HttpTestingController);

    service.getCurrentUser().subscribe();

    const request = controller.expectOne('/api/v1/me');
    expect(request.request.method).toBe('GET');
    expect(request.request.url).not.toContain('localhost:8081');
    expect(request.request.url).not.toContain('platform:8080');
    request.flush({ subject: 'subject', issuer: 'issuer', userRefId: 'user-ref', tenantId: 'tenant', externalUserId: 'external' });
    controller.verify();
  });
});