export type OperationalAuthorizationStatus =
  | 'FULLY_AUTHORIZED'
  | 'PARTIALLY_AUTHORIZED'
  | 'PENDING'
  | 'NOT_AUTHORIZED'
  | 'NOT_REQUIRED';

export interface OperationalAuthorizationEvaluation {
  serviceJobId: string;
  serviceOrderId: string;
  status: OperationalAuthorizationStatus;
  totalLineCount: number;
  authorizedLineCount: number;
  pendingLineCount: number;
  notAuthorizedLineCount: number;
}
