package com.algoprep.identity.application.dto;

/** Result of #11: where the code went and how long it stays valid. */
public record PendingEmailChange(String pendingEmail, int expiresInSeconds) {}
