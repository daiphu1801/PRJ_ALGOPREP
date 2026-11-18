// SHR0203 against the REAL api path (F2 #33): only the HTTP layer is faked. What matters: counts come
// from the summary when the caller may not read testcase bodies, a read-only viewer sees no reference
// solution and no edit button, and a missing problem reads as not found.
import { render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ProblemInfoView } from "./problem-info-view";

const m = viMessages.problemInfo;
const ta = viMessages.problemAuthoring;
const A = "/problem-bank/authoring/problems";
const P = "11111111-1111-4111-8111-111111111111";
const LEVEL = "22222222-2222-4222-8222-222222222222";

function dto(over: Record<string, unknown> = {}) {
  return {
    id: P,
    title: "Two Sum",
    statementMd: "Đề",
    constraintsMd: "",
    levelId: LEVEL,
    status: "PUBLISHED",
    limits: {
      timeLimitMs: 1000,
      memoryLimitMb: 256,
      maxOutputSizeKb: 64,
      maxSubmissionsPerHour: null,
    },
    topicIds: [],
    tags: [],
    referenceSolution: { language: "JAVA", sourceCode: "SECRET-CODE" },
    workedExamples: [],
    testcases: [],
    testcaseSummary: { total: 0, approved: 0, sample: 0, hidden: 0 },
    spec: null,
    aiContext: { briefText: null, noFullCode: false, questionOnly: false },
    aiGeneration: { used: 0, limit: 2 },
    updatedAt: "2026-11-06T01:00:00Z",
    duplicatedFromProblemId: null,
    readOnly: false,
    ...over,
  };
}

function routes(detail: unknown) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/problem-bank/levels")
      return {
        items: [
          {
            id: LEVEL,
            code: "EASY",
            displayName: "Dễ",
            sortOrder: 1,
            problemCount: null,
          },
        ],
      };
    if (path === "/problem-bank/topics") return { items: [] };
    if (path === `${A}/${P}`) {
      if (detail instanceof Error) throw detail;
      return detail;
    }
    if (path === `${A}/${P}/stats`) return { submissionCount: 0, acRate: null };
    throw new Error(`unexpected GET ${path}`);
  });
}

const open = () =>
  render(
    withTestProviders(
      <ProblemInfoView problemId={P} basePath="/admin/problems" />,
    ),
  );

describe("ProblemInfoView with the real API", () => {
  beforeEach(() => resetApiMock());

  it("shows the stored problem, the reference solution and the edit button to its author", async () => {
    routes(dto());
    open();
    expect(
      await screen.findByRole("heading", { level: 1, name: "Two Sum" }),
    ).toBeInTheDocument();
    expect(screen.getByText(m.solutionToggle)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: m.edit })).toHaveAttribute(
      "href",
      `/admin/problems/${P}/edit`,
    );
  });

  it("read-only: no reference solution card, no edit button; counts from the summary", async () => {
    routes(
      dto({
        readOnly: true,
        referenceSolution: null,
        testcaseSummary: { total: 9, approved: 8, sample: 2, hidden: 7 },
      }),
    );
    open();
    await screen.findByRole("heading", { level: 1, name: "Two Sum" });
    expect(screen.queryByText(m.solutionToggle)).toBeNull();
    expect(screen.queryByRole("link", { name: m.edit })).toBeNull();
    const approved = screen.getByText(m.statTestcases).nextElementSibling;
    expect(approved).toHaveTextContent("8");
    expect(approved).toHaveTextContent(
      m.statTestcasesMeta.replace("{count}", "1"),
    );
    expect(screen.getByText(m.statPublic).nextElementSibling).toHaveTextContent(
      "2",
    );
    expect(screen.queryByText(ta.coverageTitle)).toBeNull();
  });

  it("not found (PRB-010, also an A2 asking for someone else's draft) and a forbidden read are told apart", async () => {
    routes(new ApiError("PRB-010", 404));
    const { unmount } = open();
    expect(await screen.findByText(m.notFound)).toBeInTheDocument();
    unmount();
    routes(new ApiError("PRB-002", 403));
    open();
    expect(await screen.findByText(m.loadFailed)).toBeInTheDocument();
  });
});
