// Branches of the submission result screen that the basic view test leaves out: copy-code outcomes,
// testcase detail panes, the learner action buttons per verdict, and the instructor slots.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
} from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../../messages/vi.json";

const nav = vi.hoisted(() => ({ id: "SB-90412" }));
vi.mock("next/navigation", () => ({
  useParams: () => ({ submissionId: nav.id }),
}));

// `override` lets a test replace the loaded submission; null keeps the real mock-backed hook.
const override = vi.hoisted(() => ({ value: null as unknown }));
vi.mock("@/entities/submission", async (importOriginal) => {
  const actual = await importOriginal<Record<string, unknown>>();
  return {
    ...actual,
    useSubmissionDetail: (...args: unknown[]) => {
      const real = (
        actual.useSubmissionDetail as (...a: unknown[]) => {
          data?: Record<string, unknown> | null;
        }
      )(...args);
      if (override.value === null) return real;
      const patch = override.value as Record<string, unknown>;
      return real.data ? { ...real, data: { ...real.data, ...patch } } : real;
    },
  };
});

import { SubmissionResultView } from "./submission-result-view";

const t = viMessages.submissionResult;

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

async function renderView(
  area?: "instructor",
  extra?: Record<string, unknown>,
) {
  const view = render(
    withTestProviders(<SubmissionResultView area={area} {...extra} />),
  );
  await waitFor(() =>
    expect(
      screen.queryByRole("status", { name: "Đang tải..." }),
    ).not.toBeInTheDocument(),
  );
  return view;
}

beforeEach(() => {
  nav.id = "SB-90412";
  override.value = null;
});

afterEach(() => {
  vi.unstubAllGlobals();
  Object.defineProperty(navigator, "clipboard", {
    value: undefined,
    configurable: true,
  });
});

describe("SubmissionResultView copy code", () => {
  it("copies the source and says so", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText },
      configurable: true,
    });
    const toasts = watch();
    await renderView();

    fireEvent.click(screen.getByRole("button", { name: t.code.copy }));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["success", t.code.copied]),
    );
    expect(writeText).toHaveBeenCalledWith(
      expect.stringContaining("def two_sum"),
    );
  });

  it("reports a failure when the clipboard rejects", async () => {
    Object.defineProperty(navigator, "clipboard", {
      value: { writeText: vi.fn().mockRejectedValue(new Error("denied")) },
      configurable: true,
    });
    const toasts = watch();
    await renderView();

    fireEvent.click(screen.getByRole("button", { name: t.code.copy }));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", t.code.copyFailed]),
    );
  });

  it("reports a failure when the browser has no clipboard", async () => {
    const toasts = watch();
    await renderView();

    fireEvent.click(screen.getByRole("button", { name: t.code.copy }));

    expect(toasts().at(-1)).toEqual(["error", t.code.copyFailed]);
  });
});

describe("SubmissionResultView testcase table", () => {
  it("opens a sample testcase to show input, expected and actual output, and closes it again", async () => {
    await renderView();

    const [first] = screen.getAllByRole("button", { name: t.col.expand });
    fireEvent.click(first!);
    expect(screen.getByText(t.sample.input)).toBeInTheDocument();
    expect(screen.getByText(t.sample.expected)).toBeInTheDocument();
    expect(screen.getByText(t.sample.actual)).toBeInTheDocument();
    expect(
      screen.getByText("nums = [2,7,11,15], target = 9"),
    ).toBeInTheDocument();

    fireEvent.click(first!);
    expect(screen.queryByText(t.sample.input)).not.toBeInTheDocument();
  });

  it("only one detail row is open at a time, and hidden testcases cannot expand", async () => {
    await renderView();

    const buttons = screen.getAllByRole("button", { name: t.col.expand });
    fireEvent.click(buttons[0]!);
    fireEvent.click(buttons[1]!);
    expect(screen.getAllByText(t.sample.input)).toHaveLength(1);
    // Sample rows carry aria-expanded, hidden rows are disabled and carry none.
    expect(buttons[3]).toBeDisabled();
    expect(buttons[3]).not.toHaveAttribute("aria-expanded");
  });

  it("shows partial score and failing verdicts for a wrong answer", async () => {
    nav.id = "SB-90408";
    await renderView();

    expect(screen.getByText(t.verdict.WRONG_ANSWER)).toBeInTheDocument();
    expect(screen.getAllByText("WA").length).toBeGreaterThan(0);
  });
});

describe("SubmissionResultView learner actions", () => {
  it("links to review, interview and the workspace once accepted", async () => {
    await renderView();

    expect(
      screen.getByRole("link", { name: t.actions.solutionReview }),
    ).toHaveAttribute("href", "/submissions/SB-90412/review");
    expect(
      screen.getByRole("link", { name: t.actions.mockInterview }),
    ).toHaveAttribute("href", "/submissions/SB-90412/interview");
    expect(
      screen.getByRole("link", { name: t.actions.retry }).getAttribute("href"),
    ).toMatch(/^\/workspace\//);
  });

  it("locks the AI actions with an explanation while the submission is not accepted", async () => {
    nav.id = "SB-90408";
    await renderView();

    const review = screen
      .getByText(t.actions.solutionReview)
      .closest("button")!;
    expect(review).toBeDisabled();
    expect(review).toHaveAttribute("title", t.actions.needAccepted);
    expect(
      screen.queryByRole("link", { name: t.actions.mockInterview }),
    ).not.toBeInTheDocument();
  });

  it("shows no beats percentage for a submission that was not accepted", async () => {
    nav.id = "SB-90408";
    await renderView();

    expect(screen.getByText(/Beats -/)).toBeInTheDocument();
  });

  it("says there is not enough data when too few accepted runs back the percentage", async () => {
    override.value = { beatsSampleSize: 3 };
    await renderView();

    expect(
      screen.getByText(new RegExp(t.stats.beatsNoData)),
    ).toBeInTheDocument();
  });

  it("disables the links to a withdrawn problem", async () => {
    override.value = { problemWithdrawn: true };
    await renderView();

    const breadcrumb = screen
      .getAllByRole("link")
      .find(
        (a) =>
          a.getAttribute("href") === "#" && a.textContent !== t.actions.retry,
      )!;
    expect(breadcrumb).toHaveAttribute("aria-disabled", "true");
    const retry = screen.getByRole("link", { name: t.actions.retry });
    expect(retry).toHaveAttribute("href", "#");
    fireEvent.click(retry);
    fireEvent.click(breadcrumb);
    expect(retry).toHaveAttribute("aria-disabled", "true");
  });

  it("warns when testcase details are no longer available", async () => {
    override.value = { testcaseDetailsAvailable: false };
    await renderView();

    expect(screen.getByText(t.testcaseUnavailable)).toBeInTheDocument();
  });

  it("shows the empty testcase message when nothing ran", async () => {
    override.value = { testcases: [] };
    await renderView();

    expect(screen.getByText(t.testcaseEmpty)).toBeInTheDocument();
  });

  it("follows a judging submission without breaking the page", async () => {
    nav.id = "SB-90420";
    await renderView();

    expect(screen.getByText(t.verdict.JUDGING)).toBeInTheDocument();
  });
});

describe("SubmissionResultView instructor slots", () => {
  it("renders the caller's breadcrumb and context card in place of the defaults", async () => {
    await renderView("instructor", {
      renderBreadcrumb: (s: { id: string }) => <span>crumb {s.id}</span>,
      renderContext: (s: { id: string }) => <p>context {s.id}</p>,
    });

    expect(screen.getByText("crumb SB-90412")).toBeInTheDocument();
    expect(screen.getByText("context SB-90412")).toBeInTheDocument();
    expect(
      screen.queryByText(new RegExp(t.instructorReadOnly)),
    ).not.toBeInTheDocument();
    expect(screen.queryByText(t.breadcrumbSubmissions)).not.toBeInTheDocument();
  });

  it("omits beats and the learner actions, and still lets the instructor copy the code", async () => {
    await renderView("instructor");

    expect(screen.queryByText(/Beats/)).not.toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: t.actions.retry }),
    ).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: t.code.copy }),
    ).toBeInTheDocument();
    expect(screen.getByText(/Runtime 42 ms/)).toBeInTheDocument();
  });

  it("opens a sample testcase in the instructor layout too", async () => {
    await renderView("instructor");

    fireEvent.click(screen.getAllByRole("button", { name: t.col.expand })[0]!);

    expect(screen.getByText(t.sample.actual)).toBeInTheDocument();
  });

  it("shows the compile log and no testcase table for a compile error", async () => {
    nav.id = "SB-90403";
    await renderView("instructor");

    expect(screen.queryByText(t.testcaseTableCaption)).not.toBeInTheDocument();
    expect(
      screen.getByText(/expected ';' after return statement/),
    ).toBeInTheDocument();
  });
});
