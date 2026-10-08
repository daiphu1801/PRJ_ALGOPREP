// PROTOTYPE — no DD yet. See 06-plan/PROTOTYPE_DEBT.md section 10.2 (USR0301_solution_review).
//
// Field shapes follow 02-bd/screens/users/USR0301_solution_review.md Sheet 4.2/5: almost everything
// in areas D-G actually lives inside `ai.solution_reviews.result_json` (one JSONB column), not
// separate DB columns — the exact JSON schema is deferred to 03-dd/api/ai-review.md. These types are
// the FRONTEND shape after that JSON is parsed, which is the only thing this prototype needs.

/** Key of an admin-managed problem level (entities/problem/model/level-store); the review only displays it. */
export type ReviewDifficulty = string;

/** Screen states from BD Sheet 5 Khu vực I — mutually exclusive. */
export type SolutionReviewStatus =
  "loading" | "ready" | "errorTransient" | "errorBudgetLocked";

export type ComplexityAnalysis = {
  actualTime: string;
  optimalTime: string;
  timeExplanation: string;
  actualSpace: string;
  optimalSpace: string;
  spaceExplanation: string;
};

/** 5 fixed rubric criteria, BD Sheet 5 Khu vực E — codes are [SoT: Suy luận], admin-configurable. */
export type RubricCriterionCode =
  "correctness" | "performance" | "cleanCode" | "scalability" | "dataStructure";

export type RubricCriterionScore = {
  code: RubricCriterionCode;
  feedback: string;
};

export type DiffLineKind = "same" | "add" | "del";

export type CodeDiffLine = {
  kind: DiffLineKind;
  text: string;
};

export type CodeDiffProposal = {
  diffLines: CodeDiffLine[];
  diffNote: string;
  explanation: string;
};

export type SolutionReviewDetail = {
  submissionId: string;
  problemTitle: string;
  difficulty: ReviewDifficulty;
  approachTitle: string;
  isApproachOptimal: boolean;
  readabilityScore: number;
  createdAt: string;
  summaryText: string;
  language: string;
  sourceCode: string[];
  complexity: ComplexityAnalysis;
  criteriaScores: RubricCriterionScore[];
  strengths: string[];
  improvements: string[];
  edgeCases: string[];
  codeDiff: CodeDiffProposal;
};

export const RUBRIC_CRITERION_CODES: RubricCriterionCode[] = [
  "correctness",
  "performance",
  "cleanCode",
  "scalability",
  "dataStructure",
];
