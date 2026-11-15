package com.algoprep.identity.domain.model;

/**
 * The caller's account settings (DD api F1 section 0.3.1): five {@code user_preferences} columns
 * plus {@code users.default_language}. Defaults apply while no preferences row exists.
 */
public record MySettings(
    boolean streakReminderEnabled,
    boolean weeklyReportEnabled,
    String preferredInterviewLevel,
    int preferredMaxTurns,
    boolean preferredHintAllowed,
    String defaultLanguage) {}
