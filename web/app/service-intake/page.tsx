import { redirect } from "next/navigation";

import { ServiceIntakeShell } from "@/components/service-intake/service-intake-shell";
import { fetchVehicleById, getServiceEvent } from "@/lib/api";

export default async function ServiceIntakePage({
  searchParams,
}: {
  searchParams?: Promise<{ vehicleId?: string; eventId?: string }>;
}) {
  const params = (await searchParams) ?? {};
  const vehicleId = params.vehicleId;
  const eventId = params.eventId;

  if (!vehicleId) {
    redirect("/");
  }

  const vehicle = await fetchVehicleById(vehicleId);
  const initialEvent = eventId ? await getServiceEvent(eventId) : null;

  return <ServiceIntakeShell vehicle={vehicle} initialEvent={initialEvent} />;
}
