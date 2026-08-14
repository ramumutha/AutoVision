type StructuredSummarySectionProps = {
  summary: string;
  onChange: (next: string) => void;
  onSave: () => Promise<void> | void;
  isSaving: boolean;
  disabled?: boolean;
  revision?: number;
};

export function StructuredSummarySection({
  summary,
  onChange,
  onSave,
  isSaving,
  disabled,
  revision,
}: StructuredSummarySectionProps) {
  const readOnly = Boolean(disabled);

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.16em] text-slate-500">Service summary</p>
          <h2 className="mt-2 text-xl font-semibold text-slate-900">Structured Service Summary</h2>
        </div>
        {typeof revision === "number" ? (
          <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-700">
            Revision {revision}
          </span>
        ) : null}
      </div>

      <p className="mt-3 text-sm text-slate-600">
        This is a separate operational summary. Original complaint wording is preserved; the structured summary is independently editable.
      </p>

      <textarea
        aria-label="Structured service summary"
        value={summary}
        onChange={(event) => onChange(event.target.value)}
        readOnly={readOnly}
        disabled={readOnly}
        rows={5}
        placeholder="Document the key issue and context in a structured, operational summary..."
        className="mt-4 w-full rounded-2xl border border-slate-300 bg-white px-3 py-3 text-base text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500"
      />

      <div className="mt-5 flex justify-end">
        <button
          type="button"
          onClick={() => void onSave()}
          disabled={isSaving || readOnly}
          className="inline-flex items-center rounded-full bg-blue-600 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-blue-500 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          {isSaving ? "Saving revision..." : "Save Summary Revision"}
        </button>
      </div>
    </section>
  );
}
