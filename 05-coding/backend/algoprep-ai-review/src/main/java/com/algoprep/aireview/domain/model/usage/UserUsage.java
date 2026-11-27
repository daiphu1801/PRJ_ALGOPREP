package com.algoprep.aireview.domain.model.usage;

import java.math.BigDecimal;
import java.util.UUID;

/** Usage of one user; {@code costUsd} is null when any model of the user has no price row. */
public record UserUsage(UUID userId, long tokens, int calls, BigDecimal costUsd) {}
