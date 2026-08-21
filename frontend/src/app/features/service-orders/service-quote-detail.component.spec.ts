import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Observable, of, Subject } from 'rxjs';
import { ServiceQuoteDetailComponent } from './service-quote-detail.component';
import { ServiceQuoteApiService } from './service-quote-api.service';
import { CustomerAuthorizationApiService } from './customer-authorization-api.service';
import { ServiceQuoteDetail, ServiceQuoteStatus } from './service-quote.models';

describe('ServiceQuoteDetailComponent', () => {
  let fixture: ComponentFixture<ServiceQuoteDetailComponent>;
  let api: { getQuote: ReturnType<typeof vi.fn> };
  let authorizationApi: { requestFromServiceQuote: ReturnType<typeof vi.fn> } | undefined;
  const detail: ServiceQuoteDetail = {
    id: 'quote-1', serviceOrderId: 'order-1', quoteNumber: 'Q-100', status: 'ACCEPTED',
    currencyCode: 'EUR', validUntil: '2026-09-01T23:59:59+05:30', issuedAt: '2026-08-20T00:00:00Z',
    acceptedAt: '2026-08-21T00:00:00Z', declinedAt: null, cancelledAt: null, expiredAt: null, supersededAt: null,
    createdAt: '2026-08-19T00:00:00Z', updatedAt: '2026-08-21T00:00:00Z', termsSnapshot: 'Plain terms',
    disclaimerSnapshot: '<not html>', lines: [{ id: 'line-1', serviceQuoteId: 'quote-1', serviceLineId: 'line-secret', serviceJobId: 'job-secret', descriptionSnapshot: 'Brake service', quantity: 1.5, unitPrice: 100, currencyCode: 'EUR', netAmount: 150, taxAmount: 30, grossAmount: 180, sequence: 0, createdAt: '2026-08-19T00:00:00Z' }],
  };

  function withStatus(status: ServiceQuoteStatus, afterSalesCaseId: string | null): ServiceQuoteDetail {
    return { ...detail, status, afterSalesCaseId };
  }

  async function create(result: Observable<ServiceQuoteDetail> = of(detail)): Promise<void> {
    api = { getQuote: vi.fn().mockReturnValue(result) };
    authorizationApi ??= { requestFromServiceQuote: vi.fn() };
    await TestBed.configureTestingModule({
      imports: [ServiceQuoteDetailComponent],
      providers: [
        { provide: ServiceQuoteApiService, useValue: api },
        { provide: CustomerAuthorizationApiService, useValue: authorizationApi },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceQuoteDetailComponent);
    fixture.componentRef.setInput('orderId', 'order-1');
    fixture.componentRef.setInput('quoteId', 'quote-1');
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('renders header, lifecycle, safe snapshots, ordered line values, and no technical IDs', async () => {
    await create();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Q-100');
    expect(text).toContain('ACCEPTED');
    expect(text).toContain('EUR');
    expect(text).toContain('Plain terms');
    expect(text).toContain('<not html>');
    expect(text).toContain('Brake service');
    expect(text).toContain('1.5');
    expect(text).toContain('€');
    expect(text).not.toContain('line-secret');
    expect(text).not.toContain('job-secret');
    expect(text).not.toContain('Cancel Quote');
  });

  it('shows local detail error and retries only the detail request', async () => {
    const first = new Subject<ServiceQuoteDetail>();
    await create(first);
    first.error({ status: 404, message: 'Unavailable' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Quote unavailable');
    api.getQuote.mockReturnValue(of(detail));
    (fixture.nativeElement.querySelector('.retry') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(api.getQuote).toHaveBeenCalledTimes(2);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Q-100');
  });

  it('shows the authorization action only for an issued quote with a case', async () => {
    await create(of(withStatus('ISSUED', 'case-1')));
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Request Customer Authorization');
  });

  it.each(['DRAFT', 'ACCEPTED', 'DECLINED', 'CANCELLED', 'EXPIRED', 'SUPERSEDED'] as ServiceQuoteStatus[])(
    'hides the authorization action for %s quotes', async (status) => {
      await create(of(withStatus(status, 'case-1')));
      expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Request Customer Authorization');
    },
  );

  it('hides the authorization action when the quote has no case', async () => {
    await create(of(withStatus('ISSUED', null)));
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Request Customer Authorization');
  });

  it('opens and cancels the inline authorization request without changing quote detail', async () => {
    await create(of(withStatus('ISSUED', 'case-1')));
    (fixture.nativeElement.querySelector('.request-authorization') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Authorization Number');
    (fixture.nativeElement.querySelector('.request-panel .secondary') as HTMLButtonElement).click();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Authorization Number');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Q-100');
  });

  it('shows pending success while retaining the quote and its issued status', async () => {
    const authorization = {
      id: 'authorization-1', tenantId: 'tenant-1', dealerId: null, branchId: null,
      aftersalesCaseId: 'case-1', serviceQuoteId: 'quote-1', authorizationNumber: 'AUTH-1',
      authorizationStatus: 'REQUESTED' as const, customerReference: null, customerDisplayNameSnapshot: null,
      authorizationSummary: 'Authorize quoted work', authorizationScopeSnapshot: {}, commercialSnapshot: null,
      termsSnapshot: null, disclaimerSnapshot: null, requestedAt: '2026-08-21T10:00:00Z', decidedAt: null,
      decisionChannel: null, decisionReference: null, version: 0, createdByPrincipalId: null,
      updatedByPrincipalId: null, createdAt: '2026-08-21T10:00:00Z', updatedAt: '2026-08-21T10:00:00Z',
    };
    authorizationApi = { requestFromServiceQuote: vi.fn().mockReturnValue(of(authorization)) };
    await create(of(withStatus('ISSUED', 'case-1')));
    (fixture.nativeElement.querySelector('.request-authorization') as HTMLButtonElement).click();
    fixture.detectChanges();
    const requestForm = fixture.nativeElement.querySelector('app-service-quote-authorization-request');
    expect(requestForm).not.toBeNull();
    const inputs = fixture.nativeElement.querySelectorAll('input');
    (inputs[0] as HTMLInputElement).value = 'AUTH-1';
    (inputs[0] as HTMLInputElement).dispatchEvent(new Event('input'));
    const summary = fixture.nativeElement.querySelector('textarea') as HTMLTextAreaElement;
    summary.value = 'Authorize quoted work';
    summary.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    (fixture.nativeElement.querySelector('.request-panel form') as HTMLFormElement).dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Customer authorization requested');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('REQUESTED');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Q-100');
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('ISSUED');
  });
});