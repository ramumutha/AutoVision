"use client";

import { useMemo, useState } from "react";
import Link from "next/link";

import { AssignmentSection } from "@/components/service-intake/assignment-section";
import { ComplaintSection } from "@/components/service-intake/complaint-section";
import { ContextSection } from "@/components/service-intake/context-section";
import { OpenEventReview } from "@/components/service-intake/open-event-review";
import { ServiceEventStatus } from "@/components/service-intake/service-event-status";
import { StructuredSummarySection } from "@/components/service-intake/structured-summary-section";
import {
  createServiceEvent,
  createServiceEventAssignment,
  createServiceEventContext,
  getServiceEvent,
  openServiceEvent,
  reviseComplaint,
} from "@/lib/api";
import type { ServiceEvent, Vehicle } from "@/lib/types";

type ServiceIntakeShellProps = {
  vehicle: Vehicle;
  initialEvent?: ServiceEvent | null;
};

export function ServiceIntakeShell({ vehicle, initialEvent = null }: ServiceIntakeShellProps) {
  const [event, setEvent] = useState<ServiceEvent | null>(initialEvent);
  const [draftComplaint, setDraftComplaint] = useState(initialEvent?.complaint?.originalText ?? "");
  const [structuredSummary, setStructuredSummary] = useState(initialEvent?.complaint?.structuredSummary ?? "");
  const [assignmentRole, setAssignmentRole] = useState("");
  const [assignmentUserRef, setAssignmentUserRef] = useState("");
  const [contextType, setContextType] = useState("DIAGNOSTIC_SNAPSHOT");
  const [contextSourceRef, setContextSourceRef] = useState("");
  const defaultContextJson = JSON.stringify({
    vehicle: { odometerKm: vehicle.latestUsageSnapshot?.odometerKm ?? null },
    source: "service-intake",
  }, null, 2);

  const [contextSnapshotJson, setContextSnapshotJson] = useState(defaultContextJson);
  const [loadingEvent, setLoadingEvent] = useState(false);
  const [savingComplaint, setSavingComplaint] = useState(false);
  const [savingSummary, setSavingSummary] = useState(false);
  const [savingAssignment, setSavingAssignment] = useState(false);
  const [savingContext, setSavingContext] = useState(false);
  const [openingEvent, setOpeningEvent] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const assignments = event?.assignments ?? [];
  const contexts = event?.contexts ?? [];

  async function handleCreateDraft() {
    if (!draftComplaint.trim()) {
      setErrorMessage("Original complaint is required before creating the draft.");
      return;
    }

    setLoadingEvent(true);
    setErrorMessage(null);
    setStatusMessage(null);

    try {
      const next = await createServiceEvent({
        vehicleId: vehicle.id,
        source: "web-service-intake",
        originalComplaint: draftComplaint,
        structuredSummary: structuredSummary || undefined,
        language: "en-US",
        capturedBy: "demo-service-advisor",
      });

      setEvent(next);
      setDraftComplaint(next.complaint?.originalText ?? draftComplaint);
      setStructuredSummary(next.complaint?.structuredSummary ?? structuredSummary);
      setStatusMessage(`Draft created. Event revision ${next.revision}.`);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Unable to create the service event.");
    } finally {
      setLoadingEvent(false);
    }
  }

  async function handleSaveComplaint() {
    if (!event || !draftComplaint.trim()) {
      setErrorMessage("Create a draft before saving the complaint.");
      return;
    }

    setSavingComplaint(true);
    setErrorMessage(null);

    try {
      const updated = await reviseComplaint(event.id, { originalComplaint: draftComplaint });
      setEvent(updated);
      setDraftComplaint(updated.complaint?.originalText ?? draftComplaint);
      setStatusMessage(`Complaint saved. Revision ${updated.complaint?.revision ?? 1}.`);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Unable to save the complaint.");
    } finally {
      setSavingComplaint(false);
    }
  }

  async function handleSaveSummary() {
    if (!event) {
      setErrorMessage("Create or load a service event before saving the summary.");
      return;
    }

    setSavingSummary(true);
    setErrorMessage(null);

    try {
      const updated = await reviseComplaint(event.id, { structuredSummary });
      setEvent(updated);
      setStructuredSummary(updated.complaint?.structuredSummary ?? structuredSummary);
      setStatusMessage(`Structured summary saved. Revision ${updated.complaint?.revision ?? 1}.`);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Unable to save the summary revision.");
    } finally {
      setSavingSummary(false);
    }
  }

  async function handleCreateAssignment() {
    if (!event || !assignmentRole.trim()) {
      setErrorMessage("A role code is required before creating an assignment.");
      return;
    }

    setSavingAssignment(true);
    setErrorMessage(null);

    try {
      const next = await createServiceEventAssignment(event.id, {
        roleCode: assignmentRole.trim(),
        userRef: assignmentUserRef.trim() ? assignmentUserRef.trim() : undefined,
      });
      const refreshed = await getServiceEvent(event.id);
      setEvent(refreshed);
      setAssignmentRole("");
      setAssignmentUserRef("");
      setStatusMessage(`Assignment saved for ${next.roleCode}.`);
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Unable to create the assignment.");
    } finally {
      setSavingAssignment(false);
    }
  }

  async function handleCreateContext() {
    if (!event || !contextType.trim()) {
      setErrorMessage("A context type is required before capturing context.");
      return;
    }

    try {
      JSON.parse(contextSnapshotJson);
    } catch {
      setErrorMessage("Snapshot JSON must be valid JSON.");
      return;
    }

    setSavingContext(true);
    setErrorMessage(null);

    try {
      await createServiceEventContext(event.id, {
        contextType: contextType.trim(),
        sourceRef: contextSourceRef.trim() || undefined,
        snapshotJson: JSON.parse(contextSnapshotJson),
      });
      const refreshed = await getServiceEvent(event.id);
      setEvent(refreshed);
      setContextType("DIAGNOSTIC_SNAPSHOT");
      setContextSourceRef("");
      setContextSnapshotJson(defaultContextJson);
      setStatusMessage("Context captured successfully.");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Unable to capture the context snapshot.");
    } finally {
      setSavingContext(false);
    }
  }

  async function handleOpenServiceEvent() {
    if (!event) {
      setErrorMessage("Create a draft before opening the service event.");
      return;
    }

    setOpeningEvent(true);
    setErrorMessage(null);

    try {
      const opened = await openServiceEvent(event.id);
      setEvent(opened);
      setStatusMessage("Service event opened successfully.");
    } catch (error) {
      setErrorMessage(error instanceof Error ? error.message : "Unable to open the service event.");
    } finally {
      setOpeningEvent(false);
    }
  }

  const displayComplaint = event?.complaint?.originalText ?? draftComplaint;
  const displaySummary = event?.complaint?.structuredSummary ?? structuredSummary;

  const workflowHint = useMemo(() => {
    if (!event) return "1. Complaint";
    if (event.state === "DRAFT") return "2. Review";
    return "3. Open";
  }, [event]);

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 sm:px-6 lg:px-8">
      <header className="rounded-3xl border border-slate-200 bg-slate-900 px-6 py-6 text-white shadow-sm sm:px-8">
        <div className="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <div className="flex items-center gap-3">
              <Link href={`/vehicles/${vehicle.id}`} className="text-sm font-medium text-slate-300 hover:text-white">
                ← Back to Vehicle 360
              </Link>
            </div>
            <div className="mt-3 flex flex-col gap-2 sm:flex-row sm:items-center sm:gap-4">
              <h1 className="text-3xl font-semibold tracking-tight text-white">Service Intake</h1>
              {event ? <ServiceEventStatus state={event.state} /> : <ServiceEventStatus state="DRAFT" />}
            </div>
          </div>
          <div className="flex flex-wrap gap-3 text-sm text-slate-300">
            <span className="rounded-full bg-slate-800 px-3 py-1.5">{vehicle.modelName ?? "Vehicle"}</span>
            <span className="rounded-full bg-slate-800 px-3 py-1.5">{vehicle.vehicleClass}</span>
            <span className="rounded-full bg-slate-800 px-3 py-1.5">{vehicle.powertrain}</span>
          </div>
        </div>
      </header>

      <div className="mt-6 flex items-center gap-3 text-sm text-slate-600">
        <span className="font-medium text-slate-700">Workflow</span>
        <span className="rounded-full bg-slate-200 px-2.5 py-1 text-slate-700">{workflowHint}</span>
      </div>

      {statusMessage ? (
        <div className="mt-4 rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-3 text-sm text-emerald-700">
          {statusMessage}
        </div>
      ) : null}

      {errorMessage ? (
        <div className="mt-4 rounded-2xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
          {errorMessage}
        </div>
      ) : null}

      <div className="mt-8 grid gap-6 xl:grid-cols-[1.6fr_0.9fr]">
        <div className="space-y-6">
          {!event ? (
            <ComplaintSection
              complaint={draftComplaint}
              onChange={setDraftComplaint}
              onSave={handleCreateDraft}
              isSaving={loadingEvent}
              complaintRevision={1}
            />
          ) : (
            <ComplaintSection
              complaint={displayComplaint}
              onChange={setDraftComplaint}
              onSave={handleSaveComplaint}
              isSaving={savingComplaint}
              disabled={event.state === "OPEN"}
              eventState={event.state}
              eventRevision={event.revision}
              complaintRevision={event.complaint?.revision}
            />
          )}

          <StructuredSummarySection
            summary={displaySummary}
            onChange={setStructuredSummary}
            onSave={handleSaveSummary}
            isSaving={savingSummary}
            disabled={!event || event.state === "OPEN"}
            revision={event?.complaint?.revision}
          />

          <AssignmentSection
            roleCode={assignmentRole}
            userRef={assignmentUserRef}
            onRoleChange={setAssignmentRole}
            onUserRefChange={setAssignmentUserRef}
            onSave={handleCreateAssignment}
            isSaving={savingAssignment}
            disabled={!event || event.state === "OPEN"}
            assignments={assignments}
          />

          <ContextSection
            contextType={contextType}
            sourceRef={contextSourceRef}
            snapshotJson={contextSnapshotJson}
            onContextTypeChange={setContextType}
            onSourceRefChange={setContextSourceRef}
            onSnapshotJsonChange={setContextSnapshotJson}
            onSave={handleCreateContext}
            isSaving={savingContext}
            disabled={!event || event.state === "OPEN"}
            contexts={contexts}
          />
        </div>

        <div className="space-y-6">
          <OpenEventReview
            event={event}
            originalComplaint={displayComplaint}
            structuredSummary={displaySummary}
            assignments={assignments}
            contexts={contexts}
            onOpen={handleOpenServiceEvent}
            isOpening={openingEvent}
            isDisabled={!event || event.state !== "DRAFT"}
          />

          {event ? (
            <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
              <p className="text-sm font-medium uppercase tracking-[0.16em] text-slate-500">Event metadata</p>
              <dl className="mt-4 space-y-3 text-sm text-slate-700">
                <div className="flex items-center justify-between gap-3">
                  <dt>State</dt>
                  <dd><ServiceEventStatus state={event.state} compact /></dd>
                </div>
                <div className="flex items-center justify-between gap-3">
                  <dt>Revision</dt>
                  <dd>{event.revision}</dd>
                </div>
                <div className="flex items-center justify-between gap-3">
                  <dt>Opened</dt>
                  <dd>{event.openedAt ? new Date(event.openedAt).toLocaleString() : "—"}</dd>
                </div>
                <div className="flex items-center justify-between gap-3">
                  <dt>Created</dt>
                  <dd>{new Date(event.createdAt).toLocaleString()}</dd>
                </div>
              </dl>
            </section>
          ) : null}
        </div>
      </div>
    </div>
  );
}
