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

@Component({
  selector: 'app-service-profit-data-capability',
  imports: [DatePipe, AvStatusComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="capability" [attr.aria-labelledby]="headingId">
      <div class="capability-summary">
        <div class="heading">
          <h2 [id]="headingId">{{ localization.text('dataCapability') }}</h2>
          <p>{{ localization.text('dataCapabilityContext') }}</p>
        </div>

        @if (data().assessmentState === 'ASSESSED') {
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

        <button
          class="details-toggle"
          type="button"
          (click)="expanded.set(!expanded())"
          [attr.aria-expanded]="expanded()"
          [attr.aria-controls]="detailsId">
          {{ localization.text(expanded() ? 'hideDataDetails' : 'viewDataDetails') }}
        </button>
      </div>

      @if (expanded()) {
        <div class="capability-details" [id]="detailsId">
          @if (data().assessmentState === 'ASSESSED') {
            <dl class="assessment-metadata">
              <div>
                <dt>{{ localization.text('dataSource') }}</dt>
                <dd>
                  {{ data().sourceDatasetId || localization.text('notAvailable') }}
                  @if (data().sourceDatasetVersion) {
                    <span> / {{ data().sourceDatasetVersion }}</span>
                  }
                </dd>
              </div>
              <div>
                <dt>{{ localization.text('assessed') }}</dt>
                <dd>
                  @if (data().assessedAt) {
                    {{ data().assessedAt | date:'medium' }}
                  } @else {
                    {{ localization.text('notAvailable') }}
                  }
                </dd>
              </div>
              <div>
                <dt>{{ localization.text('assessmentPolicy') }}</dt>
                <dd>{{ data().assessmentPolicyVersion || localization.text('notAvailable') }}</dd>
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
    </section>
  `,
  styles: [`
    :host { display: block; margin-top: 1rem; }
    .capability { border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); }
    .capability-summary { display: grid; grid-template-columns: minmax(12rem, 1fr) auto auto; align-items: center; gap: 1rem; padding: .85rem 1rem; }
    .heading h2 { margin: 0; color: var(--av-color-ink); font-size: 1rem; }
    .heading p { margin: .2rem 0 0; color: var(--av-color-muted); font-size: .8rem; }
    .capability-statuses { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: .75rem 1rem; }
    .capability-status { display: flex; align-items: center; gap: .45rem; color: var(--av-color-muted); font-size: .8rem; font-weight: 700; white-space: nowrap; }
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
      .capability-summary { grid-template-columns: 1fr auto; }
      .capability-statuses { grid-column: 1 / -1; grid-row: 2; justify-content: flex-start; }
    }
    @media (max-width: 600px) {
      :host { margin-top: .75rem; }
      .capability-summary { grid-template-columns: minmax(0, 1fr) auto; gap: .5rem; padding: .75rem; }
      .heading p { display: none; }
      .capability-statuses { display: grid; grid-template-columns: 1fr; gap: .4rem; }
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
  protected readonly headingId = 'service-profit-data-capability-heading';
  protected readonly detailsId = 'service-profit-data-capability-details';

  readonly data = input.required<ServiceProfitDataCapabilityResponse>();

  protected readonly displayCapabilities = computed(() => {
    const order: ServiceProfitDataCapability[] = [
      'REVENUE_ATTRIBUTION',
      'GROSS_PROFIT_ATTRIBUTION',
    ];

    return order
      .map((capability) => this.data().capabilities.find((item) => item.capability === capability))
      .filter((item): item is ServiceProfitDataCapabilityItem => item !== undefined);
  });

  protected capabilityLabel(capability: ServiceProfitDataCapability): string {
    return capability === 'REVENUE_ATTRIBUTION'
      ? this.localization.text('revenueAttribution')
      : this.localization.text('grossProfitAttribution');
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