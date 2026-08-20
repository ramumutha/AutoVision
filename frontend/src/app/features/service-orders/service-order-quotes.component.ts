import { AsyncPipe, DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { ServiceQuoteApiService } from './service-quote-api.service';
import { ServiceQuoteCreateComponent } from './service-quote-create.component';
import { ServiceQuoteDetailComponent } from './service-quote-detail.component';
import { ServiceQuoteStatus, ServiceQuoteSummary } from './service-quote.models';

interface QuoteListState {
  loading: boolean;
  quotes: ServiceQuoteSummary[];
  error: ApiError | null;
}

@Component({
  selector: 'app-service-order-quotes',
  imports: [AsyncPipe, DatePipe, AvFeedbackComponent, AvStatusComponent, ServiceQuoteCreateComponent, ServiceQuoteDetailComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (state$ | async; as state) {
      @if (state.loading) {
        <div class="loading" role="status" aria-live="polite">{{ localization.text('loadingQuotes') }}</div>
      } @else if (state.error; as error) {
        <av-feedback kind="error" [title]="errorTitle(error)" [message]="errorMessage(error)" />
        <button class="retry" type="button" (click)="retry()">{{ localization.text('retry') }}</button>
      } @else {
        <section aria-labelledby="quotes-title">
          <div class="list-heading">
            <div>
              <p class="eyebrow">{{ localization.text('serviceOrder') }}</p>
              <h2 id="quotes-title">{{ localization.text('quotes') }}</h2>
            </div>
            <div class="heading-actions"><span class="count">{{ state.quotes.length }}</span><button class="create-action" type="button" (click)="openCreate()">{{ localization.text('createQuote') }}</button></div>
          </div>
          @if (showCreate()) { <app-service-quote-create [orderId]="orderId" (cancel)="closeCreate()" (created)="handleCreated($event)" /> }
          @if (success()) { <av-feedback kind="success" [title]="localization.text('quoteCreated')" [message]="localization.text('quoteCreatedMessage')" /> }
          @if (state.quotes.length === 0) { <av-feedback [title]="localization.text('quotes')" [message]="localization.text('noQuotes')" /> }
          @else { <div class="quote-table-wrap">
            <table class="quote-table">
              <caption class="sr-only">{{ localization.text('quotes') }}</caption>
              <thead>
                <tr>
                  <th scope="col">{{ localization.text('quoteNumber') }}</th>
                  <th scope="col">{{ localization.text('status') }}</th>
                  <th scope="col">{{ localization.text('currency') }}</th>
                  <th scope="col">{{ localization.text('created') }}</th>
                  <th scope="col">{{ localization.text('validUntil') }}</th>
                </tr>
              </thead>
              <tbody>
                @for (quote of state.quotes; track quote.id) {
                  <tr>
                    <th scope="row"><button class="quote-open" type="button" [attr.aria-current]="selectedQuoteId() === quote.id ? 'page' : null" (click)="selectQuote(quote.id)">{{ quote.quoteNumber }}</button></th>
                    <td><av-status [label]="statusLabel(quote.status)" [tone]="statusTone(quote.status)" /></td>
                    <td>{{ quote.currencyCode }}</td>
                    <td>{{ quote.createdAt | date:'mediumDate' }}</td>
                    <td>{{ quote.validUntil ? (quote.validUntil | date:'mediumDate') : '—' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
          <ul class="quote-cards" aria-label="Quote summaries">
            @for (quote of state.quotes; track quote.id) {
              <li class="quote-card">
                <div class="card-heading"><button class="quote-open" type="button" [attr.aria-current]="selectedQuoteId() === quote.id ? 'page' : null" (click)="selectQuote(quote.id)">{{ quote.quoteNumber }}</button><av-status [label]="statusLabel(quote.status)" [tone]="statusTone(quote.status)" /></div>
                <dl><div><dt>{{ localization.text('currency') }}</dt><dd>{{ quote.currencyCode }}</dd></div><div><dt>{{ localization.text('created') }}</dt><dd>{{ quote.createdAt | date:'mediumDate' }}</dd></div><div><dt>{{ localization.text('validUntil') }}</dt><dd>{{ quote.validUntil ? (quote.validUntil | date:'mediumDate') : '—' }}</dd></div></dl>
              </li>
            }
          </ul> }
          @if (selectedQuoteId(); as quoteId) { <app-service-quote-detail class="detail-panel" [orderId]="orderId" [quoteId]="quoteId" (back)="clearSelection()" /> }
        </section>
      }
    }
  `,
  styles: [`
    :host { display: block; }
    .loading { padding: 2rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); color: var(--av-color-muted); }
    .list-heading { display: flex; align-items: center; justify-content: space-between; gap: 1rem; margin-bottom: 1rem; }
    .heading-actions { display: flex; align-items: center; gap: .75rem; }
    .eyebrow { margin: 0 0 .3rem; color: var(--av-color-brand); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }
    h2 { margin: 0; color: var(--av-color-ink); font-size: 1.35rem; }
    .count { display: grid; place-items: center; min-inline-size: 2rem; min-block-size: 2rem; border-radius: 50%; background: var(--av-color-canvas); color: var(--av-color-muted); font-weight: 800; }
    .create-action { min-block-size: 2.75rem; padding: .65rem 1rem; border: 0; border-radius: var(--av-radius-sm); background: var(--av-color-brand); color: white; cursor: pointer; font-weight: 800; }
    .quote-open { padding: .35rem .5rem; border: 0; border-radius: var(--av-radius-sm); background: transparent; color: var(--av-color-brand-strong); cursor: pointer; font-weight: 800; text-decoration: underline; text-underline-offset: .18em; }
    .detail-panel { display: block; margin-top: 1.25rem; }
    .quote-table-wrap { overflow-x: auto; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); }
    .quote-table { width: 100%; border-collapse: collapse; text-align: left; }
    th, td { padding: .9rem 1rem; border-bottom: 1px solid var(--av-color-border); vertical-align: middle; white-space: nowrap; }
    thead th { background: var(--av-color-canvas); color: var(--av-color-muted); font-size: .78rem; letter-spacing: .04em; text-transform: uppercase; }
    tbody th { color: var(--av-color-ink); font-size: .95rem; }
    tbody tr:last-child th, tbody tr:last-child td { border-bottom: 0; }
    .quote-cards { display: none; padding: 0; margin: 0; list-style: none; }
    .quote-card { padding: 1rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); }
    .card-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: .75rem; }
    .quote-card dl { display: grid; grid-template-columns: repeat(3, 1fr); gap: .75rem; margin: 1rem 0 0; }
    dt { color: var(--av-color-muted); font-size: .75rem; font-weight: 700; } dd { margin: .25rem 0 0; color: var(--av-color-ink); font-weight: 700; }
    .retry { margin-top: 1rem; min-block-size: 2.75rem; padding: .6rem 1rem; border: 0; border-radius: var(--av-radius-sm); background: var(--av-color-brand); color: white; cursor: pointer; font-weight: 800; }
    .sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
    @media (max-width: 620px) { .quote-table-wrap { display: none; } .quote-cards { display: grid; gap: .75rem; } .quote-card dl { grid-template-columns: 1fr 1fr; } }
  `],
})
export class ServiceOrderQuotesComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(ServiceQuoteApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly connectivity = inject(ConnectivityService);
  protected readonly localization = inject(LocalizationService);
  protected readonly retryVersion = signal(0);
  protected readonly showCreate = signal(false);
  protected readonly success = signal(false);
  protected readonly selectedQuoteId = signal<string | null>(null);
  protected orderId = '';

  private readonly router = inject(Router);

  constructor() {
    this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => this.orderId = params.get('orderId') ?? '');
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => this.selectedQuoteId.set(params.get('quoteId')));
  }

  readonly state$ = combineLatest([this.route.paramMap, toObservable(this.retryVersion)]).pipe(
    switchMap(([params]) => {
      const orderId = params.get('orderId');
      if (!orderId) return of<QuoteListState>({ loading: false, quotes: [], error: { status: 404, message: 'Quotes unavailable.' } });
      return this.api.listQuotes(orderId).pipe(
        map((quotes) => ({ loading: false, quotes, error: null })),
        startWith({ loading: true, quotes: [], error: null }),
        catchError((error: ApiError) => of({ loading: false, quotes: [], error })),
      );
    }),
    takeUntilDestroyed(this.destroyRef),
  );

  retry(): void { this.retryVersion.update((value) => value + 1); }
  openCreate(): void { this.success.set(false); this.showCreate.set(true); }
  closeCreate(): void { this.showCreate.set(false); }
  handleCreated(quote: ServiceQuoteSummary): void {
      this.showCreate.set(false);
      this.success.set(true);
    this.retryVersion.update((value) => value + 1);
    }
  protected statusLabel(status: ServiceQuoteStatus): string { return status.replaceAll('_', ' '); }
  protected statusTone(status: ServiceQuoteStatus): AvStatusTone {
    const tones: Record<ServiceQuoteStatus, AvStatusTone> = { DRAFT: 'neutral', ISSUED: 'info', ACCEPTED: 'success', DECLINED: 'danger', CANCELLED: 'neutral', EXPIRED: 'warning', SUPERSEDED: 'neutral' };
    return tones[status];
  }
  selectQuote(quoteId: string): void { void this.router.navigate([], { relativeTo: this.route, queryParams: { section: 'quotes', quoteId }, queryParamsHandling: 'merge' }); }
  clearSelection(): void { void this.router.navigate([], { relativeTo: this.route, queryParams: { section: 'quotes', quoteId: null }, queryParamsHandling: 'merge' }); }
  protected errorTitle(error: ApiError): string { return error.status === 404 ? this.localization.text('quotesUnavailable') : this.localization.text('unableToLoadQuotes'); }
  protected errorMessage(error: ApiError): string {
    if (error.status === 403) return this.localization.text('notAuthorized');
    if (!this.connectivity.isOnline()) return this.localization.text('offlineRetry');
    return error.status >= 500 || error.status === 0 ? this.localization.text('tryAgain') : error.message;
  }
}