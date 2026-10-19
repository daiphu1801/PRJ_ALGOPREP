// PROTOTYPE — chưa có DD. Xem 06-plan/PROTOTYPE_DEBT.md mục 9.
//
// Layout theo 09-layoutBase/Bài đã lưu.dc.html: thanh công cụ (:112-123),
// bảng danh sách (:138-165), trạng thái rỗng (:167-172).
// Dải chỉ số (:100-107) đã BỎ 2026-10-18 (owner: màn danh sách không có dải KPI).
// F2-13: ghi chú riêng tư tuyệt đối theo (user_id, problem_id) — 01-rd/screens/users/USR0103_saved_problems.md.
// Owner 2026-10-18 (BD Q2-Q5): unsave is undoable (no confirm popup), the note is edited in place,
// a note is at most 1000 characters, the list pages at 20 rows (pager hidden for a single page).
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import {
  fetchSavedProblems,
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
  type SavedProblem,
  type SolveState,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  DataTable,
  EmptyState,
  PageHeader,
  Pagination,
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
type DifficultyFilter = string;

export function SavedProblemsView() {
  const t = useT("savedProblems");
  const levels = useProblemLevels();
  const [all, setAll] = useState(fetchSavedProblems);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("all");
  const [page, setPage] = useState(1);
  const [noteEdit, setNoteEdit] = useState<NoteEdit | null>(null);

  const filtered = useMemo(() => {
    const needle = query.trim().toLowerCase();
    return all.filter(
      (problem) =>
        (status === "all" || problem.solveState === status) &&
        (difficulty === "all" || problem.difficulty === difficulty) &&
        (!needle ||
          problem.code.toLowerCase().includes(needle) ||
          problem.title.toLowerCase().includes(needle)),
    );
  }, [all, query, status, difficulty]);

  // Client-side paging over the mock list; the real list is paged on the server (BD Q5).
  const lastPage = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const currentPage = Math.min(page, lastPage);
  const pageRows = filtered.slice(
    (currentPage - 1) * PAGE_SIZE,
    currentPage * PAGE_SIZE,
  );

  const solvedCount = all.filter(
    (problem) => problem.solveState === "solved",
  ).length;

  function unsave(id: string) {
    // Bỏ lưu xoá luôn ghi chú riêng tư gắn với bookmark đó — REQ-04. The removed row is kept in
    // the Hoàn tác toast closure, so undo restores the bookmark AND its note.
    const index = all.findIndex((problem) => problem.id === id);
    const removed = all[index];
    if (!removed) return;
    setAll((previous) => previous.filter((problem) => problem.id !== id));
    setNoteEdit((current) => (current?.id === id ? null : current));
    // One toast per unsave, each with its own Hoàn tác closure, so a second unsave never loses the first.
    toast.info(t("undo.message", { title: removed.title }), {
      label: t("undo.button"),
      onClick: () =>
        setAll((previous) => {
          if (previous.some((problem) => problem.id === removed.id))
            return previous;
          const next = [...previous];
          next.splice(Math.min(index, next.length), 0, removed);
          return next;
        }),
    });
  }

  function saveNote(edit: NoteEdit) {
    setAll((previous) =>
      previous.map((problem) =>
        problem.id === edit.id
          ? { ...problem, note: edit.draft.trim() }
          : problem,
      ),
    );
    setNoteEdit(null);
    toast.success(t("toast.noteSaved"));
  }

  function resetFilter<T>(setter: (value: T) => void, value: T) {
    setter(value);
    setPage(1);
    // A row hidden by the new filter must not keep an open note editor.
    setNoteEdit(null);
  }

  // The list filters live while typing (no toast per keystroke); Enter is the explicit search.
  function announceSearch() {
    toast.info(
      filtered.length > 0
        ? t("toast.searchResult", { count: filtered.length })
        : t("toast.searchEmpty"),
    );
  }

  const columns: DataTableColumn<SavedProblem>[] = [
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
      render: (problem) => (
        <Badge variant={SOLVE_STATE_VARIANT[problem.solveState]}>
          {t(`state.${problem.solveState}`)}
        </Badge>
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
                <Button size="sm" onClick={() => saveNote(noteEdit)}>
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
          {problem.savedAtLabel}
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
              {t(`table.solveLabel.${problem.solveState}`)}
            </Button>
          ) : (
            <Button
              asChild
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
            >
              <Link href={`/problems/${problem.id}`}>
                {t(`table.solveLabel.${problem.solveState}`)}
              </Link>
            </Button>
          )}
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)] text-[var(--color-admin-negative)]"
            onClick={() => unsave(problem.id)}
          >
            {t("table.btnUnsave")}
          </Button>
        </span>
      ),
    },
  ];

  return (
    <div className="p-6">
      <PageHeader
        title={t("title")}
        description={t("subtitle", { total: all.length, solved: solvedCount })}
      />

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

        {all.length === 0 ? (
          <EmptyState>{t("emptyAll")}</EmptyState>
        ) : (
          <DataTable
            caption={t("table.caption")}
            columns={columns}
            rows={pageRows}
            rowKey={(problem) => problem.id}
            emptyMessage={t("table.emptyFiltered")}
            minWidth={760}
          />
        )}

        {filtered.length > PAGE_SIZE ? (
          <Pagination
            page={currentPage}
            pageSize={PAGE_SIZE}
            total={filtered.length}
            onPageChange={setPage}
            summary={t("footer.summary", {
              from: (currentPage - 1) * PAGE_SIZE + 1,
              to: Math.min(currentPage * PAGE_SIZE, filtered.length),
              total: filtered.length,
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
