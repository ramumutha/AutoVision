export type ServiceOrderStatus = 'OPEN' | 'IN_PROGRESS' | 'WORK_COMPLETED' | 'CLOSED' | 'CANCELLED';

export interface ServiceOrderResponse {
  id: string;
  orderNumber: string;
  vehicleId: string;
  status: ServiceOrderStatus;
  openedAt: string;
  completedAt?: string | null;
  closedAt?: string | null;
  cancelledAt?: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ServiceJobSummary {
  id: string;
  jobNumber: string;
  summary: string;
}

export interface ServiceLineResponse {
  id: string;
  serviceOrderId: string;
  serviceJobId?: string | null;
  lineNumber: number;
  lineType: string;
  description: string;
  quantity: number;
  unitOfMeasure: string;
  unitPrice?: number | null;
  currencyCode?: string | null;
  netAmount?: number | null;
  taxAmount?: number | null;
  grossAmount?: number | null;
  hasCommercialSnapshot: boolean;
}

export interface ServiceOrderAggregate {
  order: ServiceOrderResponse;
  jobs: ServiceJobSummary[];
  lines: ServiceLineResponse[];
}

export type ServiceOrderSection = 'overview' | 'lines' | 'quotes';