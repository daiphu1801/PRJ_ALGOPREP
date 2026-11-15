import { render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError } from "@/shared/api";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../../messages/vi.json";

// `override` lets one test force the query state; null keeps the real mock-backed hook.
const override = vi.hoisted(() => ({ value: null as unknown }));
vi.mock("@/entities/problem", async (importOriginal) => {
  const actual = await importOriginal<Record<string, unknown>>();
  return {
    ...actual,
    useProblemDraft: (...args: unknown[]) =>
      override.value ??
      (actual.useProblemDraft as (...a: unknown[]) => unknown)(...args),
  };
});

import { ProblemPreviewView } from "./problem-preview-view";

const t = viMessages.problemPreview;

beforeEach(() => {
  override.value = null;
  window.localStorage.clear();
});

describe("ProblemPreviewView", () => {
  it("shows a skeleton first, then the sample problem when nothing was saved here", async () => {
    render(withTestProviders(<ProblemPreviewView problemId="121" />));
    expect(document.querySelector("[aria-busy='true']")).not.toBeNull();

    expect(
      await screen.findByText("Minimum Window Substring"),
    ).toBeInTheDocument();
    expect(screen.getByText(t.neverSaved)).toBeInTheDocument();
    expect(screen.getByText(t.constraintsLabel)).toBeInTheDocument();
    expect(screen.getByText(t.limitsTitle)).toBeInTheDocument();
    expect(screen.getByText(/ms ·/)).toBeInTheDocument();
  });

  it("lists every example with input and output", async () => {
    render(withTestProviders(<ProblemPreviewView problemId="121" />));
    await screen.findByText("Minimum Window Substring");

    const list = screen.getByRole("list");
    const items = within(list).getAllByRole("listitem");
    expect(items.length).toBeGreaterThan(0);
    expect(within(items[0]!).getByText(t.exampleInput)).toBeInTheDocument();
    expect(within(items[0]!).getByText(t.exampleOutput)).toBeInTheDocument();
  });

  it("says when the shown copy was saved and hides empty explanations", async () => {
    override.value = {
      isError: false,
      error: null,
      data: {
        savedAt: "2026-01-02T03:04:05.000Z",
        draft: {
          title: "Saved Title",
          difficulty: "EASY",
          body: "Body text",
          constraints: "n <= 10",
          limits: { timeLimitMs: 1500, memoryLimitMb: 128 },
          examples: [
            { id: "e1", input: "1", output: "2", explanation: "because" },
            { id: "e2", input: "3", output: "4", explanation: "" },
          ],
        },
      },
    };
    render(withTestProviders(<ProblemPreviewView problemId="9" />));

    expect(await screen.findByText("Saved Title")).toBeInTheDocument();
    expect(screen.getByText(/Bản đã lưu lúc/)).toBeInTheDocument();
    expect(screen.getByText(/1500 ms/)).toBeInTheDocument();
    expect(screen.getByText("because")).toBeInTheDocument();
    // Only the example that has an explanation renders the label.
    expect(screen.getAllByText(t.exampleExplanation)).toHaveLength(1);
  });

  it("shows the not-found state for a missing problem", async () => {
    override.value = {
      isError: true,
      error: new ApiError("PROBLEM_NOT_FOUND", 404, "nope"),
      data: undefined,
    };
    render(withTestProviders(<ProblemPreviewView problemId="x" />));

    await waitFor(() =>
      expect(screen.getByText(t.notFound)).toBeInTheDocument(),
    );
  });

  it("shows the load-failed state for any other error", () => {
    override.value = {
      isError: true,
      error: new ApiError("INTERNAL", 500, "boom"),
      data: undefined,
    };
    render(withTestProviders(<ProblemPreviewView problemId="x" />));

    expect(screen.getByText(t.loadFailed)).toBeInTheDocument();
  });
});
