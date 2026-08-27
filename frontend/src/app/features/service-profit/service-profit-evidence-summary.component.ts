import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LocalizationService } from '../../core/localization/localization.service';
import { ServiceProfitOpportunityResponse } from './service-profit.models';

@Component({
  selector: 'app-service-profit-evidence-summary',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<section class="evidence" aria-labelledby="evidence-heading"><h3 id="evidence-heading">{{ localization.text('evidence') }}</h3><article class="evidence-card"><h4>{{ opportunity().explanation.headline }}</h4><dl><div><dt>{{ localization.text('evidenceBasis') }}</dt><dd>{{ opportunity().explanation.evidenceBasis }}</dd></div><div><dt>{{ localization.text('evidenceStrength') }}</dt><dd>{{ label(opportunity().evidenceStrength) }}</dd></div><div><dt>{{ localization.text('evidenceClass') }}</dt><dd>{{ label(opportunity().evidenceClass) }}</dd></div></dl></article><p class="contract-note">{{ localization.text('evidenceEventsUnavailable') }}</p></section>`,
  styles: [`.evidence { display: grid; gap: 1rem; } h3, h4 { margin: 0; } .evidence-card { padding: 1rem; border: 1px solid var(--av-color-border); border-left: 4px solid var(--av-color-brand); background: var(--av-color-surface); } dl { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; margin: 1rem 0 0; } dt { color: var(--av-color-muted); font-size: .75rem; font-weight: 800; text-transform: uppercase; } dd { margin: .35rem 0 0; overflow-wrap: anywhere; } .contract-note { margin: 0; color: var(--av-color-muted); } @media (max-width: 720px) { dl { grid-template-columns: 1fr; } }`],
})
export class ServiceProfitEvidenceSummaryComponent {
  protected readonly localization = inject(LocalizationService);
  readonly opportunity = input.required<ServiceProfitOpportunityResponse>();
  protected label(value: string): string { return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase()); }
}
