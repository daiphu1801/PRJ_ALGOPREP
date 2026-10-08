// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 24.
//
// Body of the "Đặc tả" tab (BD SHR0202 Sheet 5 Khu vực F): ONE shared function signature for the
// function-wrapper model (the harness maps the types to each language, so only the spelling of the
// name differs), the stdin/stdout formats for Standard I/O, and the matching strategy. It edits a
// `ProblemSpec` and reports every change up; the form owns the draft, the dirty flag and saving.
//
// Simple version on purpose: a type is one kind plus, for containers, one scalar element kind (and
// 1 to 3 dimensions for arrays). No nested type picker yet (BD Q10).
"use client";

import { useRef } from "react";
import { Plus, Trash2 } from "lucide-react";
import {
  CONTAINER_KINDS,
  MATCHING_STRATEGIES,
  SPEC_LANGUAGES,
  TYPE_KINDS,
  allowsUnorderedSet,
  defaultType,
  derivedName,
  elementKinds,
  epsilonValid,
  exceedsSchema,
  renderSignature,
  signatureIssues,
  type FunctionSignature,
  type MatchingStrategy,
  type ProblemSpec,
  type SpecLanguage,
  type SpecType,
  type TypeKind,
} from "@/entities/problem";
import { useT } from "@/shared/i18n";
import { toast } from "@/shared/lib/toast-store";
import {
  Button,
  Card,
  IconAction,
  NoticeTile,
  SelectField,
  TextArea,
  TextField,
} from "@/shared/ui";

type Props = {
  spec: ProblemSpec;
  onChange: (spec: ProblemSpec) => void;
};

const LANGUAGE_CHIP: Record<SpecLanguage, string> = {
  java: "bg-[var(--color-lang-java-bg)] text-[var(--color-lang-java-fg)]",
  cpp: "bg-[var(--color-lang-cpp-bg)] text-[var(--color-lang-cpp-fg)]",
  python: "bg-[var(--color-lang-py-bg)] text-[var(--color-lang-py-fg)]",
};

export function SpecEditor({ spec, onChange }: Props) {
  const t = useT("problemAuthoring");
  const { signature } = spec;
  const issues = signatureIssues(signature);

  const patch = (changes: Partial<ProblemSpec>) =>
    onChange({ ...spec, ...changes });
  const patchSignature = (changes: Partial<FunctionSignature>) =>
    patch({ signature: { ...signature, ...changes } });

  // A return type that makes the current strategy meaningless is refused with a toast, never silently
  // fixed (BD Sheet 9 mục 6): the author decides which of the two to change.
  function setReturnType(returnType: SpecType) {
    const next = { ...spec, signature: { ...signature, returnType } };
    if (
      spec.matchingStrategy === "UNORDERED_SET" &&
      !allowsUnorderedSet(next)
    ) {
      toast.error(t("spec.errorUnorderedSet"));
      return;
    }
    onChange(next);
  }

  function setStrategy(matchingStrategy: MatchingStrategy) {
    if (matchingStrategy === "UNORDERED_SET" && !allowsUnorderedSet(spec)) {
      toast.error(t("spec.errorUnorderedSet"));
      return;
    }
    patch({ matchingStrategy });
  }

  // New parameter ids: unique inside the draft, only needed as list keys.
  const idCounter = useRef(0);
  const nextId = () => `new-${++idCounter.current}`;

  return (
    <div className="flex flex-col gap-3.5">
      {exceedsSchema(spec) ? (
        <NoticeTile tone="warn" title={t("spec.wrapperUnsupportedTitle")}>
          {t("spec.wrapperUnsupportedBody")}
        </NoticeTile>
      ) : null}

      <Card
        title={t("spec.signatureTitle")}
        description={t("spec.signatureSubtitle")}
      >
        <div className="grid gap-2.5 md:grid-cols-2">
          <TextField
            label={t("spec.functionName")}
            value={signature.functionName}
            onChange={(event) =>
              patchSignature({ functionName: event.target.value })
            }
            invalid={issues.includes("functionName")}
            className="font-mono"
          />
          <TypeField
            label={t("spec.returnType")}
            value={signature.returnType}
            onChange={setReturnType}
            invalid={issues.includes("returnType")}
          />
        </div>

        <p className="mt-3 mb-1.5 text-[11px] font-semibold tracking-[0.07em] text-[var(--color-text-subtle)] uppercase">
          {t("spec.parameters")}
        </p>
        <ul className="flex flex-col gap-2">
          {signature.parameters.map((parameter, index) => (
            <li
              key={parameter.id}
              className="grid items-end gap-2 md:grid-cols-[1fr_1.4fr_auto]"
            >
              <TextField
                label={t("spec.parameterName", { index: index + 1 })}
                value={parameter.name}
                onChange={(event) =>
                  patchSignature({
                    parameters: signature.parameters.map((row) =>
                      row.id === parameter.id
                        ? { ...row, name: event.target.value }
                        : row,
                    ),
                  })
                }
                invalid={
                  issues.includes(`parameterName:${parameter.id}`) ||
                  issues.includes(`parameterDuplicate:${parameter.id}`)
                }
                className="font-mono"
              />
              <TypeField
                label={t("spec.parameterType", { index: index + 1 })}
                value={parameter.type}
                onChange={(type) =>
                  patchSignature({
                    parameters: signature.parameters.map((row) =>
                      row.id === parameter.id ? { ...row, type } : row,
                    ),
                  })
                }
                invalid={issues.includes(`parameterType:${parameter.id}`)}
              />
              <IconAction
                icon={Trash2}
                label={t("spec.removeParameter", { index: index + 1 })}
                tone="danger"
                onClick={() =>
                  patchSignature({
                    parameters: signature.parameters.filter(
                      (row) => row.id !== parameter.id,
                    ),
                  })
                }
              />
            </li>
          ))}
        </ul>
        <Button
          variant="ghost"
          size="sm"
          className="mt-2.5 border border-[var(--color-border)]"
          onClick={() =>
            patchSignature({
              parameters: [
                ...signature.parameters,
                { id: nextId(), name: "", type: defaultType("INT") },
              ],
            })
          }
        >
          <Plus aria-hidden="true" className="mr-1 h-3.5 w-3.5" />
          {t("spec.addParameter")}
        </Button>
      </Card>

      <Card title={t("spec.stubTitle")} description={t("spec.stubSubtitle")}>
        <ul className="flex flex-col gap-2.5">
          {SPEC_LANGUAGES.map((language) => (
            <li
              key={language}
              aria-label={t(`spec.language.${language}`)}
              className="glass-surface grid items-center gap-2.5 rounded-2xl border border-[var(--color-border)] px-3.5 py-3 md:grid-cols-[minmax(0,1fr)_14rem]"
            >
              <div className="min-w-0">
                <div className="mb-1.5 flex items-center gap-2">
                  <span
                    aria-hidden="true"
                    className={`flex h-6 min-w-6 items-center justify-center rounded-lg px-1.5 font-mono text-[11px] font-semibold ${LANGUAGE_CHIP[language]}`}
                  >
                    {t(`spec.languageShort.${language}`)}
                  </span>
                  <span className="text-[13px] font-semibold">
                    {t(`spec.language.${language}`)}
                  </span>
                </div>
                <code className="block overflow-x-auto font-mono text-[12.5px] whitespace-pre">
                  {renderSignature(signature, language)}
                </code>
              </div>
              <TextField
                label={t("spec.overrideName", {
                  language: t(`spec.language.${language}`),
                })}
                placeholder={derivedName(signature.functionName, language)}
                value={signature.nameOverrides[language] ?? ""}
                onChange={(event) =>
                  patchSignature({
                    nameOverrides: {
                      ...signature.nameOverrides,
                      [language]: event.target.value,
                    },
                  })
                }
                invalid={issues.includes(`override:${language}`)}
                className="font-mono"
              />
            </li>
          ))}
        </ul>
      </Card>

      <Card title={t("spec.ioTitle")} description={t("spec.ioSubtitle")}>
        <div className="grid gap-3 md:grid-cols-2">
          <TextArea
            label={t("spec.stdinFormat")}
            rows={5}
            value={spec.stdinFormat}
            onChange={(event) => patch({ stdinFormat: event.target.value })}
            invalid={!spec.stdinFormat.trim()}
          />
          <TextArea
            label={t("spec.stdoutFormat")}
            rows={5}
            value={spec.stdoutFormat}
            onChange={(event) => patch({ stdoutFormat: event.target.value })}
            invalid={!spec.stdoutFormat.trim()}
          />
        </div>
      </Card>

      <Card
        title={t("spec.matchingTitle")}
        description={t("spec.matchingSubtitle")}
      >
        <div className="grid items-start gap-3 md:grid-cols-2">
          <SelectField
            label={t("spec.matchingStrategy")}
            value={spec.matchingStrategy}
            onChange={(event) =>
              setStrategy(event.target.value as MatchingStrategy)
            }
            options={MATCHING_STRATEGIES.map((strategy) => ({
              value: strategy,
              label: t(`spec.strategy.${strategy}.label`),
              disabled:
                strategy === "UNORDERED_SET" && !allowsUnorderedSet(spec),
            }))}
          />
          {spec.matchingStrategy === "EPSILON" ? (
            <TextField
              label={t("spec.epsilon")}
              inputMode="decimal"
              value={spec.epsilon}
              onChange={(event) => patch({ epsilon: event.target.value })}
              invalid={!epsilonValid(spec)}
              className="font-mono"
            />
          ) : null}
        </div>
        <p className="mt-2.5 text-[12.5px] text-[var(--color-text-muted)]">
          {t(`spec.strategy.${spec.matchingStrategy}.meta`)}
        </p>
      </Card>
    </div>
  );
}

/**
 * One type. A container shows its element type as a nested field with a rule line beside it, so
 * `List<List<String>>` reads as two stacked pickers (BD SHR0202 Q10, option A). `kinds` is what this
 * field may be: every kind at the top, the parent's allowed element kinds below.
 */
function TypeField({
  label,
  value,
  onChange,
  invalid,
  kinds = TYPE_KINDS,
  level = 1,
}: {
  label: string;
  value: SpecType;
  onChange: (value: SpecType) => void;
  invalid: boolean;
  kinds?: TypeKind[];
  level?: number;
}) {
  const t = useT("problemAuthoring");
  const container = CONTAINER_KINDS.includes(value.kind);

  return (
    <div className="flex flex-col gap-2">
      <div className="flex flex-wrap items-end gap-2">
        <SelectField
          label={label}
          value={value.kind}
          onChange={(event) =>
            onChange(defaultType(event.target.value as TypeKind))
          }
          options={kinds.map((kind) => ({
            value: kind,
            label: t(`spec.kind.${kind}`),
          }))}
          aria-invalid={invalid || undefined}
          wrapperClassName="min-w-[9rem] flex-1"
          className={invalid ? "border-[var(--color-danger)]" : undefined}
        />
        {value.kind === "ARRAY" ? (
          <SelectField
            label={t("spec.dimensions", { type: label })}
            hideLabel
            value={String(value.dimensions ?? 1)}
            onChange={(event) =>
              onChange({ ...value, dimensions: Number(event.target.value) })
            }
            options={[1, 2, 3].map((count) => ({
              value: String(count),
              label: t("spec.dimensionsValue", { count }),
            }))}
            wrapperClassName="w-24"
          />
        ) : null}
      </div>
      {container ? (
        <div className="border-l-2 border-[var(--color-border)] pl-3">
          <TypeField
            label={t("spec.elementKind", { type: label })}
            value={value.of ?? defaultType("INT")}
            onChange={(of) => onChange({ ...value, of })}
            invalid={false}
            kinds={elementKinds(value.kind, level)}
            level={level + 1}
          />
        </div>
      ) : null}
    </div>
  );
}
