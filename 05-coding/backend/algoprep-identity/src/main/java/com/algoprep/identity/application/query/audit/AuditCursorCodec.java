package com.algoprep.identity.application.query;

import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AuditLogCursor;
import com.algoprep.identity.domain.model.AuditLogFilter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Opaque cursor of #39 (DD 0.7.2): the {@code (created_at, id)} pair plus a short fingerprint of
 * the filter, so a cursor cannot be replayed under different criteria. It carries nothing sensitive
 * and is not a security token.
 */
final class AuditCursorCodec {

  private AuditCursorCodec() {}

  static String encode(AuditLogCursor cursor, AuditLogFilter filter) {
    String plain =
        cursor.createdAt().getEpochSecond()
            + "."
            + cursor.createdAt().getNano()
            + "|"
            + cursor.id()
            + "|"
            + fingerprint(filter);
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(plain.getBytes(StandardCharsets.UTF_8));
  }

  /** Reads a cursor issued for the same filter; anything else is {@code 400 IDT-443}. */
  static AuditLogCursor decode(String token, AuditLogFilter filter) {
    try {
      String plain = new String(Base64.getUrlDecoder().decode(token), StandardCharsets.UTF_8);
      String[] parts = plain.split("\\|", -1);
      String[] time = parts[0].split("\\.", -1);
      if (parts.length != 3 || time.length != 2 || !parts[2].equals(fingerprint(filter))) {
        throw invalid();
      }
      return new AuditLogCursor(
          Instant.ofEpochSecond(Long.parseLong(time[0]), Long.parseLong(time[1])),
          UUID.fromString(parts[1]));
    } catch (RuntimeException malformed) {
      throw invalid();
    }
  }

  private static String fingerprint(AuditLogFilter filter) {
    String plain =
        filter.keyword()
            + "|"
            + filter.category()
            + "|"
            + filter.from()
            + "|"
            + filter.toExclusive();
    return Integer.toHexString(plain.hashCode());
  }

  private static InvalidRequestException invalid() {
    return new InvalidRequestException(IdentityErrorCodes.AUDIT_FILTER_INVALID, "Invalid cursor");
  }
}
