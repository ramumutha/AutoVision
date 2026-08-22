import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';
import { routes } from '../../app.routes';
import { authenticatedGuard } from '../../core/auth/auth.guard';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitManagerComponent } from './service-profit-manager.component';
import { ServiceProfitOpportunityPage, ServiceProfitOpportunityResponse, ServiceProfitOpportunitySummary } from './service-profit.models';

describe('ServiceProfitManagerComponent', () => {
  let fixture: ComponentFixture<ServiceProfitManagerComponent>;
  let api: {
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
  };

  async function createComponent(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ServiceProfitManagerComponent],
      providers: [{ provide: ServiceProfitApiService, useValue: api }],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceProfitManagerComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  beforeEach(() => {
    api = {
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
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.opportunity-button')?.click();
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
    expect(text).toContain('Work Already Completed');
    expect(text).not.toContain('Ready to Action Ready');
  });

  it('reloads manager data with backend-supported filter values', async () => {
    await createComponent();
    const priority = (fixture.nativeElement as HTMLElement).querySelector<HTMLSelectElement>('select');
    if (!priority) throw new Error('Priority filter was not rendered');
    priority.value = 'HIGH';
    priority.dispatchEvent(new Event('change'));
    fixture.detectChanges();
    expect(api.getSummary).toHaveBeenLastCalledWith({ priority: 'HIGH' });
    expect(api.getOpportunities).toHaveBeenLastCalledWith(expect.objectContaining({ priority: 'HIGH' }));
  });
});