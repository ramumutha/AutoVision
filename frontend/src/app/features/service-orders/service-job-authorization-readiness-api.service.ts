import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../core/api/api-client.service';
import { ServiceJobAuthorizationReadiness } from './service-job-authorization-readiness.models';

@Injectable({ providedIn: 'root' })
export class ServiceJobAuthorizationReadinessApiService {
  private readonly api = inject(ApiClientService);

  evaluate(orderId: string, jobId: string): Observable<ServiceJobAuthorizationReadiness> {
    return this.api.get<ServiceJobAuthorizationReadiness>(
      `/v1/aftersales/service-orders/${encodeURIComponent(orderId)}/service-jobs/${encodeURIComponent(jobId)}/authorization-readiness`,
    );
  }
}
