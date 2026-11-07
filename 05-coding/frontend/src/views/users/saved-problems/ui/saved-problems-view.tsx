// USR0103 on the real api (03-dd/api/F2_problem-bank.md #8 to #10); mock mode answers the same contract.
//
// Layout follows 09-layoutBase/Bài đã lưu.dc.html: toolbar (:112-123), table (:138-165), empty state
// (:167-172). The KPI strip (:100-107) was dropped 2026-10-18 (owner: a list screen has no KPI strip).
// F2-13: the note is private per (user_id, problem_id), see 01-rd/screens/users/USR0103_saved_problems.md.
// Owner 2026-10-18 (BD Q2-Q5): unsave is undoable (no confirm popup), the note is edited in place, a note
// is at most 1000 characters, the list pages at 20 rows (pager hidden for a single page).
//
// The server filters and pages; this view only sends `SavedParams`. Unsaving deletes the bookmark and its
// note on the server; "Hoàn tác" puts both back with a new save date, and says so when the problem was
// withdrawn in the meantime and can no longer be bookmarked.
"use client";

import { useState } from "react";
import Link from "next/link";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
  useRestoreBookmark,
  useSaveBookmarkNote,
  useSavedProblemsPage,
  useUnsaveProblem,
  type SavedParams,
  type SavedRow,
  type SolveState,
} from "@/entities/problem";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useLocale, useT } from "@/shared/i18n";
import { formatDateVN, useDebounced } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  DataTable,
  EmptyState,
  Pagination,
  QueryError,
  Skeleton,
  TextArea,
  FilterBar,
  FilterMenu,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

const SOLVE_STATE_VARIANT: Record<SolveState, BadgeVariant> = {
  solved: "success",
  attempted: "warn",
  todo: "neutral",
};

// Named constants so the numbers have one home: BD USR0103 Q3 (note length), Q5 (page size), Sheet 9 NO 4 (search length).
const NOTE_MAX_LENGTH = 1000;
const PAGE_SIZE = 20;
const SEARCH_MAX_LENGTH = 100;

type NoteEdit = { id: string; draft: string };

type StatusFilter = SolveState | "all";

export function SavedProblemsView() {
  const t = useT("savedProblems");
  const tc = useT("common");
  const locale = useLocale() === "en" ? "en" : "vi";
  const apiMessage = useApiErrorMessage();
  const levels = useProblemLevels();
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [difficulty, setDifficulty] = useState("all");
  const [page, setPage] = useState(1);
  const [noteEdit, setNoteEdit] = useState<NoteEdit | null>(null);

  // Typing filters live, but only a pause in typing becomes a request.
  const debouncedQuery = useDebounced(query);
  const params: SavedParams = {
    q: debouncedQuery.trim().slice(0, SEARCH_MAX_LENGTH),
    status,
    difficulty,
    page,
  };
  const saved = useSavedProblemsPage(params);
  const unsave = useUnsaveProblem();
  const saveNote = useSaveBookmarkNote();
  const restore = useRestoreBookmark();
  const data = saved.data;

  function resetFilter<T>(setter: (value: T) => void, value: T) {
    setter(value);
    setPage(1);
    // A row hidden by the new filter must not keep an open note editor.
    setNoteEdit(null);
  }

  function doUnsave(problem: SavedRow) {
    // Unsaving deletes the private note with the bookmark (REQ-04). The removed row's note stays in the
    // Hoàn tác toast closure, so undo restores the bookmark AND its note. One toast per unsave, each with
    // its own closure, so a second unsave never loses the first.
    unsave.mutate(problem.id, {
      onSuccess: () => {
        setNoteEdit((current) => (current?.id === problem.id ? null : current));
        toast.info(t("undo.message", { title: problem.title }), {
          label: t("undo.button"),
          onClick: () =>
            restore.mutate(
              { problemId: problem.id, note: problem.note },
              { onError: (error) => toast.error(apiMessage(error)) },
            ),
        });
      },
      onError: (error) => toast.error(apiMessage(error)),
    });
  }

  function doSaveNote(edit: NoteEdit) {
    saveNote.mutate(
      { problemId: edit.id, note: edit.draft.trim() },
      {
        onSuccess: () => {
          setNoteEdit(null);
          toast.success(t("toast.noteSaved"));
        },
        onError: (error) => {
          toast.error(apiMessage(error));
          // PRB-014: the bookmark is gone (removed on another device). Close the editor and reload.
          if (error instanceof ApiError && error.code === "PRB-014") {
            setNoteEdit(null);
            void saved.refetch();
          }
        },
      },
    );
  }

  // The list filters live while typing (no toast per keystroke); Enter is the explicit search.
  function announceSearch() {
    const count = data?.meta.filteredTotal ?? 0;
    toast.info(
      count > 0 ? t("toast.searchResult", { count }) : t("toast.searchEmpty"),
    );
  }

  const columns: DataTableColumn<SavedRow>[] = [
    {
      key: "title",
      header: t("table.colProblem"),
      render: (problem) => (
        <div className="min-w-0">
          {problem.withdrawn ? (
            <span className="block truncate font-semibold text-[var(--color-text-muted)]">
              <span className="mr-1.5 font-mono text-xs">{problem.code}</span>
              {problem.title}
            </span>
          ) : (
            <Link
              href={`/problems/${problem.id}`}
              className="block truncate font-semibold hover:underline"
            >
              <span className="mr-1.5 font-mono text-xs text-[var(--color-text-muted)]">
                {problem.code}
              </span>
              {problem.title}
            </Link>
          )}
          <span className="mt-1 flex flex-wrap gap-1.5">
            <Badge variant={problemLevelTone(levels, problem.difficulty)}>
              {problemLevelLabel(levels, problem.difficulty)}
            </Badge>
            {problem.withdrawn ? (
              <Badge variant="warn">{t("table.withdrawn")}</Badge>
            ) : null}
          </span>
        </div>
      ),
    },
    {
      key: "topics",
      header: t("table.colTopics"),
      width: "132px",
      render: (problem) => (
        <span className="block truncate text-[12.5px] text-[var(--color-text-muted)]">
          {problem.topics.join(", ")}
        </span>
      ),
    },
    {
      key: "solveState",
      header: t("table.colState"),
      width: "108px",
      render: (problem) =>
        problem.solveState ? (
          <Badge variant={SOLVE_STATE_VARIANT[problem.solveState]}>
            {t(`state.${problem.solveState}`)}
          </Badge>
        ) : (
          // Unknown (the solve-state source did not answer): not "Chưa làm", which would be a guess.
          <span className="text-[12.5px] text-[var(--color-text-subtle)]">
            {t("state.unknown")}
          </span>
        ),
    },
    {
      key: "note",
      header: t("table.colNote"),
      render: (problem) =>
        noteEdit?.id === problem.id ? (
          <div className="min-w-[220px]">
            <TextArea
              label={t("table.noteEditLabel")}
              hideLabel
              rows={3}
              maxLength={NOTE_MAX_LENGTH}
              value={noteEdit.draft}
              onChange={(event) =>
                setNoteEdit({ id: problem.id, draft: event.target.value })
              }
            />
            <div className="mt-1 flex items-center justify-between gap-2">
              <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">
                {t("table.noteCounter", {
                  // Strings: a number argument would be grouped by locale ("1.000").
                  count: String(noteEdit.draft.length),
                  max: String(NOTE_MAX_LENGTH),
                })}
              </span>
              <span className="flex gap-1.5">
                <Button
                  variant="ghost"
                  size="sm"
                  className="border border-[var(--color-border)]"
                  onClick={() => setNoteEdit(null)}
                >
                  {t("table.noteCancel")}
                </Button>
                <Button
                  size="sm"
                  disabled={saveNote.isPending}
                  onClick={() => doSaveNote(noteEdit)}
                >
                  {t("table.noteSave")}
                </Button>
              </span>
            </div>
          </div>
        ) : (
          <button
            type="button"
            title={t("table.noteEditHint")}
            onClick={() => setNoteEdit({ id: problem.id, draft: problem.note })}
            className="block max-w-[280px] truncate text-left text-[12.5px] text-[var(--color-text-muted)] hover:underline"
          >
            {problem.note || t("table.noNote")}
          </button>
        ),
    },
    {
      key: "savedAt",
      header: t("table.colSavedAt"),
      width: "104px",
      render: (problem) => (
        <span className="text-[12.5px] text-[var(--color-text-muted)]">
          {formatDateVN(problem.savedAt, { locale })}
        </span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "168px",
      align: "right",
      render: (problem) => (
        <span className="flex justify-end gap-1.5">
          {problem.withdrawn ? (
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              disabled
              title={t("table.withdrawnHint")}
            >
              {t("table.btnOpen")}
            </Button>
          ) : (
            <Button
              asChild
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
            >
              <Link href={`/problems/${problem.id}`}>{t("table.btnOpen")}</Link>
            </Button>
          )}
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)] text-[var(--color-admin-negative)]"
            disabled={unsave.isPending}
            onClick={() => doUnsave(problem)}
          >
            {t("table.btnUnsave")}
          </Button>
        </span>
      ),
    },
  ];

  return (
    <div className="p-6">
      <div className="mb-4">
        <h1 className="text-lg font-bold tracking-tight">{t("title")}</h1>
        {data ? (
          <p className="text-[12.5px] text-[var(--color-text-muted)]">
            {/* An unknown solved count is left out, never shown as 0. */}
            {data.meta.solvedCount === null
              ? t("subtitleNoSolved", { total: data.meta.savedTotal })
              : t("subtitle", {
                  total: data.meta.savedTotal,
                  solved: data.meta.solvedCount,
                })}
          </p>
        ) : null}
      </div>

      <Card className="min-w-0 px-[18px] py-4">
        <FilterBar
          search={{
            label: t("filter.searchLabel"),
            placeholder: t("filter.searchPlaceholder"),
            value: query,
            maxLength: SEARCH_MAX_LENGTH,
            onChange: (next) => resetFilter(setQuery, next),
            onSubmit: announceSearch,
          }}
        >
          <FilterMenu
            label={t("filter.difficultyLabel")}
            value={difficulty}
            onValueChange={(next) => resetFilter(setDifficulty, next)}
            options={[
              { value: "all", label: t("filter.all") },
              ...levels.map((level) => ({
                value: level.key,
                label: level.label,
              })),
            ]}
          />
          <FilterMenu
            label={t("filter.statusLabel")}
            value={status}
            onValueChange={(next) => resetFilter(setStatus, next)}
            options={[
              { value: "all", label: t("filter.all") },
              { value: "solved", label: t("state.solved") },
              { value: "attempted", label: t("state.attempted") },
              { value: "todo", label: t("state.todo") },
            ]}
          />
        </FilterBar>

        {data && data.meta.solvedCount === null ? (
          <p
            role="status"
            className="mb-3 text-[12.5px] text-[var(--color-text-muted)]"
          >
            {t("degraded.solveState")}
          </p>
        ) : null}

        {saved.isError ? (
          <QueryError query={saved} />
        ) : !data ? (
          <div role="status" aria-label={tc("loading")} className="space-y-3">
            <Skeleton className="h-40 w-full" />
          </div>
        ) : data.meta.savedTotal === 0 ? (
          <EmptyState>{t("emptyAll")}</EmptyState>
        ) : (
          <DataTable
            caption={t("table.caption")}
            columns={columns}
            rows={data.items}
            rowKey={(problem) => problem.id}
            emptyMessage={t("table.emptyFiltered")}
            minWidth={760}
          />
        )}

        {data && data.page.totalItems > PAGE_SIZE ? (
          <Pagination
            page={data.page.number}
            pageSize={PAGE_SIZE}
            total={data.page.totalItems}
            onPageChange={(next) => {
              setPage(next);
              setNoteEdit(null);
            }}
            summary={t("footer.summary", {
              from: (data.page.number - 1) * PAGE_SIZE + 1,
              to: Math.min(data.page.number * PAGE_SIZE, data.page.totalItems),
              total: data.page.totalItems,
            })}
            previousLabel={t("footer.previous")}
            nextLabel={t("footer.next")}
            showPageNumbers
            pageLabel={(pageNumber) =>
              t("footer.pageLabel", { page: pageNumber })
            }
          />
        ) : null}

        <p className="mt-3 text-[11.5px] text-[var(--color-text-subtle)]">
          {t("footerPrivacyNote")}
        </p>
      </Card>
    </div>
  );
}
