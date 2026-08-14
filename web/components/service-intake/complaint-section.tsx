type ComplaintSectionProps = {
  complaint: string;
  onChange: (next: string) => void;
  onSave: () => Promise<void> | void;
  isSaving: boolean;
  disabled?: boolean;
  eventState?: "DRAFT" | "OPEN";
  eventRevision?: number;
  complaintRevision?: number;
};

export function ComplaintSection({
  complaint,
  onChange,
  onSave,
  isSaving,
  disabled,
  eventState,
  eventRevision,
  complaintRevision,
}: ComplaintSectionProps) {
  const readOnly = Boolean(disabled || eventState === "OPEN");

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.16em] text-slate-500">Customer complaint</p>
          <h2 className="mt-2 text-xl font-semibold text-slate-900">Original Customer Complaint</h2>
        </div>
        {typeof complaintRevision === "number" ? (
          <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium text-slate-700">
            Revision {complaintRevision}
          </span>
        ) : null}
      </div>

      <p className="mt-3 text-sm text-slate-600">
        Preserved as captured from the customer. This wording is legally and audit important and is not rewritten by the summary.
      </p>

      <textarea
        aria-label="Original customer complaint"
        value={complaint}
        onChange={(event) => onChange(event.target.value)}
        readOnly={readOnly}
        disabled={readOnly}
        rows={7}
        placeholder="Describe the customer concern in their exact words or as captured in the service conversation..."
        className="mt-4 w-full rounded-2xl border border-slate-300 bg-white px-3 py-3 text-base text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200 disabled:cursor-not-allowed disabled:bg-slate-100 disabled:text-slate-500"
      />

      <div className="mt-3 flex items-center justify-between gap-3 text-xs text-slate-500">
        <span>{complaint.length} characters</span>
        {typeof eventRevision === "number" ? <span>Event revision {eventRevision}</span> : null}
      </div>

      <div className="mt-5 flex justify-end">
        <button
          type="button"
          onClick={() => void onSave()}
          disabled={isSaving || readOnly || complaint.trim().length === 0}
          className="inline-flex items-center rounded-full bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          {isSaving ? "Saving..." : eventState === "OPEN" ? "Complaint locked" : "Save Draft"}
        </button>
      </div>
    </section>
  );
}
