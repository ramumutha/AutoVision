import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ServiceProfitGroupedQueueComponent } from './service-profit-grouped-queue.component';
import { ServiceProfitOpportunityGroup, ServiceProfitOpportunityQueueItem } from './service-profit.models';

describe('ServiceProfitGroupedQueueComponent', () => {
  let fixture: ComponentFixture<ServiceProfitGroupedQueueComponent>;
  let element: HTMLElement;
  const opportunity = (id: string, type: 'DECLINED_WORK' | 'DEFERRED_WORK'): ServiceProfitOpportunityQueueItem => ({
    id, tenantId: 'tenant-1', dealerId: 'dealer-1', branchId: 'branch-1', locationId: 'location-1', opportunityKey: id,
    opportunityType: type, status: 'DETECTED', evidenceClass: 'SOURCE_CONFIRMED', evidenceStrength: 'STRONG', priority: 'HIGH',
    actionability: 'READY', title: `Opportunity ${id}`, potentialAmount: 100, currencyCode: 'USD', detectedAt: '2026-08-24T00:00:00Z',
  });
  const groups: ServiceProfitOpportunityGroup[] = [
    { key: 'DECLINED_WORK', authoritativeCount: 3, opportunities: [opportunity('one', 'DECLINED_WORK')], complete: false },
    { key: 'DEFERRED_WORK', authoritativeCount: 1, opportunities: [opportunity('two', 'DEFERRED_WORK')], complete: true },
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ServiceProfitGroupedQueueComponent], providers: [provideRouter([])] }).compileComponents();
    fixture = TestBed.createComponent(ServiceProfitGroupedQueueComponent);
    fixture.componentRef.setInput('groups', groups);
    fixture.componentRef.setInput('groupBy', 'OPPORTUNITY_TYPE');
    fixture.componentRef.setInput('sort', 'DETECTED_DESC');
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  });

  it('renders authoritative counts with every non-empty group collapsed by default', () => {
    const headings = Array.from(element.querySelectorAll<HTMLButtonElement>('.group-heading'));
    expect(headings.map((heading) => heading.querySelector('span')?.textContent)).toEqual(['Declined Work', 'Deferred Work']);
    expect(headings.map((heading) => heading.querySelector('.group-count')?.textContent)).toEqual(['3', '1']);
    expect(headings.every((heading) => heading.getAttribute('aria-expanded') === 'false')).toBe(true);
    expect(element.textContent).not.toContain('Some opportunities could not be displayed');
  });

  it('collapses and reopens groups independently', () => {
    const headings = element.querySelectorAll<HTMLButtonElement>('.group-heading');
    headings[0].click();
    fixture.detectChanges();

    expect(headings[0].getAttribute('aria-expanded')).toBe('true');
    expect(headings[1].getAttribute('aria-expanded')).toBe('false');
    expect(element.textContent).toContain('Opportunity one');
    expect(element.textContent).not.toContain('Opportunity two');

    headings[1].click();
    fixture.detectChanges();
    expect(element.textContent).toContain('Opportunity one');
    expect(element.textContent).toContain('Opportunity two');

    headings[0].click();
    fixture.detectChanges();
    expect(element.textContent).not.toContain('Opportunity one');
  });

  it('hides redundant Type and reduces mobile metadata in type groups', () => {
    element.querySelector<HTMLButtonElement>('.group-heading')?.click();
    fixture.detectChanges();
    expect(element.querySelector('thead')?.textContent).not.toContain('Opportunity Type');
    expect(element.querySelector('.mobile-opportunity-card')?.textContent).not.toContain('Declined Work');
    expect(element.querySelector('.mobile-opportunity-card')?.textContent).not.toContain('Strong evidence');
  });

  it('sorts from Potential and Detected headers with accurate aria-sort', () => {
    element.querySelectorAll<HTMLButtonElement>('.group-heading').forEach((heading) => heading.click());
    fixture.detectChanges();
    const emitted: string[] = [];
    fixture.componentInstance.sortChanged.subscribe((sort) => emitted.push(sort));
    const sortable = element.querySelectorAll<HTMLButtonElement>('.sort-heading');

    expect(sortable[1].parentElement?.getAttribute('aria-sort')).toBe('descending');
    sortable[0].click();
    sortable[1].click();

    expect(emitted).toEqual(['POTENTIAL_DESC', 'DETECTED_ASC']);
  });

  it('resets groups when the query context changes', () => {
    const heading = element.querySelector<HTMLButtonElement>('.group-heading')!;
    heading.click();
    fixture.detectChanges();
    expect(heading.getAttribute('aria-expanded')).toBe('true');

    fixture.componentRef.setInput('queryParams', { priority: 'HIGH' });
    fixture.detectChanges();

    expect(heading.getAttribute('aria-expanded')).toBe('false');
  });
});