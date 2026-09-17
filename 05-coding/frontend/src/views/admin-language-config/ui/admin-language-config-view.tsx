// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Ngôn ngữ và giới hạn.dc.html 1:1: sticky header with the
// primary CTA (:146-159), then a two-column grid 1.65fr / minmax(290px,1fr) (:161) with the
// language table on the left (:163-203) and three stacked cards on the right (:205-242).
//
// Three deliberate divergences from that mockup, each required by a document that outranks it:
//
// 1. "Hệ số TG" is an input, not static text. F2-10/F4-11 require the multiplier to be editable;
//    the static mockup simply had nowhere to type (02-bd/screens/admin/admin_language_config.md
//    section 3.1 says so outright).
// 2. "Giới hạn mặc định" values are inputs for the same reason (BD section 3.2) — the mockup shows
//    them read-only. The exact widget is left to DD; a plain number field is the minimum that makes
//    the screen do what the BD says it does.
// 3. The "Lưu ý khi đổi giới hạn" card drops the mockup's link to a rejudge session. Rejudge was
//    removed from scope entirely by DEC-2026-0828-remove-rejudge-scope, and the screen's own RD was
//    already updated to say limits apply only to new submissions
//    (01-rd/screens/admin/admin_language_config.md:39-41). BD section 3.4 dictates the replacement
//    wording used here.
//
// Also note: BD section 3.1 has columns 1 and 2 swapped relative to the markup it cites — it reads
// the compiler string as sitting under the language name and `go-judge #<id>` as the "Trình biên
// dịch" column. The markup does the opposite (dc.html:187-188 puts judgeId under the name, :190
// puts the compiler in its own column), and the markup is self-consistent with the column header.
// Followed the markup.
"use client";

import { useState } from "react";
import {
  effectiveMemoryLimitMb,
  effectiveTimeLimitMs,
  fetchLanguageConfigPage,
  type JudgeDefaults,
  type LanguageConfig,
  type LanguageConfigPage,
  type SandboxConfig,
} from "@/entities/language-config";
import { useLocale, useT } from "@/shared/i18n";
import {
  Button,
  Card,
  DataTable,
  PageHeader,
  SettingRow,
  TextField,
  Toggle,
  type DataTableColumn,
} from "@/shared/ui";

const CHIP_STYLE: Record<LanguageConfig["key"], string> = {
  py: "bg-[var(--color-lang-py-bg)] text-[var(--color-lang-py-fg)]",
  cpp: "bg-[var(--color-lang-cpp-bg)] text-[var(--color-lang-cpp-fg)]",
  java: "bg-[var(--color-lang-java-bg)] text-[var(--color-lang-java-fg)]",
};

export function AdminLanguageConfigView() {
  const t = useT("adminLanguageConfig");
  const tCommon = useT("common");
  const locale = useLocale();

  // Mock read happens once; `saved` is the baseline the dirty check compares against, so saving
  // resets it rather than re-fetching (there is no endpoint to re-fetch from yet).
  const [saved, setSaved] = useState<LanguageConfigPage>(fetchLanguageConfigPage);
  const [draft, setDraft] = useState<LanguageConfigPage>(saved);
  const [saving, setSaving] = useState(false);

  const dirty = JSON.stringify(draft) !== JSON.stringify(saved);
  const number = (value: number) => value.toLocaleString(locale);

  function patchLanguage(key: LanguageConfig["key"], patch: Partial<LanguageConfig>) {
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

  function patchSandbox(patch: Partial<SandboxConfig>) {
    setDraft((prev) => ({ ...prev, sandbox: { ...prev.sandbox, ...patch } }));
  }

  async function save() {
    setSaving(true);
    // No endpoint yet — the delay stands in for the round trip so the `saving` state is reviewable.
    await new Promise((resolve) => setTimeout(resolve, 600));
    setSaved(draft);
    setSaving(false);
  }

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
            <span className="block font-semibold whitespace-nowrap">{language.name}</span>
            <span className="block font-mono text-[11.5px] whitespace-nowrap text-[var(--color-text-subtle)]">
              {language.judgeId}
            </span>
          </span>
        </div>
      ),
    },
    {
      key: "compiler",
      header: t("columnCompiler"),
      width: "150px",
      render: (language) => (
        <span className="font-mono text-[12.5px] whitespace-nowrap text-[var(--color-text-muted)]">
          {language.compiler}
        </span>
      ),
    },
    {
      key: "multiplier",
      header: t("columnTimeMultiplier"),
      width: "96px",
      render: (language) => (
        <TextField
          label={t("timeMultiplierFor", { language: language.name })}
          hideLabel
          type="number"
          min={0.1}
          step={0.1}
          value={language.timeMultiplier}
          disabled={saving}
          onChange={(event) =>
            patchLanguage(language.key, { timeMultiplier: Number(event.target.value) })
          }
          className="h-8 font-mono"
        />
      ),
    },
    {
      key: "timeLimit",
      header: t("columnTimeLimit"),
      width: "104px",
      render: (language) => (
        <span className="font-mono text-[12.5px] whitespace-nowrap">
          {t("milliseconds", { value: number(effectiveTimeLimitMs(draft.defaults, language)) })}
        </span>
      ),
    },
    {
      key: "memory",
      header: t("columnMemory"),
      width: "88px",
      render: (language) => (
        <span className="font-mono text-[12.5px] whitespace-nowrap">
          {t("megabytes", { value: number(effectiveMemoryLimitMb(draft.defaults, language)) })}
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
          onCheckedChange={(enabled) => patchLanguage(language.key, { enabled })}
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
          <Button
            variant="cta"
            size="sm"
            onClick={save}
            disabled={!dirty || saving}
            aria-busy={saving || undefined}
          >
            {saving ? tCommon("loading") : t("save")}
          </Button>
        }
      />

      <div className="grid grid-cols-1 gap-4 lg:grid-cols-[minmax(0,1.65fr)_minmax(290px,1fr)]">
        <Card title={t("languagesTitle")} description={t("languagesSubtitle")} className="min-w-0">
          <DataTable
            caption={t("languagesTitle")}
            columns={columns}
            rows={draft.languages}
            rowKey={(language) => language.key}
            emptyMessage={tCommon("empty")}
            minWidth={620}
          />
        </Card>

        <div className="flex min-w-0 flex-col gap-4">
          <Card title={t("defaultsTitle")} description={t("defaultsSubtitle")}>
            <div className="flex flex-col gap-2.5">
              {defaultFields.map(({ key, unit }) => (
                <SettingRow
                  key={key}
                  label={t(`defaults.${key}.label`)}
                  description={t(`defaults.${key}.meta`)}
                >
                  <span className="flex items-center gap-1.5">
                    <TextField
                      label={t(`defaults.${key}.label`)}
                      hideLabel
                      type="number"
                      min={1}
                      value={draft.defaults[key]}
                      disabled={saving}
                      onChange={(event) => patchDefaults({ [key]: Number(event.target.value) })}
                      className="h-8 w-24 font-mono"
                      wrapperClassName="w-auto"
                    />
                    <span className="font-mono text-xs text-[var(--color-text-muted)]">{unit}</span>
                  </span>
                </SettingRow>
              ))}
            </div>
          </Card>

          <Card title={t("sandboxTitle")}>
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

          <Card title={t("noticeTitle")}>
            <p className="text-[13px] leading-relaxed text-[var(--color-text-muted)]">
              {t("noticeBody")}
            </p>
          </Card>
        </div>
      </div>
    </div>
  );
}
