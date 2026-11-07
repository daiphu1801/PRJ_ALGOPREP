package com.algoprep.problembank.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Response of #40; field names are the wire names. */
public record ProblemStatsView(int submissionCount, BigDecimal acRate, Instant updatedAt) {}
