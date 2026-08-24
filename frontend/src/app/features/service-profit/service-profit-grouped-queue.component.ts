import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, effect, inject, input, output, signal } from '@angular/core';
import { Params } from '@angular/router';
import { ApiError } from '../../core/error/api-error';
import { LocalizationService } from '../../core/localization/localization.service';
import { AvFeedbackComponent } from '../../shared/feedback/av-feedback.component';
import { AvStatusComponent, AvStatusTone } from '../../shared/design-system/av-status.component';
import { AvMoneyPipe } from '../../shared/formatting/av-money.pipe';
import { ServiceProfitMobileListComponent } from './service-profit-mobile-list.component';
import { ServiceProfitOpportunityDetailComponent } from './service-profit-opportunity-detail.component';
import {
  ServiceProfitActionability,
  ServiceProfitGroupBy,
  ServiceProfitOpportunityGroup,
  ServiceProfitOpportunityResponse,
  ServiceProfitOpportunitySort,
  ServiceProfitPriority,
} from './service-profit.models';

@Component({
  selector: 'app-service-profit-grouped-queue',
  imports: [DatePipe, AvFeedbackComponent, AvMoneyPipe, AvStatusComponent, ServiceProfitMobileListComponent, ServiceProfitOpportunityDetailComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './service-profit-grouped-queue.component.html',
  styleUrl: './service-profit-grouped-queue.component.scss',
})
export class ServiceProfitGroupedQueueComponent {
  protected readonly localization = inject(LocalizationService);
  protected readonly expandedGroupKeys = signal<ReadonlySet<string>>(new Set());
  private initializedGroupBy: ServiceProfitGroupBy | null = null;
  private initializedQueryKey: string | null = null;
  private knownGroupKeys = new Set<string>();

  readonly groups = input.required<readonly ServiceProfitOpportunityGroup[]>();
  readonly groupBy = input.required<ServiceProfitGroupBy>();
  readonly sort = input.required<ServiceProfitOpportunitySort>();
  readonly queryParams = input<Params>({});
  readonly selectedOpportunityId = input<string | null>(null);
  readonly selected = input<ServiceProfitOpportunityResponse | null>(null);
  readonly detailLoading = input(false);
  readonly detailError = input<ApiError | null>(null);
  readonly opportunitySelected = output<string>();
  readonly sortChanged = output<ServiceProfitOpportunitySort>();

  constructor() {
    effect(() => {
      const groupBy = this.groupBy();
      const groups = this.groups();
      const queryParams = this.queryParams();
      const queryKey = [queryParams['type'], queryParams['priority'], queryParams['actionability'], queryParams['sort']].join('|');
      const currentKeys = new Set(groups.map((group) => group.key));
      if (this.initializedGroupBy !== groupBy || this.initializedQueryKey !== queryKey) {
        this.expandedGroupKeys.set(new Set());
        this.initializedGroupBy = groupBy;
        this.initializedQueryKey = queryKey;
      } else {
        const newKeys = [...currentKeys].filter((key) => !this.knownGroupKeys.has(key));
        if (newKeys.length) this.expandedGroupKeys.update((keys) => new Set([...keys].filter((key) => currentKeys.has(key))));
      }
      this.knownGroupKeys = currentKeys;
    });
  }

  protected isExpanded(key: string): boolean {
    return this.groupBy() === 'NONE' || this.expandedGroupKeys().has(key);
  }

  protected toggleGroup(key: string): void {
    const expanded = new Set(this.expandedGroupKeys());
    if (expanded.has(key)) expanded.delete(key);
    else expanded.add(key);
    this.expandedGroupKeys.set(expanded);
  }

  protected groupId(key: string): string {
    return `service-profit-group-${key.toLowerCase().replaceAll('_', '-')}`;
  }

  protected groupLabel(key: string): string {
    return this.label(key);
  }

  protected showOpportunityType(groupKey: string): boolean {
    return this.groupBy() !== 'OPPORTUNITY_TYPE' || groupKey === 'ALL';
  }

  protected toggleSort(column: 'POTENTIAL' | 'DETECTED'): void {
    const current = this.sort();
    if (column === 'POTENTIAL') {
      this.sortChanged.emit(current === 'POTENTIAL_DESC' ? 'POTENTIAL_ASC' : 'POTENTIAL_DESC');
      return;
    }
    this.sortChanged.emit(current === 'DETECTED_DESC' ? 'DETECTED_ASC' : 'DETECTED_DESC');
  }

  protected ariaSort(column: 'POTENTIAL' | 'DETECTED'): 'ascending' | 'descending' | 'none' {
    const sort = this.sort();
    if (!sort.startsWith(column)) return 'none';
    return sort.endsWith('ASC') ? 'ascending' : 'descending';
  }

  protected sortIndicator(column: 'POTENTIAL' | 'DETECTED'): string {
    const direction = this.ariaSort(column);
    return direction === 'ascending' ? '↑' : direction === 'descending' ? '↓' : '↕';
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

  protected detailRegionId(opportunityId: string): string {
    return `opportunity-detail-${opportunityId}`;
  }

  protected detailErrorMessage(error: ApiError): string {
    if (error.status === 401) return this.localization.text('detailSessionExpired');
    if (error.status === 403) return this.localization.text('detailNotAuthorized');
    return this.localization.text('tryAgain');
  }
}