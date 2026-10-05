// A2 (INSTRUCTOR) only sees and edits the problems they wrote (BD SHR0201 Q1); opening someone else's
// problem answers "not found", never a different error (BD SHR0203 Q8). The mock decides the viewer
// from the area the page is mounted under, so each test sets the address first.
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { render, screen } from "@testing-library/react";
import type { ReactElement } from "react";
import { ProblemAuthoringView } from "./problem-authoring";
import { ProblemInfoView } from "./problem-info";
import { ProblemManagementView } from "./problem-management";
import { ProblemPreviewView } from "./problem-preview";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
  useLocale: () => "vi",
}));

function withQuery(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={client}>{ui}</QueryClientProvider>;
}

const at = (path: string) => window.history.pushState({}, "", path);

beforeEach(() => window.localStorage.clear());
afterEach(() => at("/"));

describe("problem list scope", () => {
  it("A3 sees every problem, A2 only their own", async () => {
    at("/admin/problems");
    const admin = render(withQuery(<ProblemManagementView basePath="/admin/problems" />));
    expect(await screen.findByText("#207")).toBeInTheDocument();
    admin.unmount();

    at("/instructor/problems");
    render(withQuery(<ProblemManagementView basePath="/instructor/problems" />));
    expect(await screen.findByText("#139")).toBeInTheDocument();
    expect(screen.queryByText("#207")).not.toBeInTheDocument();
  });
});

describe("opening a problem that is not the instructor's", () => {
  beforeEach(() => at("/instructor/problems/207"));

  it.each([
    ["detail", <ProblemInfoView key="i" problemId="207" basePath="/instructor/problems" />],
    ["edit form", <ProblemAuthoringView key="a" problemId="207" basePath="/instructor/problems" />],
    ["preview", <ProblemPreviewView key="p" problemId="207" />],
  ])("%s answers not found", async (_name, view) => {
    render(withQuery(view));
    expect(await screen.findByText("notFound")).toBeInTheDocument();
  });

  it("their own problem still loads", async () => {
    at("/instructor/problems/139");
    render(withQuery(<ProblemInfoView problemId="139" basePath="/instructor/problems" />));
    expect(await screen.findByRole("heading", { level: 1, name: "Minimum Window Substring" })).toBeInTheDocument();
  });
});
