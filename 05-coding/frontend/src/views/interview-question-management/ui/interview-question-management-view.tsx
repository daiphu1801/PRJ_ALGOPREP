import { getT } from "@/shared/i18n/server";

// Empty shell for the shared 'interview_question_management' screen (A2 + A3, mounted at
// /instructor/interview-questions and /admin/interview-questions —
// DEC-2026-0825-shared-content-authoring-screens). Pending 02-bd/screens/shared/ to lock the layout.
export async function InterviewQuestionManagementView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">interview-question-management</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}
