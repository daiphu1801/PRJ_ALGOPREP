package com.algoprep.identity.domain.model;

/**
 * Audit log retention (BD 1.13).
 *
 * @param retentionDays days a row is kept, at least 1
 * @param expiredPolicy what happens to a row older than that
 */
public record LogRetentionSettings(int retentionDays, ExpiredLogPolicy expiredPolicy) {}
