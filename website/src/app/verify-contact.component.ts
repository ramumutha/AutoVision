import { ChangeDetectionStrategy, Component, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CommercialEnquiryService } from './commercial-enquiry.service';
import { MetadataService } from './metadata.service';

@Component({
  selector: 'site-verify-contact',
  imports: [RouterLink],
  template: `
    <section class="verify-page" aria-labelledby="verify-title">
      <p class="eyebrow">Contact Us</p>
      <h1 id="verify-title">{{ heading() }}</h1>
      <p role="status">{{ message() }}</p>
      @if (verified()) { <a routerLink="/contact">Return to Contact Us</a> }
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .verify-page { max-inline-size: 42rem; min-block-size: 45vh; margin: auto; padding: var(--site-space-9) var(--site-gutter); } .eyebrow { color: var(--site-color-brand); font-size: .8rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; } h1 { font-size: var(--site-type-display); } p { color: var(--site-color-ink-soft); line-height: 1.6; } a { color: var(--site-color-brand-strong); font-weight: 700; }
  `],
})
export class VerifyContactComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly service = inject(CommercialEnquiryService);
  private readonly metadata = inject(MetadataService);
  readonly heading = signal('Verifying your email');
  readonly message = signal('Please wait while we verify your enquiry.');
  readonly verified = signal(false);

  constructor() {
    this.metadata.update('Verify your email', 'Verify your commercial enquiry email.');
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) { this.fail(); return; }
    this.service.verify(token).then(result => {
      if (result.status === 'VERIFIED') {
        this.heading.set('Email verified');
        this.message.set('Thank you. Your enquiry will be reviewed and next steps will follow.');
        this.verified.set(true);
      } else this.fail();
    }).catch(() => this.fail());
  }

  private fail(): void {
    this.heading.set('Verification unavailable');
    this.message.set('This verification link is invalid, expired, or has already been used.');
  }
}
