import { Location } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Params, Router } from '@angular/router';
import { Subject, catchError, distinctUntilChanged, map, merge, of, shareReplay, switchMap, tap, withLatestFrom } from 'rxjs';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitOpportunityWorkspaceComponent } from './service-profit-opportunity-workspace.component';
import { ServiceProfitOpportunityResponse } from './service-profit.models';

@Component({
  selector: 'app-service-profit-opportunity-detail-page',
  imports: [AvFeedbackComponent, ServiceProfitOpportunityWorkspaceComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-header">
      <button class="back-action" type="button" (click)="back()"><span aria-hidden="true">←</span> {{ localization.text('backToOpportunities') }}</button>
    </header>

    @if (loading()) {
      <div class="loading" role="status" aria-live="polite">{{ localization.text('loadingOpportunity') }}</div>
    } @else if (error(); as detailError) {
      <av-feedback kind="error" [title]="errorTitle(detailError)" [message]="errorMessage(detailError)" />
      @if (detailError.status !== 401 && detailError.status !== 403 && detailError.status !== 404) {
        <button class="retry-action" type="button" (click)="retry()">{{ localization.text('retry') }}</button>
      }
    } @else if (opportunity(); as detail) {
      <app-service-profit-opportunity-workspace [opportunity]="detail" />
    }
  `,
  styles: [`
    :host { display: block; padding-block: 1.5rem 3rem; }
    .page-header { margin-bottom: 1rem; }
    .back-action { display: inline-flex; align-items: center; gap: .5rem; min-block-size: 2.75rem; padding: .5rem .75rem; border: 0; background: transparent; color: var(--av-color-brand-strong); cursor: pointer; font: inherit; font-weight: 800; }
    .loading { padding: 2rem; border: 1px solid var(--av-color-border); background: var(--av-color-surface); color: var(--av-color-muted); }
    .retry-action { min-block-size: 2.75rem; margin-top: 1rem; padding: .6rem 1rem; border: 1px solid var(--av-color-brand); border-radius: var(--av-radius-sm); background: var(--av-color-brand); color: white; cursor: pointer; font: inherit; font-weight: 800; }
    @media (max-width: 720px) { :host { padding-block-start: .75rem; } }
  `],
})
export class ServiceProfitOpportunityDetailPageComponent {
  private readonly api = inject(ServiceProfitApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly location = inject(Location);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly retryRequests = new Subject<void>();
  protected readonly localization = inject(LocalizationService);

  protected readonly loading = signal(true);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly opportunity = signal<ServiceProfitOpportunityResponse | null>(null);

  constructor() {
    const opportunityId$ = this.route.paramMap.pipe(
      map((params) => params.get('opportunityId') ?? ''),
      distinctUntilChanged(),
      shareReplay({ bufferSize: 1, refCount: true }),
    );

    merge(
      opportunityId$,
      this.retryRequests.pipe(withLatestFrom(opportunityId$), map(([, opportunityId]) => opportunityId)),
    ).pipe(
      tap(() => {
        this.loading.set(true);
        this.error.set(null);
        this.opportunity.set(null);
      }),
      switchMap((opportunityId) => this.api.getOpportunity(opportunityId).pipe(
        map((opportunity) => ({ opportunity, error: null })),
        catchError((error: ApiError) => of({ opportunity: null, error })),
      )),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(({ opportunity, error }) => {
      this.opportunity.set(opportunity);
      this.error.set(error);
      this.loading.set(false);
    });
  }

  protected retry(): void {
    this.retryRequests.next();
  }

  protected back(): void {
    if (typeof history !== 'undefined' && Number(history.state?.navigationId) > 1) {
      this.location.back();
      return;
    }
    void this.router.navigate(['/service-profit'], { queryParams: this.listQueryParams() });
  }

  protected errorTitle(error: ApiError): string {
    return error.status === 404
      ? this.localization.text('opportunityUnavailable')
      : this.localization.text('unableToLoadOpportunity');
  }

  protected errorMessage(error: ApiError): string {
    if (error.status === 401) return this.localization.text('detailSessionExpired');
    if (error.status === 403) return this.localization.text('detailNotAuthorized');
    if (error.status === 404) return this.localization.text('opportunityNotFound');
    return this.localization.text('tryAgain');
  }

  private listQueryParams(): Params {
    const params = this.route.snapshot.queryParamMap;
    return ['type', 'priority', 'actionability', 'ownership', 'handlingStatus', 'dueState', 'disposition', 'groupBy', 'sort'].reduce<Params>((result, key) => {
      const value = params.get(key);
      if (value) result[key] = value;
      return result;
    }, {});
  }
}
