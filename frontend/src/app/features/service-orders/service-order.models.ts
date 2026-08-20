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

export interface ServiceOrderAggregate {
  order: ServiceOrderResponse;
  jobs: unknown[];
  lines: unknown[];
}

export type ServiceOrderSection = 'overview' | 'lines' | 'quotes';