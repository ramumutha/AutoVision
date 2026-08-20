import { AsyncPipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, inject, input } from '@angular/core';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvStatusComponent } from '../../shared/design-system/av-status.component';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { ServiceJobSummary, ServiceLineResponse } from './service-order.models';

@Component({
  selector: 'app-service-order-lines',
  imports: [DecimalPipe, AvStatusComponent, AvFeedbackComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <section aria-labelledby="lines-title">
      <div class="heading"><div><p class="eyebrow">{{ localization.text('serviceOrder') }}</p><h2 id="lines-title">{{ localization.text('serviceLines') }}</h2></div></div>
      <p class="automation-note">{{ localization.text('quoteAutomaticLines') }}</p>
      @if (lines().length === 0) { <av-feedback [title]="localization.text('serviceLines')" [message]="localization.text('noServiceLines')" /> }
      @else {
        <div class="table-wrap"><table><caption class="sr-only">{{ localization.text('serviceLines') }}</caption><thead><tr><th scope="col">{{ localization.text('line') }}</th><th scope="col">{{ localization.text('description') }}</th><th scope="col">{{ localization.text('lineType') }}</th><th scope="col">{{ localization.text('jobContext') }}</th><th scope="col">{{ localization.text('quantity') }}</th><th scope="col">{{ localization.text('unitOfMeasure') }}</th><th scope="col">{{ localization.text('unitPrice') }}</th><th scope="col">{{ localization.text('net') }}</th><th scope="col">{{ localization.text('tax') }}</th><th scope="col">{{ localization.text('gross') }}</th><th scope="col">{{ localization.text('currency') }}</th><th scope="col">{{ localization.text('commercialData') }}</th></tr></thead><tbody>@for (line of lines(); track line.id) { <tr><th scope="row">{{ line.lineNumber }}</th><td>{{ line.description }}</td><td>{{ displayType(line.lineType) }}</td><td>{{ jobLabel(line.serviceJobId) }}</td><td>{{ line.quantity | number:'1.0-4' }}</td><td>{{ line.unitOfMeasure }}</td><td>{{ money(line.unitPrice, line.currencyCode) }}</td><td>{{ money(line.netAmount, line.currencyCode) }}</td><td>{{ money(line.taxAmount, line.currencyCode) }}</td><td>{{ money(line.grossAmount, line.currencyCode) }}</td><td>{{ line.currencyCode || '—' }}</td><td><av-status [label]="readinessLabel(line)" [tone]="line.hasCommercialSnapshot ? 'success' : 'neutral'" /></td></tr> }</tbody></table></div>
        <ul class="cards" aria-label="Service Line summaries">@for (line of lines(); track line.id) { <li><div class="card-top"><strong>#{{ line.lineNumber }} {{ line.description }}</strong><av-status [label]="readinessLabel(line)" [tone]="line.hasCommercialSnapshot ? 'success' : 'neutral'" /></div><span>{{ displayType(line.lineType) }} · {{ line.quantity | number:'1.0-4' }} {{ line.unitOfMeasure }}</span><span>{{ localization.text('jobContext') }}: {{ jobLabel(line.serviceJobId) }}</span><span>{{ localization.text('unitPrice') }}: {{ money(line.unitPrice, line.currencyCode) }}</span><span>{{ localization.text('net') }}: {{ money(line.netAmount, line.currencyCode) }} · {{ localization.text('tax') }}: {{ money(line.taxAmount, line.currencyCode) }}</span><span>{{ localization.text('gross') }}: {{ money(line.grossAmount, line.currencyCode) }} · {{ line.currencyCode || '—' }}</span></li> }</ul>
      }
    </section>
  `,
  styles: [`
    :host { display: block; }.heading { margin-bottom: .75rem; }.eyebrow { margin: 0 0 .3rem; color: var(--av-color-brand); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; }h2 { margin: 0; color: var(--av-color-ink); font-size: 1.35rem; }.automation-note { margin: 0 0 1rem; color: var(--av-color-muted); }
      .table-wrap { overflow-x: visible; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); }table { width: 100%; border-collapse: collapse; text-align: left; }th, td { padding: .8rem .75rem; border-bottom: 1px solid var(--av-color-border); white-space: nowrap; }thead th { color: var(--av-color-muted); font-size: .72rem; text-transform: uppercase; }tbody th { color: var(--av-color-ink); }tbody tr:last-child th, tbody tr:last-child td { border-bottom: 0; }.cards { display: none; padding: 0; margin: 0; list-style: none; }.cards li { display: grid; gap: .45rem; padding: 1rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-md); background: var(--av-color-surface); }.card-top { display: flex; align-items: flex-start; justify-content: space-between; gap: .75rem; }.cards span { color: var(--av-color-muted); }.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; margin: -1px; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
    @media (max-width: 700px) { .table-wrap { display: none; }.cards { display: grid; gap: .75rem; } }
    @media (max-width: 1000px) { .table-wrap { overflow-x: auto; } }
  `],
})
export class ServiceOrderLinesComponent {
  readonly lines = input.required<ServiceLineResponse[]>();
  readonly jobs = input<ServiceJobSummary[]>([]);
  protected readonly localization = inject(LocalizationService);

  protected jobLabel(jobId: string | null | undefined): string { return jobId ? this.jobs().find((job) => job.id === jobId)?.jobNumber ?? '—' : '—'; }
  protected displayType(type: string): string { return type.replaceAll('_', ' '); }
  protected readinessLabel(line: ServiceLineResponse): string { return line.hasCommercialSnapshot ? this.localization.text('commercialAvailable') : this.localization.text('commercialIncomplete'); }
  protected money(amount: number | null | undefined, currency: string | null | undefined): string { if (amount === null || amount === undefined || !currency) return this.localization.text('notAvailable'); try { return new Intl.NumberFormat(undefined, { style: 'currency', currency }).format(amount); } catch { return `${amount} ${currency}`; } }
}