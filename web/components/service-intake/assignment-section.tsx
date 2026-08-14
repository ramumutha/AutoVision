import type { ServiceEventAssignment } from "@/lib/types";

type AssignmentSectionProps = {
  roleCode: string;
  userRef: string;
  onRoleChange: (next: string) => void;
  onUserRefChange: (next: string) => void;
  onSave: () => Promise<void> | void;
  isSaving: boolean;
  disabled?: boolean;
  assignments: ServiceEventAssignment[];
};

export function AssignmentSection({
  roleCode,
  userRef,
  onRoleChange,
  onUserRefChange,
  onSave,
  isSaving,
  disabled,
  assignments,
}: AssignmentSectionProps) {
  const readOnly = Boolean(disabled);

  return (
    <section className="rounded-3xl border border-slate-200 bg-white p-5 shadow-sm sm:p-6">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-sm font-medium uppercase tracking-[0.16em] text-slate-500">Assignment</p>
          <h2 className="mt-2 text-xl font-semibold text-slate-900">Service Assignment</h2>
        </div>
      </div>

      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <label className="block">
          <span className="mb-2 block text-sm font-medium text-slate-700">Role</span>
          <input
            value={roleCode}
            onChange={(event) => onRoleChange(event.target.value)}
            disabled={readOnly}
            placeholder="TECHNICIAN"
            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-base text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200 disabled:cursor-not-allowed disabled:bg-slate-100"
          />
        </label>

        <label className="block">
          <span className="mb-2 block text-sm font-medium text-slate-700">User reference (optional)</span>
          <input
            value={userRef}
            onChange={(event) => onUserRefChange(event.target.value)}
            disabled={readOnly}
            placeholder="UUID for the tenant-scoped user ref"
            className="w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 text-base text-slate-900 shadow-sm outline-none transition focus:border-blue-500 focus:ring-2 focus:ring-blue-200 disabled:cursor-not-allowed disabled:bg-slate-100"
          />
        </label>
      </div>

      <div className="mt-5 flex justify-end">
        <button
          type="button"
          onClick={() => void onSave()}
          disabled={isSaving || readOnly || roleCode.trim().length === 0}
          className="inline-flex items-center rounded-full bg-slate-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-slate-700 disabled:cursor-not-allowed disabled:bg-slate-300"
        >
          {isSaving ? "Saving assignment..." : "Add Assignment"}
        </button>
      </div>

      {assignments.length > 0 ? (
        <div className="mt-5 flex flex-wrap gap-2">
          {assignments.map((assignment) => (
            <span
              key={assignment.id}
              className="inline-flex items-center rounded-full border border-slate-200 bg-slate-50 px-3 py-1.5 text-sm font-medium text-slate-700"
            >
              {assignment.roleCode}
              {assignment.userRef ? ` • ${assignment.userRef.slice(0, 8)}` : ""}
            </span>
          ))}
        </div>
      ) : (
        <div className="mt-5 rounded-2xl border border-dashed border-slate-300 bg-slate-50 p-4 text-sm text-slate-600">
          No assignment captured yet.
        </div>
      )}
    </section>
  );
}
