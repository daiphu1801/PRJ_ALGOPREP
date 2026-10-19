// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md sections 9 and 29.
//
// BD ADM0301 V1.0 ("Prompt và chất lượng AI"): the quality side of the AI, one tab per feature
// (DEC-2026-1010-split-ai-admin-screens). Rate limits, the token cap, prices and the testcase cap
// moved to admin_ai_limits. Everything here is a draft until "Lưu và phát hành".
//
// Layout still follows 09-layoutBase/Admin - Cấu hình AI.dc.html for the individual cards (sticky
// header with the publish CTA :147-157), but the page is no longer the mockup's two
// columns: a tab holds only a few cards so they stack in one.
//
// "Chạy đối chiếu" keeps its button and stays disabled: PROTOTYPE_DEBT 2.8 settles that the UI stays
// while the backend is deferred (risk R3). Three popups sit on top (phase 4 of
// 06-plan/261010-1902-ai-admin-tach-man-don-gian-hoa.md): edit the system instruction (saved into the
// draft, closing with unsaved text asks first), restore an older version into the draft, and a
// publish confirmation that lists what will replace the live prompt.
//
// The rubric card (weights) was dropped with DEC-2026-1014-drop-solution-review-rubric.
"use client";

import { useState } from "react";
import {
  FEATURE_KEYS,
  GUARDED_FEATURES,
  GUARD_KEYS,
  fetchAiConfigPage,
  type AiConfigPage,
  type AiFeatureKey,
  type GuardKey,
  type GuardedFeature,
  type PromptStatus,
  type PromptVersionEntry,
} from "../api";
import {
  applyPublish,
  describeChanges,
  pendingByFeature,
  restoreVersion,
  totalPending,
  withInstruction,
} from "../model/changes";
import { useT } from "@/shared/i18n";
import { useUnsavedChangesGuard } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  ConfirmDialog,
  FilterMenu,
  Modal,
  PageHeader,
  SegmentedTabs,
  SettingRow,
  TextArea,
  Toggle,
  UnsavedChangesDialog,
  type BadgeVariant,
} from "@/shared/ui";

// dc.html:414-417.
const STATUS_VARIANT: Record<PromptStatus, BadgeVariant> = {
  live: "success",
  draft: "warn",
};

// BD ADM0301 Sheet 5 Khu vực E: the guard that F5-17 and F5-18 make a hard rule, so it stays on.
const LOCKED_GUARD: GuardKey = "noFullSolution";

function isGuarded(feature: AiFeatureKey): feature is GuardedFeature {
  return (GUARDED_FEATURES as AiFeatureKey[]).includes(feature);
}

export function AdminAiConfigView() {
  const t = useT("adminAiConfig");
  // `saved` is what is live; `draft` is what the admin is editing. The difference is the pending set.
  const [saved, setSaved] = useState<AiConfigPage>(fetchAiConfigPage);
  const [draft, setDraft] = useState<AiConfigPage>(fetchAiConfigPage);
  const [feature, setFeature] = useState<AiFeatureKey>("review");
  const [historyOpen, setHistoryOpen] = useState(false);
  const [discardOpen, setDiscardOpen] = useState(false);
  const [publishOpen, setPublishOpen] = useState(false);
  // Edit popup: null when closed. `invalid` draws the red border, the message is a toast.
  const [editing, setEditing] = useState<{
    text: string;
    invalid: boolean;
  } | null>(null);
  const [closeEditOpen, setCloseEditOpen] = useState(false);
  const [restoreTarget, setRestoreTarget] = useState<PromptVersionEntry | null>(
    null,
  );

  const counts = pendingByFeature(saved, draft);
  const pendingTotal = totalPending(counts);
  const leaveGuard = useUnsavedChangesGuard(pendingTotal > 0);

  const prompt = draft.prompts.find((item) => item.feature === feature)!;
  const savedPrompt = saved.prompts.find((item) => item.feature === feature)!;
  // A prompt whose instruction was edited shows as a draft until it is published (BD EVT-2, EVT-3).
  const displayStatus: PromptStatus =
    prompt.systemInstruction !== savedPrompt.systemInstruction
      ? "draft"
      : prompt.status;
  const history = draft.versionHistory.filter(
    (entry) => entry.feature === feature,
  );

  function setModel(target: AiFeatureKey, modelId: string) {
    setDraft((previous) => ({
      ...previous,
      prompts: previous.prompts.map((item) =>
        item.feature === target ? { ...item, modelId } : item,
      ),
    }));
  }

  function setGuard(target: GuardedFeature, key: GuardKey, value: boolean) {
    // Owner instruction 2026-10-11: the anti-leak guard can be turned off, with a warning.
    if (key === LOCKED_GUARD && !value) toast.warning(t("guardOffWarning"));
    setDraft((previous) => ({
      ...previous,
      guards: {
        ...previous.guards,
        [target]: { ...previous.guards[target], [key]: value },
      },
    }));
  }

  /** The check in front of every publish; a failure explains itself in a toast. */
  function canPublish(): boolean {
    if (pendingTotal === 0) {
      toast.info(t("noChanges"));
      return false;
    }
    return true;
  }

  function commitPublish() {
    const next = applyPublish(saved, draft);
    setSaved(next);
    setDraft(next);
    setPublishOpen(false);
    toast.success(t("publishDone"));
  }

  function openEdit() {
    setEditing({ text: prompt.systemInstruction, invalid: false });
  }

  function saveEdit() {
    if (!editing) return;
    // BD Sheet 9 NO 5: an empty instruction is not saved.
    if (editing.text.trim() === "") {
      setEditing({ ...editing, invalid: true });
      toast.error(t("editDialog.errorEmpty"));
      return;
    }
    setDraft((previous) => withInstruction(previous, feature, editing.text));
    setEditing(null);
    toast.success(t("editDialog.saved"));
  }

  function requestCloseEdit() {
    if (editing && editing.text !== prompt.systemInstruction)
      setCloseEditOpen(true);
    else setEditing(null);
  }

  function confirmRestore() {
    if (!restoreTarget) return;
    setDraft((previous) => restoreVersion(previous, restoreTarget));
    toast.success(t("restore.done", { version: restoreTarget.version }));
    setRestoreTarget(null);
    setHistoryOpen(false);
  }

  function discard() {
    setDraft(saved);
    setDiscardOpen(false);
    toast.info(t("discarded"));
  }

  const tabOptions = FEATURE_KEYS.map((key) => ({
    value: key,
    label:
      counts[key] > 0
        ? `${t(`feature.${key}`)} (${counts[key]})`
        : t(`feature.${key}`),
  }));

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <>
            {pendingTotal > 0 ? (
              <>
                <span className="text-[12.5px] text-[var(--color-text-muted)]">
                  {t("pendingCount", { count: pendingTotal })}
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  className="border border-[var(--color-border)]"
                  onClick={() => setDiscardOpen(true)}
                >
                  {t("discard")}
                </Button>
              </>
            ) : null}
            <Button
              variant="cta"
              size="sm"
              aria-disabled={pendingTotal === 0 || undefined}
              onClick={() => canPublish() && setPublishOpen(true)}
            >
              {t("publish")}
            </Button>
          </>
        }
      />

      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <SegmentedTabs
          label={t("tabsLabel")}
          options={tabOptions}
          value={feature}
          onValueChange={setFeature}
        />
        <Button
          variant="ghost"
          size="sm"
          onClick={() => setHistoryOpen(true)}
          className="border border-[var(--color-border)]"
        >
          {t("versionLog")}
        </Button>
      </div>

      <div className="flex min-w-0 flex-col gap-4">
        <Card title={t("promptsTitle")} description={t("promptsSubtitle")}>
          <div className="glass-surface rounded-2xl border border-[var(--color-border)] px-3.5 py-3.5">
            <div className="mb-1.5 flex flex-wrap items-center gap-2.5">
              <span className="text-[13.5px] font-semibold">
                {t(`feature.${prompt.feature}`)}
              </span>
              <span className="rounded-md bg-[var(--color-track)] px-1.5 py-0.5 font-mono text-[11px] text-[var(--color-text-muted)]">
                {prompt.version}
              </span>
              <Badge variant={STATUS_VARIANT[displayStatus]}>
                {t(`promptStatus.${displayStatus}`)}
              </Badge>
              <Button
                variant="ghost"
                size="sm"
                className="ml-auto"
                onClick={openEdit}
              >
                {t("editPrompt")}
              </Button>
            </div>
            <p className="mb-2.5 text-[12.5px] leading-relaxed text-pretty text-[var(--color-text-muted)]">
              {t(`promptDescription.${prompt.feature}`)}
            </p>
            {/* The subtle tone sits on the two caption spans, not the row: FilterMenu's value inherits
                its colour, and on the dark track it fell to 3.6:1 (admin UX review F4). */}
            <div className="flex flex-wrap items-end gap-x-4 gap-y-2 text-xs">
              <FilterMenu
                label={t("metaModel")}
                value={prompt.modelId}
                onValueChange={(modelId) => setModel(prompt.feature, modelId)}
                options={draft.modelOptions.map((id) => ({
                  value: id,
                  label: id,
                }))}
              />
              <span className="text-[var(--color-text-subtle)]">
                {t("metaMaxTokens")}{" "}
                <span className="font-mono text-[var(--color-text-muted)]">
                  {prompt.maxTokens}
                </span>
              </span>
              <span className="text-[var(--color-text-subtle)]">
                {t("metaUpdated", { date: prompt.updatedAt })}
              </span>
            </div>
          </div>
        </Card>

        {isGuarded(feature) ? (
          <Card title={t("guardsTitle")}>
            <div className="flex flex-col gap-2.5">
              {GUARD_KEYS.map((key) => (
                <SettingRow
                  key={key}
                  label={t(`guard.${key}.label`)}
                  description={
                    key === LOCKED_GUARD
                      ? draft.guards[feature][key]
                        ? `${t(`guard.${key}.meta`)}. ${t("guardRecommended")}`
                        : t("guardOffWarning")
                      : t(`guard.${key}.meta`)
                  }
                >
                  <Toggle
                    checked={draft.guards[feature][key]}
                    onCheckedChange={(value) => setGuard(feature, key, value)}
                    label={t(`guard.${key}.label`)}
                  />
                </SettingRow>
              ))}
            </div>
          </Card>
        ) : null}

        {feature === "review" ? (
          <Card title={t("preReleaseTitle")}>
            <p className="mb-3 text-[13px] leading-relaxed text-pretty text-[var(--color-text-muted)]">
              {t("preReleaseBody")}
            </p>
            {/* Deliberately inert — PROTOTYPE_DEBT 2.8 defers the backend, so the button shows the
                intent without pretending to run anything. */}
            <Button
              variant="ghost"
              size="sm"
              disabled
              className="w-full border border-[var(--color-border)]"
            >
              {t("runComparison")}
            </Button>
          </Card>
        ) : null}
      </div>

      <Modal
        open={historyOpen}
        onClose={() => setHistoryOpen(false)}
        title={`${t("versionLog")}: ${t(`feature.${feature}`)}`}
        footer={
          <Button
            variant="ghost"
            size="sm"
            onClick={() => setHistoryOpen(false)}
          >
            {t("close")}
          </Button>
        }
      >
        {history.length === 0 ? (
          <p className="text-[13px] text-[var(--color-text-muted)]">
            {t("historyEmpty")}
          </p>
        ) : (
          <ul className="flex flex-col gap-2">
            {history.map((entry) => (
              <li
                key={entry.id}
                className="flex items-center gap-3 rounded-xl border border-[var(--color-border)] px-3 py-2"
              >
                <span className="font-mono text-[11.5px]">{entry.version}</span>
                <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">
                  {entry.publishedAt} · {entry.author}
                </span>
                {entry.version === savedPrompt.version ? (
                  <Badge variant="success" className="ml-auto">
                    {t("restore.current")}
                  </Badge>
                ) : (
                  <Button
                    variant="ghost"
                    size="sm"
                    className="ml-auto border border-[var(--color-border)]"
                    onClick={() => setRestoreTarget(entry)}
                  >
                    {t("restore.button")}
                  </Button>
                )}
              </li>
            ))}
          </ul>
        )}
      </Modal>

      <ConfirmDialog
        open={discardOpen}
        onClose={() => setDiscardOpen(false)}
        onConfirm={discard}
        title={t("discardTitle")}
        confirmLabel={t("discardConfirm")}
        cancelLabel={t("discardCancel")}
        destructive
      >
        {t("discardBody")}
      </ConfirmDialog>

      <Modal
        open={editing !== null}
        onClose={requestCloseEdit}
        title={t("editDialog.title", { feature: t(`feature.${feature}`) })}
        footer={
          <>
            <Button variant="ghost" size="sm" onClick={requestCloseEdit}>
              {t("editDialog.cancel")}
            </Button>
            <Button variant="cta" size="sm" onClick={saveEdit}>
              {t("editDialog.save")}
            </Button>
          </>
        }
      >
        {editing ? (
          <div className="flex flex-col gap-2">
            <p className="text-[12.5px] text-[var(--color-text-muted)]">
              {t("editDialog.hint")}
            </p>
            <TextArea
              label={t("editDialog.label")}
              rows={8}
              value={editing.text}
              invalid={editing.invalid}
              onChange={(event) =>
                setEditing({ text: event.target.value, invalid: false })
              }
            />
          </div>
        ) : null}
      </Modal>

      <ConfirmDialog
        open={closeEditOpen}
        onClose={() => setCloseEditOpen(false)}
        onConfirm={() => {
          setCloseEditOpen(false);
          setEditing(null);
        }}
        title={t("editDialog.closeTitle")}
        confirmLabel={t("editDialog.closeConfirm")}
        cancelLabel={t("editDialog.closeStay")}
        destructive
      >
        {t("editDialog.closeBody")}
      </ConfirmDialog>

      <ConfirmDialog
        open={restoreTarget !== null}
        onClose={() => setRestoreTarget(null)}
        onConfirm={confirmRestore}
        title={t("restore.title", { version: restoreTarget?.version ?? "" })}
        confirmLabel={t("restore.confirm")}
        cancelLabel={t("restore.cancel")}
      >
        {t("restore.body")}
      </ConfirmDialog>

      <ConfirmDialog
        open={publishOpen}
        onClose={() => setPublishOpen(false)}
        onConfirm={commitPublish}
        title={t("publishDialog.title")}
        confirmLabel={t("publishDialog.confirm")}
        cancelLabel={t("publishDialog.cancel")}
      >
        <p className="mb-2">{t("publishDialog.intro")}</p>
        <ul className="mb-3 flex flex-col gap-2">
          {describeChanges(saved, draft).map((change) => (
            <li key={change.feature}>
              <span className="font-semibold">
                {t(`feature.${change.feature}`)}
              </span>
              <ul className="ml-4 list-disc text-[12.5px] text-[var(--color-text-muted)]">
                {change.instruction ? (
                  <li>{t("publishDialog.item.instruction")}</li>
                ) : null}
                {change.model ? (
                  <li>
                    {t("publishDialog.item.model", {
                      from: change.model.from,
                      to: change.model.to,
                    })}
                  </li>
                ) : null}
                {change.guards > 0 ? (
                  <li>
                    {t("publishDialog.item.guards", { count: change.guards })}
                  </li>
                ) : null}
              </ul>
            </li>
          ))}
        </ul>
        <p className="text-[12.5px] text-[var(--color-text-muted)]">
          {t("publishDialog.warn")}
        </p>
      </ConfirmDialog>

      <UnsavedChangesDialog
        open={leaveGuard.pending}
        title={t("unsaved.title")}
        saveAndLeaveLabel={t("unsaved.saveAndLeave")}
        leaveLabel={t("unsaved.leave")}
        stayLabel={t("unsaved.stay")}
        onSaveAndLeave={() => {
          if (canPublish()) {
            commitPublish();
            leaveGuard.leave();
          } else leaveGuard.stay();
        }}
        onLeave={leaveGuard.leave}
        onStay={leaveGuard.stay}
      >
        {t("unsaved.body")}
      </UnsavedChangesDialog>
    </div>
  );
}
