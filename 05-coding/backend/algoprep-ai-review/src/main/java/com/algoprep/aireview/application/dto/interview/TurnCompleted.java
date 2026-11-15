package com.algoprep.aireview.application.dto.interview;

/** Event {@code turn.completed}: the AI line, counters and stage are committed. */
public record TurnCompleted(
    int aiTurnIndex,
    String content,
    String stage,
    boolean stageChanged,
    int turnCount,
    Integer maxTurns,
    boolean shouldComplete) {}
