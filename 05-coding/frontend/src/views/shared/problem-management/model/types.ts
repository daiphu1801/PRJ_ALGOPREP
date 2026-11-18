import type { PageMeta } from "@/shared/api";

/**
 * Problem as the management list sees it. Shared screen: A2 and A3 both reach it, mounted under
 * both /instructor and /admin (DEC-2026-0825-shared-content-authoring-screens).
 *
 * TWO lifecycle states only — draft and published. DEC-2026-0830-problem-lifecycle-two-states drops
 * the mockup's third "Đã ẩn" state and adds code F2-15. The mockup still carries it on one row and
 * in a bulk action labelled "Xuất bản / ẩn".
 */
export type ProblemStatus = "draft" | "published";

/** Stable key of an admin-managed problem difficulty level (entities/problem/model/level-store). */
export type Difficulty = string;

export type AdminProblem = {
  /**
   * Row key and the id in the row links. Real mode: the problem uuid. Mock mode: the display code
   * without its "#", which is what the detail and edit mocks look a problem up by.
   */
  id: string;
  /** Display code, e.g. "#1143" (mock) or the server's `code`. */
  code: string;
  title: string;
  /** Topic names, in the order the server lists them. */
  topics: string[];
  difficulty: Difficulty;
  status: ProblemStatus;
  submissionCount: number;
  /** Classes the problem is currently assigned to (shown as a warning when deleting; never blocks). */
  assignedClassCount: number;
  /** Accepted rate, 0-100; null while the problem has no statistics row (shown as "-", never "0%"). */
  acceptedRate: number | null;
  testcaseCount: number;
  /** ISO-8601 UTC; the view prints it as a relative time. */
  updatedAt: string;
};

/** One row of the "Phân bố theo chủ đề" block (BD TopicDistributionItemDto). */
export type TopicDistributionItem = {
  topicName: string;
  count: number;
  /** Share of the problems that have a topic, 0-100. */
  percent: number;
};

export type TopicDistribution = {
  items: TopicDistributionItem[];
  /** Problems with at least one topic: the denominator of `percent`. */
  totalProblemsWithTopic: number;
  totalTopicsUsed: number;
};

export type AttentionRuleCode =
  "noTestcase" | "lowAcceptRate" | "hiddenFromLearners" | "staleDraft";

/** One row of the "Bài cần chú ý" block (BD AttentionItemDto); `ruleCode` keys the i18n label. */
export type AttentionItem = {
  ruleCode: AttentionRuleCode;
  count: number;
};

export type { PageMeta };

/** Filters, sort and paging the screen sends to the server; every value is already normalised. */
export type AdminListParams = {
  q: string;
  /** Level key (its code), or "all". The api layer turns it into the level id the server wants. */
  level: string;
  status: ProblemStatus | "all";
  sort: ProblemSortKey;
  dir: "asc" | "desc";
  page: number;
  pageSize: number;
};

export type AdminProblemList = {
  items: AdminProblem[];
  page: PageMeta;
  /** The two figures of the header line: in the caller's scope, before any filter. */
  totals: { total: number; published: number };
  /** Mock only: problems per level key and per topic key, for the manager dialogs. Real mode reads `problemCount` of the catalogue. */
  usage?: {
    levels: Readonly<Record<string, number>>;
    topics: Readonly<Record<string, number>>;
  };
};

/** Answer of a bulk action: each problem is handled on its own and may fail on its own. */
export type BulkResult = {
  succeededIds: string[];
  failedItems: { id: string; code: string }[];
};

export type ProblemSortKey =
  | "title"
  | "code"
  | "difficulty"
  | "status"
  | "submissions"
  | "accepted"
  | "edited";
