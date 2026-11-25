import { describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";
import {
  isUnknownOutcome,
  publishRows,
  runRows,
  type ReferenceOutcome,
  type RowRun,
  type RunDeps,
} from "./import-run";

const PASS: ReferenceOutcome = {
  compileError: null,
  passed: 8,
  total: 8,
  allPass: true,
  firstFailure: null,
};

function harness(over: Partial<RunDeps<string>> = {}) {
  const log: [string, RowRun["step"]][] = [];
  const last: Record<string, RowRun> = {};
  const deps: RunDeps<string> = {
    save: async (row, onCreated) => {
      onCreated(`id-${row}`);
      return `id-${row}`;
    },
    shouldRun: () => true,
    runReference: async () => PASS,
    report: (row, run) => {
      log.push([row, run.step]);
      last[row] = run;
    },
    stopped: () => false,
    sleep: async () => {},
    ...over,
  };
  return { deps, log, last };
}

const rows = (...names: string[]) => names.map((row) => ({ row }));

describe("runRows", () => {
  it("goes one row after another", async () => {
    const { deps, log } = harness();
    await runRows(rows("a", "b"), deps);
    expect(log).toEqual([
      ["a", "saving"],
      ["a", "running"],
      ["a", "pass"],
      ["b", "saving"],
      ["b", "running"],
      ["b", "pass"],
    ]);
  });

  it("a refused row does not stop the next one", async () => {
    const { deps, last } = harness({
      save: async (row) => {
        if (row === "a") throw new ApiError("PRB-039", 422);
        return "id-b";
      },
    });
    await runRows(rows("a", "b"), deps);
    expect(last.a).toMatchObject({ step: "error", error: { code: "PRB-039" } });
    expect(last.b).toEqual({ step: "pass", total: 8 });
  });

  it("keeps an incomplete row as a draft without running it", async () => {
    const runReference = vi.fn(async () => PASS);
    const { deps, last } = harness({ shouldRun: () => false, runReference });
    await runRows(rows("a"), deps);
    expect(last.a).toEqual({ step: "draft" });
    expect(runReference).not.toHaveBeenCalled();
  });

  it("tells a wrong answer, a compile error and a sandbox failure apart", async () => {
    const { deps, last } = harness({
      runReference: async (row) => {
        if (row === "a")
          return {
            ...PASS,
            allPass: false,
            firstFailure: { position: 3, failure: "TIME_LIMIT_EXCEEDED" },
          };
        if (row === "b")
          return { ...PASS, allPass: false, compileError: "line 1" };
        throw new ApiError("PRB-050", 503);
      },
    });
    await runRows(rows("a", "b", "c"), deps);
    expect(last.a).toEqual({
      step: "fail",
      position: 3,
      failure: "TIME_LIMIT_EXCEEDED",
    });
    expect(last.b).toEqual({ step: "compileError" });
    expect(last.c).toMatchObject({ step: "error", error: { code: "PRB-050" } });
  });

  it("waits as long as PRB-055 asks, then runs again", async () => {
    const sleep = vi.fn(async () => {});
    let calls = 0;
    const { deps, log, last } = harness({
      sleep,
      runReference: async () => {
        calls += 1;
        if (calls === 1)
          throw new ApiError("PRB-055", 429, undefined, {
            retryAfterSeconds: 3,
          });
        return PASS;
      },
    });
    await runRows(rows("a"), deps);
    expect(sleep).toHaveBeenCalledTimes(3);
    expect(log.map(([, step]) => step)).toEqual([
      "saving",
      "running",
      "waiting",
      "waiting",
      "waiting",
      "running",
      "pass",
    ]);
    expect(last.a).toEqual({ step: "pass", total: 8 });
  });

  it("gives up on a limit that never clears", async () => {
    const { deps, last } = harness({
      runReference: async () => {
        throw new ApiError("PRB-055", 429);
      },
    });
    await runRows(rows("a"), deps);
    expect(last.a).toMatchObject({ step: "error", error: { code: "PRB-055" } });
  });

  it("stops after the row in flight, and stops while waiting", async () => {
    let stopped = false;
    const { deps, log } = harness({
      stopped: () => stopped,
      runReference: async () => {
        stopped = true;
        return PASS;
      },
    });
    await runRows(rows("a", "b"), deps);
    expect(log.at(-1)).toEqual(["a", "pass"]);

    stopped = false;
    const waiting = harness({
      stopped: () => stopped,
      sleep: async () => {
        stopped = true;
      },
      runReference: async () => {
        throw new ApiError("PRB-055", 429);
      },
    });
    await runRows(rows("a", "b"), waiting.deps);
    expect(waiting.last.a).toEqual({ step: "idle" });
    expect(waiting.last.b).toBeUndefined();
  });

  it("an unreadable create is unknown, but not once the problem exists", async () => {
    const offline = new ApiError("UNKNOWN_ERROR", 0);
    const first = harness({
      save: async () => {
        throw offline;
      },
    });
    await runRows(rows("a"), first.deps);
    expect(first.last.a).toEqual({ step: "unknown" });

    const later = harness({
      save: async (_row, onCreated) => {
        onCreated("id-a");
        throw offline;
      },
    });
    await runRows(rows("a"), later.deps);
    expect(later.last.a).toEqual({ step: "error", error: offline });

    const known = harness({
      save: async () => {
        throw offline;
      },
    });
    await runRows([{ row: "a", problemId: "id-a" }], known.deps);
    expect(known.last.a).toEqual({ step: "error", error: offline });
  });
});

describe("isUnknownOutcome", () => {
  it.each<[unknown, boolean]>([
    [new Error("boom"), true],
    [new ApiError("UNKNOWN_ERROR", 0), true],
    [new ApiError("UNKNOWN_ERROR", 504), true],
    [new ApiError("PRB-052", 503), false],
    [new ApiError("PRB-001", 400), false],
    [new ApiError("UNKNOWN_ERROR", 403), false],
  ])("%#", (error, expected) => {
    expect(isUnknownOutcome(error)).toBe(expected);
  });
});

describe("publishRows", () => {
  const items = ["a", "b", "c"].map((row) => ({
    row,
    problemId: `id-${row}`,
    total: 8,
  }));

  it("publishes one id per call and answers with the ones that went through", async () => {
    const last: Record<string, RowRun> = {};
    const publish = vi.fn(async (id: string) => {
      if (id === "id-c") throw new ApiError("UNKNOWN_ERROR", 504);
      return id === "id-b"
        ? { succeededIds: [], failedItems: [{ id, code: "PRB-030" }] }
        : { succeededIds: [id], failedItems: [] };
    });
    const ids = await publishRows(items, {
      publish,
      report: (row, run) => {
        last[row] = run;
      },
      stopped: () => false,
    });
    expect(publish.mock.calls.map(([id]) => id)).toEqual([
      "id-a",
      "id-b",
      "id-c",
    ]);
    expect(ids).toEqual(["id-a"]);
    expect(last.a).toEqual({ step: "published" });
    expect(last.b).toMatchObject({
      step: "publishError",
      total: 8,
      error: { code: "PRB-030" },
    });
    expect(last.c).toMatchObject({ step: "publishError" });
  });

  it("stops between problems", async () => {
    const publish = vi.fn(async (id: string) => ({
      succeededIds: [id],
      failedItems: [],
    }));
    const ids = await publishRows(items, {
      publish,
      report: () => {},
      stopped: () => publish.mock.calls.length >= 2,
    });
    expect(ids).toEqual(["id-a", "id-b"]);
  });
});
