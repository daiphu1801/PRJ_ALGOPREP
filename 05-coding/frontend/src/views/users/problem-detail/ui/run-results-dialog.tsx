// PROTOTYPE — chưa có DD. Xem 06-plan/PROTOTYPE_DEBT.md mục 9.
//
// Màn phụ (hộp thoại) hiện kết quả từng testcase khi bấm Run Code hoặc Submit (owner 2026-10-21): bảng
// kết quả nhỏ ở góc Workspace khó đọc. Run Code: danh sách testcase mẫu bên trái, chi tiết dữ liệu vào /
// thực tế / mong đợi bên phải. Submit: mỗi testcase một dòng Đạt / Không đạt; testcase ẩn không lộ dữ liệu.
"use client";

import { useState } from "react";
import Link from "next/link";
import type {
  SampleRunResult,
  SubmissionRunResult,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { Badge, Button, Modal } from "@/shared/ui";

type Props = {
  mode: "run" | "submit";
  running: boolean;
  sampleRun: SampleRunResult | null;
  result: SubmissionRunResult | null;
  onClose: () => void;
};

export function RunResultsDialog({
  mode,
  running,
  sampleRun,
  result,
  onClose,
}: Props) {
  const t = useT("problemDetail");
  const [picked, setPicked] = useState<number | null>(null);

  const cases = sampleRun?.cases ?? [];
  const firstFailed = cases.findIndex((item) => !item.passed);
  const selected = cases[picked ?? (firstFailed >= 0 ? firstFailed : 0)];
  const selectedNo = (picked ?? (firstFailed >= 0 ? firstFailed : 0)) + 1;

  const verdictText = (passed: boolean) =>
    passed ? t("console.passed") : t("console.failed");

  return (
    <Modal
      open
      onClose={onClose}
      title={t(mode === "run" ? "console.dialog.titleRun" : "console.dialog.titleSubmit")}
      className="max-w-3xl"
      footer={
        <Button variant="ghost" size="sm" className="border border-[var(--color-border)]" onClick={onClose}>
          {t("console.dialog.close")}
        </Button>
      }
    >
      {mode === "run" ? (
        !sampleRun ? (
          <p>{running ? t("console.running") : null}</p>
        ) : (
          <div className="flex flex-col gap-3">
            <Badge
              variant={cases.every((item) => item.passed) ? "success" : "negative"}
              className="self-start"
            >
              {t("console.sampleSummary", {
                passed: cases.filter((item) => item.passed).length,
                total: cases.length,
              })}
            </Badge>
            <div className="grid gap-3 md:grid-cols-[200px_minmax(0,1fr)]">
              <ul aria-label={t("console.dialog.listLabel")} className="flex max-h-72 flex-col gap-1 overflow-y-auto">
                {cases.map((item, index) => {
                  const active = index + 1 === selectedNo;
                  return (
                    <li key={index}>
                      <button
                        type="button"
                        aria-current={active}
                        onClick={() => setPicked(index)}
                        className={`flex w-full items-center justify-between gap-2 rounded-lg border px-3 py-2 text-left text-[13px] ${
                          active
                            ? "border-[var(--color-primary)] bg-[var(--color-surface-hover)]"
                            : "border-[var(--color-border)] hover:bg-[var(--color-row-hover)]"
                        }`}
                      >
                        <span className="font-semibold text-[var(--color-text)]">
                          {t("console.dialog.caseRow", { n: index + 1 })}
                        </span>
                        <Badge variant={item.passed ? "success" : "negative"}>
                          {verdictText(item.passed)}
                        </Badge>
                      </button>
                    </li>
                  );
                })}
              </ul>
              {selected ? (
                <div className="flex min-w-0 flex-col gap-2 text-[12px]">
                  <CaseField label={t("console.fieldInput")}>{selected.input}</CaseField>
                  <CaseField label={t("console.fieldActual")} failed={!selected.passed}>
                    {selected.actualOutput}
                  </CaseField>
                  <CaseField label={t("console.fieldExpected")}>{selected.expectedOutput}</CaseField>
                  <p className="text-[11.5px]">
                    {t("console.metrics", {
                      ms: selected.runtimeMs,
                      mb: Math.round(selected.memoryKb / 1024),
                    })}
                  </p>
                </div>
              ) : null}
            </div>
          </div>
        )
      ) : !result ? (
        <p>{running ? t("console.judging") : null}</p>
      ) : (
        <div className="flex flex-col gap-3">
          <div className="flex flex-wrap items-center gap-2">
            <Badge variant={result.overallVerdict === "accepted" ? "success" : "negative"}>
              {t(`verdict.${result.overallVerdict}`)} · {result.passedCount}/{result.totalCount}
            </Badge>
            <span className="text-[12px]">
              {t("console.metrics", {
                ms: result.runtimeMs,
                mb: Math.round(result.memoryKb / 1024),
              })}
            </span>
          </div>
          <ul className="grid max-h-72 grid-cols-1 gap-1.5 overflow-y-auto sm:grid-cols-2">
            {result.testcases.map((testcase) => {
              const passed = testcase.verdict === "passed";
              return (
                <li
                  key={testcase.order}
                  className="flex items-center justify-between gap-2 rounded-lg border border-[var(--color-border)] px-3 py-2 text-[13px]"
                >
                  <span className="font-semibold text-[var(--color-text)]">
                    {t("console.dialog.testcaseRow", { n: testcase.order })}
                  </span>
                  <Badge variant={passed ? "success" : "negative"}>{verdictText(passed)}</Badge>
                </li>
              );
            })}
          </ul>
          <p className="text-[11.5px]">{t("console.dialog.hiddenNote")}</p>
          {result.overallVerdict === "accepted" ? (
            <div className="flex flex-wrap gap-3">
              <Link href={`/submissions/${result.submissionId}`} className="text-[12.5px] font-semibold underline">
                {t("console.linkResult")}
              </Link>
              <Link href={`/submissions/${result.submissionId}/review`} className="text-[12.5px] font-semibold underline">
                {t("console.linkReview")}
              </Link>
              <Link href={`/submissions/${result.submissionId}/interview`} className="text-[12.5px] font-semibold underline">
                {t("console.linkInterview")}
              </Link>
            </div>
          ) : null}
        </div>
      )}
    </Modal>
  );
}

// One labelled block of testcase text; `failed` outlines a wrong actual output with the negative token.
export function CaseField({
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
