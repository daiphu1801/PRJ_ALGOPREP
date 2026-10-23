// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0301_solution_review).
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { withTestProviders } from "@/shared/test/render-with-providers";
import { SolutionReviewView } from "./solution-review-view";

let currentSubmissionId = "SB-90412";
let currentDemo: string | null = null;
const push = vi.fn();
vi.mock("next/navigation", () => ({
  useSearchParams: () => {
    const params = new URLSearchParams();
    if (currentDemo) params.set("demo", currentDemo);
    return params;
  },
  useParams: () => ({ submissionId: currentSubmissionId }),
  useRouter: () => ({ push }),
}));

function renderView() {
  return render(withTestProviders(<SolutionReviewView />));
}

beforeEach(() => {
  currentSubmissionId = "SB-90412";
  currentDemo = null;
  push.mockClear();
  window.localStorage.clear();
});

describe("SolutionReviewView", () => {
  it("shows the report once the simulated load resolves (BD Sheet 5 Khu vực I, ready state)", async () => {
    renderView();

    expect(await screen.findByText("1. Two Sum")).toBeInTheDocument();
    expect(screen.getByText("Phân tích bài giải")).toBeInTheDocument();
    // Educational disclaimer (F5-18) must be shown alongside the report.
    expect(screen.getByText(/Phản hồi để học/)).toBeInTheDocument();
  });

  it("lists the follow-up topics as plain chips with one link to the question bank (F5-06)", async () => {
    renderView();

    expect(await screen.findByText("Chủ đề nên học tiếp")).toBeInTheDocument();
    // "hash map" is also a topic badge in the page header, so read the chips from the list items.
    const chips = screen.getAllByRole("listitem").map((item) => item.textContent);
    expect(chips).toEqual(expect.arrayContaining(["hash map", "two pointers", "3Sum"]));
    const cta = screen.getByRole("link", { name: "Mở ngân hàng câu hỏi" });
    expect(cta).toHaveAttribute("href", "/interview-bank");
  });

  it("takes the submission id from the route, in the breadcrumb and the interview link", async () => {
    currentSubmissionId = "SB-90305";
    renderView();

    await screen.findByText("1. Two Sum");
    const crumbs = screen
      .getAllByRole("link")
      .filter((link) => link.getAttribute("href") === "/submissions/SB-90305");
    expect(crumbs).toHaveLength(2);
    expect(
      screen
        .getAllByRole("link")
        .some(
          (link) =>
            link.getAttribute("href") === "/submissions/SB-90305/interview",
        ),
    ).toBe(true);
  });

  it("shows a not-found notice without a Retry button for an unknown id", async () => {
    currentSubmissionId = "nope";
    renderView();

    await waitFor(() =>
      expect(screen.queryByText("Thử lại")).not.toBeInTheDocument(),
    );
    expect(await screen.findByText(/Về Bài đã nộp|backToSubmissions/)).toBeInTheDocument();
    expect(screen.queryByText("1. Two Sum")).not.toBeInTheDocument();
  });

  it("overwrites the workspace draft with the suggestion and opens the problem (EVT-6)", async () => {
    renderView();

    await screen.findByText("1. Two Sum");
    fireEvent.click(screen.getByRole("button", { name: "Áp dụng vào Workspace" }));
    fireEvent.click(screen.getByRole("button", { name: "Ghi đè & Mở Workspace" }));

    const saved = window.localStorage.getItem(
      "algoprep-draft:1:python:function",
    );
    expect(saved).toContain("seen.get(target - x)");
    expect(push).toHaveBeenCalledWith("/workspace/1");
  });

  it("offers Retry on a transient error", async () => {
    currentDemo = "errorTransient";
    renderView();

    expect(await screen.findByText("Thử lại")).toBeInTheDocument();
  });
});
