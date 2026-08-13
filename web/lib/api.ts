import type { Vehicle } from "./types";

const API_BASE_URL =
  process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8000";

const DEMO_TENANT_ID = process.env.NEXT_PUBLIC_DEMO_TENANT_ID;

function getTenantHeaders(): HeadersInit {
  if (!DEMO_TENANT_ID) {
    throw new Error(
      "NEXT_PUBLIC_DEMO_TENANT_ID is not configured. Set it in the frontend env before loading the demo UI."
    );
  }

  return {
    "X-Tenant-ID": DEMO_TENANT_ID,
  };
}

export function buildVehicleQuery(params: Record<string, string | boolean | undefined>) {
  const search = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === "") {
      return;
    }

    search.set(key, String(value));
  });

  return search.toString();
}

export async function fetchVehicles(
  params: Record<string, string | boolean | undefined> = {}
): Promise<Vehicle[]> {
  const search = buildVehicleQuery(params);
  const response = await fetch(`${API_BASE_URL}/v1/vehicles${search ? `?${search}` : ""}`, {
    method: "GET",
    headers: {
      ...getTenantHeaders(),
    },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Failed to load vehicles (${response.status})`);
  }

  return (await response.json()) as Vehicle[];
}

export async function fetchVehicleById(vehicleId: string): Promise<Vehicle> {
  const response = await fetch(`${API_BASE_URL}/v1/vehicles/${vehicleId}`, {
    method: "GET",
    headers: {
      ...getTenantHeaders(),
    },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Failed to load vehicle (${response.status})`);
  }

  return (await response.json()) as Vehicle;
}

export { API_BASE_URL, DEMO_TENANT_ID };
