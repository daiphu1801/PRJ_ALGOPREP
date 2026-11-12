// "Nhập CSV" (SHR0301, owner decision 2026-10-08): pick a .csv, validate it row by row against the
// admin-managed topic and level lists, send the good rows to F6 #24 and list the bad ones with a
// reason. Nothing is created from a file that fails as a whole (missing column, empty, too many rows).
// The client check only saves a round trip: the server re-checks every row and decides `importedCount`
// and `skippedRows` (DD SHR0301 BR-SC-02).
"use client";

import { useState } from "react";
import {
  CSV_TEMPLATE,
  parseQuestionsCsv,
  useInterviewLevels,
  useInterviewTopics,
  type CsvImportParse,
} from "@/entities/interview-question";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, FilePickerButton, Modal } from "@/shared/ui";
import { toImportRows, useImportQuestions } from "../api";

type Props = {
  open: boolean;
  onClose: () => void;
  /** Question texts already in the bank, so a repeated row is skipped instead of duplicated. */
  existing: readonly string[];
};

type SkippedRow = { line: number; code: string; detail?: string | null };
type Outcome = {
  total: number;
  imported: number;
  /** Rows rejected by the client check, then by the server, in file order. */
  skipped: SkippedRow[];
};

const noRows = (parse: CsvImportParse): Outcome => ({
  total: parse.total,
  imported: 0,
  skipped: parse.errors,
});

// The BOM makes Excel open the template as UTF-8, so Vietnamese letters survive.
function downloadTemplate() {
  const url = URL.createObjectURL(
    new Blob(["\uFEFF" + CSV_TEMPLATE], { type: "text/csv;charset=utf-8" }),
  );
  const link = document.createElement("a");
  link.href = url;
  link.download = "interview-questions-template.csv";
  link.click();
  URL.revokeObjectURL(url);
}

export function CsvImportDialog({ open, onClose, existing }: Props) {
  const t = useT("interviewQuestionManagement");
  const apiMessage = useApiErrorMessage();
  const topics = useInterviewTopics();
  const levels = useInterviewLevels();
  const importQuestions = useImportQuestions();
  const [outcome, setOutcome] = useState<Outcome | null>(null);

  async function handleFile(file: File) {
    const text = await file.text();
    const parse = parseQuestionsCsv(text, {
      topics,
      levels,
      existing,
    });
    // A file that cannot be read at all is an error toast (DEC-2026-1003-toast-feedback-channel);
    // per-row problems are a report inside the dialog.
    if (parse.fileError) {
      setOutcome(null);
      toast.error(
        t(`csvImport.fileError.${parse.fileError.code}`, {
          detail: parse.fileError.detail ?? "",
        }),
      );
      return;
    }
    if (parse.drafts.length === 0) {
      setOutcome(noRows(parse));
      return;
    }
    try {
      const result = await importQuestions.mutateAsync(
        toImportRows(text, parse),
      );
      setOutcome({
        total: parse.total,
        imported: result.importedCount,
        skipped: [
          ...parse.errors,
          ...result.skippedRows.map((row) => ({
            line: row.line,
            code: row.reasonCode,
            detail: row.detail,
          })),
        ].sort((left, right) => left.line - right.line),
      });
      if (result.importedCount > 0)
        toast.success(t("csvImport.toast", { count: result.importedCount }));
    } catch (error) {
      setOutcome(null);
      toast.error(apiMessage(error));
    }
  }

  function close() {
    setOutcome(null);
    onClose();
  }

  return (
    <Modal
      open={open}
      onClose={close}
      title={t("csvImport.title")}
      className="max-w-xl"
      footer={
        <>
          <Button variant="ghost" size="sm" onClick={downloadTemplate}>
            {t("csvImport.downloadTemplate")}
          </Button>
          <Button
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            onClick={close}
          >
            {t("csvImport.close")}
          </Button>
          <FilePickerButton
            variant="cta"
            size="sm"
            disabled={importQuestions.isPending}
            accept=".csv,text/csv"
            inputTestId="csv-input"
            onFiles={([file]) => void handleFile(file!)}
          >
            {outcome ? t("csvImport.chooseAnother") : t("csvImport.choose")}
          </FilePickerButton>
        </>
      }
    >
      <p>{t("csvImport.hint")}</p>
      <ul className="mt-2 list-disc pl-5">
        <li>{t("csvImport.columns")}</li>
        <li>{t("csvImport.listFormat")}</li>
        <li>{t("csvImport.rubricFormat")}</li>
      </ul>

      {importQuestions.isPending ? (
        <p role="status" className="mt-3">
          {t("csvImport.importing")}
        </p>
      ) : null}
      {outcome ? (
        <div className="mt-3" role="status">
          <p className="font-semibold text-[var(--color-text)]">
            {t("csvImport.summary", {
              imported: outcome.imported,
              total: outcome.total,
            })}
          </p>
          {outcome.skipped.length > 0 ? (
            <ul
              className="mt-2 max-h-48 list-disc overflow-y-auto pl-5"
              aria-label={t("csvImport.errorsLabel")}
            >
              {outcome.skipped.map((row) => (
                <li key={`${row.line}-${row.code}`}>
                  {t("csvImport.rowError", {
                    line: row.line,
                    reason: t(`csvImport.reason.${row.code}`, {
                      detail: row.detail ?? "",
                    }),
                  })}
                </li>
              ))}
            </ul>
          ) : null}
        </div>
      ) : null}
    </Modal>
  );
}
