// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// 7x10 dot grid (submissions-by-day block, 02-bd/screens/admin/admin_overview.md section 3).
// Generic column/intensity list, no domain type — shared/ui.
type DotColumn = { label: string; intensity: number };

const ROWS = 10;

export function DotGrid({
  columns,
  accentColorVar = "--color-primary",
}: {
  columns: DotColumn[];
  /** CSS custom-property name (e.g. "--color-admin-cyan"), read via var(). */
  accentColorVar?: string;
}) {
  return (
    <div className="flex justify-between gap-2" role="img" aria-label="Lượt nộp theo ngày">
      {columns.map((column) => (
        <div key={column.label} className="flex flex-col items-center gap-1">
          <div className="flex flex-col-reverse gap-0.5">
            {Array.from({ length: ROWS }, (_, rowIndex) => (
              <span
                key={rowIndex}
                className="h-1.5 w-1.5 rounded-full"
                style={{
                  background: `var(${accentColorVar})`,
                  opacity: rowIndex < column.intensity ? 0.35 + (rowIndex / ROWS) * 0.65 : 0.12,
                }}
              />
            ))}
          </div>
          <span className="text-[10px] text-[var(--color-text-muted)]">{column.label}</span>
        </div>
      ))}
    </div>
  );
}
