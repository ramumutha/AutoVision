"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { useParams } from "next/navigation";

import { fetchVehicleById } from "@/lib/api";
import type { Vehicle } from "@/lib/types";

function formatDate(value?: string | null): string {
  if (!value) {
    return "Not available";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Not available";
  }

  return new Intl.DateTimeFormat("en", {
    dateStyle: "medium",
    timeStyle: "short",
  }).format(date);
}

function formatUsageValue(value: number | null | undefined, suffix: string): string {
  if (value === null || value === undefined) {
    return `No ${suffix}`;
  }

  return `${value.toLocaleString()} ${suffix}`;
}

export function VehicleDetail() {
  const params = useParams<{ vehicleId: string }>();
  const [vehicle, setVehicle] = useState<Vehicle | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    async function loadVehicle() {
      setLoading(true);
      setError(null);

      try {
        const data = await fetchVehicleById(params.vehicleId);
        setVehicle(data);
      } catch (loadError) {
        setError(loadError instanceof Error ? loadError.message : "Vehicle could not be loaded.");
      } finally {
        setLoading(false);
      }
    }

    if (params.vehicleId) {
      loadVehicle();
    }
  }, [params.vehicleId]);

  if (loading) {
    return <div className="px-4 py-12 text-center text-slate-600">Loading vehicle details...</div>;
  }

  if (error || !vehicle) {
    return (
      <div className="mx-auto max-w-3xl px-4 py-12">
        <div className="rounded-2xl border border-red-200 bg-red-50 p-6 text-red-700">
          {error ?? "Vehicle not found."}
        </div>
      </div>
    );
  }

  const primaryIdentifier =
    vehicle.identifiers.find((identifier) => identifier.isPrimary) ?? vehicle.identifiers[0] ?? null;

  const snapshot = vehicle.latestUsageSnapshot;

  return (
    <div className="mx-auto w-full max-w-6xl px-4 py-8 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center justify-between gap-4">
        <Link href="/" className="inline-flex items-center rounded-full border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-blue-500">
          ← Back to search
        </Link>
      </div>

      <header className="rounded-3xl border border-slate-200 bg-slate-900 px-6 py-7 text-white shadow-sm sm:px-8">
        <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.2em] text-slate-300">Vehicle 360</p>
            <h1 className="mt-2 text-3xl font-semibold tracking-tight text-white">
              {vehicle.modelName ?? "Vehicle overview"}
            </h1>
          </div>
          <div className="flex flex-wrap gap-2">
            <span className="rounded-full bg-blue-50 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-blue-700">
              {vehicle.vehicleClass}
            </span>
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs font-semibold uppercase tracking-wide text-slate-700">
              {vehicle.powertrain}
            </span>
          </div>
        </div>
      </header>

      <div className="mt-8 grid gap-6 lg:grid-cols-[1.5fr_0.9fr]">
        <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold text-slate-900">Vehicle details</h2>

          <dl className="mt-6 grid gap-4 sm:grid-cols-2">
            <div className="rounded-xl bg-slate-50 p-4">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Model</dt>
              <dd className="mt-1 text-lg font-semibold text-slate-900">{vehicle.modelName ?? "Unknown"}</dd>
            </div>
            <div className="rounded-xl bg-slate-50 p-4">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Vehicle class</dt>
              <dd className="mt-1 text-lg font-semibold text-slate-900">{vehicle.vehicleClass}</dd>
            </div>
            <div className="rounded-xl bg-slate-50 p-4">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Powertrain</dt>
              <dd className="mt-1 text-lg font-semibold text-slate-900">{vehicle.powertrain}</dd>
            </div>
            <div className="rounded-xl bg-slate-50 p-4">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Year</dt>
              <dd className="mt-1 text-lg font-semibold text-slate-900">{vehicle.year ?? "Unknown"}</dd>
            </div>
            <div className="rounded-xl bg-slate-50 p-4">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Color</dt>
              <dd className="mt-1 text-lg font-semibold text-slate-900">{vehicle.color ?? "Unknown"}</dd>
            </div>
            <div className="rounded-xl bg-slate-50 p-4">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Status</dt>
              <dd className="mt-1 text-lg font-semibold text-slate-900">{vehicle.isActive ? "Active" : "Inactive"}</dd>
            </div>
            <div className="sm:col-span-2 rounded-xl bg-slate-50 p-4">
              <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Identifier</dt>
              <dd className="mt-1 text-lg font-semibold text-slate-900">
                {primaryIdentifier ? `${primaryIdentifier.identifierType}: ${primaryIdentifier.identifierValue}` : "No identifier"}
              </dd>
            </div>
          </dl>
        </section>

        <aside className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <h2 className="text-xl font-semibold text-slate-900">Latest usage</h2>

          {snapshot ? (
            <dl className="mt-6 space-y-4">
              <div className="rounded-xl bg-slate-50 p-4">
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Odometer</dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900">
                  {formatUsageValue(snapshot.odometerKm ?? null, "km")}
                </dd>
              </div>
              <div className="rounded-xl bg-slate-50 p-4">
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Engine hours</dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900">
                  {formatUsageValue(snapshot.engineHours ?? null, "hrs")}
                </dd>
              </div>
              <div className="rounded-xl bg-slate-50 p-4">
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Data source</dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900">{snapshot.dataSource ?? "Unknown"}</dd>
              </div>
              <div className="rounded-xl bg-slate-50 p-4">
                <dt className="text-xs font-medium uppercase tracking-wide text-slate-500">Recorded at</dt>
                <dd className="mt-1 text-lg font-semibold text-slate-900">{formatDate(snapshot.recordedAt)}</dd>
              </div>
            </dl>
          ) : (
            <div className="mt-6 rounded-xl border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600">
              No usage snapshot available.
            </div>
          )}
        </aside>
      </div>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-xl font-semibold text-slate-900">Health</h2>
            <span className="rounded-full bg-amber-50 px-2.5 py-1 text-xs font-medium uppercase tracking-wide text-amber-700">
              Sprint 0
            </span>
          </div>
          <div className="mt-5 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6 text-center text-slate-600">
            No inspection findings yet
          </div>
        </section>

        <section className="rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
          <div className="flex items-center justify-between gap-3">
            <h2 className="text-xl font-semibold text-slate-900">Service history</h2>
            <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium uppercase tracking-wide text-slate-700">
              Sprint 0
            </span>
          </div>
          <div className="mt-5 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-6 text-center text-slate-600">
            No service history yet
          </div>
        </section>
      </div>

      <div className="mt-8 rounded-3xl border border-slate-200 bg-white p-6 shadow-sm">
        <button
          type="button"
          disabled
          aria-disabled="true"
          className="inline-flex items-center rounded-full bg-slate-300 px-4 py-2.5 text-sm font-medium text-slate-700 opacity-70 cursor-not-allowed"
        >
          Start Service Event — Available in Sprint 1
        </button>
      </div>
    </div>
  );
}
