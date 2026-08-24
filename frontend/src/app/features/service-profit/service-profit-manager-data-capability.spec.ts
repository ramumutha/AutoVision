import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitManagerComponent } from './service-profit-manager.component';
import {
  ServiceProfitDataCapabilityResponse,
  ServiceProfitOpportunityPage,
  ServiceProfitOpportunitySummary,
} from './service-profit.models';

describe('ServiceProfitManagerComponent data capability', () => {
  let fixture: ComponentFixture<ServiceProfitManagerComponent>;

  let api: {
    getDataCapability: ReturnType<typeof vi.fn>;
    getSummary: ReturnType<typeof vi.fn>;
    getOpportunities: ReturnType<typeof vi.fn>;
    getOpportunity: ReturnType<typeof vi.fn>;
  };

  const dataCapability: ServiceProfitDataCapabilityResponse = {
    assessmentState: 'ASSESSED',
    sourceDatasetId: 'AUTOVISION-SERVICE-PROFIT-R1-DEMO',
    sourceDatasetVersion: '1.0.0',
    assessmentPolicyVersion: 'service-profit-data-readiness-r1',
    assessedAt: '2026-08-24T05:00:00Z',
    capabilities: [
      {
        capability: 'REVENUE_ATTRIBUTION',
        status: 'AVAILABLE',
        reason: 'Invoice linkage supports revenue attribution.',
      },
      {
        capability: 'GROSS_PROFIT_ATTRIBUTION',
        status: 'PARTIAL',
        reason: 'Cost data is incomplete.',
      },
    ],
  };

  const summary: ServiceProfitOpportunitySummary = {
    totalOpportunities: 1,
    highPriorityCount: 1,
    reviewRequiredCount: 0,
    readyCount: 1,
    suppressedCount: 0,
    potentialByCurrency: [
      {
        currencyCode: 'INR',
        amount: 14500,
      },
    ],
    byOpportunityType: [],
    byPriority: [],
    byActionability: [],
  };

  const queue: ServiceProfitOpportunityPage = {
    items: [
      {
        id: 'opportunity-1',
        tenantId: 'tenant-1',
        dealerId: 'dealer-1',
        branchId: 'branch-1',
        locationId: 'location-1',
        opportunityKey: 'declined-1',
        opportunityType: 'DECLINED_WORK',
        status: 'DETECTED',
        evidenceClass: 'SOURCE_CONFIRMED',
        evidenceStrength: 'STRONG',
        priority: 'HIGH',
        actionability: 'READY',
        title: 'Recover declined brake work',
        potentialAmount: 14500,
        currencyCode: 'INR',
        detectedAt: '2026-08-24T05:00:00Z',
      },
    ],
    page: 0,
    size: 25,
    totalElements: 1,
    totalPages: 1,
  };

  async function createComponent(): Promise<void> {
    await TestBed.configureTestingModule({
      imports: [ServiceProfitManagerComponent],
      providers: [
        provideRouter([]),
        {
          provide: ServiceProfitApiService,
          useValue: api,
        },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(
      ServiceProfitManagerComponent,
    );

    fixture.detectChanges();

    await fixture.whenStable();

    fixture.detectChanges();
  }

  beforeEach(() => {
    api = {
      getDataCapability: vi.fn().mockReturnValue(
        of(dataCapability),
      ),
      getSummary: vi.fn().mockReturnValue(
        of(summary),
      ),
      getOpportunities: vi.fn().mockReturnValue(
        of(queue),
      ),
      getOpportunity: vi.fn(),
    };
  });

  it('loads capability independently and presents the commercial readiness summary', async () => {
    await createComponent();

    const element = fixture.nativeElement as HTMLElement;
    const capability =
      element.querySelector(
        'app-service-profit-data-capability',
      );

    expect(api.getDataCapability).toHaveBeenCalledOnce();
    expect(api.getSummary).toHaveBeenCalledOnce();
    expect(api.getOpportunities).toHaveBeenCalledOnce();

    expect(capability).not.toBeNull();
    expect(capability?.closest('.kpi-primary')).not.toBeNull();
    expect(capability?.textContent).toContain(
      'Revenue data',
    );
    expect(capability?.textContent).toContain(
      'Available',
    );
    expect(capability?.textContent).toContain(
      'Gross profit data',
    );
    expect(capability?.textContent).toContain(
      'Partial',
    );
  });

  it('keeps opportunity management operational when capability loading fails', async () => {
    api.getDataCapability.mockReturnValue(
      throwError(() => ({
        status: 503,
        message: 'Capability unavailable',
      })),
    );

    await createComponent();

    const element = fixture.nativeElement as HTMLElement;
    const text = element.textContent ?? '';

    expect(api.getDataCapability).toHaveBeenCalledOnce();
    expect(api.getSummary).toHaveBeenCalledOnce();
    expect(api.getOpportunities).toHaveBeenCalledOnce();

    expect(text).toContain(
      'Recover declined brake work',
    );

    const capability = element.querySelector(
      'app-service-profit-data-capability',
    );

    expect(capability).not.toBeNull();
    expect(capability?.textContent).toContain(
      'Data capability unavailable',
    );
    expect(capability?.textContent).not.toContain(
      'Data capability not yet assessed',
    );

    expect(text).not.toContain(
      'Unable to load Service Profit',
    );
  });

  it('does not reload tenant data capability when opportunity filters change', async () => {
    await createComponent();

    const element = fixture.nativeElement as HTMLElement;

    element.querySelector<HTMLButtonElement>(
      '.disclosure-trigger',
    )?.click();

    fixture.detectChanges();

    const priority = element.querySelectorAll<HTMLSelectElement>(
      '.filter-panel select',
    )[1];

    if (!priority) {
      throw new Error(
        'Priority filter was not rendered',
      );
    }

    priority.value = 'HIGH';

    priority.dispatchEvent(
      new Event('change'),
    );

    element.querySelector<HTMLButtonElement>(
      '.apply-action',
    )?.click();

    await fixture.whenStable();

    fixture.detectChanges();

    expect(api.getDataCapability).toHaveBeenCalledOnce();
    expect(api.getSummary).toHaveBeenCalledTimes(2);
    expect(api.getOpportunities).toHaveBeenCalledTimes(2);
  });
});