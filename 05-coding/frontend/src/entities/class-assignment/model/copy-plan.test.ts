import { describe, expect, it, vi } from "vitest";
import { assignInBatches, copyPlan } from "./copy-plan";

const row = (id: string, withdrawn = false) => ({ id, title: id, withdrawn });
const ids = (rows: { id: string }[]) => rows.map((r) => r.id);

describe("copyPlan", () => {
  it("takes everything, oldest first, when the target is empty", () => {
    const plan = copyPlan([row("new"), row("mid"), row("old")], []);
    expect(ids(plan.toAssign)).toEqual(["old", "mid", "new"]);
    expect([plan.alreadyHas, plan.withdrawn]).toEqual([0, 0]);
  });

  it("leaves out what the target has and what is withdrawn", () => {
    const plan = copyPlan(
      [row("a"), row("b"), row("w", true), row("c")],
      [row("b"), row("other")],
    );
    expect(ids(plan.toAssign)).toEqual(["c", "a"]);
    expect([plan.alreadyHas, plan.withdrawn]).toEqual([1, 1]);
  });

  it("counts a problem that is both owned and withdrawn once, as owned", () => {
    const plan = copyPlan([row("w", true)], [row("w", true)]);
    expect(plan).toEqual({ toAssign: [], alreadyHas: 1, withdrawn: 0 });
  });

  it("has nothing to assign when the target has it all, and does not reorder its input", () => {
    const source = [row("a"), row("b")];
    expect(copyPlan(source, source).toAssign).toEqual([]);
    copyPlan(source, []);
    expect(ids(source)).toEqual(["a", "b"]);
  });
});

describe("assignInBatches", () => {
  const many = (n: number) => Array.from({ length: n }, (_, i) => `p${i}`);
  const ok = async (batch: string[]) => ({
    succeeded: batch,
    skipped: [],
    failed: [],
  });

  it.each([
    [0, []],
    [1, [1]],
    [50, [50]],
    [51, [50, 1]],
    [100, [50, 50]],
  ])("%i ids are sent as %j", async (n, sizes) => {
    const send = vi.fn(ok);
    const result = await assignInBatches(many(n), send);
    expect(send.mock.calls.map(([batch]) => batch.length)).toEqual(sizes);
    expect(result.succeeded).toEqual(many(n));
    expect(result.error).toBeUndefined();
  });

  it("stops at the batch that throws and keeps the earlier totals", async () => {
    const boom = new Error("down");
    const send = vi
      .fn(ok)
      .mockImplementationOnce(ok)
      .mockRejectedValueOnce(boom);
    const result = await assignInBatches(many(120), send);
    expect(send).toHaveBeenCalledTimes(2);
    expect(result.succeeded).toHaveLength(50);
    expect(result.error).toBe(boom);
  });
});
