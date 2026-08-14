export type VehicleIdentifier = {
  id: string;
  identifierType: string;
  identifierValue: string;
  isPrimary: boolean;
};

export type UsageSnapshot = {
  id: string;
  recordedAt: string;
  odometerKm?: number | null;
  engineHours?: number | null;
  fuelLevelPct?: number | null;
  dataSource?: string | null;
  payload?: Record<string, unknown> | null;
};

export type Vehicle = {
  id: string;
  vehicleClass: string;
  powertrain: string;
  modelName?: string | null;
  year?: number | null;
  color?: string | null;
  isActive: boolean;
  identifiers: VehicleIdentifier[];
  latestUsageSnapshot?: UsageSnapshot | null;
};

export type VehicleListFilters = {
  identifier?: string;
  modelName?: string;
  vehicleClass?: string;
  powertrain?: string;
  isActive?: boolean;
};

export type Complaint = {
  id: string;
  originalText: string;
  structuredSummary?: string | null;
  language?: string | null;
  capturedBy?: string | null;
  capturedAt: string;
  revision: number;
};

export type ServiceEventAssignment = {
  id: string;
  eventId: string;
  roleCode: string;
  userRef?: string | null;
  assignedAt: string;
};

export type ServiceEventContext = {
  id: string;
  eventId: string;
  contextType: string;
  sourceRef?: string | null;
  snapshotJson: Record<string, unknown>;
  capturedAt: string;
};

export type ServiceEventState = "DRAFT" | "OPEN";

export type ServiceEvent = {
  id: string;
  vehicleId: string;
  source: string;
  state: ServiceEventState;
  revision: number;
  openedAt?: string | null;
  createdAt: string;
  updatedAt: string;
  complaint?: Complaint | null;
  assignments: ServiceEventAssignment[];
  contexts: ServiceEventContext[];
};
