import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitManagerComponent } from './service-profit-manager.component';
import { ServiceProfitOpportunityFilters, ServiceProfitOpportunityPage, ServiceProfitOpportunitySummary } from './service-profit.models';

describe('ServiceProfitManagerComponent secondary filters', () => {
  let fixture: ComponentFixture<ServiceProfitManagerComponent>;
  let router: Router;
  let api: {
    getDataCapability: ReturnType<typeof vi.fn>;
    getSummary: ReturnType<typeof vi.fn>;
    getOpportunities: ReturnType<typeof vi.fn>;
    getOpportunity: ReturnType<typeof vi.fn>;
  };

  const summary: ServiceProfitOpportunitySummary = {
    totalOpportunities: 3, highPriorityCount: 2, reviewRequiredCount: 1, readyCount: 1, suppressedCount: 0,
    potentialByCurrency: [], byOpportunityType: [], byPriority: [], byActionability: [],
  };
  const queue: ServiceProfitOpportunityPage = { items: [], page: 0, size: 25, totalElements: 0, totalPages: 0 };

  beforeEach(() => {
    api = {
      getDataCapability: vi.fn().mockReturnValue(of({
        assessmentState: 'NOT_ASSESSED', sourceDatasetId: null, sourceDatasetVersion: null,
        assessmentPolicyVersion: null, assessedAt: null, capabilities: [],
      })),
      getSummary: vi.fn().mockReturnValue(of(summary)),
      getOpportunities: vi.fn().mockReturnValue(of(queue)),
      getOpportunity: vi.fn(),
    };
  });

  async function createComponent(initialUrl = '/'): Promise<HTMLElement> {
    await TestBed.configureTestingModule({
      imports: [ServiceProfitManagerComponent],
      providers: [provideRouter([]), { provide: ServiceProfitApiService, useValue: api }],
    }).compileComponents();
    router = TestBed.inject(Router);
    await router.navigateByUrl(initialUrl);
    fixture = TestBed.createComponent(ServiceProfitManagerComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  function openFilters(element: HTMLElement): NodeListOf<HTMLSelectElement> {
    element.querySelector<HTMLButtonElement>('.disclosure-trigger')!.click();
    fixture.detectChanges();
    return element.querySelectorAll<HTMLSelectElement>('.filter-panel select');
  }

  function change(select: HTMLSelectElement, value: string): void {
    select.value = value;
    select.dispatchEvent(new Event('change'));
  }

  it('keeps pending edits out of the URL and applies all dimensions atomically', async () => {
    const element = await createComponent('/?sort=POTENTIAL_DESC');
    const selects = openFilters(element);
    change(selects[0], 'DEFERRED_WORK');
    change(selects[1], 'MEDIUM');
    change(selects[2], 'BLOCKED');
    expect(router.url).toBe('/?sort=POTENTIAL_DESC');

    element.querySelector<HTMLButtonElement>('.apply-action')!.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(router.parseUrl(router.url).queryParams).toEqual({
      type: 'DEFERRED_WORK', priority: 'MEDIUM', actionability: 'BLOCKED', sort: 'POTENTIAL_DESC',
    });
  });

  it('clears Type, Priority, and Actionability while preserving Sort', async () => {
    const element = await createComponent('/?type=DECLINED_WORK&priority=HIGH&actionability=READY&sort=POTENTIAL_ASC');
    openFilters(element);
    element.querySelector<HTMLButtonElement>('.clear-action')!.click();
    element.querySelector<HTMLButtonElement>('.apply-action')!.click();
    await fixture.whenStable();

    expect(router.parseUrl(router.url).queryParams).toEqual({ sort: 'POTENTIAL_ASC' });
  });

  it('composes Opportunity Type with a canonical KPI and excludes the KPI from the badge', async () => {
    const element = await createComponent('/?priority=HIGH');
    expect(element.querySelector('.active-count')).toBeNull();
    const selects = openFilters(element);
    change(selects[0], 'DEFERRED_WORK');
    element.querySelector<HTMLButtonElement>('.apply-action')!.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(router.parseUrl(router.url).queryParams).toEqual({ type: 'DEFERRED_WORK', priority: 'HIGH' });
    expect(element.querySelector('.active-count')?.textContent).toContain('1');
    expect(element.querySelectorAll<HTMLButtonElement>('app-service-profit-business-navigation button')[1].getAttribute('aria-pressed')).toBe('true');
  });

  it('clears KPI selection for a noncanonical advanced combination', async () => {
    const element = await createComponent('/?priority=HIGH');
    const selects = openFilters(element);
    change(selects[2], 'BLOCKED');
    element.querySelector<HTMLButtonElement>('.apply-action')!.click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect(Array.from(element.querySelectorAll<HTMLButtonElement>('app-service-profit-business-navigation button'))
      .every((button) => button.getAttribute('aria-pressed') === 'false')).toBe(true);
    expect(element.querySelector('#opportunity-queue-heading')?.textContent).toContain('Filtered Opportunities');
  });

});