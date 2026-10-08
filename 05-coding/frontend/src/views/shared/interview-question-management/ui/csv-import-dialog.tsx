// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// "Nhập CSV" (SHR0301, owner decision 2026-10-08): pick a .csv, validate it row by row against the
// admin-managed topic and level lists, import the good rows, and list the bad ones with a reason.
// Nothing is created from a file that fails as a whole (missing column, empty, too many rows).
"use client";

import { useRef, useState } from "react";
import {
  CSV_TEMPLATE,
  parseQuestionsCsv,
  useInterviewLevels,
  useInterviewTopics,
  type CsvImportParse,
} from "@/entities/interview-question";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import { Button, Modal } from "@/shared/ui";
import { useImportQuestions } from "../api";

type Props = {
  open: boolean;
  onClose: () => void;
  /** Question texts already in the bank, so a repeated row is skipped instead of duplicated. */
  existing: readonly string[];
};

type Outcome = { parse: CsvImportParse; imported: number };

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
  const topics = useInterviewTopics();
  const levels = useInterviewLevels();
  const importQuestions = useImportQuestions();
  const input = useRef<HTMLInputElement>(null);
  const [outcome, setOutcome] = useState<Outcome | null>(null);

  async function handleFile(file: File) {
    const parse = parseQuestionsCsv(await file.text(), {
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
      setOutcome({ parse, imported: 0 });
      return;
    }
    try {
      const created = await importQuestions.mutateAsync(parse.drafts);
      setOutcome({ parse, imported: created.length });
      toast.success(t("csvImport.toast", { count: created.length }));
    } catch {
      setOutcome(null);
      toast.error(t("csvImport.failed"));
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
          <Button
            variant="cta"
            size="sm"
            disabled={importQuestions.isPending}
            onClick={() => input.current?.click()}
          >
            {outcome ? t("csvImport.chooseAnother") : t("csvImport.choose")}
          </Button>
        </>
      }
    >
      <input
        ref={input}
        type="file"
        accept=".csv,text/csv"
        hidden
        data-testid="csv-input"
        onChange={(event) => {
          const file = event.target.files?.[0];
          if (file) void handleFile(file);
          event.target.value = "";
        }}
      />

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
              total: outcome.parse.total,
            })}
          </p>
          {outcome.parse.errors.length > 0 ? (
            <ul
              className="mt-2 max-h-48 list-disc overflow-y-auto pl-5"
              aria-label={t("csvImport.errorsLabel")}
            >
              {outcome.parse.errors.map((error) => (
                <li key={`${error.line}-${error.code}`}>
                  {t("csvImport.rowError", {
                    line: error.line,
                    reason: t(`csvImport.reason.${error.code}`, {
                      detail: error.detail ?? "",
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
