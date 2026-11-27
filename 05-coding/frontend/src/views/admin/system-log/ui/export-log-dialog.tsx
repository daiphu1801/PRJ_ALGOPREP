// "Tải nhật ký" (ADM0403 Q4, DEC-2026-1001-admin-configurable-settings): CSV, the admin picks the
// columns and the date range, no row cap (the server streams it, F1 #40). Each export is itself logged
// as one row. The file is the server's: the screen never builds a CSV from the rows it holds. In mock
// mode there is no file, so the dialog only toasts what would be exported.
"use client";

import { useState } from "react";
import { EXPORT_COLUMNS, useExportAuditLog, type AuditCategory } from "../api";
import type { ExportColumn } from "../api";
import { useApiErrorMessage, useLocale, useT } from "@/shared/i18n";
import { saveBlob } from "@/shared/lib";
import { toast, toastFirstError } from "@/shared/lib/toast-store";
import { Button, Modal, TextField } from "@/shared/ui";

const COLUMNS = EXPORT_COLUMNS;
type Column = ExportColumn;

/** Filters on the main screen when the dialog opens: the export follows them (ADM0403 Q8). */
export type ExportScope = {
  query: string;
  category: AuditCategory | "all";
  categoryLabel: string | null;
  from: string;
  to: string;
};

const DAY_MS = 86_400_000;
const dayString = (offsetDays: number) =>
  new Date(Date.now() + offsetDays * DAY_MS).toISOString().slice(0, 10);
const fileName = () =>
  `audit-logs-${new Date(Date.now() + 7 * 3_600_000)
    .toISOString()
    .slice(0, 16)
    .replace(/[-:]/g, "")
    .replace("T", "-")}.csv`;

export function ExportLogDialog({
  open,
  onClose,
  retentionDays,
  scope = { query: "", category: "all", categoryLabel: null, from: "", to: "" },
}: {
  open: boolean;
  onClose: () => void;
  /** F1 #51: nothing older than this exists, so the range cannot start before it (BD Sheet 9 NO 5-6). */
  retentionDays: number;
  scope?: ExportScope;
}) {
  const t = useT("adminSystemLog");
  const toMessage = useApiErrorMessage();
  const locale = useLocale() === "en" ? "en" : "vi";
  const exportLog = useExportAuditLog();
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

  async function submit() {
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
    try {
      const file = await exportLog.mutateAsync({
        params: {
          query: scope.query,
          category: scope.category,
          from,
          to,
        },
        columns: COLUMNS.filter((column) => picked.has(column)),
        locale,
      });
      if (file) saveBlob(file, fileName());
    } catch (error) {
      // The dialog and the filters stay, and no empty file is passed off as a success.
      toast.error(toMessage(error));
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
          <Button
            variant="cta"
            size="sm"
            disabled={exportLog.isPending}
            onClick={() => void submit()}
          >
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
