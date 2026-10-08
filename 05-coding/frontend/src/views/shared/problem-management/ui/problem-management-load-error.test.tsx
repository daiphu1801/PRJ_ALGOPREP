// The error path of the list: the query fails, the screen shows the failure message and no table.
import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { ProblemManagementView } from "./problem-management-view";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string) => key,
}));
vi.mock("../api", () => ({
  useAdminProblemPage: () => ({ isError: true, data: undefined }),
}));

describe("ProblemManagementView load failure", () => {
  it("shows the load-failed message instead of the table", () => {
    render(<ProblemManagementView basePath="/admin/problems" />);
    expect(screen.getByText("loadFailed")).toBeInTheDocument();
    expect(screen.queryByRole("table")).toBeNull();
  });
});
