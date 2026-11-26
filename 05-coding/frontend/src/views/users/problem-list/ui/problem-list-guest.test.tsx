// USR0101 for a guest (no session), on the real api path with only HTTP faked: 03-dd/screens/users/
// USR0101_problem_list.md BR-SC-07, DEC-2026-1126-guest-browse-published-problems.
import { fireEvent, render, screen } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { tokenStore } from "@/shared/api";
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
const push = vi.hoisted(() => vi.fn());
vi.mock("next/navigation", async () =>
  (await import("@/shared/test/mock-url")).navigationMock({ push }),
);

import { mockUrl } from "@/shared/test/mock-url";
import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ProblemListView } from "./problem-list-view";

const m = viMessages.problemList;
const prompt = viMessages.loginPrompt;

function serve() {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/problem-bank/levels")
      return {
        items: [
          {
            id: "l1",
            code: "EASY",
            displayName: "Dễ",
            sortOrder: 1,
            problemCount: null,
          },
        ],
      };
    if (path === "/problem-bank/problems")
      return {
        items: [
          {
            id: "1",
            code: "P-1",
            title: "Bài 1",
            topicNames: ["Mảng"],
            levelCode: "EASY",
            levelDisplayName: "Dễ",
            acRate: 61.5,
            functionWrapperSupported: true,
            solveState: null,
          },
        ],
        page: { number: 1, size: 12, totalItems: 1, totalPages: 1 },
        solveStateAvailable: false,
      };
    throw new Error(`unexpected GET ${path}`);
  });
}

describe("ProblemListView for a guest", () => {
  beforeEach(() => {
    mockUrl.reset("/problems");
    resetApiMock();
    tokenStore.set(null);
    push.mockClear();
    localStorage.clear();
    serve();
  });

  it("reads only the list and the levels, and hides every personal part", async () => {
    render(withTestProviders(<ProblemListView />));
    await screen.findByText("Bài 1");

    const paths = apiMock.get.mock.calls.map(([path]) => path);
    expect(new Set(paths)).toEqual(
      new Set(["/problem-bank/levels", "/problem-bank/problems"]),
    );
    expect(screen.queryByText(m.inProgress.title)).toBeNull();
    expect(screen.queryByText(m.assignment.title)).toBeNull();
    expect(screen.queryByRole("group", { name: m.topicNav.label })).toBeNull();
    expect(screen.queryByText(m.table.colState)).toBeNull();
    expect(screen.queryByLabelText(m.filter.statusLabel)).toBeNull();
    expect(screen.queryByText(m.filter.statusLabel)).toBeNull();
    expect(screen.queryByText(m.degraded.solveState)).toBeNull();
  });

  it("ignores ?status= for a guest instead of filtering or failing", async () => {
    mockUrl.reset("/problems?status=solved");
    render(withTestProviders(<ProblemListView />));
    await screen.findByText("Bài 1");

    const call = apiMock.get.mock.calls.find(
      ([path]) => path === "/problem-bank/problems",
    );
    expect(call?.[1]).not.toHaveProperty("status", "SOLVED");
  });

  it("turns Saved problems into the sign-in dialog that returns to /saved", async () => {
    render(withTestProviders(<ProblemListView />));
    await screen.findByText("Bài 1");

    fireEvent.click(screen.getByRole("button", { name: m.summary.linkSaved }));

    expect(screen.getByText(prompt.title)).toBeInTheDocument();
    expect(screen.getByRole("link", { name: prompt.login })).toHaveAttribute(
      "href",
      `/login?redirect=${encodeURIComponent("/saved")}`,
    );
  });
});
