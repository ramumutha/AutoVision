import { ChangeDetectionStrategy, Component, inject, input, output } from '@angular/core';
import { LocalizationService } from '../../core/localization/localization.service';
import { ServiceProfitOpportunitySummary } from './service-profit.models';

export type ServiceProfitBusinessLens =
  | 'TOTAL'
  | 'HIGH_PRIORITY'
  | 'REVIEW_REQUIRED'
  | 'READY_TO_ACTION';

@Component({
  selector: 'app-service-profit-business-navigation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="business-navigation" role="group" [attr.aria-label]="localization.text('businessNavigation')">
      @for (lens of lenses; track lens) {
        <button
          type="button"
          [attr.aria-pressed]="selectedLens() === lens"
          (click)="lensSelected.emit(lens)">
          <span class="business-label">{{ label(lens) }}</span>
          <strong>{{ count(lens) }}</strong>
          @if (selectedLens() === lens) {
            <span class="selected-state">{{ localization.text('selected') }}</span>
          }
        </button>
      }
    </div>
  `,
  styles: [`
    :host { display: block; min-width: 0; }
    .business-navigation { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: .75rem; }
    button { display: grid; align-content: space-between; gap: .45rem; min-width: 0; min-block-size: 8rem; padding: 1rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); color: var(--av-color-ink); cursor: pointer; font: inherit; text-align: left; }
    button:hover { border-color: var(--av-color-brand); }
    button:focus-visible { outline: 3px solid var(--av-color-brand); outline-offset: 2px; }
    button[aria-pressed="true"] { border: 2px solid var(--av-color-brand); background: #f0f7f6; }
    .business-label { color: var(--av-color-muted); font-size: .8rem; font-weight: 800; text-transform: uppercase; }
    strong { font-size: 1.75rem; line-height: 1; }
    .selected-state { color: var(--av-color-brand-strong); font-size: .75rem; font-weight: 800; }
    @media (max-width: 1100px) { .business-navigation { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 720px) { button { min-block-size: 6.5rem; padding: .8rem; } }
  `],
})
export class ServiceProfitBusinessNavigationComponent {
  protected readonly localization = inject(LocalizationService);
  protected readonly lenses: readonly ServiceProfitBusinessLens[] = [
    'TOTAL',
    'HIGH_PRIORITY',
    'REVIEW_REQUIRED',
    'READY_TO_ACTION',
  ];

  readonly summary = input.required<ServiceProfitOpportunitySummary>();
  readonly selectedLens = input<ServiceProfitBusinessLens | null>(null);
  readonly lensSelected = output<ServiceProfitBusinessLens>();

  protected label(lens: ServiceProfitBusinessLens): string {
    if (lens === 'TOTAL') return this.localization.text('totalOpportunities');
    if (lens === 'HIGH_PRIORITY') return this.localization.text('highPriority');
    if (lens === 'REVIEW_REQUIRED') return this.localization.text('reviewRequired');
    return this.localization.text('readyToAction');
  }

  protected count(lens: ServiceProfitBusinessLens): number {
    const summary = this.summary();
    if (lens === 'TOTAL') return summary.totalOpportunities;
    if (lens === 'HIGH_PRIORITY') return summary.highPriorityCount;
    if (lens === 'REVIEW_REQUIRED') return summary.reviewRequiredCount;
    return summary.readyCount;
  }
}