import { fireEvent, render, screen, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import type { SampleRunResult, SubmissionRunResult } from "@/entities/problem";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../../messages/vi.json";
import { RunResultsDialog } from "./run-results-dialog";

const c = viMessages.problemDetail.console;

const sampleRun: SampleRunResult = {
  cases: [
    {
      input: "in-1",
      expectedOutput: "out-1",
      actualOutput: "out-1",
      passed: true,
      runtimeMs: 3,
      memoryKb: 36864,
    },
    {
      input: "in-2",
      expectedOutput: "out-2",
      actualOutput: "wrong-2",
      passed: false,
      runtimeMs: 5,
      memoryKb: 37376,
    },
  ],
};

const baseResult: SubmissionRunResult = {
  submissionId: "sub-9",
  overallVerdict: "accepted",
  passedCount: 2,
  totalCount: 2,
  runtimeMs: 12,
  memoryKb: 41984,
  testcases: [
    { order: 1, verdict: "passed" },
    { order: 2, verdict: "passed" },
  ],
};

function renderDialog(props: Partial<Parameters<typeof RunResultsDialog>[0]>) {
  const onClose = vi.fn();
  render(
    withTestProviders(
      <RunResultsDialog
        mode="run"
        running={false}
        sampleRun={null}
        result={null}
        onClose={onClose}
        {...props}
      />,
    ),
  );
  return { onClose, dialog: within(screen.getByRole("dialog")) };
}

describe("RunResultsDialog in run mode", () => {
  it("opens on the first failing sample and shows input, actual and expected output", () => {
    const { dialog } = renderDialog({ sampleRun });

    expect(dialog.getByText(c.dialog.titleRun)).toBeInTheDocument();
    expect(dialog.getByText("in-2")).toBeInTheDocument();
    expect(dialog.getByText("wrong-2")).toBeInTheDocument();
    expect(dialog.getByText("out-2")).toBeInTheDocument();
    expect(
      dialog.getByText("Thời gian 5 ms · Bộ nhớ 37 MB"),
    ).toBeInTheDocument();
    expect(dialog.getByText("1/2 testcase mẫu đạt")).toBeInTheDocument();
    expect(dialog.getByRole("button", { name: /Case 2/ })).toHaveAttribute(
      "aria-current",
      "true",
    );
  });

  it("switches the detail pane when another case is picked", () => {
    const { dialog } = renderDialog({ sampleRun });

    fireEvent.click(dialog.getByRole("button", { name: /Case 1/ }));

    expect(dialog.getByText("in-1")).toBeInTheDocument();
    expect(dialog.queryByText("wrong-2")).not.toBeInTheDocument();
  });

  it("starts on case 1 when every sample passed", () => {
    const allPass: SampleRunResult = {
      cases: [sampleRun.cases[0]!],
    };
    const { dialog } = renderDialog({ sampleRun: allPass });

    expect(dialog.getByText("1/1 testcase mẫu đạt")).toBeInTheDocument();
    expect(dialog.getByText("in-1")).toBeInTheDocument();
  });

  it("shows only the compiler message when the code did not compile", () => {
    const { dialog } = renderDialog({
      sampleRun: { cases: [], compileErrorMessage: "error: ';' expected" },
    });

    expect(dialog.getByText("error: ';' expected")).toBeInTheDocument();
    expect(dialog.queryByRole("list")).not.toBeInTheDocument();
  });

  it("says it is running while there is no result yet, and nothing once idle", () => {
    const { dialog } = renderDialog({ running: true });
    expect(dialog.getByText(c.running)).toBeInTheDocument();
  });

  it("closes from the footer button", () => {
    const { onClose, dialog } = renderDialog({ sampleRun });

    fireEvent.click(dialog.getByRole("button", { name: c.dialog.close }));

    expect(onClose).toHaveBeenCalledTimes(1);
  });

  it("shows a progress issue above the content", () => {
    const { dialog } = renderDialog({
      sampleRun,
      progressIssue: "slow update",
    });

    expect(dialog.getByRole("status")).toHaveTextContent("slow update");
  });
});

describe("RunResultsDialog in submit mode", () => {
  it("says it is judging while the result is pending", () => {
    const { dialog } = renderDialog({ mode: "submit", running: true });

    expect(dialog.getByText(c.dialog.titleSubmit)).toBeInTheDocument();
    expect(dialog.getByText(c.judging)).toBeInTheDocument();
  });

  it("lists one row per testcase, a hidden-data note and the follow-up links when accepted", () => {
    const { dialog } = renderDialog({ mode: "submit", result: baseResult });

    expect(dialog.getByText("Testcase 1")).toBeInTheDocument();
    expect(dialog.getByText("Testcase 2")).toBeInTheDocument();
    expect(dialog.getByText(c.dialog.hiddenNote)).toBeInTheDocument();
    expect(dialog.getByText("Accepted · 2/2")).toBeInTheDocument();
    expect(
      dialog.getByText("Thời gian 12 ms · Bộ nhớ 41 MB"),
    ).toBeInTheDocument();
    expect(dialog.getAllByRole("link", { name: c.linkResult })).toHaveLength(2);
    expect(dialog.getByRole("link", { name: c.linkReview })).toHaveAttribute(
      "href",
      "/submissions/sub-9/review",
    );
    expect(dialog.getByRole("link", { name: c.linkInterview })).toHaveAttribute(
      "href",
      "/submissions/sub-9/interview",
    );
  });

  it("marks failed testcases and hides the next-step links when rejected", () => {
    const { dialog } = renderDialog({
      mode: "submit",
      result: {
        ...baseResult,
        overallVerdict: "wrongAnswer",
        passedCount: 1,
        testcases: [
          { order: 1, verdict: "passed" },
          { order: 2, verdict: "failed" },
        ],
      },
    });

    expect(dialog.getByText("Sai kết quả · 1/2")).toBeInTheDocument();
    expect(dialog.getByText(c.failed)).toBeInTheDocument();
    expect(
      dialog.queryByRole("link", { name: c.linkReview }),
    ).not.toBeInTheDocument();
  });

  it("shows the compiler message of a compile error", () => {
    const { dialog } = renderDialog({
      mode: "submit",
      result: {
        ...baseResult,
        overallVerdict: "compileError",
        compileErrorMessage: "Main.java:3: error",
        testcases: [],
      },
    });

    expect(dialog.getByText("Main.java:3: error")).toBeInTheDocument();
  });
});
