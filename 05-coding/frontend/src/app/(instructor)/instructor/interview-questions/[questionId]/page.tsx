// Read-only detail (DEC-2026-1002-split-detail-and-edit-pages). The edit form lives at ./edit.
import { InterviewQuestionInfoView } from "@/views/shared/interview-question-info";

export default async function Page({ params }: { params: Promise<{ questionId: string }> }) {
  const { questionId } = await params;
  return (
    <InterviewQuestionInfoView
      questionId={questionId}
      basePath="/instructor/interview-questions"
    />
  );
}
