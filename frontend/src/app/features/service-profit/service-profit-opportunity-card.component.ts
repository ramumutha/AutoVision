import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { RouterLink } from '@angular/router';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { ServiceProfitWorkQueueDueState, ServiceProfitWorkQueueItem, ServiceProfitWorkQueueOwnership } from './service-profit.models';

@Component({
  selector: 'app-service-profit-opportunity-card',
  imports: [DatePipe, RouterLink, AvStatusComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<article class="card" [class.overdue]="dueState() === 'OVERDUE'"><div class="card-top"><av-status [label]="label(item().priority)" [tone]="priorityTone(item().priority)" /><av-status [label]="label(dueState())" [tone]="dueTone()" /></div><h4>{{ item().title }}</h4><p class="summary">{{ item().summary }}</p><dl><div><dt>{{ localization.text('handlingStatus') }}</dt><dd>{{ label(item().handlingStatus) }}</dd></div><div><dt>{{ localization.text('owner') }}</dt><dd>{{ ownerLabel() }}</dd></div><div><dt>{{ localization.text('disposition') }}</dt><dd>{{ label(item().disposition) }}</dd></div><div><dt>{{ localization.text('dueDate') }}</dt><dd>{{ item().dueAt ? (item().dueAt | date:'mediumDate') : localization.text('noDueDate') }}</dd></div></dl><a [routerLink]="['/service-profit/opportunities', item().opportunityId]">{{ localization.text('viewDetails') }}</a></article>`,
  styles: [`:host { display: block; } .card { display: grid; gap: .8rem; min-block-size: 16rem; padding: 1rem; border: 1px solid var(--av-color-border); border-left: 4px solid var(--av-color-brand); border-radius: var(--av-radius-sm); background: var(--av-color-surface); } .card.overdue { border-left-color: var(--av-color-danger); } .card-top { display: flex; justify-content: space-between; gap: .5rem; } h4 { margin: 0; color: var(--av-color-brand-strong); font-size: 1.05rem; } .summary { margin: 0; color: var(--av-color-muted); line-height: 1.45; } dl { display: grid; grid-template-columns: repeat(2, 1fr); gap: .55rem; margin: 0; } dt { color: var(--av-color-muted); font-size: .75rem; font-weight: 700; } dd { margin: .1rem 0 0; font-weight: 700; } a { align-self: end; color: var(--av-color-brand-strong); font-weight: 800; } a:focus-visible { outline: 3px solid var(--av-color-focus); outline-offset: 2px; } @media (max-width: 720px) { dl { grid-template-columns: 1fr; } }`],
})
export class ServiceProfitOpportunityCardComponent {
  protected readonly localization = inject(LocalizationService);
  readonly item = input.required<ServiceProfitWorkQueueItem>();
  readonly ownership = input<ServiceProfitWorkQueueOwnership>('ALL');

  protected dueState(): ServiceProfitWorkQueueDueState { const dueAt = this.item().dueAt; return !dueAt ? 'NO_DUE_DATE' : new Date(dueAt).getTime() < Date.now() ? 'OVERDUE' : 'UPCOMING'; }
  protected ownerLabel(): string { return this.item().ownerPrincipalId ? (this.ownership() === 'MINE' ? this.localization.text('mine') : this.localization.text('assigned')) : this.localization.text('unassigned'); }
  protected label(value: string): string { return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase()); }
  protected priorityTone(priority: string): AvStatusTone { return priority === 'HIGH' ? 'danger' : priority === 'MEDIUM' ? 'warning' : 'neutral'; }
  protected dueTone(): AvStatusTone { return this.dueState() === 'OVERDUE' ? 'danger' : this.dueState() === 'UPCOMING' ? 'warning' : 'neutral'; }
}
