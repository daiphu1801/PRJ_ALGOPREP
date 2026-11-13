// The error path of the list: the query fails, the screen shows the failure message and no table, but
// the filters stay so the admin can change them and try again.
import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { ProblemManagementView } from "./problem-management-view";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
  useLocale: () => "vi",
  useApiErrorMessage: () => () => "error",
}));
vi.mock("../api", async (importOriginal) => ({
  ...(await importOriginal<Record<string, unknown>>()),
  useAdminProblemList: () => ({ isError: true, data: undefined }),
}));

describe("ProblemManagementView load failure", () => {
  it("shows the load-failed message instead of the table, and keeps the filters", () => {
    render(
      <QueryClientProvider client={new QueryClient()}>
        <ProblemManagementView basePath="/admin/problems" />
      </QueryClientProvider>,
    );
    expect(screen.getByText("loadFailed")).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
    expect(screen.getByLabelText("searchLabel")).toBeInTheDocument();
  });
});
