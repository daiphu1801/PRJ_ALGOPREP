import { describe, expect, it, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { NextIntlClientProvider } from "@/shared/i18n";
import messages from "../../../../../messages/vi.json";
import { ProblemOverviewView } from "./problem-overview-view";

let currentProblemId: string | undefined = "121";
vi.mock("next/navigation", () => ({
  useParams: () => ({ problemId: currentProblemId }),
  useRouter: () => ({ push: vi.fn() }),
}));

function wrap(node: React.ReactNode) {
  return render(
    <NextIntlClientProvider locale="vi" messages={messages}>
      {node}
    </NextIntlClientProvider>,
  );
}

describe("ProblemOverviewView", () => {
  it("opens a catalogue problem and links the solve button to the workspace", () => {
    wrap(<ProblemOverviewView />);
    expect(
      screen.getByRole("heading", { level: 1, name: "Best Time to Buy and Sell Stock" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Giải bài toán/ })).toHaveAttribute(
      "href",
      "/workspace/121",
    );
    expect(screen.queryByText("Run Code")).not.toBeInTheDocument();
  });

  it("lists recent submissions newest first as links, and links the full review of the latest Accepted one", () => {
    currentProblemId = "1";
    wrap(<ProblemOverviewView />);
    const row = screen.getByRole("link", { name: /Đã chấp nhận|Accepted/ });
    expect(row).toHaveAttribute("href", "/submissions/s1");
    const links = screen
      .getAllByRole("link")
      .map((link) => link.getAttribute("href"))
      .filter((href) => /^\/submissions\/s\d$/.test(href ?? ""));
    expect(links).toEqual(["/submissions/s1", "/submissions/s2"]);
    expect(
      screen.getByRole("link", { name: "Mở báo cáo đầy đủ" }),
    ).toHaveAttribute("href", "/submissions/s1/review");
    currentProblemId = "121";
  });

  it("shows not-found for an unknown id", () => {
    currentProblemId = "does-not-exist";
    wrap(<ProblemOverviewView />);
    expect(screen.getByText("Không tìm thấy bài toán.")).toBeInTheDocument();
    currentProblemId = "121";
  });
});
