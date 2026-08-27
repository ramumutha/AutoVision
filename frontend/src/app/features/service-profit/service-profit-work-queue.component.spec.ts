import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitWorkQueueComponent } from './service-profit-work-queue.component';
import { ServiceProfitWorkQueueItem, ServiceProfitWorkQueuePage } from './service-profit.models';

describe('ServiceProfitWorkQueueComponent', () => {
  let fixture: ComponentFixture<ServiceProfitWorkQueueComponent>;
  let api: { getWorkQueue: ReturnType<typeof vi.fn> };

  const item = (overrides: Partial<ServiceProfitWorkQueueItem> = {}): ServiceProfitWorkQueueItem => ({
    followUpId: 'follow-up-1', opportunityId: 'opportunity-1', handlingStatus: 'OPEN', ownerPrincipalId: null,
    claimedAt: null, dueAt: '2026-08-26T10:00:00Z', disposition: 'FOLLOW_UP_REQUIRED', version: 0,
    createdAt: '2026-08-20T10:00:00Z', updatedAt: '2026-08-20T10:00:00Z', actionability: 'READY',
    opportunityStatus: 'DETECTED', evidenceClass: 'SOURCE_CONFIRMED', evidenceStrength: 'STRONG',
    priority: 'HIGH', title: 'Brake work recovery', summary: 'Previously declined brake replacement', ...overrides,
  });
  const page = (items: ServiceProfitWorkQueueItem[], totalPages = 1): ServiceProfitWorkQueuePage => ({
    items, page: 0, size: 25, totalElements: items.length, totalPages,
  });

  beforeEach(async () => {
    api = { getWorkQueue: vi.fn().mockReturnValue(of(page([item()]))) };
    await TestBed.configureTestingModule({
      imports: [ServiceProfitWorkQueueComponent], providers: [provideRouter([]), { provide: ServiceProfitApiService, useValue: api }],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceProfitWorkQueueComponent);
    fixture.detectChanges();
  });

  it('loads the frozen endpoint with ALL ownership and no principal id', () => {
    expect(api.getWorkQueue).toHaveBeenCalledWith({ ownership: 'ALL', page: 0, size: 25 });
    expect(JSON.stringify(api.getWorkQueue.mock.calls[0])).not.toContain('ownerPrincipalId');
  });

  it('maps Mine and Unassigned to the frozen ownership values', async () => {
    const buttons = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.ownership-tabs button');
    buttons[1].click(); fixture.detectChanges(); await fixture.whenStable();
    expect(api.getWorkQueue).toHaveBeenLastCalledWith({ ownership: 'MINE', page: 0, size: 25 });
    buttons[2].click(); fixture.detectChanges(); await fixture.whenStable();
    expect(api.getWorkQueue).toHaveBeenLastCalledWith({ ownership: 'UNASSIGNED', page: 0, size: 25 });
  });

  it('defaults to Due State and renders overdue, no-due-date, and disposition groups', () => {
    api.getWorkQueue.mockReturnValue(of(page([
      item(), item({ followUpId: 'follow-up-2', dueAt: null, disposition: 'NO_RESPONSE_RECORDED', priority: 'MEDIUM' }),
    ])));
    fixture = TestBed.createComponent(ServiceProfitWorkQueueComponent); fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect((element.querySelector('select') as HTMLSelectElement).value).toBe('DUE_STATE');
    expect(element.textContent).toContain('Overdue');
    expect(element.textContent).toContain('No Due Date');
    const groupSelect = element.querySelectorAll<HTMLSelectElement>('.controls select')[0];
    groupSelect.value = 'DISPOSITION'; groupSelect.dispatchEvent(new Event('change')); fixture.detectChanges();
    expect(element.textContent).toContain('Follow Up Required');
    expect(element.textContent).toContain('No Response Recorded');
  });

  it('supports priority and None grouping and renders only safe card fields', () => {
    const element = fixture.nativeElement as HTMLElement;
    const groupSelect = element.querySelectorAll<HTMLSelectElement>('.controls select')[0];
    groupSelect.value = 'PRIORITY'; groupSelect.dispatchEvent(new Event('change')); fixture.detectChanges();
    expect(element.textContent).toContain('High');
    groupSelect.value = 'NONE'; groupSelect.dispatchEvent(new Event('change')); fixture.detectChanges();
    expect(element.textContent).toContain('Brake work recovery');
    expect(element.textContent).not.toContain('tenant');
    expect(element.textContent).not.toContain('USD');
    expect(element.textContent).not.toContain('Revenue');
  });

  it('maps filters and only shows Clear Filters while filters are active', async () => {
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.clear')).toBeNull();
    element.querySelector<HTMLButtonElement>('.filter-trigger')!.click(); fixture.detectChanges();
    const selects = element.querySelectorAll<HTMLSelectElement>('.filter-panel select');
    selects[0].value = 'COMPLETED'; selects[0].dispatchEvent(new Event('change'));
    selects[1].value = 'OVERDUE'; selects[1].dispatchEvent(new Event('change'));
    selects[2].value = 'FOLLOW_UP_REQUIRED'; selects[2].dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(element.querySelector('.clear')).not.toBeNull();
    element.querySelector<HTMLButtonElement>('.apply')!.click(); fixture.detectChanges(); await fixture.whenStable();
    expect(api.getWorkQueue).toHaveBeenLastCalledWith({ ownership: 'ALL', page: 0, size: 25, handlingStatus: 'COMPLETED', dueState: 'OVERDUE', disposition: 'FOLLOW_UP_REQUIRED' });
    element.querySelector<HTMLButtonElement>('.clear')!.click(); fixture.detectChanges(); await fixture.whenStable();
    expect(api.getWorkQueue).toHaveBeenLastCalledWith({ ownership: 'ALL', page: 0, size: 25 });
  });

  it('refreshes without navigating and loads another bounded page', () => {
    const refresh = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.refresh')!;
    refresh.click(); fixture.detectChanges();
    expect(api.getWorkQueue).toHaveBeenLastCalledWith({ ownership: 'ALL', page: 0, size: 25 });
    api.getWorkQueue.mockReturnValue(of(page([item({ followUpId: 'follow-up-2' })], 2)));
    fixture = TestBed.createComponent(ServiceProfitWorkQueueComponent); fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.load-more')!.click(); fixture.detectChanges();
    expect(api.getWorkQueue).toHaveBeenLastCalledWith({ ownership: 'ALL', page: 1, size: 25 });
  });

  it('renders loading, empty, and API error states', () => {
    api.getWorkQueue.mockReturnValue(throwError(() => ({ status: 503 })));
    fixture = TestBed.createComponent(ServiceProfitWorkQueueComponent); fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Unable to load Service Profit work queue');
    api.getWorkQueue.mockReturnValue(of(page([])));
    fixture = TestBed.createComponent(ServiceProfitWorkQueueComponent); fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No follow-ups match these filters');
  });
});
