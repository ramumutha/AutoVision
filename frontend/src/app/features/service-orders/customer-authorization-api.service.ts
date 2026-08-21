import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../core/api/api-client.service';
import {
  CustomerAuthorization,
  RequestQuoteCustomerAuthorizationRequest,
} from './customer-authorization.models';

@Injectable({ providedIn: 'root' })
export class CustomerAuthorizationApiService {
  private readonly api = inject(ApiClientService);

  requestFromServiceQuote(
    caseId: string,
    quoteId: string,
    request: RequestQuoteCustomerAuthorizationRequest,
  ): Observable<CustomerAuthorization> {
    return this.api.post<CustomerAuthorization>(
      `/v1/aftersales-cases/${encodeURIComponent(caseId)}/service-quotes/${encodeURIComponent(quoteId)}/customer-authorization`,
      request,
    );
  }

  listForServiceQuote(caseId: string, quoteId: string): Observable<CustomerAuthorization[]> {
    return this.api.get<CustomerAuthorization[]>(
      `/v1/aftersales-cases/${encodeURIComponent(caseId)}/service-quotes/${encodeURIComponent(quoteId)}/customer-authorizations`,
    );
  }
}
