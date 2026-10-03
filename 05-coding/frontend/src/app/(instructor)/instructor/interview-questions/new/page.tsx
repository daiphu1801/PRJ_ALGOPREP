import { InterviewQuestionAuthoringView } from "@/views/shared/interview-question-authoring";

export default function Page() {
  return (
    <InterviewQuestionAuthoringView
      questionId="new"
      listHref="/instructor/interview-questions"
    />
  );
}
