package com.algoprep.problembank.domain.model;

import java.util.UUID;

/**
 * Validated filter of the management list. {@code sort} is one of the seven allowed keys and {@code
 * authorScope} is null for an admin, the caller's id for an instructor.
 */
public record AuthoringListFilter(
    String q,
    UUID levelId,
    String status,
    String sort,
    boolean ascending,
    int page,
    int pageSize,
    UUID authorScope) {}
