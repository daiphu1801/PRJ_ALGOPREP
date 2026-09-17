// Shared A2+A3 screen — the same view mounted under both areas, following the same pattern as
// problem_authoring (DEC-2026-0825-shared-content-authoring-screens). Route locked in
// 01-rd/screens/shared/interview_question_authoring.md:14 (open question Q6, resolved 2026-09-01).
import { InterviewQuestionAuthoringView } from "@/views/interview-question-authoring";

export default function Page() {
  return <InterviewQuestionAuthoringView />;
}
