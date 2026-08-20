import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { ServiceQuoteDetailComponent } from './service-quote-detail.component';
import { ServiceQuoteApiService } from './service-quote-api.service';

describe('ServiceQuoteDetailComponent', () => {
  let fixture: ComponentFixture<ServiceQuoteDetailComponent>;
  let api: { getQuote: ReturnType<typeof vi.fn> };
  const detail = {
    id: 'quote-1', serviceOrderId: 'order-1', quoteNumber: 'Q-100', status: 'ACCEPTED' as const,
    currencyCode: 'EUR', validUntil: '2026-09-01T23:59:59+05:30', issuedAt: '2026-08-20T00:00:00Z',
    acceptedAt: '2026-08-21T00:00:00Z', declinedAt: null, cancelledAt: null, expiredAt: null, supersededAt: null,
    createdAt: '2026-08-19T00:00:00Z', updatedAt: '2026-08-21T00:00:00Z', termsSnapshot: 'Plain terms',
    disclaimerSnapshot: '<not html>', lines: [{ id: 'line-1', serviceQuoteId: 'quote-1', serviceLineId: 'line-secret', serviceJobId: 'job-secret', descriptionSnapshot: 'Brake service', quantity: 1.5, unitPrice: 100, currencyCode: 'EUR', netAmount: 150, taxAmount: 30, grossAmount: 180, sequence: 0, createdAt: '2026-08-19T00:00:00Z' }],
  };

  async function create(result = of(detail)): Promise<void> {
    api = { getQuote: vi.fn().mockReturnValue(result) };
    await TestBed.configureTestingModule({
      imports: [ServiceQuoteDetailComponent],
      providers: [{ provide: ServiceQuoteApiService, useValue: api }],
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
    const first = new Subject<typeof detail>();
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
});