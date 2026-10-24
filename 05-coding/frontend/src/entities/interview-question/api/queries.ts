// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Data boundary of the interview question bank (F6). Mock data until 03-dd/api/F6_interview-bank.md
// defines the endpoints; `fetchReal` is a NOT_IMPLEMENTED stub (shared/api `useMockQuery`).
import { useMockMutation, useMockQuery } from "@/shared/api";
import type {
  AnswerCriterionResult,
  InterviewQuestion,
  InterviewQuestionPage,
  RubricCriterion,
} from "../model/types";
import type { QuestionDraft } from "../model/csv-import";
import {
  importQuestions,
  loadInterviewQuestion,
  loadInterviewQuestionPage,
  mockCriterionResults,
} from "./__mock__/interview-question-mocks";

const SPEC = "03-dd/api/F6_interview-bank.md";
const PAGE_KEY = ["interview-question-page"] as const;

// staleTime 0: the in-memory bank changes on CSV import and a reopened screen must show it.
export const useInterviewQuestionPage = () =>
  useMockQuery<InterviewQuestionPage>({
    queryKey: PAGE_KEY,
    mock: loadInterviewQuestionPage,
    spec: SPEC,
    staleTime: 0,
  });

/** Resolves to `null` when the code is unknown. */
export const useInterviewQuestion = (code: string, enabled = true) =>
  useMockQuery<InterviewQuestion | null>({
    queryKey: ["interview-question", code],
    mock: () => loadInterviewQuestion(code),
    spec: SPEC,
    staleTime: 0,
    enabled,
  });

/** Bulk-creates the validated rows of a CSV file, then refreshes the list. */
export const useImportInterviewQuestions = () =>
  useMockMutation<readonly QuestionDraft[], InterviewQuestion[]>({
    mock: importQuestions,
    spec: SPEC,
    invalidate: [PAGE_KEY],
  });

/**
 * Per-criterion result of a practice answer (USR0402). Stand-in for the AI grader of F5/F6; the
 * rubric is the only input, the learner's answer text never reaches this call.
 */
export const useGradePracticeAnswer = () =>
  useMockMutation<RubricCriterion[], AnswerCriterionResult[]>({
    mock: mockCriterionResults,
    spec: SPEC,
  });
