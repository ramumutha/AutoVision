import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ServiceOrderApiService } from './service-order-api.service';

describe('ServiceOrderApiService', () => {
  it('uses the authoritative ServiceOrder aggregate endpoint', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(ServiceOrderApiService);
    const controller = TestBed.inject(HttpTestingController);
    service.getServiceOrder('order-1').subscribe();

    const request = controller.expectOne('/api/v1/service-orders/order-1/aggregate');
    expect(request.request.method).toBe('GET');
    request.flush({ order: {}, jobs: [], lines: [] });
    controller.verify();
  });
});