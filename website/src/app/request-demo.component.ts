import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MetadataService } from './metadata.service';

@Component({
  selector: 'site-request-demo',
  imports: [RouterLink],
  template: `
    <section class="request-page" aria-labelledby="request-title">
      <div class="intro"><p class="eyebrow">Start a conversation</p><h1 id="request-title">Request a Demo</h1><p>Tell us enough to make a useful first conversation about AutoVision and Service Profit AI.</p></div>
      <div class="demo-form"><p class="notice" role="status">Product Demo requests now use the unified Contact Us workflow.</p><a class="back-link" routerLink="/contact" [queryParams]="{ purpose: 'product-demo', product: 'service-profit-ai' }">Continue to Contact Us</a></div>
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