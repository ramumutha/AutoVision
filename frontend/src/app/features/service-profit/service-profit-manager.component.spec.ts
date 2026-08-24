import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { routes } from '../../app.routes';
import { authenticatedGuard } from '../../core/auth/auth.guard';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitManagerComponent } from './service-profit-manager.component';
import { ServiceProfitOpportunityPage, ServiceProfitOpportunityResponse, ServiceProfitOpportunitySummary } from './service-profit.models';

describe('ServiceProfitManagerComponent', () => {
  let fixture: ComponentFixture<ServiceProfitManagerComponent>;
  let api: {
    getDataCapability: ReturnType<typeof vi.fn>;
    getSummary: ReturnType<typeof vi.fn>;
    getOpportunities: ReturnType<typeof vi.fn>;
    getOpportunity: ReturnType<typeof vi.fn>;
  };

  const summary: ServiceProfitOpportunitySummary = {
    totalOpportunities: 4, highPriorityCount: 2, reviewRequiredCount: 1, readyCount: 2, suppressedCount: 1,
    potentialByCurrency: [{ currencyCode: 'USD', amount: 100 }, { currencyCode: 'EUR', amount: 75 }],
    byOpportunityType: [], byPriority: [], byActionability: [],
  };
  const queue: ServiceProfitOpportunityPage = {
    items: [{
      id: 'opportunity-1', tenantId: 'tenant-1', dealerId: 'dealer-1', branchId: 'branch-1', locationId: 'location-1',
      opportunityKey: 'declined-1', opportunityType: 'DECLINED_WORK', status: 'SUPPRESSED', evidenceClass: 'SOURCE_CONFIRMED',
      evidenceStrength: 'STRONG', priority: 'HIGH', actionability: 'SUPPRESSED', title: 'Recover declined brake work',
      potentialAmount: 320, currencyCode: 'USD', detectedAt: '2026-08-20T10:00:00Z',
    }], page: 0, size: 25, totalElements: 1, totalPages: 1,
  };
  const detail: ServiceProfitOpportunityResponse = {
    ...queue.items[0], customerId: 'customer-1', vehicleId: 'vehicle-1', summary: 'Brake work was declined.',
    sourceSystem: 'DMS', sourceEntityType: 'SERVICE_LINE', sourceEntityId: 'line-1', sourceServiceOrderId: 'order-1',
    sourceServiceJobId: null, sourceServiceLineId: 'line-1', sourceQuoteId: null, policyVersion: 'service-profit-r1',
    suppressionReason: 'WORK_ALREADY_COMPLETED', suppressedAt: '2026-08-21T10:00:00Z',
    explanation: { headline: 'Previously declined work was identified', rationale: 'The source record shows a customer decline.', evidenceBasis: 'Confirmed service line history.', recommendedAction: 'No action while suppressed.' },
    version: 1, createdAt: '2026-08-20T10:00:00Z', updatedAt: '2026-08-21T10:00:00Z',
    context: {
      customer: { displayName: 'Arjun Mehta', reference: 'CUST-001', phone: '+919900000001', email: 'arjun.mehta@example.demo', contactable: true },
      vehicle: { registration: 'KA01AV1001', vin: 'VINDEMO00000000001', make: 'Demo Motors', model: 'City Prime', modelYear: 2022, powertrain: 'ICE' },
      service: { orderReference: 'RO-1001', serviceDate: '2026-07-01', description: 'Front brake pad replacement', advisorContext: 'Customer declined front brake work today.' },
    },
  };

  async function createComponent(initialUrl?: string): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ServiceProfitManagerComponent],
      providers: [provideRouter([]), { provide: ServiceProfitApiService, useValue: api }],
    }).compileComponents();
    if (initialUrl) await TestBed.inject(Router).navigateByUrl(initialUrl);
    fixture = TestBed.createComponent(ServiceProfitManagerComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    api = {
      getDataCapability: vi.fn().mockReturnValue(of({ assessmentState: 'NOT_ASSESSED', sourceDatasetId: null, sourceDatasetVersion: null, assessmentPolicyVersion: null, assessedAt: null, capabilities: [] })),
      getSummary: vi.fn().mockReturnValue(of(summary)),
      getOpportunities: vi.fn().mockReturnValue(of(queue)),
      getOpportunity: vi.fn().mockReturnValue(of(detail)),
    };
  });

  it('reuses the existing authenticated route guard', () => {
    const route = routes.find((candidate) => candidate.path === 'service-profit');
    expect(route?.canActivate).toContain(authenticatedGuard);
  });

  it('loads summary and queue while keeping each recoverable currency separate', async () => {
    await createComponent();
    const element = fixture.nativeElement as HTMLElement;
    const text = element.textContent ?? '';
    const currencyRows = Array.from(element.querySelectorAll('.currency-values p')).map((row) => row.textContent?.replaceAll(/\s/g, ''));
    expect(api.getSummary).toHaveBeenCalledOnce();
    expect(api.getOpportunities).toHaveBeenCalledOnce();
    expect(currencyRows).toEqual(['100USD', '75EUR']);
    expect(currencyRows).not.toContain('175USD');
    expect(text).toContain('Recover declined brake work');
  });

  it('represents loading, empty, and API failure states', async () => {
    const pending = new Subject<ServiceProfitOpportunitySummary>();
    api.getSummary.mockReturnValue(pending);
    await createComponent();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Loading Service Profit opportunities');

    pending.error({ status: 500, message: 'Unavailable' });
    await fixture.whenStable();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Unable to load Service Profit');

    TestBed.resetTestingModule();
    api.getSummary.mockReturnValue(of(summary));
    api.getOpportunities.mockReturnValue(of({ ...queue, items: [], totalElements: 0, totalPages: 0 }));
    await createComponent();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('No opportunities match these filters');
  });

  it('loads the authoritative explanation and exposes suppression state when selected', async () => {
    await createComponent();
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.opportunity-button')?.click();
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(api.getOpportunity).toHaveBeenCalledWith('opportunity-1');
    expect(text).toContain('Why AutoVision found this');
    expect(text).toContain('Previously declined work was identified');
    expect(text).toContain('Confirmed service line history.');
    expect(text).toContain('No action while suppressed.');
    expect(text).toContain('Suppressed opportunity');
    expect(text).toContain('Do not action');
    expect(text).toContain('Work Already Completed');
    expect(text).toContain('Actionability');
    expect(text).toContain('Evidence Strength');
    expect(text).toContain('Arjun Mehta');
    expect(text).toContain('CUST-001');
    expect(text).toContain('KA01AV1001');
    expect(text).toContain('Demo Motors City Prime \u00B7 2022');
    expect(text).toContain('RO-1001');
    expect(text).toContain('Front brake pad replacement');
    expect(text).toContain('$320');
    expect(text).not.toContain('customer-1');
    expect(text).not.toContain('vehicle-1');
    expect(element.querySelector('.recommended-action')?.textContent).toContain('No action while suppressed.');
    expect(element.querySelector('.suppression')?.textContent).toContain('Do not action');
    expect(element.querySelector('.detail-statuses')?.textContent).not.toContain('Ready');
    const audit = element.querySelector('details.audit-details');
    expect(audit?.querySelector('summary')?.textContent).toContain('Evidence & audit details');
    expect(audit?.textContent).toContain('line-1');
    expect(audit?.hasAttribute('open')).toBe(false);
    expect(element.querySelector('.opportunity-button')?.tagName).toBe('BUTTON');
    expect(element.querySelector('.selection-label')?.textContent).toContain('Selected');
    expect(text).not.toContain('Ready to Action Ready');
  });

  it('handles missing and partial context without rendering empty rows or raw IDs', async () => {
    api.getOpportunity.mockReturnValue(of({
      ...detail,
      customerId: 'hidden-customer-uuid',
      vehicleId: 'hidden-vehicle-uuid',
      context: {
        customer: { displayName: null, reference: 'CUST-010', phone: null, email: null, contactable: false },
        vehicle: { registration: null, vin: 'VINDEMO00000000010', make: null, model: null, modelYear: null, powertrain: null },
        service: null,
      },
    }));
    await createComponent();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.opportunity-button')?.click();
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    const text = element.textContent ?? '';
    expect(text).toContain('CUST-010');
    expect(text).toContain('Contact information incomplete');
    expect(text).toContain('VINDEMO00000000010');
    expect(element.querySelector('.service-context')).toBeNull();
    expect(text).not.toContain('hidden-customer-uuid');
    expect(text).not.toContain('hidden-vehicle-uuid');

    api.getOpportunity.mockReturnValue(of({ ...detail, context: null }));
    const opportunityButton = element.querySelector<HTMLButtonElement>('.opportunity-button');
    opportunityButton?.click();
    fixture.detectChanges();
    opportunityButton?.click();
    fixture.detectChanges();
    expect(element.querySelector('.context-grid')).toBeNull();
    expect((element.textContent ?? '')).toContain('Previously declined work was identified');
  });

  it('clearly represents review-required opportunities before customer contact', async () => {
    api.getOpportunity.mockReturnValue(of({ ...detail, status: 'DETECTED', actionability: 'REVIEW_REQUIRED', suppressionReason: null, suppressedAt: null }));
    await createComponent();

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.opportunity-button')?.click();
    fixture.detectChanges();

    const review = (fixture.nativeElement as HTMLElement).querySelector('.review-required');
    expect(review?.textContent).toContain('Review Required');
    expect(review?.textContent).toContain('Review is required before customer contact.');
  });

  it('surfaces a detail 401 instead of silently discarding the failure', async () => {
    api.getOpportunity.mockReturnValue(throwError(() => ({ status: 401, message: 'Unauthorized' })));
    await createComponent();

    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.opportunity-button')?.click();
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Unable to load opportunity details');
    expect(text).toContain('Your session could not be renewed. Sign in again to view this opportunity.');
  });

  it('renders detail loading immediately after the selected table row', async () => {
    api.getOpportunity.mockReturnValue(new Subject<ServiceProfitOpportunityResponse>());
    await createComponent();
    const row = (fixture.nativeElement as HTMLElement).querySelector<HTMLTableRowElement>('.opportunity-row');

    row?.querySelector<HTMLButtonElement>('.opportunity-button')?.click();
    fixture.detectChanges();

    const inlineRow = row?.nextElementSibling as HTMLTableRowElement | null;
    expect(inlineRow?.classList.contains('inline-detail-row')).toBe(true);
    expect(inlineRow?.parentElement?.tagName).toBe('TBODY');
    expect(inlineRow?.querySelector('td')?.colSpan).toBe(7);
    expect(inlineRow?.textContent).toContain('Loading opportunity details');
  });

  it('renders loaded detail immediately after the selected table row', async () => {
    await createComponent();
    const row = (fixture.nativeElement as HTMLElement).querySelector<HTMLTableRowElement>('.opportunity-row');

    row?.querySelector<HTMLButtonElement>('.opportunity-button')?.click();
    fixture.detectChanges();

    const inlineRow = row?.nextElementSibling as HTMLTableRowElement | null;
    expect(inlineRow?.querySelector('app-service-profit-opportunity-detail')).not.toBeNull();
    expect(inlineRow?.textContent).toContain('Previously declined work was identified');
  });

  it('renders detail errors immediately after the selected table row', async () => {
    api.getOpportunity.mockReturnValue(throwError(() => ({ status: 403, message: 'Forbidden' })));
    await createComponent();
    const row = (fixture.nativeElement as HTMLElement).querySelector<HTMLTableRowElement>('.opportunity-row');

    row?.querySelector<HTMLButtonElement>('.opportunity-button')?.click();
    fixture.detectChanges();

    expect(row?.nextElementSibling?.classList.contains('inline-detail-row')).toBe(true);
    expect(row?.nextElementSibling?.textContent).toContain('Unable to load opportunity details');
    expect(row?.nextElementSibling?.textContent).toContain('You are not authorized to view this opportunity.');
  });

  it('moves the single inline detail when another opportunity is selected', async () => {
    const secondItem = { ...queue.items[0], id: 'opportunity-2', title: 'Recover overdue service' };
    api.getOpportunities.mockReturnValue(of({ ...queue, items: [queue.items[0], secondItem], totalElements: 2 }));
    api.getOpportunity.mockImplementation((opportunityId: string) => of({
      ...detail,
      id: opportunityId,
      title: opportunityId === 'opportunity-2' ? 'Recover overdue service' : detail.title,
    }));
    await createComponent();
    const element = fixture.nativeElement as HTMLElement;
    const buttons = element.querySelectorAll<HTMLButtonElement>('.opportunity-button');

    buttons[0].click();
    fixture.detectChanges();
    buttons[1].click();
    fixture.detectChanges();

    expect(element.querySelectorAll('.inline-detail-row')).toHaveLength(1);
    const rows = element.querySelectorAll<HTMLTableRowElement>('.opportunity-row');
    expect(rows[0].nextElementSibling?.classList.contains('inline-detail-row')).toBe(false);
    expect(rows[1].nextElementSibling?.textContent).toContain('Recover overdue service');
  });

  it('prevents stale detail responses from appearing under a newer selection', async () => {
    const secondItem = { ...queue.items[0], id: 'opportunity-2', title: 'Second queue opportunity' };
    const firstResponse = new Subject<ServiceProfitOpportunityResponse>();
    const secondResponse = new Subject<ServiceProfitOpportunityResponse>();
    api.getOpportunities.mockReturnValue(of({ ...queue, items: [queue.items[0], secondItem], totalElements: 2 }));
    api.getOpportunity.mockImplementation((opportunityId: string) => opportunityId === 'opportunity-1' ? firstResponse : secondResponse);
    await createComponent();
    const element = fixture.nativeElement as HTMLElement;
    const buttons = element.querySelectorAll<HTMLButtonElement>('.opportunity-button');

    buttons[0].click();
    fixture.detectChanges();
    buttons[1].click();
    fixture.detectChanges();
    secondResponse.next({ ...detail, id: 'opportunity-2', title: 'Latest detail' });
    secondResponse.complete();
    fixture.detectChanges();
    firstResponse.next({ ...detail, title: 'Stale detail' });
    firstResponse.complete();
    fixture.detectChanges();

    expect(element.querySelector('.inline-detail-row')?.textContent).toContain('Latest detail');
    expect(element.textContent).not.toContain('Stale detail');
  });

  it('clears an expanded opportunity when filtering removes it from the queue', async () => {
    const remainingItem = { ...queue.items[0], id: 'opportunity-2', title: 'Remaining opportunity' };
    await createComponent();
    const element = fixture.nativeElement as HTMLElement;
    element.querySelector<HTMLButtonElement>('.opportunity-button')?.click();
    fixture.detectChanges();
    api.getOpportunities.mockReturnValueOnce(of({ ...queue, items: [remainingItem] }));

    const priority = element.querySelector<HTMLSelectElement>('.filters select');
    if (!priority) throw new Error('Priority filter was not rendered');
    priority.value = 'HIGH';
    priority.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.inline-detail-row')).toBeNull();
    expect(element.querySelector<HTMLButtonElement>('.opportunity-button')?.getAttribute('aria-expanded')).toBe('false');
  });

  it('exposes disclosure ARIA and collapses the selected row on a second activation', async () => {
    await createComponent();
    const element = fixture.nativeElement as HTMLElement;
    const button = element.querySelector<HTMLButtonElement>('.opportunity-button');

    expect(button?.getAttribute('aria-expanded')).toBe('false');
    expect(button?.getAttribute('aria-controls')).toBe('opportunity-detail-opportunity-1');
    button?.click();
    fixture.detectChanges();
    expect(button?.getAttribute('aria-expanded')).toBe('true');
    expect(element.querySelector('#opportunity-detail-opportunity-1')?.getAttribute('role')).toBe('region');

    button?.click();
    fixture.detectChanges();
    expect(button?.getAttribute('aria-expanded')).toBe('false');
    expect(element.querySelector('.inline-detail-row')).toBeNull();
    expect(api.getOpportunity).toHaveBeenCalledOnce();
  });

  it('marks secondary table columns for compact tablet presentation', async () => {
    await createComponent();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelectorAll('thead .secondary-tablet')).toHaveLength(3);
    expect(element.querySelectorAll('.opportunity-row .secondary-tablet')).toHaveLength(3);
  });

  it('provides a compact accessible refresh control that invokes refresh', async () => {
    await createComponent();
    const refresh = (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.refresh-control');

    expect(refresh?.getAttribute('aria-label')).toBe('Refresh Service Profit data');
    expect(refresh?.getAttribute('aria-busy')).toBe('false');
    expect(refresh?.getAttribute('title')).toBe('Refresh');
    expect(refresh?.querySelector('svg')).not.toBeNull();
    expect(refresh?.textContent?.trim()).toBe('');
    refresh?.click();

    expect(api.getSummary).toHaveBeenCalledTimes(2);
    expect(api.getOpportunities).toHaveBeenCalledTimes(2);
  });

  it('groups Priority, Actionability, and Sort with native accessible labels', async () => {
    await createComponent();
    const filters = (fixture.nativeElement as HTMLElement).querySelector<HTMLFieldSetElement>('fieldset.filters');
    const selects = filters?.querySelectorAll<HTMLSelectElement>('select');

    expect(filters?.tagName).toBe('FIELDSET');
    expect(filters?.getAttribute('aria-label')).toBeNull();
    expect(filters?.querySelector('legend')?.textContent).toBe('Opportunity filters');
    expect(Array.from(selects ?? []).map((select) => select.labels?.[0]?.textContent?.trim())).toEqual([
      'Priority AllHighMediumLow',
      'Actionability AllReadyReview RequiredContact Data MissingBlockedSuppressed',
      'Sort by NewestOldestHighest potentialLowest potential',
    ]);
  });

  it('renders accessible single-select opportunity type navigation', async () => {
    await createComponent();
    const group = (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('.opportunity-type-navigation');
    const buttons = Array.from(group?.querySelectorAll('button') ?? []);

    expect(group?.getAttribute('role')).toBe('group');
    expect(group?.getAttribute('aria-label')).toBe('Opportunity types');
    expect(buttons.map((button) => button.textContent?.trim())).toEqual([
      'All4', 'Declined Work', 'Deferred Work', 'Due Service', 'Overdue Service', 'Inactive Customer',
    ]);
    expect(buttons[0].getAttribute('aria-pressed')).toBe('true');
  });

  it('uses native focusable buttons for keyboard-accessible type selection', async () => {
    await createComponent();
    const typeButton = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.opportunity-type-navigation button')[1];

    typeButton.focus();
    expect(typeButton.tagName).toBe('BUTTON');
    expect(typeButton.type).toBe('button');
    expect(document.activeElement).toBe(typeButton);
    typeButton.click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(typeButton.getAttribute('aria-pressed')).toBe('true');
  });

  it('applies and clears the server-side opportunity type query', async () => {
    await createComponent();
    const buttons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.opportunity-type-navigation button'));

    buttons[1].click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(api.getSummary).toHaveBeenLastCalledWith({ opportunityType: 'DECLINED_WORK' });
    expect(buttons[1].getAttribute('aria-pressed')).toBe('true');

    buttons[0].click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(api.getSummary).toHaveBeenLastCalledWith({});
  });

  it('shows only authoritative opportunity type counts, including zero', async () => {
    api.getSummary.mockReturnValue(of({
      ...summary,
      byOpportunityType: [
        { key: 'DECLINED_WORK', count: 3 },
        { key: 'DEFERRED_WORK', count: 0 },
      ],
    }));
    await createComponent();
    const buttons = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.opportunity-type-navigation button'));

    expect(buttons[1].querySelector('.type-count')?.textContent).toBe('3');
    expect(buttons[2].querySelector('.type-count')?.textContent).toBe('0');
    expect(buttons[3].querySelector('.type-count')).toBeNull();
  });

  it('applies priority and actionability as server-side filters', async () => {
    await createComponent();
    const selects = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLSelectElement>('.filters select');
    const priority = selects[0];
    priority.value = 'HIGH';
    priority.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();
    expect(api.getSummary).toHaveBeenLastCalledWith({ priority: 'HIGH' });
    expect(api.getOpportunities).toHaveBeenLastCalledWith(expect.objectContaining({ priority: 'HIGH' }));

    const actionability = selects[1];
    actionability.value = 'REVIEW_REQUIRED';
    actionability.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();
    expect(api.getSummary).toHaveBeenLastCalledWith({ priority: 'HIGH', actionability: 'REVIEW_REQUIRED' });
  });

  it.each([
    ['DETECTED_DESC', 'Newest'],
    ['DETECTED_ASC', 'Oldest'],
    ['POTENTIAL_DESC', 'Highest potential'],
    ['POTENTIAL_ASC', 'Lowest potential'],
  ] as const)('applies the supported %s sort option labelled %s', async (sortValue, sortLabel) => {
    await createComponent();
    const sort = (fixture.nativeElement as HTMLElement).querySelectorAll<HTMLSelectElement>('.filters select')[2];
    expect(sort.labels?.[0]?.textContent).toContain('Sort by');
    expect(Array.from(sort.options).find((option) => option.value === sortValue)?.textContent).toBe(sortLabel);

    sort.value = sortValue;
    sort.dispatchEvent(new Event('change'));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.getOpportunities).toHaveBeenLastCalledWith(expect.objectContaining({ sort: sortValue }));
  });

  it('restores valid URL state and safely ignores unsupported values', async () => {
    await createComponent('/?type=DECLINED_WORK&priority=HIGH&actionability=READY&sort=POTENTIAL_DESC');
    expect(api.getSummary).toHaveBeenLastCalledWith({ opportunityType: 'DECLINED_WORK', priority: 'HIGH', actionability: 'READY' });
    expect(api.getOpportunities).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'POTENTIAL_DESC' }));

    TestBed.resetTestingModule();
    await createComponent('/?type=UNKNOWN&priority=URGENT&actionability=UNKNOWN&sort=PRIORITY_DESC');
    expect(api.getSummary).toHaveBeenLastCalledWith({});
    expect(api.getOpportunities).toHaveBeenLastCalledWith(expect.objectContaining({ sort: 'DETECTED_DESC' }));
  });

  it('cancels an older manager request when query state changes again', async () => {
    await createComponent();
    const olderSummary = new Subject<ServiceProfitOpportunitySummary>();
    const olderQueue = new Subject<ServiceProfitOpportunityPage>();
    api.getSummary.mockReturnValueOnce(olderSummary).mockReturnValueOnce(of({ ...summary, totalOpportunities: 9 }));
    api.getOpportunities.mockReturnValueOnce(olderQueue).mockReturnValueOnce(of({
      ...queue,
      items: [{ ...queue.items[0], title: 'Newest query result' }],
    }));
    const router = TestBed.inject(Router);

    await router.navigate([], { queryParams: { priority: 'HIGH' } });
    await router.navigate([], { queryParams: { priority: 'HIGH', actionability: 'READY' } });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Newest query result');

    olderSummary.next({ ...summary, totalOpportunities: 99 });
    olderSummary.complete();
    olderQueue.next({ ...queue, items: [{ ...queue.items[0], title: 'Stale query result' }] });
    olderQueue.complete();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Stale query result');
  });

  it('keeps loaded content visible while a background refresh is pending', async () => {
    await createComponent();
    const pendingSummary = new Subject<ServiceProfitOpportunitySummary>();
    const pendingQueue = new Subject<ServiceProfitOpportunityPage>();
    api.getSummary.mockReturnValueOnce(pendingSummary);
    api.getOpportunities.mockReturnValueOnce(pendingQueue);
    const element = fixture.nativeElement as HTMLElement;
    const refresh = element.querySelector<HTMLButtonElement>('.refresh-control');

    refresh?.click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Recover declined brake work');
    expect(refresh?.disabled).toBe(true);
    expect(refresh?.getAttribute('aria-busy')).toBe('true');
    expect(refresh?.textContent?.trim()).toBe('');

    pendingSummary.next(summary);
    pendingSummary.complete();
    pendingQueue.next(queue);
    pendingQueue.complete();
    fixture.detectChanges();
    expect(refresh?.disabled).toBe(false);
    expect(refresh?.getAttribute('aria-busy')).toBe('false');
  });

  it('retains loaded content and reports a background refresh failure', async () => {
    await createComponent();
    api.getSummary.mockReturnValueOnce(throwError(() => ({ status: 503, message: 'Unavailable' })));
    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>('.refresh-control')?.click();
    fixture.detectChanges();

    expect(element.textContent).toContain('Recover declined brake work');
    expect(element.textContent).toContain('Unable to refresh Service Profit');
    expect(element.textContent).toContain('The previously loaded data is still available.');
  });
});