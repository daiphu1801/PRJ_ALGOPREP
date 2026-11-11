package com.algoprep.aireview.application.dto;

import com.algoprep.aireview.domain.model.CallerCategory;
import java.util.UUID;

/** Endpoint 10 input, already validated at the edge (AIR-V08, AIR-V09). */
public record SendInterviewTurnCommand(
    UUID callerId,
    CallerCategory category,
    UUID sessionId,
    String content,
    int expectedTurnCount) {}
