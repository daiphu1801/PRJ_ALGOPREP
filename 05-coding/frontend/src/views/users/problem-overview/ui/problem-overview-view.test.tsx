import { describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { ProblemOverviewView } from "./problem-overview-view";

let currentProblemId: string | undefined = "121";
vi.mock("next/navigation", () => ({
  useParams: () => ({ problemId: currentProblemId }),
  useRouter: () => ({ push: vi.fn() }),
}));

// The problem arrives through a query: wait for the loading skeleton to go before asserting.
async function wrap(node: React.ReactNode) {
  const view = render(withTestProviders(node));
  await waitFor(() =>
    expect(
      screen.queryByRole("status", { name: "Đang tải..." }),
    ).not.toBeInTheDocument(),
  );
  return view;
}

describe("ProblemOverviewView", () => {
  it("opens a catalogue problem and links the solve button to the workspace", async () => {
    await wrap(<ProblemOverviewView />);
    expect(
      screen.getByRole("heading", { level: 1, name: "Best Time to Buy and Sell Stock" }),
    ).toBeInTheDocument();
    expect(screen.getByRole("link", { name: /Giải bài toán/ })).toHaveAttribute(
      "href",
      "/workspace/121",
    );
    expect(screen.queryByText("Run Code")).not.toBeInTheDocument();
  });

  it("lists recent submissions newest first as links, and links the full review of the latest Accepted one", async () => {
    currentProblemId = "1";
    await wrap(<ProblemOverviewView />);
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

  it("shows not-found for an unknown id", async () => {
    currentProblemId = "does-not-exist";
    await wrap(<ProblemOverviewView />);
    expect(screen.getByText("Không tìm thấy bài toán.")).toBeInTheDocument();
    currentProblemId = "121";
  });
});
