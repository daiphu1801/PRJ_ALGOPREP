// F6 #9, #11 to #14 (03-dd/api/F6_interview-bank.md); in-memory store in mock mode.
//
// "Quản lý chủ đề" for interview questions (DEC-2026-1001-admin-configurable-settings): the shared
// ManagedListDialog plus one extra control per row — the STAR-framework switch that replaced the old
// hard link to the BEHAVIORAL topic.
"use client";

import {
  useInterviewTopicActions,
  useInterviewTopics,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { ManagedListDialog, Toggle } from "@/shared/ui";

type Props = {
  open: boolean;
  onClose: () => void;
  /** Question count per topic key, from the list the screen is showing. */
  usage: Readonly<Record<string, number>>;
};

export function TopicManagerDialog({ open, onClose, usage }: Props) {
  const t = useT("interviewQuestionManagement");
  const topics = useInterviewTopics();
  const actions = useInterviewTopicActions();

  return (
    <ManagedListDialog
      open={open}
      onClose={onClose}
      items={topics}
      usage={usage}
      onAdd={actions.add}
      onRename={actions.rename}
      onRemove={actions.remove}
      onMove={actions.move}
      renderExtra={(topic) => (
        <Toggle
          checked={topic.usesStarFramework}
          onCheckedChange={async (checked) => {
            // A refused write already showed its own error toast; no success toast then.
            if ((await actions.setStar(topic.key, checked)) === "reported")
              return;
            toast.success(
              t(
                checked
                  ? "topicManager.starEnabled"
                  : "topicManager.starDisabled",
                {
                  name: topic.label,
                },
              ),
            );
          }}
          label={t("topicManager.starLabel", { name: topic.label })}
        />
      )}
      labels={{
        title: t("topicManager.title"),
        hint: t("topicManager.hint"),
        nameLabel: t("topicManager.nameLabel"),
        newLabel: t("topicManager.newLabel"),
        newPlaceholder: t("topicManager.newPlaceholder"),
        add: t("topicManager.add"),
        save: t("topicManager.save"),
        delete: t("delete"),
        close: t("topicManager.close"),
        usage: (count) => t("topicManager.usage", { count }),
        deleteBlocked: (count) => t("topicManager.deleteBlocked", { count }),
        moveUp: t("topicManager.moveUp"),
        moveDown: t("topicManager.moveDown"),
        error: {
          empty: t("topicManager.error.empty"),
          duplicate: t("topicManager.error.duplicate"),
        },
        done: {
          add: t("topicManager.done.add"),
          rename: t("topicManager.done.rename"),
          remove: t("topicManager.done.remove"),
        },
      }}
    />
  );
}
