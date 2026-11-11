package com.algoprep.aireview.application.dto;

import java.time.LocalDate;

/** Result of one JOB-AI-03 run, used for the single INFO log line. */
public record RolloverOutcome(
    boolean skippedByLock, boolean changed, LocalDate periodStart, boolean unlocked) {}
