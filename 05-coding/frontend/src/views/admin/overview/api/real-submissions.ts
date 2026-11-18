// Real calls for the judge-backed blocks of the admin overview: #43 and #67 to #72
// (03-dd/api/F1_identity.md section 0.8.5). Each block is its own endpoint, so one failing source
// rejects only its own query and the block shows its error state, never an invented number.
import { api } from "@/shared/api";
import type {
  DifficultyBreakdown,
  StatSummary,
  SubmissionsByDay,
  SubmissionsByLanguage,
  SubmissionsByMonth,
  TopProblems,
  VerdictDistribution,
} from "../model/types";
import { monthLabel } from "./real-dashboard";

const BASE = "/identity/admin/dashboard";

type LanguageDto = {
  pointLabels: string[];
  series: { languageCode: string; values: number[] }[];
};
type VerdictDto = {
  slices: { verdict: string; count: number; percent: number }[];
};
type DifficultyDto = {
  groups: {
    levelId: string;
    levelCode: string;
    levelDisplayName: string;
    submissions: number;
    accepted: number;
  }[];
  legend: string[];
};
type DaysDto = {
  columns: { day: string; count: number }[];
  weekTotal: number;
  dailyAverage: number;
};
type MonthsDto = {
  points: { month: string; count: number }[];
  monthTotal: number;
};
type TopDto = {
  items: { problemId: string; title: string; submissionCount: number }[];
};

// Only colours (design tokens) live here. The words are messages under `adminOverview.labels`, keyed
// by the code the server sends; the block translates `labelKey` (model/labels.ts).
// Legend order of the prototype (Python, C++, Java); the server order is Java, C++, Python.
const LANGUAGES = [
  { code: "PYTHON", colorVar: "--color-admin-cyan" },
  { code: "CPP", colorVar: "--color-admin-teal" },
  { code: "JAVA", colorVar: "--color-admin-slate" },
] as const;

const VERDICT_COLORS: Record<string, string> = {
  ACCEPTED: "--color-admin-cyan",
  WRONG_ANSWER: "--color-admin-slate",
  TIME_LIMIT_EXCEEDED: "--color-admin-teal",
  RUNTIME_ERROR: "--color-admin-negative",
  COMPILE_ERROR: "--color-admin-warn",
};

const DIFFICULTY_LEGEND_COLORS: Record<string, string> = {
  totalSubmissions: "--color-admin-slate",
  acceptedSubmissions: "--color-admin-cyan",
};

/** A code the screen has no message for shows as the code itself, never as a missing-key marker. */
function keyed(group: string, code: string, known: boolean) {
  return known
    ? { label: code, labelKey: `labels.${group}.${code}` }
    : { label: code };
}

// The server sends the local day as "yyyy-MM-dd"; the column reads T2..T7 and CN like the prototype.
const WEEKDAYS = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];

function dayLabel(day: string): string {
  return WEEKDAYS[new Date(`${day}T00:00:00Z`).getUTCDay()]!;
}

export function realSubmissionsSummary(): Promise<StatSummary> {
  return api.get<StatSummary>(`${BASE}/cross-module`);
}

export async function realSubmissionsByLanguage(): Promise<SubmissionsByLanguage> {
  const dto = await api.get<LanguageDto>(`${BASE}/submissions/by-language`);
  return {
    pointLabels: dto.pointLabels,
    series: LANGUAGES.flatMap((language) => {
      const found = dto.series.find((s) => s.languageCode === language.code);
      return found
        ? [
            {
              ...keyed("language", language.code, true),
              colorVar: language.colorVar,
              points: found.values,
            },
          ]
        : [];
    }),
  };
}

export async function realVerdictDistribution(): Promise<VerdictDistribution> {
  const dto = await api.get<VerdictDto>(`${BASE}/submissions/verdicts`);
  return {
    // The server rounds the percentages so they add up to 100; the client does not recompute them.
    slices: dto.slices.map((s) => ({
      ...keyed("verdict", s.verdict, s.verdict in VERDICT_COLORS),
      value: s.percent,
      colorVar: VERDICT_COLORS[s.verdict] ?? "--color-admin-slate",
    })),
  };
}

export async function realDifficultyBreakdown(): Promise<DifficultyBreakdown> {
  const dto = await api.get<DifficultyDto>(`${BASE}/submissions/difficulty`);
  return {
    legend: dto.legend.map((key) => ({
      ...keyed("legend", key, key in DIFFICULTY_LEGEND_COLORS),
      value: 0,
      colorVar: DIFFICULTY_LEGEND_COLORS[key] ?? "--color-admin-slate",
    })),
    // The block matches a group to the admin-managed level list by `levelCode`, which is the key of
    // that list in real mode (entities/problem/api/real-catalog.ts).
    levels: dto.groups.map((g) => ({
      key: g.levelCode,
      total: g.submissions,
      accepted: g.accepted,
    })),
  };
}

export async function realSubmissionsByDay(): Promise<SubmissionsByDay> {
  const dto = await api.get<DaysDto>(`${BASE}/submissions/by-day`);
  const peak = Math.max(0, ...dto.columns.map((c) => c.count));
  return {
    // The dot grid has ten rows: each column is scaled to the busiest day of the week.
    columns: dto.columns.map((c) => ({
      label: dayLabel(c.day),
      intensity: peak === 0 ? 0 : Math.round((c.count / peak) * 10),
    })),
    weekTotal: dto.weekTotal,
    dailyAverage: dto.dailyAverage,
  };
}

export async function realSubmissionsByMonth(): Promise<SubmissionsByMonth> {
  const dto = await api.get<MonthsDto>(`${BASE}/submissions/by-month`);
  return {
    points: dto.points.map((p) => ({
      label: monthLabel(p.month),
      value: p.count,
    })),
    monthTotal: dto.monthTotal,
  };
}

export async function realTopProblems(): Promise<TopProblems> {
  const dto = await api.get<TopDto>(`${BASE}/top-problems`);
  return {
    items: dto.items.map((i) => ({ label: i.title, value: i.submissionCount })),
  };
}
