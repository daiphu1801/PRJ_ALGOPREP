// Smoke test (PROTOTYPE lane) — route renders, filter + drill entry are reachable. Full behavioural
// coverage per 04-tdd/interview_bank_list.md's AC-nn comes later, once that file exists.
import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { InterviewBankListView } from "./interview-bank-list-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

// Filters are a button that opens a listbox; open it first, then pick from the options.
function openFilter(label: string) {
  fireEvent.click(screen.getByRole("button", { name: new RegExp(`^${label}`) }));
  return screen.getByRole("listbox", { name: label });
}

describe("InterviewBankListView", () => {
  it("renders the browse list with the seeded questions", () => {
    render(<InterviewBankListView />);

    expect(screen.getByPlaceholderText("searchPlaceholder")).toBeInTheDocument();
    // Appears twice by design: once in the list row, once in the quick-view panel (the first
    // question is selected by default).
    expect(screen.getAllByText(/Hash table xử lý collision bằng cách nào\?/).length).toBeGreaterThan(0);
  });

  it("filters the list by search text", () => {
    render(<InterviewBankListView />);

    fireEvent.change(screen.getByPlaceholderText("searchPlaceholder"), {
      target: { value: "process và thread" },
    });

    expect(screen.queryByText(/Hash table xử lý collision/)).not.toBeInTheDocument();
    expect(screen.getAllByText(/Khác biệt giữa process và thread/).length).toBeGreaterThan(0);
  });

  it("filters the list by difficulty level read from the admin-managed list", () => {
    render(<InterviewBankListView />);

    fireEvent.click(within(openFilter("levelFilterLabel")).getByRole("option", { name: "Khó" }));

    expect(screen.queryByText(/Hash table xử lý collision/)).not.toBeInTheDocument();
    expect(screen.getAllByText(/rút gọn URL/).length).toBeGreaterThan(0);
  });

  it("enters drill mode and shows a flashcard with an exit control", () => {
    render(<InterviewBankListView />);

    fireEvent.click(screen.getByText(/^startDrill/));

    expect(screen.getByText("drill.exit")).toBeInTheDocument();
    expect(screen.getByText("drill.reveal")).toBeInTheDocument();
  });
});
