package com.algoprep.interviewbank.domain.model;

import java.util.UUID;

/**
 * Filters are already resolved to ids; {@code searchQuery} is null or a tsquery-safe string already
 * built from clean tokens.
 */
public record QuestionListCriteria(
    String searchQuery,
    UUID topicId,
    UUID levelId,
    QuestionSort sort,
    boolean descending,
    int page,
    int pageSize) {}
