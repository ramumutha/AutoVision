import { ChangeDetectionStrategy, Component, DestroyRef, EventEmitter, Output, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { CreateServiceQuoteRequest, ServiceQuoteSummary } from './service-quote.models';
import { ServiceQuoteApiService } from './service-quote-api.service';

export function toEndOfLocalDayOffsetDateTime(date: string): string | null {
  if (!date) return null;

  const [year, month, day] = date.split('-').map(Number);
  const localEndOfDay = new Date(year, month - 1, day, 23, 59, 59, 0);
  const offsetMinutes = -localEndOfDay.getTimezoneOffset();
  const sign = offsetMinutes >= 0 ? '+' : '-';
  const absoluteOffset = Math.abs(offsetMinutes);
  const offsetHours = String(Math.floor(absoluteOffset / 60)).padStart(2, '0');
  const offsetRemainder = String(absoluteOffset % 60).padStart(2, '0');

  // Interim representation: the date is interpreted in the browser-local business context.
  return `${date}T23:59:59${sign}${offsetHours}:${offsetRemainder}`;
}

@Component({
  selector: 'app-service-quote-create',
  imports: [ReactiveFormsModule, AvFeedbackComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="create-panel" aria-labelledby="create-quote-title">
      <div class="panel-heading"><div><p class="eyebrow">{{ localization.text('quotes') }}</p><h3 id="create-quote-title">{{ localization.text('createQuote') }}</h3></div><button class="close" type="button" (click)="cancel.emit()" [attr.aria-label]="localization.text('cancel')">×</button></div>
      <p class="helper">{{ localization.text('quoteAutomaticLines') }}</p>
      @if (error(); as error) { <av-feedback kind="error" [title]="errorTitle(error)" [message]="errorMessage(error)" /> }
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="fields">
          <label><span>{{ localization.text('quoteNumber') }}</span><input formControlName="quoteNumber" autocomplete="off" [attr.aria-invalid]="showError('quoteNumber')" [attr.aria-describedby]="showError('quoteNumber') ? 'quote-number-error' : null" />@if (showError('quoteNumber')) { <small id="quote-number-error">{{ localization.text('requiredField') }}</small> }</label>
          <label><span>{{ localization.text('currency') }}</span><input formControlName="currencyCode" maxlength="3" autocomplete="off" [attr.aria-invalid]="showError('currencyCode')" [attr.aria-describedby]="showError('currencyCode') ? 'currency-error' : null" />@if (showError('currencyCode')) { <small id="currency-error">{{ localization.text('requiredField') }}</small> }</label>
          <label><span>{{ localization.text('validUntil') }}</span><input type="date" formControlName="validUntil" /></label>
          <label class="wide"><span>{{ localization.text('terms') }}</span><textarea rows="3" formControlName="termsSnapshot"></textarea></label>
          <label class="wide"><span>{{ localization.text('disclaimer') }}</span><textarea rows="3" formControlName="disclaimerSnapshot"></textarea></label>
        </div>
        <div class="actions"><button class="secondary" type="button" (click)="cancel.emit()" [disabled]="submitting()">{{ localization.text('cancel') }}</button><button class="primary" type="submit" [disabled]="submitting()">{{ submitting() ? localization.text('creatingQuote') : localization.text('create') }}</button></div>
      </form>
    </section>
  `,
  styles: [`
    .create-panel { margin-bottom: 1.25rem; padding: 1.25rem; border: 1px solid var(--av-color-brand); border-radius: var(--av-radius-md); background: var(--av-color-surface); box-shadow: var(--av-shadow-sm); }
    .panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; } .eyebrow { margin: 0 0 .3rem; color: var(--av-color-brand); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; } h3 { margin: 0; color: var(--av-color-ink); font-size: 1.25rem; } .helper { margin: .75rem 0 1.25rem; color: var(--av-color-muted); }
    .close { min-inline-size: 2.75rem; min-block-size: 2.75rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: transparent; color: var(--av-color-ink); cursor: pointer; font-size: 1.5rem; line-height: 1; }
    .fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; } label { display: grid; gap: .4rem; color: var(--av-color-ink); font-weight: 700; } label.wide { grid-column: 1 / -1; } input, textarea { width: 100%; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); padding: .7rem .75rem; } small { color: var(--av-color-danger); font-weight: 600; }
    .actions { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.25rem; } .actions button { min-block-size: 2.75rem; padding: .65rem 1rem; border-radius: var(--av-radius-sm); cursor: pointer; font-weight: 800; } .primary { border: 0; background: var(--av-color-brand); color: white; } .secondary { border: 1px solid var(--av-color-border); background: var(--av-color-surface); color: var(--av-color-ink); } button:disabled { cursor: wait; opacity: .6; }
    @media (max-width: 560px) { .fields { grid-template-columns: 1fr; } label.wide { grid-column: auto; } .actions { flex-direction: column-reverse; } .actions button { width: 100%; } }
  `],
})
export class ServiceQuoteCreateComponent {
  @Output() readonly cancel = new EventEmitter<void>();
  @Output() readonly created = new EventEmitter<ServiceQuoteSummary>();
  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly api = inject(ServiceQuoteApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly localization = inject(LocalizationService);
  protected readonly connectivity = inject(ConnectivityService);
  protected readonly submitting = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  readonly form = this.formBuilder.group({
    quoteNumber: ['', Validators.required],
    currencyCode: ['', Validators.required],
    validUntil: [''], termsSnapshot: [''], disclaimerSnapshot: [''],
  });

  readonly orderId = input.required<string>();

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.submitting()) return;
    if (!this.connectivity.isOnline()) { this.error.set({ status: 0, message: this.localization.text('offlineCreate') }); return; }
    this.submitting.set(true); this.error.set(null);
    const value = this.form.getRawValue();
    const request: CreateServiceQuoteRequest = {
      quoteNumber: value.quoteNumber.trim(), currencyCode: value.currencyCode.trim().toUpperCase(),
      validUntil: toEndOfLocalDayOffsetDateTime(value.validUntil),
      termsSnapshot: value.termsSnapshot.trim() || null,
      disclaimerSnapshot: value.disclaimerSnapshot.trim() || null,
    };
    this.api.createQuote(this.orderId(), request).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (quote) => { this.submitting.set(false); this.form.reset(); this.created.emit(quote); },
      error: (error: ApiError) => { this.submitting.set(false); this.error.set(error); },
    });
  }

  protected showError(field: 'quoteNumber' | 'currencyCode'): boolean { const control = this.form.controls[field]; return control.invalid && control.touched; }
  protected errorTitle(error: ApiError): string { return error.status === 403 ? this.localization.text('notAuthorized') : this.localization.text('quoteCreateError'); }
  protected errorMessage(error: ApiError): string { if (error.status === 0) return this.localization.text('offlineCreate'); if (error.status === 409) return error.message || this.localization.text('quoteConflict'); return error.status >= 500 ? this.localization.text('tryAgain') : error.message; }
}