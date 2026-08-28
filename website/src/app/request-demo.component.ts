import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MetadataService } from './metadata.service';

@Component({
  selector: 'site-request-demo',
  imports: [FormsModule, RouterLink],
  template: `
    <section class="request-page" aria-labelledby="request-title">
      <div class="intro"><p class="eyebrow">Start a conversation</p><h1 id="request-title">Request a Demo</h1><p>Tell us enough to make a useful first conversation about AutoVision and Service Profit AI.</p></div>
      <form class="demo-form" (submit)="$event.preventDefault()" aria-describedby="form-status">
        <p id="form-status" class="notice" role="status">Demo requests are not submitted from this W3 foundation. A secure intake boundary will be added in a later governed workstream.</p>
        <label>Name <span>(required)</span><input name="name" autocomplete="name" required></label>
        <label>Work email <span>(required)</span><input type="email" name="email" autocomplete="email" required></label>
        <label>Company or dealership <span>(required)</span><input name="organization" autocomplete="organization" required></label>
        <label>Country <span>(required)</span><input name="country-name" autocomplete="country-name" required></label>
        <label>Role <span>(required)</span><select name="role" required><option value="" selected>Select a role</option><option>Dealer principal / owner</option><option>General manager</option><option>Aftersales leader</option><option>Service manager</option><option>Technology or integration partner</option><option>Other</option></select></label>
        <label>Number of locations <span>(optional)</span><input type="number" name="locations" min="1" inputmode="numeric"></label>
        <label>Current DMS <span>(optional)</span><input name="dms" autocomplete="off"></label>
        <label>Message <span>(optional)</span><textarea name="message" rows="5" maxlength="2000"></textarea></label>
        <p class="privacy-note">Privacy acknowledgement and consent language will be supplied with the approved intake design.</p>
        <button type="submit" disabled>Submit request</button>
        <a class="back-link" routerLink="/">Return to AutoVision</a>
      </form>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .request-page { display: grid; grid-template-columns: minmax(0, .85fr) minmax(20rem, 1.15fr); gap: var(--site-space-8); max-inline-size: var(--site-content-max); margin: auto; padding: var(--site-space-9) var(--site-gutter); }
    .eyebrow { margin: 0 0 var(--site-space-4); color: var(--site-color-brand); font-size: .8rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
    h1 { margin: 0; font-size: var(--site-type-hero); line-height: var(--site-leading-display); } .intro > p:last-child { max-inline-size: 32rem; color: var(--site-color-ink-soft); font-size: var(--site-type-body-large); line-height: var(--site-leading-body); }
    .demo-form { display: grid; gap: var(--site-space-4); padding: var(--site-space-6); border: 1px solid var(--site-color-border); border-radius: var(--site-radius-md); background: var(--site-color-surface); box-shadow: var(--site-shadow-sm); }
    label { display: grid; gap: .35rem; color: var(--site-color-ink); font-weight: 700; } label span { color: var(--site-color-ink-soft); font-size: .8rem; font-weight: 400; }
    input, select, textarea { min-block-size: 2.75rem; inline-size: 100%; padding: .65rem .7rem; border: 1px solid var(--site-color-border); border-radius: var(--site-radius-sm); color: var(--site-color-ink); background: var(--site-color-surface); } textarea { resize: vertical; }
    .notice { margin: 0; padding: var(--site-space-4); border-inline-start: 4px solid var(--site-color-signal); background: #fff5eb; color: var(--site-color-ink-soft); line-height: 1.5; }
    .privacy-note { margin: 0; color: var(--site-color-ink-soft); font-size: .875rem; line-height: 1.5; } button { min-block-size: 2.75rem; border: 0; border-radius: var(--site-radius-sm); background: var(--site-color-brand); color: white; font-weight: 800; } button:disabled { cursor: not-allowed; opacity: .55; }
    .back-link { color: var(--site-color-brand-strong); font-weight: 700; text-align: center; }
    @media (max-width: 760px) { .request-page { grid-template-columns: 1fr; padding-block: var(--site-space-7); } .demo-form { padding: var(--site-space-4); } }
  `],
})
export class RequestDemoComponent {
  private readonly metadata = inject(MetadataService);

  constructor() { this.metadata.update('Request a Demo', 'Request a guided AutoVision demonstration.'); }
}