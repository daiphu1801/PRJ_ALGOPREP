// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 9.
//
// Layout follows 09-layoutBase/Admin - Soạn đề bài.dc.html: sticky header (:147-156), then a
// 1fr / 300px grid — four tabs on the left (:158-311) and a sticky property/checklist column on
// the right (:313-378).
//
// About PROTOTYPE_DEBT 2.6: that entry says six things still need removing from the mockup (the
// "Điểm" column, per-row weights, the "Chấm điểm từng phần" block, `totalWeight`, the weight*
// variables, and the "Tổng trọng số bằng 100" publish check). They are ALREADY GONE — grep finds no
// "trọng số" anywhere in that file. The debt entry is the stale one, not the prototype; flagged in
// the phase report so the ledger gets corrected rather than re-actioned.
//
// One divergence: the mockup's third AI guard, "Cho phép AI mở gợi ý ẩn", is dropped. Revealing
// tiered hints is the feature DEC-2026-0831-remove-tiered-hints-ai-config cut entirely. Whether
// that label meant something else is raised in the report rather than guessed at.
"use client";

import { useState } from "react";
import Link from "next/link";
import {
  AI_GUARD_KEYS,
  fetchProblemDraft,
  type AiGuardKey,
  type ProblemDraft,
  type ProblemLimits,
  type Testcase,
} from "@/entities/problem-draft";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  DataTable,
  NoticeTile,
  PageHeader,
  SegmentedTabs,
  SettingRow,
  TextArea,
  TextField,
  Toggle,
  type DataTableColumn,
} from "@/shared/ui";

type TabKey = "content" | "examples" | "testcases" | "ai";

const LIMIT_KEYS: (keyof ProblemLimits)[] = [
  "timeLimitMs",
  "memoryLimitMb",
  "outputLimitKb",
  "stackLimitMb",
];

export function ProblemAuthoringView() {
  const t = useT("problemAuthoring");
  const [draft, setDraft] = useState<ProblemDraft>(fetchProblemDraft);
  const [tab, setTab] = useState<TabKey>("content");

  function patch(changes: Partial<ProblemDraft>) {
    setDraft((previous) => ({ ...previous, ...changes }));
  }

  const publicTestcases = draft.testcases.filter((row) => row.visibility === "public").length;

  // dc.html:623-628 — the publish checklist, minus the weight-total rule that went with partial
  // scoring.
  const checklist = [
    { key: "minTestcases", done: draft.testcases.length >= 8 },
    { key: "minPublic", done: publicTestcases >= 2 },
    { key: "solutionPasses", done: true },
    { key: "minExamples", done: draft.examples.length >= 2 },
  ];
  const blocking = checklist.filter((item) => !item.done);

  const testcaseColumns: DataTableColumn<Testcase>[] = [
    {
      key: "index",
      header: "#",
      width: "40px",
      render: (row) => (
        <span className="font-mono text-[11.5px] text-[var(--color-text-subtle)]">
          {draft.testcases.indexOf(row) + 1}
        </span>
      ),
    },
    {
      key: "input",
      header: t("columnInput"),
      render: (row) => <span className="block truncate font-mono text-[12.5px]">{row.input}</span>,
    },
    {
      key: "expected",
      header: t("columnExpected"),
      width: "220px",
      render: (row) => (
        <span className="block truncate font-mono text-[12.5px] text-[var(--color-text-muted)]">
          {row.expected}
        </span>
      ),
    },
    {
      key: "visibility",
      header: t("columnVisibility"),
      width: "120px",
      render: (row) => (
        <Badge variant={row.visibility === "public" ? "success" : "neutral"}>
          {t(`visibility.${row.visibility}`)}
        </Badge>
      ),
    },
  ];

  return (
    <div>
      <PageHeader
        title={draft.title}
        description={t("subtitle", { topic: draft.topic })}
        actions={
          <>
            <Button asChild variant="ghost" size="sm" className="border border-[var(--color-border)]">
              <Link href="/admin/problems">{t("back")}</Link>
            </Button>
            <Button variant="ghost" size="sm" className="border border-[var(--color-border)]">
              {t("previewAsLearner")}
            </Button>
            <Button variant="cta" size="sm" disabled={blocking.length > 0}>
              {t("publish")}
            </Button>
          </>
        }
      />

      <div className="grid grid-cols-1 items-start gap-4 xl:grid-cols-[minmax(0,1fr)_300px]">
        <div className="flex min-w-0 flex-col gap-3.5">
          <Card className="px-1.5 py-1.5">
            <SegmentedTabs
              label={t("tabsLabel")}
              value={tab}
              onValueChange={setTab}
              className="border-0 bg-transparent p-0"
              options={[
                { value: "content", label: t("tab.content") },
                { value: "examples", label: t("tab.examples", { count: draft.examples.length }) },
                { value: "testcases", label: t("tab.testcases", { count: draft.testcases.length }) },
                { value: "ai", label: t("tab.ai") },
              ]}
            />
          </Card>

          {tab === "content" ? (
            <>
              <Card title={t("statementTitle")} description={t("statementSubtitle")}>
                <div className="flex flex-col gap-3">
                  <TextField
                    label={t("titleLabel")}
                    value={draft.title}
                    onChange={(event) => patch({ title: event.target.value })}
                  />
                  <TextArea
                    label={t("bodyLabel")}
                    rows={12}
                    value={draft.body}
                    onChange={(event) => patch({ body: event.target.value })}
                    className="font-mono text-[12.5px] leading-relaxed"
                  />
                </div>
              </Card>

              <Card title={t("limitsTitle")} description={t("limitsSubtitle")}>
                <div className="mb-4 grid gap-3 [grid-template-columns:repeat(auto-fit,minmax(150px,1fr))]">
                  {LIMIT_KEYS.map((key) => (
                    <TextField
                      key={key}
                      label={t(`limit.${key}`)}
                      type="number"
                      min={1}
                      value={draft.limits[key]}
                      onChange={(event) =>
                        patch({ limits: { ...draft.limits, [key]: Number(event.target.value) } })
                      }
                      className="font-mono"
                    />
                  ))}
                </div>
                <TextArea
                  label={t("constraintsLabel")}
                  rows={4}
                  value={draft.constraints}
                  onChange={(event) => patch({ constraints: event.target.value })}
                  className="font-mono text-[12.5px]"
                />
              </Card>

              <Card title={t("solutionTitle")} description={t("solutionSubtitle")}>
                <TextArea
                  label={t("solutionLabel", { language: draft.solutionLanguage })}
                  rows={14}
                  value={draft.solution}
                  onChange={(event) => patch({ solution: event.target.value })}
                  className="font-mono text-[12.5px] leading-relaxed"
                />
              </Card>
            </>
          ) : null}

          {tab === "examples" ? (
            <Card title={t("examplesTitle")} description={t("examplesSubtitle")}>
              <div className="flex flex-col gap-2.5">
                {draft.examples.map((example, index) => (
                  <div
                    key={example.id}
                    className="glass-surface rounded-2xl border border-[var(--color-border)] px-3.5 py-3"
                  >
                    <p className="mb-2 text-[11px] font-semibold tracking-[0.08em] text-[var(--color-text-subtle)] uppercase">
                      {t("exampleIndex", { index: index + 1 })}
                    </p>
                    <dl className="flex flex-col gap-1.5 text-[12.5px]">
                      <div className="flex gap-2">
                        <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">
                          {t("exampleInput")}
                        </dt>
                        <dd className="min-w-0 font-mono">{example.input}</dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">
                          {t("exampleOutput")}
                        </dt>
                        <dd className="min-w-0 font-mono">{example.output}</dd>
                      </div>
                      <div className="flex gap-2">
                        <dt className="w-20 shrink-0 text-[var(--color-text-subtle)]">
                          {t("exampleExplanation")}
                        </dt>
                        <dd className="min-w-0 text-pretty text-[var(--color-text-muted)]">
                          {example.explanation}
                        </dd>
                      </div>
                    </dl>
                  </div>
                ))}
              </div>
            </Card>
          ) : null}

          {tab === "testcases" ? (
            <Card
              title={t("testcasesTitle")}
              description={t("testcasesSubtitle", {
                total: draft.testcases.length,
                publicCount: publicTestcases,
              })}
              className="min-w-0"
            >
              {/* No "Điểm" column and no weight block — PROTOTYPE_DEBT 2.6. The partial score that
                  survived (F4-13) is an automatic pass ratio, not an author-declared weight. */}
              <DataTable
                caption={t("testcasesTitle")}
                columns={testcaseColumns}
                rows={draft.testcases}
                rowKey={(row) => row.id}
                emptyMessage={t("emptyTestcases")}
                minWidth={720}
              />
            </Card>
          ) : null}

          {tab === "ai" ? (
            <Card title={t("aiTitle")} description={t("aiSubtitle")}>
              <TextArea
                label={t("aiBriefLabel")}
                rows={5}
                value={draft.aiBrief}
                onChange={(event) => patch({ aiBrief: event.target.value })}
                className="font-mono text-[12.5px]"
              />
              <div className="mt-3.5 flex flex-col gap-2.5">
                {AI_GUARD_KEYS.map((key: AiGuardKey) => (
                  <SettingRow
                    key={key}
                    label={t(`aiGuard.${key}.label`)}
                    description={t(`aiGuard.${key}.meta`)}
                  >
                    <Toggle
                      checked={draft.aiGuards[key]}
                      onCheckedChange={(value) =>
                        patch({ aiGuards: { ...draft.aiGuards, [key]: value } })
                      }
                      label={t(`aiGuard.${key}.label`)}
                    />
                  </SettingRow>
                ))}
              </div>
            </Card>
          ) : null}
        </div>

        <div className="flex flex-col gap-3.5 xl:sticky xl:top-[92px]">
          <Card title={t("propertiesTitle")}>
            <div className="flex flex-col gap-3.5">
              <TextField
                label={t("topicLabel")}
                value={draft.topic}
                onChange={(event) => patch({ topic: event.target.value })}
              />
              <div>
                <p className="mb-1.5 text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
                  {t("difficultyLabel")}
                </p>
                <SegmentedTabs
                  label={t("difficultyLabel")}
                  value={draft.difficulty}
                  onValueChange={(value) => patch({ difficulty: value })}
                  className="w-full"
                  options={[
                    { value: "easy" as const, label: t("difficulty.easy") },
                    { value: "medium" as const, label: t("difficulty.medium") },
                    { value: "hard" as const, label: t("difficulty.hard") },
                  ]}
                />
              </div>
              <div>
                <p className="mb-1.5 text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
                  {t("statusLabel")}
                </p>
                {/* Two states only — DEC-2026-0830-problem-lifecycle-two-states. */}
                <SegmentedTabs
                  label={t("statusLabel")}
                  value={draft.status}
                  onValueChange={(value) => patch({ status: value })}
                  className="w-full"
                  options={[
                    { value: "draft" as const, label: t("status.draft") },
                    { value: "published" as const, label: t("status.published") },
                  ]}
                />
              </div>
            </div>
          </Card>

          <Card title={t("checklistTitle")}>
            {blocking.length > 0 ? (
              <NoticeTile tone="warn" title={t("checklistBlockTitle")} className="mb-3">
                {t("checklistBlockBody", { count: blocking.length })}
              </NoticeTile>
            ) : null}
            <ul className="flex flex-col gap-2">
              {checklist.map((item) => (
                <li key={item.key} className="flex items-start gap-2 text-[12.5px]">
                  <Badge variant={item.done ? "success" : "warn"} className="shrink-0">
                    {item.done ? t("checkDone") : t("checkPending")}
                  </Badge>
                  <span
                    className={
                      item.done ? "text-[var(--color-text-muted)]" : "text-[var(--color-text)]"
                    }
                  >
                    {t(`check.${item.key}`)}
                  </span>
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </div>
    </div>
  );
}
