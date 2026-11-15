package com.algoprep.identity.application.dto;

import java.util.UUID;

/** Partial update of the settings: a {@code null} field was not sent and keeps its value. */
public record UpdateMySettingsCommand(
    UUID userId,
    Boolean streakReminderEnabled,
    Boolean weeklyReportEnabled,
    String preferredInterviewLevel,
    Integer preferredMaxTurns,
    Boolean preferredHintAllowed,
    String defaultLanguage) {}
