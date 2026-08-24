import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ServiceProfitApiService } from './service-profit-api.service';

describe('ServiceProfitApiService data capability', () => {
  let service: ServiceProfitApiService;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });

    service = TestBed.inject(ServiceProfitApiService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    controller.verify();
  });

  it('calls the dealer data capability endpoint through the shared API client', () => {
    service.getDataCapability().subscribe();

    const request = controller.expectOne(
      '/api/v1/service-profit/data-capability',
    );

    expect(request.request.method).toBe('GET');

    // Authorization remains the interceptor/security layer's concern.
    expect(request.request.headers.has('Authorization')).toBe(false);

    request.flush({
      assessmentState: 'ASSESSED',
      sourceDatasetId: 'AUTOVISION-SERVICE-PROFIT-R1-DEMO',
      sourceDatasetVersion: '1.0.0',
      assessmentPolicyVersion: 'service-profit-data-readiness-r1',
      assessedAt: '2026-08-24T05:00:00Z',
      capabilities: [],
    });
  });
});