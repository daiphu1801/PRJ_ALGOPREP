// Smoke test (PROTOTYPE lane): testcase add/delete invalidates the reference-solution run, and
// re-running restores it (F2-18).
import { beforeEach, describe, expect, it, vi } from "vitest";
import { act, fireEvent, render, renderHook, screen, waitFor, within } from "@testing-library/react";
import Link from "next/link";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import type { ReactElement } from "react";
import { ProblemAuthoringView } from "./problem-authoring-view";

vi.mock("next/navigation", () => ({ useRouter: () => ({ push: vi.fn() }) }));

vi.mock("@/shared/i18n", () => ({
  useT: () => (key: string, params?: Record<string, unknown>) =>
    params ? `${key} ${JSON.stringify(params)}` : key,
}));

beforeEach(() => {
  act(() => toast.clear());
  window.localStorage.clear();
});

// The view loads its problem through TanStack Query, so every render waits for the form to appear.
async function renderLoaded(ui: ReactElement) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } });
  render(<QueryClientProvider client={client}>{ui}</QueryClientProvider>);
  await screen.findByLabelText("titleLabel");
}

describe("ProblemAuthoringView testcase tab", () => {
  it("adds a testcase, marks the run stale, and clears the stale state after re-running", async () => {
    await renderLoaded(<ProblemAuthoringView basePath="/admin/problems" />);
    fireEvent.click(screen.getByRole("button", { name: /tab\.testcases/ }));

    const toasts = renderHook(() => useToasts());

    fireEvent.click(screen.getByRole("button", { name: "addTestcase" }));
    const dialog = screen.getByRole("dialog");
    // Empty form is rejected.
    fireEvent.click(within(dialog).getByRole("button", { name: "dialog.save" }));
    expect(toasts.result.current.map((item) => [item.tone, item.message])).toEqual([
      ["error", "dialog.required"],
    ]);

    const fields = within(dialog).getAllByRole("textbox");
    fireEvent.change(fields[0]!, { target: { value: 's = "xyz", t = "y"' } });
    fireEvent.change(fields[1]!, { target: { value: '"y"' } });
    fireEvent.click(within(dialog).getByRole("button", { name: "dialog.save" }));

    expect(screen.getByText('s = "xyz", t = "y"')).toBeInTheDocument();
    // No inline notice any more: a stale run shows as the checklist item going back to pending.
    const solutionItem = () => screen.getByText("check.solutionPasses").closest("li");
    expect(solutionItem()).toHaveTextContent("checkPending");
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success", message: "toast.testcaseAdded" });

    fireEvent.click(screen.getByRole("button", { name: "runSolution" }));
    expect(solutionItem()).toHaveTextContent("checkDone");
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success" });
    expect(toasts.result.current.at(-1)?.message).toMatch(/"passed":6,"total":6/);
  });
});

describe("ProblemAuthoringView save and publish", () => {
  it("enables Save only when dirty, then toasts saved", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemAuthoringView basePath="/admin/problems" />);
    const save = screen.getByRole("button", { name: "save" });
    expect(save).toBeDisabled();

    fireEvent.change(screen.getByLabelText("titleLabel"), { target: { value: "New title" } });
    expect(save).toBeEnabled();

    fireEvent.click(save);
    await waitFor(() =>
      expect(toasts.result.current.at(-1)).toMatchObject({ tone: "success", message: "toast.saved" }),
    );
    expect(save).toBeDisabled();
  });

  it("blocks Publish while the checklist has unmet items and a click names the missing condition", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemAuthoringView basePath="/admin/problems" />);
    const publish = screen.getByRole("button", { name: "publish" });
    // The mock draft has only 5 approved testcases, below the 8 the checklist requires.
    expect(publish).toHaveAttribute("aria-disabled", "true");

    fireEvent.click(publish);
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "warning",
      message: "checklistBlockTitle: check.minTestcases",
    });
  });

  it("refuses to delete an example from a published problem that has only 2, and says why", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemAuthoringView basePath="/admin/problems" />);
    fireEvent.click(screen.getByRole("button", { name: /tab\.examples/ }));

    // The mock draft is published with exactly 2 examples (BD Sheet 6 area D item 6).
    fireEvent.click(screen.getAllByRole("button", { name: "delete" })[0]!);
    expect(toasts.result.current.at(-1)).toMatchObject({ tone: "warning", message: "examplesFloorWarning" });
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("explains why the AI generate button is locked when clicked", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemAuthoringView basePath="/admin/problems" />);

    // Editing the reference solution makes the last run stale, which locks generation (F2-14).
    fireEvent.change(screen.getByLabelText(/^solutionLabel/), { target: { value: "def f(): pass" } });
    fireEvent.click(screen.getByRole("button", { name: /tab\.ai/ }));

    const generate = screen.getByRole("button", { name: "generateAction" });
    expect(generate).toHaveAttribute("aria-disabled", "true");
    fireEvent.click(generate);
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "warning",
      message: 'generateLockedBody {"samples":2}',
    });
  });
});

describe("ProblemAuthoringView preview, unsaved guard and checklist (BD Q6, Q7, Q8, Q12-Q14)", () => {
  it("opens the preview in a new tab for an existing problem", async () => {
    await renderLoaded(<ProblemAuthoringView basePath="/admin/problems" problemId="121" />);
    const preview = screen.getByRole("link", { name: "previewAsLearner" });
    expect(preview).toHaveAttribute("href", "/admin/problems/121/preview");
    expect(preview).toHaveAttribute("target", "_blank");
  });

  it("has no preview link for a problem that was never saved", async () => {
    await renderLoaded(<ProblemAuthoringView basePath="/admin/problems" />);
    expect(screen.queryByRole("link", { name: "previewAsLearner" })).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: "previewNeedsSave" })).toHaveAttribute("aria-disabled", "true");
  });

  it("asks before leaving through a link while changes are unsaved", async () => {
    await renderLoaded(
      <>
        <Link href="/admin/problems/other">sidebar</Link>
        <ProblemAuthoringView basePath="/admin/problems" problemId="121" />
      </>,
    );
    fireEvent.change(screen.getByLabelText("titleLabel"), { target: { value: "Edited" } });

    fireEvent.click(screen.getByText("sidebar"));
    expect(screen.getByRole("dialog")).toHaveTextContent("unsaved.title");
    fireEvent.click(screen.getByRole("button", { name: "unsaved.stay" }));
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("names every missing publish condition in the toast", async () => {
    const toasts = renderHook(() => useToasts());
    await renderLoaded(<ProblemAuthoringView basePath="/admin/problems" problemId="121" />);
    // Editing the reference solution makes the last run stale, so a second condition goes unmet.
    fireEvent.change(screen.getByLabelText(/^solutionLabel/), { target: { value: "def f(): pass" } });
    fireEvent.click(screen.getByRole("button", { name: "publish" }));
    const message = toasts.result.current.at(-1)?.message ?? "";
    expect(message).toContain("check.minTestcases");
    expect(message).toContain("check.solutionPasses");
  });

  it("jumps to the right tab from an unmet checklist item", async () => {
    await renderLoaded(<ProblemAuthoringView basePath="/admin/problems" problemId="121" />);
    fireEvent.click(screen.getByRole("button", { name: "check.minTestcases" }));
    expect(screen.getByRole("button", { name: "addTestcase" })).toBeInTheDocument();
  });
});

describe("ProblemAuthoringView AI testcase generation (F2-14, BD EVT-16)", () => {
  async function openAiTab() {
    await renderLoaded(<ProblemAuthoringView basePath="/admin/problems" problemId="121" />);
    fireEvent.click(screen.getByRole("button", { name: /tab\.ai/ }));
  }

  const messages = (toasts: ReturnType<typeof renderHook<ReturnType<typeof useToasts>, unknown>>) =>
    toasts.result.current.map((item) => [item.tone, item.message]);

  it("adds unapproved drafts, shows one generic running state, and reports what was dropped", async () => {
    const toasts = renderHook(() => useToasts());
    await openAiTab();
    expect(screen.getByText('generateQuota {"left":2,"limit":2}')).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "generateAction" }));
    expect(await screen.findByText("generatingBody")).toBeInTheDocument();

    await waitFor(() => expect(messages(toasts)).toContainEqual(["success", 'toast.generated {"count":4}']), {
      timeout: 6000,
    });
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "warning",
      message: 'toast.droppedInvalidConstraint {"count":1}',
    });
    // 4 drafts were already waiting; the 4 new ones join them, still unapproved.
    expect(screen.getByText('draftsSubtitle {"count":8}')).toBeInTheDocument();
    expect(screen.queryByText("generatingBody")).not.toBeInTheDocument();
    // The attempt is counted once the server record is refetched.
    expect(await screen.findByText('generateQuota {"left":1,"limit":2}')).toBeInTheDocument();
  }, 10000);

  it("allows two generations per problem, a second script gives other cases, a third is refused", async () => {
    const toasts = renderHook(() => useToasts());
    await openAiTab();
    const generate = () => fireEvent.click(screen.getByRole("button", { name: "generateAction" }));

    generate();
    await screen.findByText('generateQuota {"left":1,"limit":2}', undefined, { timeout: 6000 });
    generate();
    await screen.findByText('generateQuota {"left":0,"limit":2}', undefined, { timeout: 6000 });
    // 4 waiting + 4 from the first script + 4 from the second, different one.
    expect(screen.getByText('draftsSubtitle {"count":12}')).toBeInTheDocument();

    generate();
    expect(toasts.result.current.at(-1)).toMatchObject({
      tone: "warning",
      message: 'toast.generateLimitReached {"limit":2}',
    });
    expect(screen.getByText('draftsSubtitle {"count":12}')).toBeInTheDocument();
  }, 20000);

  it("raises an error toast, adds nothing and counts no attempt when the script is rejected", async () => {
    window.history.pushState({}, "", "/?aiMock=error");
    try {
      const toasts = renderHook(() => useToasts());
      await openAiTab();

      fireEvent.click(screen.getByRole("button", { name: "generateAction" }));
      await waitFor(
        () => expect(toasts.result.current.at(-1)).toMatchObject({ tone: "error", message: "toast.generateFailed" }),
        { timeout: 6000 },
      );
      expect(screen.getByText('draftsSubtitle {"count":4}')).toBeInTheDocument();
      expect(screen.getByText('generateQuota {"left":2,"limit":2}')).toBeInTheDocument();
    } finally {
      window.history.pushState({}, "", "/");
    }
  }, 10000);
});
