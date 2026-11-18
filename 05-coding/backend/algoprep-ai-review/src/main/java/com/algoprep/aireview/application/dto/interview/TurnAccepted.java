package com.algoprep.aireview.application.dto.interview;

import java.util.UUID;

/** Event {@code turn.accepted}: the USER line is committed. */
public record TurnAccepted(
    UUID sessionId,
    int userTurnIndex,
    int aiTurnIndex,
    String stage,
    int turnCount,
    Integer maxTurns) {}
