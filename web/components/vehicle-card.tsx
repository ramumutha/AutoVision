import Link from "next/link";

import type { Vehicle } from "@/lib/types";

function formatPrimaryIdentifier(vehicle: Vehicle): string {
  const primary = vehicle.identifiers.find((identifier) => identifier.isPrimary) ?? vehicle.identifiers[0];
  return primary ? `${primary.identifierType}: ${primary.identifierValue}` : "No identifier";
}

function formatUsageSummary(vehicle: Vehicle): string {
  const snapshot = vehicle.latestUsageSnapshot;

  if (!snapshot) {
    return "No usage snapshot";
  }

  const odometer = snapshot.odometerKm !== null && snapshot.odometerKm !== undefined
    ? `${snapshot.odometerKm.toLocaleString()} km`
    : "Odometer unavailable";

  const engineHours = snapshot.engineHours !== null && snapshot.engineHours !== undefined
    ? `${snapshot.engineHours.toLocaleString()} hrs`
    : "Runtime unavailable";

  return `${odometer} • ${engineHours}`;
}

function formatFreshness(recordedAt?: string | null): string {
  if (!recordedAt) {
    return "Data not available";
  }

  const date = new Date(recordedAt);
  if (Number.isNaN(date.getTime())) {
    return "Data not available";
  }

  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

export function VehicleCard({ vehicle }: { vehicle: Vehicle }) {
  return (
    <li className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm transition hover:border-slate-300 hover:shadow-md focus-within:ring-2 focus-within:ring-blue-500">
      <Link href={`/vehicles/${vehicle.id}`} className="block focus:outline-none" aria-label={`Open vehicle details for ${vehicle.modelName ?? "vehicle"}`}>
        <div className="flex flex-col gap-4 sm:flex-row sm:items-start sm:justify-between">
          <div className="space-y-3">
            <div className="flex items-center gap-2">
              <span className="inline-flex rounded-full bg-blue-50 px-2.5 py-1 text-xs font-semibold uppercase tracking-wide text-blue-700">
                {vehicle.vehicleClass}
              </span>
              <span className="inline-flex rounded-full bg-slate-100 px-2.5 py-1 text-xs font-semibold uppercase tracking-wide text-slate-700">
                {vehicle.powertrain}
              </span>
            </div>

            <div>
              <h3 className="text-xl font-semibold text-slate-900">{vehicle.modelName ?? "Unnamed model"}</h3>
              <p className="mt-1 text-sm text-slate-600">
                {vehicle.year ?? "Year unavailable"} • {vehicle.color ?? "Color unavailable"}
              </p>
            </div>
          </div>

          <div className="rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-right text-sm text-slate-700">
            <div className="font-medium text-slate-900">{vehicle.isActive ? "Active" : "Inactive"}</div>
            <div className="mt-1">{formatPrimaryIdentifier(vehicle)}</div>
          </div>
        </div>

        <dl className="mt-4 grid gap-3 text-sm text-slate-600 sm:grid-cols-3">
          <div className="rounded-lg bg-slate-50 p-3">
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Primary ID</dt>
            <dd className="mt-1 font-medium text-slate-900">{formatPrimaryIdentifier(vehicle)}</dd>
          </div>
          <div className="rounded-lg bg-slate-50 p-3">
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Usage</dt>
            <dd className="mt-1 font-medium text-slate-900">{formatUsageSummary(vehicle)}</dd>
          </div>
          <div className="rounded-lg bg-slate-50 p-3">
            <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Freshness</dt>
            <dd className="mt-1 font-medium text-slate-900">{formatFreshness(vehicle.latestUsageSnapshot?.recordedAt)}</dd>
          </div>
        </dl>
      </Link>
    </li>
  );
}
