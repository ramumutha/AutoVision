import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type AvStatusTone = 'neutral' | 'success' | 'warning' | 'danger' | 'info';

@Component({
  selector: 'av-status',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: '<span class="status" [class]="tone()" role="status">{{ label() }}</span>',
  styles: [`
    .status { display: inline-flex; align-items: center; min-block-size: 1.75rem; padding: 0 .625rem; border: 1px solid currentColor; border-radius: .25rem; font-size: .8125rem; font-weight: 700; }
    .neutral { color: var(--av-color-muted); background: #eef2f4; }
    .success { color: var(--av-color-success); background: #e7f5ed; }
    .warning { color: var(--av-color-warning); background: #fff4dd; }
    .danger { color: var(--av-color-danger); background: #fde9e7; }
    .info { color: var(--av-color-info); background: #e6f2fb; }
  `],
})
export class AvStatusComponent {
  readonly label = input.required<string>();
  readonly tone = input<AvStatusTone>('neutral');
}