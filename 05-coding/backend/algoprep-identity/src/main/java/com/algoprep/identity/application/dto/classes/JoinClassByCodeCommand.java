package com.algoprep.identity.application.dto;

import java.util.UUID;

/** {@code code} is the raw client text; the handler normalises it. */
public record JoinClassByCodeCommand(UUID userId, String code) {}
