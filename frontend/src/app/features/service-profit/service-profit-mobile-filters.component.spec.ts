import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ServiceProfitMobileFilterState, ServiceProfitMobileFiltersComponent } from './service-profit-mobile-filters.component';

describe('ServiceProfitMobileFiltersComponent', () => {
  let fixture: ComponentFixture<ServiceProfitMobileFiltersComponent>;
  let element: HTMLElement;
  let applied: ServiceProfitMobileFilterState[];

  beforeEach(async () => {
    await TestBed.configureTestingModule({ imports: [ServiceProfitMobileFiltersComponent] }).compileComponents();
    fixture = TestBed.createComponent(ServiceProfitMobileFiltersComponent);
    fixture.componentRef.setInput('filters', { priority: 'HIGH', actionability: 'READY', opportunityType: 'DECLINED_WORK' });
    fixture.componentRef.setInput('sort', 'POTENTIAL_DESC');
    fixture.componentRef.setInput('opportunityTypes', ['DECLINED_WORK', 'DEFERRED_WORK']);
    fixture.componentRef.setInput('priorities', ['HIGH', 'MEDIUM', 'LOW']);
    fixture.componentRef.setInput('actionabilities', ['READY', 'REVIEW_REQUIRED', 'BLOCKED']);
    fixture.componentRef.setInput('sorts', [
      { value: 'DETECTED_DESC', labelKey: 'newest' },
      { value: 'POTENTIAL_DESC', labelKey: 'highestPotential' },
    ]);
    applied = [];
    fixture.componentInstance.applied.subscribe((state) => applied.push(state));
    fixture.detectChanges();
    element = fixture.nativeElement as HTMLElement;
  });

  it('exposes disclosure state and counts active secondary filters', () => {
    const trigger = element.querySelector<HTMLButtonElement>('.disclosure-trigger')!;
    expect(trigger.getAttribute('aria-expanded')).toBe('false');
    expect(trigger.getAttribute('aria-controls')).toBe('service-profit-secondary-filters');
    expect(trigger.textContent).toContain('3');
    trigger.click();
    fixture.detectChanges();
    expect(trigger.getAttribute('aria-expanded')).toBe('true');
    expect(element.querySelector('fieldset legend')?.textContent).toContain('Opportunity filters');
    expect(Array.from(element.querySelectorAll<HTMLSelectElement>('select')).map((select) => select.value)).toEqual([
      'DECLINED_WORK', 'HIGH', 'READY', 'POTENTIAL_DESC',
    ]);
  });

  it('keeps edits pending until Apply and emits the complete state once', () => {
    element.querySelector<HTMLButtonElement>('.disclosure-trigger')!.click();
    fixture.detectChanges();
    const selects = element.querySelectorAll<HTMLSelectElement>('select');
    selects[0].value = 'DEFERRED_WORK';
    selects[0].dispatchEvent(new Event('change'));
    selects[1].value = 'LOW';
    selects[1].dispatchEvent(new Event('change'));
    selects[2].value = 'BLOCKED';
    selects[2].dispatchEvent(new Event('change'));
    selects[3].value = 'DETECTED_DESC';
    selects[3].dispatchEvent(new Event('change'));
    expect(applied).toEqual([]);
    const apply = element.querySelector<HTMLButtonElement>('.apply-action')!;
    apply.focus();
    apply.click();
    expect(applied).toEqual([{ opportunityType: 'DEFERRED_WORK', priority: 'LOW', actionability: 'BLOCKED', sort: 'DETECTED_DESC' }]);
    expect(document.activeElement).toBe(element.querySelector('.disclosure-trigger'));
  });

  it('clears all secondary filters while preserving Sort', () => {
    element.querySelector<HTMLButtonElement>('.disclosure-trigger')!.click();
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('.clear-action')!.click();
    element.querySelector<HTMLButtonElement>('.apply-action')!.click();
    expect(applied).toEqual([{ opportunityType: null, priority: null, actionability: null, sort: 'POTENTIAL_DESC' }]);
  });

  it('discards pending edits when the disclosure closes without Apply', () => {
    const trigger = element.querySelector<HTMLButtonElement>('.disclosure-trigger')!;
    trigger.click();
    fixture.detectChanges();
    const priority = element.querySelector<HTMLSelectElement>('select')!;
    priority.value = 'LOW';
    priority.dispatchEvent(new Event('change'));
    trigger.click();
    fixture.detectChanges();
    trigger.click();
    fixture.detectChanges();
    element.querySelector<HTMLButtonElement>('.apply-action')!.click();
    expect(applied).toEqual([{ opportunityType: 'DECLINED_WORK', priority: 'HIGH', actionability: 'READY', sort: 'POTENTIAL_DESC' }]);
  });

  it('excludes canonical KPI dimensions from the secondary filter count', () => {
    fixture.componentRef.setInput('canonicalKpiSelected', true);
    fixture.detectChanges();

    expect(element.querySelector('.active-count')?.textContent).toContain('1');
  });
});
