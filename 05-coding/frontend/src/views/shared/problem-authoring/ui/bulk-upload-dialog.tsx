// "Tải lên hàng loạt" (SHR0202 EVT-15, F2 #45 and #46). The author picks many files; `<name>.in` and
// `<name>.out` with the same base name make one testcase (owner decision 2026-11-07). Everything that
// cannot be paired is listed before anything is sent, and the upload stays disabled until the
// selection is clean. The dialog only reports problems; the calls live in the view's `onUpload`.
"use client";

import { useState } from "react";
import type { TestcaseVisibility } from "@/entities/problem";
import { ApiError } from "@/shared/api";
import { useApiErrorMessage, useT } from "@/shared/i18n";
import { formatBytes } from "@/shared/lib";
import { toast } from "@/shared/lib/toast-store";
import {
  Button,
  DataTable,
  FilePickerButton,
  Modal,
  SelectField,
} from "@/shared/ui";
import {
  BULK_LIMITS,
  pairTestcaseFiles,
  type BulkIssue,
  type BulkPairRow,
} from "../model/bulk-pairs";

type Props = {
  /** Testcases the problem already has: the 500 cap counts them. */
  existingCount: number;
  onClose: () => void;
  onUpload: (
    pairs: BulkPairRow<File>[],
    visibility: TestcaseVisibility,
  ) => Promise<void>;
};

// Most faults a server answer toasts one by one; the rest would only push the first ones off screen.
const MAX_ITEM_TOASTS = 3;

export function BulkUploadDialog({ existingCount, onClose, onUpload }: Props) {
  const t = useT("problemAuthoring");
  const apiMessage = useApiErrorMessage();
  const [files, setFiles] = useState<File[]>([]);
  const [visibility, setVisibility] = useState<TestcaseVisibility>("hidden");
  const [busy, setBusy] = useState(false);
  const { pairs, issues } = pairTestcaseFiles(files, existingCount);
  const canUpload = pairs.length > 0 && issues.length === 0 && !busy;

  function describe(issue: BulkIssue): string {
    switch (issue.code) {
      case "tooManyPairs":
      case "tooManyTestcases":
        return t(`bulk.issue.${issue.code}`, {
          count: issue.count,
          limit: issue.limit,
        });
      case "sideTooLarge":
        return t("bulk.issue.sideTooLarge", {
          file: issue.file,
          limit: formatBytes(BULK_LIMITS.maxSideBytes),
        });
      case "totalTooLarge":
        return t("bulk.issue.totalTooLarge", {
          limit: formatBytes(issue.limit),
        });
      default:
        return t(`bulk.issue.${issue.code}`, { file: issue.file });
    }
  }

  async function submit() {
    setBusy(true);
    try {
      await onUpload(pairs, visibility);
      onClose();
    } catch (error) {
      const items =
        error instanceof ApiError && error.code === "PRB-045"
          ? error.details?.items
          : undefined;
      if (Array.isArray(items) && items.length > 0) {
        const nameOf = new Map(pairs.map((pair) => [pair.key, pair.base]));
        for (const item of items.slice(0, MAX_ITEM_TOASTS)) {
          const row = item as {
            clientKey?: string;
            side?: string;
            problem?: string;
          };
          const base = nameOf.get(row.clientKey ?? "") ?? "?";
          toast.error(
            t("bulk.itemError", {
              file: `${base}.${row.side === "expectedOutput" ? "out" : "in"}`,
              reason: t(
                `bulk.fault.${row.problem === "MISSING" || row.problem === "SIZE_MISMATCH" ? row.problem : "INVALID_TEXT"}`,
              ),
            }),
          );
        }
      } else {
        toast.error(apiMessage(error));
      }
    } finally {
      setBusy(false);
    }
  }

  return (
    <Modal
      open
      onClose={busy ? () => undefined : onClose}
      dismissible={!busy}
      title={t("bulk.title")}
      className="max-w-2xl"
      footer={
        <>
          <Button variant="ghost" size="sm" disabled={busy} onClick={onClose}>
            {t("dialog.cancel")}
          </Button>
          <FilePickerButton
            variant="ghost"
            size="sm"
            className="border border-[var(--color-border)]"
            disabled={busy}
            accept=".in,.out"
            multiple
            inputTestId="bulk-input"
            onFiles={setFiles}
          >
            {files.length > 0 ? t("bulk.chooseAgain") : t("bulk.choose")}
          </FilePickerButton>
          <Button size="sm" disabled={!canUpload} onClick={submit}>
            {busy
              ? t("bulk.uploading")
              : t("bulk.upload", { count: pairs.length })}
          </Button>
        </>
      }
    >
      <p>{t("bulk.hint", { max: BULK_LIMITS.maxPairs })}</p>
      <div className="mt-3 max-w-xs">
        <SelectField
          label={t("columnVisibility")}
          value={visibility}
          onChange={(event) =>
            setVisibility(event.target.value as TestcaseVisibility)
          }
          options={[
            { value: "public", label: t("visibility.public") },
            { value: "hidden", label: t("visibility.hidden") },
          ]}
        />
      </div>
      {files.length > 0 ? (
        <div className="mt-3 flex flex-col gap-3">
          {issues.length > 0 ? (
            <ul
              className="list-disc pl-5 text-[var(--color-danger)]"
              aria-label={t("bulk.issuesLabel")}
            >
              {issues.map((issue, index) => (
                <li key={index}>{describe(issue)}</li>
              ))}
            </ul>
          ) : null}
          <DataTable
            caption={t("bulk.tableCaption")}
            rows={pairs}
            rowKey={(row) => row.key}
            emptyMessage={t("bulk.noPairs")}
            minWidth={480}
            columns={[
              {
                key: "base",
                header: t("bulk.columnName"),
                render: (row) => row.base,
              },
              {
                key: "input",
                header: t("columnInput"),
                align: "right",
                render: (row) => formatBytes(row.input.size),
              },
              {
                key: "expected",
                header: t("columnExpected"),
                align: "right",
                render: (row) => formatBytes(row.expected.size),
              },
            ]}
          />
        </div>
      ) : null}
    </Modal>
  );
}
