// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Reused twice per BD: difficulty breakdown (3 groups x 2 bars) and new/returning users
// (6 groups x 2 bars) — 02-bd/screens/admin/admin_overview.md section 3. Generic group/bar list,
// no domain type — shared/ui.
type Bar = { label: string; value: number; colorVar: string };
type Group = { label: string; bars: Bar[] };

export function GroupedBarChart({ groups, legend }: { groups: Group[]; legend: Bar[] }) {
  const max = Math.max(...groups.flatMap((g) => g.bars.map((b) => b.value)), 1);

  return (
    <div>
      <div className="mb-3 flex flex-wrap gap-3 text-xs text-[var(--color-text-muted)]">
        {legend.map((item) => (
          <span key={item.label} className="flex items-center gap-1.5">
            <span className="h-2 w-2 rounded-full" style={{ background: `var(${item.colorVar})` }} aria-hidden="true" />
            {item.label}
          </span>
        ))}
      </div>
      <div className="flex h-24 items-end justify-around gap-3">
        {groups.map((group) => (
          <div key={group.label} className="flex flex-1 flex-col items-center gap-1">
            <div className="flex h-16 items-end gap-1">
              {group.bars.map((bar) => (
                <div
                  key={bar.label}
                  className="w-3 rounded-t-sm"
                  style={{ height: `${(bar.value / max) * 100}%`, background: `var(${bar.colorVar})`, minHeight: 2 }}
                  title={`${bar.label}: ${bar.value}`}
                />
              ))}
            </div>
            <span className="text-xs text-[var(--color-text-muted)]">{group.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
