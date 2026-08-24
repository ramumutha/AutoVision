import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { Params, RouterLink } from '@angular/router';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { AvMoneyPipe } from '../../shared/formatting/av-money.pipe';
import { ServiceProfitActionability, ServiceProfitOpportunityQueueItem, ServiceProfitPriority } from './service-profit.models';

@Component({
  selector: 'app-service-profit-mobile-list',
  imports: [AvMoneyPipe, RouterLink, AvStatusComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <ul class="mobile-opportunity-list" [attr.aria-label]="localization.text('opportunitySummaries')">
      @for (opportunity of opportunities(); track opportunity.id) {
        <li>
          <a class="mobile-opportunity-card"
            [routerLink]="['/service-profit/opportunities', opportunity.id]"
            [queryParams]="queryParams()"
            [attr.aria-label]="opportunity.title + ', ' + label(opportunity.priority) + ', ' + localization.text('openDetails')">
            <span class="card-heading">
              <strong>{{ opportunity.title }}</strong>
              <span class="opportunity-type">{{ label(opportunity.opportunityType) }}</span>
            </span>
            <span class="decision-summary">
              <span class="potential">{{ opportunity.potentialAmount | avMoney:opportunity.currencyCode:localization.locale() }}</span>
              <av-status [label]="label(opportunity.priority)" [tone]="priorityTone(opportunity.priority)" />
              <av-status [label]="label(opportunity.actionability)" [tone]="actionabilityTone(opportunity.actionability)" />
              <span class="evidence">{{ label(opportunity.evidenceStrength) }} {{ localization.text('evidence') }}</span>
            </span>
            <span class="open-details">{{ localization.text('openDetails') }} <span aria-hidden="true">→</span></span>
          </a>
        </li>
      }
    </ul>
  `,
  styles: [`
    :host { display: none; }
    .mobile-opportunity-list { display: grid; gap: .75rem; padding: 0; margin: 0; list-style: none; }
    .mobile-opportunity-card { display: grid; gap: .8rem; min-block-size: 8rem; padding: 1rem; border: 1px solid var(--av-color-border); border-left: 4px solid var(--av-color-brand); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); text-decoration: none; }
    .card-heading { display: grid; gap: .25rem; }
    .card-heading strong { color: var(--av-color-brand-strong); font-size: 1rem; }
    .opportunity-type, .evidence { color: var(--av-color-muted); font-size: .8rem; font-weight: 700; }
    .decision-summary { display: flex; align-items: center; flex-wrap: wrap; gap: .5rem; }
    .potential { margin-inline-end: auto; font-size: 1.25rem; font-weight: 800; }
    .open-details { color: var(--av-color-brand-strong); font-size: .875rem; font-weight: 800; }
    @media (max-width: 720px) { :host { display: block; } }
  `],
})
export class ServiceProfitMobileListComponent {
  protected readonly localization = inject(LocalizationService);
  readonly opportunities = input.required<readonly ServiceProfitOpportunityQueueItem[]>();
  readonly queryParams = input<Params>({});

  protected label(value: string): string {
    return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
  }

  protected priorityTone(priority: ServiceProfitPriority): AvStatusTone {
    return priority === 'HIGH' ? 'danger' : priority === 'MEDIUM' ? 'warning' : 'neutral';
  }

  protected actionabilityTone(actionability: ServiceProfitActionability): AvStatusTone {
    if (actionability === 'READY') return 'success';
    if (actionability === 'REVIEW_REQUIRED' || actionability === 'CONTACT_DATA_MISSING') return 'warning';
    return actionability === 'SUPPRESSED' ? 'neutral' : 'danger';
  }
}
