import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, inject, input, signal } from '@angular/core';
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
        @if (capabilityView.state === 'loading') {
          <p class="capability-state" role="status">{{ localization.text('dataCapabilityLoading') }}</p>
        } @else if (capabilityView.state === 'failed') {
          <p class="capability-state capability-failed">{{ localization.text('dataCapabilityUnavailable') }}</p>
        } @else if (capabilityView.data.assessmentState === 'ASSESSED') {
          <div class="capability-statuses">
            @for (item of displayCapabilities(); track item.capability) {
              <div class="capability-status">
                <span>{{ capabilityLabel(item.capability) }}</span>
                <av-status [label]="statusLabel(item.status)" [tone]="statusTone(item.status)" />
              </div>
            }
          </div>
        } @else {
          <p class="not-assessed">{{ localization.text('dataCapabilityNotAssessed') }}</p>
        }

        @if (capabilityView.state === 'available') {
          <button
            class="details-toggle"
            type="button"
            (click)="expanded.set(!expanded())"
            [attr.aria-expanded]="expanded()"
            [attr.aria-controls]="detailsId">
            {{ localization.text(expanded() ? 'hideDataDetails' : 'viewDataDetails') }}
          </button>
        }
      </div>

      @if (capabilityView.state === 'available' && expanded()) {
        <div class="capability-details" [id]="detailsId">
          @if (capabilityView.data.assessmentState === 'ASSESSED') {
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
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; margin-top: 1rem; padding-top: .75rem; border-top: 1px solid var(--av-color-border); }
    .capability-summary { display: flex; align-items: center; justify-content: space-between; gap: .75rem; }
    .capability-statuses { display: grid; gap: .45rem; }
    .capability-status { display: flex; align-items: center; gap: .45rem; color: var(--av-color-muted); font-size: .8rem; font-weight: 700; white-space: nowrap; }
    .capability-state { margin: 0; color: var(--av-color-muted); font-size: .8rem; font-weight: 700; }
    .capability-failed { color: var(--av-color-danger); }
    .details-toggle { min-block-size: 2.5rem; padding: .4rem .65rem; border: 0; background: transparent; color: var(--av-color-brand-strong); cursor: pointer; font: inherit; font-size: .8rem; font-weight: 800; white-space: nowrap; }
    .capability-details { padding: 1rem; border-top: 1px solid var(--av-color-border); }
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
      .capability-statuses { gap: .4rem; }
      .capability-status { justify-content: space-between; white-space: normal; }
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

  readonly view = input.required<ServiceProfitDataCapabilityView>();

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