import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ProductDemoService } from './product-demo.service';

@Component({
  selector: 'site-footer',
  imports: [RouterLink],
  template: `
    <footer class="footer">
      <div class="footer-grid">
        <div><p class="brand">VERSPEN</p><p class="note">AutoVision by VERSPEN</p></div>
        <div><h2>Products</h2><a routerLink="/autovision">AutoVision</a><a routerLink="/service-profit-ai">Service Profit AI</a></div>
        <div><h2>Company</h2><a routerLink="/about">About</a><a routerLink="/contact">Contact</a></div>
        <div><h2>Evaluate</h2><a routerLink="/request-demo">Request a Demo</a>@if (productDemo.url(); as productDemoUrl) { <a class="product-link" [href]="productDemoUrl">Product Demo</a> } @else { <span class="product-unavailable">Product Demo unavailable</span> }</div>
        <div><h2>Trust &amp; Legal</h2><a routerLink="/integration-security">Integration &amp; Security</a><a routerLink="/privacy">Privacy</a><a routerLink="/terms">Terms</a></div>
      </div>
      <p class="provisional">VERSPEN is a provisional brand name for this private/local website foundation. Public launch is not approved.</p>
    </footer>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .footer { margin-block-start: var(--site-space-9); padding: var(--site-space-8) var(--site-gutter) var(--site-space-6); background: var(--site-color-surface-dark); color: white; }
    .footer-grid { display: grid; grid-template-columns: 1.5fr repeat(4, 1fr); gap: var(--site-space-6); max-inline-size: var(--site-wide-max); margin: auto; }
    .brand { margin: 0; color: white; font-weight: 800; letter-spacing: .1em; }
    .note, .provisional { color: #b9c8cc; font-size: .875rem; }
    h2 { margin: 0 0 .75rem; font-size: .8rem; text-transform: uppercase; letter-spacing: .06em; }
    a { display: block; min-block-size: 2.75rem; margin-block: .55rem; color: #d9e3e5; font-size: .9rem; text-decoration: none; } a:hover { color: white; text-decoration: underline; } .product-unavailable { display: block; margin-block: .55rem; color: #9eafb3; font-size: .9rem; }
    .provisional { max-inline-size: var(--site-wide-max); margin: var(--site-space-8) auto 0; padding-block-start: var(--site-space-4); border-block-start: 1px solid #41545b; }
    @media (max-width: 900px) { .footer-grid { grid-template-columns: repeat(3, 1fr); } }
    @media (max-width: 600px) { .footer { padding-block-start: var(--site-space-7); } .footer-grid { grid-template-columns: repeat(2, 1fr); } }
  `],
})
export class PublicFooterComponent {
  readonly productDemo = inject(ProductDemoService);

}