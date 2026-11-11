package com.algoprep.interviewbank.domain.model;

import java.math.BigDecimal;

/** One scored criterion; the weight is copied from the rubric snapshot, never from the AI. */
public record FeedbackCriterion(
    String criterionCode, int achievementPercent, BigDecimal weightPercent) {}
