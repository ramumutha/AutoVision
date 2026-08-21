import { ChangeDetectionStrategy, Component, DestroyRef, effect, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { ServiceJobSummary } from './service-order.models';
import { OperationalAuthorizationApiService } from './operational-authorization-api.service';
import { OperationalAuthorizationEvaluation, OperationalAuthorizationStatus } from './operational-authorization.models';
import { ServiceJobAuthorizationReadinessApiService } from './service-job-authorization-readiness-api.service';
import { ServiceJobAuthorizationReadiness, ServiceJobAuthorizationReadinessReason } from './service-job-authorization-readiness.models';

type EvaluationState = {
  loading: boolean;
  evaluation: OperationalAuthorizationEvaluation | null;
  error: ApiError | null;
};

type ReadinessState = {
  loading: boolean;
  readiness: ServiceJobAuthorizationReadiness | null;
  error: ApiError | null;
};

@Component({
  selector: 'app-service-job-operational-authorization',
  imports: [AvStatusComponent, AvFeedbackComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section class="authorization-evaluation" aria-labelledby="authorization-evaluation-title">
      <div class="heading"><p class="eyebrow">{{ localization.text('serviceJobs') }}</p><h2 id="authorization-evaluation-title">{{ localization.text('operationalAuthorization') }}</h2></div>
      @if (jobs().length === 0) { <p class="empty">{{ localization.text('noServiceJobs') }}</p> }
      @else { <div class="job-list">@for (job of jobs(); track job.id) { @let state = stateFor(job.id); @let readiness = readinessStateFor(job.id); <article class="job-card"><div class="job-heading"><div><h3>{{ job.jobNumber }}</h3><p>{{ job.summary }}</p></div><div class="job-statuses">@if (state.loading) { <span class="loading" role="status">{{ localization.text('loadingAuthorizationEvaluation') }}</span> } @else if (state.evaluation; as evaluation) { <av-status [label]="statusLabel(evaluation.status)" [tone]="statusTone(evaluation.status)" /> } @if (readiness.loading) { <span class="loading" role="status">{{ localization.text('loadingAuthorizationReadiness') }}</span> } @else if (readiness.readiness; as result) { <av-status [label]="result.ready ? localization.text('ready') : localization.text('blocked')" [tone]="result.ready ? 'success' : 'danger'" /> }</div></div><div class="evaluation-state">@if (state.loading) { <p class="state-message">{{ localization.text('loadingAuthorizationEvaluation') }}</p> } @else if (state.error; as error) { <av-feedback kind="error" [title]="errorTitle(error)" [message]="errorMessage(error)" /><button class="retry evaluation-retry" type="button" (click)="retry(job.id)">{{ localization.text('retry') }}</button> } @else if (state.evaluation; as evaluation) { @if (evaluation.status === 'NOT_REQUIRED') { <p class="state-message">{{ localization.text('authorizationNotRequired') }} <span>{{ localization.text('totalJobLines') }}: {{ evaluation.totalLineCount }}</span></p> } @else { <dl class="counts"><div><dt>{{ localization.text('totalJobLines') }}</dt><dd>{{ evaluation.totalLineCount }}</dd></div><div><dt>{{ localization.text('authorizedLines') }}</dt><dd>{{ evaluation.authorizedLineCount }}</dd></div><div><dt>{{ localization.text('pendingLines') }}</dt><dd>{{ evaluation.pendingLineCount }}</dd></div><div><dt>{{ localization.text('notAuthorizedLines') }}</dt><dd>{{ evaluation.notAuthorizedLineCount }}</dd></div></dl> } }</div><div class="readiness-state">@if (readiness.loading) { <p class="state-message">{{ localization.text('loadingAuthorizationReadiness') }}</p> } @else if (readiness.error; as error) { <av-feedback kind="error" [title]="readinessErrorTitle(error)" [message]="readinessErrorMessage(error)" /><button class="retry readiness-retry" type="button" (click)="retryReadiness(job.id)">{{ localization.text('retry') }}</button> } @else if (readiness.readiness; as result) { <p class="reason">{{ readinessReasonLabel(result.reason) }}</p> } </div></article> }</div> }
    </section>
  `,
  styles: [`
    :host { display: block; }.authorization-evaluation { margin-top: 1.25rem; padding: 1.25rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); }.heading { margin-bottom: 1rem; }.eyebrow { margin: 0 0 .3rem; color: var(--av-color-brand); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }h2, h3 { margin: 0; color: var(--av-color-ink); }h2 { font-size: 1.35rem; }h3 { font-size: 1rem; }.job-list { display: grid; gap: .75rem; }.job-card { padding: 1rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-canvas); }.job-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; }.job-heading p { margin: .35rem 0 0; color: var(--av-color-muted); }.job-statuses { display: flex; flex-wrap: wrap; justify-content: flex-end; gap: .5rem; }.loading, .state-message, .empty, .reason { color: var(--av-color-muted); }.state-message { margin: .9rem 0 0; }.state-message span { margin-left: .75rem; }.counts { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: .6rem; margin: 1rem 0 0; }.counts div { padding: .65rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); }.counts dt { color: var(--av-color-muted); font-size: .72rem; font-weight: 700; }.counts dd { margin: .3rem 0 0; color: var(--av-color-ink); font-weight: 800; }.retry { margin-top: .75rem; min-block-size: 2.5rem; padding: .55rem .9rem; border: 0; border-radius: var(--av-radius-sm); background: var(--av-color-brand); color: white; cursor: pointer; font-weight: 800; }.readiness-state { margin-top: .75rem; padding-top: .75rem; border-top: 1px solid var(--av-color-border); }.reason { margin: .5rem 0 0; }.evaluation-retry, .readiness-retry { margin-right: .5rem; }@media (max-width: 700px) { .counts { grid-template-columns: repeat(2, minmax(0, 1fr)); } }@media (max-width: 480px) { .job-heading { flex-direction: column; }.job-statuses { justify-content: flex-start; } }
  `],
})
export class ServiceJobOperationalAuthorizationComponent {
  readonly orderId = input.required<string>();
  readonly jobs = input<ServiceJobSummary[]>([]);
  protected readonly localization = inject(LocalizationService);
  protected readonly connectivity = inject(ConnectivityService);
  private readonly api = inject(OperationalAuthorizationApiService);
  private readonly readinessApi = inject(ServiceJobAuthorizationReadinessApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly states = signal<Record<string, EvaluationState>>({});
  private readonly readinessStates = signal<Record<string, ReadinessState>>({});

  constructor() {
    effect(() => {
      const orderId = this.orderId();
      const jobs = this.jobs();
      this.states.set(Object.fromEntries(jobs.map((job) => [job.id, { loading: true, evaluation: null, error: null }])));
      this.readinessStates.set(Object.fromEntries(jobs.map((job) => [job.id, { loading: true, readiness: null, error: null }])));
      jobs.forEach((job) => { this.load(orderId, job.id); this.loadReadiness(orderId, job.id); });
    });
  }

  protected stateFor(jobId: string): EvaluationState { return this.states()[jobId] ?? { loading: true, evaluation: null, error: null }; }
  protected retry(jobId: string): void { this.load(this.orderId(), jobId); }
  protected readinessStateFor(jobId: string): ReadinessState { return this.readinessStates()[jobId] ?? { loading: true, readiness: null, error: null }; }
  protected retryReadiness(jobId: string): void { this.loadReadiness(this.orderId(), jobId); }
  private load(orderId: string, jobId: string): void {
    this.updateState(jobId, { loading: true, evaluation: null, error: null });
    this.api.evaluate(orderId, jobId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (evaluation) => this.updateState(jobId, { loading: false, evaluation, error: null }),
      error: (error: ApiError) => this.updateState(jobId, { loading: false, evaluation: null, error }),
    });
  }
  private updateState(jobId: string, state: EvaluationState): void { this.states.update((states) => ({ ...states, [jobId]: state })); }
  private loadReadiness(orderId: string, jobId: string): void {
    this.updateReadinessState(jobId, { loading: true, readiness: null, error: null });
    this.readinessApi.evaluate(orderId, jobId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (readiness) => this.updateReadinessState(jobId, { loading: false, readiness, error: null }),
      error: (error: ApiError) => this.updateReadinessState(jobId, { loading: false, readiness: null, error }),
    });
  }
  private updateReadinessState(jobId: string, state: ReadinessState): void { this.readinessStates.update((states) => ({ ...states, [jobId]: state })); }
  protected statusLabel(status: OperationalAuthorizationStatus): string { return status.replaceAll('_', ' '); }
  protected statusTone(status: OperationalAuthorizationStatus): AvStatusTone { const tones: Record<OperationalAuthorizationStatus, AvStatusTone> = { FULLY_AUTHORIZED: 'success', PARTIALLY_AUTHORIZED: 'warning', PENDING: 'warning', NOT_AUTHORIZED: 'danger', NOT_REQUIRED: 'neutral' }; return tones[status]; }
  protected errorTitle(error: ApiError): string { return error.status === 403 ? this.localization.text('notAuthorized') : this.localization.text('unableToLoadAuthorizationEvaluation'); }
  protected errorMessage(error: ApiError): string { if (error.status === 404) return this.localization.text('authorizationEvaluationUnavailable'); if (!this.connectivity.isOnline() || error.status === 0) return this.localization.text('offlineRetry'); return error.status >= 500 ? this.localization.text('tryAgain') : error.message; }
  protected readinessReasonLabel(reason: ServiceJobAuthorizationReadinessReason): string { const labels: Record<ServiceJobAuthorizationReadinessReason, string> = { AUTHORIZATION_NOT_REQUIRED: this.localization.text('authorizationReadinessNotRequired'), FULLY_AUTHORIZED: this.localization.text('authorizationReadinessFullyAuthorized'), PARTIAL_AUTHORIZATION: this.localization.text('authorizationReadinessPartial'), AUTHORIZATION_PENDING: this.localization.text('authorizationReadinessPending'), AUTHORIZATION_MISSING: this.localization.text('authorizationReadinessMissing') }; return labels[reason]; }
  protected readinessErrorTitle(error: ApiError): string { return error.status === 403 ? this.localization.text('notAuthorized') : this.localization.text('unableToLoadAuthorizationReadiness'); }
  protected readinessErrorMessage(error: ApiError): string { if (error.status === 404) return this.localization.text('authorizationReadinessUnavailable'); if (!this.connectivity.isOnline() || error.status === 0) return this.localization.text('offlineRetry'); return error.status >= 500 ? this.localization.text('tryAgain') : error.message; }
}
