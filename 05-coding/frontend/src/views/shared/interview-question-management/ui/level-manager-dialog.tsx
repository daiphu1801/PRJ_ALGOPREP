// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Quản lý độ khó" for interview questions (DEC-2026-1001-admin-configurable-settings): the shared
// ManagedListDialog, same shape as the topic manager but without a per-row extra control. At least one
// level must remain (DEC-2026-1001, round 5): the authoring form needs something to pick.
"use client";

import {
  addInterviewLevel,
  moveInterviewLevel,
  removeInterviewLevel,
  renameInterviewLevel,
  useInterviewLevels,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import { ManagedListDialog } from "@/shared/ui";

type Props = {
  open: boolean;
  onClose: () => void;
  /** Question count per level key, from the list the screen is showing. */
  usage: Readonly<Record<string, number>>;
};

export function LevelManagerDialog({ open, onClose, usage }: Props) {
  const t = useT("interviewQuestionManagement");
  const levels = useInterviewLevels();

  return (
    <ManagedListDialog
      open={open}
      onClose={onClose}
      items={levels}
      usage={usage}
      onAdd={addInterviewLevel}
      onRename={renameInterviewLevel}
      onRemove={removeInterviewLevel}
      onMove={moveInterviewLevel}
      keepAtLeast={1}
      labels={{
        title: t("levelManager.title"),
        hint: t("levelManager.hint"),
        nameLabel: t("levelManager.nameLabel"),
        newLabel: t("levelManager.newLabel"),
        newPlaceholder: t("levelManager.newPlaceholder"),
        add: t("levelManager.add"),
        save: t("levelManager.save"),
        delete: t("delete"),
        close: t("levelManager.close"),
        usage: (count) => t("levelManager.usage", { count }),
        deleteBlocked: (count) => t("levelManager.deleteBlocked", { count }),
        deleteLast: t("levelManager.deleteLast"),
        moveUp: t("levelManager.moveUp"),
        moveDown: t("levelManager.moveDown"),
        error: {
          empty: t("levelManager.error.empty"),
          duplicate: t("levelManager.error.duplicate"),
        },
        done: {
          add: t("levelManager.done.add"),
          rename: t("levelManager.done.rename"),
          remove: t("levelManager.done.remove"),
        },
      }}
    />
  );
}
