import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { of, Subject } from 'rxjs';
import { ServiceOrderQuotesComponent } from './service-order-quotes.component';
import { ServiceQuoteApiService } from './service-quote-api.service';

describe('ServiceOrderQuotesComponent', () => {
  let fixture: ComponentFixture<ServiceOrderQuotesComponent>;
  let api: { listQuotes: ReturnType<typeof vi.fn> };
  const quote = {
    id: 'quote-1', serviceOrderId: 'order-1', quoteNumber: 'Q-100', status: 'ISSUED' as const,
    currencyCode: 'EUR', validUntil: '2026-09-01T00:00:00Z', issuedAt: null,
    acceptedAt: null, declinedAt: null, cancelledAt: null, expiredAt: null, supersededAt: null,
    createdAt: '2026-08-20T00:00:00Z', updatedAt: '2026-08-20T00:00:00Z',
  };

  async function create(apiResult: unknown = of([quote])): Promise<void> {
    api = { listQuotes: vi.fn().mockReturnValue(apiResult) };
    await TestBed.configureTestingModule({
      imports: [ServiceOrderQuotesComponent],
      providers: [
        provideRouter([]),
        { provide: ServiceQuoteApiService, useValue: api },
        { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ orderId: 'order-1' })) } },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceOrderQuotesComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('renders populated summaries in backend order with readable status and dates', async () => {
    await create(of([quote, { ...quote, id: 'quote-2', quoteNumber: 'Q-101', status: 'DRAFT', validUntil: null }]));
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Q-100');
    expect(text).toContain('Q-101');
    expect(text).toContain('ISSUED');
    expect(text).toContain('EUR');
    expect(text).toContain('Sep 1, 2026');
    expect(text).toContain('—');
    expect(text).not.toContain('Cancel');
  });

  it('renders the localized empty state without loading a create flow', async () => {
    await create(of([]));
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('No quotes have been created for this service order.');
    expect(text).toContain('Create Quote');
  });

  it('renders a safe error and retries the GET locally', async () => {
    const first = new Subject<unknown>();
    await create(first);
    first.error({ status: 403, message: 'Forbidden' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('You are not authorized');

    api.listQuotes.mockReturnValue(of([quote]));
    (fixture.nativeElement.querySelector('.retry') as HTMLButtonElement).click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(api.listQuotes).toHaveBeenCalledTimes(2);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Q-100');
  });
});