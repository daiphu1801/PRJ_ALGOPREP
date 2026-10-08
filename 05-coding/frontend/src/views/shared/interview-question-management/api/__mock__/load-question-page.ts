// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// The bank is still an in-memory sample. The short delay makes the loading state real in the prototype,
// so the skeleton and the error path get exercised before the API exists (same as the problem list).
import {
  fetchInterviewQuestionPage,
  type InterviewQuestionPage,
} from "@/entities/interview-question";

export async function loadInterviewQuestionPage(): Promise<InterviewQuestionPage> {
  await new Promise((resolve) => setTimeout(resolve, 150));
  return fetchInterviewQuestionPage();
}
