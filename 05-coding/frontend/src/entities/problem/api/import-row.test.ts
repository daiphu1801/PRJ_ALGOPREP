// `saveImportRow` on the REAL api path against a server that remembers: what matters is what is stored after
// a failure and a retry, so nothing is created twice and nothing is left out.
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);

import {
  fakeAuthoringServer,
  type FakeServer,
} from "@/shared/test/fake-authoring-server";
import { resetApiMock } from "@/shared/test/real-api";
import {
  blankImportRow,
  toImportDraft,
  type ImportCatalogue,
  type ImportRow,
} from "../model/import-row";
import type { ProblemDraftRecord } from "../model/draft-types";
import {
  loadImportDefaults,
  runImportReference,
  saveImportRow,
} from "./import-row";

const catalogue: ImportCatalogue = {
  levels: [
    { key: "EASY", label: "Dễ" },
    { key: "MEDIUM", label: "Trung bình" },
  ],
  topics: [{ key: "tp-1", label: "Mảng" }],
  languages: ["Java", "C++", "Python"],
};

const makeRow = (count = 8): ImportRow => ({
  ...blankImportRow("r1"),
  title: "Tổng",
  level: "Trung bình",
  topics: "Mảng",
  statement: "Đề bài",
  stdin: "n",
  stdout: "tổng",
  constraints: "n <= 10",
  solution: "print(1)",
  cases: Array.from({ length: count }, (_, i) => ({
    input: `in${i + 1}`,
    output: `out${i + 1}`,
    note: "",
  })),
});

let server: FakeServer;
let defaults: ProblemDraftRecord;
let id: string | undefined;

const save = async (row: ImportRow) => {
  id = await saveImportRow({
    defaults,
    problemId: id,
    build: (base) => toImportDraft(row, base, catalogue),
    onCreated: (created) => {
      id = created;
    },
  });
  return id;
};
const stored = () => server.problems[0]!;
const inputs = () => stored().testcases.map((testcase) => testcase.input);
const offline = () => new ApiError("UNKNOWN_ERROR", 0);

beforeEach(async () => {
  resetApiMock();
  server = fakeAuthoringServer();
  id = undefined;
  defaults = await loadImportDefaults();
});

describe("saveImportRow", () => {
  it("creates the problem, its standard I/O spec and its testcases in order", async () => {
    await save(makeRow());
    expect(server.problems).toHaveLength(1);
    expect(stored().content).toMatchObject({
      title: "Tổng",
      statementMd: "Đề bài",
      constraintsMd: "n <= 10",
      levelId: "lv-m",
      topicIds: ["tp-1"],
      referenceSolution: { language: "PYTHON", sourceCode: "print(1)" },
      limits: {
        timeLimitMs: 1000,
        memoryLimitMb: 256,
        maxOutputSizeKb: 64,
        maxSubmissionsPerHour: null,
      },
    });
    expect(stored().content.workedExamples).toHaveLength(2);
    expect(stored().spec).toMatchObject({
      functionName: null,
      stdinFormatMd: "n",
      stdoutFormatMd: "tổng",
      matchingStrategy: "EXACT",
    });
    expect(inputs()).toEqual(makeRow().cases.map((entry) => entry.input));
    expect(stored().testcases.map((testcase) => testcase.visibility)).toEqual([
      "SAMPLE",
      "SAMPLE",
      ...Array.from({ length: 6 }, () => "HIDDEN"),
    ]);
  });

  it("a testcase that fails is retried without repeating the earlier ones", async () => {
    let creates = 0;
    server.before = (call) => {
      if (call === "POST /p1/testcases" && (creates += 1) === 4)
        throw new ApiError("PRB-052", 503);
    };
    await expect(save(makeRow())).rejects.toMatchObject({ code: "PRB-052" });
    expect(id).toBe("p1");
    expect(inputs()).toEqual(["in1", "in2", "in3"]);

    server.writes.length = 0;
    await save(makeRow());
    expect(server.problems).toHaveLength(1);
    expect(inputs()).toEqual(makeRow().cases.map((entry) => entry.input));
    expect(server.writes).toEqual(Array(5).fill("POST /p1/testcases"));
  });

  it("a testcase whose answer was lost is not created twice", async () => {
    let creates = 0;
    server.after = (call) => {
      if (call === "POST /p1/testcases" && (creates += 1) === 4)
        throw offline();
    };
    await expect(save(makeRow())).rejects.toMatchObject({ status: 0 });
    expect(inputs()).toHaveLength(4);
    await save(makeRow());
    expect(inputs()).toEqual(makeRow().cases.map((entry) => entry.input));
  });

  it("a spec that fails after the create is sent again, and the problem is not", async () => {
    let failed = false;
    server.before = (call) => {
      if (call === "PUT /p1/spec" && !failed) {
        failed = true;
        throw offline();
      }
    };
    await expect(save(makeRow())).rejects.toMatchObject({ status: 0 });
    await save(makeRow());
    expect(server.problems).toHaveLength(1);
    expect(stored().spec).toMatchObject({ stdinFormatMd: "n" });
    expect(inputs()).toHaveLength(8);
  });

  it("sends nothing when the stored problem already equals the row", async () => {
    await save(makeRow());
    server.writes.length = 0;
    await save(makeRow());
    expect(server.writes).toEqual([]);
  });

  it("an edited row changes, adds and removes only what differs", async () => {
    await save(makeRow(9));
    const edited = makeRow(9);
    edited.title = "Tổng mới";
    edited.cases[4]!.output = "sửa";
    edited.cases.splice(8, 1);
    server.writes.length = 0;
    await save(edited);
    expect(stored().content.title).toBe("Tổng mới");
    expect(stored().testcases[4]!.expectedOutput).toBe("sửa");
    expect(inputs()).toHaveLength(8);
    expect(server.writes.map((call) => call.split(" ")[0])).toEqual([
      "PUT",
      "PATCH",
      "DELETE",
    ]);
  });

  it("removing the first case moves the public ones up", async () => {
    await save(makeRow(9));
    const edited = makeRow(9);
    edited.cases.shift();
    await save(edited);
    expect(inputs()).toEqual(edited.cases.map((entry) => entry.input));
    expect(
      stored()
        .testcases.map((testcase) => testcase.visibility)
        .slice(0, 3),
    ).toEqual(["SAMPLE", "SAMPLE", "HIDDEN"]);
  });

  it("does not rewrite a problem published meanwhile", async () => {
    await save(makeRow());
    stored().status = "PUBLISHED";
    server.writes.length = 0;
    await expect(save(makeRow())).rejects.toMatchObject({ code: "PRB-020" });
    expect(server.writes).toEqual([]);
  });

  it("a refused create leaves no id", async () => {
    server.before = (call) => {
      if (call === "POST ") throw new ApiError("PRB-039", 422);
    };
    await expect(save(makeRow())).rejects.toMatchObject({ code: "PRB-039" });
    expect(id).toBeUndefined();
  });
});

describe("runImportReference", () => {
  it("sends the code of the row and reports the first failing testcase", async () => {
    const problemId = await save(makeRow());
    server.failsAt["print(0)"] = 3;
    expect(
      await runImportReference(
        problemId,
        { language: "Python", sourceCode: "print(0)" },
        8,
      ),
    ).toEqual({
      compileError: null,
      passed: 7,
      total: 8,
      allPass: false,
      firstFailure: { position: 3, failure: "WRONG_ANSWER" },
    });
  });
});
