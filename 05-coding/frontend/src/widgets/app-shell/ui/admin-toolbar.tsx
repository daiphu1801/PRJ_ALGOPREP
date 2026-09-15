// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import { getT } from "@/shared/i18n/server";

/**
 * Matches 09-layoutBase/Admin - Tổng quan.dc.html:106-122 1:1: NOT a page-wide header — this is
 * the first row INSIDE `<main>`'s own padded content (dc.html:103-105), which is why it renders no
 * full-width bar/border here, only an empty flex-1 spacer, then right-aligned decorative icons +
 * the logged-in user block. No logo/brand and no theme switcher here — the static prototype puts
 * both inside the sidebar (see admin-sidebar.tsx), not this row. Each decorative icon is its own
 * small `.glass-surface` circle (dc.html:110 uses `background: var(--glass2)` per button), not a
 * flat bordered dot — the owner flagged this as missing glassmorphism on 2026-09-14. The 3
 * unlabeled icon buttons (dc.html:109-113) have no handler and no Fx-nn code in the static
 * prototype — BD explicitly keeps them as decoration, not a feature
 * (02-bd/screens/admin/admin_overview.md section 3, "Không đưa vào component inventory"). Marked
 * disabled + aria-hidden so they don't become dead keyboard stops (a11y). The liên-thực-thể search
 * box from an older prototype draft is dropped per DEC-2026-0831-admin-overview-ui-decisions —
 * never rebuilt here.
 */
const DECORATIVE_ICONS = ["grid", "moon", "shield"] as const;

export async function AdminToolbar() {
  const t = await getT("adminNav");

  return (
    <div className="mb-3 flex items-center gap-2">
      <div className="flex-1" />
      {DECORATIVE_ICONS.map((icon) => (
        <span
          key={icon}
          aria-hidden="true"
          className="glass-surface flex h-8 w-8 items-center justify-center rounded-full text-[var(--color-text-muted)] opacity-70"
        >
          •
        </span>
      ))}
      {/* Real identity waits on app/providers/auth-provider.tsx session bootstrap (still TODO) —
          shown as a static two-line placeholder (name + role, dc.html:116-119) so the row's shape
          matches the prototype without inventing a specific fake person's name. */}
      <div className="ml-2 flex items-center gap-2">
        <span
          className="flex h-8 w-8 items-center justify-center rounded-full bg-[var(--color-primary)] text-xs font-semibold text-[var(--color-on-primary)]"
          aria-hidden="true"
        >
          A
        </span>
        <div className="leading-tight">
          <p className="text-[12.5px] font-bold text-[var(--color-text)]">{t("identityNamePlaceholder")}</p>
          <p className="text-[11px] text-[var(--color-text-muted)]">{t("adminIdentityPlaceholder")}</p>
        </div>
      </div>
    </div>
  );
}
