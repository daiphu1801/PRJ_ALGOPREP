package com.algoprep.identity.application.dto;

import java.time.Instant;

/**
 * A signed access token with its expiry.
 *
 * @param value compact JWT
 * @param expiresAt instant after which the token is rejected
 */
public record IssuedAccessToken(String value, Instant expiresAt) {}
