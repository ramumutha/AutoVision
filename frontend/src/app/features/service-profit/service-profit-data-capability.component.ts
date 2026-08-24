import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, ElementRef, computed, inject, input, signal, viewChild } from '@angular/core';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import {
  ServiceProfitDataCapability,
  ServiceProfitDataCapabilityItem,
  ServiceProfitDataCapabilityResponse,
  ServiceProfitDataCapabilityStatus,
} from './service-profit.models';

export type ServiceProfitDataCapabilityView =
  | { state: 'loading' }
  | { state: 'failed' }
  | { state: 'available'; data: ServiceProfitDataCapabilityResponse };

@Component({
  selector: 'app-service-profit-data-capability',
  imports: [DatePipe, AvStatusComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let capabilityView = view();
    <div class="capability" role="group" [attr.aria-label]="localization.text('dataCapability')">
      <div class="capability-summary">
        <button #detailsButton
            class="details-toggle"
            type="button"
            (click)="expanded.set(true)"
            [attr.aria-expanded]="expanded()"
            [attr.aria-controls]="detailsId"
            [attr.aria-label]="localization.text('viewDataDetails')"
            [title]="localization.text('viewDataDetails')">
            <svg aria-hidden="true" viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 11v5" />
              <path d="M12 8h.01" />
            </svg>
        </button>
      </div>

      @if (expanded()) {
        <div class="capability-backdrop" (click)="closeDetails()">
          <section class="capability-details" [id]="detailsId" role="dialog" aria-modal="true" [attr.aria-label]="localization.text('dataCapability')" (click)="$event.stopPropagation()" (keydown.escape)="closeDetails()">
            <button #closeButton class="close-details" type="button" (click)="closeDetails()" [attr.aria-label]="localization.text('hideDataDetails')" [title]="localization.text('hideDataDetails')">
              <span aria-hidden="true">×</span>
            </button>
          @if (capabilityView.state === 'loading') {
            <p class="capability-state" role="status">{{ localization.text('dataCapabilityLoading') }}</p>
          } @else if (capabilityView.state === 'failed') {
            <p class="capability-state capability-failed">{{ localization.text('dataCapabilityUnavailable') }}</p>
          } @else if (capabilityView.data.assessmentState === 'ASSESSED') {
            <dl class="assessment-metadata">
              <div>
                <dt>{{ localization.text('dataSource') }}</dt>
                <dd>
                  {{ capabilityView.data.sourceDatasetId || localization.text('notAvailable') }}
                  @if (capabilityView.data.sourceDatasetVersion) {
                    <span> / {{ capabilityView.data.sourceDatasetVersion }}</span>
                  }
                </dd>
              </div>
              <div>
                <dt>{{ localization.text('assessed') }}</dt>
                <dd>
                  @if (capabilityView.data.assessedAt) {
                    {{ capabilityView.data.assessedAt | date:'medium' }}
                  } @else {
                    {{ localization.text('notAvailable') }}
                  }
                </dd>
              </div>
              <div>
                <dt>{{ localization.text('assessmentPolicy') }}</dt>
                <dd>{{ capabilityView.data.assessmentPolicyVersion || localization.text('notAvailable') }}</dd>
              </div>
            </dl>

            <div class="capability-reasons">
              @for (item of displayCapabilities(); track item.capability) {
                <article>
                  <div class="reason-heading">
                    <h3>{{ capabilityLabel(item.capability) }}</h3>
                    <av-status [label]="statusLabel(item.status)" [tone]="statusTone(item.status)" />
                  </div>
                  <p>{{ item.reason || localization.text('dataCapabilityReasonUnavailable') }}</p>
                </article>
              }
            </div>
          } @else {
            <p class="not-assessed-detail">
              {{ localization.text('dataCapabilityNotAssessedContext') }}
            </p>
          }
          </section>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .capability-summary { display: flex; align-items: center; justify-content: space-between; gap: .75rem; }
    .capability-state { margin: 0; color: var(--av-color-muted); font-size: .8rem; font-weight: 700; }
    .capability-failed { color: var(--av-color-danger); }
    .details-toggle, .close-details { display: grid; place-items: center; inline-size: 2.5rem; block-size: 2.5rem; flex: 0 0 auto; padding: 0; border: 1px solid var(--av-color-border); border-radius: 50%; background: var(--av-color-surface); color: var(--av-color-brand-strong); cursor: pointer; }
    .capability-backdrop { position: fixed; inset: 0; z-index: 10; display: grid; place-items: center; padding: 1rem; background: rgb(16 31 29 / 32%); }
    .capability-details { position: relative; max-inline-size: 42rem; max-block-size: min(85vh, 42rem); overflow: auto; padding: 1.25rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); box-shadow: 0 1rem 3rem rgb(16 31 29 / 22%); }
    .close-details { position: absolute; inset-block-start: .75rem; inset-inline-end: .75rem; }
    .assessment-metadata { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; margin: 0 0 1rem; }
    .assessment-metadata div { min-width: 0; }
    dt { color: var(--av-color-muted); font-size: .72rem; font-weight: 800; text-transform: uppercase; }
    dd { margin: .25rem 0 0; color: var(--av-color-ink); overflow-wrap: anywhere; }
    .capability-reasons { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .75rem; }
    .capability-reasons article { padding: .8rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); }
    .reason-heading { display: flex; align-items: center; justify-content: space-between; gap: .75rem; }
    .reason-heading h3 { margin: 0; color: var(--av-color-ink); font-size: .875rem; }
    .capability-reasons p, .not-assessed, .not-assessed-detail { margin: .5rem 0 0; color: var(--av-color-muted); font-size: .8rem; line-height: 1.5; }
    .not-assessed { margin: 0; }
    @media (max-width: 900px) {
      .capability-summary { align-items: flex-start; }
    }
    @media (max-width: 600px) {
      .capability-summary { align-items: center; gap: .5rem; }
      .details-toggle { min-block-size: 2.75rem; }
      .capability-details { padding: .75rem; }
      .assessment-metadata, .capability-reasons { grid-template-columns: 1fr; }
    }
  `],
})
export class ServiceProfitDataCapabilityComponent {
  protected readonly localization = inject(LocalizationService);
  protected readonly expanded = signal(false);
  protected readonly detailsId = 'service-profit-data-capability-details';
  private readonly detailsButton = viewChild<ElementRef<HTMLButtonElement>>('detailsButton');

  readonly view = input.required<ServiceProfitDataCapabilityView>();

  protected closeDetails(): void {
    this.expanded.set(false);
    this.detailsButton()?.nativeElement.focus();
  }

  protected readonly displayCapabilities = computed(() => {
    const order: ServiceProfitDataCapability[] = [
      'REVENUE_ATTRIBUTION',
      'GROSS_PROFIT_ATTRIBUTION',
    ];

    const view = this.view();
    if (view.state !== 'available') return [];

    return order
      .map((capability) => view.data.capabilities.find((item) => item.capability === capability))
      .filter((item): item is ServiceProfitDataCapabilityItem => item !== undefined);
  });

  protected capabilityLabel(capability: ServiceProfitDataCapability): string {
    return capability === 'REVENUE_ATTRIBUTION'
      ? this.localization.text('revenueData')
      : this.localization.text('grossProfitData');
  }

  protected statusLabel(status: ServiceProfitDataCapabilityStatus): string {
    if (status === 'AVAILABLE') return this.localization.text('available');
    if (status === 'PARTIAL') return this.localization.text('partial');
    return this.localization.text('unavailable');
  }

  protected statusTone(status: ServiceProfitDataCapabilityStatus): AvStatusTone {
    if (status === 'AVAILABLE') return 'success';
    if (status === 'PARTIAL') return 'warning';
    return 'danger';
  }
}