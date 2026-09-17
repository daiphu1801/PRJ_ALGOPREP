import { getT } from "@/shared/i18n/server";

// Empty shell for the 'class_assignments' screen ("My Assignments", A2). This is a STANDALONE
// nav item in the instructor sidebar, not a tab under class_management — split out per
// DEC-2026-0828-split-class-management-assignments. Pending 02-bd/screens/teacher/class_assignments.md.
export async function ClassAssignmentsView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">class-assignments</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}
