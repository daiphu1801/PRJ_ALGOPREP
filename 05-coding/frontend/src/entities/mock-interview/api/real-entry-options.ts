// Source lists of the USR0302 entry tabs. Both read already-built endpoints of other contexts
// directly through the shared client (an entity may not import another entity):
// - "Bai nop" tab: F4 #23 `GET /judge/me/accepted-submissions` (own Accepted submissions, newest
//   first, default 10, STUDENT only; JDG-V14 only for a bad `limit`, which is never sent).
// - "Cau hoi ngan hang" tab: F6 #2 `GET /interview-bank/questions` page 1 (20 rows). The ids are
//   real UUIDs, so F5 #8 `questionRef` (AIR-V07) accepts them.
import { api } from "@/shared/api";
import type {
  AcceptedSubmissionOption,
  BankQuestionOption,
} from "../model/types";

type AcceptedDto = {
  items: {
    submissionId: string;
    problem: { title: string } | null;
    language: string;
  }[];
};

const LANGUAGE_LABEL: Record<string, string> = {
  JAVA: "Java",
  CPP: "C++",
  PYTHON: "Python",
};

export async function realListAcceptedSubmissionOptions(): Promise<
  AcceptedSubmissionOption[]
> {
  const body = await api.get<AcceptedDto>("/judge/me/accepted-submissions");
  return body.items.map((item) => ({
    id: item.submissionId,
    // F2 metadata can be down (problem = null): fall back to the id, never a made-up title.
    problemTitle: item.problem?.title ?? item.submissionId,
    language: LANGUAGE_LABEL[item.language] ?? item.language,
  }));
}

export async function realListBankQuestionOptions(): Promise<
  BankQuestionOption[]
> {
  const body = await api.get<{ items: { id: string; title: string }[] }>(
    "/interview-bank/questions",
    { page: 1 },
  );
  return body.items.map((item) => ({ id: item.id, title: item.title }));
}
