package com.algoprep.problembank.application.dto;

/** Only author-visible defaults; no worker/admin settings or secrets. */
public record AuthoringDefaults(
    int timeLimitMs, int memoryLimitMb, int maxOutputSizeKb, int maxAiGenerationsPerProblem) {}
