package com.algoprep.aireview.domain.model;

/** Rate-limit and warning settings of the budget singleton row (endpoints 21 and 22). */
public record LimitsConfig(
    int reviewPerHour, int interviewPerHour, int cooldownSeconds, int warnPercent) {}
