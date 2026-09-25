import { InterviewQuestionDetailView } from "@/views/interview-question-detail";

export default async function Page({ params }: { params: Promise<{ questionId: string }> }) {
  const { questionId } = await params;
  return <InterviewQuestionDetailView questionId={questionId} />;
}
