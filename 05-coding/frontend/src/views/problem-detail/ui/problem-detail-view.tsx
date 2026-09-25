// PROTOTYPE — chưa có DD. Xem 06-plan/PROTOTYPE_DEBT.md mục 9.
//
// Layout theo 09-layoutBase/Workspace giải bài.dc.html và 02-bd/screens/users/USR0102_problem_detail.md
// Sheet 4.4: thanh tác vụ (:98-107), panel thông tin bài toán cột trái (:158-256, ba tab Đề bài/Bài
// nộp/Solution Review), vùng soạn mã cột phải hàng trên (:264-325), bảng kết quả cột phải hàng dưới
// (:327-392).
//
// Ponytail — cắt bớt so với BD cho vừa scope prototype, ghi rõ để không lẫn với hành vi thật:
// - Không có thanh chia đôi kéo được (`shared/ui/split-pane`, BD :258-260) — grid tỉ lệ cố định 47/53%.
//   Thêm khi có nhu cầu resize thật.
// - Không có kênh WebSocket/STOMP thật (F4-08) — kết quả chấm hiện sau một khoảng trễ giả lập
//   cố định, không phải cập nhật từng testcase một qua kênh thời gian thực. Thay bằng
//   `shared/api/stomp` thật lúc lên production (BD Sheet 4.5).
// - Vùng soạn mã dùng <textarea> thay Monaco — Monaco Editor (README.md mục 5) cần gói riêng và
//   cấu hình build, để lại TODO dưới đây thay vì thêm một thư viện editor tạm bợ.
"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { Play, Send, Upload } from "lucide-react";
import {
  fetchProblemDetail,
  simulateSubmission,
  type Difficulty,
  type InputMethod,
  type StarterCodeKey,
  type SubmissionLanguage,
  type SubmissionRunResult,
  type WrapperMode,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { Badge, Button, Card, EmptyState, SegmentedTabs, type BadgeVariant } from "@/shared/ui";

const DIFFICULTY_VARIANT: Record<Difficulty, BadgeVariant> = {
  easy: "success",
  medium: "warn",
  hard: "negative",
};

const LANGUAGES: SubmissionLanguage[] = ["java", "cpp", "python"];
const TOTAL_TESTCASES = 12;

type Tab = "statement" | "submissions" | "review";
type ConsoleTab = "testcase" | "result";

export function ProblemDetailView() {
  const params = useParams<{ problemId?: string }>();
  const problemId = params?.problemId;
  const t = useT("problemDetail");
  const problem = useMemo(() => (problemId ? fetchProblemDetail(problemId) : undefined), [problemId]);

  const [tab, setTab] = useState<Tab>("statement");
  const [language, setLanguage] = useState<SubmissionLanguage>("java");
  const [mode, setMode] = useState<WrapperMode>("function");
  const [inputMethod, setInputMethod] = useState<InputMethod>("type");
  const [uploadedFileName, setUploadedFileName] = useState<string | null>(null);
  const [codeByKey, setCodeByKey] = useState<Partial<Record<StarterCodeKey, string>>>({});
  const [consoleTab, setConsoleTab] = useState<ConsoleTab>("testcase");
  const [sampleRun, setSampleRun] = useState<{ passed: boolean }[] | null>(null);
  const [running, setRunning] = useState(false);
  const [submitAttempt, setSubmitAttempt] = useState(0);
  const [result, setResult] = useState<SubmissionRunResult | null>(null);
  const [saved, setSaved] = useState(false);

  if (!problem) {
    return (
      <div className="p-6">
        <EmptyState>{t("empty.title")}</EmptyState>
        <p className="mt-3 text-[12.5px] text-[var(--color-text-muted)]">
          <Link href="/problems" className="underline">
            {t("empty.linkList")}
          </Link>
        </p>
      </div>
    );
  }

  // Rebound to a fresh const so its type is ProblemDetail (not ProblemDetail | undefined) for the
  // nested closures below — TS does not carry the guard's narrowing of `problem` itself into them.
  const detail = problem;
  const effectiveMode: WrapperMode = detail.submissionModel === "stdioOnly" ? "stdio" : mode;
  const key = `${language}:${effectiveMode}` as StarterCodeKey;
  const code = codeByKey[key] ?? detail.starterCode[key] ?? "";

  function setCode(value: string) {
    setCodeByKey((previous) => ({ ...previous, [key]: value }));
  }

  function runSample() {
    setRunning(true);
    setConsoleTab("testcase");
    window.setTimeout(() => {
      // ponytail: giả lập luôn Pass hết testcase mẫu — không chạy mã thật (chưa nối go-judge).
      setSampleRun(detail.sampleTestcases.map(() => ({ passed: true })));
      setRunning(false);
    }, 400);
  }

  function submit() {
    setRunning(true);
    setConsoleTab("result");
    setResult(null);
    window.setTimeout(() => {
      const attempt = submitAttempt + 1;
      setSubmitAttempt(attempt);
      // ponytail: kết quả xen kẽ Sai/Đạt theo lượt nộp để thấy được cả hai nhánh UI mà không cần
      // logic chấm thật — nâng cấp khi có JudgeExecutionPort + kênh thời gian thực.
      const run =
        attempt % 2 === 1
          ? simulateSubmission(TOTAL_TESTCASES)
          : {
              submissionId: `sub-${attempt}`,
              overallVerdict: "accepted" as const,
              passedCount: TOTAL_TESTCASES,
              totalCount: TOTAL_TESTCASES,
              testcases: Array.from({ length: TOTAL_TESTCASES }, (_, index) => ({
                order: index + 1,
                verdict: "passed" as const,
              })),
            };
      setResult(run);
      setRunning(false);
    }, 700);
  }

  return (
    <div className="flex h-[calc(100vh-56px)] flex-col overflow-hidden p-4">
      <div className="mb-3 flex shrink-0 items-center gap-3">
        <Link href="/problems" className="text-[12.5px] font-semibold underline">
          {t("taskbar.linkList")}
        </Link>
        <span className="font-mono text-xs text-[var(--color-text-muted)]">{detail.code}</span>
        <h1 className="truncate text-[13.5px] font-semibold">{detail.title}</h1>
        <Badge variant={DIFFICULTY_VARIANT[detail.difficulty]}>{t(`difficulty.${detail.difficulty}`)}</Badge>
        <div className="ml-auto flex gap-2">
          <Button variant="ghost" size="sm" className="border border-[var(--color-border)]" onClick={runSample} disabled={running}>
            <Play className="mr-1.5 h-3.5 w-3.5" />
            {t("taskbar.btnRun")}
          </Button>
          <Button variant="cta" size="sm" onClick={submit} disabled={running}>
            <Send className="mr-1.5 h-3.5 w-3.5" />
            {t("taskbar.btnSubmit")}
          </Button>
        </div>
      </div>

      <div className="grid min-h-0 flex-1 gap-3 [grid-template-columns:47%_53%]">
        <Card className="flex min-h-0 flex-col px-4 py-3">
          <SegmentedTabs
            label={t("panel.tabsLabel")}
            value={tab}
            onValueChange={setTab}
            options={[
              { value: "statement", label: t("panel.tabStatement") },
              { value: "submissions", label: t("panel.tabSubmissions") },
              { value: "review", label: t("panel.tabReview") },
            ]}
            className="mb-3 shrink-0"
          />

          <div className="min-h-0 flex-1 overflow-y-auto pr-1">
            {tab === "statement" ? (
              <div>
                <div className="mb-2 flex flex-wrap gap-1.5">
                  {detail.topics.map((topicName) => (
                    <Badge key={topicName} variant="cyan">
                      {topicName}
                    </Badge>
                  ))}
                  <Badge variant={detail.submissionModel === "both" ? "teal" : "neutral"}>
                    {t(`submissionModel.${detail.submissionModel}`)}
                  </Badge>
                </div>
                <p className="mb-3 text-[13px] whitespace-pre-line">{detail.statementMd}</p>
                <h3 className="mb-1.5 text-[12.5px] font-semibold">{t("panel.examplesTitle")}</h3>
                <ul className="mb-3 flex flex-col gap-2">
                  {detail.examples.map((example, index) => (
                    <li key={index} className="rounded-lg border border-[var(--color-border)] p-2.5 text-[12.5px]">
                      <p><span className="font-semibold">Input:</span> {example.input}</p>
                      <p><span className="font-semibold">Output:</span> {example.output}</p>
                      {example.explanation ? <p className="text-[var(--color-text-muted)]">{example.explanation}</p> : null}
                    </li>
                  ))}
                </ul>
                <h3 className="mb-1.5 text-[12.5px] font-semibold">{t("panel.constraintsTitle")}</h3>
                <ul className="list-disc pl-4 text-[12.5px] text-[var(--color-text-muted)]">
                  {detail.constraints.map((constraint) => (
                    <li key={constraint}>{constraint}</li>
                  ))}
                </ul>
              </div>
            ) : null}

            {tab === "submissions" ? (
              detail.mySubmissions.length === 0 ? (
                <EmptyState>{t("panel.noSubmissions")}</EmptyState>
              ) : (
                <ul className="flex flex-col gap-2">
                  {detail.mySubmissions.map((submission) => (
                    <li key={submission.id} className="flex items-center justify-between rounded-lg border border-[var(--color-border)] p-2.5 text-[12.5px]">
                      <span className="font-semibold">{submission.verdictLabel}</span>
                      <span className="text-[var(--color-text-muted)]">
                        {t(`language.${submission.language}`)} · {submission.submittedAtLabel}
                      </span>
                    </li>
                  ))}
                </ul>
              )
            ) : null}

            {tab === "review" ? (
              detail.solutionReview ? (
                <div>
                  <div className="mb-3 grid grid-cols-2 gap-2">
                    <div className="rounded-lg border border-[var(--color-border)] p-2.5 text-center">
                      <p className="text-[11px] text-[var(--color-text-muted)]">{t("panel.timeComplexity")}</p>
                      <p className="font-mono text-[15px] font-semibold">{detail.solutionReview.timeComplexity}</p>
                    </div>
                    <div className="rounded-lg border border-[var(--color-border)] p-2.5 text-center">
                      <p className="text-[11px] text-[var(--color-text-muted)]">{t("panel.spaceComplexity")}</p>
                      <p className="font-mono text-[15px] font-semibold">{detail.solutionReview.spaceComplexity}</p>
                    </div>
                  </div>
                  <ul className="list-disc pl-4 text-[12.5px] text-[var(--color-text-muted)]">
                    {detail.solutionReview.notes.map((note) => (
                      <li key={note}>{note}</li>
                    ))}
                  </ul>
                  <Link href={`/submissions/${detail.mySubmissions[0]?.id ?? ""}/interview`} className="mt-3 inline-block text-[12.5px] font-semibold underline">
                    {t("panel.linkMockInterview")}
                  </Link>
                </div>
              ) : (
                <EmptyState>{t("panel.noReview")}</EmptyState>
              )
            ) : null}
          </div>

          <div className="mt-2 flex shrink-0 items-center justify-between border-t border-[var(--color-border)] pt-2">
            <span className="text-[11.5px] text-[var(--color-text-muted)]">
              AC rate: {detail.acRate === null ? "-" : `${detail.acRate}%`}
            </span>
            <Button variant="ghost" size="sm" className="border border-[var(--color-border)]" onClick={() => setSaved((v) => !v)}>
              {saved ? t("panel.btnUnsave") : t("panel.btnSave")}
            </Button>
          </div>
        </Card>

        <div className="grid min-h-0 grid-rows-[minmax(0,1fr)_minmax(160px,220px)] gap-3">
          <Card className="flex min-h-0 flex-col px-3 py-2.5">
            <div className="mb-2 flex shrink-0 flex-wrap items-center gap-2">
              <SegmentedTabs
                label={t("toolbar.languageLabel")}
                value={language}
                onValueChange={setLanguage}
                options={LANGUAGES.map((lang) => ({ value: lang, label: t(`language.${lang}`) }))}
              />
              {detail.submissionModel === "both" ? (
                <SegmentedTabs
                  label={t("toolbar.modeLabel")}
                  value={mode}
                  onValueChange={setMode}
                  options={[
                    { value: "stdio", label: t("mode.stdio") },
                    { value: "function", label: t("mode.function") },
                  ]}
                />
              ) : (
                <Badge variant="neutral">{t("mode.stdio")}</Badge>
              )}
              <SegmentedTabs
                label={t("toolbar.inputMethodLabel")}
                value={inputMethod}
                onValueChange={setInputMethod}
                options={[
                  { value: "type", label: t("inputMethod.type") },
                  { value: "upload", label: t("inputMethod.upload") },
                ]}
                className="ml-auto"
              />
            </div>

            {inputMethod === "type" ? (
              // TODO: wire Monaco khi có 03-dd/screens/users/USR0102_problem_detail.md
              <textarea
                aria-label={t("toolbar.codeAreaLabel")}
                value={code}
                onChange={(event) => setCode(event.target.value)}
                spellCheck={false}
                className="min-h-0 flex-1 resize-none rounded-lg border border-[var(--color-border)] bg-[var(--color-surface)] p-3 font-mono text-[12.5px] leading-relaxed text-[var(--color-text)] focus-visible:outline-2 focus-visible:outline-offset-1 focus-visible:outline-[var(--color-primary)]"
              />
            ) : (
              <label className="flex min-h-0 flex-1 cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-[var(--color-border)] text-[12.5px] text-[var(--color-text-muted)]">
                <Upload className="h-5 w-5" />
                {uploadedFileName ?? t("toolbar.uploadHint")}
                <input
                  type="file"
                  accept=".java,.cpp,.py"
                  className="sr-only"
                  onChange={(event) => setUploadedFileName(event.target.files?.[0]?.name ?? null)}
                />
              </label>
            )}
          </Card>

          <Card className="flex min-h-0 flex-col px-3 py-2.5">
            <div className="mb-2 flex shrink-0 items-center justify-between">
              <SegmentedTabs
                label={t("console.tabsLabel")}
                value={consoleTab}
                onValueChange={setConsoleTab}
                options={[
                  { value: "testcase", label: t("console.tabTestcase") },
                  { value: "result", label: t("console.tabResult") },
                ]}
              />
              {result ? (
                <Badge variant={result.overallVerdict === "accepted" ? "success" : "negative"}>
                  {t(`verdict.${result.overallVerdict}`)} {result.passedCount}/{result.totalCount}
                </Badge>
              ) : null}
            </div>

            <div className="min-h-0 flex-1 overflow-y-auto">
              {consoleTab === "testcase" ? (
                sampleRun ? (
                  <ul className="flex flex-col gap-1.5">
                    {detail.sampleTestcases.map((testcase, index) => (
                      <li key={index} className="flex items-center justify-between rounded-lg border border-[var(--color-border)] px-2.5 py-1.5 text-[12px]">
                        <span className="font-mono truncate">{testcase.input}</span>
                        <Badge variant={sampleRun[index]?.passed ? "success" : "negative"}>
                          {sampleRun[index]?.passed ? t("console.passed") : t("console.failed")}
                        </Badge>
                      </li>
                    ))}
                  </ul>
                ) : (
                  <p className="text-[12.5px] text-[var(--color-text-muted)]">{t("console.runHint")}</p>
                )
              ) : result ? (
                <div className="flex flex-wrap gap-1">
                  {result.testcases.map((testcase) => (
                    <span
                      key={testcase.order}
                      title={`#${testcase.order}`}
                      className="inline-block h-4 w-4 rounded-sm"
                      style={{
                        background: `var(${testcase.verdict === "passed" ? "--color-success" : "--color-admin-negative"})`,
                      }}
                    />
                  ))}
                </div>
              ) : running ? (
                <p className="text-[12.5px] text-[var(--color-text-muted)]">{t("console.judging")}</p>
              ) : (
                <p className="text-[12.5px] text-[var(--color-text-muted)]">{t("console.submitHint")}</p>
              )}
            </div>

            {result?.overallVerdict === "accepted" ? (
              <div className="mt-2 flex shrink-0 flex-wrap gap-2 border-t border-[var(--color-border)] pt-2">
                <Link href={`/submissions/${result.submissionId}`} className="text-[12px] font-semibold underline">
                  {t("console.linkResult")}
                </Link>
                <Link href={`/submissions/${result.submissionId}/review`} className="text-[12px] font-semibold underline">
                  {t("console.linkReview")}
                </Link>
                <Link href={`/submissions/${result.submissionId}/interview`} className="text-[12px] font-semibold underline">
                  {t("console.linkInterview")}
                </Link>
              </div>
            ) : null}
          </Card>
        </div>
      </div>
    </div>
  );
}
