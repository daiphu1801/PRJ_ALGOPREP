// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Giao cho lớp này" popup (DEC-2026-0831-class-assignments-round2): pick one class, tick bank
// problems, confirm. Lives on this screen rather than on the learner's problem_list, so the
// instructor never leaves the instructor shell. A problem already given to the chosen class is not
// offered again.
"use client";

import { useState } from "react";
import { useInstructorClasses } from "@/entities/class";
import {
  useAssignableProblems,
  useAssignProblems,
} from "@/entities/class-assignment";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { useDebounced } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import { Button, Modal, Pagination, SelectField, TextField } from "@/shared/ui";

type Props = {
  open: boolean;
  onClose: () => void;
  /** Class preselected from the screen's class filter, when one is chosen. */
  defaultClassId?: string;
};

export function AssignProblemDialog({ open, onClose, defaultClassId }: Props) {
  const t = useT("classAssignments");
  const classes = useInstructorClasses().data ?? [];
  const [pickedClass, setPickedClass] = useState<string | null>(null);
  const [search, setSearch] = useState("");
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [page, setPage] = useState(1);
  const assign = useAssignProblems();
  const apiMessage = useApiErrorMessage();
  const q = useDebounced(search.trim());

  // Falls back to the preselected class, then the first one, so the select never shows a stale choice.
  const classId = classes.some((c) => c.id === pickedClass)
    ? pickedClass!
    : classes.some((c) => c.id === defaultClassId)
      ? defaultClassId!
      : (classes[0]?.id ?? "");
  // The server filters by title and pages by 20; a ticked problem stays ticked on another page.
  const candidates = useAssignableProblems(classId, q, page);
  const rows = candidates.data?.items ?? [];
  const pageMeta = candidates.data?.page;

  function close() {
    setSelected(new Set());
    setSearch("");
    setPickedClass(null);
    setPage(1);
    onClose();
  }

  function toggle(id: string) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (!next.delete(id)) next.add(id);
      return next;
    });
  }

  const className = classes.find((c) => c.id === classId)?.name ?? "";

  function confirm() {
    assign.mutate(
      { classId, problemIds: [...selected] },
      {
        onSuccess: (result) => {
          // One call can mix outcomes: new, already given, refused (PRB-037 not published, PRB-010 gone...).
          if (result.succeeded.length > 0 || result.failed.length === 0)
            toast.success(
              t("toast.assigned", {
                count: result.succeeded.length,
                className,
              }),
            );
          if (result.skipped.length > 0)
            toast.info(
              t("toast.assignSkipped", { count: result.skipped.length }),
            );
          for (const code of new Set(result.failed.map((item) => item.code)))
            toast.warning(
              `${t("toast.assignRefused", {
                count: result.failed.filter((item) => item.code === code)
                  .length,
              })} ${apiMessage(new ApiError(code, 422))}`,
            );
        },
        onError: (error) => toast.error(apiMessage(error)),
      },
    );
    close();
  }

  return (
    <Modal
      open={open}
      onClose={close}
      title={t("assign.title")}
      footer={
        <>
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            onClick={close}
          >
            {t("cancel")}
          </Button>
          <Button
            variant="cta"
            size="sm"
            disabled={selected.size === 0 || classId === ""}
            onClick={confirm}
          >
            {t("assign.confirm", { count: selected.size })}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3">
        <SelectField
          label={t("assign.classLabel")}
          value={classId}
          onChange={(event) => {
            setPickedClass(event.target.value);
            setPage(1);
            setSelected(new Set());
          }}
          options={classes.map((c) => ({ value: c.id, label: c.name }))}
        />
        <TextField
          label={t("assignmentList.search")}
          hideLabel
          placeholder={t("assignmentList.search")}
          value={search}
          onChange={(event) => {
            setSearch(event.target.value);
            setPage(1);
          }}
        />
        <ul
          aria-label={t("assign.listLabel")}
          className="max-h-64 overflow-y-auto rounded-lg border border-[var(--color-border)]"
        >
          {rows.length === 0 ? (
            <li className="p-3 text-sm text-[var(--color-text-muted)]">
              {candidates.isLoading ? t("assign.loading") : t("assign.empty")}
            </li>
          ) : (
            rows.map((p) => (
              <li
                key={p.id}
                className="border-b border-[var(--color-border)] last:border-0"
              >
                <label className="flex cursor-pointer items-center gap-3 px-3 py-2 text-sm hover:bg-[var(--color-surface-hover)]">
                  <input
                    type="checkbox"
                    checked={selected.has(p.id)}
                    onChange={() => toggle(p.id)}
                  />
                  <span className="flex-1">{p.title}</span>
                  <span className="text-xs text-[var(--color-text-muted)]">
                    {p.topic}
                  </span>
                </label>
              </li>
            ))
          )}
        </ul>
        {pageMeta && pageMeta.totalPages > 1 ? (
          <Pagination
            page={pageMeta.number}
            pageSize={pageMeta.size}
            total={pageMeta.totalItems}
            onPageChange={setPage}
            summary={t("pageSummary", {
              page: pageMeta.number,
              total: pageMeta.totalPages,
            })}
            previousLabel={t("previous")}
            nextLabel={t("next")}
          />
        ) : null}
      </div>
    </Modal>
  );
}
