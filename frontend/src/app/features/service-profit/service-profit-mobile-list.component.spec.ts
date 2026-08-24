import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ServiceProfitMobileListComponent } from './service-profit-mobile-list.component';
import { ServiceProfitOpportunityQueueItem } from './service-profit.models';

describe('ServiceProfitMobileListComponent', () => {
  let fixture: ComponentFixture<ServiceProfitMobileListComponent>;

  const opportunity: ServiceProfitOpportunityQueueItem = {
    id: 'opportunity-1', tenantId: 'tenant-1', dealerId: 'dealer-1', branchId: 'branch-1', locationId: 'location-1',
    opportunityKey: 'declined-1', opportunityType: 'DECLINED_WORK', status: 'DETECTED', evidenceClass: 'SOURCE_CONFIRMED',
    evidenceStrength: 'STRONG', priority: 'HIGH', actionability: 'READY', title: 'Recover declined brake work',
    potentialAmount: 320, currencyCode: 'USD', detectedAt: '2026-08-20T10:00:00Z',
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServiceProfitMobileListComponent],
      providers: [provideRouter([])],
    }).compileComponents();
    fixture = TestBed.createComponent(ServiceProfitMobileListComponent);
    fixture.componentRef.setInput('opportunities', [opportunity]);
    fixture.componentRef.setInput('queryParams', {
      type: 'DECLINED_WORK', priority: 'HIGH', actionability: 'READY', sort: 'POTENTIAL_DESC',
    });
    fixture.detectChanges();
  });

  it('renders a semantic concise opportunity list', () => {
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.mobile-opportunity-list')?.tagName).toBe('UL');
    expect(element.querySelector('.mobile-opportunity-list > li')?.tagName).toBe('LI');
    const cardText = element.querySelector('.mobile-opportunity-card')?.textContent ?? '';
    expect(cardText).toContain('Recover declined brake work');
    expect(cardText).toContain('Declined Work');
    expect(cardText).toContain('$320');
    expect(cardText).toContain('High');
    expect(cardText).toContain('Ready');
    expect(cardText).toContain('Strong evidence');
    expect(cardText).not.toContain('tenant-1');
  });

  it('uses a descriptive real link and preserves list query state', () => {
    const link = (fixture.nativeElement as HTMLElement).querySelector<HTMLAnchorElement>('.mobile-opportunity-card');
    expect(link?.tagName).toBe('A');
    expect(link?.getAttribute('aria-label')).toContain('Recover declined brake work, High, Open details');
    expect(link?.getAttribute('href')).toContain('/service-profit/opportunities/opportunity-1');
    expect(link?.getAttribute('href')).toContain('type=DECLINED_WORK');
    expect(link?.getAttribute('href')).toContain('priority=HIGH');
    expect(link?.getAttribute('href')).toContain('actionability=READY');
    expect(link?.getAttribute('href')).toContain('sort=POTENTIAL_DESC');
  });
});
