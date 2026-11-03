package com.algoprep.identity.domain.model;

import java.time.Instant;

/**
 * Already validated search criteria; every field is optional ({@code null} means "no limit").
 *
 * @param keyword lower-case keyword matched against service, actor e-mail and event code
 * @param category display group
 * @param from earliest {@code created_at}, inclusive
 * @param toExclusive first instant that is too late
 */
public record AuditLogFilter(
    String keyword, AuditCategory category, Instant from, Instant toExclusive) {

  /** No criteria at all. */
  public static AuditLogFilter none() {
    return new AuditLogFilter(null, null, null, null);
  }
}
