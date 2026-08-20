import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { AvStatusComponent } from '../../shared/design-system/av-status.component';
import { ServiceOrderResponse } from './service-order.models';

@Component({
  selector: 'app-service-order-overview',
  imports: [AvStatusComponent, DatePipe],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="overview" aria-labelledby="overview-title">
      <div class="section-heading">
        <div>
          <p class="eyebrow">Service Order</p>
          <h2 id="overview-title">Overview</h2>
        </div>
        <av-status [label]="statusLabel()" [tone]="statusTone()" />
      </div>
      <dl class="facts">
        <div><dt>Order Number</dt><dd>{{ order().orderNumber }}</dd></div>
        <div><dt>Vehicle ID</dt><dd class="value-mono">{{ order().vehicleId }}</dd></div>
        <div><dt>Opened</dt><dd>{{ order().openedAt | date:'medium' }}</dd></div>
        <div><dt>Last Updated</dt><dd>{{ order().updatedAt | date:'medium' }}</dd></div>
      </dl>
    </section>
  `,
  styles: [`
    .overview { padding: 1.25rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); box-shadow: var(--av-shadow-sm); }
    .section-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 1.5rem; }
    .eyebrow { margin: 0 0 .35rem; color: var(--av-color-brand); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }
    h2 { margin: 0; color: var(--av-color-ink); font-size: 1.35rem; }
    .facts { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 1rem; margin: 0; }
    .facts div { min-width: 0; padding: .85rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-canvas); }
    dt { color: var(--av-color-muted); font-size: .78rem; font-weight: 700; }
    dd { margin: .35rem 0 0; overflow-wrap: anywhere; color: var(--av-color-ink); font-weight: 700; }
    .value-mono { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; font-size: .82rem; }
    @media (max-width: 760px) { .facts { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 440px) { .section-heading { flex-direction: column; } .facts { grid-template-columns: 1fr; } }
  `],
})
export class ServiceOrderOverviewComponent {
  readonly order = input.required<ServiceOrderResponse>();

  protected statusLabel(): string { return this.order().status.replaceAll('_', ' '); }
  protected statusTone(): 'neutral' | 'success' | 'warning' | 'danger' | 'info' {
    const tones: Record<ServiceOrderResponse['status'], 'neutral' | 'success' | 'warning' | 'danger' | 'info'> = {
      OPEN: 'info', IN_PROGRESS: 'warning', WORK_COMPLETED: 'success', CLOSED: 'neutral', CANCELLED: 'danger',
    };
    return tones[this.order().status];
  }
}