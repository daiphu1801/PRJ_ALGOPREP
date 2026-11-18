package com.algoprep.interviewbank.domain.model;

import java.util.UUID;

/**
 * One catalogue row. {@code usesStarFramework} is meaningful for topics only (false for levels).
 */
public record CatalogItem(
    UUID id,
    String code,
    String displayName,
    int sortOrder,
    boolean usesStarFramework,
    Long questionCount) {}
