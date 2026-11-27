// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// 09-layoutBase/Giáo viên - Bài tập của tôi.dc.html: header + subtitle + "+ Gán từ ngân hàng bài
// toán" button (:115-120), 4 stat cards — bài đã gán, lượt nộp tuần này, AC trung bình, cần chấm
// tay (:123-134, values :259-264), filter row: search + class tabs + result count (:138-149,
// :266-271), assigned-problem table incl. the "Gán cho lớp" column (:151-170, :152-153, :161).
// "+ Gán từ ngân hàng bài toán" opens the dedicated screen /instructor/assignments/new with the class tab as
// ?classId= (DEC-2026-1123-assign-problems-dedicated-screen, which replaced the popup of
// DEC-2026-1011 and the problem-list route of DEC-2026-1122); "Chi tiết" wires to problem_detail per RD Q8 (closed),
// not the interview-bank link the prototype file is described as having in
// 01-rd/screens/teacher/INS0202_class_assignments.md:80-84. The remove popup asks for a class
// first only when the row spans more than one class, matching the N-N problem-class relation
// carried by "Gán cho lớp" (:161) — RD Q9 (closed).
//
// Nav sibling, not a tab of one page: this screen is the "Bài giao cho lớp" (BT) sidebar item,
// independent from "Lớp của tôi" (LH) [09-layoutBase/Giáo viên - Lớp của tôi.dc.html:221-222].
"use client";

import { useCallback, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import Link from "next/link";
import { Eye, Trash2 } from "lucide-react";
import {
  useAssignmentSummary,
  useClassAssignmentsPage,
  useRemoveAssignment,
  type AssignedRow,
} from "@/entities/class-assignment";
import { useInstructorClasses } from "@/entities/class";
import {
  problemLevelLabel,
  problemLevelTone,
  useProblemLevels,
} from "@/entities/problem";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { useDebounced } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  IconAction,
  PageContainer,
  PageHeader,
  Pagination,
  FilterMenu,
  SelectField,
  TextField,
  type DataTableColumn,
} from "@/shared/ui";
import { CopyAssignmentsDialog } from "./copy-assignments-dialog";

// 20 rows per page for a table that merges many rows (DEC-2026-1014-instructor-batch5-6-nav-pagination-rubric).
const PAGE_SIZE = 20;

/** First topic alphabetically plus "+n"; the full list stays in the tooltip (INS0202 Q5). */
function topicSummary(row: AssignedRow): { text: string; title: string } {
  const all = [...(row.topics?.length ? row.topics : [row.topic])].sort(
    (a, b) => a.localeCompare(b, "vi"),
  );
  return {
    text: all.length > 1 ? `${all[0]} +${all.length - 1}` : (all[0] ?? ""),
    title: all.join(", "),
  };
}

/** First class plus "+n"; the full list stays in the tooltip, so the column never forces a horizontal scroll. */
function classSummary(row: AssignedRow): { text: string; title: string } {
  const names = row.assignedClassNames;
  return {
    text:
      names.length > 1 ? `${names[0]} +${names.length - 1}` : (names[0] ?? ""),
    title: names.join(", "),
  };
}

/** Whole percent, so a ratio such as 22.2222 reads as 22%. */
const percent = (value: number) => `${Math.round(value)}%`;

export function ClassAssignmentsView() {
  const t = useT("classAssignments");
  const levelList = useProblemLevels();
  const summaryQuery = useAssignmentSummary();
  const apiMessage = useApiErrorMessage();
  const classesQuery = useInstructorClasses();
  const classes = classesQuery.data ?? [];

  // Class, search text and page live in the URL (?classId=&q=&page=), the one convention of every list.
  const searchParams = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const classTab = searchParams.get("classId") ?? "all";
  const page = Math.max(1, Number(searchParams.get("page")) || 1);
  const [query, setQuery] = useState(searchParams.get("q") ?? "");
  const setUrlParams = (patch: Record<string, string | null>) => {
    const next = new URLSearchParams(searchParams.toString());
    for (const [key, value] of Object.entries(patch)) {
      if (value === null || value === "") next.delete(key);
      else next.set(key, value);
    }
    const qs = next.toString();
    router.replace(qs ? `${pathname}?${qs}` : pathname, { scroll: false });
  };
  // The server filters by class and title and pages by 20; typing waits for a pause before it asks.
  const q = useDebounced((searchParams.get("q") ?? "").trim());
  const assignmentsQuery = useClassAssignmentsPage({
    classId: classTab,
    q,
    page,
  });
  const [removeTarget, setRemoveTarget] = useState<AssignedRow | null>(null);
  const [removeClassChoice, setRemoveClassChoice] = useState<string>("");
  const removeAssignment = useRemoveAssignment();

  // "Sao chép sang lớp khác": needs one class to copy from and another of the instructor to copy to.
  const [copyOpen, setCopyOpen] = useState(false);
  const closeCopy = useCallback(() => setCopyOpen(false), []);
  const copySource = classes.find((c) => c.id === classTab);
  const copyTargets = classes.filter((c) => c.id !== classTab);
  const copyBlocked =
    copyTargets.length === 0
      ? t("copy.noOtherClass")
      : q === "" &&
          !assignmentsQuery.isPlaceholderData &&
          assignmentsQuery.data?.page.totalItems === 0
        ? t("copy.sourceEmpty")
        : undefined;

  const pageRows = assignmentsQuery.data?.items ?? [];
  const totalRows = assignmentsQuery.data?.page.totalItems ?? 0;

  // Figures inside the selected class; "Tất cả" shows the problem's totals (INS0202 Q11).
  const statsFor = (row: AssignedRow) =>
    (classTab !== "all" ? row.classStats?.[classTab] : undefined) ?? row;

  const columns: DataTableColumn<AssignedRow>[] = [
    {
      key: "title",
      header: t("assignmentList.col.title"),
      render: (row) => (
        <span className="flex flex-wrap items-center gap-x-2 gap-y-1">
          <span>{row.title}</span>
          {row.withdrawn ? (
            <Badge variant="warn">{t("assignmentList.col.withdrawn")}</Badge>
          ) : null}
        </span>
      ),
    },
    {
      key: "topic",
      header: t("assignmentList.col.topic"),
      render: (row) => {
        const topic = topicSummary(row);
        return <span title={topic.title}>{topic.text}</span>;
      },
    },
    {
      key: "difficulty",
      header: t("assignmentList.col.difficulty"),
      render: (row) => (
        <Badge variant={problemLevelTone(levelList, row.difficulty)}>
          {problemLevelLabel(levelList, row.difficulty)}
        </Badge>
      ),
    },
    {
      key: "assignedClasses",
      header: t("assignmentList.col.assignedClasses"),
      render: (row) => {
        const classes = classSummary(row);
        return <span title={classes.title}>{classes.text}</span>;
      },
    },
    {
      key: "submissionCount",
      header: t("assignmentList.col.submissionCount"),
      align: "right",
      render: (row) => (
        <span className="font-mono">
          {statsFor(row).submissionCount === null
            ? "-"
            : statsFor(row).submissionCount!.toLocaleString("vi-VN")}
        </span>
      ),
    },
    {
      key: "acRate",
      header: t("assignmentList.col.acRate"),
      align: "right",
      render: (row) => {
        const acRate = statsFor(row).acRate;
        return acRate === null ? "-" : percent(acRate);
      },
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (row) => (
        <div className="flex justify-end gap-1.5">
          <IconAction
            icon={Eye}
            label={t("assignmentList.col.btnDetail")}
            href={`/instructor/problems/${row.id}`}
          />
          <IconAction
            icon={Trash2}
            tone="danger"
            label={t("assignmentList.col.btnRemove")}
            onClick={() => {
              setRemoveTarget(row);
              setRemoveClassChoice(
                classTab !== "all" ? classTab : (row.assignedClassIds[0] ?? ""),
              );
            }}
          />
        </div>
      ),
    },
  ];

  const summary = summaryQuery.data;

  return (
    <PageContainer variant="content" className="@container">
      <PageHeader
        title={t("header.title")}
        // The count comes from #50; while it is unknown no number is shown (never a made-up 0).
        // The class average sits on the same line (INS0202 V0.14: no stat strip on this screen); "-" when
        // #50 sends null (no submissions yet, or the statistics are down), never a made-up 0%.
        description={
          summary
            ? `${t("header.summary", { count: summary.assignedCount })} · ${t(
                "header.avgAcRate",
                {
                  value:
                    summary.avgAcRate == null
                      ? "-"
                      : percent(summary.avgAcRate),
                },
              )}`
            : undefined
        }
        actions={
          <>
            {copySource ? (
              <Button
                variant="ghost"
                size="sm"
                className="border border-[var(--color-border)]"
                // aria-disabled, not disabled: the reason stays reachable by hover and keyboard.
                aria-disabled={copyBlocked !== undefined || undefined}
                title={copyBlocked}
                onClick={() => {
                  if (copyBlocked === undefined) setCopyOpen(true);
                }}
              >
                {t("copy.button")}
              </Button>
            ) : null}
            <Button variant="cta" size="sm" asChild>
              <Link
                href={
                  classTab === "all"
                    ? "/instructor/assignments/new"
                    : `/instructor/assignments/new?classId=${classTab}`
                }
              >
                {t("header.btnAssignFromBank")}
              </Link>
            </Button>
          </>
        }
      />

      {/* Stat strip dropped 2026-09-27 (owner): every figure on it — số lớp, tổng học viên, cần
          chấm tay, điểm TB — is already the stat strip of instructor_overview, and repeating it on
          each screen pushed the actual worklist below the fold. These screens now open straight on
          their list + filter bar. Deliberate divergence from the mockup's own per-screen strip. */}
      <Card className="grow">
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
          <TextField
            label={t("assignmentList.search")}
            hideLabel
            placeholder={t("assignmentList.search")}
            value={query}
            onChange={(event) => {
              setQuery(event.target.value);
              setUrlParams({ q: event.target.value.trim(), page: null });
            }}
            wrapperClassName="min-w-[220px] flex-1"
          />
          <FilterMenu
            align="end"
            label={t("assignmentList.classTabs")}
            value={classTab}
            onValueChange={(value) =>
              setUrlParams({
                classId: value === "all" ? null : value,
                page: null,
              })
            }
            options={[
              { value: "all", label: t("filterAll") },
              ...classes.map((c) => ({ value: c.id, label: c.name })),
            ]}
          />
          <span className="ml-auto text-[12.5px] text-[var(--color-text-muted)]">
            {t("assignmentList.resultCount", {
              shown: totalRows,
              total: summary?.assignedCount ?? "-",
            })}
          </span>
        </div>

        <DataTable
          caption={t("header.title")}
          columns={columns}
          rows={pageRows}
          rowKey={(row) => row.id}
          status={
            assignmentsQuery.isLoading
              ? "loading"
              : assignmentsQuery.isError
                ? "error"
                : "ready"
          }
          emptyMessage={t("emptyAssignments")}
          errorMessage={t("loadFailed")}
          minWidth={0}
        />

        <Pagination
          className="mt-auto"
          hideWhenSinglePage
          page={page}
          pageSize={PAGE_SIZE}
          total={totalRows}
          onPageChange={(value) =>
            setUrlParams({ page: value > 1 ? String(value) : null })
          }
          summary={t("pageSummary", {
            page,
            total: Math.max(1, assignmentsQuery.data?.page.totalPages ?? 1),
          })}
          previousLabel={t("previous")}
          nextLabel={t("next")}
        />
      </Card>

      <ConfirmDialog
        open={removeTarget !== null}
        onClose={() => setRemoveTarget(null)}
        onConfirm={() => {
          if (removeTarget && removeClassChoice) {
            const title = removeTarget.title;
            removeAssignment.mutate(
              { problemId: removeTarget.id, classId: removeClassChoice },
              {
                onSuccess: () => toast.success(t("toast.removed", { title })),
                onError: (error) => toast.error(apiMessage(error)),
              },
            );
          }
          setRemoveTarget(null);
        }}
        title={t("popup.removeAssignmentTitle", {
          title: removeTarget?.title ?? "",
        })}
        confirmLabel={t("popup.removeAssignmentConfirm")}
        cancelLabel={t("cancel")}
        destructive
      >
        <p className="mb-3">{t("popup.removeAssignmentBody")}</p>
        {removeTarget &&
        classTab === "all" &&
        removeTarget.assignedClassIds.length > 1 ? (
          <SelectField
            label={t("popup.removeClassChoice")}
            value={removeClassChoice}
            onChange={(event) => setRemoveClassChoice(event.target.value)}
            options={removeTarget.assignedClassIds.map((id, index) => ({
              value: id,
              label: removeTarget.assignedClassNames[index] ?? id,
            }))}
          />
        ) : null}
      </ConfirmDialog>

      {copyOpen && copySource ? (
        <CopyAssignmentsDialog
          source={copySource}
          targets={copyTargets}
          onClose={closeCopy}
          onCopied={(targetId) => {
            setCopyOpen(false);
            setQuery("");
            setUrlParams({ classId: targetId, q: null, page: null });
          }}
        />
      ) : null}
    </PageContainer>
  );
}
