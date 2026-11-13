package com.algoprep.identity.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * An administrator with actions in the last 24 hours (#64).
 *
 * @param actorUserId the admin
 * @param displayName display name
 * @param roleName name of the admin's role
 * @param actionCount actions in the window
 * @param lastActionAt time of the latest action
 */
public record ActiveAdmin(
    UUID actorUserId,
    String displayName,
    String roleName,
    long actionCount,
    Instant lastActionAt) {}
