import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { ServiceProfitApiService } from './service-profit-api.service';
import {
  ServiceProfitWorkQueueDisposition, ServiceProfitWorkQueueDueState, ServiceProfitWorkQueueGroupBy,
  ServiceProfitWorkQueueGroup, ServiceProfitWorkQueueItem, ServiceProfitWorkQueueOwnership, ServiceProfitWorkQueuePage,
  ServiceProfitWorkQueueQuery, ServiceProfitWorkQueueSort,
} from './service-profit.models';
import { ServiceProfitQueueGroupComponent } from './service-profit-queue-group.component';

@Component({
  selector: 'app-service-profit-work-queue',
  imports: [AvFeedbackComponent, ServiceProfitQueueGroupComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <header class="page-header"><div><p class="eyebrow">{{ localization.text('serviceProfit') }}</p><h1>{{ localization.text('serviceProfitWorkQueue') }}</h1><p class="context">{{ localization.text('serviceProfitWorkQueueContext') }}</p></div></header>
    <section class="workspace" aria-labelledby="queue-heading" [attr.aria-busy]="loading() || refreshing()">
      <div class="queue-heading"><div><h2 id="queue-heading">{{ localization.text('serviceProfitWorkQueue') }}</h2><p class="scope-note">{{ localization.text('loadedPageOnly') }}</p></div><button class="refresh" type="button" [disabled]="loading() || refreshing()" (click)="refresh()" [attr.aria-label]="localization.text('refreshServiceProfit')">{{ refreshing() ? localization.text('refreshing') : localization.text('refresh') }}</button></div>
      <nav class="ownership-tabs" [attr.aria-label]="localization.text('ownership')"><button type="button" [class.active]="ownership() === 'ALL'" [attr.aria-pressed]="ownership() === 'ALL'" (click)="setOwnership('ALL')">{{ localization.text('all') }}</button><button type="button" [class.active]="ownership() === 'MINE'" [attr.aria-pressed]="ownership() === 'MINE'" (click)="setOwnership('MINE')">{{ localization.text('mine') }}</button><button type="button" [class.active]="ownership() === 'UNASSIGNED'" [attr.aria-pressed]="ownership() === 'UNASSIGNED'" (click)="setOwnership('UNASSIGNED')">{{ localization.text('unassigned') }}</button></nav>
      <div class="controls"><label>{{ localization.text('groupBy') }}<select [value]="groupBy()" (change)="setGroupBy($any($event.target).value)"><option value="DUE_STATE">{{ localization.text('dueState') }}</option><option value="OWNERSHIP">{{ localization.text('ownership') }}</option><option value="DISPOSITION">{{ localization.text('disposition') }}</option><option value="PRIORITY">{{ localization.text('priority') }}</option><option value="NONE">{{ localization.text('noGrouping') }}</option></select></label><label>{{ localization.text('sortBy') }}<select [value]="sort()" (change)="setSort($any($event.target).value)"><option value="PRIORITY">{{ localization.text('priority') }}</option><option value="DUE_DATE">{{ localization.text('dueDate') }}</option><option value="OLDEST">{{ localization.text('oldest') }}</option><option value="NEWEST">{{ localization.text('newest') }}</option></select></label><button type="button" class="filter-trigger" [attr.aria-expanded]="filtersOpen()" aria-controls="queue-filters" (click)="filtersOpen.update((value) => !value)">{{ localization.text('filters') }}@if (filtersActive()) { <span aria-label="active">({{ activeFilterCount() }})</span> }</button></div>
      @if (filtersOpen()) { <form id="queue-filters" class="filter-panel" (submit)="$event.preventDefault(); applyFilters()"><label>{{ localization.text('handlingStatus') }}<select [value]="handlingStatus()" (change)="handlingStatus.set($any($event.target).value)"><option value="">{{ localization.text('all') }}</option><option value="OPEN">Open</option><option value="COMPLETED">Completed</option></select></label><label>{{ localization.text('dueState') }}<select [value]="dueState()" (change)="dueState.set($any($event.target).value)"><option value="">{{ localization.text('all') }}</option><option value="OVERDUE">{{ localization.text('overdue') }}</option><option value="UPCOMING">{{ localization.text('upcoming') }}</option><option value="NO_DUE_DATE">{{ localization.text('noDueDate') }}</option></select></label><label>{{ localization.text('disposition') }}<select [value]="disposition()" (change)="disposition.set($any($event.target).value)"><option value="">{{ localization.text('all') }}</option><option value="NONE">None</option><option value="FOLLOW_UP_REQUIRED">{{ localization.text('followUpRequired') }}</option><option value="INTEREST_RECORDED">{{ localization.text('interestRecorded') }}</option><option value="DECLINED_RECORDED">{{ localization.text('declinedRecorded') }}</option><option value="NO_RESPONSE_RECORDED">{{ localization.text('noResponseRecorded') }}</option><option value="NO_FURTHER_ACTION">{{ localization.text('noFurtherAction') }}</option></select></label><button class="apply" type="submit">{{ localization.text('applyFilters') }}</button>@if (filtersActive()) { <button class="clear" type="button" (click)="clearFilters()">{{ localization.text('clearFilters') }}</button> }</form> }
      @if (loading()) { <p class="state" role="status" aria-live="polite">{{ localization.text('loadingWorkQueue') }}</p> } @else if (error()) { <av-feedback kind="error" [title]="localization.text('unableToLoadWorkQueue')" [message]="localization.text('tryAgain')" /><button class="apply" type="button" (click)="refresh()">{{ localization.text('retry') }}</button> } @else if (!groups().length) { <av-feedback [title]="localization.text('noWorkQueueItems')" [message]="localization.text('noWorkQueueItemsContext')" /> } @else { @for (group of groups(); track group.key) { <app-service-profit-queue-group [group]="group" [groupLabel]="groupLabel(group.key)" [ownership]="ownership()" /> } @if (page() && page()!.page + 1 < page()!.totalPages) { <button class="load-more" type="button" (click)="loadMore()">{{ localization.text('loadMore') }}</button> } }
    </section>
  `,
  styles: [`
    :host { display: block; padding-block: 2rem 4rem; } .page-header { padding-block: 1rem 2rem; } .eyebrow { margin: 0; color: var(--av-color-brand-strong); font-size: .75rem; font-weight: 800; letter-spacing: .1em; text-transform: uppercase; } h1, h2, h3, h4 { margin: 0; } h1 { margin-block-start: .35rem; } .context, .scope-note, .summary { color: var(--av-color-muted); } .workspace { display: grid; gap: 1.25rem; } .queue-heading { display: flex; justify-content: space-between; gap: 1rem; align-items: start; } .scope-note { margin: .35rem 0 0; font-size: .8rem; } button, select { min-block-size: 2.75rem; font: inherit; } button { cursor: pointer; } .refresh, .apply, .load-more { padding: .55rem .9rem; border: 1px solid var(--av-color-brand); border-radius: var(--av-radius-sm); background: var(--av-color-brand); color: white; font-weight: 700; } button:focus-visible, select:focus-visible, a:focus-visible { outline: 3px solid var(--av-color-focus); outline-offset: 2px; } .ownership-tabs { display: flex; gap: .25rem; border-bottom: 1px solid var(--av-color-border); } .ownership-tabs button { padding: .6rem 1rem; border: 0; border-bottom: 3px solid transparent; background: transparent; color: var(--av-color-muted); font-weight: 800; } .ownership-tabs button.active { border-color: var(--av-color-brand); color: var(--av-color-brand-strong); } .controls, .filter-panel { display: flex; flex-wrap: wrap; align-items: end; gap: .75rem; } label { display: grid; gap: .3rem; color: var(--av-color-muted); font-size: .8rem; font-weight: 800; } select { min-inline-size: 10rem; padding: .45rem .6rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); } .filter-trigger, .clear { padding: .55rem .9rem; border: 1px solid var(--av-color-border); border-radius: var(--av-radius-sm); background: var(--av-color-surface); color: var(--av-color-ink); font-weight: 700; } .filter-panel { padding: 1rem; border: 1px solid var(--av-color-border); background: #f4f7f7; } .state { padding: 2rem 0; } .group { display: grid; gap: .75rem; } .group-title { display: flex; justify-content: space-between; align-items: baseline; padding-block-end: .55rem; border-bottom: 2px solid var(--av-color-brand); } .group-title span { color: var(--av-color-muted); font-weight: 800; } .cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(min(100%, 19rem), 1fr)); gap: 1rem; padding: 0; margin: 0; list-style: none; } .card { display: grid; gap: .8rem; min-block-size: 16rem; padding: 1rem; border: 1px solid var(--av-color-border); border-left: 4px solid var(--av-color-brand); border-radius: var(--av-radius-sm); background: var(--av-color-surface); } .card.overdue { border-left-color: var(--av-color-danger); } .card-top { display: flex; justify-content: space-between; gap: .5rem; } .card h4 { color: var(--av-color-brand-strong); font-size: 1.05rem; } .summary { margin: 0; line-height: 1.45; } dl { display: grid; grid-template-columns: repeat(2, 1fr); gap: .55rem; margin: 0; } dt { color: var(--av-color-muted); font-size: .75rem; font-weight: 700; } dd { margin: .1rem 0 0; font-weight: 700; } .card a { align-self: end; color: var(--av-color-brand-strong); font-weight: 800; } .load-more { justify-self: center; } @media (max-width: 720px) { :host { padding-block: 1rem 2rem; } .queue-heading { flex-direction: column; } .refresh { inline-size: 100%; } .controls, .filter-panel { align-items: stretch; flex-direction: column; } label, select, .filter-trigger, .apply, .clear { inline-size: 100%; } .ownership-tabs { overflow-x: auto; } .ownership-tabs button { flex: 1 0 auto; } dl { grid-template-columns: 1fr; } }
  `],
})
export class ServiceProfitWorkQueueComponent {
  protected readonly localization = inject(LocalizationService);
  private readonly api = inject(ServiceProfitApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  protected readonly loading = signal(true);
  protected readonly refreshing = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly page = signal<ServiceProfitWorkQueuePage | null>(null);
  protected readonly items = signal<ServiceProfitWorkQueueItem[]>([]);
  protected readonly ownership = signal<ServiceProfitWorkQueueOwnership>('ALL');
  protected readonly groupBy = signal<ServiceProfitWorkQueueGroupBy>('DUE_STATE');
  protected readonly sort = signal<ServiceProfitWorkQueueSort>('PRIORITY');
  protected readonly filtersOpen = signal(false);
  protected readonly handlingStatus = signal('');
  protected readonly dueState = signal('');
  protected readonly disposition = signal('');
  protected readonly groups = computed<ServiceProfitWorkQueueGroup[]>(() => this.makeGroups(this.items()));
  protected readonly filtersActive = computed(() => Boolean(this.handlingStatus() || this.dueState() || this.disposition()));
  protected readonly activeFilterCount = computed(() => [this.handlingStatus(), this.dueState(), this.disposition()].filter(Boolean).length);
  private currentPage = 0;

  constructor() {
    this.route.queryParamMap.pipe(takeUntilDestroyed()).subscribe((params) => {
      this.ownership.set(this.readOneOf(params.get('ownership'), ['ALL', 'MINE', 'UNASSIGNED'], 'ALL'));
      this.handlingStatus.set(this.readOneOf(params.get('handlingStatus'), ['OPEN', 'COMPLETED'], ''));
      this.dueState.set(this.readOneOf(params.get('dueState'), ['OVERDUE', 'UPCOMING', 'NO_DUE_DATE'], ''));
      this.disposition.set(this.readOneOf(params.get('disposition'), ['NONE', 'FOLLOW_UP_REQUIRED', 'INTEREST_RECORDED', 'DECLINED_RECORDED', 'NO_RESPONSE_RECORDED', 'NO_FURTHER_ACTION'], ''));
      this.groupBy.set(this.readOneOf(params.get('groupBy'), ['DUE_STATE', 'OWNERSHIP', 'DISPOSITION', 'PRIORITY', 'NONE'], 'DUE_STATE'));
      this.sort.set(this.readOneOf(params.get('sort'), ['PRIORITY', 'DUE_DATE', 'OLDEST', 'NEWEST'], 'PRIORITY'));
      this.currentPage = 0;
      this.load();
    });
  }

  protected refresh(): void { this.currentPage = 0; this.refreshing.set(true); this.load(true); }
  protected loadMore(): void { this.currentPage++; this.load(false, true); }
  protected setOwnership(value: ServiceProfitWorkQueueOwnership): void { this.updateUrl({ ownership: value === 'ALL' ? null : value }); }
  protected setGroupBy(value: ServiceProfitWorkQueueGroupBy): void { this.updateUrl({ groupBy: value === 'DUE_STATE' ? null : value }); }
  protected setSort(value: ServiceProfitWorkQueueSort): void { this.updateUrl({ sort: value === 'PRIORITY' ? null : value }); }
  protected applyFilters(): void { this.updateUrl({ handlingStatus: this.handlingStatus() || null, dueState: this.dueState() || null, disposition: this.disposition() || null }); }
  protected clearFilters(): void { this.updateUrl({ handlingStatus: null, dueState: null, disposition: null }); }

  protected label(value: string): string { return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase()); }
  protected dueStateFor(item: ServiceProfitWorkQueueItem): ServiceProfitWorkQueueDueState { if (!item.dueAt) return 'NO_DUE_DATE'; return new Date(item.dueAt).getTime() < Date.now() ? 'OVERDUE' : 'UPCOMING'; }
  protected groupLabel(key: string): string { return key === 'ALL' ? this.localization.text('all') : this.label(key); }
  private load(background = false, append = false): void {
    if (!background && !append) this.loading.set(true);
    this.error.set(null);
    const query: ServiceProfitWorkQueueQuery = { ownership: this.ownership(), page: this.currentPage, size: 25 };
    if (this.handlingStatus()) query.handlingStatus = this.handlingStatus() as 'OPEN' | 'COMPLETED';
    if (this.dueState()) query.dueState = this.dueState() as ServiceProfitWorkQueueDueState;
    if (this.disposition()) query.disposition = this.disposition() as ServiceProfitWorkQueueDisposition;
    this.api.getWorkQueue(query).subscribe({ next: (result) => { this.page.set(result); this.items.update((items) => append ? [...items, ...result.items] : result.items); this.loading.set(false); this.refreshing.set(false); }, error: (error: ApiError) => { this.error.set(error); this.loading.set(false); this.refreshing.set(false); } });
  }

  private makeGroups(items: ServiceProfitWorkQueueItem[]): ServiceProfitWorkQueueGroup[] {
    const groups = new Map<string, ServiceProfitWorkQueueItem[]>();
    for (const item of this.sorted(items)) { const key = this.groupKey(item); groups.set(key, [...(groups.get(key) ?? []), item]); }
    return [...groups.entries()].map(([key, groupedItems]) => ({ key, items: groupedItems }));
  }
  private groupKey(item: ServiceProfitWorkQueueItem): string { switch (this.groupBy()) { case 'DUE_STATE': return this.dueStateFor(item); case 'OWNERSHIP': return item.ownerPrincipalId ? 'ASSIGNED' : 'UNASSIGNED'; case 'DISPOSITION': return item.disposition; case 'PRIORITY': return item.priority; default: return 'ALL'; } }
  private sorted(items: ServiceProfitWorkQueueItem[]): ServiceProfitWorkQueueItem[] { return [...items].sort((a, b) => this.sort() === 'PRIORITY' ? this.priorityRank(a.priority) - this.priorityRank(b.priority) : this.sort() === 'DUE_DATE' ? this.dateValue(a.dueAt) - this.dateValue(b.dueAt) : this.sort() === 'OLDEST' ? this.dateValue(a.createdAt) - this.dateValue(b.createdAt) : this.dateValue(b.createdAt) - this.dateValue(a.createdAt)); }
  private priorityRank(value: string): number { return value === 'HIGH' ? 0 : value === 'MEDIUM' ? 1 : 2; }
  private dateValue(value: string | null): number { return value ? new Date(value).getTime() : Number.MAX_SAFE_INTEGER; }
  private updateUrl(queryParams: Record<string, string | null>): void { void this.router.navigate([], { relativeTo: this.route, queryParams, queryParamsHandling: 'merge' }); }
  private readOneOf<T extends string>(value: string | null, values: readonly T[], fallback: T): T { return value && values.includes(value as T) ? value as T : fallback; }
}
