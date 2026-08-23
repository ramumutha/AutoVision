import { DatePipe, DecimalPipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, DestroyRef, inject, signal } from '@angular/core';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { ServiceProfitApiService } from './service-profit-api.service';
import {
  ServiceProfitActionability,
  ServiceProfitOpportunityFilters,
  ServiceProfitOpportunityQueueItem,
  ServiceProfitOpportunityResponse,
  ServiceProfitOpportunitySummary,
  ServiceProfitOpportunityType,
  ServiceProfitPriority,
} from './service-profit.models';

@Component({
  selector: 'app-service-profit-manager',
  imports: [DatePipe, DecimalPipe, AvFeedbackComponent, AvStatusComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './service-profit-manager.component.html',
  styleUrl: './service-profit-manager.component.scss',
})
export class ServiceProfitManagerComponent {
  private readonly api = inject(ServiceProfitApiService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly localization = inject(LocalizationService);

  protected readonly summary = signal<ServiceProfitOpportunitySummary | null>(null);
  protected readonly queue = signal<ServiceProfitOpportunityQueueItem[]>([]);
  protected readonly loading = signal(true);
  protected readonly loadError = signal<ApiError | null>(null);
  protected readonly filters = signal<ServiceProfitOpportunityFilters>({});
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

  constructor() {
    this.loadManagerData();
  }

  protected refresh(): void {
    this.loadManagerData();
    const selectedId = this.selectedOpportunityId();
    if (selectedId) this.loadDetail(selectedId);
  }

  protected updateFilter(key: keyof ServiceProfitOpportunityFilters, value: string): void {
    this.filters.update((current) => {
      const next = { ...current };
      if (value) {
        (next as Record<string, string>)[key] = value;
      } else {
        delete next[key];
      }
      return next;
    });
    this.loadManagerData();
  }

  protected loadDetail(opportunityId: string): void {
    this.selectedOpportunityId.set(opportunityId);
    this.detailLoading.set(true);
    this.detailError.set(null);
    this.api.getOpportunity(opportunityId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (detail) => {
        this.selected.set(detail);
        this.detailLoading.set(false);
      },
      error: (error: ApiError) => {
        this.detailError.set(error);
        this.detailLoading.set(false);
      },
    });
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

  private loadManagerData(): void {
    this.loading.set(true);
    this.loadError.set(null);
    const filters = this.filters();
    forkJoin({
      summary: this.api.getSummary(filters),
      queue: this.api.getOpportunities({ ...filters, page: 0, size: 25, sort: 'DETECTED_DESC' }),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ summary, queue }) => {
        this.summary.set(summary);
        this.queue.set(queue.items);
        this.loading.set(false);
      },
      error: (error: ApiError) => {
        this.loadError.set(error);
        this.loading.set(false);
      },
    });
  }
}