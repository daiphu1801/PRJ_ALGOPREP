// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0302_mock_interview).
//
// Data boundary of the mock interview (F5). The scripted question bank stands in for the AI
// interviewer; `fetchReal` is a NOT_IMPLEMENTED stub until 03-dd/api/F5_ai-review.md defines the
// endpoints. When the real interviewer lands, the learner's answer must be passed as a data
// parameter of the call, never concatenated into the system instruction (README prompt-injection
// defence); the mock variables below carry no user text for that reason.
import { useMockMutation, useMockQuery } from "@/shared/api";
import type {
  AcceptedSubmissionOption,
  BankQuestionOption,
  InterviewResult,
  InterviewStage,
} from "../model/types";
import {
  ACCEPTED_SUBMISSIONS,
  BANK_QUESTIONS,
  HINT_TEXT,
  computeInterviewResult,
  getScriptedQuestion,
} from "./__mock__/mock-interview-mocks";

const SPEC = "03-dd/api/F5_ai-review.md";

export const useAcceptedSubmissionOptions = () =>
  useMockQuery<AcceptedSubmissionOption[]>({
    queryKey: ["mock-interview", "accepted-submissions"],
    mock: () => ACCEPTED_SUBMISSIONS,
    spec: SPEC,
  });

export const useBankQuestionOptions = () =>
  useMockQuery<BankQuestionOption[]>({
    queryKey: ["mock-interview", "bank-questions"],
    mock: () => BANK_QUESTIONS,
    spec: SPEC,
  });

export type ScriptedTurn = { stage: InterviewStage; content: string };

/** The interviewer's next question for a 0-based turn index. */
export const useNextInterviewQuestion = () =>
  useMockMutation<{ turnIndex: number; maxTurns: number }, ScriptedTurn>({
    mock: ({ turnIndex, maxTurns }) => getScriptedQuestion(turnIndex, maxTurns),
    spec: SPEC,
  });

/** A hint from the interviewer; the stage is chosen by the caller. */
export const useInterviewHint = () =>
  useMockMutation<void, string>({ mock: () => HINT_TEXT, spec: SPEC });

/** The closing rubric report of a finished session. */
export const useFinishInterview = () =>
  useMockMutation<void, InterviewResult>({
    mock: computeInterviewResult,
    spec: SPEC,
  });
