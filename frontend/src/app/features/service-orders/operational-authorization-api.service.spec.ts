import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OperationalAuthorizationApiService } from './operational-authorization-api.service';
import { OperationalAuthorizationEvaluation } from './operational-authorization.models';

describe('OperationalAuthorizationApiService', () => {
  it('gets the canonical evaluation with independently encoded IDs', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(OperationalAuthorizationApiService);
    const controller = TestBed.inject(HttpTestingController);
    const evaluation: OperationalAuthorizationEvaluation = {
      serviceJobId: 'job/1', serviceOrderId: 'order/1', status: 'PARTIALLY_AUTHORIZED',
      totalLineCount: 2, authorizedLineCount: 1, pendingLineCount: 1, notAuthorizedLineCount: 0,
    };

    service.evaluate('order/1', 'job/1').subscribe((result) => expect(result).toEqual(evaluation));
    const request = controller.expectOne('/api/v1/aftersales/service-orders/order%2F1/service-jobs/job%2F1/authorization-evaluation');
    expect(request.request.method).toBe('GET');
    request.flush(evaluation);
    controller.verify();
  });
});
