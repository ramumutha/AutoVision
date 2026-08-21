import { AsyncPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, output, signal } from '@angular/core';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { catchError, combineLatest, map, of, startWith, switchMap } from 'rxjs';
import { ConnectivityService } from '../../core/connectivity/connectivity.service';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { CustomerAuthorization } from './customer-authorization.models';
import { ServiceQuoteAuthorizationRequestComponent } from './service-quote-authorization-request.component';
import { ServiceQuoteApiService } from './service-quote-api.service';
import { ServiceQuoteDetail, ServiceQuoteStatus } from './service-quote.models';

const LIFECYCLE_KEYS = ['issuedAt', 'acceptedAt', 'declinedAt', 'cancelledAt', 'expiredAt', 'supersededAt'] as const;

@Component({
  selector: 'app-service-quote-detail',
  imports: [AsyncPipe, DatePipe, DecimalPipe, AvStatusComponent, AvFeedbackComponent, ServiceQuoteAuthorizationRequestComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (state$ | async; as state) {
      @if (state.loading) { <div class="loading" role="status" aria-live="polite">{{ localization.text('loadingQuoteDetails') }}</div> }
      @else if (state.error; as error) { <av-feedback kind="error" [title]="errorTitle(error)" [message]="errorMessage(error)" /><button class="retry" type="button" (click)="retry()">{{ localization.text('retry') }}</button> }
      @else if (state.detail; as detail) {
        <article class="detail" aria-labelledby="quote-detail-title">
          <div class="detail-heading"><div><p class="eyebrow">{{ localization.text('quoteDetails') }}</p><h2 id="quote-detail-title">{{ detail.quoteNumber }}</h2></div><div class="heading-actions"><av-status [label]="statusLabel(detail.status)" [tone]="statusTone(detail.status)" /><button class="back" type="button" (click)="back.emit()">{{ localization.text('backToQuotes') }}</button></div></div>
          @if (canRequestAuthorization(detail)) { <button class="request-authorization" type="button" (click)="requestAuthorizationOpen.set(true)">{{ localization.text('requestCustomerAuthorization') }}</button> }
          @if (requestAuthorizationOpen() && detail.afterSalesCaseId) { <app-service-quote-authorization-request [caseId]="detail.afterSalesCaseId" [quoteId]="detail.id" (cancel)="closeAuthorizationRequest()" (requested)="authorizationRequested($event)" /> }
          @if (requestedAuthorization(); as authorization) { <av-feedback kind="success" [title]="localization.text('customerAuthorizationRequested')" [message]="localization.text('customerAuthorizationPending')" /><div class="authorization-status"><span>{{ localization.text('status') }}</span><av-status [label]="authorization.authorizationStatus" tone="info" /></div> }
          <dl class="facts"><div><dt>{{ localization.text('status') }}</dt><dd>{{ statusLabel(detail.status) }}</dd></div><div><dt>{{ localization.text('currency') }}</dt><dd>{{ detail.currencyCode }}</dd></div><div><dt>{{ localization.text('validUntil') }}</dt><dd>{{ detail.validUntil ? (detail.validUntil | date:'medium') : '—' }}</dd></div><div><dt>{{ localization.text('created') }}</dt><dd>{{ detail.createdAt | date:'medium' }}</dd></div><div><dt>{{ localization.text('updated') }}</dt><dd>{{ detail.updatedAt | date:'medium' }}</dd></div></dl>
          <dl class="lifecycle">@for (event of lifecycle(detail); track event.label) { <div><dt>{{ event.label }}</dt><dd>{{ event.value | date:'medium' }}</dd></div> }</dl>
          <div class="disclosures"><details><summary>{{ localization.text('terms') }}</summary><p>{{ detail.termsSnapshot || '—' }}</p></details><details><summary>{{ localization.text('disclaimer') }}</summary><p>{{ detail.disclaimerSnapshot || '—' }}</p></details></div>
          <section aria-labelledby="lines-title"><h3 id="lines-title">{{ localization.text('quoteLines') }}</h3><div class="line-table-wrap"><table><caption class="sr-only">{{ localization.text('quoteLines') }}</caption><thead><tr><th scope="col">{{ localization.text('description') }}</th><th scope="col">{{ localization.text('quantity') }}</th><th scope="col">{{ localization.text('unitPrice') }}</th><th scope="col">{{ localization.text('net') }}</th><th scope="col">{{ localization.text('tax') }}</th><th scope="col">{{ localization.text('gross') }}</th><th scope="col">{{ localization.text('currency') }}</th></tr></thead><tbody>@for (line of detail.lines; track line.id) { <tr><th scope="row">{{ line.descriptionSnapshot }}</th><td>{{ line.quantity | number:'1.0-4' }}</td><td>{{ money(line.unitPrice, line.currencyCode) }}</td><td>{{ money(line.netAmount, line.currencyCode) }}</td><td>{{ money(line.taxAmount, line.currencyCode) }}</td><td>{{ money(line.grossAmount, line.currencyCode) }}</td><td>{{ line.currencyCode }}</td></tr> }</tbody></table></div><ul class="line-cards">@for (line of detail.lines; track line.id) { <li><strong>{{ line.descriptionSnapshot }}</strong><span>{{ localization.text('quantity') }}: {{ line.quantity | number:'1.0-4' }} × {{ money(line.unitPrice, line.currencyCode) }}</span><span>{{ localization.text('net') }}: {{ money(line.netAmount, line.currencyCode) }}</span><span>{{ localization.text('tax') }}: {{ money(line.taxAmount, line.currencyCode) }}</span><span>{{ localization.text('gross') }}: {{ money(line.grossAmount, line.currencyCode) }}</span></li> }</ul></section>
        </article>
      }
    }
  `,
  styles: [`
    :host { display: block; }
    .detail { padding: 1.25rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); }
    .detail-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 1rem; margin-bottom: 1.25rem; }.heading-actions { display: flex; align-items: center; gap: .75rem; }.eyebrow { margin: 0 0 .3rem; color: var(--av-color-brand); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }h2 { margin: 0; color: var(--av-color-ink); font-size: 1.5rem; }.back { min-block-size: 2.75rem; padding: .6rem .85rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); cursor: pointer; font-weight: 800; }
    .request-authorization { margin: 0 0 1.25rem; min-block-size: 2.75rem; padding: .65rem 1rem; border: 0; border-radius: var(--av-radius-sm); background: var(--av-color-brand); color: white; cursor: pointer; font-weight: 800; }.authorization-status { display: flex; align-items: center; gap: .75rem; margin: 1rem 0 1.25rem; color: var(--av-color-muted); font-weight: 700; }
    .facts, .lifecycle { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: .75rem; margin: 0 0 1.25rem; }.facts div, .lifecycle div { padding: .75rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-canvas); }dt { color: var(--av-color-muted); font-size: .75rem; font-weight: 700; }dd { margin: .3rem 0 0; color: var(--av-color-ink); font-weight: 700; }
    .disclosures { display: grid; gap: .5rem; margin-bottom: 1.5rem; }details { border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); padding: .8rem 1rem; }summary { cursor: pointer; color: var(--av-color-ink); font-weight: 800; }details p { margin: .75rem 0 0; white-space: pre-wrap; color: var(--av-color-muted); }
    h3 { margin: 0 0 .75rem; color: var(--av-color-ink); }.line-table-wrap { overflow-x: auto; }table { width: 100%; border-collapse: collapse; text-align: left; }th, td { padding: .8rem .75rem; border-bottom: 1px solid var(--av-color-border); white-space: nowrap; }thead th { color: var(--av-color-muted); font-size: .75rem; text-transform: uppercase; }tbody th { color: var(--av-color-ink); }.line-cards { display: none; padding: 0; margin: 0; list-style: none; }.line-cards li { display: grid; gap: .45rem; padding: 1rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); }.line-cards span { color: var(--av-color-muted); }.loading { padding: 2rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); color: var(--av-color-muted); }.retry { margin-top: 1rem; min-block-size: 2.75rem; padding: .6rem 1rem; border: 0; border-radius: var(--av-radius-sm); background: var(--av-color-brand); color: white; cursor: pointer; font-weight: 800; }.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0, 0, 0, 0); white-space: nowrap; border: 0; }
    @media (max-width: 800px) { .facts, .lifecycle { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
    @media (max-width: 620px) { .detail-heading { flex-direction: column; }.heading-actions { width: 100%; justify-content: space-between; }.line-table-wrap { display: none; }.line-cards { display: grid; gap: .75rem; } }
  `],
})
export class ServiceQuoteDetailComponent {
  readonly orderId = input.required<string>();
  readonly quoteId = input.required<string>();
  readonly back = output<void>();
  private readonly api = inject(ServiceQuoteApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly connectivity = inject(ConnectivityService);
  protected readonly localization = inject(LocalizationService);
  protected readonly retryVersion = signal(0);
  protected readonly requestAuthorizationOpen = signal(false);
  protected readonly requestedAuthorization = signal<CustomerAuthorization | null>(null);
  readonly state$ = combineLatest([toObservable(this.orderId), toObservable(this.quoteId), toObservable(this.retryVersion)]).pipe(
    switchMap(([orderId, quoteId]) => this.api.getQuote(orderId, quoteId).pipe(map((detail) => ({ loading: false, detail, error: null })), startWith({ loading: true, detail: null, error: null }), catchError((error: ApiError) => of({ loading: false, detail: null, error })))), takeUntilDestroyed(this.destroyRef),
  );
  retry(): void { this.retryVersion.update((value) => value + 1); }
  protected canRequestAuthorization(detail: ServiceQuoteDetail): boolean { return detail.status === 'ISSUED' && !!detail.afterSalesCaseId?.trim(); }
  protected closeAuthorizationRequest(): void { this.requestAuthorizationOpen.set(false); }
  protected authorizationRequested(authorization: CustomerAuthorization): void { this.requestedAuthorization.set(authorization); this.requestAuthorizationOpen.set(false); }
  protected statusLabel(status: ServiceQuoteStatus): string { return status.replaceAll('_', ' '); }
  protected statusTone(status: ServiceQuoteStatus): AvStatusTone { const tones: Record<ServiceQuoteStatus, AvStatusTone> = { DRAFT: 'neutral', ISSUED: 'info', ACCEPTED: 'success', DECLINED: 'danger', CANCELLED: 'neutral', EXPIRED: 'warning', SUPERSEDED: 'neutral' }; return tones[status]; }
  protected money(amount: number, currency: string): string { try { return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount); } catch { return `${amount} ${currency}`; } }
  protected lifecycle(detail: ServiceQuoteDetail): { label: string; value: string }[] { return LIFECYCLE_KEYS.map((key) => [key, detail[key]] as const).filter((event): event is [typeof LIFECYCLE_KEYS[number], string] => !!event[1]).map(([key, value]) => ({ label: this.localization.text(key), value })); }
  protected errorTitle(error: ApiError): string { return error.status === 404 ? this.localization.text('quoteUnavailable') : this.localization.text('unableToLoadQuoteDetails'); }
  protected errorMessage(error: ApiError): string { if (error.status === 403) return this.localization.text('notAuthorized'); if (!this.connectivity.isOnline()) return this.localization.text('offlineRetry'); return error.status >= 500 || error.status === 0 ? this.localization.text('tryAgain') : error.message; }
}