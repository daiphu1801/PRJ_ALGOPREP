// PROTOTYPE — chưa có DD. Xem 06-plan/PROTOTYPE_DEBT.md mục 9.
//
// Layout theo 09-layoutBase/Bài đã lưu.dc.html: dải chỉ số (:100-107), thanh công cụ (:112-123),
// bảng danh sách (:138-165), trạng thái rỗng (:167-172).
// F2-13: ghi chú riêng tư tuyệt đối theo (user_id, problem_id) — 01-rd/screens/users/USR0103_saved_problems.md.
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { Search } from "lucide-react";
import { fetchSavedProblems, type Difficulty, type SavedProblem, type SolveState } from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  DataTable,
  EmptyState,
  PageHeader,
  SegmentedTabs,
  StatCard,
  TextField,
  type BadgeVariant,
  type DataTableColumn,
} from "@/shared/ui";

const DIFFICULTY_VARIANT: Record<Difficulty, BadgeVariant> = {
  easy: "success",
  medium: "warn",
  hard: "negative",
};

const SOLVE_STATE_VARIANT: Record<SolveState, BadgeVariant> = {
  solved: "success",
  attempted: "warn",
  todo: "neutral",
};

type StatusFilter = SolveState | "all";
type DifficultyFilter = Difficulty | "all";

export function SavedProblemsView() {
  const t = useT("savedProblems");
  const [all, setAll] = useState(fetchSavedProblems);
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState<StatusFilter>("all");
  const [difficulty, setDifficulty] = useState<DifficultyFilter>("all");

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

  const solvedCount = all.filter((problem) => problem.solveState === "solved").length;

  function unsave(id: string) {
    // Bỏ lưu xoá luôn ghi chú riêng tư gắn với bookmark đó — REQ-04.
    setAll((previous) => previous.filter((problem) => problem.id !== id));
    toast.success(t("toast.unsaved"));
  }

  // The list filters live while typing (no toast per keystroke); Enter is the explicit search.
  function announceSearch() {
    toast.info(filtered.length > 0 ? t("toast.searchResult", { count: filtered.length }) : t("toast.searchEmpty"));
  }

  const columns: DataTableColumn<SavedProblem>[] = [
    {
      key: "title",
      header: t("table.colProblem"),
      render: (problem) => (
        <div className="min-w-0">
          <Link href={`/problems/${problem.id}`} className="block truncate font-semibold hover:underline">
            <span className="mr-1.5 font-mono text-xs text-[var(--color-text-muted)]">{problem.code}</span>
            {problem.title}
          </Link>
          <Badge variant={DIFFICULTY_VARIANT[problem.difficulty]} className="mt-1">
            {t(`difficulty.${problem.difficulty}`)}
          </Badge>
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
        <Badge variant={SOLVE_STATE_VARIANT[problem.solveState]}>{t(`state.${problem.solveState}`)}</Badge>
      ),
    },
    {
      key: "note",
      header: t("table.colNote"),
      render: (problem) => (
        <span className="block max-w-[280px] truncate text-[12.5px] text-[var(--color-text-muted)]">
          {problem.note || t("table.noNote")}
        </span>
      ),
    },
    {
      key: "savedAt",
      header: t("table.colSavedAt"),
      width: "104px",
      render: (problem) => (
        <span className="text-[12.5px] text-[var(--color-text-muted)]">{problem.savedAtLabel}</span>
      ),
    },
    {
      key: "actions",
      header: "",
      width: "168px",
      align: "right",
      render: (problem) => (
        <span className="flex justify-end gap-1.5">
          <Button asChild variant="ghost" size="sm" className="border border-[var(--color-border)]">
            <Link href={`/problems/${problem.id}`}>{t("table.btnSolve")}</Link>
          </Button>
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
      <PageHeader title={t("title")} description={t("subtitle", { total: all.length, solved: solvedCount })} />

      <div className="mb-4 grid gap-3.5 [grid-template-columns:repeat(auto-fit,minmax(160px,1fr))]">
        <StatCard label={t("summary.total")} value={String(all.length)} />
        <StatCard label={t("summary.solved")} value={String(solvedCount)} />
      </div>

      <Card className="min-w-0 px-[18px] py-4">
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
          <TextField
            label={t("filter.searchLabel")}
            hideLabel
            leadingIcon={<Search className="h-3.5 w-3.5" />}
            placeholder={t("filter.searchPlaceholder")}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === "Enter") announceSearch();
            }}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <SegmentedTabs
            label={t("filter.difficultyLabel")}
            value={difficulty}
            onValueChange={setDifficulty}
            options={[
              { value: "all", label: t("filter.all") },
              { value: "easy", label: t("difficulty.easy") },
              { value: "medium", label: t("difficulty.medium") },
              { value: "hard", label: t("difficulty.hard") },
            ]}
          />
          <SegmentedTabs
            label={t("filter.statusLabel")}
            value={status}
            onValueChange={setStatus}
            options={[
              { value: "all", label: t("filter.all") },
              { value: "solved", label: t("state.solved") },
              { value: "attempted", label: t("state.attempted") },
              { value: "todo", label: t("state.todo") },
            ]}
          />
        </div>

        {all.length === 0 ? (
          <EmptyState>{t("emptyAll")}</EmptyState>
        ) : (
          <DataTable
            caption={t("table.caption")}
            columns={columns}
            rows={filtered}
            rowKey={(problem) => problem.id}
            emptyMessage={t("table.emptyFiltered")}
            minWidth={760}
          />
        )}

        <p className="mt-3 text-[11.5px] text-[var(--color-text-subtle)]">{t("footerPrivacyNote")}</p>
      </Card>
    </div>
  );
}
