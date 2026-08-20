import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../core/api/api-client.service';
import { ServiceOrderAggregate } from './service-order.models';

@Injectable({ providedIn: 'root' })
export class ServiceOrderApiService {
  private readonly api = inject(ApiClientService);

  getServiceOrder(orderId: string): Observable<ServiceOrderAggregate> {
    return this.api.get<ServiceOrderAggregate>(`/v1/service-orders/${encodeURIComponent(orderId)}/aggregate`);
  }
}