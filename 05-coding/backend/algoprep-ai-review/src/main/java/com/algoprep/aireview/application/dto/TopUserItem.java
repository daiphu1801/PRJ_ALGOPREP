package com.algoprep.aireview.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Endpoint 30 row; the name and the cost are null when their source is unavailable. */
public record TopUserItem(
    UUID userId, String displayName, long tokens, int calls, BigDecimal costUsd) {}
