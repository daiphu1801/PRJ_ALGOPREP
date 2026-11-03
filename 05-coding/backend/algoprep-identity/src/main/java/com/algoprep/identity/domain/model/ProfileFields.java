package com.algoprep.identity.domain.model;

/**
 * The editable profile columns after validation. A {@code null} optional value is stored as {@code
 * NULL}; {@code displayName} is never null.
 */
public record ProfileFields(
    String displayName,
    String schoolOrCompany,
    String currentPosition,
    String targetPosition,
    String phone) {}
