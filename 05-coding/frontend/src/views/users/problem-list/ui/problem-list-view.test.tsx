import { afterEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, renderHook, screen, within } from "@testing-library/react";
import { addProblemLevel, removeProblemLevel, useProblemLevels } from "@/entities/problem";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { ProblemListView } from "./problem-list-view";

vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn() }),
}));

function renderView() {
  return render(
    <NextIntlClientProvider locale="vi" messages={messages}>
      <ProblemListView />
    </NextIntlClientProvider>,
  );
}

// Filters are a button that opens a listbox; open it first, then pick from the options.
function openFilter(label: string) {
  fireEvent.click(screen.getByRole("button", { name: new RegExp(`^${label}`) }));
  return screen.getByRole("listbox", { name: label });
}

describe("ProblemListView", () => {
  it("renders the page title and the primary action reachable (Vào giải)", () => {
    renderView();

    expect(screen.getByText("Danh sách bài toán")).toBeInTheDocument();
    expect(screen.getAllByText("Vào giải").length).toBeGreaterThan(0);
  });

  it("filters the table by search query", () => {
    renderView();

    expect(screen.getByText("Two Sum")).toBeInTheDocument();
    const search = screen.getByPlaceholderText("Tìm theo tên bài hoặc mã bài");
    fireEvent.change(search, { target: { value: "#139" } });

    // "Word Break" also shows up in the unrelated "Bài đang làm dở" side panel (not filtered by the
    // search box), so assert on the unique row link instead of the bare title text.
    expect(screen.getByRole("link", { name: /#139/ })).toBeInTheDocument();
    expect(screen.queryByText("Two Sum")).not.toBeInTheDocument();
  });
});

describe("ProblemListView difficulty tabs", () => {
  afterEach(() => {
    const { result } = renderHook(() => useProblemLevels());
    const extra = result.current.find((l) => l.label === "Rất khó");
    if (extra) act(() => void removeProblemLevel(extra.key));
  });

  it("shows one tab per level in admin order and filters by it", () => {
    renderView();
    const group = openFilter("Lọc theo độ khó");
    expect(within(group).getAllByRole("option").map((b) => b.textContent)).toEqual([
      "Tất cả",
      "Dễ",
      "Trung bình",
      "Khó",
    ]);

    const rowsBefore = screen.getAllByRole("row").length;
    fireEvent.click(within(group).getByRole("option", { name: "Khó" }));
    expect(screen.getAllByRole("row").length).toBeLessThan(rowsBefore);
  });

  it("adds a tab for a level the admin created", () => {
    act(() => void addProblemLevel("Rất khó"));
    renderView();
    const group = openFilter("Lọc theo độ khó");
    expect(within(group).getByRole("option", { name: "Rất khó" })).toBeInTheDocument();
  });

  it("shows one progress card per level, and 0 / 0 for a level with no problems yet", () => {
    act(() => void addProblemLevel("Rất khó"));
    renderView();
    // Seed levels carry the mock numbers; the level the admin just created has no problems.
    expect(screen.getByText("20 / 40")).toBeInTheDocument();
    expect(screen.getByText("4 / 28")).toBeInTheDocument();
    expect(screen.getByText("0 / 0")).toBeInTheDocument();
  });
});
