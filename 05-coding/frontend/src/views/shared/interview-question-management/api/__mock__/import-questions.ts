// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
import {
  appendImportedQuestions,
  type QuestionDraft,
} from "@/entities/interview-question";

export async function importQuestions(drafts: readonly QuestionDraft[]) {
  await new Promise((resolve) => setTimeout(resolve, 150));
  return appendImportedQuestions(drafts);
}
