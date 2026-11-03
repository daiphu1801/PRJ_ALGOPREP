package com.algoprep.identity.application.dto;

/**
 * Outcome of a successful login.
 *
 * @param accessToken signed JWT
 * @param expiresInSeconds remaining lifetime of the access token
 * @param user who logged in
 * @param rawRefreshToken value for the HttpOnly cookie; never stored, only its hash is
 */
public record LoginResult(
    String accessToken, long expiresInSeconds, UserSummary user, String rawRefreshToken) {}
