import { getT } from "@/shared/i18n/server";

/**
 * Matches 09-layoutBase/Admin - Người dùng.dc.html:277-290 1:1: last element INSIDE `<main>`'s own
 * padded content (same `<footer>` position as every other Admin prototype), not a sibling of
 * `<aside>`/`<main>` and not pinned to the viewport bottom. Shared across ALL 11 Admin screens
 * including admin_overview, whose own prototype has no footer at all
 * (09-layoutBase/Admin - Tổng quan.dc.html:296-297) — a deliberate divergence, decided 2026-09-21
 * and recorded in 02-bd/screens/admin/_shell.md section 9.2: one shared shell footer beats matching
 * one screen's incomplete prototype. The 4 links are `href="#"` in every prototype (no real
 * destination yet), so they render as plain text here rather than dead `<a>` elements — same
 * decorative treatment already applied to the 3 toolbar icons (admin-toolbar.tsx).
 */
export async function AdminFooter() {
  const t = await getT("adminNav");

  const links = [
    t("footerLinkApiDocs"),
    t("footerLinkJudgeStatus"),
    t("footerLinkChangelog"),
    t("footerLinkSupport"),
  ];

  return (
    <div className="glass-surface mt-4 flex flex-wrap items-center gap-5 rounded-2xl px-5 py-4">
      <div className="min-w-0">
        <p className="text-[13px] font-semibold text-[var(--color-text)]">{t("footerBrand")}</p>
        <p className="text-xs text-[var(--color-text-muted)]">{t("footerCopyright")}</p>
      </div>
      <div className="ml-auto flex items-center gap-2 text-[12.5px] text-[var(--color-text-muted)]">
        <span className="h-1.5 w-1.5 rounded-full bg-[var(--color-success)]" aria-hidden="true" />
        {t("footerStatusOk")}
      </div>
      <div className="flex gap-4 text-[12.5px] font-medium text-[var(--color-text-muted)]">
        {links.map((label) => (
          <span key={label}>{label}</span>
        ))}
      </div>
    </div>
  );
}
