package com.algoprep.aireview.domain.model;

import java.util.UUID;

/** A cache row joined with the original review it points at. */
public record ReviewCacheEntry(
    UUID cacheId, UUID originalReviewId, String resultJson, int readabilityScore) {}
