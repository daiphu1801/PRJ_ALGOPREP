package com.algoprep.identity.application.dto;

/**
 * Outcome of a successful refresh.
 *
 * @param accessToken new signed JWT
 * @param expiresInSeconds remaining lifetime of the access token
 * @param rawRefreshToken rotated value for the cookie
 */
public record RefreshResult(String accessToken, long expiresInSeconds, String rawRefreshToken) {}
