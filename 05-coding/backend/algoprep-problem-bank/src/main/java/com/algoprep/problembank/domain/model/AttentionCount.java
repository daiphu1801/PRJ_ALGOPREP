package com.algoprep.problembank.domain.model;

/** How many problems of the scope match one attention rule (DD F2 section 27). */
public record AttentionCount(String ruleCode, int count) {}
