import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, input, output, signal, viewChild } from '@angular/core';
import { LocalizationService } from '../../core/localization/localization.service';
import {
  ServiceProfitActionability,
  ServiceProfitOpportunityFilters,
  ServiceProfitOpportunitySort,
  ServiceProfitPriority,
} from './service-profit.models';

export interface ServiceProfitMobileFilterState {
  priority: ServiceProfitPriority | null;
  actionability: ServiceProfitActionability | null;
  sort: ServiceProfitOpportunitySort;
}

@Component({
  selector: 'app-service-profit-mobile-filters',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="mobile-filter-control">
      <button #disclosureTrigger class="disclosure-trigger" type="button" (click)="toggle()" [attr.aria-expanded]="open()" aria-controls="mobile-service-profit-filters">
        <span>{{ localization.text('sortAndFilter') }}</span>
        @if (activeCount()) {
          <span class="active-count" [attr.aria-label]="activeCount() + ' ' + localization.text('activeFilters')">{{ activeCount() }}</span>
        }
        <span aria-hidden="true">{{ open() ? '−' : '+' }}</span>
      </button>

      @if (open()) {
        <div id="mobile-service-profit-filters" class="filter-panel">
          <fieldset>
            <legend>{{ localization.text('opportunityFilters') }}</legend>
            <label>{{ localization.text('priority') }}
              <select [value]="pendingPriority() ?? ''" (change)="pendingPriority.set(toPriority($any($event.target).value))">
                <option value="">{{ localization.text('all') }}</option>
                @for (priority of priorities(); track priority) { <option [value]="priority">{{ label(priority) }}</option> }
              </select>
            </label>
            <label>{{ localization.text('actionability') }}
              <select [value]="pendingActionability() ?? ''" (change)="pendingActionability.set(toActionability($any($event.target).value))">
                <option value="">{{ localization.text('all') }}</option>
                @for (actionability of actionabilities(); track actionability) { <option [value]="actionability">{{ label(actionability) }}</option> }
              </select>
            </label>
            <label>{{ localization.text('sortBy') }}
              <select [value]="pendingSort()" (change)="pendingSort.set($any($event.target).value)">
                @for (sortOption of sorts(); track sortOption.value) { <option [value]="sortOption.value">{{ localization.text(sortOption.labelKey) }}</option> }
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
    :host { display: none; }
    .mobile-filter-control { position: relative; }
    .disclosure-trigger { display: flex; align-items: center; justify-content: space-between; gap: .6rem; inline-size: 100%; min-block-size: 2.75rem; padding: .55rem .75rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); font: inherit; font-weight: 800; }
    .active-count { display: grid; place-items: center; min-inline-size: 1.5rem; min-block-size: 1.5rem; margin-inline-start: auto; border-radius: 50%; background: var(--av-color-brand); color: white; font-size: .75rem; }
    .filter-panel { display: grid; gap: 1rem; padding: 1rem; border: 1px solid var(--av-color-border); border-top: 0; background: var(--av-color-surface); }
    fieldset { display: grid; gap: .75rem; padding: 0; border: 0; margin: 0; }
    legend { position: absolute; inline-size: 1px; block-size: 1px; padding: 0; border: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; }
    label { display: grid; gap: .3rem; color: var(--av-color-muted); font-size: .75rem; font-weight: 800; }
    select { inline-size: 100%; min-block-size: 2.75rem; padding: .45rem 2rem .45rem .65rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); font: inherit; }
    .panel-actions { display: grid; grid-template-columns: 1fr 1fr; gap: .75rem; }
    .panel-actions button { min-block-size: 2.75rem; border-radius: var(--av-radius-sm); font: inherit; font-weight: 800; }
    .clear-action { border: 1px solid var(--av-color-border); background: var(--av-color-surface); color: var(--av-color-ink); }
    .apply-action { border: 1px solid var(--av-color-brand); background: var(--av-color-brand); color: white; }
    @media (max-width: 720px) { :host { display: block; } }
  `],
})
export class ServiceProfitMobileFiltersComponent {
  protected readonly localization = inject(LocalizationService);
  readonly filters = input.required<ServiceProfitOpportunityFilters>();
  readonly sort = input.required<ServiceProfitOpportunitySort>();
  readonly priorities = input.required<readonly ServiceProfitPriority[]>();
  readonly actionabilities = input.required<readonly ServiceProfitActionability[]>();
  readonly sorts = input.required<ReadonlyArray<{ value: ServiceProfitOpportunitySort; labelKey: 'newest' | 'oldest' | 'highestPotential' | 'lowestPotential' }>>();
  readonly applied = output<ServiceProfitMobileFilterState>();
  private readonly disclosureTrigger = viewChild.required<ElementRef<HTMLButtonElement>>('disclosureTrigger');

  protected readonly open = signal(false);
  protected readonly pendingPriority = signal<ServiceProfitPriority | null>(null);
  protected readonly pendingActionability = signal<ServiceProfitActionability | null>(null);
  protected readonly pendingSort = signal<ServiceProfitOpportunitySort>('DETECTED_DESC');
  protected readonly activeCount = computed(() => Number(!!this.filters().priority) + Number(!!this.filters().actionability));

  protected toggle(): void {
    if (this.open()) {
      this.open.set(false);
      return;
    }
    this.resetPending();
    this.open.set(true);
  }

  protected clear(): void {
    this.pendingPriority.set(null);
    this.pendingActionability.set(null);
    this.pendingSort.set('DETECTED_DESC');
  }

  protected apply(): void {
    this.applied.emit({
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

  protected toActionability(value: string): ServiceProfitActionability | null {
    return this.actionabilities().includes(value as ServiceProfitActionability) ? value as ServiceProfitActionability : null;
  }

  protected label(value: string): string {
    return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
  }

  private resetPending(): void {
    this.pendingPriority.set(this.filters().priority ?? null);
    this.pendingActionability.set(this.filters().actionability ?? null);
    this.pendingSort.set(this.sort());
  }
}
