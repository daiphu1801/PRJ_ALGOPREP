package com.algoprep.problembank.application.dto;

import java.util.UUID;

/**
 * Raw, unvalidated parameters of DD F2 section 25.2: the handler rejects bad values with 400
 * PRB-001 (the management endpoints read strictly). {@code admin} widens the scope to the bank.
 */
public record ListProblemsQuery(
    UUID actorId,
    boolean admin,
    String q,
    String level,
    String status,
    String sort,
    String dir,
    Integer page,
    Integer pageSize) {}
