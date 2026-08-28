import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ProductDemoService } from './product-demo.service';
import { PublicFooterComponent } from './public-footer.component';
import { PublicHeaderComponent } from './public-header.component';

@Component({
  selector: 'site-root',
  imports: [RouterOutlet, PublicFooterComponent, PublicHeaderComponent],
  template: `
    <a class="skip-link" href="#main-content">Skip to content</a>
    <site-header />
    <main id="main-content"><router-outlet /></main>
    <site-footer />
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    :host { display: block; min-block-size: 100vh; }
    main { min-block-size: 55vh; }
    .skip-link { position: absolute; inset-block-start: .5rem; inset-inline-start: .5rem; z-index: 10; padding: .65rem 1rem; transform: translateY(-150%); background: var(--site-color-ink); color: white; }
    .skip-link:focus { transform: translateY(0); }
  `],
})
export class AppComponent {
  private readonly productDemo = inject(ProductDemoService);

  constructor() { void this.productDemo.load(); }
}