// Copying the problems of one class to another (F2-12, DEC-2026-1125-copy-class-assignments).
import type { AssignedRow, AssignResult } from "./types";

/** Most ids one call of `AssignProblemsToClass` (#52) takes; more is PRB-001. */
const BATCH_MAX = 50;

type Row = Pick<AssignedRow, "id" | "title" | "withdrawn">;

export type CopyPlan = {
  /** Oldest assignment first, so the receiving class ends up in the order of the source. */
  toAssign: Row[];
  alreadyHas: number;
  withdrawn: number;
};

/**
 * Every source problem lands in exactly one group, so the three counts add up to the source. A problem the
 * target already has is counted there even when it is also withdrawn. `source` is newest first (#49).
 */
export function copyPlan(
  source: readonly Row[],
  target: readonly Pick<Row, "id">[],
): CopyPlan {
  const has = new Set(target.map((row) => row.id));
  const missing = source.filter((row) => !has.has(row.id));
  const toAssign = missing.filter((row) => !row.withdrawn).reverse();
  return {
    toAssign,
    alreadyHas: source.length - missing.length,
    withdrawn: missing.length - toAssign.length,
  };
}

/**
 * Sends the ids in order, `BATCH_MAX` at a time. A batch that throws stops the run: `error` is set and the
 * totals hold what the earlier batches did.
 */
export async function assignInBatches(
  ids: readonly string[],
  send: (batch: string[]) => Promise<AssignResult>,
): Promise<AssignResult & { error?: unknown }> {
  const total: AssignResult = { succeeded: [], skipped: [], failed: [] };
  for (let start = 0; start < ids.length; start += BATCH_MAX) {
    try {
      const result = await send(ids.slice(start, start + BATCH_MAX));
      total.succeeded.push(...result.succeeded);
      total.skipped.push(...result.skipped);
      total.failed.push(...result.failed);
    } catch (error) {
      return { ...total, error };
    }
  }
  return total;
}
