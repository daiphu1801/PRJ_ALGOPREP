// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Quản lý độ khó" for problems (DEC-2026-1001-admin-configurable-settings, round 6): the shared
// ManagedListDialog over the problem level list, same shape as the topic manager. At least one level
// must remain: the authoring form needs something to pick.
"use client";

import {
  addProblemLevel,
  moveProblemLevel,
  removeProblemLevel,
  renameProblemLevel,
  useProblemLevels,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { ManagedListDialog } from "@/shared/ui";

type Props = {
  open: boolean;
  onClose: () => void;
  /** Problem count per level key, from the list the screen is showing. */
  usage: Readonly<Record<string, number>>;
};

export function LevelManagerDialog({ open, onClose, usage }: Props) {
  const t = useT("problemManagement");
  const levels = useProblemLevels();

  return (
    <ManagedListDialog
      open={open}
      onClose={onClose}
      items={levels}
      usage={usage}
      onAdd={addProblemLevel}
      onRename={renameProblemLevel}
      onRemove={removeProblemLevel}
      onMove={moveProblemLevel}
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
