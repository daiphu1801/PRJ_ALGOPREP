// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Soạn đề bài.dc.html: sticky header (:147-156), then a
// 1fr / 300px grid — four tabs on the left (:158-311) and a sticky property/checklist column on
// the right (:313-378).
//
// About PROTOTYPE_DEBT 2.6: that entry says six things still need removing from the mockup (the
// "Điểm" column, per-row weights, the "Chấm điểm từng phần" block, `totalWeight`, the weight*
// variables, and the "Tổng trọng số bằng 100" publish check). They are ALREADY GONE — grep finds no
// "trọng số" anywhere in that file. The debt entry is the stale one, not the prototype; flagged in
// the phase report so the ledger gets corrected rather than re-actioned.
//
// One divergence: the mockup's third AI guard, "Cho phép AI mở gợi ý ẩn", is dropped. Revealing
// tiered hints is the feature DEC-2026-0831-remove-tiered-hints-ai-config cut entirely. Whether
// that label meant something else is raised in the report rather than guessed at.
"use client";

import { useRef, useState } from "react";
import { ArrowLeft, Check, Eye, Pencil, Trash2, X } from "lucide-react";
import {
  AI_GUARD_KEYS,
  TESTCASE_CATEGORIES,
  isSpecComplete,
  realBulkUpload,
  realCreateTestcase,
  realDeleteTestcase,
  realRunReferenceSolution,
  realUpdateTestcase,
  useGenerateTestcases,
  useProblemDraft,
  usePublishProblem,
  useSaveProblemDraft,
  problemTopicLabel,
  useProblemLevels,
  useProblemTopics,
  type AiGuardKey,
  type ProblemDraft,
  type ProblemStats,
  type SaveResult,
  type SaveSection,
  type ServerState,
  type Testcase,
  type TestcaseCategory,
  type WorkedExample,
} from "@/entities/problem";
import { ApiError, isMockMode, isNotFound } from "@/shared/api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { useUnsavedChangesGuard } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  EmptyState,
  ErrorState,
  IconAction,
  NoticeTile,
  PageHeader,
  SegmentedTabs,
  SelectField,
  SettingRow,
  Skeleton,
  TextArea,
  TextField,
  Toggle,
  UnsavedChangesDialog,
  type DataTableColumn,
} from "@/shared/ui";
import type { BulkPairRow } from "../model/bulk-pairs";
import { BulkUploadDialog } from "./bulk-upload-dialog";
import { ExampleDialog, TestcaseDialog } from "./edit-dialogs";
import { SpecEditor } from "./spec-editor";

type TabKey = "content" | "examples" | "testcases" | "spec" | "ai";

// BD SHR0202 NO4-6: required positive integers. NO7 (submissions per hour) is optional and rendered
// separately because an empty box means "system default", not zero.
const LIMIT_KEYS: ("timeLimitMs" | "memoryLimitMb" | "outputLimitKb")[] = [
  "timeLimitMs",
  "memoryLimitMb",
  "outputLimitKb",
];

const SOLUTION_LANGUAGES = ["Java", "C++", "Python"] as const;

const isPositiveInt = (value: number) => Number.isInteger(value) && value > 0;

// F2-18: the check covers the testcase set and solution as they were when it ran, so any edit to
// either one makes the last result stale and the author has to run it again.
const STALE_CHECK = { ran: false, passed: 0, total: 0 };

type SaveState = "idle" | "saving" | "saved" | "error";

// Fields of the draft that each save step stores (real mode: `realSaveDraft`).
const SECTION_FIELDS: Record<SaveSection, (keyof ProblemDraft)[]> = {
  content: [
    "title",
    "body",
    "constraints",
    "topic",
    "otherTopicIds",
    "difficulty",
    "limits",
    "tags",
    "solutionLanguage",
    "solution",
    "examples",
  ],
  spec: ["spec"],
  ai: ["aiBrief", "aiGuards"],
  status: ["status"],
};

/** The saved copy after a save that stopped half way: only the sections the server took. */
function withApplied(
  saved: ProblemDraft,
  draft: ProblemDraft,
  applied: SaveSection[],
): ProblemDraft {
  const next: Record<string, unknown> = { ...saved };
  for (const section of applied)
    for (const field of SECTION_FIELDS[section]) next[field] = draft[field];
  return next as ProblemDraft;
}

// PRB-030 lists every missing publish condition; each one reads as the checklist line of the same meaning.
const PUBLISH_ITEM_KEY: Record<string, string> = {
  MIN_APPROVED_TESTCASES: "minTestcases",
  MIN_APPROVED_SAMPLES: "minPublic",
  MIN_WORKED_EXAMPLES: "minExamples",
  REFERENCE_SOLUTION_PASSES_ALL: "solutionPasses",
  SPEC_DECLARED: "specDeclared",
};

type Props = {
  /** Area root the back link returns to, e.g. "/admin/problems". */
  basePath: string;
  /** Route param of an existing problem; absent on the "new" route (nothing saved to preview yet). */
  problemId?: string;
  /** Source problem of a duplicate (F2-16): the "new" route opens pre-filled and unsaved. */
  fromId?: string;
};

/** Loads the problem, then hands it to the form, which owns the editable copy. */
export function ProblemAuthoringView({ basePath, problemId, fromId }: Props) {
  const t = useT("problemAuthoring");
  const query = useProblemDraft(problemId, fromId);

  if (isNotFound(query.error)) return <EmptyState>{t("notFound")}</EmptyState>;
  if (query.isError) return <ErrorState>{t("loadFailed")}</ErrorState>;
  if (!query.data) {
    return (
      <div className="flex flex-col gap-3.5" aria-busy="true">
        <Skeleton className="h-[62px] w-full" />
        <Skeleton className="h-[320px] w-full" />
      </div>
    );
  }
  return (
    <ProblemAuthoringForm
      basePath={basePath}
      problemId={problemId}
      isCopy={!problemId && !!fromId}
      server={query.data.server}
      initial={query.data.draft}
      aiGeneration={query.data.aiGeneration}
      stats={query.data.stats}
      initialSavedAt={query.data.savedAt}
    />
  );
}

type FormProps = Props & {
  initial: ProblemDraft;
  /** A duplicate starts dirty, so Save is enabled before the first edit. */
  isCopy?: boolean;
  /** F2-14 quota read from the server: attempts used and the cap. */
  aiGeneration: { used: number; limit: number };
  stats: ProblemStats | null;
  initialSavedAt: string | null;
  /** Real mode: what the server said at load (lock value, read-only, counts). Absent on mock data. */
  server?: ServerState;
};

function ProblemAuthoringForm({
  basePath,
  problemId,
  initial,
  isCopy,
  aiGeneration,
  stats,
  initialSavedAt,
  server,
}: FormProps) {
  const t = useT("problemAuthoring");
  const apiMessage = useApiErrorMessage();
  const [draft, setDraft] = useState<ProblemDraft>(initial);
  const [savedAt, setSavedAt] = useState(initialSavedAt);
  const topicList = useProblemTopics();
  const levelList = useProblemLevels();
  const saveDraft = useSaveProblemDraft();
  const publishDraft = usePublishProblem();
  // A save from "new" (or a copy) creates the problem; later saves of this form must target it.
  const [createdId, setCreatedId] = useState<string>();
  const id = problemId ?? createdId;
  const generate = useGenerateTestcases();
  const [tab, setTab] = useState<TabKey>("content");
  // Last persisted copy: "dirty" is "the draft no longer matches it". A duplicate has none yet. In real
  // mode the run result is not persisted, so it does not count as a change.
  const [saved, setSaved] = useState<ProblemDraft | null>(
    isCopy ? null : initial,
  );
  const persistKey = (value: ProblemDraft) =>
    JSON.stringify(server ? { ...value, solutionCheck: STALE_CHECK } : value);
  // The `updatedAt` the next write must quote (optimistic lock); testcase writes move it too.
  const lock = useRef(server?.updatedAt ?? null);
  const [running, setRunning] = useState(false);
  const [saveState, setSaveState] = useState<SaveState>("idle");
  const dirty = saved === null || persistKey(draft) !== persistKey(saved);
  const saving = saveState === "saving";
  // BD Q7/Q12/Q14: leaving with unsaved changes (back icon, sidebar, browser Back, tab close) asks first.
  const leaveGuard = useUnsavedChangesGuard(dirty);

  // "new" = add dialog open; a row = edit dialog for that row; null = closed.
  const [testcaseDialog, setTestcaseDialog] = useState<Testcase | "new" | null>(
    null,
  );
  const [bulkOpen, setBulkOpen] = useState(false);
  const [exampleDialog, setExampleDialog] = useState<
    WorkedExample | "new" | null
  >(null);
  const [testcaseToDelete, setTestcaseToDelete] = useState<Testcase | null>(
    null,
  );
  const [exampleToDelete, setExampleToDelete] = useState<WorkedExample | null>(
    null,
  );
  const nextId = useRef(0);

  function patch(changes: Partial<ProblemDraft>) {
    setDraft((previous) => ({ ...previous, ...changes }));
  }

  // Real mode with a saved problem: a testcase is written the moment the author confirms it (BD EVT-19
  // to EVT-23), and the row on screen becomes the server's row. Otherwise the row stays on the screen.
  const liveId = server ? id : undefined;
  const readOnly = server?.readOnly ?? false;
  // The caller may not read testcase bodies (no TESTCASE_MANAGEMENT:READ, or a read-only view): the
  // server then sends counts only, and the table must not pretend the problem has no testcases.
  const testcasesHidden =
    server !== undefined &&
    draft.testcases.length === 0 &&
    server.testcaseSummary.total > 0;
  const testcaseCount = testcasesHidden
    ? server.testcaseSummary.total
    : draft.testcases.length;

  // The screen row and the saved copy move together: a live write is never an unsaved change.
  function commitTestcases(next: (rows: Testcase[]) => Testcase[]) {
    setDraft((previous) => ({
      ...previous,
      solutionCheck: STALE_CHECK,
      testcases: next(previous.testcases),
    }));
    setSaved((previous) =>
      previous
        ? { ...previous, testcases: next(previous.testcases) }
        : previous,
    );
  }

  async function saveTestcase(
    values: Pick<Testcase, "input" | "expected" | "visibility">,
  ) {
    const editing = testcaseDialog !== "new" ? testcaseDialog : null;
    if (liveId) {
      try {
        const written = editing
          ? await realUpdateTestcase(liveId, editing.id, {
              // A side stored outside the database is never sent back (the dialog cannot show it).
              ...(editing.largeSides?.includes("input") ||
              values.input === editing.input
                ? {}
                : { input: values.input }),
              ...(editing.largeSides?.includes("expected") ||
              values.expected === editing.expected
                ? {}
                : { expected: values.expected }),
              ...(values.visibility === editing.visibility
                ? {}
                : { visibility: values.visibility }),
            })
          : await realCreateTestcase(liveId, values);
        lock.current = written.updatedAt;
        commitTestcases((rows) =>
          editing
            ? rows.map((row) =>
                row.id === editing.id ? written.testcase : row,
              )
            : [...rows, written.testcase],
        );
      } catch (error) {
        toast.error(apiMessage(error));
        return;
      }
    } else {
      setDraft((previous) => ({
        ...previous,
        solutionCheck: STALE_CHECK,
        testcases: editing
          ? previous.testcases.map((row) =>
              row.id === editing.id ? { ...row, ...values } : row,
            )
          : [
              ...previous.testcases,
              {
                ...values,
                id: `tc-new-${++nextId.current}`,
                origin: "manual",
                approved: true,
              },
            ],
      }));
    }
    setTestcaseDialog(null);
    toast.success(t(editing ? "toast.testcaseUpdated" : "toast.testcaseAdded"));
  }

  // EVT-15. Real mode needs a saved problem (the upload URLs are per problem id); mock mode reads the
  // files and appends the rows to the draft like a manual add.
  const bulkNeedsSave = !isMockMode() && !liveId;
  async function bulkUpload(
    pairs: BulkPairRow<File>[],
    visibility: Testcase["visibility"],
  ) {
    if (liveId) {
      const done = await realBulkUpload(
        liveId,
        pairs.map((pair) => ({
          clientKey: pair.key,
          input: pair.input,
          expected: pair.expected,
          visibility,
        })),
      );
      lock.current = done.updatedAt;
      commitTestcases((rows) => [...rows, ...done.added]);
      toast.success(t("bulk.done", { count: done.added.length }));
      return;
    }
    const added: Testcase[] = [];
    for (const pair of pairs) {
      added.push({
        id: `tc-new-${++nextId.current}`,
        input: (await pair.input.text()).trim(),
        expected: (await pair.expected.text()).trim(),
        visibility,
        origin: "manual",
        approved: true,
      });
    }
    setDraft((previous) => ({
      ...previous,
      solutionCheck: STALE_CHECK,
      testcases: [...previous.testcases, ...added],
    }));
    toast.success(t("bulk.done", { count: added.length }));
  }

  // Delete of a row, and "Loại" of an unapproved draft (the same call, DD #43).
  async function removeTestcase(
    rowId: string,
    messageKey: "toast.testcaseDeleted" | "toast.draftRejected",
  ) {
    if (liveId) {
      try {
        lock.current = (await realDeleteTestcase(liveId, rowId)).updatedAt;
      } catch (error) {
        toast.error(apiMessage(error));
        return;
      }
      commitTestcases((rows) => rows.filter((row) => row.id !== rowId));
    } else {
      setDraft((previous) => ({
        ...previous,
        // A draft was never part of the checked set, so discarding one leaves the last run valid.
        solutionCheck:
          messageKey === "toast.draftRejected"
            ? previous.solutionCheck
            : STALE_CHECK,
        testcases: previous.testcases.filter((row) => row.id !== rowId),
      }));
    }
    toast.success(t(messageKey));
  }

  async function deleteTestcase(rowId: string) {
    await removeTestcase(rowId, "toast.testcaseDeleted");
    setTestcaseToDelete(null);
  }

  function saveExample(
    values: Pick<WorkedExample, "input" | "output" | "explanation">,
  ) {
    const editing = exampleDialog !== "new" ? exampleDialog : null;
    setDraft((previous) => ({
      ...previous,
      examples: editing
        ? previous.examples.map((row) =>
            row.id === editing.id ? { ...row, ...values } : row,
          )
        : [
            ...previous.examples,
            { ...values, id: `ex-new-${++nextId.current}` },
          ],
    }));
    setExampleDialog(null);
    toast.success(t(editing ? "toast.exampleUpdated" : "toast.exampleAdded"));
  }

  // BD Sheet 6 area D item 6: a published problem keeps at least 2 examples (publish checklist).
  // The button stays visible; a blocked click only warns.
  function requestDeleteExample(example: WorkedExample) {
    if (draft.status === "published" && draft.examples.length <= 2) {
      toast.warning(t("examplesFloorWarning"));
      return;
    }
    setExampleToDelete(example);
  }

  function deleteExample(id: string) {
    setDraft((previous) => ({
      ...previous,
      examples: previous.examples.filter((row) => row.id !== id),
    }));
    setExampleToDelete(null);
    toast.success(t("toast.exampleDeleted"));
  }

  // What a finished save changes on screen. Mock data: the whole draft counts as saved. Real data: either
  // the server's own copy (every step went through) or, after a step failed, only the sections it took.
  // Returns true when the save went through completely.
  function applyResult(result: SaveResult, snapshot: ProblemDraft): boolean {
    if (result.createdId) setCreatedId(result.createdId);
    if (result.updatedAt !== undefined) lock.current = result.updatedAt;
    const failed = result.error !== undefined;
    if (!server) {
      setSaved(snapshot);
    } else if (!failed && result.record) {
      const fresh = result.record.draft;
      setDraft((previous) => ({
        ...fresh,
        solutionCheck: previous.solutionCheck,
      }));
      setSaved(fresh);
    } else {
      const base = saved ?? initial;
      const next = withApplied(base, snapshot, result.applied);
      // A created problem now has its own testcase ids; later live writes need them.
      const rows = result.record?.draft.testcases;
      setSaved(rows ? { ...next, testcases: rows } : next);
      if (rows) setDraft((previous) => ({ ...previous, testcases: rows }));
      // A publish the server refused must not leave the status switch on "published".
      if (failed && !result.applied.includes("status"))
        setDraft((previous) => ({ ...previous, status: next.status }));
    }
    if (result.record?.savedAt != null) setSavedAt(result.record.savedAt);
    else if (!failed) setSavedAt(new Date().toISOString());
    return !failed;
  }

  // The server's refusal, in words: PRB-030 names every missing publish condition, the rest are one line.
  function reportFailure(error: unknown, fallbackKey: string) {
    if (error instanceof ApiError && error.code === "PRB-030") {
      const items = Array.isArray(error.details?.items)
        ? (error.details.items as { code?: string; met?: boolean | null }[])
        : [];
      const missing = items
        .filter((item) => item.met !== true && item.code)
        .map((item) => PUBLISH_ITEM_KEY[item.code!])
        .filter(Boolean)
        .map((key) => t(`check.${key}`));
      toast.warning(
        missing.length
          ? `${t("checklistBlockTitle")}: ${missing.join("; ")}`
          : apiMessage(error),
      );
      return;
    }
    toast.error(error instanceof ApiError ? apiMessage(error) : t(fallbackKey));
  }

  // Manual save (BD Q7). Resolves true when the save went through, so "save and leave" can wait on it.
  async function save(): Promise<boolean> {
    const snapshot = draft;
    // BD SHR0202 Sheet 9 NO3-4: required Tab 1 fields and positive integer limits. Before 2026-10-14
    // Save sent anything (UX review B8). One toast names the first problem (toast channel decision).
    const invalidLimit = LIMIT_KEYS.find(
      (key) => !isPositiveInt(snapshot.limits[key]),
    );
    const problemText = !snapshot.title.trim()
      ? t("validation.titleRequired")
      : !snapshot.body.trim()
        ? t("validation.bodyRequired")
        : invalidLimit
          ? t("validation.limitPositive", { field: t(`limit.${invalidLimit}`) })
          : snapshot.limits.maxSubmissionsPerHour !== null &&
              !isPositiveInt(snapshot.limits.maxSubmissionsPerHour)
            ? t("validation.limitPositive", {
                field: t("limit.maxSubmissionsPerHour"),
              })
            : null;
    if (problemText) {
      setTab("content");
      toast.error(problemText);
      return false;
    }
    return write(snapshot, false);
  }

  async function write(
    snapshot: ProblemDraft,
    publishing: boolean,
  ): Promise<boolean> {
    setSaveState("saving");
    try {
      const mutate = publishing ? publishDraft : saveDraft;
      const result = await mutate.mutateAsync({
        draft: snapshot,
        problemId: id,
        saved: saved ?? initial,
        server,
        updatedAt: lock.current,
      });
      const done = applyResult(result, snapshot);
      if (!done) {
        setSaveState("error");
        reportFailure(
          result.error,
          publishing ? "toast.publishFailed" : "toast.saveFailed",
        );
        return false;
      }
      if (publishing)
        setDraft((previous) => ({ ...previous, status: "published" }));
      setSaveState("saved");
      toast.success(t(publishing ? "toast.published" : "toast.saved"));
      return true;
    } catch {
      setSaveState("error");
      toast.error(t(publishing ? "toast.publishFailed" : "toast.saveFailed"));
      return false;
    }
  }

  async function saveAndLeave() {
    if (await save()) leaveGuard.leave();
  }

  // BD Q8: hard block. The button looks disabled while the checklist has unmet items but stays
  // clickable, so a click names EVERY missing condition and the checklist items jump to their tab.
  // Real mode: the server runs the reference solution itself at this moment (BD EVT-33), so the screen
  // does not insist on a client-side run first, and PRB-030 names whatever is still missing.
  async function publish() {
    const clientBlocking = server
      ? blocking.filter((item) => item.key !== "solutionPasses")
      : blocking;
    if (clientBlocking.length > 0) {
      toast.warning(
        `${t("checklistBlockTitle")}: ${clientBlocking.map((item) => t(`check.${item.key}`)).join("; ")}`,
      );
      return;
    }
    await write({ ...draft, status: "published" }, true);
  }

  // Mock data: every approved row passes. Real data (#47): the server runs the editor's code against
  // every approved testcase and answers with verdicts only (F2-18).
  async function runSolution() {
    if (!draft.solution.trim()) {
      toast.warning(t("runBlocked.noSolution"));
      return;
    }
    if (server && !id) {
      toast.warning(t("runBlocked.needsSave"));
      return;
    }
    if (!server && approved.length === 0) {
      toast.warning(t("runBlocked.noTestcase"));
      return;
    }
    if (server && id) {
      if (running) return;
      setRunning(true);
      try {
        const run = await realRunReferenceSolution(id, {
          language: draft.solutionLanguage,
          sourceCode: draft.solution,
        });
        if (run.compileError !== null) {
          toast.error(t("runCompileError"));
        } else if (run.total === 0) {
          toast.warning(t("runBlocked.noTestcase"));
        } else {
          setDraft((previous) => ({
            ...previous,
            solutionCheck: { ran: true, passed: run.passed, total: run.total },
          }));
          if (run.allPass) {
            toast.success(
              t("runResultBody", { passed: run.passed, total: run.total }),
            );
          } else {
            const reason = run.firstFailure
              ? t(`runFailure.${run.firstFailure.failure}`)
              : "";
            toast.warning(
              t("runFailedAt", {
                position: run.firstFailure?.position ?? 0,
                reason,
                passed: run.passed,
                total: run.total,
              }),
            );
          }
        }
      } catch (error) {
        toast.error(apiMessage(error));
      } finally {
        setRunning(false);
      }
      return;
    }
    const total = draft.testcases.filter((row) => row.approved).length;
    const passed = total;
    setDraft((previous) => ({
      ...previous,
      solutionCheck: { ran: true, passed, total },
    }));
    // The run result is an operation result, so it is a toast; a failing run is a warning.
    (passed === total ? toast.success : toast.warning)(
      t("runResultBody", { passed, total }),
    );
  }

  // F2-14 (BD EVT-16). The backend does the pipeline; the screen sends the statement, constraints
  // and the author-written Samples (never the reference solution), then merges the returned rows as
  // unapproved drafts. Inputs already in the set are skipped: the generator seed is fixed, so asking
  // again returns the same rows. Drafts are not part of the checked set, so the last run stays valid.
  async function generateTestcases() {
    if (!canGenerate) {
      toast.warning(t("generateLockedBody", { samples: seedSamples }));
      return;
    }
    if (attemptsLeft <= 0) {
      toast.warning(
        t("toast.generateLimitReached", { limit: aiGeneration.limit }),
      );
      return;
    }
    if (generate.isPending) return;
    const samples = draft.testcases
      .filter(
        (row) =>
          row.origin === "manual" &&
          row.visibility === "public" &&
          row.approved,
      )
      .map(({ input, expected }) => ({ input, expected }));
    try {
      const result = await generate.mutateAsync({
        problemId,
        body: draft.body,
        constraints: draft.constraints,
        samples,
      });
      const known = new Set(draft.testcases.map((row) => row.input));
      const fresh = result.testcases.filter((row) => !known.has(row.input));
      const skipped = result.testcases.length - fresh.length;
      if (fresh.length > 0) {
        setDraft((previous) => ({
          ...previous,
          testcases: [...previous.testcases, ...fresh],
        }));
        toast.success(t("toast.generated", { count: fresh.length }));
      } else {
        toast.info(t("toast.generateNothing"));
      }
      const warnings = [
        ...result.dropped.map(({ reason, count }) =>
          t(`toast.dropped${reason[0]!.toUpperCase()}${reason.slice(1)}`, {
            count,
          }),
        ),
        ...(skipped > 0
          ? [t("toast.generateDuplicates", { count: skipped })]
          : []),
        ...(result.largestCaseWarning
          ? [t("toast.generateLargestWarning")]
          : []),
      ];
      if (warnings.length > 0) toast.warning(warnings.join(". "));
    } catch (error) {
      if (
        error instanceof ApiError &&
        error.code === "AI_GENERATION_LIMIT_REACHED"
      ) {
        toast.warning(
          t("toast.generateLimitReached", { limit: aiGeneration.limit }),
        );
      } else {
        toast.error(t("toast.generateFailed"));
      }
    }
  }

  // RD amended 2026-09-28 (01-rd/req/F2_problem-bank.md F2-14): only APPROVED rows count. An
  // AI-generated draft nobody has reviewed must not help a problem reach the publish bar.
  const approved = draft.testcases.filter((row) => row.approved);
  const drafts = draft.testcases.filter((row) => !row.approved);
  const publicTestcases = approved.filter(
    (row) => row.visibility === "public",
  ).length;
  const solutionPasses =
    draft.solutionCheck.ran &&
    draft.solutionCheck.passed === draft.solutionCheck.total;

  // F2-14 precondition: at least 2 author-written Sample rows AND the reference solution (F2-18)
  // passing them. AI adds to an existing set, it cannot bootstrap one from nothing.
  const seedSamples = draft.testcases.filter(
    (row) =>
      row.origin === "manual" && row.visibility === "public" && row.approved,
  ).length;
  const canGenerate = seedSamples >= 2 && solutionPasses;
  const attemptsLeft = Math.max(0, aiGeneration.limit - aiGeneration.used);

  // dc.html:623-628 — the publish checklist, minus the weight-total rule that went with partial
  // scoring.
  const checklist = [
    { key: "minTestcases", done: approved.length >= 8, tab: "testcases" },
    { key: "minPublic", done: publicTestcases >= 2, tab: "testcases" },
    { key: "solutionPasses", done: solutionPasses, tab: "testcases" },
    { key: "minExamples", done: draft.examples.length >= 2, tab: "examples" },
    { key: "specDeclared", done: isSpecComplete(draft.spec), tab: "spec" },
  ] satisfies { key: string; done: boolean; tab: TabKey }[];
  const blocking = checklist.filter((item) => !item.done);

  // Coverage matrix: how many approved rows claim each case class. Gaps are the point — the
  // author reads this to see which kind of case is still missing (F2-14 step 3).
  const coverage = TESTCASE_CATEGORIES.map((category: TestcaseCategory) => ({
    category,
    total: draft.testcases.filter((row) => row.category === category).length,
    approved: approved.filter((row) => row.category === category).length,
  }));

  async function approveDraft(rowId: string) {
    if (liveId) {
      try {
        const written = await realUpdateTestcase(liveId, rowId, {
          approved: true,
        });
        lock.current = written.updatedAt;
        commitTestcases((rows) =>
          rows.map((row) => (row.id === rowId ? written.testcase : row)),
        );
      } catch (error) {
        toast.error(apiMessage(error));
        return;
      }
    } else {
      setDraft((previous) => ({
        ...previous,
        solutionCheck: STALE_CHECK,
        testcases: previous.testcases.map((row) =>
          row.id === rowId ? { ...row, approved: true } : row,
        ),
      }));
    }
    toast.success(t("toast.draftApproved"));
  }

  async function rejectDraft(rowId: string) {
    await removeTestcase(rowId, "toast.draftRejected");
  }

  const testcaseColumns: DataTableColumn<Testcase>[] = [
    {
      key: "index",
      header: "#",
      width: "40px",
      render: (row) => (
        <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">
          {draft.testcases.indexOf(row) + 1}
        </span>
      ),
    },
    {
      key: "input",
      header: t("columnInput"),
      render: (row) => (
        <span className="block truncate font-mono text-[12.5px]">
          {row.largeSides?.includes("input") ? t("largeSideCell") : row.input}
        </span>
      ),
    },
    {
      key: "expected",
      header: t("columnExpected"),
      width: "220px",
      render: (row) => (
        <span className="block truncate font-mono text-[12.5px] text-[var(--color-text-muted)]">
          {row.largeSides?.includes("expected")
            ? t("largeSideCell")
            : row.expected}
        </span>
      ),
    },
    {
      key: "visibility",
      header: t("columnVisibility"),
      width: "120px",
      render: (row) => (
        <Badge variant={row.visibility === "public" ? "success" : "neutral"}>
          {t(`visibility.${row.visibility}`)}
        </Badge>
      ),
    },
    {
      key: "status",
      header: t("columnStatus"),
      width: "100px",
      render: (row) =>
        row.approved ? null : <Badge variant="warn">{t("statusDraft")}</Badge>,
    },
    {
      key: "actions",
      header: <span className="sr-only">{t("columnActions")}</span>,
      width: "130px",
      align: "right",
      render: (row) => (
        <div className="flex justify-end gap-1">
          <IconAction
            icon={Pencil}
            label={t("edit")}
            onClick={() => setTestcaseDialog(row)}
          />
          <IconAction
            icon={Trash2}
            label={t("delete")}
            tone="danger"
            onClick={() => setTestcaseToDelete(row)}
          />
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        leading={
          <IconAction icon={ArrowLeft} label={t("back")} href={basePath} />
        }
        title={draft.title}
        description={t("subtitle", {
          topic: problemTopicLabel(topicList, draft.topic),
        })}
        actions={
          <>
            {/* BD Q6/Q13: opens the last SAVED data in a new tab; a problem never saved has none. */}
            {problemId ? (
              <IconAction
                icon={Eye}
                label={t("previewAsLearner")}
                href={`${basePath}/${problemId}/preview`}
                external
              />
            ) : (
              <IconAction icon={Eye} label={t("previewNeedsSave")} disabled />
            )}
            <Button
              variant="primary"
              size="sm"
              disabled={!dirty || saving || readOnly}
              onClick={save}
            >
              {t("save")}
            </Button>
            <Button
              variant="cta"
              size="sm"
              disabled={saving || readOnly}
              aria-disabled={blocking.length > 0 || undefined}
              onClick={publish}
            >
              {t("publish")}
            </Button>
          </>
        }
      />

      {readOnly ? (
        <div className="mb-3.5">
          <NoticeTile tone="info" title={t("readOnlyNotice")} />
        </div>
      ) : null}

      <div className="grid grid-cols-1 items-start gap-4 xl:grid-cols-[minmax(0,1fr)_300px]">
        <div className="flex min-w-0 flex-col gap-3.5">
          <Card className="px-1.5 py-1.5">
            <SegmentedTabs
              label={t("tabsLabel")}
              value={tab}
              onValueChange={setTab}
              className="border-0 bg-transparent p-0"
              options={[
                { value: "content", label: t("tab.content") },
                {
                  value: "examples",
                  label: t("tab.examples", { count: draft.examples.length }),
                },
                {
                  value: "testcases",
                  label: t("tab.testcases", { count: testcaseCount }),
                },
                { value: "spec", label: t("tab.spec") },
                { value: "ai", label: t("tab.ai") },
              ]}
            />
          </Card>

          {tab === "content" ? (
            <>
              <Card
                title={t("statementTitle")}
                description={t("statementSubtitle")}
              >
                <div className="flex flex-col gap-3">
                  <TextField
                    label={t("titleLabel")}
                    value={draft.title}
                    onChange={(event) => patch({ title: event.target.value })}
                  />
                  <TextArea
                    label={t("bodyLabel")}
                    rows={12}
                    value={draft.body}
                    onChange={(event) => patch({ body: event.target.value })}
                    className="font-mono text-[12.5px] leading-relaxed"
                  />
                </div>
              </Card>

              <Card title={t("limitsTitle")} description={t("limitsSubtitle")}>
                <div className="mb-4 grid gap-3 [grid-template-columns:repeat(auto-fit,minmax(150px,1fr))]">
                  {LIMIT_KEYS.map((key) => (
                    <TextField
                      key={key}
                      label={t(`limit.${key}`)}
                      type="number"
                      min={1}
                      value={draft.limits[key]}
                      onChange={(event) =>
                        patch({
                          limits: {
                            ...draft.limits,
                            [key]: Number(event.target.value),
                          },
                        })
                      }
                      className="font-mono"
                    />
                  ))}
                  <TextField
                    label={t("limit.maxSubmissionsPerHour")}
                    type="number"
                    min={1}
                    placeholder={t("limit.systemDefault")}
                    value={draft.limits.maxSubmissionsPerHour ?? ""}
                    onChange={(event) =>
                      patch({
                        limits: {
                          ...draft.limits,
                          maxSubmissionsPerHour:
                            event.target.value === ""
                              ? null
                              : Number(event.target.value),
                        },
                      })
                    }
                    className="font-mono"
                  />
                </div>
                <TextArea
                  label={t("constraintsLabel")}
                  rows={4}
                  value={draft.constraints}
                  onChange={(event) =>
                    patch({ constraints: event.target.value })
                  }
                  className="font-mono text-[12.5px]"
                />
              </Card>

              <Card
                title={t("solutionTitle")}
                description={t("solutionSubtitle")}
              >
                {/* BD SHR0202 NO9: the reference solution's language, one of the three judged ones. */}
                <SelectField
                  label={t("solutionLanguageLabel")}
                  value={draft.solutionLanguage}
                  onChange={(event) =>
                    patch({
                      solutionLanguage: event.target.value,
                      solutionCheck: STALE_CHECK,
                    })
                  }
                  options={SOLUTION_LANGUAGES.map((language) => ({
                    value: language,
                    label: language,
                  }))}
                  wrapperClassName="mb-3 max-w-[220px]"
                />
                <TextArea
                  label={t("solutionLabel", {
                    language: draft.solutionLanguage,
                  })}
                  rows={14}
                  value={draft.solution}
                  onChange={(event) =>
                    patch({
                      solution: event.target.value,
                      solutionCheck: STALE_CHECK,
                    })
                  }
                  className="font-mono text-[12.5px] leading-relaxed"
                />
              </Card>
            </>
          ) : null}

          {tab === "examples" ? (
            <Card
              title={t("examplesTitle")}
              description={t("examplesSubtitle")}
              action={
                <Button
                  variant="primary"
                  size="sm"
                  onClick={() => setExampleDialog("new")}
                >
                  {t("addExample")}
                </Button>
              }
            >
              {draft.examples.length === 0 ? (
                <p className="text-[13px] text-[var(--color-text-muted)]">
                  {t("emptyExamples")}
                </p>
              ) : null}
              <div className="flex flex-col gap-2.5">
                {draft.examples.map((example, index) => (
                  <div
                    key={example.id}
                    className="glass-surface rounded-2xl border border-[var(--color-border)] px-3.5 py-3"
                  >
                    <div className="mb-2 flex items-center justify-between gap-2">
                      <p className="text-[11px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                        {t("exampleIndex", { index: index + 1 })}
                      </p>
                      <div className="flex gap-1">
                        <IconAction
                          icon={Pencil}
                          label={t("edit")}
                          onClick={() => setExampleDialog(example)}
                        />
                        <IconAction
                          icon={Trash2}
                          label={t("delete")}
                          tone="danger"
                          onClick={() => requestDeleteExample(example)}
                        />
                      </div>
                    </div>
                    <dl className="flex flex-col gap-1.5 text-[12.5px]">
                      <div className="flex gap-2">
                        <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">
                          {t("exampleInput")}
                        </dt>
                        <dd className="min-w-0 font-mono">{example.input}</dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">
                          {t("exampleOutput")}
                        </dt>
                        <dd className="min-w-0 font-mono">{example.output}</dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">
                          {t("exampleExplanation")}
                        </dt>
                        <dd className="min-w-0 text-pretty text-[var(--color-text-muted)]">
                          {example.explanation}
                        </dd>
                      </div>
                    </dl>
                  </div>
                ))}
              </div>
            </Card>
          ) : null}

          {tab === "testcases" ? (
            <Card
              title={t("testcasesTitle")}
              description={t("testcasesSubtitle", {
                total: draft.testcases.length,
                publicCount: publicTestcases,
              })}
              className="min-w-0"
              action={
                <div className="flex gap-2">
                  <Button
                    variant="ghost"
                    size="sm"
                    className="border border-[var(--color-border)]"
                    aria-disabled={
                      approved.length === 0 ||
                      !draft.solution.trim() ||
                      undefined
                    }
                    aria-busy={running || undefined}
                    disabled={running}
                    onClick={runSolution}
                  >
                    {running ? t("running") : t("runSolution")}
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    className="border border-[var(--color-border)]"
                    disabled={bulkNeedsSave || readOnly}
                    title={bulkNeedsSave ? t("bulk.saveFirst") : undefined}
                    onClick={() => setBulkOpen(true)}
                  >
                    {t("testcase.btnBulkUpload")}
                  </Button>
                  <Button
                    variant="primary"
                    size="sm"
                    onClick={() => setTestcaseDialog("new")}
                  >
                    {t("addTestcase")}
                  </Button>
                </div>
              }
            >
              {server?.willCopy && !id ? (
                <div className="mb-3">
                  <NoticeTile
                    tone="info"
                    title={t(
                      server.willCopy.hasSpec
                        ? "copyWillCopyWithSpec"
                        : "copyWillCopy",
                      { count: server.willCopy.testcaseCount },
                    )}
                  />
                </div>
              ) : null}
              {testcasesHidden ? (
                <div className="mb-3">
                  <NoticeTile
                    tone="info"
                    title={t("testcasesHiddenNotice", {
                      total: server.testcaseSummary.total,
                    })}
                  />
                </div>
              ) : null}
              {/* F2-18: the run result is a toast; only the stale state stays on the page. */}
              <div
                className={draft.solutionCheck.ran ? undefined : "mb-3"}
              ></div>
              {/* No "Điểm" column and no weight block — PROTOTYPE_DEBT 2.6. The partial score that
                  survived (F4-13) is an automatic pass ratio, not an author-declared weight. */}
              <DataTable
                caption={t("testcasesTitle")}
                columns={testcaseColumns}
                rows={draft.testcases}
                rowKey={(row) => row.id}
                emptyMessage={t("emptyTestcases")}
                minWidth={720}
              />
            </Card>
          ) : null}

          {tab === "spec" ? (
            <SpecEditor
              spec={draft.spec}
              onChange={(spec) => patch({ spec })}
            />
          ) : null}

          {tab === "ai" ? (
            <div className="flex flex-col gap-3.5">
              <Card
                title={t("generateTitle")}
                description={t("generateSubtitle")}
              >
                <div className="mt-3.5 flex flex-col gap-3">
                  {/* No inline notice (owner, 2026-10-03): looks disabled while locked but stays
                      clickable, and the click says why with a toast. */}
                  <div className="flex flex-wrap items-center gap-3">
                    <Button
                      variant="primary"
                      aria-disabled={
                        !canGenerate || attemptsLeft <= 0 || undefined
                      }
                      aria-busy={generate.isPending || undefined}
                      disabled={generate.isPending}
                      onClick={generateTestcases}
                    >
                      {generate.isPending
                        ? t("generating")
                        : t("generateAction")}
                    </Button>
                    <span className="text-[12.5px] text-[var(--color-text-muted)]">
                      {t("generateQuota", {
                        left: attemptsLeft,
                        limit: aiGeneration.limit,
                      })}
                    </span>
                  </div>
                  {generate.isPending ? (
                    <NoticeTile tone="info" title={t("generating")}>
                      {t("generatingBody")}
                    </NoticeTile>
                  ) : null}
                </div>
              </Card>

              <Card
                title={t("coverageTitle")}
                description={t("coverageSubtitle")}
              >
                <div className="flex flex-wrap gap-2">
                  {coverage.map((row) => (
                    <Badge
                      key={row.category}
                      variant={row.approved > 0 ? "success" : "neutral"}
                    >
                      {t(`category.${row.category}`)}: {row.approved}/
                      {row.total}
                    </Badge>
                  ))}
                </div>
              </Card>

              <Card
                title={t("draftsTitle")}
                description={t("draftsSubtitle", { count: drafts.length })}
              >
                {drafts.length === 0 ? (
                  <p className="text-[13px] text-[var(--text-muted)]">
                    {t("draftsEmpty")}
                  </p>
                ) : (
                  <ul className="flex flex-col gap-2.5">
                    {drafts.map((row) => (
                      <li
                        key={row.id}
                        className="flex flex-wrap items-center gap-2.5 rounded-lg border border-[var(--border)] p-2.5"
                      >
                        <Badge variant="neutral">
                          {row.category
                            ? t(`category.${row.category}`)
                            : t("category.typical")}
                        </Badge>
                        <code className="flex-1 text-[12.5px]">
                          {row.input}
                        </code>
                        <IconAction
                          icon={Check}
                          label={t("approveAction")}
                          onClick={() => approveDraft(row.id)}
                        />
                        <IconAction
                          icon={X}
                          label={t("rejectAction")}
                          tone="danger"
                          onClick={() => rejectDraft(row.id)}
                        />
                      </li>
                    ))}
                  </ul>
                )}
              </Card>

              <Card title={t("aiTitle")} description={t("aiSubtitle")}>
                <TextArea
                  label={t("aiBriefLabel")}
                  rows={5}
                  value={draft.aiBrief}
                  onChange={(event) => patch({ aiBrief: event.target.value })}
                  className="font-mono text-[12.5px]"
                />
                <div className="mt-3.5 flex flex-col gap-2.5">
                  {AI_GUARD_KEYS.map((key: AiGuardKey) => (
                    <SettingRow
                      key={key}
                      label={t(`aiGuard.${key}.label`)}
                      description={t(`aiGuard.${key}.meta`)}
                    >
                      <Toggle
                        checked={draft.aiGuards[key]}
                        onCheckedChange={(value) =>
                          patch({
                            aiGuards: { ...draft.aiGuards, [key]: value },
                          })
                        }
                        label={t(`aiGuard.${key}.label`)}
                      />
                    </SettingRow>
                  ))}
                </div>
              </Card>
            </div>
          ) : null}
        </div>

        <div className="flex flex-col gap-3.5 xl:sticky xl:top-[92px]">
          <Card title={t("propertiesTitle")}>
            <div className="flex flex-col gap-3.5">
              <SelectField
                label={t("topicLabel")}
                value={draft.topic}
                onChange={(event) => patch({ topic: event.target.value })}
                options={topicList.map((topic) => ({
                  value: topic.key,
                  label: topic.label,
                }))}
              />
              <div>
                <p className="mb-1.5 text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
                  {t("difficultyLabel")}
                </p>
                <SelectField
                  label={t("difficultyLabel")}
                  hideLabel
                  value={draft.difficulty}
                  onChange={(event) =>
                    patch({ difficulty: event.target.value })
                  }
                  options={[
                    // A level an admin deleted while this form was open stays visible, but cannot be re-picked.
                    ...(levelList.some(
                      (level) => level.key === draft.difficulty,
                    )
                      ? []
                      : [
                          {
                            value: draft.difficulty,
                            label: draft.difficulty,
                            disabled: true,
                          },
                        ]),
                    ...levelList.map((level) => ({
                      value: level.key,
                      label: level.label,
                    })),
                  ]}
                />
              </div>
              <div>
                <p className="mb-1.5 text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
                  {t("statusLabel")}
                </p>
                {/* Two states only — DEC-2026-0830-problem-lifecycle-two-states. */}
                <SegmentedTabs
                  label={t("statusLabel")}
                  value={draft.status}
                  onValueChange={(value) => patch({ status: value })}
                  className="w-full"
                  options={[
                    { value: "draft" as const, label: t("status.draft") },
                    {
                      value: "published" as const,
                      label: t("status.published"),
                    },
                  ]}
                />
              </div>
              {/* BD SHR0202 props NO4: free tags, comma separated in the prototype. */}
              <TextField
                label={t("tagsLabel")}
                placeholder={t("tagsPlaceholder")}
                value={draft.tags.join(", ")}
                onChange={(event) =>
                  patch({
                    tags: event.target.value
                      .split(",")
                      .map((tag) => tag.trim())
                      .filter(
                        (tag, index, all) =>
                          // Keep the trailing empty entry while typing so "a, " can become "a, b".
                          tag !== "" || index === all.length - 1,
                      ),
                  })
                }
              />
            </div>
          </Card>

          {/* BD SHR0202 props NO6, 7, 10. NO8-9 have no column (Q5), so they are not shown. */}
          <Card title={t("metricsTitle")}>
            <dl className="flex flex-col gap-2 text-[13px]">
              <div className="flex justify-between gap-2">
                <dt className="text-[var(--color-text-muted)]">
                  {t("metrics.submissions")}
                </dt>
                <dd className="font-mono">
                  {stats ? stats.submissionCount.toLocaleString("vi-VN") : "-"}
                </dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-[var(--color-text-muted)]">
                  {t("metrics.acRate")}
                </dt>
                <dd className="font-mono">
                  {stats?.acRate == null ? "-" : `${stats.acRate}%`}
                </dd>
              </div>
              <div className="flex justify-between gap-2">
                <dt className="text-[var(--color-text-muted)]">
                  {t("metrics.lastEdited")}
                </dt>
                <dd>
                  {savedAt
                    ? new Date(savedAt).toLocaleString("vi-VN")
                    : t("metrics.neverSaved")}
                </dd>
              </div>
            </dl>
          </Card>

          <Card title={t("checklistTitle")}>
            <ul className="flex flex-col gap-2">
              {checklist.map((item) => (
                <li
                  key={item.key}
                  className="flex items-start gap-2 text-[12.5px]"
                >
                  <Badge
                    variant={item.done ? "success" : "warn"}
                    className="shrink-0"
                  >
                    {item.done ? t("checkDone") : t("checkPending")}
                  </Badge>
                  {item.done ? (
                    <span className="text-[var(--color-text-muted)]">
                      {t(`check.${item.key}`)}
                    </span>
                  ) : (
                    <button
                      type="button"
                      className="cursor-pointer text-left text-[var(--color-text)] underline decoration-dotted underline-offset-2"
                      onClick={() => setTab(item.tab)}
                    >
                      {t(`check.${item.key}`)}
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </div>

      <UnsavedChangesDialog
        open={leaveGuard.pending}
        title={t("unsaved.title")}
        saveAndLeaveLabel={t("unsaved.saveAndLeave")}
        leaveLabel={t("unsaved.leave")}
        stayLabel={t("unsaved.stay")}
        onSaveAndLeave={saveAndLeave}
        onLeave={leaveGuard.leave}
        onStay={leaveGuard.stay}
        pending={saving}
      >
        {t("unsaved.body")}
      </UnsavedChangesDialog>
      {testcaseDialog ? (
        <TestcaseDialog
          testcase={testcaseDialog === "new" ? undefined : testcaseDialog}
          onClose={() => setTestcaseDialog(null)}
          onSave={saveTestcase}
        />
      ) : null}
      {bulkOpen ? (
        <BulkUploadDialog
          existingCount={testcaseCount}
          onClose={() => setBulkOpen(false)}
          onUpload={bulkUpload}
        />
      ) : null}
      {exampleDialog ? (
        <ExampleDialog
          example={exampleDialog === "new" ? undefined : exampleDialog}
          onClose={() => setExampleDialog(null)}
          onSave={saveExample}
        />
      ) : null}
      <ConfirmDialog
        open={testcaseToDelete !== null}
        onClose={() => setTestcaseToDelete(null)}
        onConfirm={() =>
          testcaseToDelete && deleteTestcase(testcaseToDelete.id)
        }
        title={t("confirmDeleteTestcaseTitle")}
        confirmLabel={t("delete")}
        cancelLabel={t("dialog.cancel")}
        destructive
      >
        {t("confirmDeleteTestcaseBody")}
      </ConfirmDialog>
      <ConfirmDialog
        open={exampleToDelete !== null}
        onClose={() => setExampleToDelete(null)}
        onConfirm={() => exampleToDelete && deleteExample(exampleToDelete.id)}
        title={t("confirmDeleteExampleTitle")}
        confirmLabel={t("delete")}
        cancelLabel={t("dialog.cancel")}
        destructive
      >
        {t("confirmDeleteExampleBody")}
      </ConfirmDialog>
    </div>
  );
}
