package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * One account as the admin user list shows it (#35).
 *
 * @param lastActiveAt latest recorded activity ({@code users.last_active_at}, set at login at most
 *     once a minute), {@code null} when the account never logged in
 * @param hasPassword {@code password_hash IS NOT NULL}
 * @param anonymized {@code anonymized_at IS NOT NULL}; the unlock button is disabled for these
 * @param solvedCount problems with {@code best_verdict = ACCEPTED} in {@code
 *     identity.user_problem_best_score}; 0 for an account with no row
 * @param totalSubmissions {@code identity.user_submission_stats.total_submissions}; 0 for an
 *     account with no row
 */
public record AdminUserItem(
    UUID id,
    String displayName,
    String email,
    UUID roleId,
    String roleName,
    String baseCategory,
    String status,
    Instant lastActiveAt,
    boolean hasPassword,
    boolean anonymized,
    long solvedCount,
    long totalSubmissions) {}
