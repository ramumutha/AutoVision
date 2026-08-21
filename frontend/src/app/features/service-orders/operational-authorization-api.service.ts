import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../core/api/api-client.service';
import { OperationalAuthorizationEvaluation } from './operational-authorization.models';

@Injectable({ providedIn: 'root' })
export class OperationalAuthorizationApiService {
  private readonly api = inject(ApiClientService);

  evaluate(orderId: string, jobId: string): Observable<OperationalAuthorizationEvaluation> {
    return this.api.get<OperationalAuthorizationEvaluation>(
      `/v1/aftersales/service-orders/${encodeURIComponent(orderId)}/service-jobs/${encodeURIComponent(jobId)}/authorization-evaluation`,
    );
  }
}
