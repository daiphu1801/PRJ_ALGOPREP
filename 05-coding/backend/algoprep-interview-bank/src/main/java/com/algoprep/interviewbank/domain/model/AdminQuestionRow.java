package com.algoprep.interviewbank.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/** One line of the management list. */
public record AdminQuestionRow(
    UUID id,
    int displayNo,
    String levelCode,
    String levelName,
    String topicCode,
    String topicName,
    String title,
    int followUpCount,
    int rubricCount,
    long usageCount,
    BigDecimal averageScorePercent) {}
