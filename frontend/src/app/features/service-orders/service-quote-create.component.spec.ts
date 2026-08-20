import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject } from 'rxjs';
import { ServiceQuoteCreateComponent } from './service-quote-create.component';
import { toEndOfLocalDayOffsetDateTime } from './service-quote-create.component';
import { ServiceQuoteApiService } from './service-quote-api.service';

describe('ServiceQuoteCreateComponent', () => {
  let fixture: ComponentFixture<ServiceQuoteCreateComponent>;
  let api: { createQuote: ReturnType<typeof vi.fn> };
  const created = {
    id: 'quote-1', serviceOrderId: 'order-1', quoteNumber: 'Q-100', status: 'DRAFT' as const,
    currencyCode: 'EUR', validUntil: null, issuedAt: null, acceptedAt: null, declinedAt: null,
    cancelledAt: null, expiredAt: null, supersededAt: null,
    createdAt: '2026-08-20T00:00:00Z', updatedAt: '2026-08-20T00:00:00Z',
  };

  it('returns null for a blank valid-until date', () => {
    expect(toEndOfLocalDayOffsetDateTime('')).toBeNull();
  });

  it('serializes the selected date as browser-local end of day with its actual offset', () => {
    const selectedDate = '2026-08-20';
    const result = toEndOfLocalDayOffsetDateTime(selectedDate);
    const localEndOfDay = new Date(2026, 7, 20, 23, 59, 59, 0);
    const offsetMinutes = -localEndOfDay.getTimezoneOffset();
    const sign = offsetMinutes >= 0 ? '+' : '-';
    const absoluteOffset = Math.abs(offsetMinutes);
    const offset = `${sign}${String(Math.floor(absoluteOffset / 60)).padStart(2, '0')}:${String(absoluteOffset % 60).padStart(2, '0')}`;

    expect(result).toBe(`${selectedDate}T23:59:59${offset}`);
    expect(result).not.toBe(`${selectedDate}T23:59:59Z`);
    expect(result).toContain('T23:59:59');
  });

  async function create(result = of(created)): Promise<void> {
    api = { createQuote: vi.fn().mockReturnValue(result) };
    await TestBed.configureTestingModule({
      imports: [ServiceQuoteCreateComponent],
      providers: [{ provide: ServiceQuoteApiService, useValue: api }],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceQuoteCreateComponent);
    fixture.componentRef.setInput('orderId', 'order-1');
    fixture.detectChanges();
    await fixture.whenStable();
  }

  it('requires quote number and currency without exposing line selection', async () => {
    await create();
    fixture.componentInstance.submit();
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('This field is required.');
    expect(text).not.toContain('ServiceLine');
    expect(text).not.toContain('Select Lines');
    expect(api.createQuote).not.toHaveBeenCalled();
  });

  it('submits one request, emits the backend DRAFT, and resets after success', async () => {
    await create();
    const component = fixture.componentInstance;
    component.form.setValue({
      quoteNumber: ' Q-100 ', currencyCode: 'eur', validUntil: '2026-09-01',
      termsSnapshot: 'Terms', disclaimerSnapshot: '',
    });
    const received: unknown[] = [];
    component.created.subscribe((quote) => received.push(quote));
    component.submit();
    component.submit();
    await fixture.whenStable();

    expect(api.createQuote).toHaveBeenCalledTimes(1);
    expect(api.createQuote).toHaveBeenCalledWith('order-1', {
      quoteNumber: 'Q-100', currencyCode: 'EUR', validUntil: toEndOfLocalDayOffsetDateTime('2026-09-01'),
      termsSnapshot: 'Terms', disclaimerSnapshot: null,
    });
    expect(received).toEqual([created]);
    expect(component.form.controls.quoteNumber.value).toBe('');
  });

  it('keeps submission busy until the backend response returns', async () => {
    const pending = new Subject<typeof created>();
    await create(pending);
    const component = fixture.componentInstance;
    component.form.setValue({ quoteNumber: 'Q-100', currencyCode: 'EUR', validUntil: '', termsSnapshot: '', disclaimerSnapshot: '' });
    component.submit();
    component.submit();
    expect(api.createQuote).toHaveBeenCalledTimes(1);
    pending.next(created);
    pending.complete();
    await fixture.whenStable();
  });
});