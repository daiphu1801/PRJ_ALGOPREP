// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Khu vực A — reuses shared/ui/layout/theme-lang-switcher.tsx wholesale (per task instruction) instead of
// re-implementing two more segmented tabs: same two controls, same immediate-apply behaviour
// (REQ-09), no "Lưu cài đặt" involvement (BD Sheet 4.5 — "không có slice riêng").
import { Palette } from "lucide-react";
import { useT } from "@/shared/i18n";
import { Card, ThemeLangSwitcher } from "@/shared/ui";

export function AppearanceCard() {
  const t = useT("settings");

  return (
    <Card
      title={
        <span className="flex items-center gap-2">
          <Palette size={18} aria-hidden="true" />
          {t("appearance.title")}
        </span>
      }
      description={t("appearance.description")}
    >
      <ThemeLangSwitcher
        variant="inline"
        className="flex-wrap gap-x-2 gap-y-1"
      />
    </Card>
  );
}
