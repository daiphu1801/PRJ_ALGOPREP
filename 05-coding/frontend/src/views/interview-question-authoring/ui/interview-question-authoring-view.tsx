import { getT } from "@/shared/i18n/server";

// Empty shell for the shared 'interview_question_authoring' screen (A2 + A3, mounted at
// /instructor/interview-questions/[questionId] and /admin/interview-questions/[questionId] —
// route locked in 01-rd/screens/shared/interview_question_authoring.md:14). Follows the same
// pattern as problem_authoring per DEC-2026-0825-shared-content-authoring-screens.
export async function InterviewQuestionAuthoringView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">interview-question-authoring</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}
