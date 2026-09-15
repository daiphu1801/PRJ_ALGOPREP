// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Ranked list with a proportional bar per item (top-problems block, the ONLY block with a
// "View all" link onward to problem_management —
// 02-bd/screens/admin/admin_overview.md section 2 point 5). Generic item list in, the link is a
// plain href/label pair — no domain type — shared/ui.
import Link from "next/link";

type RankedItem = { label: string; value: number };

export function RankedProgressList({
  items,
  viewAllHref,
  viewAllLabel,
  accentColorVar = "--color-primary",
}: {
  items: RankedItem[];
  viewAllHref: string;
  viewAllLabel: string;
  /** CSS custom-property name (e.g. "--color-admin-cyan"), read via var(). */
  accentColorVar?: string;
}) {
  const max = Math.max(...items.map((item) => item.value), 1);

  return (
    <div>
      <ul className="space-y-2">
        {items.map((item, index) => (
          <li key={item.label} className="text-xs">
            <div className="mb-1 flex items-center justify-between text-[var(--color-text)]">
              <span>
                {index + 1}. {item.label}
              </span>
              <span className="text-[var(--color-text-muted)]">{item.value}</span>
            </div>
            <div className="h-1.5 rounded-full bg-[var(--color-surface-hover)]">
              <div
                className="h-1.5 rounded-full"
                style={{ width: `${(item.value / max) * 100}%`, background: `var(${accentColorVar})` }}
              />
            </div>
          </li>
        ))}
      </ul>
      <Link
        href={viewAllHref}
        className="mt-3 inline-block text-xs font-medium hover:underline"
        style={{ color: `var(${accentColorVar})` }}
      >
        {viewAllLabel}
      </Link>
    </div>
  );
}
