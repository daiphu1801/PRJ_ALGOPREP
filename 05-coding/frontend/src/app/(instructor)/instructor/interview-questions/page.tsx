import { InterviewQuestionManagementView } from "@/views/shared/interview-question-management";

// Shared A2+A3 screen (DEC-2026-0825-shared-content-authoring-screens); topic management stays ADMIN-only.
export default function Page() {
  return <InterviewQuestionManagementView basePath="/instructor/interview-questions" />;
}
