// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Giao cho lớp này" popup (DEC-2026-0831-class-assignments-round2): pick one class, tick bank
// problems, confirm. Lives on this screen rather than on the learner's problem_list, so the
// instructor never leaves the instructor shell. A problem already given to the chosen class is not
// offered again.
"use client";

import { useMemo, useState } from "react";
import { useInstructorClasses } from "@/entities/class";
import {
  useAssignableProblems,
  useAssignProblems,
} from "@/entities/class-assignment";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, Modal, SelectField, TextField } from "@/shared/ui";

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
  const assign = useAssignProblems();

  // Falls back to the preselected class, then the first one, so the select never shows a stale choice.
  const classId = classes.some((c) => c.id === pickedClass)
    ? pickedClass!
    : classes.some((c) => c.id === defaultClassId)
      ? defaultClassId!
      : (classes[0]?.id ?? "");
  const candidates = useAssignableProblems(classId);

  const rows = useMemo(() => {
    const needle = search.trim().toLowerCase();
    return (candidates.data ?? []).filter(
      (p) => !needle || p.title.toLowerCase().includes(needle),
    );
  }, [candidates.data, search]);

  function close() {
    setSelected(new Set());
    setSearch("");
    setPickedClass(null);
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
    const count = selected.size;
    assign.mutate(
      { classId, problemIds: [...selected] },
      {
        onSuccess: () =>
          toast.success(t("toast.assigned", { count, className })),
        onError: () => toast.error(t("toast.assignFailed")),
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
            setSelected(new Set());
          }}
          options={classes.map((c) => ({ value: c.id, label: c.name }))}
        />
        <TextField
          label={t("assignmentList.search")}
          hideLabel
          placeholder={t("assignmentList.search")}
          value={search}
          onChange={(event) => setSearch(event.target.value)}
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
      </div>
    </Modal>
  );
}
