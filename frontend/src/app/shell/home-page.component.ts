import { ChangeDetectionStrategy, Component } from '@angular/core';
import { AvFeedbackComponent } from '../shared/feedback/av-feedback.component';

@Component({
  selector: 'app-home-page',
  imports: [AvFeedbackComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="welcome" aria-labelledby="welcome-title">
      <p class="eyebrow">Platform foundation</p>
      <h1 id="welcome-title">Operational clarity for every service team.</h1>
      <p class="lede">The AutoVision workspace is ready for authenticated, order-centric business modules.</p>
      <av-feedback title="Workspace ready" message="Service Order and Quote experiences will arrive as independently lazy-loaded features." kind="success" />
    </section>
  `,
  styles: [`
    .welcome { max-width: 52rem; padding-block: clamp(2rem, 8vw, 6rem); }
    .eyebrow { margin: 0 0 .75rem; color: var(--av-color-brand); font-size: .8125rem; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
    h1 { max-width: 13ch; margin: 0; color: var(--av-color-ink); font-size: clamp(2.25rem, 5vw, 4.5rem); line-height: 1.02; }
    .lede { max-width: 42rem; margin: 1.5rem 0 2rem; color: var(--av-color-muted); font-size: 1.125rem; line-height: 1.6; }
  `],
})
export class HomePageComponent {}