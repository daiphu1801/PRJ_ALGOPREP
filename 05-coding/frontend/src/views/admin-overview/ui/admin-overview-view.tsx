import { getT } from "@/shared/i18n/server";

// Khung rỗng cho màn hình 'admin-overview' — slug bổ sung 2026-08-25 sau đợt đối chiếu prototype khu
// Admin (system_survey.md mục 7.3). Chờ 02-bd/screens/ + 03-dd/screens/ chốt bố cục.
export async function AdminOverviewView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">admin-overview</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}
