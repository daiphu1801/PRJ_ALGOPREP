package com.algoprep.identity.application.dto;

import com.algoprep.identity.domain.model.ScheduleSpec;
import java.util.UUID;

/** New class of the calling instructor (#20); raw form values, validated by the handler. */
public record CreateClassCommand(
    UUID instructorId, String name, String description, ScheduleSpec schedule) {}
