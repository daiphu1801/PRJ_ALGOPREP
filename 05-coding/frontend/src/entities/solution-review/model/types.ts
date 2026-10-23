// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0301_solution_review).
//
// Field shapes follow the result_json schema of ai.solution_reviews, 02-bd/database/ai-review.md
// section 1.3a (schemaVersion, approachTitle, verdict, isApproachOptimal, complexity, strengths,
// improvements, edgeCases, followUpTopics, codeDiff). There is no summaryText / criteriaScores /
// diffLines / diffNote: the diff is computed from the original vs suggested code at render time.
// readabilityScore is a separate DB column; problem/level/createdAt/sourceCode are joined from
// other tables. This is the FRONTEND shape after the DTO is parsed.

/** Key of an admin-managed problem level (entities/problem/model/level-store); the review only displays it. */
export type ReviewDifficulty = string;

/** Screen states from BD Sheet 5 Khu vực I — mutually exclusive. */
export type SolutionReviewStatus =
  "loading" | "ready" | "errorTransient" | "errorBudgetLocked";

export type ReviewVerdict = "OPTIMAL" | "IMPROVABLE" | "NOT_OPTIMAL";

export type ComplexityAnalysis = {
  actualTime: string;
  /** null when the problem has no known optimal complexity (1.3a). */
  optimalTime: string | null;
  timeExplanation: string;
  actualSpace: string;
  optimalSpace: string | null;
  spaceExplanation: string;
};

/** Both null when the approach is already optimal (nothing to suggest). */
export type CodeDiffProposal = {
  suggestedCode: string | null;
  explanation: string | null;
};

export type SolutionReviewDetail = {
  submissionId: string;
  /** Where "apply to workspace" writes the suggestion (browser draft key = problem + language + model). */
  problemId: string;
  languageKey: "java" | "cpp" | "python";
  submissionModel: "function" | "stdio";
  problemTitle: string;
  difficulty: ReviewDifficulty;
  readabilityScore: number;
  createdAt: string;
  sourceCode: string[];
  // --- result_json (1.3a) ---
  schemaVersion: string;
  approachTitle: string;
  verdict: ReviewVerdict;
  isApproachOptimal: boolean;
  complexity: ComplexityAnalysis;
  strengths: string[];
  improvements: string[];
  edgeCases: string[];
  /** Topic keywords only; USR0301 Sheet 5 has no item showing them yet, so the view skips them. */
  followUpTopics: string[];
  codeDiff: CodeDiffProposal;
};
