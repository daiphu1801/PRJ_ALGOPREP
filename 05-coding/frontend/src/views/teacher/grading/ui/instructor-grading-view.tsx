// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Layout theo 09-layoutBase/Giáo viên - Chấm bài.dc.html (per 02-bd/screens/teacher/INS0301_grading.md
// Sheet 4.4): header row (:115-121), stat card strip (:122-133, cut to 3 cards — "Yêu cầu review" card
// dropped per DEC-2026-0830-remove-student-review-request, see BD Sheet 5 Khu vực B note at line 298-300),
// filter row — status tabs + class tabs + result count (:137-149), queue table header + rows (:152-168),
// grade popup (:176-191). Reads ?studentId=&classId= from the URL (set by class_student_detail's
// "Xem chấm bài của học viên này" link, Sheet 3.1) to seed the class tab and the dismissible student
// chip — no line range in the mockup, this entry path was added later (DEC-2026-0921-teacher-screens-
// conflict-resolutions), see BD Sheet 3.1 "Chi tiết học viên → Chấm tay".
//
// Deliberate divergence from the mockup, already resolved in RD/BD: the "Điểm" column shows both AI
// and manual score side by side ("AI: x/10 · GV: y/10") instead of overwriting the display like the
// mockup's data() (:213-222, 297-300) does — per RD Q3 / DEC-2026-0831-instructor-grading-round2. The
// mockup's "Yêu cầu review" string value in the score column (:214, 218) never appears — every row here
// is Accepted + low AI score, per DEC-2026-0830-remove-student-review-request.
"use client";

import { useState } from "react";
import { useSearchParams } from "next/navigation";
import { useInstructorClasses } from "@/entities/class";
import {
  useManualGradingQueue,
  useSaveManualGrade,
  type GradingStatusFilter,
  type ManualGradingItem,
} from "@/entities/manual-grading";
import { useT } from "@/shared/i18n";
import { toast, toastFirstError } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  DataTable,
  Modal,
  PageHeader,
  FilterMenu,
  TextArea,
  TextField,
  type DataTableColumn,
} from "@/shared/ui";

export function InstructorGradingView() {
  const t = useT("instructorGrading");
  const searchParams = useSearchParams();
  const initialClassId = searchParams.get("classId") ?? "all";
  const initialStudentId = searchParams.get("studentId");

  const classesQuery = useInstructorClasses();
  const classes = classesQuery.data ?? [];

  const [status, setStatus] = useState<GradingStatusFilter>("pending");
  const [classTab, setClassTab] = useState(initialClassId);
  const [studentChip, setStudentChip] = useState<string | null>(
    initialStudentId,
  );
  const [grading, setGrading] = useState<ManualGradingItem | null>(null);

  const queueQuery = useManualGradingQueue(
    status,
    classTab === "all" ? undefined : classTab,
    studentChip ?? undefined,
  );

  const columns: DataTableColumn<ManualGradingItem>[] = [
    {
      key: "student",
      header: t("queue.col.student"),
      render: (row) => row.studentName,
    },
    {
      key: "problem",
      header: t("queue.col.problem"),
      render: (row) => row.problemTitle,
    },
    {
      key: "className",
      header: t("queue.col.className"),
      render: (row) => row.className,
    },
    {
      key: "submittedAt",
      header: t("queue.col.submittedAt"),
      render: (row) => row.submittedAtLabel,
    },
    {
      key: "score",
      header: t("queue.col.score"),
      align: "right",
      render: (row) => (
        <span className="font-mono">
          {`AI: ${row.aiScore10}/10`}
          {row.manualScore !== null ? ` · GV: ${row.manualScore}/10` : ""}
        </span>
      ),
    },
    {
      key: "grade",
      header: "",
      align: "right",
      render: (row) => (
        <Button size="sm" onClick={() => setGrading(row)}>
          {t("queue.col.btnGrade")}
        </Button>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={t("header.title")}
        description={t("header.subtitle")}
      />

      {/* Stat strip dropped 2026-09-27 (owner): every figure on it — số lớp, tổng học viên, cần
          chấm tay, điểm TB — is already the stat strip of instructor_overview, and repeating it on
          each screen pushed the actual worklist below the fold. These screens now open straight on
          their list + filter bar. Deliberate divergence from the mockup's own per-screen strip. */}
      <Card>
        <div className="mb-3.5 flex flex-wrap items-center gap-2.5">
          <FilterMenu
            label={t("filter.statusTabs")}
            value={status}
            onValueChange={setStatus}
            options={[
              { value: "pending", label: t("statusFilter.pending") },
              { value: "graded", label: t("statusFilter.graded") },
              { value: "all", label: t("filterAll") },
            ]}
          />
          {classes.length > 1 ? (
            <FilterMenu
              label={t("filter.classTabs")}
              value={classTab}
              onValueChange={setClassTab}
              options={[
                { value: "all", label: t("filterAll") },
                ...classes.map((c) => ({ value: c.id, label: c.name })),
              ]}
            />
          ) : null}
          {studentChip ? (
            <Badge variant="blue">
              {t("filter.studentChip", { name: studentChip })}
              <button
                type="button"
                onClick={() => setStudentChip(null)}
                className="ml-1.5 font-bold"
                aria-label={t("filter.clearStudentChip")}
              >
                ×
              </button>
            </Badge>
          ) : null}
          <span className="ml-auto text-[12.5px] text-[var(--color-text-muted)]">
            {t("filter.resultCount", { count: queueQuery.data?.length ?? 0 })}
          </span>
        </div>

        <DataTable
          caption={t("header.title")}
          columns={columns}
          rows={queueQuery.data ?? []}
          rowKey={(row) => row.id}
          status={
            queueQuery.isLoading
              ? "loading"
              : queueQuery.isError
                ? "error"
                : "ready"
          }
          emptyMessage={t("empty")}
          minWidth={900}
        />
      </Card>

      <GradeDialog item={grading} onClose={() => setGrading(null)} t={t} />
    </div>
  );
}

function GradeDialog({
  item,
  onClose,
  t,
}: {
  item: ManualGradingItem | null;
  onClose: () => void;
  t: ReturnType<typeof useT>;
}) {
  const [score, setScore] = useState("");
  const [comment, setComment] = useState("");
  const [touched, setTouched] = useState(false);
  const saveGrade = useSaveManualGrade();

  if (!item) return null;

  const scoreValue = Number(score || item.manualScore || "");
  const scoreInvalid =
    Number.isNaN(scoreValue) || scoreValue < 0 || scoreValue > 10;

  function save() {
    if (!item) return;
    setTouched(true);
    if (scoreInvalid) {
      toastFirstError([t("validation.scoreRange")]);
      return;
    }
    saveGrade.mutate(
      {
        id: item.id,
        score: scoreValue,
        comment: comment || item.manualComment || "",
      },
      {
        onSuccess: () =>
          toast.success(t("toast.saved", { student: item.studentName })),
        onError: () => toast.error(t("toast.failed")),
      },
    );
    onClose();
  }

  return (
    <Modal
      open
      onClose={onClose}
      title={t("popup.title", {
        student: item.studentName,
        problem: item.problemTitle,
      })}
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={onClose}>
            {t("popup.btnCancel")}
          </Button>
          <Button size="sm" onClick={save}>
            {t("popup.btnSave")}
          </Button>
        </>
      }
    >
      <p className="mb-3 text-[12.5px] text-[var(--color-text-muted)]">
        {t("popup.meta", {
          ai: item.aiScore10,
          manual: item.manualScore ?? "chưa nhập",
        })}
      </p>
      <div className="flex flex-col gap-3">
        <TextField
          label={t("popup.scoreInput")}
          type="number"
          min={0}
          max={10}
          step={0.5}
          defaultValue={item.manualScore ?? undefined}
          invalid={touched && scoreInvalid}
          onChange={(event) => setScore(event.target.value)}
        />
        <TextArea
          label={t("popup.commentInput")}
          defaultValue={item.manualComment ?? ""}
          onChange={(event) => setComment(event.target.value)}
        />
      </div>
    </Modal>
  );
}
