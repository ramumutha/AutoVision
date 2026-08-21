import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiClientService } from '../../core/api/api-client.service';
import {
  CustomerAuthorization,
  CustomerAuthorizationDecisionRequest,
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

  authorize(caseId: string, authorizationId: string, request: CustomerAuthorizationDecisionRequest): Observable<CustomerAuthorization> {
    return this.decide(caseId, authorizationId, 'authorize', request);
  }

  decline(caseId: string, authorizationId: string, request: CustomerAuthorizationDecisionRequest): Observable<CustomerAuthorization> {
    return this.decide(caseId, authorizationId, 'decline', request);
  }

  defer(caseId: string, authorizationId: string, request: CustomerAuthorizationDecisionRequest): Observable<CustomerAuthorization> {
    return this.decide(caseId, authorizationId, 'defer', request);
  }

  cancel(caseId: string, authorizationId: string, request: CustomerAuthorizationDecisionRequest): Observable<CustomerAuthorization> {
    return this.decide(caseId, authorizationId, 'cancel', request);
  }

  private decide(
    caseId: string,
    authorizationId: string,
    action: 'authorize' | 'decline' | 'defer' | 'cancel',
    request: CustomerAuthorizationDecisionRequest,
  ): Observable<CustomerAuthorization> {
    return this.api.post<CustomerAuthorization>(
      `/v1/aftersales-cases/${encodeURIComponent(caseId)}/customer-authorizations/${encodeURIComponent(authorizationId)}/${action}`,
      request,
    );
  }
}
