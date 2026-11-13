package com.algoprep.aireview.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** One rubric score of one COMPLETED session, as the summary query reads it (endpoint 13). */
public record RubricRow(
    UUID sessionId,
    InterviewEntryType entryType,
    Instant startedAt,
    Instant endedAt,
    RubricCriterion criterion,
    BigDecimal score) {}
