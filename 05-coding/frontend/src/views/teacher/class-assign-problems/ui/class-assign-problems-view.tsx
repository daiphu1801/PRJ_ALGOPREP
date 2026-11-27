// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Giao bài cho lớp" (INS0202 child, DEC-2026-1123-assign-problems-dedicated-screen): its own screen, opened from
// "+ Gán từ ngân hàng bài toán" on class_assignments. The class is chosen at the top (preselected from ?classId=),
// the list shows the PUBLISHED bank problems with search, level filter, topic filter and paging, a problem the
// class already has stays visible but dimmed with the badge "Đã giao" and cannot be ticked, and a bar fixed at the
// bottom confirms the whole selection with one button. Search, filters and paging are done by the server (F2 #51).
"use client";

import { useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import {
  useAssignableProblems,
  useAssignProblems,
  type AssignableProblem,
} from "@/entities/class-assignment";
import { useInstructorClasses } from "@/entities/class";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
  useProblemTopics,
} from "@/entities/problem";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { useDebounced } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  BulkActionBar,
  Button,
  Card,
  DataTable,
  EmptyState,
  FilterMenu,
  IconAction,
  PageContainer,
  PageHeader,
  Pagination,
  TextField,
  type DataTableColumn,
} from "@/shared/ui";

const PAGE_SIZE = 20;
// The server takes 1 to 50 problems per call (ClassAssignmentCommandHandler.MAX_IDS, PRB-001).
const ASSIGN_MAX = 50;
const SEARCH_MAX_LENGTH = 100;

/** First topic alphabetically plus "+n"; the full list stays in the tooltip (same rule as INS0202). */
function topicSummary(row: AssignableProblem): { text: string; title: string } {
  const all = [...(row.topics?.length ? row.topics : [row.topic])]
    .filter(Boolean)
    .sort((a, b) => a.localeCompare(b, "vi"));
  return {
    text: all.length > 1 ? `${all[0]} +${all.length - 1}` : (all[0] ?? "-"),
    title: all.join(", "),
  };
}

export function ClassAssignProblemsView() {
  const t = useT("classAssignProblems");
  const apiMessage = useApiErrorMessage();
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const levelList = useProblemLevels();
  const topicList = useProblemTopics();
  const classesQuery = useInstructorClasses();
  const classes = classesQuery.data ?? [];
  const assign = useAssignProblems();

  // The class lives in the URL (?classId=), so the screen can be linked to and survives a reload; an unknown or
  // missing id falls back to the first class.
  const requestedClass = searchParams.get("classId") ?? "";
  const classId = classes.some((c) => c.id === requestedClass)
    ? requestedClass
    : (classes[0]?.id ?? "");
  const className = classes.find((c) => c.id === classId)?.name ?? "";

  const [query, setQuery] = useState("");
  const [level, setLevel] = useState("all");
  const [topic, setTopic] = useState("all");
  const [page, setPage] = useState(1);
  const [selected, setSelected] = useState<ReadonlySet<string>>(new Set());

  const list = useAssignableProblems({
    classId,
    q: useDebounced(query.trim().slice(0, SEARCH_MAX_LENGTH)),
    level,
    topic,
    page,
  });
  const rows = list.data?.items ?? [];
  const totalItems = list.data?.page.totalItems ?? 0;
  const totalPages = Math.max(1, list.data?.page.totalPages ?? 1);

  // A filter or a page change keeps the ticks (they are problem ids, still valid); a class change clears them,
  // because "already given" differs per class.
  function filter(apply: () => void) {
    apply();
    setPage(1);
  }

  function chooseClass(next: string) {
    const params = new URLSearchParams(searchParams.toString());
    params.set("classId", next);
    router.replace(`${pathname}?${params.toString()}`, { scroll: false });
    setSelected(new Set());
    setPage(1);
  }

  function toggleRow(id: string) {
    setSelected((previous) => {
      const next = new Set(previous);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }

  function confirm() {
    const problemIds = [...selected];
    assign.mutate(
      { classId, problemIds },
      {
        onSuccess: (result) => {
          // PRB-037 not published, PRB-010 gone... are refused one by one; the rest still go through.
          if (result.succeeded.length > 0 || result.failed.length === 0)
            toast.success(
              t("toast.assigned", {
                count: result.succeeded.length,
                className,
              }),
            );
          if (result.skipped.length > 0)
            toast.info(t("toast.skipped", { count: result.skipped.length }));
          for (const code of new Set(result.failed.map((item) => item.code)))
            toast.warning(
              `${t("toast.refused", {
                count: result.failed.filter((item) => item.code === code)
                  .length,
              })} ${apiMessage(new ApiError(code, 422))}`,
            );
          router.push(`/instructor/assignments?classId=${classId}`);
        },
        onError: (error) => toast.error(apiMessage(error)),
      },
    );
  }

  const columns: DataTableColumn<AssignableProblem>[] = [
    {
      key: "title",
      header: t("col.title"),
      render: (row) => (
        <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
          {row.code ? (
            <span
              title={row.code}
              className="max-w-28 truncate font-mono text-xs text-[var(--color-text-muted)]"
            >
              {row.code}
            </span>
          ) : null}
          <Link
            href={`/instructor/problems/${row.id}`}
            target="_blank"
            rel="noopener noreferrer"
            className="font-semibold hover:underline"
          >
            {row.title}
          </Link>
          {row.assigned ? <Badge variant="teal">{t("assigned")}</Badge> : null}
        </span>
      ),
    },
    {
      key: "topic",
      header: t("col.topic"),
      render: (row) => {
        const summary = topicSummary(row);
        return <span title={summary.title}>{summary.text}</span>;
      },
    },
    {
      key: "difficulty",
      header: t("col.difficulty"),
      render: (row) => (
        <Badge variant={problemLevelTone(levelList, row.difficulty)}>
          {problemLevelLabel(levelList, row.difficulty)}
        </Badge>
      ),
    },
  ];

  const back = (
    <IconAction
      icon={ArrowLeft}
      label={t("back")}
      href={
        classId
          ? `/instructor/assignments?classId=${classId}`
          : "/instructor/assignments"
      }
    />
  );

  if (classesQuery.isSuccess && classes.length === 0) {
    return (
      <PageContainer variant="content">
        <PageHeader leading={back} title={t("title")} />
        <EmptyState>
          {t("noClass")}{" "}
          <Link className="underline" href="/instructor/classes">
            {t("noClassLink")}
          </Link>
        </EmptyState>
      </PageContainer>
    );
  }

  return (
    <PageContainer variant="content" className="@container">
      <PageHeader
        leading={back}
        title={t("title")}
        description={t("subtitle")}
        actions={
          <FilterMenu
            align="end"
            label={t("classLabel")}
            value={classId}
            onValueChange={chooseClass}
            options={classes.map((c) => ({ value: c.id, label: c.name }))}
          />
        }
      />

      <Card className="grow">
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
          <TextField
            label={t("search")}
            hideLabel
            placeholder={t("search")}
            maxLength={SEARCH_MAX_LENGTH}
            value={query}
            onChange={(event) => filter(() => setQuery(event.target.value))}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <FilterMenu
            label={t("levelFilter")}
            value={level}
            onValueChange={(value) => filter(() => setLevel(value))}
            options={[
              { value: "all", label: t("filterAll") },
              ...levelList.map((item) => ({
                value: item.key,
                label: item.label,
              })),
            ]}
          />
          <FilterMenu
            align="end"
            label={t("topicFilter")}
            value={topic}
            onValueChange={(value) => filter(() => setTopic(value))}
            options={[
              { value: "all", label: t("filterAll") },
              // The server matches a topic by its name, so the label is the value.
              ...topicList.map((item) => ({
                value: item.label,
                label: item.label,
              })),
            ]}
          />
          <span className="ml-auto text-[12.5px] text-[var(--color-text-muted)]">
            {t("resultCount", { count: totalItems })}
          </span>
        </div>

        <DataTable
          caption={t("title")}
          columns={columns}
          rows={rows}
          rowKey={(row) => row.id}
          status={list.isLoading ? "loading" : list.isError ? "error" : "ready"}
          emptyMessage={t("empty")}
          errorMessage={t("loadFailed")}
          minWidth={0}
          selection={{
            selectedKeys: selected,
            onToggleRow: toggleRow,
            onToggleAll: (selectAll) =>
              setSelected((previous) => {
                const next = new Set(previous);
                for (const row of rows) {
                  if (row.assigned) continue;
                  if (selectAll) next.add(row.id);
                  else next.delete(row.id);
                }
                return next;
              }),
            selectAllLabel: t("selectAll"),
            rowLabel: (row) => t("selectRow", { title: row.title }),
            isRowDisabled: (row) => row.assigned,
          }}
        />

        <Pagination
          className="mt-auto"
          hideWhenSinglePage
          page={page}
          pageSize={PAGE_SIZE}
          total={totalItems}
          onPageChange={setPage}
          summary={t("pageSummary", { page, total: totalPages })}
          previousLabel={t("previous")}
          nextLabel={t("next")}
        />
      </Card>

      <BulkActionBar
        placement="bottom"
        count={selected.size}
        label={t("selection", { count: selected.size, className })}
      >
        <Button
          variant="ghost"
          size="sm"
          className="border border-[var(--color-border)]"
          disabled={assign.isPending}
          onClick={() => setSelected(new Set())}
        >
          {t("clear")}
        </Button>
        {selected.size > ASSIGN_MAX ? (
          <span className="text-[12.5px] text-[var(--color-admin-warn-text)]">
            {t("overLimit", { max: ASSIGN_MAX })}
          </span>
        ) : null}
        <Button
          variant="cta"
          size="sm"
          disabled={
            assign.isPending || classId === "" || selected.size > ASSIGN_MAX
          }
          onClick={confirm}
        >
          {t("confirm", { count: selected.size })}
        </Button>
      </BulkActionBar>
    </PageContainer>
  );
}
