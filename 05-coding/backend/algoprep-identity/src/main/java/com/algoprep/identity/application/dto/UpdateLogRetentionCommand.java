package com.algoprep.identity.application.dto;

import java.util.UUID;

/**
 * Edit of the audit log retention (#52). Both values are required; {@code retentionDays} is a
 * {@code Long} so a number too large for an int still reaches validation.
 *
 * @param actorUserId the admin, taken from the token
 * @param retentionDays sent days, or null when missing or not a whole number
 * @param expiredPolicy sent policy name
 */
public record UpdateLogRetentionCommand(
    UUID actorUserId, Long retentionDays, String expiredPolicy) {}
