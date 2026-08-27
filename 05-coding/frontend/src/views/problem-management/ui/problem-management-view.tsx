import { getT } from "@/shared/i18n/server";

// Khung rỗng cho màn hình dùng chung 'problem_management' (A2 + A3, mount ở /instructor/problems và
// /admin/problems — DEC-2026-0825-shared-content-authoring-screens). Chờ 02-bd/screens/shared/ chốt bố cục.
// Phạm vi dữ liệu (bài của giảng viên vs. toàn kho) là việc của tầng ứng dụng/API, không phải của view này.
export async function ProblemManagementView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">problem-management</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}
