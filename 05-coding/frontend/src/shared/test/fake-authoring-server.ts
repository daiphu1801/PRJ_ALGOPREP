// Test-only: the part of the problem-bank server that SHR0204 (F2-19) talks to, kept in memory behind the
// faked HTTP layer of `real-api`. It remembers problems and testcases, so a test can assert what is stored
// after a failure and a retry instead of only counting calls.
import { ApiError } from "@/shared/api/api-error";
import { apiMock } from "./real-api";

const A = "/problem-bank/authoring/problems";

export type StoredTestcase = {
  id: string;
  visibility: string;
  input: string;
  expectedOutput: string;
};

export type StoredProblem = {
  id: string;
  content: Record<string, unknown> & { title: string };
  spec: Record<string, unknown> | null;
  testcases: StoredTestcase[];
  status: "UNPUBLISHED" | "PUBLISHED";
  version: number;
};

export type FakeServer = {
  problems: StoredProblem[];
  /** "METHOD path" of every write, in order. */
  writes: string[];
  /** Assignments by class id. */
  assigned: Record<string, string[]>;
  /**
   * Runs before each request; throw to fail it. `after` runs the request first and then throws, which is how
   * a lost answer looks from the browser.
   */
  before: (call: string) => void;
  after: (call: string) => void;
  /** Testcase position (1-based) the reference solution fails at, by source code; absent = all pass. */
  failsAt: Record<string, number>;
  /** Titles the bank holds besides `problems`. */
  otherTitles: string[];
};

export function fakeAuthoringServer(): FakeServer {
  let nextId = 0;
  const server: FakeServer = {
    problems: [],
    writes: [],
    assigned: {},
    before: () => {},
    after: () => {},
    failsAt: {},
    otherTitles: [],
  };
  const find = (id: string) => {
    const problem = server.problems.find((item) => item.id === id);
    if (!problem) throw new ApiError("PRB-010", 404);
    return problem;
  };
  const stamp = (problem: StoredProblem) =>
    `2026-11-25T00:00:${String(problem.version).padStart(2, "0")}Z`;
  const touch = (problem: StoredProblem) => {
    problem.version += 1;
    return stamp(problem);
  };
  const lock = (problem: StoredProblem, body: Record<string, unknown>) => {
    if (body.expectedUpdatedAt !== stamp(problem))
      throw new ApiError("PRB-020", 409);
  };
  const testcaseDto = (row: StoredTestcase, index: number) => ({
    ...row,
    position: index + 1,
    origin: "MANUAL",
    approved: true,
    category: null,
    inputStorage: "INLINE",
    expectedOutputStorage: "INLINE",
  });
  const detail = (problem: StoredProblem) => {
    const { content } = problem;
    return {
      id: problem.id,
      ...content,
      status: problem.status,
      tags: [],
      workedExamples: (content.workedExamples as object[]).map(
        (example, index) => ({
          ...example,
          id: `6666666${index}-6666-4666-8666-666666666666`,
        }),
      ),
      testcases: problem.testcases.map(testcaseDto),
      testcaseSummary: {
        total: problem.testcases.length,
        approved: problem.testcases.length,
        sample: 0,
        hidden: 0,
      },
      spec: problem.spec,
      aiContext: { briefText: null, noFullCode: false, questionOnly: false },
      aiGeneration: { used: 0, limit: 2 },
      updatedAt: stamp(problem),
      duplicatedFromProblemId: null,
      readOnly: false,
    };
  };

  function handle(
    method: string,
    path: string,
    body: Record<string, unknown> = {},
  ): unknown {
    const [, , id, rest, sub] = path.startsWith(`${A}/`)
      ? ["", "", ...path.slice(A.length + 1).split("/")]
      : [];
    if (method === "GET") {
      switch (path) {
        case "/problem-bank/levels":
          return {
            items: [
              { id: "lv-e", code: "EASY", displayName: "Dễ", sortOrder: 1 },
              {
                id: "lv-m",
                code: "MEDIUM",
                displayName: "Trung bình",
                sortOrder: 2,
              },
            ],
          };
        case "/problem-bank/topics":
          return {
            items: [{ id: "tp-1", code: "ARRAY", name: "Mảng", sortOrder: 1 }],
          };
        case "/judge/languages/enabled":
          return { items: ["JAVA", "CPP", "PYTHON"] };
        case "/problem-bank/authoring/defaults":
          return {
            timeLimitMs: 1000,
            memoryLimitMb: 256,
            maxOutputSizeKb: 64,
            maxAiGenerationsPerProblem: 2,
          };
        case "/identity/classes/my":
          return {
            items: [
              {
                id: "c1",
                name: "Lớp K66",
                description: null,
                schedule: null,
                studentCount: 3,
                createdAt: "2026-11-01T00:00:00Z",
              },
            ],
          };
        case A: {
          const q = String(body.q ?? "").toLowerCase();
          const items = [
            ...server.otherTitles,
            ...server.problems.map((problem) => problem.content.title),
          ]
            .filter((title) => title.toLowerCase().includes(q))
            .map((title, index) => ({
              id: `l${index}`,
              code: `L${index}`,
              title,
              topicNames: [],
              levelCode: "EASY",
              status: "UNPUBLISHED",
              submissionCount: 0,
              assignedClassCount: 0,
              acRate: null,
              testcaseCount: 0,
              updatedAt: "2026-11-01T00:00:00Z",
            }));
          return {
            items,
            page: {
              number: 1,
              size: 50,
              totalItems: items.length,
              totalPages: 1,
            },
            totals: { total: items.length, published: 0 },
          };
        }
        default:
      }
      if (rest === "stats") return { submissionCount: 0, acRate: null };
      return detail(find(id!));
    }

    if (method === "POST" && path === A) {
      const problem: StoredProblem = {
        id: `p${(nextId += 1)}`,
        content: body as StoredProblem["content"],
        spec: null,
        testcases: [],
        status: "UNPUBLISHED",
        version: 0,
      };
      server.problems.push(problem);
      return detail(problem);
    }
    if (path === `${A}/bulk/publish`) {
      const ids = body.problemIds as string[];
      for (const item of ids) find(item).status = "PUBLISHED";
      return { succeededIds: ids, failedItems: [] };
    }
    const toClass = /\/classes\/([^/]+)\/assignments$/.exec(path);
    if (toClass) {
      const ids = body.problemIds as string[];
      (server.assigned[toClass[1]!] ??= []).push(...ids);
      return { succeededIds: ids, skippedIds: [], failedItems: [] };
    }

    const problem = find(id!);
    if (rest === "content") {
      lock(problem, body);
      problem.content = body as StoredProblem["content"];
      touch(problem);
      return detail(problem);
    }
    if (rest === "spec") {
      lock(problem, body);
      problem.spec = { nameOverrides: null, ...body };
      return { problemUpdatedAt: touch(problem) };
    }
    if (rest === "reference-solution") {
      const failAt = server.failsAt[String(body.sourceCode)];
      const total = problem.testcases.length;
      return {
        compileError: null,
        summary: {
          passed: failAt ? total - 1 : total,
          total,
          allPass: !failAt,
        },
        results: problem.testcases.map((_, index) => ({
          position: index + 1,
          verdict: index + 1 === failAt ? "FAIL" : "PASS",
          failure: index + 1 === failAt ? "WRONG_ANSWER" : null,
        })),
      };
    }
    if (rest === "testcases" && method === "POST") {
      const row: StoredTestcase = {
        id: `t${(nextId += 1)}`,
        visibility: String(body.visibility),
        input: String(body.input),
        expectedOutput: String(body.expectedOutput),
      };
      problem.testcases.push(row);
      return {
        testcase: testcaseDto(row, problem.testcases.length - 1),
        problemUpdatedAt: touch(problem),
      };
    }
    const at = problem.testcases.findIndex((row) => row.id === sub);
    if (rest === "testcases" && at >= 0) {
      if (method === "DELETE") problem.testcases.splice(at, 1);
      else Object.assign(problem.testcases[at]!, body);
      return {
        testcase: testcaseDto(
          problem.testcases[at] ?? {
            id: "",
            visibility: "",
            input: "",
            expectedOutput: "",
          },
          at,
        ),
        problemUpdatedAt: touch(problem),
      };
    }
    throw new Error(`unexpected ${method} ${path}`);
  }

  const route =
    (method: string) =>
    async (path: string, body?: Record<string, unknown>) => {
      const call = `${method} ${path.replace("/problem-bank/authoring/problems", "")}`;
      server.before(call);
      if (method !== "GET") server.writes.push(call);
      const answer = structuredClone(handle(method, path, body));
      server.after(call);
      return answer;
    };
  apiMock.get.mockImplementation(route("GET"));
  apiMock.post.mockImplementation(route("POST"));
  apiMock.put.mockImplementation(route("PUT"));
  apiMock.patch.mockImplementation(route("PATCH"));
  apiMock.delete.mockImplementation(route("DELETE"));
  return server;
}
