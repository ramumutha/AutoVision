"use client";

import { useEffect, useMemo, useState } from "react";

import { fetchVehicles } from "@/lib/api";
import type { Vehicle } from "@/lib/types";
import { VehicleCard } from "@/components/vehicle-card";

const vehicleClassOptions = ["", "PASSENGER", "HEAVY_SPECIAL"];
const powertrainOptions = ["", "ICE", "EV"];

export function VehicleSearch() {
  const [search, setSearch] = useState("");
  const [vehicleClass, setVehicleClass] = useState("");
  const [powertrain, setPowertrain] = useState("");
  const [isActive, setIsActive] = useState("all");
  const [vehicles, setVehicles] = useState<Vehicle[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const filters = useMemo(
    () => ({
      identifier: search || undefined,
      modelName: search || undefined,
      vehicleClass: vehicleClass || undefined,
      powertrain: powertrain || undefined,
      isActive: isActive === "all" ? undefined : isActive === "true",
    }),
    [search, vehicleClass, powertrain, isActive]
  );

  useEffect(() => {
    let ignore = false;

    async function loadVehicles() {
      setLoading(true);
      setError(null);

      try {
        const data = await fetchVehicles(filters);
        if (!ignore) {
          setVehicles(data);
        }
      } catch (loadError) {
        if (!ignore) {
          setError(loadError instanceof Error ? loadError.message : "Unable to load vehicles.");
          setVehicles([]);
        }
      } finally {
        if (!ignore) {
          setLoading(false);
        }
      }
    }

    loadVehicles();

    return () => {
      ignore = true;
    };
  }, [filters]);

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <header className="mb-8 flex flex-col gap-4 rounded-3xl border border-slate-200 bg-slate-900 px-6 py-7 text-white shadow-sm sm:px-8">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.2em] text-slate-300">AutoVision</p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight text-white">Vehicle Search</h1>
        </div>
        <p className="max-w-2xl text-sm text-slate-300">
          Search and filter the live Sprint 0 vehicle inventory for the active tenant.
        </p>
      </header>

      <section className="rounded-3xl border border-slate-200 bg-white p-4 shadow-sm sm:p-6">
        <div className="grid gap-4 md:grid-cols-[2fr_1fr_1fr_1fr]">
          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Search</span>
            <input
              aria-label="Search vehicles"
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder="Model, id, or reference"
              className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-base text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
            />
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Vehicle class</span>
            <select
              aria-label="Filter by vehicle class"
              value={vehicleClass}
              onChange={(event) => setVehicleClass(event.target.value)}
              className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-base text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
            >
              {vehicleClassOptions.map((option) => (
                <option key={option || "all-class"} value={option}>
                  {option ? option : "All"}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Powertrain</span>
            <select
              aria-label="Filter by powertrain"
              value={powertrain}
              onChange={(event) => setPowertrain(event.target.value)}
              className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-base text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
            >
              {powertrainOptions.map((option) => (
                <option key={option || "all-powertrain"} value={option}>
                  {option ? option : "All"}
                </option>
              ))}
            </select>
          </label>

          <label className="block">
            <span className="mb-2 block text-sm font-medium text-slate-700">Status</span>
            <select
              aria-label="Filter by active status"
              value={isActive}
              onChange={(event) => setIsActive(event.target.value)}
              className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-base text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200"
            >
              <option value="all">All</option>
              <option value="true">Active</option>
              <option value="false">Inactive</option>
            </select>
          </label>
        </div>
      </section>

      <section className="mt-8">
        {loading && (
          <div className="rounded-2xl border border-slate-200 bg-slate-50 px-5 py-8 text-center text-slate-600">
            Loading vehicles...
          </div>
        )}

        {!loading && error && (
          <div className="rounded-2xl border border-red-200 bg-red-50 px-5 py-8 text-center text-red-700">
            {error}
          </div>
        )}

        {!loading && !error && vehicles.length === 0 && (
          <div className="rounded-2xl border border-dashed border-slate-300 bg-slate-50 px-5 py-10 text-center text-slate-600">
            No vehicles match your current search and filters.
          </div>
        )}

        {!loading && !error && vehicles.length > 0 && (
          <ul className="space-y-4">
            {vehicles.map((vehicle) => (
              <VehicleCard key={vehicle.id} vehicle={vehicle} />
            ))}
          </ul>
        )}
      </section>
    </div>
  );
}
