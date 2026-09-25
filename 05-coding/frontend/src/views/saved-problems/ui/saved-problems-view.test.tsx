import { describe, expect, it } from "vitest";
import { fireEvent, render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../messages/vi.json";
import { SavedProblemsView } from "./saved-problems-view";

function renderView() {
  return render(
    <NextIntlClientProvider locale="vi" messages={messages}>
      <SavedProblemsView />
    </NextIntlClientProvider>,
  );
}

describe("SavedProblemsView", () => {
  it("renders the page title and the bookmarked rows", () => {
    renderView();

    expect(screen.getByText("Bài đã lưu")).toBeInTheDocument();
    expect(screen.getByText("Two Sum")).toBeInTheDocument();
  });

  it("removes a row from the list when Bỏ lưu is clicked (REQ-04)", () => {
    renderView();

    expect(screen.getByText("Two Sum")).toBeInTheDocument();
    const [firstUnsaveButton] = screen.getAllByText("Bỏ lưu");
    fireEvent.click(firstUnsaveButton!);

    expect(screen.queryByText("Two Sum")).not.toBeInTheDocument();
  });
});
