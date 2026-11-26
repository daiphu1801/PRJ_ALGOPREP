// Side panel of SHR0204: everything of one imported row, in three parts. The parent owns the row; this only
// reports changes. Mounted with the row id as key, so switching rows starts on the statement again.
"use client";

import { useState } from "react";
import { Plus, Trash2, X } from "lucide-react";
import {
  IMPORT_MAX_CASES,
  IMPORT_MIN_CASES,
  IMPORT_SAMPLES,
  resolveLanguage,
  type ImportCase,
  type ImportCatalogue,
  type ImportCheck,
  type ImportRow,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import {
  Badge,
  Button,
  Card,
  FilterMenu,
  IconAction,
  SegmentedTabs,
  TextArea,
  TextField,
} from "@/shared/ui";

type Part = "statement" | "cases" | "solution";

type Props = {
  row: ImportRow;
  number: number;
  check: ImportCheck;
  catalogue: ImportCatalogue;
  locked: boolean;
  onChange: (patch: Partial<ImportRow>) => void;
  onClose: () => void;
};

export function ImportRowEditor({
  row,
  number,
  check,
  catalogue,
  locked,
  onChange,
  onClose,
}: Props) {
  const t = useT("problemImport");
  const has = (...codes: string[]) =>
    check.issues.some((issue) => codes.includes(issue.code));
  const [part, setPart] = useState<Part>(
    has("caseHalf", "caseTooLarge") &&
      !has("titleMissing", "statementMissing", "topicUnknown")
      ? "cases"
      : "statement",
  );

  const changeCase = (index: number, patch: Partial<ImportCase>) =>
    onChange({
      cases: row.cases.map((entry, at) =>
        at === index ? { ...entry, ...patch } : entry,
      ),
    });

  const language = resolveLanguage(row.language, catalogue.languages);
  const mono = "font-mono";

  return (
    <Card className="min-w-0 self-start p-4">
      <div className="mb-3 flex items-center justify-between gap-2">
        <h2 className="text-sm font-semibold">
          {t("editor.title", { number })}
        </h2>
        <IconAction icon={X} label={t("editor.close")} onClick={onClose} />
      </div>
      <SegmentedTabs
        className="mb-3"
        label={t("editor.parts")}
        value={part}
        onValueChange={setPart}
        options={[
          { value: "statement", label: t("editor.statement") },
          {
            value: "cases",
            label: t("editor.cases", {
              count: check.caseCount,
              needed: IMPORT_MIN_CASES,
            }),
          },
          { value: "solution", label: t("editor.solution") },
        ]}
      />

      {part === "statement" ? (
        <div className="grid gap-3">
          <TextField
            label={t("field.title")}
            value={row.title}
            disabled={locked}
            invalid={has("titleMissing", "titleTooLong", "titleRepeated")}
            onChange={(event) => onChange({ title: event.target.value })}
          />
          <TextField
            label={t("field.topics")}
            list="problem-import-topics"
            value={row.topics}
            disabled={locked}
            invalid={has("topicUnknown")}
            onChange={(event) => onChange({ topics: event.target.value })}
          />
          <datalist id="problem-import-topics">
            {catalogue.topics.map((topic) => (
              <option key={topic.key} value={topic.label} />
            ))}
          </datalist>
          <TextArea
            label={t("field.statement")}
            rows={6}
            value={row.statement}
            disabled={locked}
            invalid={has("statementMissing", "statementTooLong")}
            onChange={(event) => onChange({ statement: event.target.value })}
          />
          <TextArea
            label={t("field.stdin")}
            rows={3}
            value={row.stdin}
            disabled={locked}
            onChange={(event) => onChange({ stdin: event.target.value })}
          />
          <TextArea
            label={t("field.stdout")}
            rows={3}
            value={row.stdout}
            disabled={locked}
            onChange={(event) => onChange({ stdout: event.target.value })}
          />
          <TextArea
            label={t("field.constraints")}
            rows={3}
            value={row.constraints}
            disabled={locked}
            invalid={has("constraintsTooLong")}
            onChange={(event) => onChange({ constraints: event.target.value })}
          />
        </div>
      ) : null}

      {part === "cases" ? (
        <div className="grid gap-3">
          {row.cases.map((entry, index) => (
            <fieldset
              key={index}
              className="min-w-0 rounded-lg border border-[var(--color-border)] p-3"
            >
              <legend className="px-1 text-[13px] font-semibold">
                {t("editor.case", { number: index + 1 })}{" "}
                <Badge>
                  {index < IMPORT_SAMPLES
                    ? t("editor.public")
                    : t("editor.hidden")}
                </Badge>
              </legend>
              <div className="grid gap-2 sm:grid-cols-2">
                <TextArea
                  label={t("field.caseInput", { number: index + 1 })}
                  rows={3}
                  className={mono}
                  value={entry.input}
                  disabled={locked}
                  invalid={!entry.input && entry.output !== ""}
                  onChange={(event) =>
                    changeCase(index, { input: event.target.value })
                  }
                />
                <TextArea
                  label={t("field.caseOutput", { number: index + 1 })}
                  rows={3}
                  className={mono}
                  value={entry.output}
                  disabled={locked}
                  invalid={!entry.output && entry.input !== ""}
                  onChange={(event) =>
                    changeCase(index, { output: event.target.value })
                  }
                />
              </div>
              {index < IMPORT_SAMPLES ? (
                <TextField
                  wrapperClassName="mt-2"
                  label={t("field.caseNote", { number: index + 1 })}
                  value={entry.note}
                  disabled={locked}
                  onChange={(event) =>
                    changeCase(index, { note: event.target.value })
                  }
                />
              ) : null}
              <div className="mt-2 flex justify-end">
                <Button
                  variant="ghost"
                  size="sm"
                  disabled={locked}
                  className="gap-1.5 border border-[var(--color-border)]"
                  aria-label={t("editor.removeCase", { number: index + 1 })}
                  onClick={() =>
                    onChange({
                      cases: row.cases.filter((_, at) => at !== index),
                    })
                  }
                >
                  <Trash2 aria-hidden="true" className="h-4 w-4" />
                  {t("remove")}
                </Button>
              </div>
            </fieldset>
          ))}
          <Button
            variant="ghost"
            size="sm"
            disabled={locked || row.cases.length >= IMPORT_MAX_CASES}
            className="gap-1.5 justify-self-start border border-[var(--color-border)]"
            onClick={() =>
              onChange({
                cases: [...row.cases, { input: "", output: "", note: "" }],
              })
            }
          >
            <Plus aria-hidden="true" className="h-4 w-4" />
            {t("editor.addCase")}
          </Button>
        </div>
      ) : null}

      {part === "solution" ? (
        <div className="grid gap-3">
          <div className="grid gap-1">
            <span className="text-xs font-medium text-[var(--color-text-muted)]">
              {t("field.language")}
            </span>
            <FilterMenu
              label={t("field.language")}
              compact
              className="w-full [&>button]:h-10 [&>button]:w-full [&>button]:justify-between [&>button]:bg-[var(--color-surface)] [&>button]:px-3 [&>button]:text-sm [&>button]:text-[var(--color-text)] [&>[role=listbox]]:w-full [&>[role=listbox]]:max-w-full"
              value={language ?? row.language}
              disabled={locked}
              onValueChange={(value) => onChange({ language: value })}
              options={[
                // Shown only until a language is picked, so it can never be chosen again.
                ...(language
                  ? []
                  : [
                      {
                        value: row.language,
                        label: row.language || t("choose"),
                      },
                    ]),
                ...catalogue.languages.map((name) => ({
                  value: name,
                  label: name,
                })),
              ]}
            />
          </div>
          <p className="text-xs text-[var(--color-text-muted)]">
            {t("editor.solutionHint")}
          </p>
          <TextArea
            label={t("field.solution")}
            rows={16}
            className={mono}
            spellCheck={false}
            value={row.solution}
            disabled={locked}
            invalid={has("solutionTooLong")}
            onChange={(event) => onChange({ solution: event.target.value })}
          />
        </div>
      ) : null}
    </Card>
  );
}
