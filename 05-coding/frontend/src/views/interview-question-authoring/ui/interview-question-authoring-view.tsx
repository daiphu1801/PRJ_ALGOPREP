import { getT } from "@/shared/i18n/server";

// Khung rỗng cho màn dùng chung 'interview_question_authoring' (A2 + A3, mount ở
// /instructor/interview-questions/[questionId] và /admin/interview-questions/[questionId] —
// route chốt tại 01-rd/screens/shared/interview_question_authoring.md:14). Cùng khuôn với
// problem_authoring theo DEC-2026-0825-shared-content-authoring-screens.
export async function InterviewQuestionAuthoringView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">interview-question-authoring</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}
