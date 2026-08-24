import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Observable, Subject, of } from 'rxjs';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitManagerComponent } from './service-profit-manager.component';
import {
  ServiceProfitOpportunityFilters,
  ServiceProfitOpportunityPage,
  ServiceProfitOpportunitySummary,
} from './service-profit.models';

describe('ServiceProfitManagerComponent business navigation', () => {
  let fixture: ComponentFixture<ServiceProfitManagerComponent>;
  let api: {
    getDataCapability: ReturnType<typeof vi.fn>;
    getSummary: ReturnType<typeof vi.fn>;
    getOpportunities: ReturnType<typeof vi.fn>;
    getOpportunity: ReturnType<typeof vi.fn>;
  };

  const navigationSummary: ServiceProfitOpportunitySummary = {
    totalOpportunities: 10,
    highPriorityCount: 5,
    reviewRequiredCount: 1,
    readyCount: 8,
    suppressedCount: 1,
    potentialByCurrency: [{ currencyCode: 'INR', amount: 97400 }],
    byOpportunityType: [],
    byPriority: [],
    byActionability: [],
  };

  const emptyQueue: ServiceProfitOpportunityPage = {
    items: [],
    page: 0,
    size: 25,
    totalElements: 0,
    totalPages: 0,
  };

  function filteredSummary(filters: ServiceProfitOpportunityFilters): ServiceProfitOpportunitySummary {
    if (filters.actionability === 'REVIEW_REQUIRED') return { ...navigationSummary, totalOpportunities: 1 };
    if (filters.actionability === 'READY') return { ...navigationSummary, totalOpportunities: 8 };
    if (filters.priority === 'HIGH') return { ...navigationSummary, totalOpportunities: 5, highPriorityCount: 5 };
    return navigationSummary;
  }

  async function createComponent(initialUrl = '/'): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ServiceProfitManagerComponent],
      providers: [provideRouter([]), { provide: ServiceProfitApiService, useValue: api }],
    }).compileComponents();

    await TestBed.inject(Router).navigateByUrl(initialUrl);
    fixture = TestBed.createComponent(ServiceProfitManagerComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    api = {
      getDataCapability: vi.fn().mockReturnValue(of({
        assessmentState: 'NOT_ASSESSED',
        sourceDatasetId: null,
        sourceDatasetVersion: null,
        assessmentPolicyVersion: null,
        assessedAt: null,
        capabilities: [],
      })),
      getSummary: vi.fn().mockImplementation((filters: ServiceProfitOpportunityFilters = {}) => of(filteredSummary(filters))),
      getOpportunities: vi.fn().mockReturnValue(of(emptyQueue)),
      getOpportunity: vi.fn(),
    };
  });

  it('uses Total as the canonical default and reports the authoritative result count', async () => {
    await createComponent();
    const element = fixture.nativeElement as HTMLElement;
    const buttons = element.querySelectorAll<HTMLButtonElement>('app-service-profit-business-navigation button');

    expect(buttons[0].getAttribute('aria-pressed')).toBe('true');
    expect(element.querySelector('#opportunity-queue-heading')?.textContent).toContain('Opportunities');
    expect(element.querySelector('.result-count')?.textContent).toContain('10');
    expect(api.getSummary).toHaveBeenCalledOnce();
  });

  it.each([
    ['?priority=HIGH', 1, 'Opportunities', '5'],
    ['?actionability=REVIEW_REQUIRED', 2, 'Opportunities', '1'],
    ['?actionability=READY', 3, 'Opportunities', '8'],
  ] as const)('restores the canonical KPI from %s', async (query, selectedIndex, heading, count) => {
    await createComponent(`/${query}`);
    const element = fixture.nativeElement as HTMLElement;
    const buttons = element.querySelectorAll<HTMLButtonElement>('app-service-profit-business-navigation button');

    expect(buttons[selectedIndex].getAttribute('aria-pressed')).toBe('true');
    expect(element.querySelector('#opportunity-queue-heading')?.textContent).toContain(heading);
    expect(element.querySelector('.result-count')?.textContent).toContain(count);
  });

  it('atomically replaces both KPI dimensions while preserving Type and Sort', async () => {
    await createComponent('/?type=DECLINED_WORK&priority=HIGH&actionability=READY&sort=POTENTIAL_DESC');
    const router = TestBed.inject(Router);
    const buttons = (fixture.nativeElement as HTMLElement)
      .querySelectorAll<HTMLButtonElement>('app-service-profit-business-navigation button');

    expect(Array.from(buttons).every((button) => button.getAttribute('aria-pressed') === 'false')).toBe(true);
    expect((fixture.nativeElement as HTMLElement).querySelector('#opportunity-queue-heading')?.textContent).toContain('Opportunities');

    buttons[2].click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(router.url).toContain('type=DECLINED_WORK');
    expect(router.url).toContain('actionability=REVIEW_REQUIRED');
    expect(router.url).toContain('sort=POTENTIAL_DESC');
    expect(router.url).not.toContain('priority=');

    buttons[0].click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(router.url).toContain('type=DECLINED_WORK');
    expect(router.url).toContain('sort=POTENTIAL_DESC');
    expect(router.url).not.toContain('priority=');
    expect(router.url).not.toContain('actionability=');
  });

  it.each([
    [0, null, null],
    [1, 'HIGH', null],
    [2, null, 'REVIEW_REQUIRED'],
    [3, null, 'READY'],
  ] as const)('maps KPI button %s to its canonical query state', async (buttonIndex, priority, actionability) => {
    await createComponent('/?type=DECLINED_WORK&priority=MEDIUM&actionability=BLOCKED&sort=POTENTIAL_ASC');
    const buttons = (fixture.nativeElement as HTMLElement)
      .querySelectorAll<HTMLButtonElement>('app-service-profit-business-navigation button');

    buttons[buttonIndex].click();
    await fixture.whenStable();
    fixture.detectChanges();

    const params = TestBed.inject(Router).parseUrl(TestBed.inject(Router).url).queryParams;
    expect(params['type']).toBe('DECLINED_WORK');
    expect(params['sort']).toBe('POTENTIAL_ASC');
    expect(params['priority'] ?? null).toBe(priority);
    expect(params['actionability'] ?? null).toBe(actionability);
  });

  it('retains Type-scoped navigation counts across KPI-only changes', async () => {
    await createComponent();
    const buttons = (fixture.nativeElement as HTMLElement)
      .querySelectorAll<HTMLButtonElement>('app-service-profit-business-navigation button');

    buttons[1].click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.getSummary).toHaveBeenCalledTimes(2);
    expect(buttons[1].querySelector('strong')?.textContent).toBe('5');
  });

  it('loads a new navigation summary when Type changes under an active KPI', async () => {
    await createComponent('/?priority=HIGH');
    api.getSummary.mockClear();
    await TestBed.inject(Router).navigate([], { queryParams: { type: 'DECLINED_WORK' }, queryParamsHandling: 'merge' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(api.getSummary).toHaveBeenCalledWith({ opportunityType: 'DECLINED_WORK', priority: 'HIGH' });
    expect(api.getSummary).toHaveBeenCalledWith({ opportunityType: 'DECLINED_WORK' });
  });

  it('prevents a stale navigation-summary response from replacing the latest Type context', async () => {
    const staleNavigation = new Subject<ServiceProfitOpportunitySummary>();
    api.getSummary.mockImplementation((filters: ServiceProfitOpportunityFilters = {}): Observable<ServiceProfitOpportunitySummary> => {
      if (!filters.opportunityType && !filters.priority && !filters.actionability) return staleNavigation;
      if (filters.opportunityType && !filters.priority && !filters.actionability) {
        return of({ ...navigationSummary, totalOpportunities: 3, highPriorityCount: 2 });
      }
      return of(filteredSummary(filters));
    });

    await createComponent('/?priority=HIGH');
    await TestBed.inject(Router).navigate([], {
      queryParams: { type: 'DECLINED_WORK' },
      queryParamsHandling: 'merge',
    });
    fixture.detectChanges();
    staleNavigation.next({ ...navigationSummary, highPriorityCount: 99 });
    staleNavigation.complete();
    fixture.detectChanges();

    const highCount = (fixture.nativeElement as HTMLElement)
      .querySelectorAll<HTMLButtonElement>('app-service-profit-business-navigation button')[1]
      .querySelector('strong')?.textContent;
    expect(highCount).toBe('2');
  });
});