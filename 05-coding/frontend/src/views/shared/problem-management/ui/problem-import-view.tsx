// SHR0204 "Thêm nhiều bài" (F2-19, DEC-2026-1125-bulk-problem-import): an Excel file or pasted cells become an
// editable table, then drafts, reference runs, publishing and (instructor area) a class assignment. Only
// existing endpoints are used; the order and failure rules of the run live in model/import-run.ts.
//
// Shared A2 and A3 screen, mounted at /instructor/problems/import and /admin/problems/import.
"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { ArrowLeft, ExternalLink, RotateCcw, Trash2 } from "lucide-react";
import {
  assignInBatches,
  useAssignProblems,
} from "@/entities/class-assignment";
import {
  IMPORT_MAX_ROWS,
  IMPORT_MIN_CASES,
  blankImportRow,
  checkImportRow,
  foldText,
  gridToRows,
  loadImportDefaults,
  repeatedTitles,
  resolveLevel,
  runImportReference,
  saveImportRow,
  toImportDraft,
  useEnabledLanguages,
  useProblemLevels,
  useProblemTopics,
  type ImportCatalogue,
  type ImportRow,
} from "@/entities/problem";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import {
  cn,
  parseTsv,
  readXlsx,
  saveBlob,
  useDebounced,
  useUnsavedChangesGuard,
  XLSX_MAX_BYTES,
  XlsxError,
} from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  FilePickerButton,
  IconAction,
  NoticeTile,
  PageContainer,
  PageHeader,
  FilterMenu,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";
import { useBulkPublish, useExistingTitles } from "../api";
import { publishRows, runRows, type RowRun } from "../model/import-run";
import { ImportPublishDialog } from "./import-publish-dialog";
import { ImportRowEditor } from "./import-row-editor";

const TEMPLATE = "/templates/mau-nhap-bai-toan.xlsx";
const TEMPLATE_NAME = "mau-nhap-bai-toan.xlsx";
const LANGUAGE_NAME = { java: "Java", cpp: "C++", python: "Python" } as const;
const IDLE: RowRun = { step: "idle" };
/** Outcomes whose reason is told in a toast as soon as they happen. */
const TOASTED: RowRun["step"][] = [
  "fail",
  "compileError",
  "error",
  "unknown",
  "publishError",
];

/** A paste or a drop inside one of these belongs to that control, not to the table. */
const OWN_PASTE =
  'input, textarea, select, [contenteditable="true"], [role="dialog"]';

/** Where a row stands on the server. */
type RowState = { problemId?: string; run: RowRun };
type ClassTarget = { id: string; name: string };
type AssignLeft = { target: ClassTarget; ids: string[]; error: unknown };
type Badged = { variant: BadgeVariant; label: string; detail?: string };

const isXlsx = (file: File) => /\.xlsx$/i.test(file.name);
const sleep = (ms: number) =>
  new Promise<void>((resolve) => setTimeout(resolve, ms));

type Props = {
  /** Area root the back link returns to, e.g. "/instructor/problems". */
  basePath: string;
  /** Only the instructor area can give the new problems to a class. */
  canAssign?: boolean;
};

export function ProblemImportView({ basePath, canAssign = false }: Props) {
  const t = useT("problemImport");
  const apiMessage = useApiErrorMessage();
  const levels = useProblemLevels();
  const topics = useProblemTopics();
  const languagesQuery = useEnabledLanguages();
  const publish = useBulkPublish();
  const assign = useAssignProblems();

  const [rows, setRows] = useState<ImportRow[]>([]);
  const [states, setStates] = useState<Record<string, RowState>>({});
  const [currentId, setCurrentId] = useState<string | null>(null);
  const [notices, setNotices] = useState<string[]>([]);
  const [busy, setBusy] = useState(false);
  const [stopping, setStopping] = useState(false);
  const [dragging, setDragging] = useState(false);
  const [confirmingPublish, setConfirmingPublish] = useState(false);
  const [assignLeft, setAssignLeft] = useState<AssignLeft | null>(null);
  const counter = useRef(0);
  const stop = useRef(false);
  const alive = useRef(true);

  useEffect(() => {
    alive.current = true;
    return () => {
      // Leaving ends the run after the row in flight.
      alive.current = false;
      stop.current = true;
    };
  }, []);

  const catalogue = useMemo<ImportCatalogue>(
    () => ({
      levels,
      topics,
      languages: (languagesQuery.data ?? []).map((key) => LANGUAGE_NAME[key]),
    }),
    [levels, topics, languagesQuery.data],
  );
  const catalogueReady = levels.length > 0 && languagesQuery.data !== undefined;

  const repeated = useMemo(() => repeatedTitles(rows), [rows]);
  const checks = useMemo(
    () =>
      new Map(
        rows.map((row) => [
          row.id,
          checkImportRow(row, catalogue, repeated.has(foldText(row.title))),
        ]),
      ),
    [rows, catalogue, repeated],
  );
  const stateOf = (id: string): RowState => states[id] ?? { run: IDLE };

  // Titles already in the bank are a warning (importing one file twice is easy). Created rows are not asked.
  const newTitles = useDebounced(
    JSON.stringify(
      rows
        .filter((row) => !states[row.id]?.problemId && row.title.trim())
        .map((row) => row.title.trim()),
    ),
  );
  const existing = useExistingTitles(
    useMemo(() => JSON.parse(newTitles) as string[], [newTitles]),
  );
  // Rows just brought in are counted once the catalogue is there; before that every level looks unknown.
  const unannounced = useRef(new Set<string>());
  useEffect(() => {
    if (!catalogueReady || unannounced.current.size === 0) return;
    const broken = rows.filter(
      (row) =>
        unannounced.current.has(row.id) &&
        (checks.get(row.id)?.issues.length ?? 0) > 0,
    ).length;
    unannounced.current.clear();
    if (broken > 0) toast.warning(t("blockedOnIntake", { count: broken }));
  }, [catalogueReady, rows, checks, t]);

  // Said once per title, as a toast, and only for titles as they came from the file or the paste: a title
  // being typed is not checked on every key.
  const announced = useRef(new Set<string>());
  const arrived = useRef(new Map<string, string>());
  useEffect(() => {
    const fresh = rows
      .filter((row) => arrived.current.get(row.id) === row.title)
      .map((row) => row.title.trim())
      .filter(
        (title) =>
          existing.has(foldText(title)) &&
          !announced.current.has(foldText(title)),
      );
    if (fresh.length === 0) return;
    for (const title of fresh) announced.current.add(foldText(title));
    toast.warning(t("titlesExist", { titles: fresh.join(", ") }));
  }, [existing, rows, t]);

  const runnable = rows.filter(
    (row) =>
      checks.get(row.id)!.issues.length === 0 &&
      ["idle", "fail", "compileError", "error"].includes(
        stateOf(row.id).run.step,
      ),
  );
  const publishable = rows.flatMap((row) => {
    const { problemId, run } = stateOf(row.id);
    return problemId && (run.step === "pass" || run.step === "publishError")
      ? [{ row, problemId, total: run.total }]
      : [];
  });
  const blocked = rows.filter(
    (row) => checks.get(row.id)!.issues.length > 0,
  ).length;
  const publishedCount = rows.filter(
    (row) => stateOf(row.id).run.step === "published",
  ).length;
  const unsent = rows.some(
    (row) => !stateOf(row.id).problemId || stateOf(row.id).run.step === "idle",
  );
  const guard = useUnsavedChangesGuard(busy || unsent);

  function report(row: ImportRow, run: RowRun) {
    if (!alive.current) return;
    setStates((previous) => ({
      ...previous,
      [row.id]: { ...previous[row.id], run },
    }));
    // The reason goes to a toast; the row only shows its status.
    if (!TOASTED.includes(run.step)) return;
    const message = `${rowLabel(row)}: ${statusOf(row, run).detail ?? ""}`;
    if (run.step === "fail" || run.step === "compileError")
      toast.warning(message);
    else toast.error(message);
  }

  const rowLabel = (row: ImportRow) =>
    t("rowLabel", {
      number: rows.findIndex((item) => item.id === row.id) + 1,
      title: row.title.trim() || t("untitled"),
    });

  /** Clicking a title opens the row and, when something is wrong or missing, says why. */
  function openRow(row: ImportRow) {
    setCurrentId(row.id);
    const status = statusOf(row);
    if (
      status.detail &&
      (status.variant === "negative" || status.variant === "warn")
    )
      toast.warning(`${rowLabel(row)}: ${status.detail}`);
  }

  function change(id: string, patch: Partial<ImportRow>) {
    setRows((previous) =>
      previous.map((row) => (row.id === id ? { ...row, ...patch } : row)),
    );
    // An edited row has to be sent again; one with an unknown outcome still needs the author's word first.
    setStates((previous) =>
      !previous[id] || previous[id].run.step === "unknown"
        ? previous
        : { ...previous, [id]: { ...previous[id], run: IDLE } },
    );
  }

  const nextId = () => `row-${(counter.current += 1)}`;

  // ---- bringing rows in -------------------------------------------------------------------------

  function addGrid(grid: string[][], warnings: string[], needsHeader: boolean) {
    const room = IMPORT_MAX_ROWS - rows.length;
    const result = gridToRows(grid, room, nextId);
    if (needsHeader && !result.hasHeader) {
      toast.error(t("error.noHeader"));
      return;
    }
    const next = [...warnings];
    if (!result.hasHeader) next.push(t("notice.noHeader"));
    if (result.unknownColumns.length > 0)
      next.push(
        t("notice.unknownColumns", {
          names: result.unknownColumns.join(", "),
        }),
      );
    if (result.leftOver > 0)
      next.push(
        t("notice.leftOver", {
          max: IMPORT_MAX_ROWS,
          taken: result.rows.length,
          left: result.leftOver,
        }),
      );
    setNotices(next);
    if (result.rows.length === 0) {
      if (result.leftOver === 0) toast.warning(t("error.noRows"));
      return;
    }
    setRows((previous) => [...previous, ...result.rows]);
    setCurrentId((previous) => previous ?? result.rows[0]!.id);
    toast.success(t("received", { count: result.rows.length }));
    for (const row of result.rows) {
      unannounced.current.add(row.id);
      arrived.current.set(row.id, row.title);
    }
  }

  async function addFile(file: File) {
    if (busy) return;
    if (!isXlsx(file)) {
      toast.error(t("error.file.extension"));
      return;
    }
    if (file.size > XLSX_MAX_BYTES) {
      toast.error(t("error.file.tooLarge"));
      return;
    }
    try {
      const sheet = await readXlsx(new Uint8Array(await file.arrayBuffer()));
      const warnings: string[] = [];
      const some = (cells: string[]) => cells.slice(0, 5).join(", ");
      if (sheet.dateCells.length > 0)
        warnings.push(
          t("notice.dates", {
            count: sheet.dateCells.length,
            cells: some(sheet.dateCells),
          }),
        );
      if (sheet.errorCells.length > 0)
        warnings.push(t("notice.formulas", { cells: some(sheet.errorCells) }));
      if (sheet.sheetCount > 1)
        warnings.push(t("notice.sheets", { name: sheet.sheetName }));
      addGrid(sheet.rows, warnings, true);
    } catch (error) {
      const code = error instanceof XlsxError ? error.code : "corrupt";
      toast.error(t(`error.file.${code}`));
    }
  }

  function addBlank() {
    const row = blankImportRow(nextId());
    setRows((previous) => [...previous, row]);
    setCurrentId(row.id);
  }

  function removeRow(row: ImportRow) {
    if (stateOf(row.id).problemId && stateOf(row.id).run.step !== "published")
      toast.info(t("removedKeepsDraft"));
    setRows((previous) => previous.filter((item) => item.id !== row.id));
    if (currentId === row.id) setCurrentId(null);
  }

  async function downloadTemplate() {
    try {
      const response = await fetch(TEMPLATE);
      if (!response.ok) throw new Error(String(response.status));
      saveBlob(await response.blob(), TEMPLATE_NAME);
      toast.success(t("templateSaved"));
    } catch {
      toast.error(t("error.template"));
    }
  }

  // The listeners live on the document, so a file can be dropped or cells pasted anywhere on the screen.
  // A ref keeps them current without re-binding on every keystroke.
  const intake = useRef({ busy, addFile, addGrid });
  useEffect(() => {
    intake.current = { busy, addFile, addGrid };
  });
  useEffect(() => {
    const hasFiles = (event: DragEvent) =>
      Array.from(event.dataTransfer?.types ?? []).includes("Files");
    const onPaste = (event: ClipboardEvent) => {
      if ((event.target as Element | null)?.closest?.(OWN_PASTE)) return;
      if (document.querySelector('[role="dialog"]')) return;
      const file = Array.from(event.clipboardData?.files ?? []).find(isXlsx);
      const text = event.clipboardData?.getData("text/plain") ?? "";
      if (!file && !text.trim()) return;
      event.preventDefault();
      if (intake.current.busy) return;
      if (file) void intake.current.addFile(file);
      else intake.current.addGrid(parseTsv(text), [], false);
    };
    const onDragOver = (event: DragEvent) => {
      if (!hasFiles(event)) return;
      event.preventDefault();
      setDragging(true);
    };
    const onDragLeave = (event: DragEvent) => {
      if (!event.relatedTarget) setDragging(false);
    };
    const onDrop = (event: DragEvent) => {
      if (!hasFiles(event)) return;
      // Always: without it the browser would leave the screen to open the file.
      event.preventDefault();
      setDragging(false);
      const file = event.dataTransfer?.files[0];
      if (file) void intake.current.addFile(file);
    };
    document.addEventListener("paste", onPaste);
    document.addEventListener("dragover", onDragOver);
    document.addEventListener("dragleave", onDragLeave);
    document.addEventListener("drop", onDrop);
    return () => {
      document.removeEventListener("paste", onPaste);
      document.removeEventListener("dragover", onDragOver);
      document.removeEventListener("dragleave", onDragLeave);
      document.removeEventListener("drop", onDrop);
    };
  }, []);

  // ---- the run ------------------------------------------------------------------------------------

  async function whileBusy(work: () => Promise<void>) {
    if (busy) return;
    stop.current = false;
    setBusy(true);
    try {
      await work();
    } finally {
      if (alive.current) {
        setBusy(false);
        setStopping(false);
      }
    }
  }

  const start = () =>
    whileBusy(async () => {
      let defaults;
      try {
        defaults = await loadImportDefaults();
      } catch (error) {
        toast.error(apiMessage(error));
        return;
      }
      const tally = { pass: 0, bad: 0, draft: 0 };
      await runRows(
        runnable.map((row) => ({ row, problemId: stateOf(row.id).problemId })),
        {
          save: (row, onCreated) =>
            saveImportRow({
              defaults,
              problemId: stateOf(row.id).problemId,
              build: (base) => toImportDraft(row, base, catalogue),
              onCreated: (problemId) => {
                onCreated(problemId);
                setStates((previous) => ({
                  ...previous,
                  [row.id]: { run: previous[row.id]?.run ?? IDLE, problemId },
                }));
              },
            }),
          shouldRun: (row) => checks.get(row.id)!.gaps.length === 0,
          runReference: (row, problemId) => {
            const draft = toImportDraft(row, defaults.draft, catalogue);
            return runImportReference(
              problemId,
              { language: draft.solutionLanguage, sourceCode: row.solution },
              draft.testcases.length,
            );
          },
          report: (row, run) => {
            if (run.step === "pass") tally.pass += 1;
            else if (run.step === "draft") tally.draft += 1;
            else if (TOASTED.includes(run.step)) tally.bad += 1;
            report(row, run);
          },
          stopped: () => stop.current,
          sleep,
        },
      );
      if (alive.current && tally.pass + tally.bad + tally.draft > 0) {
        if (tally.bad > 0) toast.info(t("runDone", tally));
        else toast.success(t("runDone", tally));
      }
    });

  async function giveToClass(target: ClassTarget, ids: string[]) {
    const result = await assignInBatches(ids, (problemIds) =>
      assign.mutateAsync({ classId: target.id, problemIds }),
    );
    const done = new Set([...result.succeeded, ...result.skipped]);
    if (done.size > 0)
      toast.success(
        t("assigned", { count: done.size, className: target.name }),
      );
    const left = ids.filter((id) => !done.has(id));
    const error =
      result.error ??
      (result.failed[0] ? new ApiError(result.failed[0].code, 422) : undefined);
    if (!alive.current) return;
    setAssignLeft(left.length > 0 ? { target, ids: left, error } : null);
  }

  function publishAll(target: ClassTarget | null) {
    setConfirmingPublish(false);
    void whileBusy(async () => {
      const ids = await publishRows(publishable, {
        publish: (problemId) => publish.mutateAsync([problemId]),
        report,
        stopped: () => stop.current,
      });
      if (ids.length === 0) return;
      toast.success(t("published", { count: ids.length }));
      if (target) await giveToClass(target, ids);
    });
  }

  // ---- what a row says ------------------------------------------------------------------------------

  function statusOf(row: ImportRow, override?: RowRun): Badged {
    const check = checks.get(row.id) ?? checkImportRow(row, catalogue, false);
    const { problemId } = stateOf(row.id);
    const run = override ?? stateOf(row.id).run;
    const gaps = () =>
      t("status.gaps", {
        items: check.gaps
          .map((gap) =>
            t(`gap.${gap}`, { count: IMPORT_MIN_CASES - check.caseCount }),
          )
          .join(", "),
      });
    switch (run.step) {
      case "saving":
        return { variant: "blue", label: t("status.saving") };
      case "running":
        return { variant: "blue", label: t("status.running") };
      case "waiting":
        return {
          variant: "blue",
          label: t("status.waiting", { seconds: run.seconds }),
        };
      case "publishing":
        return { variant: "blue", label: t("status.publishing") };
      case "published":
        return { variant: "success", label: t("status.published") };
      case "publishError":
        return {
          variant: "negative",
          label: t("status.publishError"),
          detail: apiMessage(run.error),
        };
      case "unknown":
        return {
          variant: "negative",
          label: t("status.unknown"),
          detail: t("status.unknownDetail"),
        };
      default:
    }
    if (check.issues.length > 0)
      return {
        variant: "negative",
        label: t("status.blocked"),
        detail: check.issues
          .map((issue) =>
            t(`issue.${issue.code}`, { value: issue.value ?? "" }),
          )
          .join(" "),
      };
    switch (run.step) {
      case "pass":
        return {
          variant: "success",
          label: t("status.pass", { total: run.total }),
        };
      case "fail":
        return {
          variant: "negative",
          label: t("status.fail", { position: run.position }),
          detail: t("status.failDetail", {
            reason: t(`failure.${run.failure}`),
          }),
        };
      case "compileError":
        return {
          variant: "negative",
          label: t("status.compileError"),
          detail: t("status.compileErrorDetail"),
        };
      case "error":
        return {
          variant: "warn",
          label: problemId ? t("status.errorStored") : t("status.error"),
          detail: apiMessage(run.error),
        };
      case "draft":
        return { variant: "warn", label: t("status.draft"), detail: gaps() };
      default:
    }
    if (check.gaps.length > 0)
      return { variant: "warn", label: t("status.incomplete"), detail: gaps() };
    return {
      variant: "success",
      label: problemId ? t("status.changed") : t("status.ready"),
    };
  }

  const columns: DataTableColumn<ImportRow>[] = [
    {
      key: "number",
      header: t("column.number"),
      width: "36px",
      align: "right",
      render: (row) => rows.indexOf(row) + 1,
    },
    {
      key: "title",
      header: t("column.title"),
      width: "32%",
      render: (row) => (
        <button
          type="button"
          aria-current={row.id === currentId || undefined}
          onClick={() => openRow(row)}
          className={cn(
            "rounded text-left font-medium hover:underline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[var(--color-primary)]",
            row.id === currentId && "font-bold text-[var(--color-primary)]",
          )}
        >
          {row.title.trim() || t("untitled")}
        </button>
      ),
    },
    {
      key: "level",
      header: t("column.level"),
      width: "150px",
      render: (row) => {
        const level = resolveLevel(row.level, levels);
        return (
          <FilterMenu
            compact
            label={t("levelOf", { number: rows.indexOf(row) + 1 })}
            className="w-full [&>button]:w-full [&>button]:justify-between [&>button]:whitespace-nowrap"
            value={level?.label ?? row.level}
            disabled={isLocked(row)}
            onValueChange={(value) => change(row.id, { level: value })}
            options={[
              // Shown only while the cell holds no known level, so it can never be chosen again.
              ...(level
                ? []
                : [{ value: row.level, label: row.level || t("choose") }]),
              ...levels.map((item) => ({
                value: item.label,
                label: item.label,
              })),
            ]}
          />
        );
      },
    },
    {
      key: "cases",
      header: t("column.cases"),
      width: "84px",
      align: "right",
      render: (row) => (
        <span className="font-mono">
          {checks.get(row.id)!.caseCount}/{IMPORT_MIN_CASES}
        </span>
      ),
    },
    {
      key: "status",
      header: t("column.status"),
      render: (row) => {
        const status = statusOf(row);
        return (
          <Badge variant={status.variant} title={status.detail}>
            {status.label}
          </Badge>
        );
      },
    },
    {
      key: "actions",
      header: <span className="sr-only">{t("column.actions")}</span>,
      width: "124px",
      align: "right",
      render: (row) => {
        const { run, problemId } = stateOf(row.id);
        return (
          <div className="flex justify-end gap-1">
            {run.step === "unknown" ? (
              <>
                <IconAction
                  icon={RotateCcw}
                  label={t("createAgain")}
                  disabled={busy}
                  onClick={() => {
                    if (!busy) report(row, IDLE);
                  }}
                />
                <IconAction
                  icon={ExternalLink}
                  label={t("openList")}
                  href={basePath}
                  external
                />
              </>
            ) : problemId ? (
              <IconAction
                icon={ExternalLink}
                label={t("openEditor")}
                ariaLabel={t("openEditorOf", { title: row.title.trim() })}
                href={`${basePath}/${problemId}/edit`}
                external
              />
            ) : null}
            <IconAction
              icon={Trash2}
              tone="danger"
              label={t("remove")}
              ariaLabel={t("removeRow", { number: rows.indexOf(row) + 1 })}
              disabled={busy}
              onClick={() => {
                if (!busy) removeRow(row);
              }}
            />
          </div>
        );
      },
    },
  ];

  function isLocked(row: ImportRow) {
    return busy || stateOf(row.id).run.step === "published";
  }

  const current = rows.find((row) => row.id === currentId);
  const empty = rows.length === 0;
  const step = publishable.length > 0 || publishedCount > 0 ? 2 : busy ? 1 : 0;
  const outlined = "border border-[var(--color-border)]";

  const tools = (
    <>
      <FilePickerButton
        accept=".xlsx"
        inputTestId="problem-import-file"
        variant={empty ? "cta" : "ghost"}
        size="sm"
        className={empty ? undefined : outlined}
        disabled={busy}
        onFiles={([file]) => void addFile(file!)}
      >
        {t("chooseFile")}
      </FilePickerButton>
      <Button
        variant="ghost"
        size="sm"
        className={outlined}
        onClick={() => void downloadTemplate()}
      >
        {t("downloadTemplate")}
      </Button>
      <Button
        variant="ghost"
        size="sm"
        className={outlined}
        disabled={busy || rows.length >= IMPORT_MAX_ROWS}
        onClick={addBlank}
      >
        {t("addRow")}
      </Button>
    </>
  );

  const zone = cn(
    "rounded-2xl border-2 border-dashed border-[var(--color-border)] transition-colors",
    dragging && "border-[var(--color-primary)] bg-[var(--color-surface-hover)]",
  );

  return (
    <PageContainer variant="content" className="@container">
      <PageHeader
        leading={
          <IconAction icon={ArrowLeft} label={t("back")} href={basePath} />
        }
        title={t("title")}
        description={
          empty
            ? t("subtitleEmpty")
            : [
                t("count.rows", { count: rows.length }),
                blocked > 0 ? t("count.blocked", { count: blocked }) : "",
                publishedCount > 0
                  ? t("count.published", { count: publishedCount })
                  : "",
              ]
                .filter(Boolean)
                .join(", ")
        }
      />

      <ol className="mb-3 flex flex-wrap gap-x-5 gap-y-1 text-[13px] text-[var(--color-text-muted)]">
        {(["prepare", "create", "publish"] as const).map((key, index) => (
          <li
            key={key}
            aria-current={index === step ? "step" : undefined}
            className={
              index === step
                ? "font-semibold text-[var(--color-text)]"
                : undefined
            }
          >
            {index + 1}. {t(`step.${key}`)}
          </li>
        ))}
      </ol>

      {/* Left-aligned, below the header: toasts stack at the top right and would cover these buttons. */}
      {busy || runnable.length > 0 || publishable.length > 0 ? (
        <div className="mb-3 flex flex-wrap items-center gap-2">
          {busy ? (
            <Button
              variant="ghost"
              size="sm"
              className={outlined}
              disabled={stopping}
              onClick={() => {
                stop.current = true;
                setStopping(true);
              }}
            >
              {stopping ? t("stopping") : t("stop")}
            </Button>
          ) : null}
          {runnable.length > 0 ? (
            <Button
              variant={publishable.length > 0 ? "ghost" : "cta"}
              size="sm"
              className={publishable.length > 0 ? outlined : undefined}
              disabled={busy || !catalogueReady}
              onClick={() => void start()}
            >
              {t("run", { count: runnable.length })}
            </Button>
          ) : null}
          {publishable.length > 0 ? (
            <Button
              variant="cta"
              size="sm"
              disabled={busy}
              onClick={() => setConfirmingPublish(true)}
            >
              {t("publishCount", { count: publishable.length })}
            </Button>
          ) : null}
        </div>
      ) : null}

      {languagesQuery.isError ? (
        <NoticeTile
          tone="warn"
          className="mb-3"
          title={t("error.languages")}
          action={
            <Button
              variant="ghost"
              size="sm"
              className={outlined}
              onClick={() => void languagesQuery.refetch()}
            >
              {t("retry")}
            </Button>
          }
        />
      ) : null}
      {notices.map((notice) => (
        <NoticeTile key={notice} tone="warn" className="mb-3" title={notice} />
      ))}
      {assignLeft ? (
        <NoticeTile
          tone="negative"
          className="mb-3"
          title={t("assignFailed", {
            count: assignLeft.ids.length,
            className: assignLeft.target.name,
          })}
          action={
            <Button
              variant="ghost"
              size="sm"
              className={outlined}
              disabled={busy}
              onClick={() =>
                void whileBusy(() =>
                  giveToClass(assignLeft.target, assignLeft.ids),
                )
              }
            >
              {t("assignAgain")}
            </Button>
          }
        >
          {apiMessage(assignLeft.error)}
        </NoticeTile>
      ) : null}

      {empty ? (
        <section
          className={cn(
            zone,
            "grid justify-items-center gap-3 px-4 py-16 text-center",
          )}
        >
          <h2 className="text-base font-semibold">{t("zone.title")}</h2>
          <p className="max-w-[60ch] text-[13.5px] text-pretty text-[var(--color-text-muted)]">
            {t("zone.body", { max: IMPORT_MAX_ROWS })}
          </p>
          <div className="flex flex-wrap justify-center gap-2">{tools}</div>
        </section>
      ) : (
        <>
          <section
            className={cn(
              zone,
              "mb-3 flex flex-wrap items-center justify-between gap-2 px-4 py-2.5",
            )}
          >
            <span className="text-[13px] text-[var(--color-text-muted)]">
              {t("zone.more")}
            </span>
            <div className="flex flex-wrap gap-2">{tools}</div>
          </section>
          <div
            className={cn(
              "grid gap-3",
              // A narrower editor below 1536px, so the table keeps its action column in view at 1280.
              current &&
                "xl:grid-cols-[minmax(0,1fr)_340px] 2xl:grid-cols-[minmax(0,1fr)_380px]",
            )}
          >
            <Card className="min-w-0 self-start px-3 py-2">
              <DataTable
                caption={t("title")}
                columns={columns}
                rows={rows}
                rowKey={(row) => row.id}
                emptyMessage=""
                minWidth={560}
                // From lg the table fits its card, so it need not scroll sideways, and the level pop-up of a
                // row is not cut off by the scroll box.
                className="lg:overflow-x-visible"
              />
            </Card>
            {current ? (
              <ImportRowEditor
                key={current.id}
                row={current}
                number={rows.indexOf(current) + 1}
                check={checks.get(current.id)!}
                catalogue={catalogue}
                locked={isLocked(current)}
                onChange={(patch) => change(current.id, patch)}
                onClose={() => setCurrentId(null)}
              />
            ) : null}
          </div>
        </>
      )}

      <details className="mt-4 text-[13px] text-[var(--color-text-muted)]">
        <summary className="cursor-pointer">{t("columnsHelp.title")}</summary>
        <p className="mt-1 max-w-[90ch] text-pretty">{t("columnsHelp.body")}</p>
      </details>

      {confirmingPublish ? (
        <ImportPublishDialog
          count={publishable.length}
          canAssign={canAssign}
          onClose={() => setConfirmingPublish(false)}
          onConfirm={publishAll}
        />
      ) : null}
      <ConfirmDialog
        open={guard.pending}
        onClose={guard.stay}
        onConfirm={guard.leave}
        title={t("leave.title")}
        confirmLabel={t("leave.confirm")}
        cancelLabel={t("leave.stay")}
        destructive
      >
        {busy ? t("leave.bodyBusy") : t("leave.body")}
      </ConfirmDialog>
    </PageContainer>
  );
}
