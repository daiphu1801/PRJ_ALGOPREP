// Save-time rules of the language-and-limits screen (BD ADM0501 Sheet 9 NO 2-6, 9) and the change
// counter behind the save bar. Pure functions so the rules can be tested without rendering.
import {
  effectiveMemoryLimitMb,
  effectiveTimeLimitMs,
  GENERATOR_OUTPUT_RANGE,
  GENERATOR_RUNTIME_RANGE,
  type LanguageConfig,
  type LanguageConfigPage,
} from "./types";

export const MULTIPLIER_RANGE = { min: 0.1, max: 9.99 } as const;
export const TIME_LIMIT_RANGE_MS = { min: 100, max: 30_000 } as const;
export const MEMORY_LIMIT_RANGE_MB = { min: 64, max: 1024 } as const;

export type ValidationError =
  | "errorInvalidGenerator"
  | "errorMultiplier"
  | "errorInvalidValue"
  | "errorTimeRange"
  | "errorMemoryRange"
  | "errorLastLanguage";

const inRange = (value: number, range: { min: number; max: number }) =>
  value >= range.min && value <= range.max;

const integerInRange = (value: number, range: { min: number; max: number }) =>
  Number.isInteger(value) && inRange(value, range);

/** 0.10 to 9.99 with at most two decimals (the column is DECIMAL(3,2)). */
export function badMultiplier(value: number): boolean {
  return (
    !Number.isFinite(value) ||
    !inRange(value, MULTIPLIER_RANGE) ||
    Math.abs(value * 100 - Math.round(value * 100)) > 1e-9
  );
}

export const badDefault = (value: number) =>
  !Number.isInteger(value) || value < 1;

export function badTimeLimit(
  page: LanguageConfigPage,
  language: LanguageConfig,
) {
  return !inRange(
    effectiveTimeLimitMs(page.defaults, language),
    TIME_LIMIT_RANGE_MS,
  );
}

export function badMemoryLimit(
  page: LanguageConfigPage,
  language: LanguageConfig,
) {
  return !inRange(
    effectiveMemoryLimitMb(page.defaults, language),
    MEMORY_LIMIT_RANGE_MB,
  );
}

export const badRuntime = (value: number) =>
  !integerInRange(value, GENERATOR_RUNTIME_RANGE);
export const badOutput = (value: number) =>
  !integerInRange(value, GENERATOR_OUTPUT_RANGE);

/** The first rule the draft breaks, or null when it may be saved. Order follows the BD "Thứ tự". */
export function validateLanguageConfig(
  page: LanguageConfigPage,
): ValidationError | null {
  if (
    badRuntime(page.generator.maxRuntimeSeconds) ||
    badOutput(page.generator.maxOutputMb)
  )
    return "errorInvalidGenerator";
  if (
    page.languages.some(
      (language) =>
        badMultiplier(language.timeMultiplier) ||
        badMultiplier(language.memoryMultiplier),
    )
  )
    return "errorMultiplier";
  if (Object.values(page.defaults).some(badDefault)) return "errorInvalidValue";
  if (page.languages.some((language) => badTimeLimit(page, language)))
    return "errorTimeRange";
  if (page.languages.some((language) => badMemoryLimit(page, language)))
    return "errorMemoryRange";
  if (!page.languages.some((language) => language.enabled))
    return "errorLastLanguage";
  return null;
}

/** Number of individual fields that differ between the saved page and the draft. */
export function countChanges(
  saved: LanguageConfigPage,
  draft: LanguageConfigPage,
): number {
  const diff = <T extends object>(a: T, b: T) =>
    (Object.keys(a) as (keyof T)[]).filter((key) => a[key] !== b[key]).length;
  return (
    draft.languages.reduce((sum, language, index) => {
      const before = saved.languages[index];
      return before ? sum + diff(before, language) : sum;
    }, 0) +
    diff(saved.defaults, draft.defaults) +
    diff(saved.sandbox, draft.sandbox) +
    diff(saved.generator, draft.generator)
  );
}

/** Languages switched from enabled to disabled by the draft. */
export function newlyDisabled(
  saved: LanguageConfigPage,
  draft: LanguageConfigPage,
): LanguageConfig[] {
  return draft.languages.filter(
    (language, index) => saved.languages[index]?.enabled && !language.enabled,
  );
}
