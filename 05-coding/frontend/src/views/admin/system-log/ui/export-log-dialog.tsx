// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Tải nhật ký" (ADM0403 Q4, DEC-2026-1001-admin-configurable-settings): CSV, the admin picks the
// columns and the date range, no row cap (the server streams it). Each export is itself logged as one
// row. No endpoint yet, so the dialog only toasts what would be exported.
"use client";

import { useState } from "react";
import { useT } from "@/shared/i18n";
import { toast, toastFirstError } from "@/shared/lib/toast-store";
import { Button, Modal, TextField } from "@/shared/ui";
import { logSettings } from "../model/settings";

const COLUMNS = [
  "time",
  "actor",
  "service",
  "category",
  "content",
  "id",
] as const;
type Column = (typeof COLUMNS)[number];

/** Filters on the main screen when the dialog opens: the export follows them (ADM0403 Q8). */
export type ExportScope = {
  query: string;
  categoryLabel: string | null;
  from: string;
  to: string;
};

const DAY_MS = 86_400_000;
const dayString = (offsetDays: number) =>
  new Date(Date.now() + offsetDays * DAY_MS).toISOString().slice(0, 10);

export function ExportLogDialog({
  open,
  onClose,
  scope = { query: "", categoryLabel: null, from: "", to: "" },
}: {
  open: boolean;
  onClose: () => void;
  scope?: ExportScope;
}) {
  const t = useT("adminSystemLog");
  const { retentionDays } = logSettings.use();
  const [picked, setPicked] = useState<ReadonlySet<Column>>(new Set(COLUMNS));
  const [from, setFrom] = useState(scope.from);
  const [to, setTo] = useState(scope.to);
  const [touched, setTouched] = useState(false);

  const today = dayString(0);
  const oldest = dayString(-retentionDays);
  const rangeInvalid = from !== "" && to !== "" && from > to;
  // BD Sheet 9 NO 5-6: nothing older than the retention period exists, nothing newer than today yet.
  const futureInvalid = from > today || to > today;
  const tooOldInvalid = from !== "" && from < oldest;
  const dateInvalid = rangeInvalid || futureInvalid || tooOldInvalid;
  const scopeParts = [
    scope.query ? t("exportDialog.scopeQuery", { query: scope.query }) : null,
    scope.categoryLabel
      ? t("exportDialog.scopeCategory", { category: scope.categoryLabel })
      : null,
  ].filter(Boolean);

  function close() {
    setTouched(false);
    onClose();
  }

  function submit() {
    setTouched(true);
    if (picked.size === 0 || dateInvalid) {
      toastFirstError([
        picked.size === 0 ? t("exportDialog.errorNoColumns") : undefined,
        rangeInvalid ? t("exportDialog.errorRange") : undefined,
        futureInvalid ? t("exportDialog.errorFuture") : undefined,
        tooOldInvalid
          ? t("exportDialog.errorRetention", { days: retentionDays })
          : undefined,
      ]);
      return;
    }
    toast.success(t("exportDialog.doneBody", { count: picked.size }));
    close();
  }

  function toggle(column: Column) {
    setPicked((previous) => {
      const next = new Set(previous);
      if (next.has(column)) next.delete(column);
      else next.add(column);
      return next;
    });
  }

  return (
    <Modal
      open={open}
      onClose={close}
      title={t("exportDialog.title")}
      footer={
        <>
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            onClick={close}
          >
            {t("exportDialog.cancel")}
          </Button>
          <Button variant="cta" size="sm" onClick={submit}>
            {t("exportDialog.submit")}
          </Button>
        </>
      }
    >
      <div className="flex flex-col gap-3.5">
        <fieldset>
          <legend className="mb-1.5 text-[12.5px] font-semibold">
            {t("exportDialog.columns")}
          </legend>
          <div className="grid grid-cols-2 gap-1.5">
            {COLUMNS.map((column) => (
              <label
                key={column}
                className="flex items-center gap-2 text-[13px]"
              >
                <input
                  type="checkbox"
                  checked={picked.has(column)}
                  onChange={() => toggle(column)}
                />
                {t(`exportDialog.col.${column}`)}
              </label>
            ))}
          </div>
        </fieldset>
        <div className="flex gap-3">
          <TextField
            label={t("exportDialog.from")}
            type="date"
            value={from}
            max={today}
            onChange={(event) => setFrom(event.target.value)}
            invalid={touched && (futureInvalid || tooOldInvalid) && from !== ""}
            wrapperClassName="flex-1"
          />
          <TextField
            label={t("exportDialog.to")}
            type="date"
            value={to}
            max={today}
            onChange={(event) => setTo(event.target.value)}
            invalid={touched && (rangeInvalid || to > today)}
            wrapperClassName="flex-1"
          />
        </div>
        {scopeParts.length ? (
          <p className="text-xs font-semibold">
            {t("exportDialog.scopeTitle")} {scopeParts.join(", ")}
          </p>
        ) : null}
        <p className="text-xs text-[var(--color-text-muted)]">
          {t("exportDialog.note")}
        </p>
      </div>
    </Modal>
  );
}
