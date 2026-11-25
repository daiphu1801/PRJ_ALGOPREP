// Wired to F6 #20 / #21 / #22 / #23 (03-dd/api/F6_interview-bank.md). Route param: the question UUID (the
// code of the mock bank in mock mode) or "new". Save is pessimistic: the form keeps the draft until the
// server answers. `contentMarkdown` and each criterion's `description` have no input on this form
// (BD SHR0302 Q7), so a save sends back what was loaded: PUT replaces the whole question.
//
// History: THE ONE ADMIN SCREEN WITH NO PROTOTYPE. 09-layoutBase has no mockup for it and the BD says so
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

import { useId, useState } from "react";
import { useRouter } from "next/navigation";
import { ArrowLeft, Eye } from "lucide-react";
import {
  InterviewQuestionPreview,
  useAdminQuestion,
  useCreateAdminQuestion,
  useInterviewLevels,
  useInterviewTopics,
  useRetireAdminQuestions,
  useUpdateAdminQuestion,
  type AdminQuestionDetail,
  type AdminQuestionInput,
} from "@/entities/interview-question";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  EmptyState,
  IconAction,
  MarkdownPreview,
  NoticeTile,
  NumberStepper,
  PageContainer,
  PageHeader,
  QueryBoundary,
  SelectField,
  TextArea,
  TextField,
} from "@/shared/ui";

type DraftCriterion = {
  label: string;
  /** Weight percentage, 0-100. */
  weight: number;
  description: string | null;
};

// `topicId` / `levelId` are the list rows' ids (API) or keys (mock); empty means "first row of the list",
// which an admin may have renamed, reordered or deleted (DEC-2026-1001).
type Draft = {
  title: string;
  topicId: string;
  levelId: string;
  contentMarkdown: string | null;
  followUps: string[];
  rubric: DraftCriterion[];
};

const TITLE_MAX = 255;

const EMPTY_DRAFT: Draft = {
  title: "",
  topicId: "",
  levelId: "",
  contentMarkdown: null,
  followUps: [""],
  rubric: [],
};

type Props = {
  /** Route param: a question code, or "new" for the create form. */
  questionId: string;
  /** Where "back" goes — the list under the area this screen is mounted in. */
  listHref: string;
};

function draftFor(
  questionId: string,
  found: AdminQuestionDetail | null,
): Draft | null {
  if (questionId === "new") return EMPTY_DRAFT;
  if (!found) return null;
  return {
    title: found.title,
    topicId: found.topicId,
    levelId: found.levelId,
    contentMarkdown: found.contentMarkdown,
    followUps: found.followUps.length > 0 ? [...found.followUps] : [""],
    rubric: found.rubric.map((criterion) => ({
      label: criterion.criterionCode,
      weight: criterion.weightPercent,
      description: criterion.description,
    })),
  };
}

export function InterviewQuestionAuthoringView(props: Props) {
  const isNew = props.questionId === "new";
  const query = useAdminQuestion(props.questionId, !isNew);
  // The create form has nothing to load, so it skips the boundary.
  if (isNew) return <InterviewQuestionAuthoring {...props} found={null} />;
  return (
    <QueryBoundary query={query}>
      {(found) => <InterviewQuestionAuthoring {...props} found={found} />}
    </QueryBoundary>
  );
}

function InterviewQuestionAuthoring({
  questionId,
  listHref,
  found,
}: Props & { found: AdminQuestionDetail | null }) {
  const t = useT("interviewQuestionAuthoring");
  const isNew = questionId === "new";
  const apiMessage = useApiErrorMessage();
  const router = useRouter();
  const topicList = useInterviewTopics();
  const levelList = useInterviewLevels();
  const create = useCreateAdminQuestion();
  const update = useUpdateAdminQuestion();
  const retire = useRetireAdminQuestions();
  const [loaded] = useState(() => draftFor(questionId, found));
  // Kept from the first render: after a soft delete the detail query refetches and comes back null.
  const [displayNo] = useState(found?.displayNo ?? 0);
  const [draft, setDraft] = useState<Draft>(loaded ?? EMPTY_DRAFT);
  const [savedDraft, setSavedDraft] = useState(loaded);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [previewOpen, setPreviewOpen] = useState(false);
  const sectionId = useId();
  const saving = create.isPending || update.isPending;
  const dirty =
    JSON.stringify(draft) !== JSON.stringify(savedDraft ?? EMPTY_DRAFT);
  const saveStatus = saving
    ? t("saving")
    : dirty
      ? t("unsaved")
      : isNew
        ? null
        : t("saved");
  const idOf = (item: { id?: string; key: string }) => item.id ?? item.key;
  // An untouched create form shows (and saves) the first row of each list.
  const topicId = draft.topicId || (topicList[0] ? idOf(topicList[0]) : "");
  const levelId = draft.levelId || (levelList[0] ? idOf(levelList[0]) : "");

  const rubricTotal = draft.rubric.reduce(
    (sum, criterion) => sum + criterion.weight,
    0,
  );
  // Empty is fine; non-empty must add to exactly 100.
  const rubricBlocksSave = draft.rubric.length > 0 && rubricTotal !== 100;
  const canSave = draft.title.trim().length > 0 && !rubricBlocksSave && !saving;

  function patch(changes: Partial<Draft>) {
    setDraft((previous) => ({ ...previous, ...changes }));
  }

  function setFollowUp(index: number, value: string) {
    patch({
      followUps: draft.followUps.map((item, i) => (i === index ? value : item)),
    });
  }

  function setCriterion(index: number, changes: Partial<DraftCriterion>) {
    patch({
      rubric: draft.rubric.map((item, i) =>
        i === index ? { ...item, ...changes } : item,
      ),
    });
  }

  // The Save button looks disabled while blocked but stays clickable, so a click names the reason.
  function save() {
    if (saving) return;
    const title = draft.title.trim();
    if (title.length === 0) {
      toast.warning(t("saveBlocked.empty"));
      return;
    }
    if ([...title].length > TITLE_MAX) {
      toast.warning(t("saveBlocked.titleTooLong", { max: TITLE_MAX }));
      return;
    }
    const names = draft.rubric.map((criterion) => criterion.label.trim());
    if (names.some((name) => name === "")) {
      toast.warning(t("saveBlocked.criterionBlank"));
      return;
    }
    if (
      new Set(names.map((name) => name.toLocaleLowerCase("vi"))).size <
      names.length
    ) {
      toast.warning(t("saveBlocked.criterionDuplicate"));
      return;
    }
    if (rubricBlocksSave) {
      toast.warning(
        `${t("rubricBlockTitle")}: ${t("rubricBlockBody", { total: rubricTotal })}`,
      );
      return;
    }
    if (!topicId || !levelId) {
      toast.warning(t("saveBlocked.classify"));
      return;
    }
    const input: AdminQuestionInput = {
      topicId,
      levelId,
      title,
      contentMarkdown: draft.contentMarkdown,
      followUps: draft.followUps
        .map((item) => item.trim())
        .filter((item) => item !== ""),
      rubric: draft.rubric.map((criterion, index) => ({
        criterionCode: names[index]!,
        weightPercent: criterion.weight,
        description: criterion.description,
      })),
    };
    const onError = (error: unknown) => toast.error(apiMessage(error));
    if (isNew) {
      create.mutate(input, {
        onSuccess: (created) => {
          toast.success(t("toast.saved"));
          router.replace(`${listHref}/${created.id}/edit`);
        },
        onError,
      });
    } else {
      update.mutate(
        { id: questionId, input },
        {
          onSuccess: () => {
            setSavedDraft(draft);
            toast.success(t("toast.saved"));
          },
          onError,
        },
      );
    }
  }

  // Soft delete is bulk-delete of one id (F6 #23); a 200 can still list the id as failed.
  function softDelete() {
    retire.mutate([questionId], {
      onSuccess: (result) => {
        const failed = result.failedItems[0];
        if (failed) {
          toast.error(apiMessage(new ApiError(failed.code, 0)));
          return;
        }
        toast.success(t("toast.softDeleted"));
        router.push(listHref);
      },
      onError: (error) => toast.error(apiMessage(error)),
    });
  }

  if (loaded === null) {
    return (
      <div>
        <PageHeader
          leading={
            <IconAction icon={ArrowLeft} label={t("back")} href={listHref} />
          }
          title={t("notFoundTitle")}
        />
        <Card>
          <EmptyState>{t("notFoundBody", { code: questionId })}</EmptyState>
        </Card>
      </div>
    );
  }

  return (
    <PageContainer variant="content" className="@container">
      <div className="flex flex-col gap-3 pb-3 @min-[900px]:sticky @min-[900px]:top-0 @min-[900px]:z-20">
        <PageHeader
          className="static mb-0 [&>div:nth-child(2)]:min-w-0 [&_h1]:whitespace-normal @max-[640px]:[&>div:last-child]:w-full @max-[640px]:[&>div:last-child]:justify-start"
          leading={
            <IconAction icon={ArrowLeft} label={t("back")} href={listHref} />
          }
          title={
            isNew
              ? t("titleNew")
              : t("titleEdit", {
                  code: `IQ-${String(displayNo).padStart(3, "0")}`,
                })
          }
          description={t("subtitle")}
          actions={
            <>
              <Button
                variant="ghost"
                size="sm"
                className="gap-1.5 border border-[var(--color-border)]"
                onClick={() => setPreviewOpen(true)}
              >
                <Eye aria-hidden="true" className="size-4" />
                {t("previewAsLearner")}
              </Button>
              <Button
                variant="cta"
                size="sm"
                onClick={save}
                disabled={saving}
                aria-disabled={!canSave || undefined}
              >
                {t("save")}
              </Button>
            </>
          }
        />
        <nav
          aria-label={t("sectionsLabel")}
          className="glass-card flex flex-wrap items-center gap-1.5 border border-[var(--color-border)] p-1.5"
        >
          {[
            ["question", t("sectionQuestion")],
            ["follow-ups", t("sectionFollowUps")],
            ["rubric", t("sectionRubric")],
            ["classification", t("classifyTitle")],
          ].map(([id, label]) => (
            <Button
              key={id}
              variant="ghost"
              size="sm"
              className="text-xs"
              onClick={() => {
                const target = document.getElementById(`${sectionId}-${id}`);
                target?.scrollIntoView({ block: "start" });
                target?.focus({ preventScroll: true });
              }}
            >
              {label}
            </Button>
          ))}
          <p
            role="status"
            className="ml-auto px-2 text-xs text-[var(--color-text-muted)]"
          >
            {saveStatus}
          </p>
        </nav>
      </div>

      <div className="grid flex-1 items-start gap-4 @min-[900px]:grid-cols-[minmax(0,1fr)_clamp(16rem,25cqw,24rem)]">
        <div className="flex min-w-0 flex-col gap-4">
          <div
            id={`${sectionId}-question`}
            tabIndex={-1}
            className="scroll-mt-3 rounded-2xl @min-[900px]:scroll-mt-36"
          >
            <Card title={t("group1Title")} description={t("group1Subtitle")}>
              <TextArea
                label={t("questionLabel")}
                placeholder={t("questionPlaceholder")}
                rows={3}
                className="block resize-y"
                value={draft.title}
                onChange={(event) => patch({ title: event.target.value })}
              />
              {draft.contentMarkdown?.trim() && (
                <details className="mt-3 rounded-lg border border-[var(--color-border)] px-3 py-2">
                  <summary className="cursor-pointer text-sm font-medium">
                    {t("savedContext")}
                  </summary>
                  <MarkdownPreview className="mt-2 overflow-x-auto text-[var(--color-text-muted)]">
                    {draft.contentMarkdown}
                  </MarkdownPreview>
                </details>
              )}
            </Card>
          </div>

          <div
            id={`${sectionId}-follow-ups`}
            tabIndex={-1}
            className="scroll-mt-3 rounded-2xl @min-[900px]:scroll-mt-36"
          >
            <Card title={t("group2Title")} description={t("group2Subtitle")}>
              <div className="flex flex-col gap-2.5">
                {draft.followUps.map((followUp, index) => (
                  <div key={index} className="flex items-end gap-2">
                    <TextArea
                      label={t("followUpLabel", { index: index + 1 })}
                      rows={1}
                      className="block max-h-40 resize-y field-sizing-content"
                      placeholder={t("followUpPlaceholder")}
                      value={followUp}
                      onChange={(event) =>
                        setFollowUp(index, event.target.value)
                      }
                    />
                    <Button
                      variant="ghost"
                      size="sm"
                      aria-label={t("removeFollowUp", { index: index + 1 })}
                      disabled={draft.followUps.length === 1}
                      onClick={() =>
                        patch({
                          followUps: draft.followUps.filter(
                            (_, i) => i !== index,
                          ),
                        })
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
          </div>

          <div
            id={`${sectionId}-rubric`}
            tabIndex={-1}
            className="scroll-mt-3 rounded-2xl @min-[900px]:scroll-mt-36"
          >
            <Card
              title={t("group3Title")}
              description={t("group3Subtitle")}
              action={
                draft.rubric.length > 0 ? (
                  <Badge variant={rubricBlocksSave ? "warn" : "success"}>
                    {t("rubricTotal", { total: rubricTotal })}
                  </Badge>
                ) : undefined
              }
            >
              {draft.rubric.length === 0 ? (
                <NoticeTile
                  tone="info"
                  title={t("rubricEmptyTitle")}
                  className="mb-3"
                >
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
                        onChange={(event) =>
                          setCriterion(index, { label: event.target.value })
                        }
                        wrapperClassName="min-w-0 flex-[1_1_12rem]"
                      />
                      <NumberStepper
                        value={criterion.weight}
                        onValueChange={(weight) =>
                          setCriterion(index, { weight })
                        }
                        label={t("criterionWeightLabel", { index: index + 1 })}
                        format={(value) => `${value}%`}
                        decrementLabel={t("criterionWeightDecrement", {
                          index: index + 1,
                        })}
                        incrementLabel={t("criterionWeightIncrement", {
                          index: index + 1,
                        })}
                        className="mb-1"
                      />
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label={t("removeCriterion", { index: index + 1 })}
                        onClick={() =>
                          patch({
                            rubric: draft.rubric.filter((_, i) => i !== index),
                          })
                        }
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
                onClick={() =>
                  patch({
                    rubric: [
                      ...draft.rubric,
                      { label: "", weight: 0, description: null },
                    ],
                  })
                }
                className="self-start border border-dashed border-[var(--color-border)]"
              >
                {t("addCriterion")}
              </Button>
            </Card>
          </div>
        </div>

        <div
          id={`${sectionId}-classification`}
          tabIndex={-1}
          className="flex min-w-0 scroll-mt-3 flex-col gap-4 rounded-2xl @min-[900px]:scroll-mt-36"
        >
          <Card title={t("classifyTitle")} description={t("classifySubtitle")}>
            <div className="flex flex-col gap-3">
              <SelectField
                label={t("topicLabel")}
                value={topicId}
                onChange={(event) => patch({ topicId: event.target.value })}
                options={topicList.map((topic) => ({
                  value: idOf(topic),
                  label: topic.label,
                }))}
              />
              <SelectField
                label={t("levelLabel")}
                value={levelId}
                onChange={(event) => patch({ levelId: event.target.value })}
                options={levelList.map((item) => ({
                  value: idOf(item),
                  label: item.label,
                }))}
              />
            </div>
          </Card>

          {isNew ? null : (
            <Card title={t("group4Title")} description={t("group4Subtitle")}>
              <div className="flex flex-wrap gap-2">
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
          softDelete();
        }}
        title={t("confirmSoftDeleteTitle")}
        confirmLabel={t("softDelete")}
        cancelLabel={t("cancel")}
        destructive
      >
        {t("confirmSoftDeleteBody")}
      </ConfirmDialog>
      {previewOpen && (
        <InterviewQuestionPreview
          title={draft.title}
          content={draft.contentMarkdown}
          followUps={draft.followUps}
          onClose={() => setPreviewOpen(false)}
        />
      )}
    </PageContainer>
  );
}
