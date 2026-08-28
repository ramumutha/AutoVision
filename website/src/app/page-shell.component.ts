import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MetadataService } from './metadata.service';

@Component({
  selector: 'site-page-shell',
  imports: [RouterLink],
  template: `
    <section class="page" [class.not-found]="notFound" aria-labelledby="page-title">
      <div class="page-inner">
        <p class="eyebrow">{{ eyebrow }}</p>
        <h1 id="page-title">{{ title }}</h1>
        <p class="lede">{{ description }}</p>
        @if (notFound) { <a class="button primary" routerLink="/">Return home</a> }
        @else { <p class="foundation-note">This route is ready for W4 content implementation.</p><a class="button primary" routerLink="/request-demo">Request a Demo</a> }
      </div>
    </section>
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  styles: [`
    .page { min-block-size: 55vh; padding: var(--site-space-9) var(--site-gutter); background: var(--site-color-canvas); }
    .page-inner { max-inline-size: var(--site-content-max); margin: auto; }
    .eyebrow { margin: 0 0 var(--site-space-4); color: var(--site-color-brand); font-size: .8rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; }
    h1 { max-inline-size: 15ch; margin: 0; font-family: var(--site-font-display); font-size: var(--site-type-display); line-height: var(--site-leading-display); }
    .lede { max-inline-size: 42rem; margin: var(--site-space-5) 0 var(--site-space-6); color: var(--site-color-ink-soft); font-size: clamp(1.05rem, 2vw, 1.3rem); line-height: 1.6; }
    .foundation-note { max-inline-size: 42rem; margin-block-end: var(--site-space-5); color: var(--site-color-ink-soft); }
    .button { display: inline-block; min-block-size: 2.75rem; padding: .75rem 1rem; border-radius: var(--site-radius-sm); text-decoration: none; } .primary { background: var(--site-color-brand); color: white; font-weight: 800; }
  `],
})
export class PageShellComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly metadata = inject(MetadataService);
  readonly title = String(this.route.snapshot.data['title'] ?? 'AutoVision');
  readonly description = String(this.route.snapshot.data['description'] ?? '');
  readonly notFound = Boolean(this.route.snapshot.data['notFound']);
  readonly eyebrow = this.notFound ? '404' : 'AutoVision by VERSPEN';

  constructor() { this.metadata.update(this.title, this.description); }
}