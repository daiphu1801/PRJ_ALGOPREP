import { getT } from "@/shared/i18n/server";

// Khung rỗng cho màn 'class_assignments' ("Bài tập của tôi", A2). Là một mục nav ĐỘC LẬP
// trong sidebar giáo viên, không phải tab của class_management — tách theo
// DEC-2026-0828-split-class-management-assignments. Chờ 02-bd/screens/teacher/class_assignments.md.
export async function ClassAssignmentsView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">class-assignments</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}
