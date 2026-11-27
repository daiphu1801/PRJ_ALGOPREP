// USR0104 for a guest (no session), real api path with only HTTP faked: 03-dd/screens/users/
// USR0104_problem_overview.md section 4A, DEC-2026-1126-guest-browse-published-problems.
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError, tokenStore } from "@/shared/api";
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
const push = vi.fn();
vi.mock("next/navigation", () => ({
  useParams: () => ({ problemId: "p1" }),
  useRouter: () => ({ push }),
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ProblemOverviewView } from "./problem-overview-view";

const d = viMessages.problemDetail;
const o = viMessages.problemOverview;
const prompt = viMessages.loginPrompt;

const OVERVIEW = {
  problemId: "p1",
  code: "P-1",
  title: "Two Sum",
  levelCode: "EASY",
  levelDisplayName: "Dễ",
  statementMd: "Cho mảng số nguyên.",
  constraintsMd: "",
  topicNames: ["Mảng"],
  acRate: 40,
  hiddenTestcaseCount: 7,
  bookmarked: false,
  examples: [{ input: "1 2", output: "3", explanationMd: null }],
};

function serve(overview: unknown = OVERVIEW) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/problem-bank/levels") return { items: [] };
    if (path === "/problem-bank/problems/p1/overview") {
      if (overview instanceof Error) throw overview;
      return overview;
    }
    throw new Error(`unexpected GET ${path}`);
  });
}

describe("ProblemOverviewView for a guest", () => {
  beforeEach(() => {
    resetApiMock();
    tokenStore.set(null);
    push.mockReset();
    serve();
  });

  it("reads the statement without the F4 limits and history, and hides the personal cards", async () => {
    render(withTestProviders(<ProblemOverviewView />));
    expect(
      await screen.findByRole("heading", { level: 1, name: "Two Sum" }),
    ).toBeInTheDocument();

    const paths = apiMock.get.mock.calls.map(([path]) => path);
    expect(paths).not.toContain("/judge/problems/p1/execution-limits");
    expect(paths).not.toContain("/judge/problems/p1/submissions/mine");
    expect(screen.queryByText(o.recentTitle)).toBeNull();
    expect(screen.queryByText(o.colLanguage)).toBeNull();
    expect(screen.getByText("7")).toBeInTheDocument();
  });

  it("opens the sign-in dialog for Solve and Save instead of navigating or calling the api", async () => {
    render(withTestProviders(<ProblemOverviewView />));
    await screen.findByRole("heading", { level: 1, name: "Two Sum" });

    fireEvent.click(screen.getByRole("button", { name: o.btnSolve }));
    expect(screen.getByText(prompt.title)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: prompt.login })).toHaveAttribute(
      "href",
      `/login?redirect=${encodeURIComponent("/problems/p1")}`,
    );
    fireEvent.click(screen.getByRole("button", { name: prompt.later }));

    fireEvent.click(screen.getByRole("button", { name: d.panel.btnSave }));
    expect(screen.getByText(prompt.title)).toBeInTheDocument();
    expect(apiMock.put).not.toHaveBeenCalled();
    expect(push).not.toHaveBeenCalled();
  });

  it("still reads PRB-010 as not found", async () => {
    serve(new ApiError("PRB-010", 404));
    render(withTestProviders(<ProblemOverviewView />));
    expect(await screen.findByText(d.empty.title)).toBeInTheDocument();
  });
});
