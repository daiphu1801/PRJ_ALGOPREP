// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Picker for the bulk action "Đổi độ khó" (SHR0201): choose one level from the admin-managed list and
// apply it to every selected problem. This is also how an admin frees a level before deleting it.
"use client";

import { useState } from "react";
import { useProblemLevels } from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { Button, Modal, SelectField } from "@/shared/ui";

type Props = {
  open: boolean;
  onClose: () => void;
  /** Number of selected problems, shown in the title. */
  count: number;
  onApply: (levelKey: string) => void;
};

export function BulkLevelDialog({ open, onClose, count, onApply }: Props) {
  const t = useT("problemManagement");
  const levels = useProblemLevels();
  const [picked, setPicked] = useState<string | null>(null);
  // Falls back to the first level so the select never shows a stale or empty choice.
  const value = levels.some((level) => level.key === picked)
    ? picked!
    : (levels[0]?.key ?? "");

  return (
    <Modal
      open={open}
      onClose={onClose}
      title={t("bulkLevel.title", { count })}
      footer={
        <>
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            onClick={onClose}
          >
            {t("bulkLevel.cancel")}
          </Button>
          <Button
            variant="cta"
            size="sm"
            onClick={() => onApply(value)}
            disabled={value === ""}
          >
            {t("bulkLevel.apply")}
          </Button>
        </>
      }
    >
      <SelectField
        label={t("bulkLevel.label")}
        value={value}
        onChange={(event) => setPicked(event.target.value)}
        options={levels.map((level) => ({
          value: level.key,
          label: level.label,
        }))}
      />
    </Modal>
  );
}
