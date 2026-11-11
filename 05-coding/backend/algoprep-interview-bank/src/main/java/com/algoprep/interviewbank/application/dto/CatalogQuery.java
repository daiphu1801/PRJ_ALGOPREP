package com.algoprep.interviewbank.application.dto;

/** {@code includeCounts} only takes effect for an administrator. */
public record CatalogQuery(boolean admin, boolean includeCounts) {}
