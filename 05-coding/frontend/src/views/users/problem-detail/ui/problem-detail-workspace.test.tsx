// Mock-mode behaviour of the workspace that the basic view test does not reach: the side-panel tabs,
// language and model preferences, file upload rules, Run Code and Submit outcomes, bookmark toasts.
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";
import viMessages from "../../../../../messages/vi.json";

const nav = vi.hoisted(() => ({
  problemId: "1" as string | undefined,
  push: vi.fn(),
}));
vi.mock("@/shared/ui/data/code-editor", () => ({
  CodeEditor: ({
    value,
    onChange,
    label,
  }: {
    value: string;
    onChange: (value: string) => void;
    label: string;
  }) => (
    <textarea
      aria-label={label}
      value={value}
      onChange={(event) => onChange(event.target.value)}
    />
  ),
}));
vi.mock("next/navigation", () => ({
  useParams: () => ({ problemId: nav.problemId }),
  useRouter: () => ({ push: nav.push }),
}));

import { ProblemDetailView } from "./problem-detail-view";

const d = viMessages.problemDetail;

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

async function renderView(problemId: string | null = "1") {
  nav.problemId = problemId ?? undefined;
  const view = render(withTestProviders(<ProblemDetailView />));
  if (problemId) {
    await screen.findByLabelText(d.toolbar.codeAreaLabel);
    // The editor only unlocks once languages and starter code have arrived.
    await waitFor(() =>
      expect(
        screen.getByRole("button", { name: d.taskbar.btnRun }),
      ).not.toBeDisabled(),
    );
  }
  return view;
}

const type = (value: string) =>
  fireEvent.change(screen.getByLabelText(d.toolbar.codeAreaLabel), {
    target: { value },
  });

beforeEach(() => {
  nav.problemId = "1";
  nav.push.mockReset();
  window.localStorage.clear();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("ProblemDetailView side panel", () => {
  it("lists the viewer's earlier submissions with verdict, metrics and language", async () => {
    await renderView("1");

    fireEvent.click(
      screen.getByRole("button", { name: d.panel.tabSubmissions }),
    );

    expect(screen.getByText(d.verdict.accepted)).toBeInTheDocument();
    expect(
      screen.getByText(`${d.verdict.wrongAnswer} 2/12`),
    ).toBeInTheDocument();
    // Accepted run has metrics; the rejected one stored none, so a dash stands in.
    expect(screen.getByText("4 ms · 38 MB")).toBeInTheDocument();
    expect(screen.getByText("-")).toBeInTheDocument();
  });

  it("says so when the viewer has no submission for the problem", async () => {
    await renderView("121");

    fireEvent.click(
      screen.getByRole("button", { name: d.panel.tabSubmissions }),
    );

    expect(screen.getByText(d.panel.noSubmissions)).toBeInTheDocument();
  });

  it("shows the complexity card and report links on the review tab", async () => {
    await renderView("1");

    fireEvent.click(screen.getByRole("button", { name: d.panel.tabReview }));

    expect(screen.getAllByText("O(n)")).toHaveLength(2);
    expect(
      screen.getByRole("link", { name: d.panel.linkFullReport }),
    ).toHaveAttribute("href", "/submissions/s1/review");
    expect(
      screen.getByRole("link", { name: d.panel.linkMockInterview }),
    ).toHaveAttribute("href", "/submissions/s1/interview");
  });

  it("shows an empty review state when no report exists", async () => {
    await renderView("121");

    fireEvent.click(screen.getByRole("button", { name: d.panel.tabReview }));

    expect(screen.getByText(d.panel.noReview)).toBeInTheDocument();
  });

  it("lists the statement examples, with the explanation only where one exists", async () => {
    await renderView("1");

    expect(
      screen.getByText("nums = [2,7,11,15], target = 9"),
    ).toBeInTheDocument();
    expect(
      screen.getByText("nums[0] + nums[1] = 2 + 7 = 9"),
    ).toBeInTheDocument();
    expect(screen.getAllByText(d.panel.input)).toHaveLength(3);
    expect(screen.getByText(`${d.panel.acRate} 74%`)).toBeInTheDocument();
  });
});

describe("ProblemDetailView bookmark in mock mode", () => {
  it("unsaves, toasts, and offers a way back to the saved list after saving again", async () => {
    await renderView("1");
    const toasts = watch();

    fireEvent.click(screen.getByRole("button", { name: d.panel.btnUnsave }));
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["success", d.toast.unsaved]),
    );
    fireEvent.click(
      await screen.findByRole("button", { name: d.panel.btnSave }),
    );
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["success", d.toast.saved]),
    );
  });
});

describe("ProblemDetailView preferences", () => {
  it("remembers the last language and submission model and restores them", async () => {
    await renderView("1");

    fireEvent.click(screen.getByRole("button", { name: "Python" }));
    fireEvent.click(screen.getByRole("button", { name: d.mode.stdio }));
    expect(
      JSON.parse(window.localStorage.getItem("algoprep-workspace-prefs")!),
    ).toEqual({ language: "python", mode: "stdio" });
  });

  it("starts from stored preferences and ignores corrupt ones", async () => {
    window.localStorage.setItem(
      "algoprep-workspace-prefs",
      JSON.stringify({ language: "cpp", mode: "stdio" }),
    );
    const { unmount } = await renderView("1");
    await waitFor(() =>
      expect(screen.getByText("Time 1s · Memory 256MB")).toBeInTheDocument(),
    );
    unmount();

    window.localStorage.setItem("algoprep-workspace-prefs", "{not json");
    await renderView("1");
    expect(screen.getByText("Time 2s · Memory 512MB")).toBeInTheDocument();
  });

  it("restores a stored draft and shows the stdio-only badge for stdio problems", async () => {
    window.localStorage.setItem(
      "algoprep-draft:1:java:function",
      "// my draft",
    );
    await renderView("1");

    await waitFor(() =>
      expect(screen.getByLabelText(d.toolbar.codeAreaLabel)).toHaveValue(
        "// my draft",
      ),
    );
    expect(screen.getByText(d.toolbar.draftSaved)).toBeInTheDocument();
  });
});

describe("ProblemDetailView file upload", () => {
  async function openUpload() {
    await renderView("1");
    fireEvent.click(
      screen.getByRole("button", {
        name: viMessages.problemDetail.inputMethod.upload,
      }),
    );
    return document.querySelector("input[type='file']") as HTMLInputElement;
  }

  it("rejects a file whose extension does not match the language", async () => {
    const toasts = watch();
    const input = await openUpload();

    fireEvent.change(input, {
      target: { files: [new File(["x"], "main.py", { type: "text/plain" })] },
    });

    expect(toasts().at(-1)).toEqual([
      "error",
      d.toast.fileExtension.replace("{ext}", ".java"),
    ]);
    expect(screen.queryByText(d.toolbar.btnViewFile)).not.toBeInTheDocument();
  });

  it("rejects a file over 64 KB", async () => {
    const toasts = watch();
    const input = await openUpload();

    fireEvent.change(input, {
      target: {
        files: [
          new File(["x".repeat(65 * 1024)], "Big.java", { type: "text/plain" }),
        ],
      },
    });

    expect(toasts().at(-1)).toEqual(["error", d.toast.fileTooLarge]);
  });

  it("shows an accepted file, lets the user peek at it, and drops it when the language changes", async () => {
    const input = await openUpload();

    fireEvent.change(input, {
      target: {
        files: [new File(["class A {}"], "A.java", { type: "text/plain" })],
      },
    });
    expect(await screen.findByText(/A\.java/)).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: d.toolbar.btnViewFile }),
    );
    expect(screen.getByText("class A {}")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Python" }));
    expect(screen.queryByText(/A\.java/)).not.toBeInTheDocument();
  });

  it("accepts a dropped file too", async () => {
    await openUpload();
    const zone = screen.getByText(/Kéo thả/).closest("label")!;

    fireEvent.drop(zone, {
      dataTransfer: {
        files: [new File(["class B {}"], "B.java", { type: "text/plain" })],
      },
    });

    expect(await screen.findByText(/B\.java/)).toBeInTheDocument();
  });

  it("warns instead of running when no file was chosen", async () => {
    const toasts = watch();
    await openUpload();

    fireEvent.click(screen.getByRole("button", { name: d.taskbar.btnRun }));

    expect(toasts().at(-1)).toEqual(["warning", d.toast.noFile]);
  });

  it("submits the uploaded file's content", async () => {
    const input = await openUpload();
    fireEvent.change(input, {
      target: {
        files: [new File(["class C {}"], "C.java", { type: "text/plain" })],
      },
    });
    await screen.findByText(/C\.java/);

    fireEvent.click(screen.getByRole("button", { name: d.taskbar.btnSubmit }));

    expect(await screen.findByRole("dialog", undefined)).toBeInTheDocument();
  });
});

describe("ProblemDetailView run and submit", () => {
  it("rejects code over 64 KB before sending anything", async () => {
    const toasts = watch();
    await renderView("1");

    type("x".repeat(65 * 1024));
    fireEvent.click(screen.getByRole("button", { name: d.taskbar.btnSubmit }));

    expect(toasts().at(-1)).toEqual(["error", d.toast.codeTooLarge]);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });

  it("Run Code toasts the failed ratio and summarises the samples in the console", async () => {
    const toasts = watch();
    await renderView("1");

    fireEvent.click(screen.getByRole("button", { name: d.taskbar.btnRun }));
    await screen.findByRole("dialog");
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: d.console.dialog.close,
      }),
    );

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "warning",
        "Chạy thử chưa đạt: 2/3 testcase mẫu đúng.",
      ]),
    );
    expect(screen.getByText("2 đạt")).toBeInTheDocument();
    expect(screen.getByText("1 không đạt")).toBeInTheDocument();
    // The detail button reopens the sub-screen.
    fireEvent.click(screen.getByRole("button", { name: d.console.btnDetail }));
    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });

  it("Run Code on an always-passing problem toasts success", async () => {
    const toasts = watch();
    await renderView("121");

    fireEvent.click(screen.getByRole("button", { name: d.taskbar.btnRun }));

    await waitFor(
      () =>
        expect(toasts().at(-1)).toEqual([
          "success",
          "Đã chạy xong 10 testcase mẫu.",
        ]),
      { timeout: 4000 },
    );
  });

  it("a rejected Submit toasts the ratio and shows the verdict without next-step links", async () => {
    const toasts = watch();
    await renderView("1");

    fireEvent.click(screen.getByRole("button", { name: d.taskbar.btnSubmit }));
    await screen.findByRole("dialog");
    await waitFor(
      () =>
        expect(toasts().at(-1)).toEqual([
          "error",
          "Bài nộp chưa đạt: 4/12 testcase đúng.",
        ]),
      { timeout: 4000 },
    );
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: d.console.dialog.close,
      }),
    );

    expect(screen.getByText(/Sai kết quả · 4\/12/)).toBeInTheDocument();
    expect(
      screen.queryByRole("link", { name: d.console.linkResult }),
    ).not.toBeInTheDocument();
  });

  it("an accepted Submit toasts success and offers the result, review and interview links", async () => {
    const toasts = watch();
    await renderView("121");

    fireEvent.click(screen.getByRole("button", { name: d.taskbar.btnSubmit }));
    await waitFor(
      () =>
        expect(toasts().at(-1)).toEqual(["success", d.toast.submitAccepted]),
      { timeout: 4000 },
    );
    fireEvent.click(
      within(screen.getByRole("dialog")).getByRole("button", {
        name: d.console.dialog.close,
      }),
    );

    expect(
      screen.getByRole("link", { name: d.console.linkResult }),
    ).toHaveAttribute("href", "/submissions/sub-2");
    expect(
      screen.getByRole("link", { name: d.console.linkReview }),
    ).toHaveAttribute("href", "/submissions/sub-2/review");
    expect(
      screen.getByRole("link", { name: d.console.linkInterview }),
    ).toHaveAttribute("href", "/submissions/sub-2/interview");
    expect(
      screen.getByText("Thời gian 12 ms · Bộ nhớ 41 MB"),
    ).toBeInTheDocument();
  });

  it("switches between the Testcase and Result console tabs", async () => {
    await renderView("1");

    expect(screen.getByText(/Còn 9 testcase ẩn/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: d.console.tabResult }));
    expect(screen.getByText(d.console.submitHint)).toBeInTheDocument();
  });
});

describe("ProblemDetailView without a problem id", () => {
  it("shows the not-found state with a link back to the list", async () => {
    await renderView(null);

    expect(screen.getByText(d.empty.title)).toBeInTheDocument();
    expect(
      screen.getByRole("link", { name: d.empty.linkList }),
    ).toHaveAttribute("href", "/problems");
  });
});
