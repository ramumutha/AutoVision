import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ServiceProfitApiService } from './service-profit-api.service';

describe('ServiceProfitApiService', () => {
  let service: ServiceProfitApiService;
  let controller: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    service = TestBed.inject(ServiceProfitApiService);
    controller = TestBed.inject(HttpTestingController);
  });

  afterEach(() => controller.verify());

  it('calls the manager summary endpoint through the shared API client', () => {
    service.getSummary().subscribe();

    const request = controller.expectOne('/api/v1/service-profit/opportunities/summary');
    expect(request.request.method).toBe('GET');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
  });

  it('calls the queue endpoint and translates supported filters to backend query parameters', () => {
    service.getOpportunities({
      priority: 'HIGH',
      opportunityType: 'DECLINED_WORK',
      actionability: 'REVIEW_REQUIRED',
      page: 1,
      size: 25,
      sort: 'POTENTIAL_DESC',
    }).subscribe();

    const request = controller.expectOne((candidate) => candidate.url === '/api/v1/service-profit/opportunities');
    expect(request.request.method).toBe('GET');
    expect(request.request.params.get('priority')).toBe('HIGH');
    expect(request.request.params.get('opportunityType')).toBe('DECLINED_WORK');
    expect(request.request.params.get('actionability')).toBe('REVIEW_REQUIRED');
    expect(request.request.params.get('page')).toBe('1');
    expect(request.request.params.get('size')).toBe('25');
    expect(request.request.params.get('sort')).toBe('POTENTIAL_DESC');
    request.flush({ items: [], page: 1, size: 25, totalElements: 0, totalPages: 0 });
  });

  it('calls the detail endpoint with an encoded opportunity id', () => {
    service.getOpportunity('opportunity/1').subscribe();

    const request = controller.expectOne('/api/v1/service-profit/opportunities/opportunity%2F1');
    expect(request.request.method).toBe('GET');
    request.flush({});
  });
});