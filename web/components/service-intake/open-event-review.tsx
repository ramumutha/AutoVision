import type { ServiceEvent, ServiceEventAssignment, ServiceEventContext } from "@/lib/types";

import { ServiceEventStatus } from "@/components/service-intake/service-event-status";

type OpenEventReviewProps = {
  event: ServiceEvent | null;
  originalComplaint: string;
  structuredSummary: string;
  assignments: ServiceEventAssignment[];
  contexts: ServiceEventContext[];
  onOpen: () => Promise<void> | void;
  isOpening: boolean;
  isDisabled?: boolean;
};

export function OpenEventReview({
  event,
  originalComplaint,
  structuredSummary,
  assignments,
  contexts,
  onOpen,
  isOpening,
  isDisabled,
}: OpenEventReviewProps) {
  const eventState = event?.state ?? "DRAFT";

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.16em] text-slate-500">Ready to open</p>
          <h2 className="mt-2 text-xl font-semibold text-slate-900">Review & Open</h2>
        </div>
        <ServiceEventStatus state={eventState} />
      </div>

      <div className="mt-5 space-y-4">
        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div className="text-xs font-medium uppercase tracking-wide text-slate-500">Vehicle</div>
          <div className="mt-2 font-medium text-slate-900">{event ? event.vehicleId : "Not selected"}</div>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div className="text-xs font-medium uppercase tracking-wide text-slate-500">Original complaint</div>
          <p className="mt-2 whitespace-pre-wrap text-sm text-slate-700">{originalComplaint || "No complaint captured yet."}</p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div className="text-xs font-medium uppercase tracking-wide text-slate-500">Structured summary</div>
          <p className="mt-2 whitespace-pre-wrap text-sm text-slate-700">{structuredSummary || "No structured summary captured yet."}</p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div className="text-xs font-medium uppercase tracking-wide text-slate-500">Assignments</div>
          {assignments.length > 0 ? (
            <div className="mt-2 flex flex-wrap gap-2">
              {assignments.map((assignment) => (
                <span key={assignment.id} className="rounded-full bg-slate-200 px-2.5 py-1 text-xs font-medium text-slate-700">
                  {assignment.roleCode}
                </span>
              ))}
            </div>
          ) : (
            <p className="mt-2 text-sm text-slate-600">No assignment captured.</p>
          )}
        </div>

        <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
          <div className="text-xs font-medium uppercase tracking-wide text-slate-500">Context</div>
          {contexts.length > 0 ? (
            <div className="mt-2 space-y-2">
              {contexts.map((context) => (
                <div key={context.id} className="rounded-xl border border-slate-200 bg-white p-2 text-xs text-slate-600">
                  {context.contextType}
                </div>
              ))}
            </div>
          ) : (
            <p className="mt-2 text-sm text-slate-600">No context captured.</p>
          )}
        </div>
      </div>

      <div className="mt-6 flex justify-end">
        <button
          type="button"
          onClick={() => void onOpen()}
          disabled={isOpening || isDisabled || !event || event.state !== "DRAFT" || !originalComplaint.trim()}
          className="inline-flex items-center rounded-full bg-emerald-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-emerald-500 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          {isOpening ? "Opening service event..." : "Open Service Event"}
        </button>
      </div>
    </section>
  );
}
