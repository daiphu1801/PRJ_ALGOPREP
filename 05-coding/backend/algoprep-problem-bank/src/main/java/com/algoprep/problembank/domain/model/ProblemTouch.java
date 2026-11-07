package com.algoprep.problembank.domain.model;

import java.time.Instant;

/** The problem row after a testcase change: what the client needs to carry on. */
public record ProblemTouch(Instant updatedAt, int testcaseSetVersion) {}
