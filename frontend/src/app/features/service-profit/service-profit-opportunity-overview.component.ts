import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { LocalizationService } from '../../core/localization/localization.service';
import { ServiceProfitOpportunityResponse } from './service-profit.models';

@Component({
  selector: 'app-service-profit-opportunity-overview',
  imports: [AvStatusComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<section class="overview" aria-labelledby="overview-heading"><h3 id="overview-heading">{{ localization.text('overview') }}</h3><div class="reason"><p class="eyebrow">{{ localization.text('whyAutoVisionFoundThis') }}</p><h4>{{ opportunity().explanation.headline }}</h4><p>{{ opportunity().explanation.rationale }}</p><p class="summary">{{ opportunity().summary }}</p></div><dl class="facts"><div><dt>{{ localization.text('priority') }}</dt><dd><av-status [label]="label(opportunity().priority)" [tone]="priorityTone()" /></dd></div><div><dt>{{ localization.text('opportunityStatus') }}</dt><dd>{{ label(opportunity().status) }}</dd></div><div><dt>{{ localization.text('actionability') }}</dt><dd>{{ label(opportunity().actionability) }}</dd></div><div><dt>{{ localization.text('evidenceStrength') }}</dt><dd>{{ label(opportunity().evidenceStrength) }}</dd></div></dl></section>`,
  styles: [`.overview { display: grid; gap: 1rem; } h3, h4 { margin: 0; } .reason { padding: 1rem; border: 1px solid var(--av-color-border); background: var(--av-color-canvas); } .eyebrow { margin: 0 0 .35rem; color: var(--av-color-brand-strong); font-size: .75rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; } .reason p { color: var(--av-color-muted); line-height: 1.5; } .reason .summary { margin-block-end: 0; font-weight: 700; } .facts { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; margin: 0; } dt { color: var(--av-color-muted); font-size: .75rem; font-weight: 800; text-transform: uppercase; } dd { margin: .35rem 0 0; font-weight: 700; } @media (max-width: 900px) { .facts { grid-template-columns: repeat(2, minmax(0, 1fr)); } } @media (max-width: 720px) { .facts { grid-template-columns: 1fr; } }`],
})
export class ServiceProfitOpportunityOverviewComponent {
  protected readonly localization = inject(LocalizationService);
  readonly opportunity = input.required<ServiceProfitOpportunityResponse>();
  protected label(value: string): string { return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase()); }
  protected priorityTone(): AvStatusTone { return this.opportunity().priority === 'HIGH' ? 'danger' : this.opportunity().priority === 'MEDIUM' ? 'warning' : 'neutral'; }
}
