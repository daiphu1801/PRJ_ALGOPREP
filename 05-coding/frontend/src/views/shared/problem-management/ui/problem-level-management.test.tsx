// Difficulty levels on the problem list (DEC-2026-1001, round 6): the ADMIN-only manager, the filter
// tabs that follow the list, and the bulk "Đổi độ khó" picker that also frees a level before deleting it.
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  within,
} from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { removeProblemLevel, useProblemLevels } from "@/entities/problem";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { ProblemManagementView } from "./problem-management-view";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));
vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

beforeEach(() => act(() => toast.clear()));

// Levels added by a test are removed again so the module-level list starts clean for the next one.
afterEach(() => {
  const levels = renderHook(() => useProblemLevels());
  for (const level of levels.result.current.filter(
    (item) => !["EASY", "MEDIUM", "HARD"].includes(item.key),
  )) {
    act(() => void removeProblemLevel(level.key));
  }
});

async function renderLoaded(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
  await screen.findByLabelText("searchLabel");
}

// Filters are a button that opens a listbox; open it first, then pick from the options.
function openFilter(label: string) {
  fireEvent.click(
    screen.getByRole("button", { name: new RegExp(`^${label}`) }),
  );
  return screen.getByRole("listbox", { name: label });
}

describe("problem level management", () => {
  it("shows the level manager to ADMIN only", async () => {
    await renderLoaded(
      <ProblemManagementView basePath="/admin/problems" canManageTopics />,
    );
    expect(
      screen.getByRole("button", { name: "manageLevels" }),
    ).toBeInTheDocument();
  });

  it("hides the level manager from INSTRUCTOR", async () => {
    await renderLoaded(
      <ProblemManagementView basePath="/instructor/problems" />,
    );
    expect(
      screen.queryByRole("button", { name: "manageLevels" }),
    ).not.toBeInTheDocument();
  });

  it("a level added in the manager becomes a filter tab", async () => {
    await renderLoaded(
      <ProblemManagementView basePath="/admin/problems" canManageTopics />,
    );
    fireEvent.click(screen.getByRole("button", { name: "manageLevels" }));
    const dialog = screen.getByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText("levelManager.newLabel"), {
      target: { value: "Cực khó" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: "levelManager.add" }),
    );
    fireEvent.click(
      within(dialog).getByRole("button", { name: "levelManager.close" }),
    );

    expect(
      within(openFilter("difficultyFilterLabel")).getByRole("option", {
        name: "Cực khó",
      }),
    ).toBeInTheDocument();
  });

  it("refuses to delete a level that problems still use", async () => {
    await renderLoaded(
      <ProblemManagementView basePath="/admin/problems" canManageTopics />,
    );
    fireEvent.click(screen.getByRole("button", { name: "manageLevels" }));
    const dialog = screen.getByRole("dialog");
    expect(
      within(dialog).getAllByRole("button", {
        name: /^levelManager\.deleteBlocked/,
      }),
    ).toHaveLength(3);
  });

  it("bulk change moves the selected problems to another level, which frees the old one to delete", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(
      <ProblemManagementView basePath="/admin/problems" canManageTopics />,
    );

    // Add a level nothing uses, then move every visible problem onto it through the bulk picker.
    fireEvent.click(screen.getByRole("button", { name: "manageLevels" }));
    let dialog = screen.getByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText("levelManager.newLabel"), {
      target: { value: "Cực khó" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: "levelManager.add" }),
    );
    fireEvent.click(
      within(dialog).getByRole("button", { name: "levelManager.close" }),
    );

    fireEvent.click(screen.getByRole("checkbox", { name: "selectAll" }));
    fireEvent.click(
      screen.getByRole("button", { name: "bulk.changeDifficulty" }),
    );
    dialog = screen.getByRole("dialog");
    fireEvent.change(within(dialog).getByLabelText("bulkLevel.label"), {
      target: { value: "CUC_KHO" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: "bulkLevel.apply" }),
    );

    expect(toasts.result.current.at(-1)?.message).toMatch(
      /^toast\.bulk\.changeDifficulty/,
    );
    fireEvent.click(
      within(openFilter("difficultyFilterLabel")).getByRole("option", {
        name: "Cực khó",
      }),
    );
    expect(
      screen.getAllByRole("button", { name: /^deleteProblem/ }).length,
    ).toBeGreaterThan(0);
  });
});
