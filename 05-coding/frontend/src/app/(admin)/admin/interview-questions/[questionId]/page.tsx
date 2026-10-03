// Read-only detail. The edit form moved to ./edit; list rows link here, the "Sửa" button links there.
// Shared A2+A3 screen (DEC-2026-0825-shared-content-authoring-screens) — admin mount only for now.
import { InterviewQuestionInfoView } from "@/views/shared/interview-question-info";

export default async function Page({ params }: { params: Promise<{ questionId: string }> }) {
  const { questionId } = await params;
  return (
    <InterviewQuestionInfoView questionId={questionId} basePath="/admin/interview-questions" />
  );
}
