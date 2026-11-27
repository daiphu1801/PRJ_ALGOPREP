import { beforeEach, describe, expect, it, vi } from "vitest";
const get = vi.fn();
vi.mock("@/shared/api", async (original) => ({
  ...(await original<Record<string, unknown>>()),
  api: { get: (...args: unknown[]) => get(...args) },
}));
import { ApiError } from "@/shared/api";
import {
  fetchEnabledLanguages,
  fetchOverviewProblem,
  fetchStarterCode,
  fetchWorkspaceProblem,
} from "./workspace-queries";

beforeEach(() => {
  get.mockReset();
});
describe("workspace API contracts", () => {
  it("keeps enabled language order and never invents a disabled language", async () => {
    get.mockResolvedValue({ items: ["PYTHON", "CPP"] });
    expect(await fetchEnabledLanguages()).toEqual(["python", "cpp"]);
    expect(get).toHaveBeenCalledWith("/judge/languages/enabled");
  });
  it.each([{ items: [] }, { items: ["GO"] }])(
    "rejects unusable language configuration %j",
    async (response) => {
      get.mockResolvedValue(response);
      await expect(fetchEnabledLanguages()).rejects.toBeInstanceOf(ApiError);
    },
  );
  it.each([
    ["java", "function", "JAVA", "FUNCTION_WRAPPER"],
    ["cpp", "stdio", "CPP", "STANDARD_IO"],
    ["python", "function", "PYTHON", "FUNCTION_WRAPPER"],
  ] as const)(
    "loads %s %s with exact wire enums",
    async (language, mode, wireLanguage, model) => {
      get.mockResolvedValue({
        language: wireLanguage,
        submissionModel: model,
        sourceCode: "real starter",
      });
      expect(await fetchStarterCode("problem", language, mode)).toBe(
        "real starter",
      );
      expect(get).toHaveBeenCalledWith(
        "/harness/problems/problem/starter-code",
        { language: wireLanguage, model },
      );
    },
  );
  it("rejects a starter for another language", async () => {
    get.mockResolvedValue({
      language: "JAVA",
      submissionModel: "STANDARD_IO",
      sourceCode: "wrong",
    });
    await expect(
      fetchStarterCode("p", "python", "stdio"),
    ).rejects.toBeInstanceOf(ApiError);
  });
  it("preserves permission and availability errors rather than returning mock code", async () => {
    const error = new ApiError("HRN-003", 503, "unavailable");
    get.mockRejectedValue(error);
    await expect(fetchStarterCode("p", "java", "function")).rejects.toBe(error);
  });
  it("treats a withdrawn workspace as not found", async () => {
    get.mockRejectedValue(new ApiError("PRB-001", 404, "not found"));
    expect(await fetchWorkspaceProblem("p")).toBeNull();
  });

  describe("overview (#6 + F4 limits and history)", () => {
    const overview = {
      problemId: "p1",
      code: "P-1",
      title: "T",
      levelCode: "EASY",
      statementMd: "S",
      constraintsMd: "C",
      topicNames: ["A"],
      acRate: null,
      hiddenTestcaseCount: 4,
      bookmarked: true,
      examples: [{ input: "i", output: "o", explanationMd: null }],
    };
    const limits = {
      limits: ["JAVA", "CPP", "PYTHON"].map((language) => ({
        language,
        timeLimitMs: 2000,
        memoryLimitMb: 128,
      })),
    };
    function answer(overrides: Record<string, unknown> = {}) {
      get.mockImplementation(async (path: string) => {
        const key = path.split("/").pop() as string;
        const value = {
          overview,
          "execution-limits": limits,
          mine: { items: [] },
          ...overrides,
        }[key === "mine" ? "mine" : key];
        if (value instanceof Error) throw value;
        return value;
      });
    }

    it("maps the overview, keeps acRate null and joins statement with constraints", async () => {
      answer();
      const result = await fetchOverviewProblem("p1");
      expect(get).toHaveBeenCalledWith("/problem-bank/problems/p1/overview");
      expect(result).toMatchObject({
        id: "p1",
        saved: true,
        acRate: null,
        hiddenTestcaseCount: 4,
        statementMd: "S\n\nC",
        examples: [{ input: "i", output: "o", explanation: undefined }],
      });
      expect(result?.limitsByLanguage.cpp).toEqual({
        timeLimitMs: 2000,
        memoryLimitMb: 128,
      });
    });

    it("returns null on a 404 and rethrows when the problem itself cannot be read", async () => {
      answer({ overview: new ApiError("PRB-010", 404) });
      expect(await fetchOverviewProblem("p1")).toBeNull();
      const down = new ApiError("UNKNOWN_ERROR", 503);
      answer({ overview: down });
      await expect(fetchOverviewProblem("p1")).rejects.toBe(down);
    });

    it("keeps the problem and drops the limits when the F4 limits read fails", async () => {
      answer({ "execution-limits": new ApiError("UNKNOWN_ERROR", 500) });
      const result = await fetchOverviewProblem("p1");
      expect(result?.title).toBe("T");
      expect(result?.limitsByLanguage).toEqual({});
      expect(result?.mySubmissions).toEqual([]);
    });

    it("marks the history as unavailable, not empty, when its read fails", async () => {
      answer({ mine: new ApiError("JDG-506", 503) });
      const result = await fetchOverviewProblem("p1");
      expect(result?.mySubmissions).toBeNull();
      expect(result?.limitsByLanguage.java).toBeDefined();
    });

    it("drops an incomplete limits answer instead of inventing a language", async () => {
      answer({ "execution-limits": { limits: limits.limits.slice(0, 2) } });
      const result = await fetchOverviewProblem("p1");
      expect(result?.limitsByLanguage).toEqual({});
    });

    it("keeps the workspace usable when both F4 reads fail", async () => {
      get.mockImplementation(async (path: string) => {
        if (path.endsWith("/workspace"))
          return {
            ...overview,
            id: "p1",
            functionWrapperSupported: true,
            workedExamples: [],
          };
        if (path.endsWith("/overview")) return overview;
        if (path.endsWith("/sample-testcases")) return { items: [] };
        throw new ApiError("UNKNOWN_ERROR", 503);
      });
      const result = await fetchWorkspaceProblem("p1");
      expect(result?.title).toBe("T");
      expect(result?.limitsByLanguage).toEqual({});
      expect(result?.mySubmissions).toBeNull();
    });
  });
});
