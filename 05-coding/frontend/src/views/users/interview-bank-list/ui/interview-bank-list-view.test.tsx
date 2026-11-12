// Mock-mode behaviour of USR0401: the mock behaves like the server (filters, pages, keeps recall), so
// every filter change is an asynchronous reload. The real-API branches are in
// interview-bank-list-real.test.tsx.
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  fireEvent,
  render,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
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

// The first page has arrived once the search box is on screen and a row is listed.
async function loaded() {
  await screen.findByPlaceholderText("searchPlaceholder");
  await screen.findAllByText(/Hash table xử lý collision bằng cách nào\?/);
}

afterEach(() => resetRecallAndBookmarkState());

describe("InterviewBankListView", () => {
  it("renders the browse list with the seeded questions", async () => {
    renderWithQuery(<InterviewBankListView />);
    await loaded();

    expect(
      screen.getByPlaceholderText("searchPlaceholder"),
    ).toBeInTheDocument();
    // Appears twice by design: once in the list row, once in the quick-view panel (the first
    // question is selected by default).
    expect(
      screen.getAllByText(/Hash table xử lý collision bằng cách nào\?/).length,
    ).toBe(2);
    // The count line reads the server's `meta`.
    expect(screen.getByRole("status")).toHaveTextContent(
      /shownCount \{"shown":\d+,"total":\d+\}/,
    );
  });

  it("filters the list by search text, which reaches the server after a pause", async () => {
    renderWithQuery(<InterviewBankListView />);
    await loaded();

    fireEvent.change(screen.getByPlaceholderText("searchPlaceholder"), {
      target: { value: "process và thread" },
    });

    await waitFor(() =>
      expect(
        screen.queryByText(/Hash table xử lý collision/),
      ).not.toBeInTheDocument(),
    );
    expect(
      screen.getAllByText(/Khác biệt giữa process và thread/).length,
    ).toBeGreaterThan(0);
  });

  it("filters the list by difficulty level read from the admin-managed list", async () => {
    renderWithQuery(<InterviewBankListView />);
    await loaded();

    fireEvent.click(
      within(openFilter("levelFilterLabel")).getByRole("option", {
        name: "Khó",
      }),
    );

    await waitFor(() =>
      expect(
        screen.queryByText(/Hash table xử lý collision/),
      ).not.toBeInTheDocument(),
    );
    expect(screen.getAllByText(/rút gọn URL/).length).toBeGreaterThan(0);
  });

  it("enters drill mode and shows a flashcard with an exit control", async () => {
    renderWithQuery(<InterviewBankListView />);
    await loaded();

    fireEvent.click(screen.getByText(/^startDrill/));

    expect(await screen.findByText("drill.exit")).toBeInTheDocument();
    expect(screen.getByText("drill.reveal")).toBeInTheDocument();
  });

  it("searches the question content and core keywords, not only the title", async () => {
    renderWithQuery(<InterviewBankListView />);
    await loaded();

    fireEvent.change(screen.getByPlaceholderText("searchPlaceholder"), {
      target: { value: "collision" },
    });

    await waitFor(() =>
      expect(
        screen.queryByText(/Khác biệt giữa process và thread/),
      ).not.toBeInTheDocument(),
    );
    expect(
      screen.getAllByText(/Hash table xử lý collision bằng cách nào\?/).length,
    ).toBeGreaterThan(0);
  });

  it("keeps a just-rated question in the list while a status filter would drop it", async () => {
    renderWithQuery(<InterviewBankListView />);
    await loaded();

    fireEvent.click(
      within(openFilter("statusFilterLabel")).getByRole("option", {
        name: "statusTabs.new",
      }),
    );
    // The reload for "new" has landed once the "known" question IQ-021 has left the list.
    await waitFor(() =>
      expect(
        screen.queryByText(/Khác biệt giữa process và thread/),
      ).not.toBeInTheDocument(),
    );
    const title = screen.getByRole("heading", { level: 2 }).textContent!;
    fireEvent.click(
      screen.getAllByRole("button", { name: "recall.known" })[0]!,
    );

    // The server saves the rating, then the quick view shows the review date (7 days after "known").
    expect(
      await screen.findByText(/quickView.nextReview \{"n":7\}/),
    ).toBeInTheDocument();
    // Row and quick view both still show the question the filter would now drop.
    expect(screen.getAllByText(title).length).toBe(2);
  });
});
