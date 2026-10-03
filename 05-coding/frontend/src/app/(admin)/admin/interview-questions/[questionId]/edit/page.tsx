import { InterviewQuestionAuthoringView } from "@/views/shared/interview-question-authoring";

export default async function Page({ params }: { params: Promise<{ questionId: string }> }) {
  const { questionId } = await params;
  return (
    <InterviewQuestionAuthoringView
      questionId={questionId}
      listHref="/admin/interview-questions"
    />
  );
}
