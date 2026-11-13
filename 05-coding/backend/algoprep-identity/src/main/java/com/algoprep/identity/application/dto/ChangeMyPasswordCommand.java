package com.algoprep.identity.application.dto;

import java.util.UUID;

/**
 * @param userId caller, from the access token subject
 * @param sessionId refresh token family of the caller's session, from the access token {@code sid}
 * @param currentPassword password being replaced (a temporary one when the flag is set)
 * @param newPassword password to store
 */
public record ChangeMyPasswordCommand(
    UUID userId, UUID sessionId, String currentPassword, String newPassword) {
  @Override
  public String toString() {
    return "ChangeMyPasswordCommand[redacted]";
  }
}
