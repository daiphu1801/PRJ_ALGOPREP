// PROTOTYPE — chưa có DD. Xem 06-plan/PROTOTYPE_DEBT.md mục 9.
//
// Layout theo 09-layoutBase/Workspace giải bài.dc.html và 02-bd/screens/users/USR0102_problem_detail.md
// Sheet 4.4: thanh tác vụ (:98-107), panel thông tin bài toán cột trái (:158-256, ba tab Đề bài/Bài
// nộp/Solution Review), vùng soạn mã cột phải hàng trên (:264-325), bảng kết quả cột phải hàng dưới
// (:327-392).
//
// Ponytail — cắt bớt so với BD cho vừa scope prototype, ghi rõ để không lẫn với hành vi thật:
// - Bố cục full màn hình kiểu LeetCode (2026-10-18): view gắn `data-fullscreen-view`, khung student
//   (globals.css) bỏ footer và giới hạn bề rộng, chiều cao còn lại chia bằng flex. Hai thanh chia đôi kéo
//   được là `shared/ui` `SplitPane` (cột đề/mã và vùng mã/kết quả), tỉ lệ nhớ trong localStorage. Dưới `lg`
//   các khối xếp dọc.
// - Không có kênh WebSocket/STOMP thật (F4-08) — kết quả chấm hiện sau một khoảng trễ giả lập
//   cố định, không phải cập nhật từng testcase một qua kênh thời gian thực. Thay bằng
//   `shared/api/stomp` thật lúc lên production (BD Sheet 4.5).
// - Vùng soạn mã dùng <textarea> thay Monaco — Monaco Editor (README.md mục 5) cần gói riêng và
//   cấu hình build, để lại TODO dưới đây thay vì thêm một thư viện editor tạm bợ.
"use client";

import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type DragEvent,
} from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { Play, Send, Upload } from "lucide-react";
import {
  fetchProblemDetail,
  problemLevelLabel,
  problemLevelTone,
  saveWorkspaceDraft,
  simulateSampleRun,
  simulateSubmission,
  useProblemLevels,
  workspaceDraftKey,
  type InputMethod,
  type ProblemDetail,
  type SampleRunResult,
  type StarterCodeKey,
  type SubmissionLanguage,
  type SubmissionRunResult,
  type WrapperMode,
} from "@/entities/problem";
import { useLocale, useT } from "@/shared/i18n";
import { formatDateVN } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Badge,
  Button,
  Card,
  EmptyState,
  MarkdownPreview,
  SegmentedTabs,
  SplitPane,
} from "@/shared/ui";

const LANGUAGES: SubmissionLanguage[] = ["java", "cpp", "python"];
const FILE_EXTENSION: Record<SubmissionLanguage, string> = {
  java: ".java",
  cpp: ".cpp",
  python: ".py",
};
// Typed code is capped like an uploaded file (USR0102 Q7, owner 2026-10-18); the server re-checks.
const MAX_CODE_BYTES = 64 * 1024;
const PREFS_STORAGE_KEY = "algoprep-workspace-prefs";

type Tab = "statement" | "submissions" | "review";
type ConsoleTab = "testcase" | "result";
type UploadedFile = { name: string; size: number; content: string };

function byteLength(text: string) {
  return new TextEncoder().encode(text).byteLength;
}

export function ProblemDetailView() {
  const params = useParams<{ problemId?: string }>();
  const problemId = params?.problemId;
  const t = useT("problemDetail");
  const problem = useMemo(
    () => (problemId ? fetchProblemDetail(problemId) : undefined),
    [problemId],
  );

  // BD Sheet 9 NO 2: an unknown or unpublished id reads as "not found", never as "no permission".
  if (!problemId || !problem) {
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

  // `key` remounts the workspace per problem so code, results and the save flag never leak between
  // two problems when Next reuses the page component for a new `[problemId]`.
  return <Workspace key={problemId} problemId={problemId} detail={problem} />;
}

function Workspace({
  problemId,
  detail,
}: {
  problemId: string;
  detail: ProblemDetail;
}) {
  const t = useT("problemDetail");
  const locale = useLocale() === "en" ? "en" : "vi";
  const router = useRouter();
  const levels = useProblemLevels();

  const [tab, setTab] = useState<Tab>("statement");
  const [language, setLanguage] = useState<SubmissionLanguage>("java");
  const [mode, setMode] = useState<WrapperMode>("function");
  const [inputMethod, setInputMethod] = useState<InputMethod>("type");
  const [uploaded, setUploaded] = useState<UploadedFile | null>(null);
  const [showUploaded, setShowUploaded] = useState(false);
  const [codeByKey, setCodeByKey] = useState<
    Partial<Record<StarterCodeKey, string>>
  >({});
  // True only for keys whose draft was really written to localStorage (it can be blocked or full).
  const [persistedKeys, setPersistedKeys] = useState<
    Partial<Record<StarterCodeKey, true>>
  >({});
  const [consoleTab, setConsoleTab] = useState<ConsoleTab>("testcase");
  const [caseIndex, setCaseIndex] = useState(0);
  const [sampleRun, setSampleRun] = useState<SampleRunResult | null>(null);
  const [runCount, setRunCount] = useState(0);
  const [running, setRunning] = useState(false);
  const [submitAttempt, setSubmitAttempt] = useState(0);
  const [result, setResult] = useState<SubmissionRunResult | null>(null);
  const [saved, setSaved] = useState(detail.saved);
  const timerRef = useRef<number | undefined>(undefined);

  const totalTestcases =
    detail.sampleTestcases.length + detail.hiddenTestcaseCount;
  const effectiveMode: WrapperMode =
    detail.submissionModel === "stdioOnly" ? "stdio" : mode;
  const key = `${language}:${effectiveMode}` as StarterCodeKey;
  const draftStorageKey = workspaceDraftKey(problemId, language, effectiveMode);
  const code = codeByKey[key] ?? detail.starterCode[key] ?? "";
  const limits = detail.limitsByLanguage[language];

  // A pending simulated run must not report into another screen after leaving.
  useEffect(() => () => window.clearTimeout(timerRef.current), []);

  // Draft lives in this browser only (USR0102 Q4, owner 2026-10-18): one entry per
  // (problem, language, submission model).
  useEffect(() => {
    try {
      const draft = window.localStorage.getItem(draftStorageKey);
      if (draft === null) return;
      // eslint-disable-next-line react-hooks/set-state-in-effect -- one-time restore after hydration
      setCodeByKey((prev) => ({ ...prev, [key]: draft }));
      setPersistedKeys((prev) => ({ ...prev, [key]: true }));
    } catch {
      // storage blocked: the draft just stays in memory
    }
  }, [draftStorageKey, key]);

  // Last language and model are remembered in this browser (USR0102 Q9, owner 2026-10-18) and used as
  // the default for the next problem. Restored after mount to keep hydration stable.
  useEffect(() => {
    try {
      const raw = window.localStorage.getItem(PREFS_STORAGE_KEY);
      if (!raw) return;
      const prefs = JSON.parse(raw) as { language?: string; mode?: string };
      const lang = LANGUAGES.find((value) => value === prefs.language);
      // eslint-disable-next-line react-hooks/set-state-in-effect -- one-time restore after hydration
      if (lang) setLanguage(lang);
      if (prefs.mode === "stdio" || prefs.mode === "function") setMode(prefs.mode);
    } catch {
      // storage blocked or corrupt: keep the defaults
    }
  }, []);

  function savePrefs(next: { language: SubmissionLanguage; mode: WrapperMode }) {
    try {
      window.localStorage.setItem(PREFS_STORAGE_KEY, JSON.stringify(next));
    } catch {
      // storage blocked or full: the choice just is not remembered
    }
  }

  function changeLanguage(value: SubmissionLanguage) {
    setLanguage(value);
    savePrefs({ language: value, mode });
    // An uploaded file only fits its own language's extension.
    if (
      uploaded &&
      !uploaded.name.toLowerCase().endsWith(FILE_EXTENSION[value])
    ) {
      setUploaded(null);
      setShowUploaded(false);
    }
  }

  function changeMode(value: WrapperMode) {
    setMode(value);
    savePrefs({ language, mode: value });
  }

  function setCode(value: string) {
    setCodeByKey((previous) => ({ ...previous, [key]: value }));
    // Keep editing even when storage is blocked or full, but never claim the draft was kept.
    const kept = saveWorkspaceDraft(problemId, language, effectiveMode, value);
    setPersistedKeys((prev) => {
      const next = { ...prev };
      if (kept) next[key] = true;
      else delete next[key];
      return next;
    });
  }

  function handleFile(file: File | undefined) {
    if (!file) return;
    // BD Sheet 9 NO 5, 6: extension of the selected language first, then 64 KB, before reading anything.
    const ext = FILE_EXTENSION[language];
    if (!file.name.toLowerCase().endsWith(ext)) {
      toast.error(t("toast.fileExtension", { ext }));
      return;
    }
    if (file.size > MAX_CODE_BYTES) {
      toast.error(t("toast.fileTooLarge"));
      return;
    }
    const reader = new FileReader();
    reader.onload = () => {
      setUploaded({
        name: file.name,
        size: file.size,
        content: String(reader.result ?? ""),
      });
      setShowUploaded(false);
    };
    reader.readAsText(file);
  }

  function onFileInput(event: ChangeEvent<HTMLInputElement>) {
    handleFile(event.target.files?.[0]);
    event.target.value = "";
  }

  function onDrop(event: DragEvent<HTMLLabelElement>) {
    event.preventDefault();
    handleFile(event.dataTransfer.files[0]);
  }

  // BD Sheet 9 NO 5-8 for Run Code and Submit: a source to send, not blank, not over 64 KB. The real
  // check is on the server.
  function sourceToSend(): string | null {
    const source = inputMethod === "type" ? code : uploaded?.content;
    if (source === undefined) {
      toast.warning(t("toast.noFile"));
      return null;
    }
    if (source.trim() === "") {
      toast.warning(t("toast.codeEmpty"));
      return null;
    }
    if (byteLength(source) > MAX_CODE_BYTES) {
      toast.error(t("toast.codeTooLarge"));
      return null;
    }
    return source;
  }

  function toggleSaved() {
    if (saved) {
      toast.success(t("toast.unsaved"));
    } else {
      toast.success(t("toast.saved"), {
        label: t("toast.openSaved"),
        onClick: () => router.push("/saved"),
      });
    }
    setSaved(!saved);
  }

  function runSample() {
    if (sourceToSend() === null) return;
    setRunning(true);
    setConsoleTab("testcase");
    window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => {
      const attempt = runCount + 1;
      setRunCount(attempt);
      const run = simulateSampleRun(detail.sampleTestcases, attempt);
      setSampleRun(run);
      setRunning(false);
      const passed = run.cases.filter((item) => item.passed).length;
      if (passed === run.cases.length)
        toast.success(t("toast.runDone", { count: run.cases.length }));
      else
        toast.warning(
          t("toast.runFailed", { passed, total: run.cases.length }),
        );
    }, 400);
  }

  function submit() {
    if (sourceToSend() === null) return;
    setRunning(true);
    setConsoleTab("result");
    setResult(null);
    window.clearTimeout(timerRef.current);
    timerRef.current = window.setTimeout(() => {
      const attempt = submitAttempt + 1;
      setSubmitAttempt(attempt);
      const run = simulateSubmission(totalTestcases, attempt);
      setResult(run);
      setRunning(false);
      if (run.overallVerdict === "accepted")
        toast.success(t("toast.submitAccepted"));
      else
        toast.error(
          t("toast.submitRejected", {
            passed: run.passedCount,
            total: run.totalCount,
          }),
        );
    }, 700);
  }

  const latestSubmission = detail.mySubmissions[0];
  const selectedCase = detail.sampleTestcases[caseIndex];
  const selectedRun = sampleRun?.cases[caseIndex];

  const statementPane = (
    <Card className="flex min-h-0 flex-1 flex-col px-4 py-3">
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
              <Badge
                variant={
                  detail.submissionModel === "both" ? "teal" : "neutral"
                }
              >
                {t(`submissionModel.${detail.submissionModel}`)}
              </Badge>
            </div>
            {/* Constraints and the follow-up question are sections of this Markdown (Q5), not fields. */}
            <MarkdownPreview className="mb-3">{detail.statementMd}</MarkdownPreview>
            <h3 className="mb-1.5 text-[12.5px] font-semibold">
              {t("panel.examplesTitle")}
            </h3>
            <ul className="mb-3 flex flex-col gap-2">
              {detail.examples.map((example, index) => (
                <li
                  key={index}
                  className="rounded-lg border border-[var(--color-border)] p-2.5 text-[12.5px]"
                >
                  <p>
                    <span className="font-semibold">{t("panel.input")}</span>{" "}
                    {example.input}
                  </p>
                  <p>
                    <span className="font-semibold">{t("panel.output")}</span>{" "}
                    {example.output}
                  </p>
                  {example.explanation ? (
                    <p className="text-[var(--color-text-muted)]">
                      {example.explanation}
                    </p>
                  ) : null}
                </li>
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
                <li
                  key={submission.id}
                  className="flex items-center justify-between gap-2 rounded-lg border border-[var(--color-border)] p-2.5 text-[12.5px]"
                >
                  <span>
                    <span className="font-semibold">
                      {t(`verdict.${submission.verdict}`)}
                      {submission.verdict === "accepted"
                        ? ""
                        : ` ${submission.passedCount}/${submission.totalCount}`}
                    </span>
                    <span className="block text-[11.5px] text-[var(--color-text-muted)]">
                      {submission.runtimeMs === null ||
                      submission.memoryKb === null
                        ? "-"
                        : t("panel.metrics", {
                            ms: submission.runtimeMs,
                            mb: Math.round(submission.memoryKb / 1024),
                          })}
                    </span>
                  </span>
                  <span className="text-right text-[var(--color-text-muted)]">
                    {t(`language.${submission.language}`)}
                    <span className="block text-[11.5px]">
                      {formatDateVN(submission.submittedAt, {
                        locale,
                        withTime: true,
                      })}
                    </span>
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
                  <p className="text-[11px] text-[var(--color-text-muted)]">
                    {t("panel.timeComplexity")}
                  </p>
                  <p className="font-mono text-[15px] font-semibold">
                    {detail.solutionReview.timeComplexity}
                  </p>
                </div>
                <div className="rounded-lg border border-[var(--color-border)] p-2.5 text-center">
                  <p className="text-[11px] text-[var(--color-text-muted)]">
                    {t("panel.spaceComplexity")}
                  </p>
                  <p className="font-mono text-[15px] font-semibold">
                    {detail.solutionReview.spaceComplexity}
                  </p>
                </div>
              </div>
              <ul className="list-disc pl-4 text-[12.5px] text-[var(--color-text-muted)]">
                {detail.solutionReview.notes.map((note) => (
                  <li key={note}>{note}</li>
                ))}
              </ul>
              {/* Both links need a submission to point at; without one the route would be broken. */}
              {latestSubmission ? (
                <div className="mt-3 flex flex-wrap gap-x-4 gap-y-1">
                  <Link
                    href={`/submissions/${latestSubmission.id}/review`}
                    className="text-[12.5px] font-semibold underline"
                  >
                    {t("panel.linkFullReport")}
                  </Link>
                  <Link
                    href={`/submissions/${latestSubmission.id}/interview`}
                    className="text-[12.5px] font-semibold underline"
                  >
                    {t("panel.linkMockInterview")}
                  </Link>
                </div>
              ) : null}
            </div>
          ) : (
            <EmptyState>{t("panel.noReview")}</EmptyState>
          )
        ) : null}
      </div>

      <div className="mt-2 flex shrink-0 items-center justify-between border-t border-[var(--color-border)] pt-2">
        <span className="text-[11.5px] text-[var(--color-text-muted)]">
          {t("panel.acRate")} {detail.acRate === null ? "-" : `${detail.acRate}%`}
        </span>
        <Button
          variant="ghost"
          size="sm"
          className="border border-[var(--color-border)]"
          onClick={toggleSaved}
        >
          {saved ? t("panel.btnUnsave") : t("panel.btnSave")}
        </Button>
      </div>
    </Card>
  );

  const editorPane = (
    <Card className="flex min-h-0 flex-1 flex-col px-3 py-2.5">
      <div className="mb-2 flex shrink-0 flex-wrap items-center gap-2">
        <SegmentedTabs
          label={t("toolbar.languageLabel")}
          value={language}
          onValueChange={changeLanguage}
          options={LANGUAGES.map((lang) => ({
            value: lang,
            label: t(`language.${lang}`),
          }))}
        />
        {detail.submissionModel === "both" ? (
          <SegmentedTabs
            label={t("toolbar.modeLabel")}
            value={mode}
            onValueChange={changeMode}
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
        <div className="flex min-h-0 flex-1 flex-col gap-2">
          <label
            onDragOver={(event) => event.preventDefault()}
            onDrop={onDrop}
            className="flex min-h-0 flex-1 cursor-pointer flex-col items-center justify-center gap-2 rounded-lg border border-dashed border-[var(--color-border)] px-3 text-center text-[12.5px] text-[var(--color-text-muted)]"
          >
            <Upload className="h-5 w-5" aria-hidden="true" />
            {t("toolbar.uploadHint", { ext: FILE_EXTENSION[language] })}
            <input
              type="file"
              accept={FILE_EXTENSION[language]}
              className="sr-only"
              onChange={onFileInput}
            />
          </label>
          {uploaded ? (
            <div className="shrink-0 rounded-lg border border-[var(--color-border)] p-2 text-[12px]">
              <div className="flex items-center justify-between gap-2">
                <span className="min-w-0 truncate font-mono">
                  {uploaded.name} · {(uploaded.size / 1024).toFixed(1)} KB
                </span>
                <Button
                  variant="ghost"
                  size="sm"
                  className="shrink-0 border border-[var(--color-border)]"
                  aria-expanded={showUploaded}
                  onClick={() => setShowUploaded((open) => !open)}
                >
                  {t("toolbar.btnViewFile")}
                </Button>
              </div>
              {showUploaded ? (
                <pre className="mt-2 max-h-32 overflow-auto rounded-md bg-[var(--color-surface-hover)] p-2 font-mono text-[11.5px]">
                  {uploaded.content}
                </pre>
              ) : null}
            </div>
          ) : null}
        </div>
      )}

      <div className="mt-1.5 flex shrink-0 flex-wrap items-center justify-between gap-x-3 text-[11px] text-[var(--color-text-muted)]">
        <span>
          {inputMethod === "type" && persistedKeys[key]
            ? t("toolbar.draftSaved")
            : ""}
        </span>
        <span>
          {t("toolbar.limits", {
            time: limits.timeLimitMs / 1000,
            memory: limits.memoryLimitMb,
          })}
        </span>
      </div>
    </Card>
  );

  const consolePane = (
    <Card className="flex min-h-0 flex-1 flex-col px-3 py-2.5">
      <div className="mb-2 flex shrink-0 flex-wrap items-center justify-between gap-2">
        <SegmentedTabs
          label={t("console.tabsLabel")}
          value={consoleTab}
          onValueChange={setConsoleTab}
          options={[
            { value: "testcase", label: t("console.tabTestcase") },
            { value: "result", label: t("console.tabResult") },
          ]}
        />
        {consoleTab === "result" && result ? (
          <Badge
            variant={
              result.overallVerdict === "accepted" ? "success" : "negative"
            }
          >
            {t(`verdict.${result.overallVerdict}`)} · {result.passedCount}/
            {result.totalCount}
          </Badge>
        ) : null}
        {consoleTab === "testcase" && sampleRun ? (
          <Badge
            variant={
              sampleRun.cases.every((item) => item.passed)
                ? "success"
                : "negative"
            }
          >
            {t("console.sampleSummary", {
              passed: sampleRun.cases.filter((item) => item.passed).length,
              total: sampleRun.cases.length,
            })}
          </Badge>
        ) : null}
      </div>

      <div className="min-h-0 flex-1 overflow-y-auto">
        {consoleTab === "testcase" ? (
          <div className="flex flex-col gap-2">
            <SegmentedTabs
              label={t("console.caseTabsLabel")}
              value={String(caseIndex)}
              onValueChange={(value) => setCaseIndex(Number(value))}
              options={detail.sampleTestcases.map((_, index) => ({
                value: String(index),
                label: sampleRun
                  ? t("console.caseTabWithVerdict", {
                      n: index + 1,
                      verdict: sampleRun.cases[index]?.passed
                        ? t("console.passed")
                        : t("console.failed"),
                    })
                  : t("console.caseTab", { n: index + 1 }),
              }))}
              className="self-start"
            />
            {selectedCase ? (
              <div className="flex flex-col gap-2 text-[12px]">
                <CaseField label={t("console.fieldInput")}>
                  {selectedCase.input}
                </CaseField>
                {selectedRun ? (
                  <>
                    <CaseField
                      label={t("console.fieldActual")}
                      failed={!selectedRun.passed}
                    >
                      {selectedRun.actualOutput}
                    </CaseField>
                    <CaseField label={t("console.fieldExpected")}>
                      {selectedRun.expectedOutput}
                    </CaseField>
                    <p className="text-[11.5px] text-[var(--color-text-muted)]">
                      {t("console.metrics", {
                        ms: selectedRun.runtimeMs,
                        mb: Math.round(selectedRun.memoryKb / 1024),
                      })}
                    </p>
                  </>
                ) : running ? (
                  <p className="text-[12.5px] text-[var(--color-text-muted)]">
                    {t("console.running")}
                  </p>
                ) : null}
              </div>
            ) : null}
            {!sampleRun && !running ? (
              <p className="text-[12.5px] text-[var(--color-text-muted)]">
                {t("console.runHint", {
                  samples: detail.sampleTestcases.length,
                  hidden: detail.hiddenTestcaseCount,
                })}
              </p>
            ) : null}
          </div>
        ) : result ? (
          <div>
            <div className="flex flex-wrap gap-1">
              {result.testcases.map((testcase) => {
                const label = t("console.caseDot", {
                  n: testcase.order,
                  verdict:
                    testcase.verdict === "passed"
                      ? t("console.passed")
                      : t("console.failed"),
                });
                return (
                  <span
                    key={testcase.order}
                    role="img"
                    aria-label={label}
                    title={label}
                    className="inline-block h-4 w-4 rounded-sm"
                    style={{
                      background: `var(${testcase.verdict === "passed" ? "--color-success" : "--color-admin-negative"})`,
                    }}
                  />
                );
              })}
            </div>
            <p className="mt-2 text-[11.5px] text-[var(--color-text-muted)]">
              {t("console.metrics", {
                ms: result.runtimeMs,
                mb: Math.round(result.memoryKb / 1024),
              })}
            </p>
          </div>
        ) : running ? (
          <p className="text-[12.5px] text-[var(--color-text-muted)]">
            {t("console.judging")}
          </p>
        ) : (
          <p className="text-[12.5px] text-[var(--color-text-muted)]">
            {t("console.submitHint")}
          </p>
        )}
      </div>

      {consoleTab === "result" && result?.overallVerdict === "accepted" ? (
        <div className="mt-2 flex shrink-0 flex-wrap gap-2 border-t border-[var(--color-border)] pt-2">
          <Link
            href={`/submissions/${result.submissionId}`}
            className="text-[12px] font-semibold underline"
          >
            {t("console.linkResult")}
          </Link>
          <Link
            href={`/submissions/${result.submissionId}/review`}
            className="text-[12px] font-semibold underline"
          >
            {t("console.linkReview")}
          </Link>
          <Link
            href={`/submissions/${result.submissionId}/interview`}
            className="text-[12px] font-semibold underline"
          >
            {t("console.linkInterview")}
          </Link>
        </div>
      ) : null}
    </Card>
  );

  return (
    // `data-fullscreen-view` makes the student shell hand this view the whole viewport (globals.css);
    // below `lg` it is an ordinary stacked page.
    <div
      data-fullscreen-view
      className="flex min-h-[80dvh] flex-col lg:min-h-0 lg:flex-1"
    >
      <div className="mb-3 flex shrink-0 flex-wrap items-center gap-x-3 gap-y-2">
        <Link
          href="/problems"
          className="text-[12.5px] font-semibold underline"
        >
          {t("taskbar.linkList")}
        </Link>
        <span className="font-mono text-xs text-[var(--color-text-muted)]">
          {detail.code}
        </span>
        <h1 className="min-w-0 truncate text-[13.5px] font-semibold">
          {detail.title}
        </h1>
        <Badge variant={problemLevelTone(levels, detail.difficulty)}>
          {problemLevelLabel(levels, detail.difficulty)}
        </Badge>
        <div className="ml-auto flex gap-2">
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            onClick={runSample}
            disabled={running}
          >
            <Play className="mr-1.5 h-3.5 w-3.5" />
            {t("taskbar.btnRun")}
          </Button>
          <Button variant="cta" size="sm" onClick={submit} disabled={running}>
            <Send className="mr-1.5 h-3.5 w-3.5" />
            {t("taskbar.btnSubmit")}
          </Button>
        </div>
      </div>

      <SplitPane
        direction="horizontal"
        label={t("workspace.resizeColumns")}
        defaultRatio={0.47}
        min={0.22}
        max={0.74}
        storageKey="algoprep-workspace-split-x"
        className="flex-1"
        first={statementPane}
        second={
          <SplitPane
            direction="vertical"
            label={t("workspace.resizeRows")}
            defaultRatio={0.62}
            min={0.25}
            max={0.85}
            storageKey="algoprep-workspace-split-y"
            className="min-h-[70dvh] flex-1 lg:min-h-0"
            first={editorPane}
            second={consolePane}
          />
        }
      />
    </div>
  );
}

// One labelled block of testcase text; `failed` outlines a wrong actual output with the negative token.
function CaseField({
  label,
  failed = false,
  children,
}: {
  label: string;
  failed?: boolean;
  children: string;
}) {
  return (
    <div>
      <p className="mb-0.5 text-[11px] font-semibold text-[var(--color-text-muted)]">
        {label}
      </p>
      <pre
        className={`overflow-x-auto rounded-md border bg-[var(--color-surface-hover)] px-2 py-1.5 font-mono text-[12px] ${
          failed
            ? "border-[var(--color-admin-negative)]"
            : "border-[var(--color-border)]"
        }`}
      >
        {children}
      </pre>
    </div>
  );
}
