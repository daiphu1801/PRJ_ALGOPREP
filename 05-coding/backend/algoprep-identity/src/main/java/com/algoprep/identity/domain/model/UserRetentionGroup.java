package com.algoprep.identity.domain.model;

/** One month of the new-versus-returning chart; {@code month} reads {@code yyyy-MM} in UTC+7. */
public record UserRetentionGroup(String month, long newUsers, long returningUsers) {}
