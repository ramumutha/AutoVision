import type {
  ServiceEvent,
  ServiceEventAssignment,
  ServiceEventContext,
  Vehicle,
} from "./types";

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

export async function createServiceEvent(payload: {
  vehicleId: string;
  source: string;
  originalComplaint: string;
  structuredSummary?: string;
  language?: string;
  capturedBy?: string;
}): Promise<ServiceEvent> {
  const response = await fetch(`${API_BASE_URL}/v1/service-events`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...getTenantHeaders(),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `Failed to create service event (${response.status})`);
  }

  return (await response.json()) as ServiceEvent;
}

export async function fetchServiceEventsForVehicle(vehicleId: string): Promise<ServiceEvent[]> {
  const response = await fetch(`${API_BASE_URL}/v1/service-events?vehicleId=${encodeURIComponent(vehicleId)}`, {
    method: "GET",
    headers: {
      ...getTenantHeaders(),
    },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Failed to load service events (${response.status})`);
  }

  return (await response.json()) as ServiceEvent[];
}

export async function getServiceEvent(eventId: string): Promise<ServiceEvent> {
  const response = await fetch(`${API_BASE_URL}/v1/service-events/${eventId}`, {
    method: "GET",
    headers: {
      ...getTenantHeaders(),
    },
    cache: "no-store",
  });

  if (!response.ok) {
    throw new Error(`Failed to load service event (${response.status})`);
  }

  return (await response.json()) as ServiceEvent;
}

export async function reviseComplaint(
  eventId: string,
  payload: { originalComplaint?: string; structuredSummary?: string; language?: string; capturedBy?: string }
): Promise<ServiceEvent> {
  const response = await fetch(`${API_BASE_URL}/v1/service-events/${eventId}/complaint`, {
    method: "PATCH",
    headers: {
      "Content-Type": "application/json",
      ...getTenantHeaders(),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to revise complaint (${response.status})`);
  }

  return (await response.json()) as ServiceEvent;
}

export async function createServiceEventAssignment(
  eventId: string,
  payload: { roleCode: string; userRef?: string }
): Promise<ServiceEventAssignment> {
  const response = await fetch(`${API_BASE_URL}/v1/service-events/${eventId}/assignments`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...getTenantHeaders(),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to create assignment (${response.status})`);
  }

  return (await response.json()) as ServiceEventAssignment;
}

export async function createServiceEventContext(
  eventId: string,
  payload: { contextType: string; sourceRef?: string; snapshotJson: Record<string, unknown> }
): Promise<ServiceEventContext> {
  const response = await fetch(`${API_BASE_URL}/v1/service-events/${eventId}/contexts`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...getTenantHeaders(),
    },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to capture context (${response.status})`);
  }

  return (await response.json()) as ServiceEventContext;
}

export async function openServiceEvent(eventId: string): Promise<ServiceEvent> {
  const response = await fetch(`${API_BASE_URL}/v1/service-events/${eventId}/open`, {
    method: "POST",
    headers: {
      ...getTenantHeaders(),
    },
  });

  if (!response.ok) {
    const text = await response.text();
    throw new Error(text || `Failed to open service event (${response.status})`);
  }

  return (await response.json()) as ServiceEvent;
}

export { API_BASE_URL, DEMO_TENANT_ID };
