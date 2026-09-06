// Màn dùng chung A2+A3 — cùng view mount ở cả hai khu vực, khuôn mẫu giống problem_authoring
// (DEC-2026-0825-shared-content-authoring-screens). Route chốt tại
// 01-rd/screens/shared/interview_question_authoring.md:14 (Câu hỏi mở Q6, chốt 2026-09-01).
import { InterviewQuestionAuthoringView } from "@/views/interview-question-authoring";

export default function Page() {
  return <InterviewQuestionAuthoringView />;
}
