import { CurrencyPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { ServiceProfitOpportunityResponse, ServiceProfitVehicleContext } from './service-profit.models';

@Component({
  selector: 'app-service-profit-opportunity-detail',
  imports: [CurrencyPipe, DatePipe, AvStatusComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @let selectedOpportunity = opportunity();
    <section class="detail" [id]="regionId()" role="region" [attr.aria-labelledby]="headingId()">
      <header class="detail-header">
        <div>
          <p class="eyebrow">{{ localization.text('opportunityDetail') }}</p>
          <h2 [id]="headingId()">{{ selectedOpportunity.title }}</h2>
        </div>
        <div class="detail-statuses">
          <av-status [label]="label(selectedOpportunity.status)" [tone]="isSuppressed() ? 'neutral' : 'info'" />
          <av-status [label]="isSuppressed() ? localization.text('suppressed') : label(selectedOpportunity.actionability)" [tone]="isSuppressed() ? 'neutral' : actionabilityTone()" />
        </div>
      </header>

      <div class="commercial-summary">
        <div class="detail-value">
          <h3>{{ localization.text('recoverablePotential') }}</h3>
          <p class="commercial-value">{{ selectedOpportunity.potentialAmount | currency:selectedOpportunity.currencyCode:'symbol':'1.0-2' }}</p>
        </div>
        <dl class="detail-facts">
          <div><dt>{{ localization.text('priority') }}</dt><dd>{{ label(selectedOpportunity.priority) }}</dd></div>
          <div><dt>{{ localization.text('actionability') }}</dt><dd>{{ label(selectedOpportunity.actionability) }}</dd></div>
          <div><dt>{{ localization.text('opportunityType') }}</dt><dd>{{ label(selectedOpportunity.opportunityType) }}</dd></div>
        </dl>
      </div>

      @if (isSuppressed()) {
        <aside class="safety-notice suppression" [attr.aria-label]="localization.text('suppressedOpportunity')">
          <strong>{{ localization.text('doNotAction') }}</strong>
          <span>{{ localization.text('suppressedOpportunity') }}</span>
          <span>{{ selectedOpportunity.suppressionReason ? label(selectedOpportunity.suppressionReason) : localization.text('suppressed') }}</span>
          @if (selectedOpportunity.suppressedAt) { <time [attr.datetime]="selectedOpportunity.suppressedAt">{{ selectedOpportunity.suppressedAt | date:'medium' }}</time> }
        </aside>
      }

      @if (requiresReview()) {
        <aside class="safety-notice review-required" role="note">
          <strong>{{ localization.text('reviewRequired') }}</strong>
          <span>{{ localization.text('reviewBeforeCustomerContact') }}</span>
        </aside>
      }

      @if (hasCustomerContext() || hasVehicleContext() || hasServiceContext()) {
        <div class="context-grid">
          @if (hasCustomerContext() && selectedOpportunity.context?.customer; as customer) {
            <section class="context-section customer-context" [attr.aria-labelledby]="contextHeadingId('customer')">
              <h3 [id]="contextHeadingId('customer')">{{ localization.text('customer') }}</h3>
              @if (customer.displayName) { <strong class="context-primary">{{ customer.displayName }}</strong> }
              <dl>
                @if (customer.reference) { <div><dt>{{ localization.text('customerRef') }}</dt><dd>{{ customer.reference }}</dd></div> }
                @if (customer.phone) { <div><dt>{{ localization.text('phone') }}</dt><dd>{{ customer.phone }}</dd></div> }
                @if (customer.email) { <div><dt>{{ localization.text('email') }}</dt><dd>{{ customer.email }}</dd></div> }
                @if (customer.contactable === true) { <div><dt>{{ localization.text('contactable') }}</dt><dd>{{ localization.text('yes') }}</dd></div> }
              </dl>
              @if (customer.contactable === false) { <p>{{ localization.text('contactInformationIncomplete') }}</p> }
            </section>
          }
          @if (hasVehicleContext() && selectedOpportunity.context?.vehicle; as vehicle) {
            <section class="context-section vehicle-context" [attr.aria-labelledby]="contextHeadingId('vehicle')">
              <h3 [id]="contextHeadingId('vehicle')">{{ localization.text('vehicle') }}</h3>
              <strong class="context-primary">{{ vehiclePrimary(vehicle) }}</strong>
              @if (vehicleDescription(vehicle); as description) { <p>{{ description }}</p> }
              <dl>
                @if (vehicle.vin) { <div><dt>{{ localization.text('vin') }}</dt><dd>{{ vehicle.vin }}</dd></div> }
                @if (vehicle.powertrain) { <div><dt>{{ localization.text('powertrain') }}</dt><dd>{{ vehicle.powertrain }}</dd></div> }
              </dl>
            </section>
          }
          @if (hasServiceContext() && selectedOpportunity.context?.service; as service) {
            <section class="context-section service-context" [attr.aria-labelledby]="contextHeadingId('service')">
              <h3 [id]="contextHeadingId('service')">{{ localization.text('serviceContextHeading') }}</h3>
              <dl>
                @if (service.orderReference) { <div><dt>{{ localization.text('repairOrder') }}</dt><dd>{{ service.orderReference }}</dd></div> }
                @if (service.serviceDate) { <div><dt>{{ localization.text('serviceDate') }}</dt><dd>{{ service.serviceDate | date:'mediumDate' }}</dd></div> }
                @if (service.description) { <div><dt>{{ localization.text('serviceItem') }}</dt><dd>{{ service.description }}</dd></div> }
                @if (service.advisorContext) { <div><dt>{{ localization.text('advisorContext') }}</dt><dd>{{ service.advisorContext }}</dd></div> }
              </dl>
            </section>
          }
        </div>
      }

      <section class="explanation" [attr.aria-labelledby]="contextHeadingId('explanation')">
        <div>
          <p class="eyebrow">{{ localization.text('whyAutoVisionFoundThis') }}</p>
          <h3 [id]="contextHeadingId('explanation')">{{ selectedOpportunity.explanation.headline }}</h3>
          <p>{{ selectedOpportunity.explanation.rationale }}</p>
          <dl><div><dt>{{ localization.text('evidenceBasis') }}</dt><dd>{{ selectedOpportunity.explanation.evidenceBasis }}</dd></div></dl>
        </div>
        <aside class="safety-notice recommended-action">
          <h3>{{ localization.text('recommendedAction') }}</h3>
          <p>{{ selectedOpportunity.explanation.recommendedAction }}</p>
        </aside>
      </section>

      <details class="audit-details provenance">
        <summary>{{ localization.text('evidenceAndAuditDetails') }}</summary>
        <dl>
          <div><dt>{{ localization.text('evidenceStrength') }}</dt><dd>{{ label(selectedOpportunity.evidenceStrength) }}</dd></div>
          <div><dt>{{ localization.text('detected') }}</dt><dd>{{ selectedOpportunity.detectedAt | date:'medium' }}</dd></div>
          <div><dt>{{ localization.text('sourceSystem') }}</dt><dd>{{ selectedOpportunity.sourceSystem }}</dd></div>
          <div><dt>{{ localization.text('sourceEntityType') }}</dt><dd>{{ selectedOpportunity.sourceEntityType }}</dd></div>
          <div><dt>{{ localization.text('sourceEntityId') }}</dt><dd>{{ selectedOpportunity.sourceEntityId }}</dd></div>
          <div><dt>{{ localization.text('policyVersion') }}</dt><dd>{{ selectedOpportunity.policyVersion }}</dd></div>
        </dl>
      </details>
    </section>
  `,
  styles: [`
    :host { display: block; }
    .detail { padding: 1.25rem; border-top: 4px solid var(--av-color-brand); background: var(--av-color-surface); color: var(--av-color-ink); }
    .detail-header { display: flex; justify-content: space-between; gap: 1.5rem; }
    .detail-header h2 { margin: 0; font-size: 1.5rem; }
    .detail-statuses { display: flex; align-items: flex-start; gap: .5rem; }
    .eyebrow { margin: 0 0 .35rem; color: var(--av-color-brand); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }
    .commercial-summary { display: grid; grid-template-columns: minmax(12rem, .8fr) minmax(0, 2fr); gap: 1rem; margin-top: 1rem; padding: 1rem; border: 1px solid var(--av-color-border); background: var(--av-color-canvas); }
    .commercial-summary h3 { margin: 0; color: var(--av-color-muted); font-size: .8rem; text-transform: uppercase; }
    .commercial-value { margin: .75rem 0 0; font-size: 1.75rem; font-weight: 800; }
    .detail-facts, .provenance dl { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; margin: 0; }
    dl div { min-width: 0; }
    dt { color: var(--av-color-muted); font-size: .75rem; font-weight: 800; text-transform: uppercase; }
    dd { margin: .3rem 0 0; overflow-wrap: anywhere; }
    .safety-notice { display: flex; flex-wrap: wrap; gap: .5rem 1rem; margin: 1rem 0; padding: .8rem 1rem; border-left: 4px solid var(--av-color-muted); background: #eef2f4; }
    .context-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 1rem; padding: 1.25rem 0; border-top: 1px solid var(--av-color-border); }
    .context-section h3, .explanation h3 { margin: 0; font-size: 1.1rem; }
    .context-section p, .explanation p { color: var(--av-color-muted); line-height: 1.55; }
    .context-section dl { display: grid; gap: .65rem; }
    .context-primary { display: block; margin-top: .65rem; }
    .explanation { display: grid; grid-template-columns: minmax(0, 1.3fr) minmax(16rem, 1fr); gap: 2rem; padding: 1.25rem 0; border-block: 1px solid var(--av-color-border); }
    .explanation dl { margin: 1rem 0 0; }
    .recommended-action { align-content: start; margin: 0; }
    .provenance { margin-top: 1rem; }
    .provenance summary { cursor: pointer; font-weight: 800; }
    .provenance dl { margin-top: 1rem; }
    @media (max-width: 1100px) { .context-grid { grid-template-columns: 1fr 1fr; } .explanation { grid-template-columns: 1fr; } }
    @media (max-width: 720px) { .detail-header { flex-direction: column; } .commercial-summary, .context-grid, .detail-facts, .provenance dl { grid-template-columns: 1fr; } }
  `],
})
export class ServiceProfitOpportunityDetailComponent {
  protected readonly localization = inject(LocalizationService);
  readonly opportunity = input.required<ServiceProfitOpportunityResponse>();

  protected regionId(): string {
    return `opportunity-detail-${this.opportunity().id}`;
  }

  protected headingId(): string {
    return `${this.regionId()}-heading`;
  }

  protected contextHeadingId(section: string): string {
    return `${this.regionId()}-${section}-heading`;
  }

  protected label(value: string): string {
    return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
  }

  protected isSuppressed(): boolean {
    const opportunity = this.opportunity();
    return opportunity.status === 'SUPPRESSED'
      || opportunity.actionability === 'SUPPRESSED'
      || !!opportunity.suppressionReason;
  }

  protected requiresReview(): boolean {
    return this.opportunity().actionability === 'REVIEW_REQUIRED';
  }

  protected actionabilityTone(): AvStatusTone {
    const actionability = this.opportunity().actionability;
    if (actionability === 'READY') return 'success';
    if (actionability === 'REVIEW_REQUIRED' || actionability === 'CONTACT_DATA_MISSING') return 'warning';
    return actionability === 'SUPPRESSED' ? 'neutral' : 'danger';
  }

  protected hasCustomerContext(): boolean {
    const customer = this.opportunity().context?.customer;
    return !!customer && Object.values(customer).some((value) => value !== null && value !== '');
  }

  protected hasVehicleContext(): boolean {
    const vehicle = this.opportunity().context?.vehicle;
    return !!vehicle && Object.values(vehicle).some((value) => value !== null && value !== '');
  }

  protected hasServiceContext(): boolean {
    const service = this.opportunity().context?.service;
    return !!service && Object.values(service).some((value) => value !== null && value !== '');
  }

  protected vehiclePrimary(vehicle: ServiceProfitVehicleContext): string {
    return vehicle.registration
      || [vehicle.make, vehicle.model].filter(Boolean).join(' ')
      || vehicle.vin
      || '';
  }

  protected vehicleDescription(vehicle: ServiceProfitVehicleContext): string {
    return [[vehicle.make, vehicle.model].filter(Boolean).join(' '), vehicle.modelYear]
      .flat()
      .filter(Boolean)
      .join(' · ');
  }
}
