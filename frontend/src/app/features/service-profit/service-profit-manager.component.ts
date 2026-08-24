import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { ParamMap, ActivatedRoute, Router } from '@angular/router';
import { Subject, catchError, distinctUntilChanged, forkJoin, map, merge, of, shareReplay, switchMap, tap, withLatestFrom } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { ServiceProfitApiService } from './service-profit-api.service';
import { ServiceProfitOpportunityControlsComponent } from './service-profit-opportunity-controls.component';
import {
  ServiceProfitActionability,
  ServiceProfitOpportunityFilters,
  ServiceProfitOpportunityQueueItem,
  ServiceProfitOpportunityResponse,
  ServiceProfitOpportunitySort,
  ServiceProfitOpportunitySummary,
  ServiceProfitOpportunityType,
  ServiceProfitPriority,
  ServiceProfitVehicleContext,
} from './service-profit.models';

interface ServiceProfitManagerQueryState {
  filters: ServiceProfitOpportunityFilters;
  sort: ServiceProfitOpportunitySort;
}

@Component({
  selector: 'app-service-profit-manager',
  imports: [CurrencyPipe, DatePipe, DecimalPipe, AvFeedbackComponent, AvStatusComponent, ServiceProfitOpportunityControlsComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './service-profit-manager.component.html',
  styleUrl: './service-profit-manager.component.scss',
})
export class ServiceProfitManagerComponent {
  private readonly api = inject(ServiceProfitApiService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly refreshRequests = new Subject<void>();
  private readonly detailRequests = new Subject<string>();
  protected readonly localization = inject(LocalizationService);

  protected readonly summary = signal<ServiceProfitOpportunitySummary | null>(null);
  protected readonly queue = signal<ServiceProfitOpportunityQueueItem[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<ApiError | null>(null);
  protected readonly refreshing = signal(false);
  protected readonly refreshError = signal<ApiError | null>(null);
  protected readonly filters = signal<ServiceProfitOpportunityFilters>({});
  protected readonly sort = signal<ServiceProfitOpportunitySort>('DETECTED_DESC');
  protected readonly selected = signal<ServiceProfitOpportunityResponse | null>(null);
  protected readonly selectedOpportunityId = signal<string | null>(null);
  protected readonly detailLoading = signal(false);
  protected readonly detailError = signal<ApiError | null>(null);

  protected readonly priorities: ServiceProfitPriority[] = ['HIGH', 'MEDIUM', 'LOW'];
  protected readonly opportunityTypes: ServiceProfitOpportunityType[] = [
    'DECLINED_WORK', 'DEFERRED_WORK', 'DUE_SERVICE', 'OVERDUE_SERVICE', 'INACTIVE_CUSTOMER',
  ];
  protected readonly actionabilities: ServiceProfitActionability[] = [
    'READY', 'REVIEW_REQUIRED', 'CONTACT_DATA_MISSING', 'BLOCKED', 'SUPPRESSED',
  ];
  protected readonly sorts: ReadonlyArray<{ value: ServiceProfitOpportunitySort; labelKey: 'newest' | 'oldest' | 'highestPotential' | 'lowestPotential' }> = [
    { value: 'DETECTED_DESC', labelKey: 'newest' },
    { value: 'DETECTED_ASC', labelKey: 'oldest' },
    { value: 'POTENTIAL_DESC', labelKey: 'highestPotential' },
    { value: 'POTENTIAL_ASC', labelKey: 'lowestPotential' },
  ];

  constructor() {
    const queryState$ = this.route.queryParamMap.pipe(
      map((params) => this.readQueryState(params)),
      distinctUntilChanged((previous, current) => this.queryStateKey(previous) === this.queryStateKey(current)),
      tap((state) => {
        this.filters.set(state.filters);
        this.sort.set(state.sort);
      }),
      shareReplay({ bufferSize: 1, refCount: true }),
    );

    merge(
      queryState$.pipe(map((state) => ({ state, background: false }))),
      this.refreshRequests.pipe(
        withLatestFrom(queryState$),
        map(([, state]) => ({ state, background: true })),
      ),
    ).pipe(
      tap(({ background }) => this.beginManagerLoad(background)),
      switchMap(({ state, background }) => forkJoin({
        summary: this.api.getSummary(state.filters),
        queue: this.api.getOpportunities({ ...state.filters, page: 0, size: 25, sort: state.sort }),
      }).pipe(
        map((data) => ({ background, data, error: null })),
        catchError((error: ApiError) => of({ background, data: null, error })),
      )),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe((result) => this.finishManagerLoad(result));

    this.detailRequests.pipe(
      tap((opportunityId) => {
        this.selectedOpportunityId.set(opportunityId);
        this.detailLoading.set(true);
        this.detailError.set(null);
      }),
      switchMap((opportunityId) => this.api.getOpportunity(opportunityId).pipe(
        map((detail) => ({ detail, error: null })),
        catchError((error: ApiError) => of({ detail: null, error })),
      )),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(({ detail, error }) => {
      if (detail) this.selected.set(detail);
      this.detailError.set(error);
      this.detailLoading.set(false);
    });
  }

  protected refresh(): void {
    this.refreshRequests.next();
    const selectedId = this.selectedOpportunityId();
    if (selectedId) this.loadDetail(selectedId);
  }

  protected updateFilter(key: keyof ServiceProfitOpportunityFilters, value: string): void {
    const queryKey = key === 'opportunityType' ? 'type' : key;
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { [queryKey]: value || null },
      queryParamsHandling: 'merge',
    });
  }

  protected updateSort(value: string): void {
    void this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { sort: this.isSupportedSort(value) ? value : null },
      queryParamsHandling: 'merge',
    });
  }

  protected loadDetail(opportunityId: string): void {
    this.detailRequests.next(opportunityId);
  }

  protected label(value: string): string {
    return value.replaceAll('_', ' ').toLowerCase().replace(/(^|\s)\S/g, (letter) => letter.toUpperCase());
  }

  protected priorityTone(priority: ServiceProfitPriority): AvStatusTone {
    return priority === 'HIGH' ? 'danger' : priority === 'MEDIUM' ? 'warning' : 'neutral';
  }

  protected actionabilityTone(actionability: ServiceProfitActionability): AvStatusTone {
    if (actionability === 'READY') return 'success';
    if (actionability === 'REVIEW_REQUIRED' || actionability === 'CONTACT_DATA_MISSING') return 'warning';
    return actionability === 'SUPPRESSED' ? 'neutral' : 'danger';
  }

  protected isSuppressed(opportunity: ServiceProfitOpportunityResponse): boolean {
    return opportunity.status === 'SUPPRESSED' || opportunity.actionability === 'SUPPRESSED' || !!opportunity.suppressionReason;
  }

  protected requiresReview(opportunity: ServiceProfitOpportunityResponse): boolean {
    return opportunity.actionability === 'REVIEW_REQUIRED';
  }

  protected hasCustomerContext(opportunity: ServiceProfitOpportunityResponse): boolean {
    const customer = opportunity.context?.customer;
    return !!customer && Object.values(customer).some((value) => value !== null && value !== '');
  }

  protected hasVehicleContext(opportunity: ServiceProfitOpportunityResponse): boolean {
    const vehicle = opportunity.context?.vehicle;
    return !!vehicle && Object.values(vehicle).some((value) => value !== null && value !== '');
  }

  protected hasServiceContext(opportunity: ServiceProfitOpportunityResponse): boolean {
    const service = opportunity.context?.service;
    return !!service && Object.values(service).some((value) => value !== null && value !== '');
  }

  protected vehiclePrimary(vehicle: ServiceProfitVehicleContext): string {
    return vehicle.registration
      || [vehicle.make, vehicle.model].filter(Boolean).join(' ')
      || vehicle.vin
      || '';
  }

  protected vehicleDescription(vehicle: ServiceProfitVehicleContext): string {
    return [[vehicle.make, vehicle.model].filter(Boolean).join(' '), vehicle.modelYear]
      .flat()
      .filter(Boolean)
      .join(' · ');
  }

  private readQueryState(params: ParamMap): ServiceProfitManagerQueryState {
    const opportunityType = params.get('type');
    const priority = params.get('priority');
    const actionability = params.get('actionability');
    const sort = params.get('sort');
    const filters: ServiceProfitOpportunityFilters = {};
    if (this.isOneOf(opportunityType, this.opportunityTypes)) filters.opportunityType = opportunityType;
    if (this.isOneOf(priority, this.priorities)) filters.priority = priority;
    if (this.isOneOf(actionability, this.actionabilities)) filters.actionability = actionability;
    return { filters, sort: this.isSupportedSort(sort) ? sort : 'DETECTED_DESC' };
  }

  private queryStateKey(state: ServiceProfitManagerQueryState): string {
    return [state.filters.opportunityType, state.filters.priority, state.filters.actionability, state.sort].join('|');
  }

  private isSupportedSort(value: string | null): value is ServiceProfitOpportunitySort {
    return this.sorts.some((sort) => sort.value === value);
  }

  private isOneOf<T extends string>(value: string | null, choices: readonly T[]): value is T {
    return value !== null && choices.includes(value as T);
  }

  private beginManagerLoad(background: boolean): void {
    this.refreshError.set(null);
    if (background) {
      this.refreshing.set(true);
      return;
    }
    this.refreshing.set(false);
    this.loading.set(true);
    this.loadError.set(null);
  }

  private finishManagerLoad(result: {
    background: boolean;
    data: { summary: ServiceProfitOpportunitySummary; queue: { items: ServiceProfitOpportunityQueueItem[] } } | null;
    error: ApiError | null;
  }): void {
    if (result.background) this.refreshing.set(false);
    else this.loading.set(false);
    if (result.error) {
      if (result.background) this.refreshError.set(result.error);
      else this.loadError.set(result.error);
      return;
    }
    if (result.data) {
      this.summary.set(result.data.summary);
      this.queue.set(result.data.queue.items);
    }
  }
}