// A2 (INSTRUCTOR) lists and edits only the problems they wrote (BD SHR0201 Q1); reading someone else's
// published problem is allowed, writing answers "not found" (BD SHR0203 Q8 as relaxed by
// DEC-2026-1011-a2-read-published-problems-of-others). The mock decides the viewer
// from the area the page is mounted under, so each test sets the address first.
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { ProblemAuthoringView } from "./problem-authoring";
import { ProblemInfoView } from "./problem-info";
import { ProblemManagementView } from "./problem-management";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
  useLocale: () => "vi",
}));

function withQuery(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return <QueryClientProvider client={client}>{ui}</QueryClientProvider>;
}

const at = (path: string) => window.history.pushState({}, "", path);

beforeEach(() => window.localStorage.clear());
afterEach(() => at("/"));

describe("problem list scope", () => {
  it("A3 sees every problem, A2 only their own", async () => {
    at("/admin/problems");
    const admin = render(
      withQuery(<ProblemManagementView basePath="/admin/problems" />),
    );
    expect(await screen.findByText("#207")).toBeInTheDocument();
    admin.unmount();

    at("/instructor/problems");
    render(
      withQuery(<ProblemManagementView basePath="/instructor/problems" />),
    );
    expect(await screen.findByText("#139")).toBeInTheDocument();
    expect(screen.queryByText("#207")).not.toBeInTheDocument();
  });
});

describe("opening a problem that is not the instructor's", () => {
  it("reads it (detail, preview) because it is published", async () => {
    at("/instructor/problems/207");
    render(
      withQuery(
        <ProblemInfoView problemId="207" basePath="/instructor/problems" />,
      ),
    );
    expect(
      await screen.findByRole("heading", { level: 1 }),
    ).toBeInTheDocument();
    expect(screen.queryByText("notFound")).not.toBeInTheDocument();
  });

  it("answers not found on the write routes (edit form, duplicate)", async () => {
    at("/instructor/problems/207/edit");
    const edit = render(
      withQuery(
        <ProblemAuthoringView
          problemId="207"
          basePath="/instructor/problems"
        />,
      ),
    );
    expect(await screen.findByText("notFound")).toBeInTheDocument();
    edit.unmount();

    at("/instructor/problems/new");
    render(
      withQuery(
        <ProblemAuthoringView basePath="/instructor/problems" fromId="207" />,
      ),
    );
    expect(await screen.findByText("notFound")).toBeInTheDocument();
  });

  it("their own problem still loads", async () => {
    at("/instructor/problems/139");
    render(
      withQuery(
        <ProblemInfoView problemId="139" basePath="/instructor/problems" />,
      ),
    );
    expect(
      await screen.findByRole("heading", {
        level: 1,
        name: "Minimum Window Substring",
      }),
    ).toBeInTheDocument();
  });
});
