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
