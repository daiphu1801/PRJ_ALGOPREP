import type { LanguageFilter, VerdictFilter } from "@/entities/submission";

const VERDICTS: readonly VerdictFilter[] = [
  "ALL",
  "ACCEPTED",
  "WRONG_ANSWER",
  "TIME_LIMIT_EXCEEDED",
  "COMPILE_ERROR",
  "RUNTIME_ERROR",
];
const LANGUAGES: readonly LanguageFilter[] = ["ALL", "PYTHON", "JAVA", "CPP"];
export type SubmissionFilters = {
  query: string;
  verdict: VerdictFilter;
  language: LanguageFilter;
  page: number;
};

export function normalizeSubmissionQuery(value: string): string {
  return Array.from(value.trim()).slice(0, 100).join("");
}

export function readSubmissionFilters(
  search: URLSearchParams,
): SubmissionFilters {
  const verdict = search.get("verdict") as VerdictFilter;
  const language = search.get("language") as LanguageFilter;
  const page = Number(search.get("page"));
  return {
    query: normalizeSubmissionQuery(search.get("q") ?? ""),
    verdict: VERDICTS.includes(verdict) ? verdict : "ALL",
    language: LANGUAGES.includes(language) ? language : "ALL",
    page:
      Number.isSafeInteger(page) && page > 0 && page <= 2147483647 ? page : 1,
  };
}

/** Keep unrelated query parameters while writing one normalized filter state. */
export function submissionFiltersUrl(
  path: string,
  search: string,
  filters: SubmissionFilters,
): string {
  const next = new URLSearchParams(search);
  const values = {
    q: normalizeSubmissionQuery(filters.query),
    verdict: filters.verdict === "ALL" ? "" : filters.verdict,
    language: filters.language === "ALL" ? "" : filters.language,
    page: filters.page <= 1 ? "" : String(filters.page),
  };
  for (const [key, value] of Object.entries(values)) {
    if (value) next.set(key, value);
    else next.delete(key);
  }
  return next.size ? `${path}?${next.toString()}` : path;
}
