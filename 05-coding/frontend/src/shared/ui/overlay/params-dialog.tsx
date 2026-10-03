// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Small settings dialog for ADMIN-tunable parameters (thresholds, retention, limits —
// DEC-2026-1001-admin-configurable-settings). Number fields take a whole number at or above `min`;
// there is no upper bound, because the point of these parameters is that the admin decides. Text
// arrives through `labels`, so this stays free of any one screen's i18n namespace.
"use client";

import { useState } from "react";
import { toast, toastFirstError } from "@/shared/lib/toast-store";
import { Button } from "../primitives/button";
import { Modal } from "./modal";
import { SelectField } from "../form/select-field";
import { TextField } from "../form/text-field";

export type ParamField =
  | { type: "number"; key: string; label: string; hint?: string; unit?: string; min: number }
  | {
      type: "select";
      key: string;
      label: string;
      hint?: string;
      options: { value: string; label: string }[];
    };

export type ParamValues = Record<string, string>;

export type ParamsDialogLabels = {
  save: string;
  cancel: string;
  /** Success toast after a valid save, e.g. "Đã lưu cài đặt". */
  saved: string;
  /** Error text for a number below its minimum, e.g. "Nhập số nguyên từ 1 trở lên". */
  errorMin: (min: number) => string;
};

type Props = {
  open: boolean;
  onClose: () => void;
  title: string;
  fields: readonly ParamField[];
  values: ParamValues;
  onSave: (values: ParamValues) => void;
  labels: ParamsDialogLabels;
};

function ParamsForm({
  onClose,
  fields,
  values,
  onSave,
  labels,
}: Omit<Props, "open" | "title">) {
  const [draft, setDraft] = useState<ParamValues>(values);
  const [touched, setTouched] = useState(false);

  const errorFor = (field: ParamField) => {
    if (field.type !== "number") return undefined;
    const value = Number(draft[field.key]);
    return Number.isInteger(value) && value >= field.min ? undefined : labels.errorMin(field.min);
  };
  const hasError = fields.some((field) => errorFor(field));

  function save() {
    setTouched(true);
    if (hasError) {
      toastFirstError(fields.map(errorFor));
      return;
    }
    onSave(draft);
    toast.success(labels.saved);
    onClose();
  }

  return (
    <>
      <div className="flex flex-col gap-3.5">
        {fields.map((field) => {
          const change = (value: string) => setDraft((previous) => ({ ...previous, [field.key]: value }));
          return field.type === "number" ? (
            <div key={field.key}>
              <TextField
                label={field.unit ? `${field.label} (${field.unit})` : field.label}
                type="number"
                min={field.min}
                value={draft[field.key] ?? ""}
                onChange={(event) => change(event.target.value)}
                invalid={touched && Boolean(errorFor(field))}
              />
              {field.hint ? (
                <p className="mt-1 text-xs text-[var(--color-text-muted)]">{field.hint}</p>
              ) : null}
            </div>
          ) : (
            <div key={field.key}>
              <SelectField
                label={field.label}
                value={draft[field.key] ?? ""}
                onChange={(event) => change(event.target.value)}
                options={field.options}
              />
              {field.hint ? (
                <p className="mt-1 text-xs text-[var(--color-text-muted)]">{field.hint}</p>
              ) : null}
            </div>
          );
        })}
      </div>
      <div className="mt-5 flex justify-end gap-2">
        <Button variant="ghost" size="sm" className="border border-[var(--color-border)]" onClick={onClose}>
          {labels.cancel}
        </Button>
        <Button variant="cta" size="sm" onClick={save}>
          {labels.save}
        </Button>
      </div>
    </>
  );
}

export function ParamsDialog({ open, onClose, title, ...form }: Props) {
  // The form mounts only while the dialog is open, so each opening starts from the saved values.
  return (
    <Modal open={open} onClose={onClose} title={title}>
      <ParamsForm onClose={onClose} {...form} />
    </Modal>
  );
}
