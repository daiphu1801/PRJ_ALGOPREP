package com.algoprep.interviewbank.domain.model;

import java.util.UUID;

/**
 * Already-normalised learner list filters. {@code tsQuery} and {@code likeText} are null when the
 * search text had no usable token; {@code likeText} is already LIKE-escaped.
 */
public record LearnerQuestionCriteria(
    UUID userId,
    String tsQuery,
    String likeText,
    UUID topicId,
    UUID levelId,
    RecallFilter status,
    boolean bookmarkedOnly,
    int page,
    int pageSize,
    boolean drill) {}
