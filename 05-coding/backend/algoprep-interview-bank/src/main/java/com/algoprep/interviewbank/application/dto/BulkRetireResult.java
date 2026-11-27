package com.algoprep.interviewbank.application.dto;

import java.util.List;
import java.util.UUID;

/** Partial success is a normal 200: failed ids carry {@code INB-010}. */
public record BulkRetireResult(List<UUID> retiredIds, List<UUID> failedIds) {}
