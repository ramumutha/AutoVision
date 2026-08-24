import { ChangeDetectionStrategy, Component, inject, input, output, viewChild } from '@angular/core';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { ServiceProfitMobileFiltersComponent, ServiceProfitMobileFilterState } from './service-profit-mobile-filters.component';
import { ServiceProfitActionability, ServiceProfitGroupBy, ServiceProfitOpportunityFilters, ServiceProfitOpportunitySort, ServiceProfitOpportunityType, ServiceProfitPriority } from './service-profit.models';

@Component({
  selector: 'app-service-profit-opportunity-controls',
  imports: [AvFeedbackComponent, ServiceProfitMobileFiltersComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="queue-toolbar">
      @if (showAdvancedFilters()) {
        <app-service-profit-mobile-filters [filters]="filters()" [sort]="sort()" [opportunityTypes]="opportunityTypes()" [priorities]="priorities()"
          [actionabilities]="actionabilities()" [sorts]="sorts()" [canonicalKpiSelected]="canonicalKpiSelected()"
          (applied)="secondaryFiltersApplied.emit($event)" />
        @if (secondaryFilterCount()) {
          <button class="utility-control" type="button" (click)="clearSecondaryFilters()"
            [attr.aria-label]="localization.text('clearFilters')" [title]="localization.text('clearFilters')">
            <svg aria-hidden="true" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 6h16M7 12h10M10 18h4" /><path d="m18 15 4 4M22 15l-4 4" /></svg>
          </button>
        }
      }
      <label class="group-by">{{ localization.text('groupBy') }}
        <select [value]="groupBy()" (change)="groupByChanged.emit($any($event.target).value)">
          @for (option of groupByOptions; track option.value) { <option [value]="option.value">{{ localization.text(option.labelKey) }}</option> }
        </select>
      </label>
      <button class="refresh-control" type="button" (click)="refreshRequested.emit()" [disabled]="loading() || refreshing()"
        [attr.aria-label]="localization.text('refreshServiceProfit')" [attr.aria-busy]="refreshing()" [title]="localization.text('refresh')">
        <svg aria-hidden="true" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M20 11a8 8 0 1 0-2.34 5.66" />
          <path d="M20 4v7h-7" />
        </svg>
      </button>
    </div>

    @if (refreshError()) {
      <av-feedback class="refresh-feedback" kind="error" [title]="localization.text('unableToRefreshServiceProfit')" [message]="localization.text('existingServiceProfitDataRetained')" />
    }
  `,
  styles: [`
    :host { display: block; }
    .queue-toolbar { display: flex; align-items: flex-end; justify-content: flex-end; gap: .75rem; min-inline-size: 0; }
    app-service-profit-mobile-filters { flex: 0 1 38rem; }
    .group-by { display: grid; gap: .3rem; color: var(--av-color-muted); font-size: .75rem; font-weight: 800; white-space: nowrap; }
    select { min-block-size: 2.5rem; max-width: 13rem; padding: .45rem 2rem .45rem .65rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); font: inherit; }
    .refresh-control { display: inline-flex; align-items: center; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); cursor: pointer; font-weight: 700; white-space: nowrap; }
    .refresh-control { flex: 0 0 2.25rem; justify-content: center; inline-size: 2.25rem; block-size: 2.25rem; padding: 0; color: var(--av-color-brand-strong); }
    .utility-control { display: inline-flex; align-items: center; justify-content: center; flex: 0 0 2.25rem; inline-size: 2.25rem; block-size: 2.25rem; padding: 0; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-brand-strong); cursor: pointer; }
    .refresh-control:disabled { cursor: wait; opacity: .65; }
    .refresh-control[aria-busy="true"] svg { animation: refresh-turn .9s linear infinite; }
    .refresh-feedback { display: block; margin-bottom: .75rem; }
    @keyframes refresh-turn { to { transform: rotate(360deg); } }
    @media (prefers-reduced-motion: reduce) { .refresh-control[aria-busy="true"] svg { animation: none; opacity: .55; } }
    @media (max-width: 1100px) { .queue-toolbar { flex-wrap: wrap; justify-content: flex-start; } app-service-profit-mobile-filters { flex: 1 1 12rem; min-inline-size: 0; } .group-by { flex: 1 1 12rem; } }
    @media (max-width: 720px) { .queue-toolbar { display: grid; grid-template-columns: minmax(0, 1fr) minmax(7rem, auto) 2.75rem; align-items: start; gap: .75rem; } .refresh-control { inline-size: 2.75rem; block-size: 2.75rem; } }
    @media (max-width: 480px) { .queue-toolbar { grid-template-columns: minmax(0, 1fr) 2.75rem; } .group-by { grid-column: 1 / -1; grid-row: 2; } }
  `],
})
export class ServiceProfitOpportunityControlsComponent {
  protected readonly localization = inject(LocalizationService);
  private readonly secondaryFilters = viewChild(ServiceProfitMobileFiltersComponent);

  readonly filters = input.required<ServiceProfitOpportunityFilters>();
  readonly sort = input.required<ServiceProfitOpportunitySort>();
  readonly groupBy = input<ServiceProfitGroupBy>('OPPORTUNITY_TYPE');
  readonly loading = input(false);
  readonly refreshing = input(false);
  readonly refreshError = input<ApiError | null>(null);
  readonly priorities = input.required<readonly ServiceProfitPriority[]>();
  readonly opportunityTypes = input.required<readonly ServiceProfitOpportunityType[]>();
  readonly actionabilities = input.required<readonly ServiceProfitActionability[]>();
  readonly sorts = input.required<ReadonlyArray<{ value: ServiceProfitOpportunitySort; labelKey: 'newest' | 'oldest' | 'highestPotential' | 'lowestPotential' }>>();
  readonly canonicalKpiSelected = input(false);
  readonly showAdvancedFilters = input(false);

  readonly groupByChanged = output<ServiceProfitGroupBy>();
  readonly secondaryFiltersApplied = output<ServiceProfitMobileFilterState>();
  readonly refreshRequested = output<void>();
  protected readonly groupByOptions: ReadonlyArray<{ value: ServiceProfitGroupBy; labelKey: 'noGrouping' | 'opportunityType' | 'priority' | 'actionability' }> = [
    { value: 'OPPORTUNITY_TYPE', labelKey: 'opportunityType' },
    { value: 'PRIORITY', labelKey: 'priority' },
    { value: 'ACTIONABILITY', labelKey: 'actionability' },
    { value: 'NONE', labelKey: 'noGrouping' },
  ];

  protected readonly secondaryFilterCount = () => Number(!!this.filters().opportunityType)
    + (this.canonicalKpiSelected() ? 0 : Number(!!this.filters().priority) + Number(!!this.filters().actionability));

  protected clearSecondaryFilters(): void {
    this.secondaryFilters()?.clearApplied();
  }

  focusFiltersTrigger(): void {
    this.secondaryFilters()?.focusTrigger();
  }
}
