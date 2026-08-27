import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { catchError, of } from 'rxjs';
import { ApiError } from '../../core/error/api-error';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { LocalizationService } from '../../core/localization/localization.service';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitEvidenceSummaryComponent } from './service-profit-evidence-summary.component';
import { ServiceProfitOpportunityOverviewComponent } from './service-profit-opportunity-overview.component';
import { ServiceProfitFollowUpHistoryResponse, ServiceProfitFollowUpResponse, ServiceProfitOpportunityResponse } from './service-profit.models';

type WorkspaceSection = 'OVERVIEW' | 'EVIDENCE' | 'FOLLOW_UP' | 'HISTORY';

@Component({
  selector: 'app-service-profit-opportunity-workspace',
  imports: [AvStatusComponent, DatePipe, ServiceProfitEvidenceSummaryComponent, ServiceProfitOpportunityOverviewComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<section class="workspace" aria-labelledby="workspace-heading"><header class="workspace-header"><div><p class="eyebrow">{{ localization.text('opportunityWorkCard') }}</p><h2 id="workspace-heading">{{ opportunity().title }}</h2><p class="summary">{{ opportunity().summary }}</p></div><div class="status-stack"><av-status [label]="label(opportunity().priority)" [tone]="priorityTone()" /><av-status [label]="label(opportunity().actionability)" [tone]="actionabilityTone()" /></div></header><nav class="section-tabs" aria-label="Opportunity sections" role="tablist">@for (section of sections; track section) { <button type="button" role="tab" [attr.aria-selected]="activeSection() === section" [class.active]="activeSection() === section" (click)="selectSection(section)">{{ sectionLabel(section) }}</button> }</nav>@switch (activeSection()) { @case ('OVERVIEW') { <app-service-profit-opportunity-overview [opportunity]="opportunity()" /> } @case ('EVIDENCE') { <app-service-profit-evidence-summary [opportunity]="opportunity()" /> } @case ('FOLLOW_UP') { <section class="data-section" aria-labelledby="follow-up-heading"><header class="section-heading"><h3 id="follow-up-heading">{{ localization.text('followUp') }}</h3><button type="button" class="refresh" (click)="loadFollowUp()">{{ localization.text('refresh') }}</button></header>@if (followUpLoading()) { <p role="status" aria-live="polite">{{ localization.text('loadingFollowUp') }}</p> } @else if (followUpError()) { <p class="error" role="alert">{{ localization.text('unableToLoadFollowUp') }}</p><button type="button" class="retry" (click)="loadFollowUp()">{{ localization.text('retry') }}</button> } @else if (followUp(); as detail) { <dl class="facts"><div><dt>{{ localization.text('handlingStatus') }}</dt><dd>{{ label(detail.handlingStatus) }}</dd></div><div><dt>{{ localization.text('ownership') }}</dt><dd>{{ ownershipLabel(detail.ownership) }}</dd></div><div><dt>{{ localization.text('dueDate') }}</dt><dd>{{ detail.dueAt ? (detail.dueAt | date:'medium') : localization.text('noDueDate') }}</dd></div><div><dt>{{ localization.text('dueState') }}</dt><dd>{{ label(detail.dueState) }}</dd></div><div><dt>{{ localization.text('disposition') }}</dt><dd>{{ label(detail.disposition) }}</dd></div></dl> } @else if (followUpNotFound()) { <p>{{ localization.text('noFollowUp') }}</p> }</section> } @case ('HISTORY') { <section class="data-section" aria-labelledby="history-heading"><header class="section-heading"><h3 id="history-heading">{{ localization.text('history') }}</h3><button type="button" class="refresh" (click)="loadHistory()">{{ localization.text('refresh') }}</button></header>@if (historyLoading()) { <p role="status" aria-live="polite">{{ localization.text('loadingFollowUpHistory') }}</p> } @else if (historyError() && !historyNotFound()) { <p class="error" role="alert">{{ localization.text('unableToLoadFollowUpHistory') }}</p><button type="button" class="retry" (click)="loadHistory()">{{ localization.text('retry') }}</button> } @else if (history()?.length) { <ol class="timeline">@for (event of history(); track event.occurredAt + event.eventType) { <li><article><h4>{{ label(event.eventType) }}</h4><p>{{ actorLabel(event) }}</p>@if (event.previousValue || event.newValue) { <dl class="event-values">@if (event.previousValue) { <div><dt>{{ localization.text('previousValue') }}</dt><dd>{{ label(event.previousValue) }}</dd></div> } @if (event.newValue) { <div><dt>{{ localization.text('newValue') }}</dt><dd>{{ label(event.newValue) }}</dd></div> }</dl> }<time [attr.datetime]="event.occurredAt">{{ event.occurredAt | date:'medium' }}</time></article></li> }</ol> } @else { <p>{{ localization.text('noFollowUpHistory') }}</p> }</section> } }</section>`,
  styles: [`.workspace { display: grid; gap: 1.25rem; padding: 1.25rem; border-top: 4px solid var(--av-color-brand); background: var(--av-color-surface); } .workspace-header { display: flex; justify-content: space-between; gap: 1rem; } .eyebrow { margin: 0 0 .35rem; color: var(--av-color-brand-strong); font-size: .75rem; font-weight: 800; letter-spacing: .08em; text-transform: uppercase; } h2, h3, h4 { margin: 0; } .summary { max-width: 58rem; margin: .45rem 0 0; color: var(--av-color-muted); line-height: 1.5; } .status-stack { display: flex; flex-wrap: wrap; align-items: start; gap: .5rem; } .section-tabs { display: flex; flex-wrap: wrap; gap: .25rem; border-bottom: 1px solid var(--av-color-border); } .section-tabs button { min-block-size: 2.75rem; padding: .55rem .9rem; border: 0; border-bottom: 3px solid transparent; background: transparent; color: var(--av-color-muted); cursor: pointer; font: inherit; font-weight: 800; } .section-tabs button.active { border-color: var(--av-color-brand); color: var(--av-color-brand-strong); } button:focus-visible { outline: 3px solid var(--av-color-focus); outline-offset: 2px; } .data-section { padding: 1rem; border: 1px solid var(--av-color-border); background: var(--av-color-canvas); } .section-heading { display: flex; justify-content: space-between; align-items: center; gap: 1rem; } .refresh, .retry { min-block-size: 2.5rem; padding: .5rem .8rem; border: 1px solid var(--av-color-brand); border-radius: var(--av-radius-sm); background: transparent; color: var(--av-color-brand-strong); cursor: pointer; font: inherit; font-weight: 800; } .facts { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 1rem; margin: 1rem 0 0; } dt { color: var(--av-color-muted); font-size: .75rem; font-weight: 800; text-transform: uppercase; } dd { margin: .3rem 0 0; overflow-wrap: anywhere; } .error { color: var(--av-color-danger, #a32116); } .timeline { display: grid; gap: .75rem; margin: 1rem 0 0; padding: 0; list-style: none; } .timeline article { padding: .85rem; border-left: 3px solid var(--av-color-brand); background: var(--av-color-surface); } .timeline p { margin: .35rem 0; color: var(--av-color-muted); } .timeline time { display: block; margin-top: .5rem; color: var(--av-color-muted); font-size: .9rem; } .event-values { display: flex; gap: 1.5rem; margin: .75rem 0 0; } @media (max-width: 900px) { .facts { grid-template-columns: repeat(2, minmax(0, 1fr)); } } @media (max-width: 720px) { .workspace-header { flex-direction: column; } .section-tabs { overflow-x: auto; flex-wrap: nowrap; } .section-tabs button { flex: 1 0 auto; } .facts, .event-values { grid-template-columns: 1fr; flex-direction: column; gap: .65rem; } }`],
})
export class ServiceProfitOpportunityWorkspaceComponent {
  protected readonly localization = inject(LocalizationService);
  private readonly api = inject(ServiceProfitApiService);
  private readonly destroyRef = inject(DestroyRef);
  readonly opportunity = input.required<ServiceProfitOpportunityResponse>();
  protected readonly activeSection = signal<WorkspaceSection>('OVERVIEW');
  protected readonly sections: readonly WorkspaceSection[] = ['OVERVIEW', 'EVIDENCE', 'FOLLOW_UP', 'HISTORY'];
  protected readonly followUp = signal<ServiceProfitFollowUpResponse | null>(null);
  protected readonly followUpLoading = signal(false);
  protected readonly followUpError = signal<ApiError | null>(null);
  protected readonly followUpNotFound = signal(false);
  protected readonly history = signal<ServiceProfitFollowUpHistoryResponse[] | null>(null);
  protected readonly historyLoading = signal(false);
  protected readonly historyError = signal<ApiError | null>(null);
  protected readonly historyNotFound = signal(false);

  protected sectionLabel(section: WorkspaceSection): string { return section === 'FOLLOW_UP' ? this.localization.text('followUp') : section === 'HISTORY' ? this.localization.text('history') : section === 'EVIDENCE' ? this.localization.text('evidence') : this.localization.text('overview'); }
  protected selectSection(section: WorkspaceSection): void {
    this.activeSection.set(section);
    if (section === 'FOLLOW_UP' && !this.followUp() && !this.followUpNotFound() && !this.followUpError()) this.loadFollowUp();
    if (section === 'HISTORY' && !this.history() && !this.historyError()) this.loadHistory();
  }
  protected loadFollowUp(): void {
    this.followUpLoading.set(true);
    this.followUpError.set(null);
    this.followUpNotFound.set(false);
    this.api.getOpportunityFollowUp(this.opportunity().id).pipe(
      catchError((error: ApiError) => of({ error })),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((result) => {
      if ('error' in result) {
        this.followUp.set(null);
        this.followUpNotFound.set(result.error.status === 404);
        this.followUpError.set(result.error.status === 404 ? null : result.error);
      } else {
        this.followUp.set(result);
      }
      this.followUpLoading.set(false);
    });
  }
  protected loadHistory(): void {
    this.historyLoading.set(true);
    this.historyError.set(null);
    this.historyNotFound.set(false);
    this.api.getOpportunityFollowUpHistory(this.opportunity().id).pipe(
      catchError((error: ApiError) => of({ error })),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((result) => {
      if ('error' in result) {
        this.history.set(null);
        this.historyNotFound.set(result.error.status === 404);
        this.historyError.set(result.error.status === 404 ? null : result.error);
      } else {
        this.history.set(result);
      }
      this.historyLoading.set(false);
    });
  }
  protected ownershipLabel(value: string): string { return value === 'MINE' ? this.localization.text('mine') : value === 'UNASSIGNED' ? this.localization.text('unassigned') : this.localization.text('assigned'); }
  protected actorLabel(event: ServiceProfitFollowUpHistoryResponse): string { return event.actorIdentity === 'ME' ? this.localization.text('me') : event.actorIdentity === 'AUTOVISION_SERVICE_PROFIT' ? this.localization.text('autoVisionServiceProfit') : this.localization.text('humanActor'); }
  protected label(value: string): string { return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase()); }
  protected priorityTone(): AvStatusTone { return this.opportunity().priority === 'HIGH' ? 'danger' : this.opportunity().priority === 'MEDIUM' ? 'warning' : 'neutral'; }
  protected actionabilityTone(): AvStatusTone { const value = this.opportunity().actionability; return value === 'READY' ? 'success' : value === 'REVIEW_REQUIRED' || value === 'CONTACT_DATA_MISSING' ? 'warning' : value === 'SUPPRESSED' ? 'neutral' : 'danger'; }
}
