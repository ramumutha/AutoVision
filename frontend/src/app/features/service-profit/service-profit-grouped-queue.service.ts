import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map, of, switchMap } from 'rxjs';
import { ServiceProfitApiService } from './service-profit-api.service';
import {
  ServiceProfitActionability,
  ServiceProfitGroupBy,
  ServiceProfitOpportunityFilters,
  ServiceProfitOpportunityGroup,
  ServiceProfitOpportunityPage,
  ServiceProfitOpportunitySort,
  ServiceProfitOpportunitySummary,
  ServiceProfitOpportunityType,
  ServiceProfitPriority,
} from './service-profit.models';

interface GroupDescriptor {
  key: string;
  authoritativeCount: number;
  filters: ServiceProfitOpportunityFilters;
}

@Injectable({ providedIn: 'root' })
export class ServiceProfitGroupedQueueService {
  private readonly api = inject(ServiceProfitApiService);

  load(
    summary: ServiceProfitOpportunitySummary,
    groupBy: ServiceProfitGroupBy,
    filters: ServiceProfitOpportunityFilters,
    sort: ServiceProfitOpportunitySort,
  ): Observable<ServiceProfitOpportunityGroup[]> {
    const descriptors = this.descriptors(summary, groupBy, filters);
    if (!descriptors.length) return of([]);
    return forkJoin(descriptors.map((descriptor) => this.loadGroup(descriptor, sort)));
  }

  private descriptors(
    summary: ServiceProfitOpportunitySummary,
    groupBy: ServiceProfitGroupBy,
    filters: ServiceProfitOpportunityFilters,
  ): GroupDescriptor[] {
    if (groupBy === 'NONE') {
      return summary.totalOpportunities > 0
        ? [{ key: 'ALL', authoritativeCount: summary.totalOpportunities, filters }]
        : [];
    }

    const facets = groupBy === 'OPPORTUNITY_TYPE'
      ? summary.byOpportunityType
      : groupBy === 'PRIORITY'
        ? summary.byPriority
        : summary.byActionability;

    if (!facets.length && summary.totalOpportunities > 0) {
      return [{ key: 'ALL', authoritativeCount: summary.totalOpportunities, filters }];
    }

    return facets
      .filter((facet) => facet.count > 0)
      .map((facet) => ({
        key: facet.key,
        authoritativeCount: facet.count,
        filters: { ...filters, ...this.groupFilter(groupBy, facet.key) },
      }));
  }

  private loadGroup(
    descriptor: GroupDescriptor,
    sort: ServiceProfitOpportunitySort,
  ): Observable<ServiceProfitOpportunityGroup> {
    return this.api.getOpportunities({ ...descriptor.filters, page: 0, size: 100, sort }).pipe(
      switchMap((firstPage) => {
        if (firstPage.totalPages <= 1) return of([firstPage]);
        const remaining = Array.from({ length: firstPage.totalPages - 1 }, (_, index) => index + 1)
          .map((page) => this.api.getOpportunities({ ...descriptor.filters, page, size: 100, sort }));
        return forkJoin([of(firstPage), ...remaining]);
      }),
      map((pages: ServiceProfitOpportunityPage[]) => ({
        key: descriptor.key,
        authoritativeCount: descriptor.authoritativeCount,
        opportunities: pages.flatMap((page) => page.items),
        complete: pages.reduce((count, page) => count + page.items.length, 0) === descriptor.authoritativeCount,
      })),
    );
  }

  private groupFilter(groupBy: ServiceProfitGroupBy, key: string): ServiceProfitOpportunityFilters {
    if (groupBy === 'OPPORTUNITY_TYPE') return { opportunityType: key as ServiceProfitOpportunityType };
    if (groupBy === 'PRIORITY') return { priority: key as ServiceProfitPriority };
    return { actionability: key as ServiceProfitActionability };
  }
}