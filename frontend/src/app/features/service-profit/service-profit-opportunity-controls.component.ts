import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import {
  ServiceProfitActionability,
  ServiceProfitOpportunityFilters,
  ServiceProfitOpportunitySort,
  ServiceProfitOpportunitySummary,
  ServiceProfitOpportunityType,
  ServiceProfitPriority,
} from './service-profit.models';

export interface ServiceProfitFilterChange {
  key: 'opportunityType' | 'priority' | 'actionability';
  value: string;
}

@Component({
  selector: 'app-service-profit-opportunity-controls',
  imports: [AvFeedbackComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="opportunity-type-navigation" role="group" [attr.aria-label]="localization.text('opportunityTypes')">
      <button type="button" (click)="changeFilter('opportunityType', '')" [attr.aria-pressed]="!filters().opportunityType">
        <span>{{ localization.text('all') }}</span>
        @if (!filters().opportunityType) { <span class="type-count">{{ summary().totalOpportunities }}</span> }
      </button>
      @for (type of opportunityTypes(); track type) {
        <button type="button" (click)="changeFilter('opportunityType', type)" [attr.aria-pressed]="filters().opportunityType === type">
          <span>{{ label(type) }}</span>
          @let typeCount = opportunityTypeCount(type);
          @if (typeCount !== null) { <span class="type-count">{{ typeCount }}</span> }
        </button>
      }
    </div>

    <div class="queue-toolbar">
      <fieldset class="filters">
        <legend>{{ localization.text('opportunityFilters') }}</legend>
        <label>{{ localization.text('priority') }}
          <select [value]="filters().priority ?? ''" (change)="changeFilter('priority', $any($event.target).value)">
            <option value="">{{ localization.text('all') }}</option>
            @for (priority of priorities(); track priority) { <option [value]="priority">{{ label(priority) }}</option> }
          </select>
        </label>
        <label>{{ localization.text('actionability') }}
          <select [value]="filters().actionability ?? ''" (change)="changeFilter('actionability', $any($event.target).value)">
            <option value="">{{ localization.text('all') }}</option>
            @for (actionability of actionabilities(); track actionability) { <option [value]="actionability">{{ label(actionability) }}</option> }
          </select>
        </label>
        <label>{{ localization.text('sortBy') }}
          <select [value]="sort()" (change)="sortChanged.emit($any($event.target).value)">
            @for (sortOption of sorts(); track sortOption.value) { <option [value]="sortOption.value">{{ localization.text(sortOption.labelKey) }}</option> }
          </select>
        </label>
      </fieldset>
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
    .opportunity-type-navigation { display: flex; flex-wrap: wrap; gap: .5rem; margin: 1rem 0; }
    .opportunity-type-navigation button, .refresh-control { display: inline-flex; align-items: center; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); cursor: pointer; font-weight: 700; white-space: nowrap; }
    .opportunity-type-navigation button { gap: .45rem; padding: .45rem .75rem; color: var(--av-color-ink); }
    .opportunity-type-navigation button[aria-pressed="true"] { border: 2px solid var(--av-color-brand); background: #f0f7f6; color: var(--av-color-brand-strong); }
    .type-count { display: grid; place-items: center; min-inline-size: 1.5rem; min-block-size: 1.5rem; padding-inline: .3rem; border-radius: 50%; background: var(--av-color-canvas); color: var(--av-color-muted); font-size: .75rem; }
    .queue-toolbar { display: flex; align-items: flex-end; justify-content: space-between; gap: 1rem; margin-bottom: .75rem; }
    .filters { display: flex; flex: 1 1 auto; flex-wrap: wrap; gap: .75rem; min-inline-size: 0; padding: 0; border: 0; margin: 0; }
    .filters legend { position: absolute; inline-size: 1px; block-size: 1px; padding: 0; border: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; }
    .filters label { display: grid; gap: .3rem; color: var(--av-color-muted); font-size: .75rem; font-weight: 800; }
    select { min-block-size: 2.5rem; max-width: 13rem; padding: .45rem 2rem .45rem .65rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); font: inherit; }
    .refresh-control { flex: 0 0 2.25rem; justify-content: center; inline-size: 2.25rem; block-size: 2.25rem; padding: 0; color: var(--av-color-brand-strong); }
    .refresh-control:disabled { cursor: wait; opacity: .65; }
    .refresh-control[aria-busy="true"] svg { animation: refresh-turn .9s linear infinite; }
    .refresh-feedback { display: block; margin-bottom: .75rem; }
    @keyframes refresh-turn { to { transform: rotate(360deg); } }
    @media (prefers-reduced-motion: reduce) { .refresh-control[aria-busy="true"] svg { animation: none; opacity: .55; } }
    @media (max-width: 720px) { .opportunity-type-navigation { flex-wrap: nowrap; overflow-x: auto; padding-block-end: .25rem; } .queue-toolbar { display: grid; grid-template-columns: minmax(0, 1fr) 2.75rem; align-items: end; gap: .75rem; } .filters { width: 100%; } .filters label { flex: 1 1 9rem; } select { width: 100%; max-width: none; } .refresh-control { inline-size: 2.75rem; block-size: 2.75rem; } }
  `],
})
export class ServiceProfitOpportunityControlsComponent {
  protected readonly localization = inject(LocalizationService);

  readonly summary = input.required<ServiceProfitOpportunitySummary>();
  readonly filters = input.required<ServiceProfitOpportunityFilters>();
  readonly sort = input.required<ServiceProfitOpportunitySort>();
  readonly loading = input(false);
  readonly refreshing = input(false);
  readonly refreshError = input<ApiError | null>(null);
  readonly priorities = input.required<readonly ServiceProfitPriority[]>();
  readonly opportunityTypes = input.required<readonly ServiceProfitOpportunityType[]>();
  readonly actionabilities = input.required<readonly ServiceProfitActionability[]>();
  readonly sorts = input.required<ReadonlyArray<{ value: ServiceProfitOpportunitySort; labelKey: 'newest' | 'oldest' | 'highestPotential' | 'lowestPotential' }>>();

  readonly filterChanged = output<ServiceProfitFilterChange>();
  readonly sortChanged = output<string>();
  readonly refreshRequested = output<void>();

  protected changeFilter(key: ServiceProfitFilterChange['key'], value: string): void {
    this.filterChanged.emit({ key, value });
  }

  protected opportunityTypeCount(type: ServiceProfitOpportunityType): number | null {
    return this.summary().byOpportunityType.find((count) => count.key === type)?.count ?? null;
  }

  protected label(value: string): string {
    return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
  }
}
