// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// THE ONE ADMIN SCREEN WITH NO PROTOTYPE. 09-layoutBase has no mockup for it and the BD says so
// outright, describing the layout by structure only
// [SoT: 02-bd/screens/shared/interview_question_authoring.md:7-13]. So this build follows the BD
// rather than a picture, reuses the tokens and primitives the other Admin screens already
// established, and needs the owner to approve how it looks — there is nothing to compare against.
//
// BD rules this screen turns on:
// - Sticky header: back, identity, save indicator, "Xem như học viên", ONE "Lưu" button. There is
//   no draft/publish split — a question is live the moment it is saved (BD section 1.1, RD Q5).
// - Four field groups stacked vertically, no tabs (BD section 1, Q-BD1 — it asks the owner to
//   confirm this, so it is in the phase report).
// - Rubric weights: an EMPTY rubric is valid and saves fine (the question just stays out of
//   practice mode). A non-empty rubric whose weights do not total 100 BLOCKS saving
//   (BD section 1.4, `answer_rubrics` enforces it at application level).
// - Topic is one of five seeded values; this screen cannot invent a sixth (BD section 1.2).
"use client";

import { useState } from "react";
import Link from "next/link";
import {
  QUESTION_LEVELS,
  QUESTION_TOPICS,
  type QuestionLevel,
  type QuestionTopic,
  type RubricCriterion,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import {
  Button,
  Card,
  ConfirmDialog,
  NoticeTile,
  NumberStepper,
  PageHeader,
  SelectField,
  TextArea,
  TextField,
} from "@/shared/ui";

type Draft = {
  question: string;
  topic: QuestionTopic;
  level: QuestionLevel;
  followUps: string[];
  rubric: RubricCriterion[];
};

const EMPTY_DRAFT: Draft = {
  question: "",
  topic: "csTheory",
  level: "medium",
  followUps: [""],
  rubric: [],
};

export function InterviewQuestionAuthoringView() {
  const t = useT("interviewQuestionAuthoring");
  const [draft, setDraft] = useState<Draft>(EMPTY_DRAFT);
  const [saving, setSaving] = useState(false);
  const [savedAt, setSavedAt] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const rubricTotal = draft.rubric.reduce((sum, criterion) => sum + criterion.weight, 0);
  // Empty is fine; non-empty must add to exactly 100.
  const rubricBlocksSave = draft.rubric.length > 0 && rubricTotal !== 100;
  const canSave = draft.question.trim().length > 0 && !rubricBlocksSave && !saving;

  function patch(changes: Partial<Draft>) {
    setDraft((previous) => ({ ...previous, ...changes }));
    setSavedAt(null);
  }

  function setFollowUp(index: number, value: string) {
    patch({ followUps: draft.followUps.map((item, i) => (i === index ? value : item)) });
  }

  function setCriterion(index: number, changes: Partial<RubricCriterion>) {
    patch({
      rubric: draft.rubric.map((item, i) => (i === index ? { ...item, ...changes } : item)),
    });
  }

  async function save() {
    setSaving(true);
    // No endpoint yet — the delay stands in for the round trip so the saving state is reviewable.
    await new Promise((resolve) => setTimeout(resolve, 500));
    setSaving(false);
    setSavedAt(new Date().toLocaleTimeString("vi-VN"));
  }

  return (
    <div>
      <PageHeader
        title={t("titleNew")}
        description={t("subtitle")}
        actions={
          <>
            <Button asChild variant="ghost" size="sm" className="border border-[var(--color-border)]">
              <Link href="/admin/interview-questions">{t("back")}</Link>
            </Button>
            <span
              aria-live="polite"
              className="text-xs whitespace-nowrap text-[var(--color-text-subtle)]"
            >
              {saving ? t("saving") : savedAt ? t("savedAt", { time: savedAt }) : t("unsaved")}
            </span>
            <Button variant="ghost" size="sm" className="border border-[var(--color-border)]">
              {t("previewAsLearner")}
            </Button>
            <Button variant="cta" size="sm" onClick={save} disabled={!canSave}>
              {t("save")}
            </Button>
          </>
        }
      />

      <div className="flex max-w-[860px] flex-col gap-4">
        <Card title={t("group1Title")} description={t("group1Subtitle")}>
          <div className="flex flex-col gap-3">
            <TextArea
              label={t("questionLabel")}
              placeholder={t("questionPlaceholder")}
              rows={4}
              value={draft.question}
              onChange={(event) => patch({ question: event.target.value })}
            />
            <div className="flex flex-wrap gap-3">
              <SelectField
                label={t("topicLabel")}
                value={draft.topic}
                onChange={(event) => patch({ topic: event.target.value as QuestionTopic })}
                options={QUESTION_TOPICS.map((key) => ({ value: key, label: t(`topic.${key}`) }))}
                wrapperClassName="min-w-[200px] flex-1"
              />
              <SelectField
                label={t("levelLabel")}
                value={draft.level}
                onChange={(event) => patch({ level: event.target.value as QuestionLevel })}
                options={QUESTION_LEVELS.map((key) => ({ value: key, label: t(`level.${key}`) }))}
                wrapperClassName="min-w-[160px] flex-1"
              />
            </div>
          </div>
        </Card>

        <Card title={t("group2Title")} description={t("group2Subtitle")}>
          <div className="flex flex-col gap-2.5">
            {draft.followUps.map((followUp, index) => (
              <div key={index} className="flex items-start gap-2">
                <TextArea
                  label={t("followUpLabel", { index: index + 1 })}
                  hideLabel
                  rows={2}
                  placeholder={t("followUpPlaceholder")}
                  value={followUp}
                  onChange={(event) => setFollowUp(index, event.target.value)}
                />
                <Button
                  variant="ghost"
                  size="sm"
                  aria-label={t("removeFollowUp", { index: index + 1 })}
                  disabled={draft.followUps.length === 1}
                  onClick={() =>
                    patch({ followUps: draft.followUps.filter((_, i) => i !== index) })
                  }
                  className="border border-[var(--color-border)] px-2"
                >
                  {t("remove")}
                </Button>
              </div>
            ))}
            <Button
              variant="ghost"
              size="sm"
              onClick={() => patch({ followUps: [...draft.followUps, ""] })}
              className="self-start border border-dashed border-[var(--color-border)]"
            >
              {t("addFollowUp")}
            </Button>
          </div>
        </Card>

        <Card
          title={t("group3Title")}
          description={t("group3Subtitle")}
          action={
            draft.rubric.length > 0 ? (
              <span
                className="font-mono text-[12.5px]"
                style={{
                  color: rubricBlocksSave
                    ? "var(--color-admin-negative)"
                    : "var(--color-text-muted)",
                }}
              >
                {t("rubricTotal", { total: rubricTotal })}
              </span>
            ) : undefined
          }
        >
          {rubricBlocksSave ? (
            <NoticeTile tone="negative" title={t("rubricBlockTitle")} className="mb-3">
              {t("rubricBlockBody", { total: rubricTotal })}
            </NoticeTile>
          ) : null}

          {draft.rubric.length === 0 ? (
            <NoticeTile tone="info" title={t("rubricEmptyTitle")} className="mb-3">
              {t("rubricEmptyBody")}
            </NoticeTile>
          ) : (
            <div className="mb-3 flex flex-col gap-2.5">
              {draft.rubric.map((criterion, index) => (
                <div
                  key={index}
                  className="glass-surface flex flex-wrap items-end gap-2.5 rounded-2xl border border-[var(--color-border)] px-3 py-3"
                >
                  <TextField
                    label={t("criterionLabel", { index: index + 1 })}
                    placeholder={t("criterionPlaceholder")}
                    value={criterion.label}
                    onChange={(event) => setCriterion(index, { label: event.target.value })}
                    wrapperClassName="min-w-[200px] flex-1"
                  />
                  <NumberStepper
                    value={criterion.weight}
                    onValueChange={(weight) => setCriterion(index, { weight })}
                    label={t("criterionWeightLabel", { index: index + 1 })}
                    format={(value) => `${value}%`}
                    decrementLabel={t("criterionWeightDecrement", { index: index + 1 })}
                    incrementLabel={t("criterionWeightIncrement", { index: index + 1 })}
                    className="mb-1"
                  />
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={t("removeCriterion", { index: index + 1 })}
                    onClick={() => patch({ rubric: draft.rubric.filter((_, i) => i !== index) })}
                    className="mb-1 border border-[var(--color-border)] px-2"
                  >
                    {t("remove")}
                  </Button>
                </div>
              ))}
            </div>
          )}

          <Button
            variant="ghost"
            size="sm"
            onClick={() => patch({ rubric: [...draft.rubric, { label: "", weight: 0 }] })}
            className="self-start border border-dashed border-[var(--color-border)]"
          >
            {t("addCriterion")}
          </Button>
        </Card>

        <Card title={t("group4Title")} description={t("group4Subtitle")}>
          <div className="flex flex-wrap gap-2">
            <Button variant="ghost" size="sm" className="border border-[var(--color-border)]">
              {t("duplicate")}
            </Button>
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setConfirmingDelete(true)}
              className="border border-[var(--color-admin-negative)] text-[var(--color-admin-negative)]"
            >
              {t("softDelete")}
            </Button>
          </div>
        </Card>
      </div>

      <ConfirmDialog
        open={confirmingDelete}
        onClose={() => setConfirmingDelete(false)}
        onConfirm={() => setConfirmingDelete(false)}
        title={t("confirmSoftDeleteTitle")}
        confirmLabel={t("softDelete")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmSoftDeleteBody")}
      </ConfirmDialog>
    </div>
  );
}
