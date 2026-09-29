import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { ProblemDetailView } from "./problem-detail-view";

let currentProblemId: string | undefined = "1";
vi.mock("next/navigation", () => ({
  useParams: () => ({ problemId: currentProblemId }),
}));

function renderView() {
  return render(
    <NextIntlClientProvider locale="vi" messages={messages}>
      <ProblemDetailView />
    </NextIntlClientProvider>,
  );
}

describe("ProblemDetailView", () => {
  it("renders the problem statement and the primary actions (Run Code / Submit)", () => {
    currentProblemId = "1";
    renderView();

    expect(screen.getByText("Two Sum")).toBeInTheDocument();
    expect(screen.getByText("Run Code")).toBeInTheDocument();
    expect(screen.getByText("Submit")).toBeInTheDocument();
  });

  it("shows the empty state for an unknown problem id", () => {
    currentProblemId = "does-not-exist";
    renderView();

    expect(screen.getByText("Chưa chọn bài toán")).toBeInTheDocument();
  });
});
