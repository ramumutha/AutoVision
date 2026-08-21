export type CustomerAuthorizationStatus =
  | 'REQUESTED'
  | 'AUTHORIZED'
  | 'DECLINED'
  | 'DEFERRED'
  | 'CANCELLED';

export interface CustomerAuthorization {
  id: string;
  tenantId: string;
  dealerId: string | null;
  branchId: string | null;
  aftersalesCaseId: string;
  serviceQuoteId: string | null;
  authorizationNumber: string;
  authorizationStatus: CustomerAuthorizationStatus;
  customerReference: string | null;
  customerDisplayNameSnapshot: string | null;
  authorizationSummary: string;
  authorizationScopeSnapshot: Record<string, unknown>;
  commercialSnapshot: Record<string, unknown> | null;
  termsSnapshot: string | null;
  disclaimerSnapshot: string | null;
  requestedAt: string;
  decidedAt: string | null;
  decisionChannel: string | null;
  decisionReference: string | null;
  version: number;
  createdByPrincipalId: string | null;
  updatedByPrincipalId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface RequestQuoteCustomerAuthorizationRequest {
  authorizationNumber: string;
  customerReference: string | null;
  customerDisplayNameSnapshot: string | null;
  authorizationSummary: string;
}
