import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ServiceQuoteApiService } from './service-quote-api.service';

describe('ServiceQuoteApiService', () => {
  it('calls the order-scoped Quote summary endpoint', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(ServiceQuoteApiService);
    const controller = TestBed.inject(HttpTestingController);
    service.listQuotes('order/1').subscribe();

    const request = controller.expectOne('/api/v1/aftersales/service-orders/order%2F1/quotes');
    expect(request.request.method).toBe('GET');
    request.flush([]);
    controller.verify();
  });

  it('creates a Quote with only the backend request fields', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(ServiceQuoteApiService);
    const controller = TestBed.inject(HttpTestingController);
    service.createQuote('order-1', {
      quoteNumber: 'Q-100', currencyCode: 'EUR', validUntil: '2026-09-01T23:59:59Z',
      termsSnapshot: 'Terms', disclaimerSnapshot: null,
    }).subscribe();

    const request = controller.expectOne('/api/v1/aftersales/service-orders/order-1/quotes');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      quoteNumber: 'Q-100', currencyCode: 'EUR', validUntil: '2026-09-01T23:59:59Z',
      termsSnapshot: 'Terms', disclaimerSnapshot: null,
    });
    expect(JSON.stringify(request.request.body)).not.toContain('serviceLine');
    expect(JSON.stringify(request.request.body)).not.toContain('selectedLines');
    request.flush({});
    controller.verify();
  });
});