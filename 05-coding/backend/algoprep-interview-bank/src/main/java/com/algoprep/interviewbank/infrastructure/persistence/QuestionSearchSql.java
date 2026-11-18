package com.algoprep.interviewbank.infrastructure.persistence;

/** SQL predicates shared by the learner list and the management list; bind :tsq as text. */
final class QuestionSearchSql {
  /** Prefix full-text match on title and content, accent-insensitive, GIN-backed (DB6 index). */
  static final String FTS =
      "to_tsvector('simple', interview_bank.f_unaccent(q.title || ' ' || coalesce(q.content_markdown, '')))"
          + " @@ to_tsquery('simple', interview_bank.f_unaccent(CAST(:tsq AS text)))";

  private QuestionSearchSql() {}
}
