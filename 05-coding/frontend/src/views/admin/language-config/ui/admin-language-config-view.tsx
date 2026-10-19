// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Reworked 2026-10-10 from 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html (five cards, fourteen
// inputs on one page) into two tabs, because only the language table is touched regularly:
//   tab 1 "Ngôn ngữ và giới hạn": the four default limits as one compact row, then a four-column
//         table (language, multipliers, effective limits, enabled);
//   tab 2 "Sandbox và sinh testcase": the three sandbox switches and the generator-script limits,
//         which are set once and rarely change.
// One sticky save bar saves the whole page (BD EVT-5); the "Lưu ý khi đổi giới hạn" card became a
// one-line note beside it, and the mockup's decorative "go-judge #71" column is gone (BD Q3).
//
// Save rules live in model/validation.ts. One confirmation, at save time, only when a language goes
// from enabled to disabled (BD Q5) — never one per toggle.
"use client";

import Link from "next/link";
import { useState } from "react";
import {
  effectiveMemoryLimitMb,
  effectiveTimeLimitMs,
  useLanguageConfigPage,
  useSaveLanguageConfigPage,
  GENERATOR_OUTPUT_RANGE,
  GENERATOR_RUNTIME_RANGE,
  type GeneratorSandbox,
  type JudgeDefaults,
  type LanguageConfig,
  type LanguageConfigPage,
  type SandboxConfig,
} from "../api";
import {
  badDefault,
  badMemoryLimit,
  badMultiplier,
  badOutput,
  badRuntime,
  badTimeLimit,
  countChanges,
  newlyDisabled,
  validateLanguageConfig,
} from "../model/validation";
import { useLocale, useT } from "@/shared/i18n";
import { useUnsavedChangesGuard } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Card,
  ConfirmDialog,
  DataTable,
  ErrorState,
  PageHeader,
  SaveBar,
  SegmentedTabs,
  SettingRow,
  Skeleton,
  TextField,
  Toggle,
  UnsavedChangesDialog,
  type DataTableColumn,
} from "@/shared/ui";

const CHIP_STYLE: Record<LanguageConfig["key"], string> = {
  py: "bg-[var(--color-lang-py-bg)] text-[var(--color-lang-py-fg)]",
  cpp: "bg-[var(--color-lang-cpp-bg)] text-[var(--color-lang-cpp-fg)]",
  java: "bg-[var(--color-lang-java-bg)] text-[var(--color-lang-java-fg)]",
};

type Tab = "languages" | "advanced";

/** Loads the page, then hands it to the form, which owns the editable copy. */
export function AdminLanguageConfigView() {
  const t = useT("adminLanguageConfig");
  const query = useLanguageConfigPage();

  if (query.isError) return <ErrorState>{t("loadFailed")}</ErrorState>;
  if (!query.data) {
    return (
      <div className="flex flex-col gap-4" aria-busy="true">
        <Skeleton className="h-[62px] w-full" />
        <Skeleton className="h-[320px] w-full" />
      </div>
    );
  }
  return <LanguageConfigForm initial={query.data} />;
}

function LanguageConfigForm({ initial }: { initial: LanguageConfigPage }) {
  const t = useT("adminLanguageConfig");
  const tCommon = useT("common");
  const locale = useLocale();

  // `saved` is the baseline the change count compares against: it starts as the loaded page and moves
  // to the draft once a save succeeds.
  const [saved, setSaved] = useState<LanguageConfigPage>(initial);
  const [draft, setDraft] = useState<LanguageConfigPage>(initial);
  const [tab, setTab] = useState<Tab>("languages");
  const [attempted, setAttempted] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const saveConfig = useSaveLanguageConfigPage();
  const saving = saveConfig.isPending;

  const dirtyCount = countChanges(saved, draft);
  const leaveGuard = useUnsavedChangesGuard(dirtyCount > 0);
  const number = (value: number) => value.toLocaleString(locale);

  function patchLanguage(
    key: LanguageConfig["key"],
    patch: Partial<LanguageConfig>,
  ) {
    setDraft((prev) => ({
      ...prev,
      languages: prev.languages.map((language) =>
        language.key === key ? { ...language, ...patch } : language,
      ),
    }));
  }

  function patchDefaults(patch: Partial<JudgeDefaults>) {
    setDraft((prev) => ({ ...prev, defaults: { ...prev.defaults, ...patch } }));
  }

  function patchGenerator(patch: Partial<GeneratorSandbox>) {
    setDraft((prev) => ({
      ...prev,
      generator: { ...prev.generator, ...patch },
    }));
  }

  function patchSandbox(patch: Partial<SandboxConfig>) {
    setDraft((prev) => ({ ...prev, sandbox: { ...prev.sandbox, ...patch } }));
  }

  /** True when the draft passes every rule; otherwise raises the error toast and returns false. */
  function passes(): boolean {
    setAttempted(true);
    const error = validateLanguageConfig(draft);
    if (error) toast.error(t(error));
    return error === null;
  }

  /** The success toast sits inside the try, after the await: a failed save must not report success. */
  async function commit(): Promise<boolean> {
    try {
      await saveConfig.mutateAsync(draft);
      setSaved(draft);
      setAttempted(false);
      toast.success(t("saveDone"));
      return true;
    } catch {
      toast.error(t("saveFailed"));
      return false;
    }
  }

  async function save() {
    if (!passes()) return;
    if (newlyDisabled(saved, draft).length > 0) {
      setConfirming(true);
      return;
    }
    await commit();
  }

  const disabledNow = newlyDisabled(saved, draft);

  const columns: DataTableColumn<LanguageConfig>[] = [
    {
      key: "language",
      header: t("columnLanguage"),
      render: (language) => (
        <div className="flex items-center gap-2.5">
          <span
            aria-hidden="true"
            className={`flex h-7 w-7 shrink-0 items-center justify-center rounded-[9px] font-mono text-[11px] font-semibold ${CHIP_STYLE[language.key]}`}
          >
            {language.short}
          </span>
          <span className="min-w-0">
            <span className="block font-semibold whitespace-nowrap">
              {language.name}
            </span>
            <span className="block font-mono text-[11.5px] whitespace-nowrap text-[var(--color-text-subtle)]">
              {language.compiler}
            </span>
          </span>
        </div>
      ),
    },
    {
      key: "multipliers",
      header: t("columnMultipliers"),
      width: "230px",
      render: (language) => (
        <span className="flex items-center gap-2">
          <MultiplierField
            tag={t("multiplierTime")}
            label={t("timeMultiplierFor", { language: language.name })}
            value={language.timeMultiplier}
            disabled={saving}
            invalid={
              attempted &&
              (badMultiplier(language.timeMultiplier) ||
                badTimeLimit(draft, language))
            }
            onChange={(value) =>
              patchLanguage(language.key, { timeMultiplier: value })
            }
          />
          <MultiplierField
            tag={t("multiplierMemory")}
            label={t("memoryMultiplierFor", { language: language.name })}
            value={language.memoryMultiplier}
            disabled={saving}
            invalid={
              attempted &&
              (badMultiplier(language.memoryMultiplier) ||
                badMemoryLimit(draft, language))
            }
            onChange={(value) =>
              patchLanguage(language.key, { memoryMultiplier: value })
            }
          />
        </span>
      ),
    },
    {
      key: "effective",
      header: t("columnEffective"),
      width: "170px",
      render: (language) => (
        <span className="font-mono text-[12.5px] whitespace-nowrap">
          {t("effectiveLimit", {
            time: number(effectiveTimeLimitMs(draft.defaults, language)),
            memory: number(effectiveMemoryLimitMb(draft.defaults, language)),
          })}
        </span>
      ),
    },
    {
      key: "enabled",
      header: t("columnEnabled"),
      width: "64px",
      align: "right",
      render: (language) => (
        <Toggle
          checked={language.enabled}
          onCheckedChange={(enabled) =>
            patchLanguage(language.key, { enabled })
          }
          label={t("enableLanguage", { language: language.name })}
          disabled={saving}
        />
      ),
    },
  ];

  const defaultFields: { key: keyof JudgeDefaults; unit: string }[] = [
    { key: "timeLimitMs", unit: t("unitMs") },
    { key: "memoryLimitMb", unit: t("unitMb") },
    { key: "outputLimitKb", unit: t("unitKb") },
    { key: "compileTimeoutSec", unit: t("unitSec") },
  ];

  const sandboxFields: { key: keyof SandboxConfig }[] = [
    { key: "networkAccess" },
    { key: "limitChildProcesses" },
    { key: "returnStderr" },
  ];

  return (
    <div>
      <PageHeader
        title={t("title")}
        description={t("subtitle")}
        actions={
          <Link
            href="/admin/system-log"
            className="text-[12.5px] font-semibold text-[var(--color-primary)] hover:underline"
          >
            {t("historyLink")}
          </Link>
        }
      />

      <SegmentedTabs
        label={t("tabsLabel")}
        value={tab}
        onValueChange={setTab}
        className="mb-4 w-fit"
        options={[
          { value: "languages", label: t("tabLanguages") },
          { value: "advanced", label: t("tabAdvanced") },
        ]}
      />

      {tab === "languages" ? (
        <Card
          title={t("languagesTitle")}
          description={t("languagesSubtitle")}
          className="min-w-0"
        >
          <fieldset className="mb-4 rounded-2xl border border-[var(--color-border)] px-3.5 py-2.5">
            <legend className="px-1.5 text-[12.5px] font-semibold text-[var(--color-text-muted)]">
              {t("defaultsTitle")} · {t("defaultsSubtitle")}
            </legend>
            <div className="flex flex-wrap items-center gap-x-5 gap-y-2">
              {defaultFields.map(({ key, unit }) => (
                <span key={key} className="flex items-center gap-1.5">
                  <span className="text-[12.5px] font-medium whitespace-nowrap">
                    {t(`defaults.${key}.label`)}
                  </span>
                  <TextField
                    label={t(`defaults.${key}.label`)}
                    hideLabel
                    type="number"
                    min={1}
                    value={draft.defaults[key]}
                    disabled={saving}
                    onChange={(event) =>
                      patchDefaults({ [key]: Number(event.target.value) })
                    }
                    invalid={attempted && badDefault(draft.defaults[key])}
                    className="h-8 w-24 font-mono"
                    wrapperClassName="w-auto"
                  />
                  <span className="font-mono text-xs text-[var(--color-text-muted)]">
                    {unit}
                  </span>
                </span>
              ))}
            </div>
          </fieldset>
          <DataTable
            caption={t("languagesTitle")}
            columns={columns}
            rows={draft.languages}
            rowKey={(language) => language.key}
            emptyMessage={tCommon("empty")}
            minWidth={620}
          />
        </Card>
      ) : (
        <div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
          <Card title={t("sandboxTitle")} className="min-w-0">
            <div className="flex flex-col gap-2.5">
              {sandboxFields.map(({ key }) => (
                <SettingRow
                  key={key}
                  label={t(`sandbox.${key}.label`)}
                  description={t(`sandbox.${key}.meta`)}
                >
                  <Toggle
                    checked={draft.sandbox[key]}
                    onCheckedChange={(value) => patchSandbox({ [key]: value })}
                    label={t(`sandbox.${key}.label`)}
                    disabled={saving}
                  />
                </SettingRow>
              ))}
            </div>
          </Card>

          <Card
            title={t("generator.title")}
            description={t("generator.subtitle")}
            className="min-w-0"
          >
            <div className="flex flex-col gap-2.5">
              <SettingRow
                label={t("generator.languageLabel")}
                description={t("generator.languageMeta")}
              >
                <span className="font-mono text-sm font-semibold">Python</span>
              </SettingRow>
              <SettingRow
                label={t("generator.runtime.label")}
                description={t("generator.runtime.meta")}
              >
                <span className="flex items-center gap-1.5">
                  <TextField
                    label={t("generator.runtime.label")}
                    hideLabel
                    type="number"
                    min={GENERATOR_RUNTIME_RANGE.min}
                    max={GENERATOR_RUNTIME_RANGE.max}
                    value={draft.generator.maxRuntimeSeconds}
                    disabled={saving}
                    onChange={(event) =>
                      patchGenerator({
                        maxRuntimeSeconds: Number(event.target.value),
                      })
                    }
                    invalid={
                      attempted && badRuntime(draft.generator.maxRuntimeSeconds)
                    }
                    className="h-8 w-24 font-mono"
                    wrapperClassName="w-auto"
                  />
                  <span className="font-mono text-xs text-[var(--color-text-muted)]">
                    {t("unitSec")}
                  </span>
                </span>
              </SettingRow>
              <SettingRow
                label={t("generator.output.label")}
                description={t("generator.output.meta")}
              >
                <span className="flex items-center gap-1.5">
                  <TextField
                    label={t("generator.output.label")}
                    hideLabel
                    type="number"
                    min={GENERATOR_OUTPUT_RANGE.min}
                    max={GENERATOR_OUTPUT_RANGE.max}
                    value={draft.generator.maxOutputMb}
                    disabled={saving}
                    onChange={(event) =>
                      patchGenerator({
                        maxOutputMb: Number(event.target.value),
                      })
                    }
                    invalid={
                      attempted && badOutput(draft.generator.maxOutputMb)
                    }
                    className="h-8 w-24 font-mono"
                    wrapperClassName="w-auto"
                  />
                  <span className="font-mono text-xs text-[var(--color-text-muted)]">
                    {t("unitMb")}
                  </span>
                </span>
              </SettingRow>
            </div>
          </Card>
        </div>
      )}

      <SaveBar
        dirtyCount={dirtyCount}
        saving={saving}
        onSave={save}
        onDiscard={() => {
          setDraft(saved);
          setAttempted(false);
        }}
        unsavedLabel={t("unsavedCount", { count: dirtyCount })}
        discardLabel={t("discard")}
        saveLabel={saving ? t("saving") : t("save")}
        note={t("appliesNote")}
      />

      <ConfirmDialog
        open={confirming}
        onClose={() => setConfirming(false)}
        onConfirm={async () => {
          setConfirming(false);
          await commit();
        }}
        title={t("confirmDisable.title")}
        confirmLabel={t("confirmDisable.yes")}
        cancelLabel={t("confirmDisable.no")}
        destructive
      >
        <p className="text-[13.5px]">
          {t("confirmDisable.body", {
            languages: disabledNow.map((language) => language.name).join(", "),
          })}
        </p>
      </ConfirmDialog>

      <UnsavedChangesDialog
        open={leaveGuard.pending}
        title={t("unsaved.title")}
        saveAndLeaveLabel={t("unsaved.saveAndLeave")}
        leaveLabel={t("unsaved.leave")}
        stayLabel={t("unsaved.stay")}
        pending={saving}
        // Save-and-leave runs the same rules but skips the disable confirmation: choosing it is
        // already an explicit decision to keep the draft as it is.
        onSaveAndLeave={async () => {
          if (passes() && (await commit())) leaveGuard.leave();
          else leaveGuard.stay();
        }}
        onLeave={leaveGuard.leave}
        onStay={leaveGuard.stay}
      >
        {t("unsaved.body")}
      </UnsavedChangesDialog>
    </div>
  );
}

/** A small multiplier box with a visible two-letter tag in front, since two sit side by side. */
function MultiplierField({
  tag,
  label,
  value,
  disabled,
  invalid,
  onChange,
}: {
  tag: string;
  label: string;
  value: number;
  disabled: boolean;
  invalid: boolean;
  onChange: (value: number) => void;
}) {
  return (
    <span className="flex items-center gap-1">
      <span
        aria-hidden="true"
        className="text-[11px] font-semibold text-[var(--color-text-subtle)]"
      >
        {tag}
      </span>
      <TextField
        label={label}
        hideLabel
        type="number"
        min={0.1}
        step={0.1}
        value={value}
        disabled={disabled}
        onChange={(event) => onChange(Number(event.target.value))}
        invalid={invalid}
        className="h-8 font-mono"
        wrapperClassName="w-[68px]"
      />
    </span>
  );
}
