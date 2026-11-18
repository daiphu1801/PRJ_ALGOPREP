package com.algoprep.interviewbank.domain.model;

import java.math.BigDecimal;

/** One rubric row; {@code description} is always null from the current screens. */
public record RubricCriterion(String criterionCode, BigDecimal weightPercent, String description) {}
