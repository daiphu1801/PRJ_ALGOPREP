import { getT } from "@/shared/i18n/server";

// Empty shell for the 'class_student_detail' screen (A2). This is a SEPARATE screen/route, not
// an expandable row in the class_management student table — locked in Q4 of class_management.md
// (01-rd/screens/teacher/class_student_detail.md:22-26). Pending 02-bd/screens/teacher/.
export async function ClassStudentDetailView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">class-student-detail</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}
