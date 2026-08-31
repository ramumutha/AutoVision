import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MetadataService } from './metadata.service';
import { CommercialEnquiryPayload, CommercialEnquiryService } from './commercial-enquiry.service';

type EnquiryPurpose = 'PRODUCT_DEMO' | 'ADVISORY_IMPLEMENTATION' | 'PARTNERSHIP' | 'GENERAL_ENQUIRY';

@Component({
  selector: 'site-contact-us',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="contact-page" aria-labelledby="contact-title">
      <div class="intro"><p class="eyebrow">Start a conversation</p><h1 id="contact-title">Contact SPENMER</h1><p>Tell us what you are exploring and we will route your enquiry to the right conversation.</p><p class="privacy-note">We use this information to respond to your enquiry and arrange requested conversations. It is not used to subscribe you to marketing.</p></div>
      <form class="contact-form" (ngSubmit)="submit()" #contactForm="ngForm" aria-describedby="form-status">
        <p id="form-status" class="notice" role="status">{{ statusMessage() }}</p>
        <fieldset><legend>What brings you here?</legend><label><input type="radio" name="purpose" value="PRODUCT_DEMO" [(ngModel)]="purpose" (ngModelChange)="resetPurposeFields()" required> Product demo</label><label><input type="radio" name="purpose" value="ADVISORY_IMPLEMENTATION" [(ngModel)]="purpose" (ngModelChange)="resetPurposeFields()" required> Services</label><label><input type="radio" name="purpose" value="PARTNERSHIP" [(ngModel)]="purpose" (ngModelChange)="resetPurposeFields()" required> Partnership</label><label><input type="radio" name="purpose" value="GENERAL_ENQUIRY" [(ngModel)]="purpose" (ngModelChange)="resetPurposeFields()" required> General enquiry</label></fieldset>
        @if (purpose() === 'PRODUCT_DEMO') { <fieldset><legend>Product interest</legend><p class="hint">AutoVision is the platform family. Service Profit is the current capability available for demonstration.</p><label>Product<select name="product" [(ngModel)]="product"><option value="SERVICE_PROFIT_AI">Service Profit</option></select></label><label>Evaluation preference <span>(required)</span><select name="evaluationPreference" [(ngModel)]="evaluationPreference" required><option value="">Select an option</option><option value="GUIDED_DEMO">Guided demo</option><option value="DEMO_ENVIRONMENT">Explore a demo environment</option><option value="DEALER_PILOT">Discuss a dealer pilot</option></select></label><label>What would you like to see?<textarea name="demoMessage" [(ngModel)]="demoMessage" rows="4" maxlength="2000"></textarea></label>@if (evaluationPreference === 'DEALER_PILOT') { <details><summary>Optional dealer-pilot context</summary><div class="optional-details"><div class="check-grid"><span class="field-label">Business objectives</span>@for (item of businessObjectiveOptions; track item.value) { <label class="check"><input type="checkbox" [checked]="businessObjectives.includes(item.value)" (change)="toggle(businessObjectives, item.value)">{{ item.label }}</label> }</div><div class="two-column"><label>Service locations<input type="number" name="serviceLocations" [(ngModel)]="serviceLocations" min="1" max="100000"></label><label>Monthly service orders<select name="monthlyServiceOrders" [(ngModel)]="monthlyServiceOrders"><option value="">Select a range</option><option value="UNDER_250">Under 250</option><option value="250_1000">250 to 1,000</option><option value="1001_5000">1,001 to 5,000</option><option value="OVER_5000">Over 5,000</option></select></label></div><label>Current DMS / platform<input name="currentTechnology" [(ngModel)]="currentTechnology" maxlength="160"></label><label>Historical service data<select name="historicalDataAvailability" [(ngModel)]="historicalDataAvailability"><option value="">Select an option</option><option value="AVAILABLE">Available to discuss</option><option value="PARTIAL">Partially available</option><option value="UNKNOWN">Not sure yet</option></select></label><label class="check"><input type="checkbox" name="declinedRecommendationsRecorded" [(ngModel)]="declinedRecommendationsRecorded"> Declined or deferred recommendations are recorded</label></div></details> }</fieldset> }
        @if (purpose() === 'ADVISORY_IMPLEMENTATION') { <fieldset><legend>Services of interest</legend><div class="check-grid">@for (item of serviceOptions; track item.value) { <label class="check"><input type="checkbox" [checked]="servicePractices.includes(item.value)" (change)="toggle(servicePractices, item.value)">{{ item.label }}</label> }</div><label>What would you like help with?<textarea name="advisoryMessage" [(ngModel)]="advisoryMessage" rows="5" maxlength="2000" required></textarea></label></fieldset> }
        @if (purpose() === 'PARTNERSHIP') { <fieldset><legend>Partnership context</legend><label>Partnership type <span>(required)</span><select name="partnershipType" [(ngModel)]="partnershipType" required><option value="">Select a type</option><option value="TECHNOLOGY_INTEGRATION">Technology integration</option><option value="DMS_PLATFORM_INTEGRATION">DMS / platform integration</option><option value="OEM_DEALER_PARTNERSHIP">OEM / dealer partnership</option><option value="IMPLEMENTATION_PARTNERSHIP">Implementation partnership</option><option value="DATA_TECHNOLOGY_PARTNERSHIP">Data / technology partnership</option><option value="OTHER">Other</option></select></label><label>Proposed opportunity<textarea name="partnershipMessage" [(ngModel)]="partnershipMessage" rows="5" maxlength="2000" required></textarea></label></fieldset> }
        @if (purpose() === 'GENERAL_ENQUIRY') { <label>Message or requirement <span>(required)</span><textarea name="message" [(ngModel)]="message" rows="5" maxlength="2000" required></textarea></label> }
        <fieldset><legend>Your contact information</legend><label>Company or organization <span>(required)</span><input name="companyName" [(ngModel)]="companyName" autocomplete="organization" maxlength="160" required></label><div class="two-column"><label>First name <span>(required)</span><input name="firstName" [(ngModel)]="firstName" autocomplete="given-name" maxlength="80" required></label><label>Last name <span>(required)</span><input name="lastName" [(ngModel)]="lastName" autocomplete="family-name" maxlength="80" required></label></div><label>Business email <span>(required)</span><input type="email" name="businessEmail" [(ngModel)]="businessEmail" autocomplete="email" maxlength="254" required></label><div class="two-column"><label>Role / job title <input name="roleOrTitle" [(ngModel)]="roleOrTitle" maxlength="120"></label><label>Country / market <input name="countryOrMarket" [(ngModel)]="countryOrMarket" autocomplete="country-name" maxlength="80"></label></div><label>Phone <span>(optional)</span><input name="phone" [(ngModel)]="phone" autocomplete="tel" maxlength="40"></label></fieldset>
        <button type="submit" [disabled]="!contactForm.form.valid || !purposeSpecificValid() || submitting()">{{ submitting() ? 'Submitting...' : 'Submit enquiry' }}</button><a class="back-link" routerLink="/">Return to SPENMER</a>
      </form>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [],
})
export class ContactUsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly metadata = inject(MetadataService);
  private readonly enquiryService = inject(CommercialEnquiryService);
  readonly purpose = signal<EnquiryPurpose>(this.readPurpose());
  product = 'SERVICE_PROFIT_AI';
  readonly products: string[] = ['SERVICE_PROFIT_AI'];
  readonly businessObjectives: string[] = [];
  readonly servicePractices: string[] = [];
  readonly supportTypes: string[] = [];
  evaluationPreference = '';
  organizationType = '';
  partnershipType = '';
  currentTechnology = '';
  monthlyServiceOrders = '';
  historicalDataAvailability = '';
  serviceLocations?: number;
  declinedRecommendationsRecorded = false;
  phone = '';
  demoMessage = '';
  advisoryMessage = '';
  partnershipMessage = '';
  message = '';
  companyName = '';
  firstName = '';
  lastName = '';
  businessEmail = '';
  roleOrTitle = '';
  countryOrMarket = '';
  readonly submitting = signal(false);
  readonly statusMessage = signal('Your information is used to respond to this enquiry and arrange the requested conversation.');
  readonly businessObjectiveOptions = [{ value: 'INCREASE_SERVICE_REVENUE', label: 'Increase service revenue' }, { value: 'RECOVER_DECLINED_DEFERRED_WORK', label: 'Recover declined / deferred work' }, { value: 'IMPROVE_CUSTOMER_RETENTION', label: 'Improve customer retention' }, { value: 'IMPROVE_ADVISOR_PRODUCTIVITY', label: 'Improve advisor productivity' }, { value: 'IMPROVE_WORKSHOP_UTILIZATION', label: 'Improve workshop utilization' }, { value: 'IMPROVE_SERVICE_FOLLOW_UP', label: 'Improve service follow-up' }, { value: 'IMPROVE_MANAGEMENT_VISIBILITY', label: 'Improve management visibility' }, { value: 'OTHER', label: 'Other' }];
  readonly serviceOptions = [{ value: 'AUTOMOTIVE_DEALER_TECHNOLOGY', label: 'Automotive & Dealer Technology' }, { value: 'PRODUCT_SOLUTION_ENGINEERING', label: 'Product & Solution Engineering' }, { value: 'QUALITY_ENGINEERING_TEST_AUTOMATION', label: 'Quality Engineering & Test Automation' }, { value: 'PRODUCT_DEFINITION_REQUIREMENTS', label: 'Product Definition & Requirements Engineering' }, { value: 'AI_DIGITAL_TRANSFORMATION', label: 'AI & Digital Transformation' }, { value: 'FINTECH_SOLUTIONS_ADVISORY', label: 'FinTech Solutions Advisory' }, { value: 'OTHER', label: 'Other' }];

  constructor() { this.metadata.update('Contact Us', 'Start a conversation with SPENMER about products, services, and partnerships.'); }
  toggle(values: string[], value: string): void { const index = values.indexOf(value); index < 0 ? values.push(value) : values.splice(index, 1); }
  resetPurposeFields(): void { this.statusMessage.set('Your information is used to respond to this enquiry and arrange the requested conversation.'); }
  purposeSpecificValid(): boolean { return this.purpose() !== 'ADVISORY_IMPLEMENTATION' || this.servicePractices.length > 0; }
  submit(): void {
    if (this.submitting() || !this.purposeSpecificValid()) return;
    this.submitting.set(true);
    const payload: CommercialEnquiryPayload = { purpose: this.purpose(), companyName: this.companyName.trim(), firstName: this.firstName.trim(), lastName: this.lastName.trim(), businessEmail: this.businessEmail.trim(), roleOrTitle: this.roleOrTitle.trim(), countryOrMarket: this.countryOrMarket.trim(), messageOrRequirement: [this.message, this.demoMessage, this.advisoryMessage, this.partnershipMessage].filter(Boolean).join('\n').trim(), ...(this.purpose() === 'PRODUCT_DEMO' ? { productFamily: 'AUTOVISION', product: 'SERVICE_PROFIT_AI', products: this.canonical(this.products), evaluationPreference: this.evaluationPreference, businessObjectives: this.canonical(this.businessObjectives), serviceLocations: this.serviceLocations, monthlyServiceOrders: this.monthlyServiceOrders, currentTechnology: this.currentTechnology, historicalDataAvailability: this.historicalDataAvailability, declinedRecommendationsRecorded: this.declinedRecommendationsRecorded } : {}), ...(this.purpose() === 'ADVISORY_IMPLEMENTATION' ? { advisoryArea: 'OTHER', servicePractices: this.canonical(this.servicePractices), supportTypes: this.canonical(this.supportTypes), projectStage: '', desiredTimeframe: '', currentTechnology: this.currentTechnology } : {}), ...(this.purpose() === 'PARTNERSHIP' ? { partnershipType: this.partnershipType } : {}), organizationType: this.organizationType, phone: this.phone };
    this.enquiryService.submit(payload).then(() => this.statusMessage.set('Thank you. We have received your enquiry. Please check your business email to verify it.')).catch(error => this.statusMessage.set(CommercialEnquiryService.messageFor(error))).finally(() => this.submitting.set(false));
  }
  private canonical(values: string[]): string[] { return [...new Set(values.map(value => value.trim()).filter(Boolean))].sort(); }
  private readPurpose(): EnquiryPurpose { const purpose = this.route.snapshot.queryParamMap?.get('purpose'); if (purpose === 'product-demo' && this.readProduct()) return 'PRODUCT_DEMO'; if (purpose === 'advisory-implementation') return 'ADVISORY_IMPLEMENTATION'; if (purpose === 'partnership') return 'PARTNERSHIP'; return 'GENERAL_ENQUIRY'; }
  private readProduct(): boolean { return this.route.snapshot.queryParamMap?.get('product') === 'service-profit-ai'; }
}
