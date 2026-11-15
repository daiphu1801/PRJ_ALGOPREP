package com.algoprep.interviewbank.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Management view of one ACTIVE question, with aggregate usage (never one person's data). */
public record AdminQuestion(
    UUID id,
    int displayNo,
    UUID topicId,
    UUID levelId,
    String title,
    String contentMarkdown,
    List<String> followUps,
    List<RubricCriterion> rubric,
    Instant createdAt,
    Instant updatedAt,
    long usageCount,
    BigDecimal averageScorePercent) {}
