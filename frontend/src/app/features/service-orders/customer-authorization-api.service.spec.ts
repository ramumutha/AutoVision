import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CustomerAuthorizationApiService } from './customer-authorization-api.service';
import { CustomerAuthorization } from './customer-authorization.models';

describe('CustomerAuthorizationApiService', () => {
  const authorization: CustomerAuthorization = {
    id: 'authorization-1',
    tenantId: 'tenant-1',
    dealerId: null,
    branchId: null,
    aftersalesCaseId: 'case-1',
    serviceQuoteId: 'quote-1',
    authorizationNumber: 'AUTH-1001',
    authorizationStatus: 'REQUESTED',
    customerReference: 'customer-1',
    customerDisplayNameSnapshot: 'Test Customer',
    authorizationSummary: 'Authorize quoted work',
    authorizationScopeSnapshot: {
      sourceType: 'SERVICE_QUOTE',
      quoteLineIds: ['line-1'],
    },
    commercialSnapshot: {
      sourceType: 'SERVICE_QUOTE',
      currencyCode: 'EUR',
    },
    termsSnapshot: 'Quote terms',
    disclaimerSnapshot: 'Quote disclaimer',
    requestedAt: '2026-08-21T10:00:00Z',
    decidedAt: null,
    decisionChannel: null,
    decisionReference: null,
    version: 0,
    createdByPrincipalId: 'principal-1',
    updatedByPrincipalId: 'principal-1',
    createdAt: '2026-08-21T10:00:00Z',
    updatedAt: '2026-08-21T10:00:00Z',
  };

  function setup() {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    return {
      service: TestBed.inject(CustomerAuthorizationApiService),
      controller: TestBed.inject(HttpTestingController),
    };
  }

  it('requests quote authorization with the exact bounded body and encoded URL', () => {
    const { service, controller } = setup();
    const body = {
      authorizationNumber: 'AUTH-1001',
      customerReference: 'customer/1',
      customerDisplayNameSnapshot: 'Test Customer',
      authorizationSummary: 'Authorize quoted work',
    };

    service.requestFromServiceQuote('case/1', 'quote/1', body).subscribe((result) => {
      expect(result).toEqual(authorization);
    });

    const request = controller.expectOne(
      '/api/v1/aftersales-cases/case%2F1/service-quotes/quote%2F1/customer-authorization',
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(request.request.body.serviceQuoteId).toBeUndefined();
    expect(request.request.body.authorizationScopeSnapshot).toBeUndefined();
    expect(request.request.body.commercialSnapshot).toBeUndefined();
    expect(request.request.body.termsSnapshot).toBeUndefined();
    expect(request.request.body.disclaimerSnapshot).toBeUndefined();
    request.flush(authorization);
    controller.verify();
  });

  it('lists quote authorization history with encoded URL and preserves an empty list', () => {
    const { service, controller } = setup();
    let result: CustomerAuthorization[] | undefined;

    service.listForServiceQuote('case/1', 'quote/1').subscribe((history) => {
      result = history;
    });

    const request = controller.expectOne(
      '/api/v1/aftersales-cases/case%2F1/service-quotes/quote%2F1/customer-authorizations',
    );
    expect(request.request.method).toBe('GET');
    request.flush([]);
    expect(result).toEqual([]);
    controller.verify();
  });

  it.each([
    ['authorize', 'authorize'],
    ['decline', 'decline'],
    ['defer', 'defer'],
    ['cancel', 'cancel'],
  ] as const)('%s decision uses the exact bounded endpoint and payload', (method, action) => {
    const { service, controller } = setup();
    const body = { decisionChannel: 'EMAIL', decisionReference: 'DEC-1' };
    const decision = service[method]('case/1', 'authorization/1', body);

    decision.subscribe((result) => expect(result).toEqual(authorization));

    const request = controller.expectOne(
      `/api/v1/aftersales-cases/case%2F1/customer-authorizations/authorization%2F1/${action}`,
    );
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(body);
    expect(Object.keys(request.request.body)).toEqual(['decisionChannel', 'decisionReference']);
    request.flush(authorization);
    controller.verify();
  });
});
