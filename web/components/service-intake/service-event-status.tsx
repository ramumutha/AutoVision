export type ServiceEventStatusProps = {
  state: "DRAFT" | "OPEN";
  compact?: boolean;
};

export function ServiceEventStatus({ state, compact = false }: ServiceEventStatusProps) {
  const tone = state === "OPEN"
    ? "bg-emerald-50 text-emerald-700 ring-emerald-200"
    : "bg-amber-50 text-amber-700 ring-amber-200";

  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold uppercase tracking-[0.18em] ring-1 ${tone}`}
    >
      {compact ? state : `Service Event ${state}`}
    </span>
  );
}
