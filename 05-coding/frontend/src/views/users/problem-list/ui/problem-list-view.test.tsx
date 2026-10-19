import { afterEach, describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  within,
} from "@testing-library/react";
import {
  addProblemLevel,
  removeProblemLevel,
  useProblemLevels,
} from "@/entities/problem";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { ProblemListView } from "./problem-list-view";

const push = vi.fn();
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push }),
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
  fireEvent.click(
    screen.getByRole("button", { name: new RegExp(`^${label}`) }),
  );
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
    expect(
      within(group)
        .getAllByRole("option")
        .map((b) => b.textContent),
    ).toEqual(["Tất cả", "Dễ", "Trung bình", "Khó"]);

    const rowsBefore = screen.getAllByRole("row").length;
    fireEvent.click(within(group).getByRole("option", { name: "Khó" }));
    expect(screen.getAllByRole("row").length).toBeLessThan(rowsBefore);
  });

  it("adds a tab for a level the admin created", () => {
    act(() => void addProblemLevel("Rất khó"));
    renderView();
    const group = openFilter("Lọc theo độ khó");
    expect(
      within(group).getByRole("option", { name: "Rất khó" }),
    ).toBeInTheDocument();
  });

  it("has no KPI strip: the only section headings are the two side blocks (removed 2026-10-18)", () => {
    renderView();
    expect(
      screen.getAllByRole("heading", { level: 2 }).map((h) => h.textContent),
    ).toEqual(["Bài đang làm dở", "Bài tập lớp"]);
  });
});

describe("ProblemListView behaviour from the BD", () => {
  it("labels only Standard I/O problems with a submission model badge", () => {
    renderView();
    expect(screen.queryByText("Cả hai")).not.toBeInTheDocument();
    expect(screen.queryByText("Chỉ Standard I/O")).not.toBeInTheDocument();

    fireEvent.change(screen.getByPlaceholderText("Tìm theo tên bài hoặc mã bài"), {
      target: { value: "#297" },
    });
    expect(screen.getByText("Chỉ Standard I/O")).toBeInTheDocument();
  });

  it("pressing the active topic again clears the topic filter (EVT-5)", () => {
    renderView();
    const topic = screen.getByRole("button", { name: /^Array/ });
    fireEvent.click(topic);
    expect(topic).toHaveAttribute("aria-pressed", "true");
    fireEvent.click(topic);
    expect(topic).toHaveAttribute("aria-pressed", "false");
    expect(screen.getByRole("button", { name: "Tất cả" })).toHaveAttribute(
      "aria-pressed",
      "true",
    );
  });

  it("offers a clear-filters action in the empty state (EVT-6)", () => {
    renderView();
    fireEvent.change(screen.getByPlaceholderText("Tìm theo tên bài hoặc mã bài"), {
      target: { value: "zzzz-no-match" },
    });
    expect(screen.getByText("Không có bài nào khớp bộ lọc")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Xóa bộ lọc" }));
    expect(screen.getByText("Two Sum")).toBeInTheDocument();
  });

  it("opens a random problem from the filtered set", () => {
    push.mockClear();
    renderView();
    fireEvent.click(screen.getByRole("button", { name: /Bài ngẫu nhiên/ }));
    expect(push).toHaveBeenCalledWith(expect.stringMatching(/^\/problems\/\w+/));
  });

  it("maps the last verdict and the assignment meta through i18n", () => {
    renderView();
    expect(screen.getByText(/Sai kết quả 8\/12/)).toBeInTheDocument();
    expect(screen.getAllByText("0 / 5 bài · giao 24/08/2026").length).toBe(2);
  });
});
