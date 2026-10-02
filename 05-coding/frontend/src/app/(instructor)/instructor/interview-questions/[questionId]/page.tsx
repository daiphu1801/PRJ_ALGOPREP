// Shared A2+A3 screen — the same view mounted under both areas, following the same pattern as
// problem_authoring (DEC-2026-0825-shared-content-authoring-screens). Route locked in
// 01-rd/screens/shared/interview_question_authoring.md:14 (open question Q6, resolved 2026-09-01).
// `[questionId]` is a question code, or the literal "new" for the create form.
import { InterviewQuestionAuthoringView } from "@/views/shared/interview-question-authoring";

export default async function Page({ params }: { params: Promise<{ questionId: string }> }) {
  const { questionId } = await params;
  return (
    <InterviewQuestionAuthoringView
      questionId={questionId}
      listHref="/instructor/interview-questions"
    />
  );
}
