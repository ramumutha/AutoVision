import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { CustomerAuthorizationApiService } from './customer-authorization-api.service';
import { CustomerAuthorization, RequestQuoteCustomerAuthorizationRequest } from './customer-authorization.models';

@Component({
  selector: 'app-service-quote-authorization-request',
  imports: [ReactiveFormsModule, AvFeedbackComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="request-panel" aria-labelledby="authorization-request-title">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">{{ localization.text('customerAuthorization') }}</p>
          <h3 id="authorization-request-title">{{ localization.text('requestCustomerAuthorization') }}</h3>
        </div>
        <button class="close" type="button" (click)="cancel.emit()" [disabled]="submitting()" [attr.aria-label]="localization.text('cancel')">×</button>
      </div>
      @if (error(); as error) { <av-feedback kind="error" [title]="errorTitle(error)" [message]="errorMessage(error)" /> }
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="fields">
          <label>
            <span>{{ localization.text('authorizationNumber') }}</span>
            <input formControlName="authorizationNumber" maxlength="80" autocomplete="off" [attr.aria-invalid]="showError('authorizationNumber')" />
            @if (showError('authorizationNumber')) { <small>{{ fieldError('authorizationNumber') }}</small> }
          </label>
          <label>
            <span>{{ localization.text('customerReference') }}</span>
            <input formControlName="customerReference" maxlength="160" autocomplete="off" [attr.aria-invalid]="showError('customerReference')" />
            @if (showError('customerReference')) { <small>{{ fieldError('customerReference') }}</small> }
          </label>
          <label>
            <span>{{ localization.text('customerDisplayName') }}</span>
            <input formControlName="customerDisplayNameSnapshot" maxlength="200" autocomplete="name" [attr.aria-invalid]="showError('customerDisplayNameSnapshot')" />
            @if (showError('customerDisplayNameSnapshot')) { <small>{{ fieldError('customerDisplayNameSnapshot') }}</small> }
          </label>
          <label class="wide">
            <span>{{ localization.text('authorizationSummary') }}</span>
            <textarea rows="4" formControlName="authorizationSummary" maxlength="500" [attr.aria-invalid]="showError('authorizationSummary')"></textarea>
            @if (showError('authorizationSummary')) { <small>{{ fieldError('authorizationSummary') }}</small> }
          </label>
        </div>
        <div class="actions">
          <button class="secondary" type="button" (click)="cancel.emit()" [disabled]="submitting()">{{ localization.text('cancel') }}</button>
          <button class="primary" type="submit" [disabled]="form.invalid || submitting()">{{ submitting() ? localization.text('requestingAuthorization') : localization.text('requestAuthorization') }}</button>
        </div>
      </form>
    </section>
  `,
  styles: [`
    .request-panel { margin: 1.25rem 0; padding: 1.25rem; border: 1px solid var(--av-color-brand); border-radius: var(--av-radius-md); background: var(--av-color-surface); box-shadow: var(--av-shadow-sm); }
    .panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 1rem; }.eyebrow { margin: 0 0 .3rem; color: var(--av-color-brand); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }h3 { margin: 0; color: var(--av-color-ink); font-size: 1.25rem; }.close { min-inline-size: 2.75rem; min-block-size: 2.75rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: transparent; color: var(--av-color-ink); cursor: pointer; font-size: 1.5rem; line-height: 1; }
    .fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 1rem; }label { display: grid; gap: .4rem; color: var(--av-color-ink); font-weight: 700; }label.wide { grid-column: 1 / -1; }input, textarea { width: 100%; box-sizing: border-box; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); padding: .7rem .75rem; font: inherit; }small { color: var(--av-color-danger); font-weight: 600; }
    .actions { display: flex; justify-content: flex-end; gap: .75rem; margin-top: 1.25rem; }.actions button { min-block-size: 2.75rem; padding: .65rem 1rem; border-radius: var(--av-radius-sm); cursor: pointer; font-weight: 800; }.primary { border: 0; background: var(--av-color-brand); color: white; }.secondary { border: 1px solid var(--av-color-border); background: var(--av-color-surface); color: var(--av-color-ink); }button:disabled { cursor: wait; opacity: .6; }
    @media (max-width: 560px) { .fields { grid-template-columns: 1fr; }label.wide { grid-column: auto; }.actions { flex-direction: column-reverse; }.actions button { width: 100%; } }
  `],
})
export class ServiceQuoteAuthorizationRequestComponent {
  readonly caseId = input.required<string>();
  readonly quoteId = input.required<string>();
  readonly requested = output<CustomerAuthorization>();
  readonly cancel = output<void>();

  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly api = inject(CustomerAuthorizationApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly connectivity = inject(ConnectivityService);
  protected readonly localization = inject(LocalizationService);
  protected readonly submitting = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  readonly form = this.formBuilder.group({
    authorizationNumber: ['', [Validators.required, Validators.maxLength(80)]],
    customerReference: ['', Validators.maxLength(160)],
    customerDisplayNameSnapshot: ['', Validators.maxLength(200)],
    authorizationSummary: ['', [Validators.required, Validators.maxLength(500)]],
  });

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.submitting()) return;
    if (!this.connectivity.isOnline()) {
      this.error.set({ status: 0, message: this.localization.text('authorizationOffline') });
      return;
    }

    this.submitting.set(true);
    this.error.set(null);
    const value = this.form.getRawValue();
    const request: RequestQuoteCustomerAuthorizationRequest = {
      authorizationNumber: value.authorizationNumber.trim(),
      customerReference: value.customerReference.trim() || null,
      customerDisplayNameSnapshot: value.customerDisplayNameSnapshot.trim() || null,
      authorizationSummary: value.authorizationSummary.trim(),
    };

    this.api.requestFromServiceQuote(this.caseId(), this.quoteId(), request)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (authorization) => {
          this.submitting.set(false);
          this.form.reset();
          this.requested.emit(authorization);
        },
        error: (error: ApiError) => {
          this.submitting.set(false);
          this.error.set(error);
        },
      });
  }

  protected showError(field: keyof typeof this.form.controls): boolean {
    const control = this.form.controls[field];
    return control.invalid && control.touched;
  }

  protected fieldError(field: keyof typeof this.form.controls): string {
    return this.form.controls[field].hasError('maxlength')
      ? this.localization.text('fieldTooLong')
      : this.localization.text('requiredField');
  }

  protected errorTitle(error: ApiError): string {
    if (error.status === 403) return this.localization.text('notAuthorized');
    if (error.status === 404) return this.localization.text('authorizationContextUnavailable');
    return this.localization.text('authorizationRequestError');
  }

  protected errorMessage(error: ApiError): string {
    if (error.status === 0 || !this.connectivity.isOnline()) return this.localization.text('authorizationOffline');
    if (error.status === 409) return error.message || this.localization.text('authorizationConflict');
    if (error.status >= 500) return this.localization.text('tryAgain');
    return error.message || this.localization.text('tryAgain');
  }
}
