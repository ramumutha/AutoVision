import { HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../core/api/api-client.service';
import {
  ServiceProfitOpportunityFilters,
  ServiceProfitOpportunityPage,
  ServiceProfitOpportunityQuery,
  ServiceProfitDataCapabilityResponse,
  ServiceProfitOpportunityResponse,
  ServiceProfitOpportunitySummary,
  ServiceProfitWorkQueuePage,
  ServiceProfitWorkQueueQuery,
} from './service-profit.models';

@Injectable({ providedIn: 'root' })
export class ServiceProfitApiService {
  private readonly api = inject(ApiClientService);

  getSummary(filters: ServiceProfitOpportunityFilters = {}): Observable<ServiceProfitOpportunitySummary> {
    return this.api.get<ServiceProfitOpportunitySummary>('/v1/service-profit/opportunities/summary', {
      params: this.toParams(filters),
    });
  }

  getOpportunities(query: ServiceProfitOpportunityQuery = {}): Observable<ServiceProfitOpportunityPage> {
    return this.api.get<ServiceProfitOpportunityPage>('/v1/service-profit/opportunities', {
      params: this.toParams(query),
    });
  }

  getOpportunity(opportunityId: string): Observable<ServiceProfitOpportunityResponse> {
    return this.api.get<ServiceProfitOpportunityResponse>(
      `/v1/service-profit/opportunities/${encodeURIComponent(opportunityId)}`,
    );
  }

  getDataCapability(): Observable<ServiceProfitDataCapabilityResponse> {
    return this.api.get<ServiceProfitDataCapabilityResponse>(
      '/v1/service-profit/data-capability',
    );
  }

  getWorkQueue(query: ServiceProfitWorkQueueQuery = {}): Observable<ServiceProfitWorkQueuePage> {
    return this.api.get<ServiceProfitWorkQueuePage>('/v1/service-profit/follow-ups', {
      params: this.toParams(query),
    });
  }
  private toParams(values: ServiceProfitOpportunityQuery): HttpParams {
    return Object.entries(values).reduce(
      (params, [key, value]) => value === undefined ? params : params.set(key, String(value)),
      new HttpParams(),
    );
  }
}