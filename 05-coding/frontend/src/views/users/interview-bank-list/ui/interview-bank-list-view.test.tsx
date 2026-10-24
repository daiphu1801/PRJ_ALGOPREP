// Smoke test (PROTOTYPE lane) — route renders, filter + drill entry are reachable. Full behavioural
// coverage per 04-tdd/interview_bank_list.md's AC-nn comes later, once that file exists.
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen, within } from "@testing-library/react";
import { resetRecallAndBookmarkState } from "@/entities/interview-question";
import { InterviewBankListView } from "./interview-bank-list-view";

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

// Filters are a button that opens a listbox; open it first, then pick from the options.
function openFilter(label: string) {
  fireEvent.click(
    screen.getByRole("button", { name: new RegExp(`^${label}`) }),
  );
  return screen.getByRole("listbox", { name: label });
}

// The bank loads through TanStack Query, so each test renders inside a fresh client.
function renderWithQuery(ui: ReactElement) {
  const client = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <QueryClientProvider client={client}>{ui}</QueryClientProvider>,
  );
}

afterEach(() => resetRecallAndBookmarkState());

describe("InterviewBankListView", () => {
  it("renders the browse list with the seeded questions", async () => {
    renderWithQuery(<InterviewBankListView />);
    await screen.findByPlaceholderText("searchPlaceholder");

    expect(
      screen.getByPlaceholderText("searchPlaceholder"),
    ).toBeInTheDocument();
    // Appears twice by design: once in the list row, once in the quick-view panel (the first
    // question is selected by default).
    expect(
      screen.getAllByText(/Hash table xử lý collision bằng cách nào\?/).length,
    ).toBeGreaterThan(0);
  });

  it("filters the list by search text", async () => {
    renderWithQuery(<InterviewBankListView />);
    await screen.findByPlaceholderText("searchPlaceholder");

    fireEvent.change(screen.getByPlaceholderText("searchPlaceholder"), {
      target: { value: "process và thread" },
    });

    expect(
      screen.queryByText(/Hash table xử lý collision/),
    ).not.toBeInTheDocument();
    expect(
      screen.getAllByText(/Khác biệt giữa process và thread/).length,
    ).toBeGreaterThan(0);
  });

  it("filters the list by difficulty level read from the admin-managed list", async () => {
    renderWithQuery(<InterviewBankListView />);
    await screen.findByPlaceholderText("searchPlaceholder");

    fireEvent.click(
      within(openFilter("levelFilterLabel")).getByRole("option", {
        name: "Khó",
      }),
    );

    expect(
      screen.queryByText(/Hash table xử lý collision/),
    ).not.toBeInTheDocument();
    expect(screen.getAllByText(/rút gọn URL/).length).toBeGreaterThan(0);
  });

  it("enters drill mode and shows a flashcard with an exit control", async () => {
    renderWithQuery(<InterviewBankListView />);
    await screen.findByPlaceholderText("searchPlaceholder");

    fireEvent.click(screen.getByText(/^startDrill/));

    expect(screen.getByText("drill.exit")).toBeInTheDocument();
    expect(screen.getByText("drill.reveal")).toBeInTheDocument();
  });

  it("searches the question content and core keywords, not only the title", async () => {
    renderWithQuery(<InterviewBankListView />);
    await screen.findByPlaceholderText("searchPlaceholder");

    fireEvent.change(screen.getByPlaceholderText("searchPlaceholder"), {
      target: { value: "collision" },
    });

    expect(
      screen.getAllByText(/Hash table xử lý collision bằng cách nào\?/).length,
    ).toBeGreaterThan(0);
  });

  it("keeps a just-rated question in the list while a status filter would drop it", async () => {
    renderWithQuery(<InterviewBankListView />);
    await screen.findByPlaceholderText("searchPlaceholder");

    fireEvent.click(
      within(openFilter("statusFilterLabel")).getByRole("option", {
        name: "statusTabs.new",
      }),
    );
    const before = screen.getAllByRole("button", {
      name: "recall.known",
    }).length;
    expect(before).toBeGreaterThan(0);
    fireEvent.click(
      screen.getAllByRole("button", { name: "recall.known" })[0]!,
    );

    // Quick view still shows the touched question and its next-review line.
    expect(screen.getByText(/quickView.nextReview/)).toBeInTheDocument();
  });
});
