package com.algoprep.identity.application.dto;

import com.algoprep.identity.domain.model.ScheduleSpec;
import java.util.UUID;

/**
 * Partial update (#21): a {@code null} field was not sent and keeps its value; an empty description
 * clears it; a schedule object replaces the whole schedule and an empty one clears it.
 */
public record UpdateClassCommand(
    UUID instructorId, UUID classId, String name, String description, ScheduleSpec schedule) {}
