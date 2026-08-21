import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { CustomerAuthorizationApiService } from './customer-authorization-api.service';
import { CustomerAuthorization, CustomerAuthorizationDecisionRequest } from './customer-authorization.models';

export type CustomerAuthorizationDecisionAction = 'authorize' | 'decline' | 'defer' | 'cancel';

@Component({
  selector: 'app-service-quote-authorization-decision',
  imports: [ReactiveFormsModule, AvFeedbackComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="decision-panel" aria-labelledby="decision-title">
      <div class="panel-heading">
        <div><p class="eyebrow">{{ localization.text('customerAuthorization') }}</p><h4 id="decision-title">{{ title() }}</h4></div>
        <button class="close" type="button" (click)="cancel.emit()" [disabled]="submitting()" [attr.aria-label]="localization.text('cancel')">×</button>
      </div>
      @if (error(); as error) { <av-feedback kind="error" [title]="errorTitle(error)" [message]="errorMessage(error)" /> }
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <div class="fields">
          <label><span>{{ localization.text('decisionChannel') }}</span><input formControlName="decisionChannel" maxlength="32" autocomplete="off" /></label>
          <label><span>{{ localization.text('decisionReference') }}</span><input formControlName="decisionReference" maxlength="160" autocomplete="off" /></label>
        </div>
        <div class="actions"><button class="secondary" type="button" (click)="cancel.emit()" [disabled]="submitting()">{{ localization.text('cancel') }}</button><button class="primary" type="submit" [disabled]="submitting()">{{ submitting() ? localization.text('recordingDecision') : submitLabel() }}</button></div>
      </form>
    </section>
  `,
  styles: [`
    .decision-panel { margin-top: .9rem; padding: 1rem; border: 1px solid var(--av-color-brand); border-radius: var(--av-radius-sm); background: var(--av-color-surface); }.panel-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: .9rem; }.eyebrow { margin: 0 0 .25rem; color: var(--av-color-brand); font-size: .7rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }h4 { margin: 0; color: var(--av-color-ink); }.close { min-inline-size: 2.5rem; min-block-size: 2.5rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: transparent; color: var(--av-color-ink); cursor: pointer; font-size: 1.35rem; }.fields { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: .75rem; }label { display: grid; gap: .35rem; color: var(--av-color-ink); font-weight: 700; }input { width: 100%; box-sizing: border-box; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); padding: .65rem .7rem; font: inherit; }.actions { display: flex; justify-content: flex-end; gap: .65rem; margin-top: 1rem; }.actions button { min-block-size: 2.5rem; padding: .6rem .9rem; border-radius: var(--av-radius-sm); cursor: pointer; font-weight: 800; }.primary { border: 0; background: var(--av-color-brand); color: white; }.secondary { border: 1px solid var(--av-color-border); background: var(--av-color-surface); color: var(--av-color-ink); }button:disabled { cursor: wait; opacity: .6; }@media (max-width: 560px) { .fields { grid-template-columns: 1fr; }.actions { flex-direction: column-reverse; }.actions button { width: 100%; } }
  `],
})
export class ServiceQuoteAuthorizationDecisionComponent {
  readonly caseId = input.required<string>();
  readonly authorization = input.required<CustomerAuthorization>();
  readonly action = input.required<CustomerAuthorizationDecisionAction>();
  readonly decided = output<CustomerAuthorization>();
  readonly cancel = output<void>();

  private readonly formBuilder = inject(NonNullableFormBuilder);
  private readonly api = inject(CustomerAuthorizationApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly connectivity = inject(ConnectivityService);
  protected readonly localization = inject(LocalizationService);
  protected readonly submitting = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  readonly form = this.formBuilder.group({
    decisionChannel: [''],
    decisionReference: [''],
  });

  protected title(): string {
    return this.action() === 'authorize'
      ? this.localization.text('recordCustomerAuthorization')
      : this.action() === 'decline'
        ? this.localization.text('recordCustomerDecline')
        : this.action() === 'defer'
          ? this.localization.text('recordCustomerDeferral')
          : this.localization.text('withdrawAuthorizationRequest');
  }

  protected submitLabel(): string { return this.title(); }

  submit(): void {
    if (this.submitting()) return;
    if (!this.connectivity.isOnline()) { this.error.set({ status: 0, message: this.localization.text('authorizationDecisionOffline') }); return; }
    this.submitting.set(true);
    this.error.set(null);
    const values = this.form.getRawValue();
    const request: CustomerAuthorizationDecisionRequest = {
      decisionChannel: values.decisionChannel.trim() || null,
      decisionReference: values.decisionReference.trim() || null,
    };
    const result = this.action() === 'authorize'
      ? this.api.authorize(this.caseId(), this.authorization().id, request)
      : this.action() === 'decline'
        ? this.api.decline(this.caseId(), this.authorization().id, request)
        : this.action() === 'defer'
          ? this.api.defer(this.caseId(), this.authorization().id, request)
          : this.api.cancel(this.caseId(), this.authorization().id, request);
    result.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (authorization) => { this.submitting.set(false); this.decided.emit(authorization); },
      error: (error: ApiError) => { this.submitting.set(false); this.error.set(error); },
    });
  }

  protected errorTitle(error: ApiError): string {
    if (error.status === 403) return this.localization.text('notAuthorized');
    if (error.status === 404) return this.localization.text('authorizationDecisionContextUnavailable');
    return this.localization.text('authorizationDecisionError');
  }

  protected errorMessage(error: ApiError): string {
    if (error.status === 0 || !this.connectivity.isOnline()) return this.localization.text('authorizationDecisionOffline');
    if (error.status === 409) return error.message || this.localization.text('authorizationDecisionConflict');
    if (error.status >= 500) return this.localization.text('tryAgain');
    return error.message || this.localization.text('tryAgain');
  }
}
