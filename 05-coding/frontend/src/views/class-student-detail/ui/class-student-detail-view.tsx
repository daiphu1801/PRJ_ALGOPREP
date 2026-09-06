import { getT } from "@/shared/i18n/server";

// Khung rỗng cho màn 'class_student_detail' (A2). Là một màn/route RIÊNG, không phải expand row
// tại bảng học viên của class_management — chốt ở Q4 của class_management.md
// (01-rd/screens/teacher/class_student_detail.md:22-26). Chờ 02-bd/screens/teacher/.
export async function ClassStudentDetailView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">class-student-detail</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}
