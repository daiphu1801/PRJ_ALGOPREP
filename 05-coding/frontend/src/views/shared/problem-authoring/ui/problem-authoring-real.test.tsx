// SHR0202 against the REAL api path (F2 #33 to #47): only the HTTP layer is faked, so the real modules,
// the error mapping and the real vi texts are what the assertions read. Mock-data behaviour stays in
// problem-authoring-view.test.tsx.
import {
  act,
  fireEvent,
  render,
  renderHook,
  screen,
  waitFor,
  within,
} from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
import viMessages from "../../../../../messages/vi.json";
import { ApiError } from "@/shared/api";
import { toast, useToasts } from "@/shared/lib/toast-store";
import { withTestProviders } from "@/shared/test/render-with-providers";

vi.mock("@/shared/api", async (importOriginal) =>
  (await import("@/shared/test/real-api")).realApiModule(
    await importOriginal<Record<string, unknown>>(),
  ),
);
// `useMockQuery` reads the flag from `mock-mode` directly, not through the barrel mocked above.
vi.mock("@/shared/api/mock-mode", () => ({
  isMockMode: () => false,
  withMockData: <T,>(_mock: () => unknown, real: () => Promise<T>) => real(),
}));
const { replace } = vi.hoisted(() => ({ replace: vi.fn() }));
vi.mock("next/navigation", () => ({
  useRouter: () => ({ push: vi.fn(), replace }),
}));

import { apiMock, resetApiMock } from "@/shared/test/real-api";
import { ProblemAuthoringView } from "./problem-authoring-view";

const m = viMessages.problemAuthoring;
const apiErrors = viMessages.apiErrors as Record<string, string>;
const A = "/problem-bank/authoring/problems";
const P = "11111111-1111-4111-8111-111111111111";
const LEVEL = "22222222-2222-4222-8222-222222222222";
const TOPIC = "44444444-4444-4444-8444-444444444444";
const T0 = "2026-11-06T01:00:00Z";
const T1 = "2026-11-06T02:00:00Z";

const tc = (n: number, over: Record<string, unknown> = {}) => ({
  id: `tc-${n}`,
  position: n,
  visibility: n <= 2 ? "SAMPLE" : "HIDDEN",
  origin: "MANUAL",
  approved: true,
  category: null,
  input: `in-${n}`,
  inputStorage: "INLINE",
  inputSizeBytes: 4,
  inputDownloadUrl: null,
  expectedOutput: `out-${n}`,
  expectedOutputStorage: "INLINE",
  expectedOutputSizeBytes: 5,
  expectedOutputDownloadUrl: null,
  ...over,
});

function detail(over: Record<string, unknown> = {}) {
  const testcases = Array.from({ length: 8 }, (_, i) => tc(i + 1));
  return {
    id: P,
    code: "TWO-SUM",
    title: "Two Sum",
    statementMd: "Đề bài",
    constraintsMd: "",
    levelId: LEVEL,
    status: "UNPUBLISHED",
    limits: {
      timeLimitMs: 1000,
      memoryLimitMb: 256,
      maxOutputSizeKb: 64,
      maxSubmissionsPerHour: null,
    },
    topicIds: [TOPIC],
    tags: [],
    referenceSolution: { language: "PYTHON", sourceCode: "print(1)" },
    workedExamples: [
      { id: "e1", position: 1, input: "a", output: "b", explanationMd: null },
      { id: "e2", position: 2, input: "c", output: "d", explanationMd: null },
    ],
    testcases,
    testcaseSummary: { total: 8, approved: 8, sample: 2, hidden: 6 },
    spec: {
      matchingStrategy: "EXACT",
      epsilonValue: null,
      stdinFormatMd: "stdin",
      stdoutFormatMd: "stdout",
      functionName: "two_sum",
      returnType: { kind: "INT" },
      parameters: [{ name: "a", type: { kind: "INT" } }],
      nameOverrides: null,
    },
    functionWrapperSupported: true,
    aiContext: { briefText: null, noFullCode: false, questionOnly: false },
    aiGeneration: { used: 0, limit: 2 },
    updatedAt: T0,
    duplicatedFromProblemId: null,
    readOnly: false,
    ...over,
  };
}

const PREVIEW = {
  wrapperSupported: true,
  unsupportedReasons: [],
  declarations: { JAVA: "j", CPP: "c", PYTHON: "p" },
};
const isPreview = (path: unknown) =>
  String(path).includes("function-signature/preview");
/** Routes POSTs: the signature preview gets a valid answer, everything else goes to `other`. */
function postRoutes(other: (path: string, body?: unknown) => unknown) {
  apiMock.post.mockImplementation(async (path: string, body?: unknown) =>
    isPreview(path) ? PREVIEW : other(path, body),
  );
}
/** The POSTs a test cares about: the preview is not one of them. */
const postCalls = () =>
  apiMock.post.mock.calls.filter(([path]) => !isPreview(path));

function routes(dto: unknown = detail()) {
  apiMock.get.mockImplementation(async (path: string) => {
    if (path === "/problem-bank/authoring/defaults")
      return {
        timeLimitMs: 1000,
        memoryLimitMb: 256,
        maxOutputSizeKb: 64,
        maxAiGenerationsPerProblem: 2,
      };
    if (path === "/problem-bank/levels")
      return {
        items: [
          {
            id: LEVEL,
            code: "EASY",
            displayName: "Dễ",
            sortOrder: 1,
            problemCount: null,
          },
        ],
      };
    if (path === "/problem-bank/topics")
      return {
        items: [
          {
            id: TOPIC,
            code: "ARRAY",
            name: "Mảng",
            sortOrder: 1,
            problemCount: null,
          },
        ],
      };
    if (path === `${A}/${P}`) return dto;
    if (path === `${A}/${P}/stats`)
      return { submissionCount: 12, acRate: null, updatedAt: null };
    throw new Error(`unexpected GET ${path}`);
  });
}

function watch() {
  const toasts = renderHook(() => useToasts());
  act(() => toast.clear());
  return () => toasts.result.current.map((item) => [item.tone, item.message]);
}

async function open() {
  render(
    withTestProviders(
      <ProblemAuthoringView basePath="/admin/problems" problemId={P} />,
    ),
  );
  await screen.findByLabelText(m.titleLabel);
}

const goExamples = () =>
  fireEvent.click(screen.getByRole("button", { name: /Ví dụ mẫu \(\d+\)/ }));

const goTestcases = () =>
  fireEvent.click(screen.getByRole("button", { name: /Testcase \(\d+\)/ }));

describe("ProblemAuthoringView with the real API", () => {
  beforeEach(() => {
    resetApiMock();
    routes();
  });

  it("loads the server's problem and shows unknown statistics as a dash, never as 0%", async () => {
    await open();
    expect(screen.getByLabelText(m.titleLabel)).toHaveValue("Two Sum");
    expect(apiMock.get).toHaveBeenCalledWith(`${A}/${P}`);
    expect(apiMock.get).toHaveBeenCalledWith(`${A}/${P}/stats`);
    expect(screen.getByText("12")).toBeInTheDocument();
    const rate = screen.getByText(m.metrics.acRate).nextElementSibling;
    expect(rate).toHaveTextContent("-");
  });

  it("saves an edited title with the lock value, toasts, and is clean again", async () => {
    apiMock.put.mockResolvedValue(
      detail({ title: "Two Sum II", updatedAt: T1 }),
    );
    routes(detail());
    const toasts = watch();
    await open();
    const save = screen.getByRole("button", { name: m.save });
    expect(save).toBeDisabled();
    fireEvent.change(screen.getByLabelText(m.titleLabel), {
      target: { value: "Two Sum II" },
    });
    expect(save).toBeEnabled();
    routes(detail({ title: "Two Sum II", updatedAt: T1 }));
    fireEvent.click(save);

    await waitFor(() =>
      expect(apiMock.put).toHaveBeenCalledWith(
        `${A}/${P}/content`,
        expect.objectContaining({ title: "Two Sum II", expectedUpdatedAt: T0 }),
      ),
    );
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["success", m.toast.saved]),
    );
    expect(apiMock.put).toHaveBeenCalledTimes(1);
    await waitFor(() => expect(save).toBeDisabled());
  });

  it("the first save of a new problem continues at its own edit address", async () => {
    const created = detail({ id: "new-problem-id" });
    apiMock.post.mockResolvedValue(created);
    apiMock.put.mockResolvedValue(created);
    replace.mockClear();
    render(
      withTestProviders(<ProblemAuthoringView basePath="/admin/problems" />),
    );
    await screen.findByLabelText(m.titleLabel);
    fireEvent.change(screen.getByLabelText(m.titleLabel), {
      target: { value: "Brand new" },
    });
    fireEvent.change(
      screen.getByLabelText(m.bodyLabel, { selector: "textarea" }),
      {
        target: { value: "Statement" },
      },
    );
    fireEvent.click(screen.getByRole("button", { name: m.save }));

    await waitFor(() =>
      expect(replace).toHaveBeenCalledWith(
        "/admin/problems/new-problem-id/edit",
      ),
    );
  });

  it("saving an existing problem never changes the address", async () => {
    apiMock.put.mockResolvedValue(detail({ title: "Other", updatedAt: T1 }));
    replace.mockClear();
    await open();
    fireEvent.change(screen.getByLabelText(m.titleLabel), {
      target: { value: "Other" },
    });
    fireEvent.click(screen.getByRole("button", { name: m.save }));

    await waitFor(() => expect(apiMock.put).toHaveBeenCalled());
    expect(replace).not.toHaveBeenCalled();
  });

  it("a stale lock (PRB-020) shows its text, keeps the edit and keeps Save enabled", async () => {
    apiMock.put.mockRejectedValue(new ApiError("PRB-020", 409));
    const toasts = watch();
    await open();
    fireEvent.change(screen.getByLabelText(m.titleLabel), {
      target: { value: "Other" },
    });
    fireEvent.click(screen.getByRole("button", { name: m.save }));

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["PRB-020"]]),
    );
    expect(screen.getByLabelText(m.titleLabel)).toHaveValue("Other");
    expect(screen.getByRole("button", { name: m.save })).toBeEnabled();
  });

  it("publish refused with PRB-030 names every missing condition and puts the status switch back", async () => {
    apiMock.put.mockImplementation(async (path: string) => {
      if (path.endsWith("/status"))
        throw new ApiError("PRB-030", 422, undefined, {
          items: [
            { code: "MIN_APPROVED_TESTCASES", met: false },
            { code: "REFERENCE_SOLUTION_PASSES_ALL", met: false },
            { code: "MIN_WORKED_EXAMPLES", met: true },
          ],
        });
      return {};
    });
    const toasts = watch();
    await open();
    fireEvent.click(screen.getByRole("button", { name: m.publish }));

    await waitFor(() => expect(apiMock.put).toHaveBeenCalled());
    expect(apiMock.put).toHaveBeenCalledWith(`${A}/${P}/status`, {
      status: "PUBLISHED",
      expectedUpdatedAt: T0,
    });
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "warning",
        `${m.checklistBlockTitle}: ${m.check.minTestcases}; ${m.check.solutionPasses}`,
      ]),
    );
    expect(
      screen.getByRole("button", { name: m.status.draft }),
    ).toHaveAttribute("aria-pressed", "true");
  });

  it("publishes when the server accepts: status step after the content, success toast", async () => {
    apiMock.put.mockResolvedValue({ problemUpdatedAt: T1 });
    const toasts = watch();
    await open();
    routes(detail({ status: "PUBLISHED", updatedAt: T1 }));
    fireEvent.click(screen.getByRole("button", { name: m.publish }));
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["success", m.toast.published]),
    );
    expect(apiMock.put).toHaveBeenCalledTimes(1);
    expect(
      screen.getByRole("button", { name: m.status.published }),
    ).toHaveAttribute("aria-pressed", "true");
  });

  // A standard-I/O problem: formats written, no function signature (DD api F2 checklist SPEC_DECLARED).
  const stdioOnly = {
    spec: {
      matchingStrategy: "EXACT",
      epsilonValue: null,
      stdinFormatMd: "stdin",
      stdoutFormatMd: "stdout",
      functionName: null,
      returnType: null,
      parameters: null,
      nameOverrides: null,
    },
    functionWrapperSupported: false,
  };
  const pendingSpec = () =>
    screen.queryByRole("button", { name: m.check.specDeclared });

  it("a standard-I/O spec the server calls declared reads as met and publishes without a client block", async () => {
    apiMock.put.mockResolvedValue({ problemUpdatedAt: T1 });
    routes(detail({ ...stdioOnly, checklist: { specDeclared: true } }));
    const toasts = watch();
    await open();
    expect(pendingSpec()).not.toBeInTheDocument();
    expect(screen.getByText(m.check.specDeclared)).toBeInTheDocument();

    routes(
      detail({
        ...stdioOnly,
        status: "PUBLISHED",
        updatedAt: T1,
        checklist: { specDeclared: true },
      }),
    );
    fireEvent.click(screen.getByRole("button", { name: m.publish }));
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["success", m.toast.published]),
    );
    expect(apiMock.put).toHaveBeenCalledWith(
      `${A}/${P}/status`,
      expect.objectContaining({ status: "PUBLISHED" }),
    );
  });

  it("without a server verdict complete STDIO formats satisfy the local spec check", async () => {
    routes(detail(stdioOnly));
    await open();
    expect(pendingSpec()).not.toBeInTheDocument();
  });

  it("the server's 'not declared' wins over a spec the client would call complete", async () => {
    routes(detail({ checklist: { specDeclared: false } }));
    await open();
    expect(pendingSpec()).toBeInTheDocument();
  });

  it("adds a testcase straight to the server and shows the server's row; Save stays disabled", async () => {
    apiMock.post.mockResolvedValue({
      testcase: tc(9, { input: "x", expectedOutput: "y" }),
      problemUpdatedAt: T1,
    });
    const toasts = watch();
    await open();
    goTestcases();
    fireEvent.click(screen.getByRole("button", { name: m.addTestcase }));
    const dialog = screen.getByRole("dialog");
    const [input, expected] = within(dialog).getAllByRole("textbox");
    fireEvent.change(input!, { target: { value: "x" } });
    fireEvent.change(expected!, { target: { value: "y" } });
    fireEvent.click(
      within(dialog).getByRole("button", { name: m.dialog.save }),
    );

    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith(`${A}/${P}/testcases`, {
        visibility: "HIDDEN",
        input: "x",
        expectedOutput: "y",
      }),
    );
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["success", m.toast.testcaseAdded]),
    );
    expect(screen.getByRole("button", { name: m.save })).toBeDisabled();
  });

  it("a refused testcase add (PRB-046) reads its own text and the dialog stays open", async () => {
    apiMock.post.mockRejectedValue(new ApiError("PRB-046", 422));
    const toasts = watch();
    await open();
    goTestcases();
    fireEvent.click(screen.getByRole("button", { name: m.addTestcase }));
    const dialog = screen.getByRole("dialog");
    const [input, expected] = within(dialog).getAllByRole("textbox");
    fireEvent.change(input!, { target: { value: "x" } });
    fireEvent.change(expected!, { target: { value: "y" } });
    fireEvent.click(
      within(dialog).getByRole("button", { name: m.dialog.save }),
    );

    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["PRB-046"]]),
    );
    expect(screen.getByRole("dialog")).toBeInTheDocument();
  });

  it("edits only what changed: a new expected output sends expectedOutput alone", async () => {
    apiMock.patch.mockResolvedValue({
      testcase: tc(3, { expectedOutput: "new" }),
      problemUpdatedAt: T1,
    });
    await open();
    goTestcases();
    fireEvent.click(screen.getAllByRole("button", { name: m.edit })[2]!);
    const dialog = screen.getByRole("dialog");
    fireEvent.change(within(dialog).getAllByRole("textbox")[1]!, {
      target: { value: "new" },
    });
    fireEvent.click(
      within(dialog).getByRole("button", { name: m.dialog.save }),
    );
    await waitFor(() =>
      expect(apiMock.patch).toHaveBeenCalledWith(`${A}/${P}/testcases/tc-3`, {
        expectedOutput: "new",
      }),
    );
  });

  it("deletes after confirmation; PRB-054 keeps the row and shows its text", async () => {
    apiMock.delete.mockRejectedValueOnce(new ApiError("PRB-054", 422));
    apiMock.delete.mockResolvedValueOnce({ problemUpdatedAt: T1 });
    const toasts = watch();
    await open();
    goTestcases();
    const confirm = async () => {
      fireEvent.click(screen.getAllByRole("button", { name: m.delete })[0]!);
      const dialog = await screen
        .findByRole("alertdialog")
        .catch(() => screen.findByRole("dialog"));
      fireEvent.click(within(dialog).getByRole("button", { name: m.delete }));
    };
    await confirm();
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["PRB-054"]]),
    );
    expect(screen.getByText("in-1")).toBeInTheDocument();

    await confirm();
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["success", m.toast.testcaseDeleted]),
    );
    expect(apiMock.delete).toHaveBeenLastCalledWith(`${A}/${P}/testcases/tc-1`);
    await waitFor(() => expect(screen.queryByText("in-1")).toBeNull());
  });

  it("runs the reference solution with the editor's code and reads the first failure", async () => {
    apiMock.post.mockResolvedValue({
      compileError: null,
      summary: { passed: 1, total: 3, allPass: false },
      results: [
        { position: 2, verdict: "FAIL", failure: "WRONG_ANSWER" },
        { position: 1, verdict: "PASS", failure: null },
        { position: 3, verdict: "FAIL", failure: "TIME_LIMIT_EXCEEDED" },
      ],
    });
    const toasts = watch();
    await open();
    fireEvent.click(screen.getByRole("button", { name: m.runSolution }));
    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith(
        `${A}/${P}/reference-solution/run`,
        { language: "PYTHON", sourceCode: "print(1)" },
      ),
    );
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual([
        "warning",
        m.runFailedAt
          .replace("{position}", "2")
          .replace("{reason}", m.runFailure.WRONG_ANSWER)
          .replace("{passed}", "1")
          .replace("{total}", "3"),
      ]),
    );
  });

  it("a testcase that does not match the declared signature (PRB-060) is explained and stays on screen", async () => {
    postRoutes(() => {
      throw new ApiError("PRB-060", 422);
    });
    await open();
    fireEvent.click(screen.getByRole("button", { name: m.runSolution }));

    const note = await screen.findAllByText(apiErrors["PRB-060"]!);
    // Once in the solution card and once under the checklist: both outlive the toast.
    expect(note.length).toBeGreaterThanOrEqual(2);
    expect(
      screen.getAllByText(m.runNote.negative).length,
    ).toBeGreaterThanOrEqual(2);
    // The checklist must not keep claiming the solution passes after a run that could not run.
    expect(
      screen.getByRole("button", { name: m.check.solutionPasses }),
    ).toBeInTheDocument();
  });

  it("copies the public testcases into the examples once, and says so when there is nothing new", async () => {
    postRoutes(() => undefined);
    const toasts = watch();
    await open();
    goExamples();
    expect(
      screen.getByRole("button", {
        name: m.tab.examples.replace("{count}", "2"),
      }),
    ).toBeInTheDocument();

    fireEvent.click(
      screen.getByRole("button", { name: m.examplesFromTestcases }),
    );
    // The fixture has 2 public testcases that the 2 existing examples do not repeat.
    expect(
      screen.getByRole("button", {
        name: m.tab.examples.replace("{count}", "4"),
      }),
    ).toBeInTheDocument();
    expect(toasts().at(-1)).toEqual([
      "success",
      m.toast.examplesFromTestcases.replace("{count}", "2"),
    ]);

    fireEvent.click(
      screen.getByRole("button", { name: m.examplesFromTestcases }),
    );
    expect(toasts().at(-1)).toEqual(["info", m.toast.examplesNothingNew]);
    expect(
      screen.getByRole("button", {
        name: m.tab.examples.replace("{count}", "4"),
      }),
    ).toBeInTheDocument();
  });

  it("a problem with no approved public testcase has nothing to copy", async () => {
    routes(
      detail({
        testcases: Array.from({ length: 8 }, (_, i) =>
          tc(i + 1, { visibility: "HIDDEN" }),
        ),
        testcaseSummary: { total: 8, approved: 8, sample: 0, hidden: 8 },
      }),
    );
    postRoutes(() => undefined);
    const toasts = watch();
    await open();
    goExamples();
    fireEvent.click(
      screen.getByRole("button", { name: m.examplesFromTestcases }),
    );
    expect(toasts().at(-1)).toEqual(["warning", m.toast.examplesNoPublic]);
  });

  it("shows one tab at a time; the reference solution sits with the statement", async () => {
    await open();
    const testcases = m.tab.testcases.replace("{count}", "8");

    expect(screen.getByRole("region", { name: m.tab.solution })).toBeVisible();
    expect(screen.queryByRole("region", { name: testcases })).toBeNull();

    fireEvent.click(screen.getByRole("button", { name: testcases }));
    expect(screen.getByRole("region", { name: testcases })).toBeVisible();
    expect(screen.queryByRole("region", { name: m.tab.solution })).toBeNull();
  });

  it("a compile error and a rate limit (PRB-055) each read as their own message", async () => {
    let runs = 0;
    postRoutes(() => {
      if (++runs === 1)
        return {
          compileError: "boom",
          summary: { passed: 0, total: 8, allPass: false },
          results: [],
        };
      throw new ApiError("PRB-055", 429);
    });
    const toasts = watch();
    await open();
    fireEvent.click(screen.getByRole("button", { name: m.runSolution }));
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", m.runCompileError]),
    );
    fireEvent.click(screen.getByRole("button", { name: m.runSolution }));
    await waitFor(() =>
      expect(toasts().at(-1)).toEqual(["error", apiErrors["PRB-055"]]),
    );
  });

  it("a read-only problem (A2, someone else's published one) shows the notice and cannot be saved", async () => {
    routes(detail({ readOnly: true, status: "PUBLISHED" }));
    await open();
    expect(screen.getByText(m.readOnlyNotice)).toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(m.titleLabel), {
      target: { value: "x" },
    });
    expect(screen.getByRole("button", { name: m.save })).toBeDisabled();
    expect(screen.getByRole("button", { name: m.publish })).toBeDisabled();
  });

  it("without testcase access the tab count and a notice come from the summary, not from an empty list", async () => {
    routes(
      detail({
        testcases: [],
        testcaseSummary: { total: 9, approved: 8, sample: 2, hidden: 7 },
      }),
    );
    await open();
    expect(
      screen.getByRole("button", { name: "Testcase (9)" }),
    ).toBeInTheDocument();
    goTestcases();
    expect(
      screen.getByText(m.testcasesHiddenNotice.replace("{total}", "9")),
    ).toBeInTheDocument();
  });

  it("a problem not found reads as not found, a forbidden one as a load failure", async () => {
    apiMock.get.mockImplementation(async (path: string) => {
      if (path === "/problem-bank/levels") return { items: [] };
      if (path === "/problem-bank/topics") return { items: [] };
      throw new ApiError("PRB-010", 404);
    });
    render(
      withTestProviders(
        <ProblemAuthoringView basePath="/admin/problems" problemId={P} />,
      ),
    );
    expect(await screen.findByText(m.notFound)).toBeInTheDocument();
  });
});

describe("bulk upload (EVT-15, #45 and #46)", () => {
  const file = (name: string, content: string) =>
    new File([content], name, { type: "text/plain" });

  async function pick(files: File[]) {
    goTestcases();
    fireEvent.click(
      screen.getByRole("button", { name: m.testcase.btnBulkUpload }),
    );
    const dialog = screen.getByRole("dialog");
    fireEvent.change(within(dialog).getByTestId("bulk-input"), {
      target: { files },
    });
    return dialog;
  }

  beforeEach(() => {
    resetApiMock();
    routes();
  });

  it("shows the matched pairs with sizes, uploads through the presigned URLs, confirms and adds the rows", async () => {
    postRoutes((path: string) =>
      path.endsWith("bulk-upload-url")
        ? {
            uploadId: "u1",
            items: [
              {
                clientKey: "p1",
                input: { putUrl: "https://s/in-1" },
                expectedOutput: { putUrl: "https://s/out-1" },
              },
            ],
          }
        : path.endsWith("reference-solution/run")
          ? {
              compileError: null,
              summary: { passed: 9, total: 9, allPass: true },
              results: [],
            }
          : {
              added: [tc(9, { input: "abc", expectedOutput: "de" })],
              problemUpdatedAt: T1,
            },
    );
    const fetchMock = vi.fn().mockResolvedValue({ ok: true });
    vi.stubGlobal("fetch", fetchMock);
    const toasts = watch();
    await open();
    const dialog = await pick([file("01.out", "de"), file("01.in", "abc")]);

    expect(within(dialog).getByText("01")).toBeInTheDocument();
    expect(within(dialog).getByText("3 B")).toBeInTheDocument();
    expect(within(dialog).getByText("2 B")).toBeInTheDocument();
    fireEvent.click(
      within(dialog).getByRole("button", {
        name: m.bulk.upload.replace("{count}", "1"),
      }),
    );

    await waitFor(() =>
      expect(toasts()).toContainEqual([
        "success",
        m.bulk.done.replace("{count}", "1"),
      ]),
    );
    expect(postCalls()[0]).toEqual([
      `${A}/${P}/testcases/bulk-upload-url`,
      {
        items: [
          {
            clientKey: "p1",
            inputSizeBytes: 3,
            expectedOutputSizeBytes: 2,
            // The fixture already has 2 public testcases, so nothing defaults to public.
            visibility: "HIDDEN",
          },
        ],
      },
    ]);
    expect(fetchMock).toHaveBeenCalledTimes(2);
    expect(postCalls()[1]).toEqual([
      `${A}/${P}/testcases/bulk-upload/confirm`,
      { uploadId: "u1" },
    ]);
    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
    expect(
      screen.getByRole("button", { name: "Testcase (9)" }),
    ).toBeInTheDocument();
    vi.unstubAllGlobals();
  });

  it("sends the first two pairs as public and the rest hidden, and a row the author changed keeps its choice", async () => {
    postRoutes((path: string) =>
      path.endsWith("bulk-upload-url")
        ? {
            uploadId: "u2",
            items: ["p1", "p2", "p3"].map((clientKey) => ({
              clientKey,
              input: { putUrl: `https://s/in-${clientKey}` },
              expectedOutput: { putUrl: `https://s/out-${clientKey}` },
            })),
          }
        : path.endsWith("reference-solution/run")
          ? {
              compileError: null,
              summary: { passed: 1, total: 1, allPass: true },
              results: [],
            }
          : { added: [], problemUpdatedAt: T1 },
    );
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true }));
    // A problem with no testcases yet: the publish minimum of 2 public ones is still to be filled.
    routes(
      detail({
        testcases: [],
        testcaseSummary: { total: 0, approved: 0, sample: 0, hidden: 0 },
      }),
    );
    await open();
    const dialog = await pick(
      ["01", "02", "03"].flatMap((base) => [
        file(`${base}.in`, "1"),
        file(`${base}.out`, "2"),
      ]),
    );

    const choice = (base: string) =>
      within(dialog).getByLabelText(
        m.bulk.visibilityOf.replace("{file}", base),
      ) as HTMLSelectElement;
    expect([
      choice("01").value,
      choice("02").value,
      choice("03").value,
    ]).toEqual(["public", "public", "hidden"]);
    fireEvent.change(choice("02"), { target: { value: "hidden" } });
    fireEvent.click(
      within(dialog).getByRole("button", {
        name: m.bulk.upload.replace("{count}", "3"),
      }),
    );

    await waitFor(() =>
      expect(apiMock.post).toHaveBeenCalledWith(
        `${A}/${P}/testcases/bulk-upload-url`,
        {
          items: [
            expect.objectContaining({ clientKey: "p1", visibility: "SAMPLE" }),
            expect.objectContaining({ clientKey: "p2", visibility: "HIDDEN" }),
            expect.objectContaining({ clientKey: "p3", visibility: "HIDDEN" }),
          ],
        },
      ),
    );
    vi.unstubAllGlobals();
  });

  it("reports unmatched, wrong-extension and duplicate files before any request and keeps Upload disabled", async () => {
    await open();
    const dialog = await pick([
      file("01.in", "a"),
      file("01.out", "b"),
      file("02.in", "a"),
      file("notes.txt", "x"),
      file("01.IN", "a"),
    ]);
    const issues = within(dialog).getByRole("list", {
      name: m.bulk.issuesLabel,
    });
    expect(issues).toHaveTextContent(
      m.bulk.issue.unmatched.replace("{file}", "02.in"),
    );
    expect(issues).toHaveTextContent(
      m.bulk.issue.extension.replace("{file}", "notes.txt"),
    );
    expect(issues).toHaveTextContent(
      m.bulk.issue.duplicate.replace("{file}", "01.IN"),
    );
    expect(
      within(dialog).getByRole("button", {
        name: m.bulk.upload.replace("{count}", "1"),
      }),
    ).toBeDisabled();
    expect(postCalls()).toHaveLength(0);
  });

  it("reports more than 100 pairs", async () => {
    await open();
    const files = Array.from({ length: 101 }, (_, i) => [
      file(`${i}.in`, "a"),
      file(`${i}.out`, "b"),
    ]).flat();
    const dialog = await pick(files);
    expect(
      within(dialog).getByRole("list", { name: m.bulk.issuesLabel }),
    ).toHaveTextContent(
      m.bulk.issue.tooManyPairs
        .replace("{count}", "101")
        .replace("{limit}", "100"),
    );
  });

  it("PRB-045 toasts one line per faulty file, named from the picked files, and keeps the dialog open", async () => {
    postRoutes((path: string) => {
      if (path.endsWith("bulk-upload-url"))
        return {
          uploadId: "u1",
          items: [
            {
              clientKey: "p1",
              input: { putUrl: "https://s/1" },
              expectedOutput: { putUrl: "https://s/2" },
            },
          ],
        };
      throw new ApiError("PRB-045", 422, undefined, {
        items: [
          { clientKey: "p1", side: "expectedOutput", problem: "SIZE_MISMATCH" },
          { clientKey: "p1", side: "input", problem: "INVALID_TEXT" },
        ],
      });
    });
    vi.stubGlobal("fetch", vi.fn().mockResolvedValue({ ok: true }));
    const toasts = watch();
    await open();
    const dialog = await pick([file("07.in", "a"), file("07.out", "b")]);
    fireEvent.click(
      within(dialog).getByRole("button", {
        name: m.bulk.upload.replace("{count}", "1"),
      }),
    );
    await waitFor(() => expect(toasts()).toHaveLength(2));
    expect(toasts()).toEqual([
      [
        "error",
        m.bulk.itemError
          .replace("{file}", "07.out")
          .replace("{reason}", m.bulk.fault.SIZE_MISMATCH),
      ],
      [
        "error",
        m.bulk.itemError
          .replace("{file}", "07.in")
          .replace("{reason}", m.bulk.fault.INVALID_TEXT),
      ],
    ]);
    expect(screen.getByRole("dialog")).toBeInTheDocument();
    vi.unstubAllGlobals();
  });

  it.each(["PRB-044", "PRB-046", "PRB-047", "PRB-052"])(
    "%s reads its own text",
    async (code) => {
      postRoutes(() => {
        throw new ApiError(code, 422);
      });
      const toasts = watch();
      await open();
      const dialog = await pick([file("1.in", "a"), file("1.out", "b")]);
      fireEvent.click(
        within(dialog).getByRole("button", {
          name: m.bulk.upload.replace("{count}", "1"),
        }),
      );
      await waitFor(() =>
        expect(toasts().at(-1)).toEqual(["error", apiErrors[code]]),
      );
    },
  );

  it("a new problem that is not saved yet cannot upload: the button is disabled with a hint", async () => {
    render(
      withTestProviders(<ProblemAuthoringView basePath="/admin/problems" />),
    );
    await screen.findByLabelText(m.titleLabel);
    goTestcases();
    const button = screen.getByRole("button", {
      name: m.testcase.btnBulkUpload,
    });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute("title", m.bulk.saveFirst);
  });
});
