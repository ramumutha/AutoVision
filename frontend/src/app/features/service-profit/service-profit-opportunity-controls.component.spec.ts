import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServiceProfitMobileFilterState } from './service-profit-mobile-filters.component';
import { ServiceProfitOpportunityControlsComponent } from './service-profit-opportunity-controls.component';

describe('ServiceProfitOpportunityControlsComponent', () => {
  let fixture: ComponentFixture<ServiceProfitOpportunityControlsComponent>;
  let element: HTMLElement;

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ServiceProfitOpportunityControlsComponent] }).compileComponents();
    fixture = TestBed.createComponent(ServiceProfitOpportunityControlsComponent);
    fixture.componentRef.setInput('filters', {});
    fixture.componentRef.setInput('showAdvancedFilters', true);
    fixture.componentRef.setInput('sort', 'DETECTED_DESC');
    fixture.componentRef.setInput('opportunityTypes', ['DECLINED_WORK', 'DEFERRED_WORK']);
    fixture.componentRef.setInput('priorities', ['HIGH', 'MEDIUM', 'LOW']);
    fixture.componentRef.setInput('actionabilities', ['READY', 'REVIEW_REQUIRED', 'BLOCKED']);
    fixture.componentRef.setInput('sorts', [
      { value: 'DETECTED_DESC', labelKey: 'newest' },
      { value: 'POTENTIAL_DESC', labelKey: 'highestPotential' },
    ]);
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  });

  it('keeps Type, Priority, and Actionability behind the disclosure by default', () => {
    expect(element.querySelector('.opportunity-type-navigation')).toBeNull();
    expect(element.querySelector('fieldset')).toBeNull();

    const trigger = element.querySelector<HTMLButtonElement>('.disclosure-trigger')!;
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
    expect(trigger.getAttribute('aria-controls')).toBe('service-profit-secondary-filters');

    trigger.click();
    fixture.detectChanges();

    expect(Array.from(element.querySelectorAll('fieldset label')).map((label) => label.textContent?.trim())).toEqual([
      'Opportunity Type AllDeclined WorkDeferred Work',
      'Priority AllHighMediumLow',
      'Actionability AllReadyReview RequiredBlocked',
      'Sort by NewestHighest potential',
    ]);
  });

  it('keeps Group By visible and mobile Sort inside the combined disclosure', () => {
    const groupBy = element.querySelector<HTMLSelectElement>('.group-by select');
    expect(groupBy?.labels?.[0]?.textContent).toContain('Group by');
    expect(groupBy?.value).toBe('OPPORTUNITY_TYPE');
    expect(element.querySelector('.desktop-sort')).toBeNull();

    element.querySelector<HTMLButtonElement>('.disclosure-trigger')!.click();
    fixture.detectChanges();

    expect(element.querySelector('.mobile-sort select')).not.toBeNull();
  });

  it('emits a local grouping change', () => {
    const changed: string[] = [];
    fixture.componentInstance.groupByChanged.subscribe((groupBy) => changed.push(groupBy));
    const groupBy = element.querySelector<HTMLSelectElement>('.group-by select')!;
    groupBy.value = 'PRIORITY';
    groupBy.dispatchEvent(new Event('change'));

    expect(changed).toEqual(['PRIORITY']);
  });

  it('forwards one complete applied secondary-filter state', () => {
    const applied: ServiceProfitMobileFilterState[] = [];
    fixture.componentInstance.secondaryFiltersApplied.subscribe((state) => applied.push(state));
    element.querySelector<HTMLButtonElement>('.disclosure-trigger')!.click();
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('.apply-action')!.click();

    expect(applied).toEqual([{ opportunityType: null, priority: null, actionability: null, sort: 'DETECTED_DESC' }]);
  });

  it('preserves compact accessible Refresh behavior', () => {
    let refreshCount = 0;
    fixture.componentInstance.refreshRequested.subscribe(() => refreshCount++);
    const refresh = element.querySelector<HTMLButtonElement>('.refresh-control')!;

    expect(refresh.getAttribute('aria-label')).toBe('Refresh Service Profit data');
    expect(refresh.getAttribute('aria-busy')).toBe('false');
    expect(refresh.textContent?.trim()).toBe('');
    refresh.click();
    expect(refreshCount).toBe(1);

    fixture.componentRef.setInput('refreshing', true);
    fixture.detectChanges();
    expect(refresh.disabled).toBe(true);
    expect(refresh.getAttribute('aria-busy')).toBe('true');
  });
});