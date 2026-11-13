package com.algoprep.aireview.application.dto;

/** Result of one JOB-AI-05 run: skipped by the lock, or the number of alerts written. */
public record AnomalyRunOutcome(boolean skippedByLock, int created) {}
