// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md
//
// Mock data for the 9 admin_overview blocks. Values are ported VERBATIM from
// 09-layoutBase/Admin - Tổng quan.dc.html `renderVals()` (statCards, gaugeVals, dayVals/weekTotal/
// dayAvg, monthVals/monthTotal, topProblems, visitorGroups) — the owner asked for 1:1 fidelity
// against the live prototype, not just matching proportions, so the exact sample numbers/labels are
// kept instead of invented ones. Each block's loading/empty/error state machine
// (shared/ui/charts/dashboard-block-state.tsx) is exercised for real via TanStack Query's own
// isLoading/isError, only the "always succeeds with real-looking data" part is mocked.
import type {
  DifficultyBreakdown,
  StatSummary,
  SubmissionsByDay,
  SubmissionsByLanguage,
  SubmissionsByMonth,
  TopProblems,
  UserRetention,
  VerdictDistribution,
} from "../../model/types";

function delay<T>(value: T, ms = 500): Promise<T> {
  return new Promise((resolve) => setTimeout(() => resolve(value), ms));
}

// dc.html:424-425 (statCards).
export function fakeSubmissionsSummary(): Promise<StatSummary> {
  return delay({
    value: 9416,
    deltaPercent: 12.1,
    deltaDirection: "up",
    sparklineSeries: [8, 9, 8.6, 10, 9.4, 11.2, 12.8],
  });
}

export function fakeActiveUsersSummary(): Promise<StatSummary> {
  return delay({
    value: 1284,
    deltaPercent: 8.4,
    deltaDirection: "up",
    sparklineSeries: [12, 11.4, 11.8, 10.6, 10.9, 10.1, 9.4],
  });
}

export function fakeSubmissionsByLanguage(): Promise<SubmissionsByLanguage> {
  // dc.html:428-432 (langLegend order Python/C++/Java, barSeed shared across all 3 series in the
  // static mock — kept as one representative wave per language here since the prototype's own bar
  // color cycles i%3 across a single shared series, not 3 independent series).
  const pointLabels = Array.from({ length: 20 }, (_, i) => `N${i + 1}`);
  const barSeed = [40, 55, 30, 70, 45, 60, 35, 80, 50, 65, 42, 58, 38, 72, 48, 62, 33, 68, 52, 44];
  return delay({
    pointLabels,
    series: [
      { label: "Python", colorVar: "--color-admin-cyan", points: barSeed.map((v, i) => (i % 3 === 0 ? v : 0)) },
      { label: "C++", colorVar: "--color-admin-teal", points: barSeed.map((v, i) => (i % 3 === 1 ? v : 0)) },
      { label: "Java", colorVar: "--color-admin-slate", points: barSeed.map((v, i) => (i % 3 === 2 ? v : 0)) },
    ],
  });
}

export function fakeVerdictDistribution(): Promise<VerdictDistribution> {
  // dc.html:434-438 (gaugeVals) — full Vietnamese labels as actually shown in the mockup legend,
  // not the raw judge-verdict codes (AC/WA/TLE/RE/CE stay as the domain enum elsewhere, e.g.
  // judge-orchestration; this screen displays the human label per dc.html).
  return delay({
    slices: [
      { label: "Accepted", value: 48, colorVar: "--color-admin-cyan" },
      { label: "Sai kết quả", value: 24, colorVar: "--color-admin-slate" },
      { label: "Quá thời gian", value: 14, colorVar: "--color-admin-teal" },
      { label: "Lỗi thực thi", value: 8, colorVar: "--color-admin-negative" },
      { label: "Lỗi biên dịch", value: 6, colorVar: "--color-admin-warn" },
    ],
  });
}

export function fakeDifficultyBreakdown(): Promise<DifficultyBreakdown> {
  // dc.html:454-467 (difficultyLegend/diffData) use "AI sinh"/"Giảng viên soạn" as the 2-series
  // labels — kept here ONLY as colors/proportions reference. The labels themselves are
  // deliberately NOT copied 1:1: 02-bd/screens/admin/admin_overview.md section 2 point 4 already
  // decided (DEC-2026-0831-admin-overview-dashboard-stats) that "nguồn gốc bài toán" (AI-authored vs.
  // instructor-authored) is not a real attribute in problem-bank, and replaced the axis with
  // Tổng lượt nộp / Accepted — a real, derivable pair. Visual shape (2 bars x 3 difficulty groups,
  // cyan/slate) still matches dc.html; only the semantic label differs, on purpose.
  return delay({
    legend: [
      { label: "Tổng lượt nộp", value: 0, colorVar: "--color-admin-slate" },
      { label: "Accepted", value: 0, colorVar: "--color-admin-cyan" },
    ],
    groups: [
      {
        label: "Dễ",
        bars: [
          { label: "Tổng", value: 260, colorVar: "--color-admin-slate" },
          { label: "AC", value: 140, colorVar: "--color-admin-cyan" },
        ],
      },
      {
        label: "Trung bình",
        bars: [
          { label: "Tổng", value: 380, colorVar: "--color-admin-slate" },
          { label: "AC", value: 260, colorVar: "--color-admin-cyan" },
        ],
      },
      {
        label: "Khó",
        bars: [
          { label: "Tổng", value: 300, colorVar: "--color-admin-slate" },
          { label: "AC", value: 420, colorVar: "--color-admin-cyan" },
        ],
      },
    ],
  });
}

export function fakeSubmissionsByDay(): Promise<SubmissionsByDay> {
  // dc.html:470-474 (dayLabels/dayVals/weekTotal/dayAvg) — dc.html scales intensity out of 10 dots.
  const dayVals = [7, 8, 6, 7, 10, 6, 4];
  return delay({
    columns: ["T2", "T3", "T4", "T5", "T6", "T7", "CN"].map((label, i) => ({
      label,
      intensity: dayVals[i]!,
    })),
    weekTotal: 942,
    dailyAverage: 135,
  });
}

export function fakeSubmissionsByMonth(): Promise<SubmissionsByMonth> {
  // dc.html:476-479 (monthLabels/monthVals/monthTotal).
  return delay({
    points: [
      { label: "T1", value: 720 },
      { label: "T2", value: 810 },
      { label: "T3", value: 690 },
      { label: "T4", value: 940 },
      { label: "T5", value: 880 },
      { label: "T6", value: 1020 },
    ],
    monthTotal: 3618,
  });
}

export function fakeTopProblems(): Promise<TopProblems> {
  // dc.html:481-484 (topProblems).
  return delay({
    items: [
      { label: "Two Sum", value: 295 },
      { label: "Course Schedule", value: 285 },
      { label: "Coin Change", value: 265 },
      { label: "Merge Intervals", value: 245 },
    ],
  });
}

export function fakeUserRetention(): Promise<UserRetention> {
  // dc.html:488-492 (visitorLegend/visitorGroups) — labels T1..T6 to match monthLabels2 = monthLabels.
  const pairs: Array<[number, number]> = [
    [320, 480],
    [410, 530],
    [280, 600],
    [520, 690],
    [450, 610],
    [600, 740],
  ];
  const labels = ["T1", "T2", "T3", "T4", "T5", "T6"];
  return delay({
    legend: [
      { label: "Người dùng mới", value: 0, colorVar: "--color-admin-teal" },
      { label: "Quay lại", value: 0, colorVar: "--color-admin-cyan" },
    ],
    groups: pairs.map(([mới, quayLại], i) => ({
      label: labels[i]!,
      bars: [
        { label: "Mới", value: mới, colorVar: "--color-admin-teal" },
        { label: "Quay lại", value: quayLại, colorVar: "--color-admin-cyan" },
      ],
    })),
  });
}
