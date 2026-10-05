// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// THE ONE ADMIN SCREEN WITH NO PROTOTYPE. 09-layoutBase has no mockup for it and the BD says so
// outright, describing the layout by structure only
// [SoT: 02-bd/screens/shared/SHR0302_interview_question_authoring.md:12-14]. So this build follows the BD
// rather than a picture, reuses the tokens and primitives the other Admin screens already
// established, and needs the owner to approve how it looks — there is nothing to compare against.
//
// Layout (owner instruction 2026-10-01): full-width two-column grid. The wide column holds what the
// author actually writes (question, follow-ups, rubric); the narrow column holds classification
// and admin actions. The header carries a back icon on the left, the save state and the ONE "Lưu"
// button on the right.
//
// BD rules this screen turns on:
// - Sticky header: back, identity, save indicator, "Xem như học viên", ONE "Lưu" button. There is
//   no draft/publish split — a question is live the moment it is saved (BD section 1.1, RD Q5).
// - Field groups stacked, no tabs (BD section 1, Q-BD1 — it asks the owner to confirm this, so it
//   is in the phase report).
// - Rubric weights: an EMPTY rubric is valid and saves fine (the question just stays out of
//   practice mode). A non-empty rubric whose weights do not total 100 BLOCKS saving
//   (BD section 1.4, `answer_rubrics` enforces it at application level).
// - Topic is one of five seeded values; this screen cannot invent a sixth (BD section 1.2).
"use client";

import { useState } from "react";
import { ArrowLeft, Eye } from "lucide-react";
import {
  findInterviewQuestionByCode,
  useInterviewLevels,
  useInterviewTopics,
  type QuestionLevel,
  type QuestionTopic,
  type RubricCriterion,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  IconAction,
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

// topic and level start empty: the create form fills them with the first row of the admin-managed lists,
// which an admin may have renamed, reordered or deleted (DEC-2026-1001).
const EMPTY_DRAFT: Draft = {
  question: "",
  topic: "",
  level: "",
  followUps: [""],
  rubric: [],
};

type Props = {
  /** Route param: a question code, or "new" for the create form. */
  questionId: string;
  /** Where "back" goes — the list under the area this screen is mounted in. */
  listHref: string;
};

function draftFor(questionId: string): Draft | null {
  if (questionId === "new") return EMPTY_DRAFT;
  const found = findInterviewQuestionByCode(questionId);
  if (!found) return null;
  return {
    question: found.question,
    topic: found.topic,
    level: found.level,
    followUps: found.followUps.length > 0 ? [...found.followUps] : [""],
    rubric: found.rubric.map((criterion) => ({ ...criterion })),
  };
}

export function InterviewQuestionAuthoringView({ questionId, listHref }: Props) {
  const t = useT("interviewQuestionAuthoring");
  const isNew = questionId === "new";
  const topicList = useInterviewTopics();
  const levelList = useInterviewLevels();
  const [loaded] = useState(() => draftFor(questionId));
  const [draft, setDraft] = useState<Draft>(
    loaded ?? { ...EMPTY_DRAFT, topic: topicList[0]?.key ?? "", level: levelList[0]?.key ?? "" },
  );
  const [saving, setSaving] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);

  const rubricTotal = draft.rubric.reduce((sum, criterion) => sum + criterion.weight, 0);
  // Empty is fine; non-empty must add to exactly 100.
  const rubricBlocksSave = draft.rubric.length > 0 && rubricTotal !== 100;
  const canSave = draft.question.trim().length > 0 && !rubricBlocksSave && !saving;

  function patch(changes: Partial<Draft>) {
    setDraft((previous) => ({ ...previous, ...changes }));
  }

  function setFollowUp(index: number, value: string) {
    patch({ followUps: draft.followUps.map((item, i) => (i === index ? value : item)) });
  }

  function setCriterion(index: number, changes: Partial<RubricCriterion>) {
    patch({
      rubric: draft.rubric.map((item, i) => (i === index ? { ...item, ...changes } : item)),
    });
  }

  // The Save button looks disabled while blocked but stays clickable, so a click names the reason.
  async function save() {
    if (saving) return;
    if (draft.question.trim().length === 0) {
      toast.warning(t("saveBlocked.empty"));
      return;
    }
    if (rubricBlocksSave) {
      toast.warning(`${t("rubricBlockTitle")}: ${t("rubricBlockBody", { total: rubricTotal })}`);
      return;
    }
    setSaving(true);
    // No endpoint yet — the delay stands in for the round trip so the saving state is reviewable.
    try {
      await new Promise((resolve) => setTimeout(resolve, 500));
      toast.success(t("toast.saved"));
    } catch {
      toast.error(t("toast.saveFailed"));
    } finally {
      setSaving(false);
    }
  }

  if (loaded === null) {
    return (
      <div>
        <PageHeader
          leading={<IconAction icon={ArrowLeft} label={t("back")} href={listHref} />}
          title={t("notFoundTitle")}
        />
        <Card>
          <EmptyState>{t("notFoundBody", { code: questionId })}</EmptyState>
        </Card>
      </div>
    );
  }

  return (
    <div>
      <PageHeader
        leading={<IconAction icon={ArrowLeft} label={t("back")} href={listHref} />}
        title={isNew ? t("titleNew") : t("titleEdit", { code: questionId })}
        description={t("subtitle")}
        actions={
          <>
            <IconAction icon={Eye} label={t("previewAsLearner")} />
            <Button variant="cta" size="sm" onClick={save} disabled={saving} aria-disabled={!canSave || undefined}>
              {t("save")}
            </Button>
          </>
        }
      />

      <div className="grid items-start gap-4 lg:grid-cols-[minmax(0,1fr)_320px]">
        <div className="flex min-w-0 flex-col gap-4">
          <Card title={t("group1Title")} description={t("group1Subtitle")}>
            <TextArea
              label={t("questionLabel")}
              placeholder={t("questionPlaceholder")}
              rows={4}
              value={draft.question}
              onChange={(event) => patch({ question: event.target.value })}
            />
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
        </div>

        {/* Sticks below the sticky header (62px + 16px gap) so classification stays in view while
            the long rubric list scrolls. */}
        <div className="flex min-w-0 flex-col gap-4 lg:sticky lg:top-[90px]">
          <Card title={t("classifyTitle")} description={t("classifySubtitle")}>
            <div className="flex flex-col gap-3">
              <SelectField
                label={t("topicLabel")}
                value={draft.topic}
                onChange={(event) => patch({ topic: event.target.value })}
                options={topicList.map((topic) => ({ value: topic.key, label: topic.label }))}
              />
              <SelectField
                label={t("levelLabel")}
                value={draft.level}
                onChange={(event) => patch({ level: event.target.value as QuestionLevel })}
                options={levelList.map((item) => ({ value: item.key, label: item.label }))}
              />
            </div>
          </Card>

          {isNew ? null : (
            <Card title={t("group4Title")} description={t("group4Subtitle")}>
              <div className="flex flex-wrap gap-2">
                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => toast.success(t("toast.duplicated"))}
                  className="border border-[var(--color-border)]"
                >
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
          )}
        </div>
      </div>

      <ConfirmDialog
        open={confirmingDelete}
        onClose={() => setConfirmingDelete(false)}
        onConfirm={() => {
          setConfirmingDelete(false);
          toast.success(t("toast.softDeleted"));
        }}
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
