package com.algoprep.identity.domain.model;

/**
 * One of the four exclusive groups of #63.
 *
 * @param groupCode {@code STUDENT}, {@code INSTRUCTOR}, {@code ADMIN} or {@code DEACTIVATED}
 * @param percent share of the four-group total, already rounded to one decimal
 */
public record UserGroupShare(String groupCode, long count, double percent) {}
