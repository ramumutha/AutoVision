import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LocalizationService } from '../../core/localization/localization.service';
import { ServiceProfitOpportunityCardComponent } from './service-profit-opportunity-card.component';
import { ServiceProfitWorkQueueGroup, ServiceProfitWorkQueueOwnership } from './service-profit.models';

@Component({
  selector: 'app-service-profit-queue-group',
  imports: [ServiceProfitOpportunityCardComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<section class="group" [attr.aria-labelledby]="headingId()"><div class="group-title"><h3 [id]="headingId()">{{ groupLabel() }}</h3><span>{{ group().items.length }}</span></div><ul class="cards">@for (item of group().items; track item.followUpId) { <li><app-service-profit-opportunity-card [item]="item" [ownership]="ownership()" /></li> }</ul></section>`,
  styles: [`.group { display: grid; gap: .75rem; } .group-title { display: flex; justify-content: space-between; align-items: baseline; padding-block-end: .55rem; border-bottom: 2px solid var(--av-color-brand); } .group-title h3 { margin: 0; } .group-title span { color: var(--av-color-muted); font-weight: 800; } .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 19rem), 1fr)); gap: 1rem; padding: 0; margin: 0; list-style: none; }`],
})
export class ServiceProfitQueueGroupComponent {
  protected readonly localization = inject(LocalizationService);
  readonly group = input.required<ServiceProfitWorkQueueGroup>();
  readonly groupLabel = input.required<string>();
  readonly ownership = input<ServiceProfitWorkQueueOwnership>('ALL');
  protected headingId(): string { return `queue-group-${this.group().key}`; }
}
