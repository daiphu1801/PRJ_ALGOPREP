// The run of SHR0204 (F2-19): rows go one after another, a row that fails never stops the next one, and the
// author can stop between rows. Pure: every server call comes in through `deps`, so the order, the waiting
// and the failure rules are testable without a screen.
import { ApiError } from "@/shared/api";

export type ReferenceOutcome = {
  compileError: string | null;
  passed: number;
  total: number;
  allPass: boolean;
  firstFailure: { position: number; failure: string } | null;
};

export type RowRun =
  /** Not sent yet, or edited since the last run. */
  | { step: "idle" }
  | { step: "saving" }
  /** The server allows 10 reference runs a minute (PRB-055). */
  | { step: "waiting"; seconds: number }
  | { step: "running" }
  /** Stored, but something is missing so it was not run. */
  | { step: "draft" }
  | { step: "pass"; total: number }
  | { step: "fail"; position: number; failure: string }
  | { step: "compileError" }
  /** Stored or not, the step that failed says nothing about the solution: sandbox down, refused, offline. */
  | { step: "error"; error: unknown }
  /** The create call got no readable answer: the problem may or may not exist. */
  | { step: "unknown" }
  | { step: "publishing" }
  | { step: "publishError"; error: unknown; total: number }
  | { step: "published" };

export type RunDeps<Row> = {
  /** Creates or updates the problem; must call `onCreated` as soon as the problem exists. */
  save: (row: Row, onCreated: (id: string) => void) => Promise<string>;
  /** False when the row is only kept as a draft. */
  shouldRun: (row: Row) => boolean;
  runReference: (row: Row, problemId: string) => Promise<ReferenceOutcome>;
  report: (row: Row, run: RowRun) => void;
  stopped: () => boolean;
  sleep: (ms: number) => Promise<void>;
};

const WAIT_DEFAULT_S = 10;
const WAIT_MAX_S = 60;
/** Enough for the sliding window to empty several times; a limit that never clears ends as an error. */
const WAIT_ROUNDS = 6;

/**
 * True when a failed create may still have been carried out: no answer, or a gateway gave up. An answer
 * carrying one of the module's own codes is a refusal, and a refused create is rolled back.
 */
export function isUnknownOutcome(error: unknown): boolean {
  if (!(error instanceof ApiError)) return true;
  if (error.code.startsWith("PRB-")) return false;
  return error.status === 0 || error.status >= 500;
}

async function runRow<Row>(
  row: Row,
  knownId: string | undefined,
  deps: RunDeps<Row>,
): Promise<RowRun> {
  let problemId = knownId;
  deps.report(row, { step: "saving" });
  try {
    problemId = await deps.save(row, (id) => {
      problemId = id;
    });
  } catch (error) {
    return !problemId && isUnknownOutcome(error)
      ? { step: "unknown" }
      : { step: "error", error };
  }
  if (!deps.shouldRun(row)) return { step: "draft" };

  for (let round = 0; ; round += 1) {
    deps.report(row, { step: "running" });
    try {
      const outcome = await deps.runReference(row, problemId);
      if (outcome.compileError !== null) return { step: "compileError" };
      if (outcome.allPass) return { step: "pass", total: outcome.total };
      return {
        step: "fail",
        position: outcome.firstFailure?.position ?? 0,
        failure: outcome.firstFailure?.failure ?? "WRONG_ANSWER",
      };
    } catch (error) {
      const limited = error instanceof ApiError && error.code === "PRB-055";
      if (!limited || round >= WAIT_ROUNDS) return { step: "error", error };
      const asked = Number(error.details?.retryAfterSeconds);
      const seconds = Math.min(asked > 0 ? asked : WAIT_DEFAULT_S, WAIT_MAX_S);
      for (let left = seconds; left > 0; left -= 1) {
        if (deps.stopped()) return { step: "idle" };
        deps.report(row, { step: "waiting", seconds: left });
        await deps.sleep(1000);
      }
      if (deps.stopped()) return { step: "idle" };
    }
  }
}

export async function runRows<Row>(
  rows: readonly { row: Row; problemId?: string }[],
  deps: RunDeps<Row>,
): Promise<void> {
  for (const { row, problemId } of rows) {
    if (deps.stopped()) return;
    deps.report(row, await runRow(row, problemId, deps));
  }
}

export type PublishDeps<Row> = {
  /** One id per call: the server runs the reference solution again for each problem (proxy timeout 240 s). */
  publish: (problemId: string) => Promise<{
    succeededIds: string[];
    failedItems: { id: string; code: string }[];
  }>;
  report: (row: Row, run: RowRun) => void;
  stopped: () => boolean;
};

/** Publishes one by one and answers with the ids that are now public, in order. */
export async function publishRows<Row>(
  rows: readonly { row: Row; problemId: string; total: number }[],
  deps: PublishDeps<Row>,
): Promise<string[]> {
  const published: string[] = [];
  for (const { row, problemId, total } of rows) {
    if (deps.stopped()) break;
    deps.report(row, { step: "publishing" });
    try {
      const result = await deps.publish(problemId);
      const refused = result.failedItems.find((item) => item.id === problemId);
      if (refused) throw new ApiError(refused.code, 422);
      published.push(problemId);
      deps.report(row, { step: "published" });
    } catch (error) {
      deps.report(row, { step: "publishError", error, total });
    }
  }
  return published;
}
