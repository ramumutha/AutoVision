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
});