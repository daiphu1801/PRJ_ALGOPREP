// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Sao chép sang lớp khác" of INS0202 (F2-12, DEC-2026-1125-copy-class-assignments): gives every problem of one
// class to another class of the instructor in one confirmation. Reads both classes (#49), shows what will happen,
// then sends #52. Mounted only while open, so each opening starts clean.
"use client";

import { useCallback, useState } from "react";
import {
  assignInBatches,
  copyPlan,
  useAssignProblems,
  useClassAssignedProblems,
} from "@/entities/class-assignment";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, ConfirmDialog, FilterMenu } from "@/shared/ui";

type ClassOption = { id: string; name: string };

type CopyAssignmentsDialogProps = {
  source: ClassOption;
  /** The instructor's other classes; at least one. */
  targets: ClassOption[];
  onClose: () => void;
  onCopied: (targetId: string) => void;
};

const NAMED_MAX = 3;

export function CopyAssignmentsDialog({
  source,
  targets,
  onClose,
  onCopied,
}: CopyAssignmentsDialogProps) {
  const t = useT("classAssignments");
  const apiMessage = useApiErrorMessage();
  const [targetId, setTargetId] = useState(
    targets.length === 1 ? targets[0]!.id : "",
  );
  const [sending, setSending] = useState(false);
  const targetName = targets.find((c) => c.id === targetId)?.name ?? "";

  const sourceQuery = useClassAssignedProblems(source.id);
  const targetQuery = useClassAssignedProblems(targetId); // disabled while ""
  const { mutateAsync: assign } = useAssignProblems();

  const loadError =
    sourceQuery.isError || (targetId !== "" && targetQuery.isError);
  const plan =
    !loadError && sourceQuery.data && targetId !== "" && targetQuery.data
      ? copyPlan(sourceQuery.data, targetQuery.data)
      : null;
  // A refetch in flight may still change the plan: a retry must only send what is missing.
  const settled = !sourceQuery.isFetching && !targetQuery.isFetching;

  // Stable between renders: Modal moves the focus again whenever `onClose` changes.
  const close = useCallback(() => {
    if (!sending) onClose();
  }, [sending, onClose]);

  async function confirm() {
    if (!plan || sending) return;
    setSending(true);
    const titles = new Map(plan.toAssign.map((row) => [row.id, row.title]));
    const result = await assignInBatches(
      plan.toAssign.map((row) => row.id),
      (problemIds) => assign({ classId: targetId, problemIds }),
    );
    setSending(false);

    for (const code of new Set(result.failed.map((item) => item.code))) {
      const names = result.failed
        .filter((item) => item.code === code)
        .map((item) => titles.get(item.id) ?? item.id);
      const shown = names.slice(0, NAMED_MAX).join(", ");
      const more = names.length - NAMED_MAX;
      toast.warning(
        `${t("copy.refused", {
          count: names.length,
          titles: more > 0 ? `${shown} +${more}` : shown,
        })} ${apiMessage(new ApiError(code, 422))}`,
      );
    }

    if (result.error !== undefined) {
      toast.error(
        result.succeeded.length > 0
          ? `${t("copy.partial", {
              done: result.succeeded.length,
              total: plan.toAssign.length,
            })} ${apiMessage(result.error)}`
          : apiMessage(result.error),
      );
      // The answer may be lost after the server did the work: read the class again before a retry.
      void targetQuery.refetch();
      return;
    }
    if (result.skipped.length > 0)
      toast.info(t("copy.skipped", { count: result.skipped.length }));
    if (result.succeeded.length > 0)
      toast.success(
        t("copy.done", {
          count: result.succeeded.length,
          className: targetName,
        }),
      );
    if (result.succeeded.length + result.skipped.length > 0) onCopied(targetId);
  }

  const count = plan?.toAssign.length ?? 0;
  const rows: [string, number][] =
    plan && sourceQuery.data
      ? [
          [
            t("copy.sourceCount", { className: source.name }),
            sourceQuery.data.length,
          ],
          [t("copy.alreadyHas", { className: targetName }), plan.alreadyHas],
          [t("copy.withdrawn"), plan.withdrawn],
        ]
      : [];

  return (
    <ConfirmDialog
      open
      onClose={close}
      onConfirm={() => void confirm()}
      title={t("copy.title", { className: source.name })}
      confirmLabel={
        sending
          ? t("copy.sending")
          : count > 0
            ? t("copy.confirm", { count })
            : t("copy.confirmEmpty")
      }
      cancelLabel={t("cancel")}
      pending={sending}
      confirmDisabled={count === 0 || !settled}
    >
      <FilterMenu
        label={t("copy.target")}
        compact
        className="w-full [&>button]:h-10 [&>button]:w-full [&>button]:justify-between [&>button]:bg-[var(--color-surface)] [&>button]:px-3 [&>button]:text-sm [&>button]:text-[var(--color-text)] [&>[role=listbox]]:w-full [&>[role=listbox]]:max-w-full"
        value={targetId}
        onValueChange={(value) => {
          if (!sending) setTargetId(value);
        }}
        options={[
          // The placeholder is offered only until a class is picked, so it can never be chosen again.
          ...(targetId === ""
            ? [{ value: "", label: t("copy.targetPlaceholder") }]
            : []),
          ...targets.map((c) => ({ value: c.id, label: c.name })),
        ]}
      />
      <div className="mt-3" aria-live="polite">
        {loadError ? (
          <div className="flex flex-wrap items-center gap-2">
            <span className="text-[var(--color-danger)]">
              {t("loadFailed")}
            </span>
            <Button
              variant="ghost"
              size="sm"
              className="border border-[var(--color-border)]"
              onClick={() => {
                if (sourceQuery.isError) void sourceQuery.refetch();
                if (targetQuery.isError) void targetQuery.refetch();
              }}
            >
              {t("copy.retry")}
            </Button>
          </div>
        ) : plan ? (
          <>
            <dl className="grid gap-1.5">
              {rows.map(([label, value]) => (
                <div key={label} className="flex justify-between gap-3">
                  <dt>{label}</dt>
                  <dd className="font-mono">{value}</dd>
                </div>
              ))}
              <div className="flex justify-between gap-3 border-t border-[var(--color-border)] pt-1.5 font-semibold text-[var(--color-text)]">
                <dt>{t("copy.willAssign")}</dt>
                <dd className="font-mono">{count}</dd>
              </div>
            </dl>
            {count === 0 && !sending && settled ? (
              <p className="mt-3">
                {t("copy.nothingLeft", { className: targetName })}
              </p>
            ) : null}
          </>
        ) : targetId !== "" ? (
          <p>{t("copy.loading")}</p>
        ) : null}
      </div>
    </ConfirmDialog>
  );
}
