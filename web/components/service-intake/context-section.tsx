import type { ServiceEventContext } from "@/lib/types";

type ContextSectionProps = {
  contextType: string;
  sourceRef: string;
  snapshotJson: string;
  onContextTypeChange: (next: string) => void;
  onSourceRefChange: (next: string) => void;
  onSnapshotJsonChange: (next: string) => void;
  onSave: () => Promise<void> | void;
  isSaving: boolean;
  disabled?: boolean;
  contexts: ServiceEventContext[];
};

export function ContextSection({
  contextType,
  sourceRef,
  snapshotJson,
  onContextTypeChange,
  onSourceRefChange,
  onSnapshotJsonChange,
  onSave,
  isSaving,
  disabled,
  contexts,
}: ContextSectionProps) {
  const readOnly = Boolean(disabled);

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.16em] text-slate-500">Service context</p>
          <h2 className="mt-2 text-xl font-semibold text-slate-900">Captured Context</h2>
        </div>
      </div>

      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <label className="block">
          <span className="mb-2 block text-sm font-medium text-slate-700">Context type</span>
          <input
            value={contextType}
            onChange={(event) => onContextTypeChange(event.target.value)}
            disabled={readOnly}
            placeholder="DIAGNOSTIC_SNAPSHOT"
            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-base text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200 disabled:cursor-not-allowed disabled:bg-slate-100"
          />
        </label>

        <label className="block">
          <span className="mb-2 block text-sm font-medium text-slate-700">Source reference</span>
          <input
            value={sourceRef}
            onChange={(event) => onSourceRefChange(event.target.value)}
            disabled={readOnly}
            placeholder="dashboard-1"
            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-base text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200 disabled:cursor-not-allowed disabled:bg-slate-100"
          />
        </label>
      </div>

      <label className="mt-4 block">
        <span className="mb-2 block text-sm font-medium text-slate-700">Snapshot payload (JSON)</span>
        <textarea
          aria-label="Service context JSON payload"
          value={snapshotJson}
          onChange={(event) => onSnapshotJsonChange(event.target.value)}
          disabled={readOnly}
          rows={6}
          placeholder='{"vehicle":{"odometerKm":12345},"signals":["battery"]}'
          className="w-full rounded-2xl border border-slate-300 bg-white px-3 py-3 font-mono text-sm text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200 disabled:cursor-not-allowed disabled:bg-slate-100"
        />
      </label>

      <div className="mt-5 flex justify-end">
        <button
          type="button"
          onClick={() => void onSave()}
          disabled={isSaving || readOnly || contextType.trim().length === 0 || snapshotJson.trim().length === 0}
          className="inline-flex items-center rounded-full bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          {isSaving ? "Capturing context..." : "Capture Context"}
        </button>
      </div>

      {contexts.length > 0 ? (
        <div className="mt-5 space-y-3">
          {contexts.map((context) => (
            <div key={context.id} className="rounded-2xl border border-slate-200 bg-slate-50 p-3 text-sm text-slate-700">
              <div className="flex items-center justify-between gap-3">
                <strong className="font-semibold text-slate-900">{context.contextType}</strong>
                <span className="text-xs text-slate-500">{new Date(context.capturedAt).toLocaleString()}</span>
              </div>
              {context.sourceRef ? <div className="mt-2 text-xs text-slate-500">Source: {context.sourceRef}</div> : null}
              <pre className="mt-2 overflow-x-auto rounded-xl bg-white p-3 text-xs text-slate-700">
                {JSON.stringify(context.snapshotJson, null, 2)}
              </pre>
            </div>
          ))}
        </div>
      ) : (
        <div className="mt-5 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600">
          No service context captured yet.
        </div>
      )}
    </section>
  );
}
