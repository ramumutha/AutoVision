import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ServiceJobAuthorizationReadinessApiService } from './service-job-authorization-readiness-api.service';
import { ServiceJobAuthorizationReadiness } from './service-job-authorization-readiness.models';

describe('ServiceJobAuthorizationReadinessApiService', () => {
  it('gets the canonical readiness response with independently encoded IDs', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(ServiceJobAuthorizationReadinessApiService);
    const controller = TestBed.inject(HttpTestingController);
    const readiness: ServiceJobAuthorizationReadiness = {
      serviceJobId: 'job/1', serviceOrderId: 'order/1', ready: true,
      operationalAuthorizationStatus: 'NOT_AUTHORIZED', reason: 'AUTHORIZATION_NOT_REQUIRED',
    };

    service.evaluate('order/1', 'job/1').subscribe((result) => expect(result).toEqual(readiness));
    const request = controller.expectOne('/api/v1/aftersales/service-orders/order%2F1/service-jobs/job%2F1/authorization-readiness');
    expect(request.request.method).toBe('GET');
    request.flush(readiness);
    controller.verify();
  });
});
