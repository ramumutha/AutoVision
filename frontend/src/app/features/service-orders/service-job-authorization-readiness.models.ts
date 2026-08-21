export type ServiceJobAuthorizationReadinessReason =
  | 'AUTHORIZATION_NOT_REQUIRED'
  | 'FULLY_AUTHORIZED'
  | 'PARTIAL_AUTHORIZATION'
  | 'AUTHORIZATION_PENDING'
  | 'AUTHORIZATION_MISSING';

export interface ServiceJobAuthorizationReadiness {
  serviceJobId: string;
  serviceOrderId: string;
  ready: boolean;
  operationalAuthorizationStatus: import('./operational-authorization.models').OperationalAuthorizationStatus;
  reason: ServiceJobAuthorizationReadinessReason;
}
