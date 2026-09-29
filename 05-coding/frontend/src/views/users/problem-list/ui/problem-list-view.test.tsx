import { describe, expect, it, vi } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
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
