import { getT } from "@/shared/i18n/server";

// Khung rỗng cho màn hình dùng chung 'interview_question_management' (A2 + A3, mount ở
// /instructor/interview-questions và /admin/interview-questions —
// DEC-2026-0825-shared-content-authoring-screens). Chờ 02-bd/screens/shared/ chốt bố cục.
export async function InterviewQuestionManagementView() {
  const t = await getT("common");

  return (
    <section className="p-6">
      <h1 className="text-lg font-semibold">interview-question-management</h1>
      <p className="text-sm text-[var(--color-text-muted)]">{t("pendingDesign")}</p>
    </section>
  );
}
