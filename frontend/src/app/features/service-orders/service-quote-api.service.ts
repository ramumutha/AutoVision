import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../core/api/api-client.service';
import { ServiceQuoteSummary } from './service-quote.models';

@Injectable({ providedIn: 'root' })
export class ServiceQuoteApiService {
  private readonly api = inject(ApiClientService);

  listQuotes(orderId: string): Observable<ServiceQuoteSummary[]> {
    return this.api.get<ServiceQuoteSummary[]>(
      `/v1/aftersales/service-orders/${encodeURIComponent(orderId)}/quotes`,
    );
  }
}