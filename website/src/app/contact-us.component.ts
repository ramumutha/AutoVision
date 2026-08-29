import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MetadataService } from './metadata.service';
import { CommercialEnquiryPayload, CommercialEnquiryService } from './commercial-enquiry.service';

type EnquiryPurpose = 'PRODUCT_DEMO' | 'ADVISORY_IMPLEMENTATION' | 'PARTNERSHIP' | 'GENERAL_ENQUIRY';
type DemoProduct = 'SERVICE_PROFIT_AI';

@Component({
  selector: 'site-contact-us',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="contact-page" aria-labelledby="contact-title">
      <div class="intro">
        <p class="eyebrow">Start a conversation</p>
        <h1 id="contact-title">Contact Us</h1>
        <p>Tell us what you are exploring and we will route your enquiry to the right conversation.</p>
        <p class="privacy-note">Your information is used to respond to this enquiry and arrange requested demonstrations or services. It is not used to subscribe you to marketing.</p>
      </div>
      <form class="contact-form" (ngSubmit)="submit()" #contactForm="ngForm" aria-describedby="form-status">
        <p id="form-status" class="notice" role="status">{{ statusMessage() }}</p>

        <fieldset>
          <legend>How can we help?</legend>
          <label><input type="radio" name="purpose" value="PRODUCT_DEMO" [(ngModel)]="purpose" required> Product Demo</label>
          <label><input type="radio" name="purpose" value="ADVISORY_IMPLEMENTATION" [(ngModel)]="purpose" required> Advisory &amp; Implementation</label>
          <label><input type="radio" name="purpose" value="PARTNERSHIP" [(ngModel)]="purpose" required> Partnership</label>
          <label><input type="radio" name="purpose" value="GENERAL_ENQUIRY" [(ngModel)]="purpose" required> General Enquiry</label>
        </fieldset>

        @if (purpose() === 'PRODUCT_DEMO') {
          <fieldset>
            <legend>Product Demo details</legend>
            <label>Product <span>(required)</span><select name="product" [(ngModel)]="product" required><option value="SERVICE_PROFIT_AI">Service Profit AI</option></select></label>
            <label>What would you like to see? <textarea name="demoMessage" [(ngModel)]="demoMessage" rows="4" maxlength="2000"></textarea></label>
          </fieldset>
        }
        @if (purpose() === 'ADVISORY_IMPLEMENTATION') {
          <fieldset>
            <legend>Advisory &amp; Implementation details</legend>
            <label>Area of interest <span>(required)</span><select name="advisoryArea" [(ngModel)]="advisoryArea" required><option value="">Select an area</option><option value="AUTOMOTIVE_TECHNOLOGY_ADVISORY">Automotive Technology Advisory</option><option value="AI_DIGITAL_TRANSFORMATION">AI &amp; Digital Transformation</option><option value="INTEGRATION_IMPLEMENTATION">Integration &amp; Implementation</option><option value="OTHER">Other</option></select></label>
            <label>What are you exploring? <textarea name="advisoryMessage" [(ngModel)]="advisoryMessage" rows="4" maxlength="2000"></textarea></label>
          </fieldset>
        }
        @if (purpose() === 'PARTNERSHIP' || purpose() === 'GENERAL_ENQUIRY') {
          <label>Message or requirement <span>(required)</span><textarea name="message" [(ngModel)]="message" rows="5" maxlength="2000" required></textarea></label>
        }

        <fieldset>
          <legend>Your business/contact information</legend>
          <label>Company or dealership <span>(required)</span><input name="companyName" [(ngModel)]="companyName" autocomplete="organization" maxlength="160" required></label>
          <div class="two-column"><label>First name <span>(required)</span><input name="firstName" [(ngModel)]="firstName" autocomplete="given-name" maxlength="80" required></label><label>Last name <span>(required)</span><input name="lastName" [(ngModel)]="lastName" autocomplete="family-name" maxlength="80" required></label></div>
          <label>Business email <span>(required)</span><input type="email" name="businessEmail" [(ngModel)]="businessEmail" autocomplete="email" maxlength="254" required></label>
          <div class="two-column"><label>Role or title <span>(required)</span><input name="roleOrTitle" [(ngModel)]="roleOrTitle" maxlength="120" required></label><label>Country or market <span>(required)</span><input name="countryOrMarket" [(ngModel)]="countryOrMarket" autocomplete="country-name" maxlength="80" required></label></div>
        </fieldset>

        <button type="submit" [disabled]="!contactForm.form.valid || submitting()">{{ submitting() ? 'Submitting...' : 'Submit enquiry' }}</button>
        <a class="back-link" routerLink="/">Return to AutoVision</a>
      </form>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .contact-page { display: grid; grid-template-columns: minmax(0, .85fr) minmax(20rem, 1.15fr); gap: var(--site-space-8); max-inline-size: var(--site-content-max); margin: auto; padding: var(--site-space-9) var(--site-gutter); }
    .eyebrow { margin: 0 0 var(--site-space-4); color: var(--site-color-brand); font-size: .8rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
    h1 { margin: 0; font-size: var(--site-type-hero); line-height: var(--site-leading-display); } .intro > p:not(.eyebrow) { max-inline-size: 32rem; color: var(--site-color-ink-soft); font-size: var(--site-type-body-large); line-height: var(--site-leading-body); }
    .privacy-note { font-size: .9rem !important; }
    .contact-form { display: grid; gap: var(--site-space-4); padding: var(--site-space-6); border: 1px solid var(--site-color-border); border-radius: var(--site-radius-md); background: var(--site-color-surface); box-shadow: var(--site-shadow-sm); }
    fieldset { display: grid; gap: .7rem; border: 0; padding: 0; } legend { margin-block-end: .25rem; color: var(--site-color-ink); font-weight: 800; } label { display: grid; gap: .35rem; color: var(--site-color-ink); font-weight: 700; } label:has(input[type="radio"]) { display: flex; align-items: center; font-weight: 500; } label span { color: var(--site-color-ink-soft); font-size: .8rem; font-weight: 400; }
    input, select, textarea { min-block-size: 2.75rem; inline-size: 100%; padding: .65rem .7rem; border: 1px solid var(--site-color-border); border-radius: var(--site-radius-sm); color: var(--site-color-ink); background: var(--site-color-surface); } textarea { resize: vertical; }
    .two-column { display: grid; grid-template-columns: 1fr 1fr; gap: var(--site-space-3); }
    .notice { margin: 0; padding: var(--site-space-4); border-inline-start: 4px solid var(--site-color-signal); background: #fff5eb; color: var(--site-color-ink-soft); line-height: 1.5; } button { min-block-size: 2.75rem; border: 0; border-radius: var(--site-radius-sm); background: var(--site-color-brand); color: white; font-weight: 800; } button:disabled { cursor: not-allowed; opacity: .55; } .back-link { color: var(--site-color-brand-strong); font-weight: 700; text-align: center; }
    @media (max-width: 760px) { .contact-page { grid-template-columns: 1fr; padding-block: var(--site-space-7); } .contact-form { padding: var(--site-space-4); } .two-column { grid-template-columns: 1fr; } }
  `],
})
export class ContactUsComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly metadata = inject(MetadataService);
  private readonly enquiryService = inject(CommercialEnquiryService);
  readonly purpose = signal<EnquiryPurpose>(this.readPurpose());
  readonly product = signal<DemoProduct>('SERVICE_PROFIT_AI');
  advisoryArea = '';
  demoMessage = '';
  advisoryMessage = '';
  message = '';
  companyName = '';
  firstName = '';
  lastName = '';
  businessEmail = '';
  roleOrTitle = '';
  countryOrMarket = '';
  readonly submitting = signal(false);
  readonly statusMessage = signal('Your information is used to respond to this enquiry and arrange requested demonstrations or services.');

  constructor() { this.metadata.update('Contact Us', 'Start a conversation about AutoVision.'); }

  submit(): void {
    if (this.submitting()) return;
    this.submitting.set(true);
    const payload: CommercialEnquiryPayload = {
      purpose: this.purpose(), companyName: this.companyName.trim(), firstName: this.firstName.trim(), lastName: this.lastName.trim(),
      businessEmail: this.businessEmail.trim(), roleOrTitle: this.roleOrTitle.trim(), countryOrMarket: this.countryOrMarket.trim(),
      messageOrRequirement: [this.message, this.demoMessage, this.advisoryMessage].filter(Boolean).join('\n').trim(),
      ...(this.purpose() === 'PRODUCT_DEMO' ? { productFamily: 'AUTOVISION', product: this.product() } : {}),
      ...(this.purpose() === 'ADVISORY_IMPLEMENTATION' ? { advisoryArea: this.advisoryArea } : {}),
    };
    this.enquiryService.submit(payload).then(() => {
      this.statusMessage.set('Thank you. We have received your enquiry. Please check your business email to verify it.');
    }).catch(error => this.statusMessage.set(CommercialEnquiryService.messageFor(error))).finally(() => this.submitting.set(false));
  }

  private readPurpose(): EnquiryPurpose {
    const purpose = this.route.snapshot.queryParamMap.get('purpose');
    const product = this.route.snapshot.queryParamMap.get('product');
    if (purpose === 'product-demo' && product === 'service-profit-ai') return 'PRODUCT_DEMO';
    if (purpose === 'advisory-implementation') return 'ADVISORY_IMPLEMENTATION';
    if (purpose === 'partnership') return 'PARTNERSHIP';
    return 'GENERAL_ENQUIRY';
  }
}
