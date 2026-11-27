// Writes of the bulk problem import (F2-19, SHR0204): one row to the server through the same calls the
// authoring form uses (#33, #35 to #37, #41 to #43, #47). Mock mode keeps the same contract.
import { ApiError, withMockData } from "@/shared/api";
import type {
  ProblemDraft,
  ProblemDraftRecord,
  Testcase,
} from "../model/draft-types";
import {
  loadProblemDraft,
  saveProblemDraft,
} from "./__mock__/problem-draft-mocks";
import {
  emptySpec,
  realCreateTestcase,
  realDeleteTestcase,
  realLoadDraft,
  realRunReferenceSolution,
  realSaveDraft,
  realUpdateTestcase,
  type ReferenceRun,
} from "./real-authoring";

/** A blank draft with the system's default limits; one load serves every row of a run. */
export const loadImportDefaults = (): Promise<ProblemDraftRecord> =>
  withMockData(
    async () => {
      const record = await loadProblemDraft();
      return {
        ...record,
        draft: {
          ...record.draft,
          examples: [],
          testcases: [],
          tags: [],
          spec: emptySpec(),
          status: "draft",
        },
      };
    },
    () => realLoadDraft(),
  );

export type SaveImportInput = {
  defaults: ProblemDraftRecord;
  /** Set once the row has been created: the problem is then updated, never created again. */
  problemId?: string;
  /** The row over what the server holds (`toImportDraft`). */
  build: (base: ProblemDraft) => ProblemDraft;
  /** Called as soon as the problem exists, before the later steps can fail. */
  onCreated: (problemId: string) => void;
};

/**
 * Creates the problem or brings the created one up to date. Also the way to finish a row that stopped half
 * way: it reads what the server holds and only sends the difference.
 */
export const saveImportRow = (input: SaveImportInput): Promise<string> =>
  withMockData(
    async () => {
      const created = await saveProblemDraft(
        input.build(input.defaults.draft),
        input.problemId,
      );
      const id = input.problemId ?? created!;
      if (created) input.onCreated(created);
      return id;
    },
    () => realSaveImportRow(input),
  );

async function realSaveImportRow({
  defaults,
  problemId,
  build,
  onCreated,
}: SaveImportInput): Promise<string> {
  const record = problemId ? await realLoadDraft(problemId) : defaults;
  // Published elsewhere meanwhile: learners may be solving it, so the table must not rewrite it.
  if (record.draft.status === "published") throw new ApiError("PRB-020", 409);
  const draft = build(record.draft);
  const saved = await realSaveDraft({
    // Testcases are written below, where a retry can tell which ones already exist.
    draft: { ...draft, testcases: [] },
    saved: record.draft,
    problemId,
    server: record.server!,
    updatedAt: record.server!.updatedAt,
  });
  if (saved.createdId) onCreated(saved.createdId);
  if (saved.error !== undefined) throw saved.error;
  const id = problemId ?? saved.createdId!;
  await syncTestcases(
    id,
    problemId ? record.draft.testcases : [],
    draft.testcases,
  );
  return id;
}

/** Matches by position: changes a differing testcase, adds the missing ones, removes the surplus. */
async function syncTestcases(
  problemId: string,
  stored: readonly Testcase[],
  wanted: readonly Testcase[],
) {
  for (const [index, testcase] of wanted.entries()) {
    const current = stored[index];
    if (!current) {
      await realCreateTestcase(problemId, testcase);
      continue;
    }
    // A side kept in MinIO arrives without its text, so it cannot be compared: it is sent again.
    const differs = (side: "input" | "expected") =>
      current.largeSides?.includes(side) || current[side] !== testcase[side];
    const patch = {
      ...(differs("input") ? { input: testcase.input } : {}),
      ...(differs("expected") ? { expected: testcase.expected } : {}),
      ...(current.visibility !== testcase.visibility
        ? { visibility: testcase.visibility }
        : {}),
      ...(current.approved ? {} : { approved: true as const }),
    };
    if (Object.keys(patch).length > 0)
      await realUpdateTestcase(problemId, current.id, patch);
  }
  for (const surplus of stored.slice(wanted.length).reverse())
    await realDeleteTestcase(problemId, surplus.id);
}

/** #47 on every stored testcase. Mock mode has no sandbox: it answers "all pass". */
export const runImportReference = (
  problemId: string,
  code: { language: string; sourceCode: string },
  testcaseCount: number,
): Promise<ReferenceRun> =>
  withMockData<ReferenceRun>(
    () => ({
      compileError: null,
      passed: testcaseCount,
      total: testcaseCount,
      allPass: true,
      firstFailure: null,
    }),
    () => realRunReferenceSolution(problemId, code),
  );
