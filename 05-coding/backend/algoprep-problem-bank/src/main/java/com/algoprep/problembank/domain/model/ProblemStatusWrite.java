package com.algoprep.problembank.domain.model;

import java.time.Instant;

/** What a status write left on the problem row. */
public record ProblemStatusWrite(
    Instant updatedAt, Instant publishedAt, int currentTestcaseSetVersion) {}
