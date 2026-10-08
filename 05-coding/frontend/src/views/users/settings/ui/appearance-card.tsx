// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực A — reuses shared/ui/layout/theme-lang-switcher.tsx wholesale (per task instruction) instead of
// re-implementing two more segmented tabs: same two controls, same immediate-apply behaviour
// (REQ-09), no "Lưu cài đặt" involvement (BD Sheet 4.5 — "không có slice riêng").
import { useT } from "@/shared/i18n";
import { Card, ThemeLangSwitcher } from "@/shared/ui";

export function AppearanceCard() {
  const t = useT("settings");

  return (
    <Card title={t("appearance.title")}>
      <div className="flex flex-col gap-3">
        <p className="text-xs text-[var(--color-text-muted)]">
          {t("appearance.description")}
        </p>
        <ThemeLangSwitcher variant="inline" />
      </div>
    </Card>
  );
}
