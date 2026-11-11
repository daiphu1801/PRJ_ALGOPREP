package com.algoprep.aireview.domain.model;

/** Partial update of {@link LimitsConfig}: a null component keeps the stored value. */
public record LimitsPatch(
    Integer reviewPerHour,
    Integer interviewPerHour,
    Integer cooldownSeconds,
    Integer warnPercent) {}
