// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Quản lý chủ đề" for interview questions (DEC-2026-1001-admin-configurable-settings): the shared
// ManagedListDialog plus one extra control per row — the STAR-framework switch that replaced the old
// hard link to the BEHAVIORAL topic.
"use client";

import {
  addInterviewTopic,
  removeInterviewTopic,
  renameInterviewTopic,
  setInterviewTopicStar,
  useInterviewTopics,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
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

  return (
    <ManagedListDialog
      open={open}
      onClose={onClose}
      items={topics}
      usage={usage}
      onAdd={addInterviewTopic}
      onRename={renameInterviewTopic}
      onRemove={removeInterviewTopic}
      renderExtra={(topic) => (
        <Toggle
          checked={topic.usesStarFramework}
          onCheckedChange={(checked) => setInterviewTopicStar(topic.key, checked)}
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
        error: {
          empty: t("topicManager.error.empty"),
          duplicate: t("topicManager.error.duplicate"),
        },
      }}
    />
  );
}
