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

const COLUMNS = ["time", "actor", "service", "category", "content", "id"] as const;
type Column = (typeof COLUMNS)[number];

export function ExportLogDialog({ open, onClose }: { open: boolean; onClose: () => void }) {
  const t = useT("adminSystemLog");
  const [picked, setPicked] = useState<ReadonlySet<Column>>(new Set(COLUMNS));
  const [from, setFrom] = useState("");
  const [to, setTo] = useState("");
  const [touched, setTouched] = useState(false);

  const rangeInvalid = from !== "" && to !== "" && from > to;

  function close() {
    setTouched(false);
    onClose();
  }

  function submit() {
    setTouched(true);
    if (picked.size === 0 || rangeInvalid) {
      toastFirstError([
        picked.size === 0 ? t("exportDialog.errorNoColumns") : undefined,
        rangeInvalid ? t("exportDialog.errorRange") : undefined,
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
          <Button variant="ghost" size="sm" className="border border-[var(--color-border)]" onClick={close}>
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
          <legend className="mb-1.5 text-[12.5px] font-semibold">{t("exportDialog.columns")}</legend>
          <div className="grid grid-cols-2 gap-1.5">
            {COLUMNS.map((column) => (
              <label key={column} className="flex items-center gap-2 text-[13px]">
                <input type="checkbox" checked={picked.has(column)} onChange={() => toggle(column)} />
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
            onChange={(event) => setFrom(event.target.value)}
            wrapperClassName="flex-1"
          />
          <TextField
            label={t("exportDialog.to")}
            type="date"
            value={to}
            onChange={(event) => setTo(event.target.value)}
            invalid={touched && rangeInvalid}
            wrapperClassName="flex-1"
          />
        </div>
        <p className="text-xs text-[var(--color-text-muted)]">{t("exportDialog.note")}</p>
      </div>
    </Modal>
  );
}
