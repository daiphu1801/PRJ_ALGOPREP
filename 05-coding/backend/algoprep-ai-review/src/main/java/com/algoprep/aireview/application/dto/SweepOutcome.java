package com.algoprep.aireview.application.dto;

/** What one JOB-AI-04 run did; {@code failed} counts sessions left for the next run. */
public record SweepOutcome(int scored, int abandoned, int skipped, int failed) {}
