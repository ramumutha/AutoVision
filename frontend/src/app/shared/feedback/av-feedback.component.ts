import { ChangeDetectionStrategy, Component, input } from '@angular/core';

@Component({
  selector: 'av-feedback',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<section class="feedback" [class]="kind()" role="status"><strong>{{ title() }}</strong><p>{{ message() }}</p></section>',
  styles: [`
    .feedback { padding: 1rem 1.25rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); }
    p { margin: .25rem 0 0; color: var(--av-color-muted); }
    .error { border-color: #e9b7b1; background: #fff5f4; }
    .success { border-color: #abd9bd; background: #f0fbf4; }
  `],
})
export class AvFeedbackComponent {
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly kind = input<'info' | 'error' | 'success'>('info');
}