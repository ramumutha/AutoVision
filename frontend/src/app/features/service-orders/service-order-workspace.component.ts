import { AsyncPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink, RouterLinkActive } from '@angular/router';
import { catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { AvStatusComponent } from '../../shared/design-system/av-status.component';
import { ServiceOrderApiService } from './service-order-api.service';
import { ServiceOrderOverviewComponent } from './service-order-overview.component';
import { ServiceOrderSection } from './service-order.models';

@Component({
  selector: 'app-service-order-workspace',
  imports: [AsyncPipe, RouterLink, RouterLinkActive, AvFeedbackComponent, AvStatusComponent, ServiceOrderOverviewComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (state$ | async; as state) {
      @if (state.loading) {
        <div class="loading" role="status" aria-live="polite">{{ localization.text('loadingServiceOrder') }}</div>
      } @else if (state.error; as error) {
        <av-feedback kind="error" [title]="errorTitle(error)" [message]="errorMessage(error)" />
        <button class="retry" type="button" (click)="retry()">{{ localization.text('retry') }}</button>
      } @else if (state.aggregate; as aggregate) {
        <header class="workspace-header">
          <div class="title-block">
            <p class="eyebrow">{{ localization.text('serviceOrder') }}</p>
            <h1>{{ aggregate.order.orderNumber }}</h1>
            <p class="context">{{ localization.text('vehicle') }} <span class="value-mono">{{ aggregate.order.vehicleId }}</span></p>
          </div>
          <div class="header-state"><av-status [label]="aggregate.order.status.replaceAll('_', ' ')" tone="info" /></div>
        </header>
        <nav class="sections" [attr.aria-label]="localization.text('workspaceSections')">
          <a routerLink="." [queryParams]="{ section: 'overview' }" queryParamsHandling="merge" routerLinkActive="active" [routerLinkActiveOptions]="{ queryParams: 'subset' }">{{ localization.text('overview') }}</a>
          <a routerLink="." [queryParams]="{ section: 'lines' }" queryParamsHandling="merge" routerLinkActive="active">{{ localization.text('serviceLines') }}</a>
          <a routerLink="." [queryParams]="{ section: 'quotes' }" queryParamsHandling="merge" routerLinkActive="active">{{ localization.text('quotes') }}</a>
        </nav>
        @if (section() === 'overview') {
          <app-service-order-overview [order]="aggregate.order" />
        } @else {
          <av-feedback [title]="sectionTitle()" [message]="localization.text('nextDelivery')" />
        }
      }
    }
  `,
  styles: [`
    :host { display: block; padding-block: 1.5rem 3rem; }
    .workspace-header { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; padding: 1.5rem 0; }
    .eyebrow { margin: 0 0 .35rem; color: var(--av-color-brand); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }
    h1 { margin: 0; color: var(--av-color-ink); font-size: clamp(1.8rem, 4vw, 3rem); line-height: 1.1; }
    .context { margin: .65rem 0 0; color: var(--av-color-muted); }
    .value-mono { font-family: ui-monospace, SFMono-Regular, Consolas, monospace; }
    .sections { display: flex; gap: .25rem; overflow-x: auto; margin-bottom: 1.25rem; border-bottom: 1px solid var(--av-color-border); }
    .sections a { flex: 0 0 auto; min-block-size: 3rem; padding: .9rem 1rem .75rem; border-bottom: 3px solid transparent; color: var(--av-color-muted); font-weight: 800; text-decoration: none; white-space: nowrap; }
    .sections a:hover, .sections a.active { border-bottom-color: var(--av-color-brand); color: var(--av-color-brand-strong); }
    .loading { padding: 2rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); color: var(--av-color-muted); }
    .retry { margin-top: 1rem; min-block-size: 2.75rem; padding: .6rem 1rem; border: 0; border-radius: var(--av-radius-sm); background: var(--av-color-brand); color: white; cursor: pointer; font-weight: 800; }
    @media (max-width: 520px) { .workspace-header { flex-direction: column; } }
  `],
})
export class ServiceOrderWorkspaceComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ServiceOrderApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly connectivity = inject(ConnectivityService);
  protected readonly localization = inject(LocalizationService);
  protected readonly section = signal<ServiceOrderSection>('overview');
  protected readonly retryVersion = signal(0);

  readonly state$ = combineLatest([
    this.route.paramMap,
    this.route.queryParamMap,
    toObservable(this.retryVersion),
  ]).pipe(
    switchMap(([params, query]) => {
      const orderId = params.get('orderId');
      if (!orderId) return of({ loading: false, aggregate: null, error: { status: 404, message: 'Service Order not found.' } as ApiError });
      const section = query.get('section') as ServiceOrderSection | null;
      this.section.set(section === 'lines' || section === 'quotes' ? section : 'overview');
      return this.api.getServiceOrder(orderId).pipe(
        map((aggregate) => ({ loading: false, aggregate, error: null })),
        startWith({ loading: true, aggregate: null, error: null }),
        catchError((error: ApiError) => of({ loading: false, aggregate: null, error })),
      );
    }),
    takeUntilDestroyed(this.destroyRef),
  );

  retry(): void { this.retryVersion.update((value) => value + 1); }
  protected sectionTitle(): string { return this.section() === 'lines' ? this.localization.text('serviceLines') : this.localization.text('quotes'); }
  protected errorTitle(error: ApiError): string { return error.status === 404 ? this.localization.text('notFound') : this.localization.text('unableToLoad'); }
  protected errorMessage(error: ApiError): string {
    if (error.status === 403) return this.localization.text('notAuthorized');
    if (!this.connectivity.isOnline()) return this.localization.text('offlineRetry');
    return error.status >= 500 || error.status === 0 ? this.localization.text('tryAgain') : error.message;
  }
}