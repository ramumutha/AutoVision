import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, input, output, signal, viewChild } from '@angular/core';
import { LocalizationService } from '../../core/localization/localization.service';
import {
  ServiceProfitActionability,
  ServiceProfitOpportunityFilters,
  ServiceProfitOpportunitySort,
  ServiceProfitOpportunityType,
  ServiceProfitPriority,
} from './service-profit.models';

export interface ServiceProfitMobileFilterState {
  opportunityType: ServiceProfitOpportunityType | null;
  priority: ServiceProfitPriority | null;
  actionability: ServiceProfitActionability | null;
  sort: ServiceProfitOpportunitySort;
}

@Component({
  selector: 'app-service-profit-mobile-filters',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="filter-control">
      <button #disclosureTrigger class="disclosure-trigger" type="button" (click)="toggle()" [attr.aria-expanded]="open()" aria-controls="service-profit-secondary-filters">
        <span class="desktop-label">{{ localization.text('filters') }}</span>
        <span class="mobile-label">{{ localization.text('sortAndFilter') }}</span>
        @if (activeCount()) {
          <span class="active-count" [attr.aria-label]="activeCount() + ' ' + localization.text('activeFilters')">{{ activeCount() }}</span>
        }
        <span aria-hidden="true">{{ open() ? '−' : '+' }}</span>
      </button>

      @if (open()) {
        <div id="service-profit-secondary-filters" class="filter-panel">
          <fieldset>
            <legend>{{ localization.text('opportunityFilters') }}</legend>
            <label>{{ localization.text('opportunityType') }}
              <select [value]="pendingOpportunityType() ?? ''" (change)="pendingOpportunityType.set(toOpportunityType($any($event.target).value))">
                <option value="" [selected]="pendingOpportunityType() === null">{{ localization.text('all') }}</option>
                @for (type of opportunityTypes(); track type) { <option [value]="type" [selected]="pendingOpportunityType() === type">{{ label(type) }}</option> }
              </select>
            </label>
            <label>{{ localization.text('priority') }}
              <select [value]="pendingPriority() ?? ''" (change)="pendingPriority.set(toPriority($any($event.target).value))">
                <option value="" [selected]="pendingPriority() === null">{{ localization.text('all') }}</option>
                @for (priority of priorities(); track priority) { <option [value]="priority" [selected]="pendingPriority() === priority">{{ label(priority) }}</option> }
              </select>
            </label>
            <label>{{ localization.text('actionability') }}
              <select [value]="pendingActionability() ?? ''" (change)="pendingActionability.set(toActionability($any($event.target).value))">
                <option value="" [selected]="pendingActionability() === null">{{ localization.text('all') }}</option>
                @for (actionability of actionabilities(); track actionability) { <option [value]="actionability" [selected]="pendingActionability() === actionability">{{ label(actionability) }}</option> }
              </select>
            </label>
            <label class="mobile-sort">{{ localization.text('sortBy') }}
              <select [value]="pendingSort()" (change)="pendingSort.set($any($event.target).value)">
                @for (sortOption of sorts(); track sortOption.value) { <option [value]="sortOption.value" [selected]="pendingSort() === sortOption.value">{{ localization.text(sortOption.labelKey) }}</option> }
              </select>
            </label>
          </fieldset>
          <div class="panel-actions">
            <button class="clear-action" type="button" (click)="clear()">{{ localization.text('clearFilters') }}</button>
            <button class="apply-action" type="button" (click)="apply()">{{ localization.text('applyFilters') }}</button>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; min-inline-size: 0; }
    .filter-control { position: relative; }
    .disclosure-trigger { display: flex; align-items: center; justify-content: space-between; gap: .6rem; min-block-size: 2.5rem; padding: .45rem .7rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); font: inherit; font-weight: 800; }
    .mobile-label { display: none; }
    .active-count { display: grid; place-items: center; min-inline-size: 1.5rem; min-block-size: 1.5rem; margin-inline-start: auto; border-radius: 50%; background: var(--av-color-brand); color: white; font-size: .75rem; }
    .filter-panel { display: grid; gap: 1rem; padding: .85rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); margin-top: .4rem; background: var(--av-color-surface); }
    fieldset { display: grid; grid-template-columns: repeat(3, minmax(9rem, 1fr)); gap: .75rem; padding: 0; border: 0; margin: 0; }
    legend { position: absolute; inline-size: 1px; block-size: 1px; padding: 0; border: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; }
    label { display: grid; gap: .3rem; color: var(--av-color-muted); font-size: .75rem; font-weight: 800; }
    select { inline-size: 100%; min-block-size: 2.75rem; padding: .45rem 2rem .45rem .65rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); font: inherit; }
    .mobile-sort { display: none; }
    .panel-actions { display: flex; justify-content: flex-end; gap: .75rem; }
    .panel-actions button { min-block-size: 2.75rem; border-radius: var(--av-radius-sm); font: inherit; font-weight: 800; }
    .clear-action { border: 1px solid var(--av-color-border); background: var(--av-color-surface); color: var(--av-color-ink); }
    .apply-action { border: 1px solid var(--av-color-brand); background: var(--av-color-brand); color: white; }
    @media (max-width: 720px) {
      .disclosure-trigger { inline-size: 100%; min-block-size: 2.75rem; padding: .55rem .75rem; }
      .desktop-label { display: none; }
      .mobile-label { display: inline; }
      fieldset { grid-template-columns: 1fr; }
      .mobile-sort { display: grid; }
      .panel-actions { display: grid; grid-template-columns: 1fr 1fr; }
    }
  `],
})
export class ServiceProfitMobileFiltersComponent {
  protected readonly localization = inject(LocalizationService);
  readonly filters = input.required<ServiceProfitOpportunityFilters>();
  readonly sort = input.required<ServiceProfitOpportunitySort>();
  readonly opportunityTypes = input.required<readonly ServiceProfitOpportunityType[]>();
  readonly priorities = input.required<readonly ServiceProfitPriority[]>();
  readonly actionabilities = input.required<readonly ServiceProfitActionability[]>();
  readonly sorts = input.required<ReadonlyArray<{ value: ServiceProfitOpportunitySort; labelKey: 'newest' | 'oldest' | 'highestPotential' | 'lowestPotential' }>>();
  readonly canonicalKpiSelected = input(false);
  readonly applied = output<ServiceProfitMobileFilterState>();
  private readonly disclosureTrigger = viewChild.required<ElementRef<HTMLButtonElement>>('disclosureTrigger');

  protected readonly open = signal(false);
  protected readonly pendingOpportunityType = signal<ServiceProfitOpportunityType | null>(null);
  protected readonly pendingPriority = signal<ServiceProfitPriority | null>(null);
  protected readonly pendingActionability = signal<ServiceProfitActionability | null>(null);
  protected readonly pendingSort = signal<ServiceProfitOpportunitySort>('DETECTED_DESC');
  protected readonly activeCount = computed(() => Number(!!this.filters().opportunityType)
    + (this.canonicalKpiSelected() ? 0 : Number(!!this.filters().priority) + Number(!!this.filters().actionability)));

  protected toggle(): void {
    if (this.open()) {
      this.open.set(false);
      return;
    }
    this.resetPending();
    this.open.set(true);
  }

  protected clear(): void {
    this.pendingOpportunityType.set(null);
    this.pendingPriority.set(null);
    this.pendingActionability.set(null);
  }

  protected apply(): void {
    this.applied.emit({
      opportunityType: this.pendingOpportunityType(),
      priority: this.pendingPriority(),
      actionability: this.pendingActionability(),
      sort: this.pendingSort(),
    });
    this.open.set(false);
    this.disclosureTrigger().nativeElement.focus();
  }

  protected toPriority(value: string): ServiceProfitPriority | null {
    return this.priorities().includes(value as ServiceProfitPriority) ? value as ServiceProfitPriority : null;
  }

  protected toOpportunityType(value: string): ServiceProfitOpportunityType | null {
    return this.opportunityTypes().includes(value as ServiceProfitOpportunityType) ? value as ServiceProfitOpportunityType : null;
  }

  protected toActionability(value: string): ServiceProfitActionability | null {
    return this.actionabilities().includes(value as ServiceProfitActionability) ? value as ServiceProfitActionability : null;
  }

  protected label(value: string): string {
    return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
  }

  focusTrigger(): void {
    this.disclosureTrigger().nativeElement.focus();
  }

  private resetPending(): void {
    this.pendingOpportunityType.set(this.filters().opportunityType ?? null);
    this.pendingPriority.set(this.filters().priority ?? null);
    this.pendingActionability.set(this.filters().actionability ?? null);
    this.pendingSort.set(this.sort());
  }
}
