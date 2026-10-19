// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// CLASS management only — restructured 2026-09-27 (owner: "1 pages quản lý lớp học, 1 pages quản
// lý danh sách học viên"). The "Danh sách học viên" table the mockup puts here (:159-188) moved to
// class_progress, which already listed the same students with progress columns; keeping both meant
// two near-identical tables over one dataset. "Xem học viên" now links there with ?classId= so the
// per-class view is one click away.
//
// Layout theo 09-layoutBase/Giáo viên - Lớp của tôi.dc.html: header + nút "+ Tạo lớp mới"
// (:115-121). The class grid of the mockup (:136-157) became a LIST (a table inside a card, search on
// top, icon actions per row) on 2026-10-11 by owner instruction, like the other list screens.
//
// Layout follows 02-bd/screens/teacher/INS0201_class_management.md Sheet 4.4 for the rest of the
// structure (header row, 4 stat cards, class card grid, student table with class tabs).
// The action menu / class-card click filter / student "Gỡ khỏi lớp" / student name link (Sheet 4.4
// last paragraph) are all BD-added — prototype predates F1-24..F1-27 — rendered here as plain
// buttons rather than a full dropdown-menu primitive (none exists in shared/ui yet and one screen
// does not justify adding it).
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Eye, Pencil, Trash2 } from "lucide-react";
import {
  useCreateClass,
  useDeleteClass,
  useInstructorClasses,
  formatClassSchedule,
  useUpdateClass,
  type ClassSummary,
} from "@/entities/class";
import { ClassFormDialog, type ClassDialogState } from "@/features/edit-class";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Button,
  Card,
  ConfirmDialog,
  DataTable,
  FilterBar,
  IconAction,
  PageHeader,
  ProgressBar,
  type DataTableColumn,
} from "@/shared/ui";

export function ClassManagementView() {
  const t = useT("classManagement");
  const classesQuery = useInstructorClasses();
  const classes = useMemo(() => classesQuery.data ?? [], [classesQuery.data]);

  const [formDialog, setFormDialog] = useState<ClassDialogState>(null);
  const router = useRouter();
  const [deleteTarget, setDeleteTarget] = useState<ClassSummary | null>(null);

  const createClass = useCreateClass();
  const updateClass = useUpdateClass();
  const deleteClass = useDeleteClass();

  const dayLabel = (day: number) => t(`schedule.dayShort.${day}`);
  const [search, setSearch] = useState("");
  const rows = useMemo(() => {
    const needle = search.trim().toLowerCase();
    return classes.filter(
      (klass) => !needle || klass.name.toLowerCase().includes(needle),
    );
  }, [classes, search]);

  const totalStudents = classes.reduce((sum, c) => sum + c.studentCount, 0);

  const columns: DataTableColumn<ClassSummary>[] = [
    {
      key: "name",
      header: t("classList.col.name"),
      render: (klass) => (
        <span className="block min-w-0">
          <Link
            href={`/instructor/classes/${klass.id}`}
            className="block font-semibold hover:underline"
          >
            {klass.name}
          </Link>
          {formatClassSchedule(klass.schedule, dayLabel) ? (
            <span className="block text-[12px] text-[var(--color-text-muted)]">
              {formatClassSchedule(klass.schedule, dayLabel)}
            </span>
          ) : null}
        </span>
      ),
    },
    {
      key: "studentCount",
      header: t("classList.col.studentHeader"),
      align: "right",
      render: (klass) => (
        <span className="font-mono">{klass.studentCount}</span>
      ),
    },
    {
      key: "completion",
      header: t("classList.col.completionBar"),
      render: (klass) => (
        <span className="flex min-w-[120px] items-center gap-2">
          <ProgressBar
            fill="var(--instructor-progress-fill, var(--color-admin-teal))"
            value={klass.completionPct}
            label={t("classList.col.completionBar")}
            className="flex-1"
          />
          <span className="w-9 text-right text-[12.5px] text-[var(--color-text-muted)]">
            {klass.completionPct}%
          </span>
        </span>
      ),
    },
    {
      key: "avgScore",
      header: t("classList.col.avgScore"),
      align: "right",
      render: (klass) => klass.avgScore ?? "-",
    },
    {
      key: "absentCount",
      header: t("classList.col.absentCount"),
      align: "right",
      render: (klass) => klass.absentCount,
    },
    {
      key: "actions",
      header: "",
      align: "right",
      render: (klass) => (
        <div className="flex justify-end gap-1.5">
          <IconAction
            icon={Eye}
            label={t("btnViewDetail")}
            ariaLabel={t("btnViewDetailOf", { name: klass.name })}
            href={`/instructor/classes/${klass.id}`}
          />
          <IconAction
            icon={Pencil}
            label={t("classList.col.menuEdit")}
            ariaLabel={t("classList.col.menuEditOf", { name: klass.name })}
            onClick={() => setFormDialog({ mode: "edit", target: klass })}
          />
          <IconAction
            icon={Trash2}
            tone="danger"
            label={t("classList.col.menuDelete")}
            ariaLabel={t("classList.col.menuDeleteOf", { name: klass.name })}
            onClick={() => setDeleteTarget(klass)}
          />
        </div>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t("header.title")}
        description={t("header.summary", {
          classCount: classes.length,
          studentCount: totalStudents,
        })}
        actions={
          <Button
            variant="cta"
            size="sm"
            onClick={() => setFormDialog({ mode: "create" })}
          >
            {t("header.btnCreateClass")}
          </Button>
        }
      />

      {/* Stat strip dropped 2026-09-27 (owner): every figure on it — số lớp, tổng học viên, cần
          chấm tay, điểm TB — is already the stat strip of instructor_overview, and repeating it on
          each screen pushed the actual worklist below the fold. These screens now open straight on
          their list + filter bar. Deliberate divergence from the mockup's own per-screen strip. */}
      <Card>
        <FilterBar
          search={{
            label: t("classList.search"),
            placeholder: t("classList.search"),
            value: search,
            onChange: setSearch,
          }}
          resultCount={t("classList.resultCount", {
            shown: rows.length,
            total: classes.length,
          })}
        />
        <DataTable
          caption={t("header.title")}
          columns={columns}
          rows={rows}
          rowKey={(row) => row.id}
          status={
            classesQuery.isLoading
              ? "loading"
              : classesQuery.isError
                ? "error"
                : "ready"
          }
          emptyMessage={t("classList.empty")}
          minWidth={860}
        />
      </Card>

      <ClassFormDialog
        key={
          formDialog === null
            ? "closed"
            : formDialog.mode === "edit"
              ? formDialog.target.id
              : "create"
        }
        dialog={formDialog}
        onClose={() => setFormDialog(null)}
        onSubmit={(form) => {
          const onError = () => toast.error(t("toast.failed"));
          if (formDialog?.mode === "edit") {
            updateClass.mutate(
              { id: formDialog.target.id, form },
              {
                onSuccess: () =>
                  toast.success(t("toast.updated", { name: form.name })),
                onError,
              },
            );
          } else {
            createClass.mutate(form, {
              onSuccess: (created) => {
                toast.success(t("toast.created", { name: form.name }));
                router.push(`/instructor/classes/${created.id}`);
              },
              onError,
            });
          }
          setFormDialog(null);
        }}
        t={t}
      />

      <ConfirmDialog
        open={deleteTarget !== null}
        onClose={() => setDeleteTarget(null)}
        onConfirm={() => {
          if (deleteTarget) {
            const name = deleteTarget.name;
            deleteClass.mutate(deleteTarget.id, {
              onSuccess: () => toast.success(t("toast.deleted", { name })),
              onError: () => toast.error(t("toast.failed")),
            });
          }
          setDeleteTarget(null);
        }}
        title={t("popup.deleteClassTitle", { name: deleteTarget?.name ?? "" })}
        confirmLabel={t("popup.deleteClassConfirm")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("popup.deleteClassBody")}
      </ConfirmDialog>
    </div>
  );
}
