// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// List with a proportional bar per item. Two shapes now use it:
// - admin_overview "Bài phổ biến nhất": numbered, one accent, a "Xem tất cả" link
//   (02-bd/screens/admin/admin_overview.md section 2 point 5 — the ONLY block with such a link).
// - admin_system_log "Phân loại hành động · 7 ngày": unnumbered, one colour per category, a plain
//   footnote instead of a link (09-layoutBase/Admin - Nhật ký hệ thống.dc.html:233-246).
//
// Generic items in, the link is a plain href/label pair — no domain type, so shared/ui.
import Link from "next/link";

type RankedItem = {
  label: string;
  /** Drives the bar width. */
  value: number;
  /**
   * Shown on the right instead of `value`. Use it when the bar measures a share but the figure
   * worth reading is something else — e.g. a 57% bar labelled "21,9 tr".
   */
  valueLabel?: React.ReactNode;
  /** Per-item bar colour as a CSS custom-property name; falls back to `accentColorVar`. */
  colorVar?: string;
};

export function RankedProgressList({
  items,
  viewAllHref,
  viewAllLabel,
  footnote,
  numbered = true,
  accentColorVar = "--color-primary",
}: {
  items: RankedItem[];
  /** Omit both to render no link. */
  viewAllHref?: string;
  viewAllLabel?: string;
  /** Rendered under the list when there is no link — e.g. a pointer to another screen. */
  footnote?: React.ReactNode;
  numbered?: boolean;
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
                {numbered ? `${index + 1}. ` : null}
                {item.label}
              </span>
              <span className="font-mono text-[var(--color-text-muted)]">
                {item.valueLabel ?? item.value}
              </span>
            </div>
            <div className="h-1.5 rounded-full bg-[var(--color-track)]">
              <div
                className="h-1.5 rounded-full"
                style={{
                  width: `${(item.value / max) * 100}%`,
                  background: `var(${item.colorVar ?? accentColorVar})`,
                }}
              />
            </div>
          </li>
        ))}
      </ul>
      {viewAllHref && viewAllLabel ? (
        <Link
          href={viewAllHref}
          className="mt-3 inline-block text-xs font-medium hover:underline"
          style={{ color: `var(${accentColorVar})` }}
        >
          {viewAllLabel}
        </Link>
      ) : null}
      {footnote ? (
        <p className="mt-3.5 text-[12.5px] text-[var(--color-text-subtle)]">{footnote}</p>
      ) : null}
    </div>
  );
}
