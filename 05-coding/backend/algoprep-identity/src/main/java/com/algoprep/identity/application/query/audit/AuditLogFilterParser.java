package com.algoprep.identity.application.query;

import com.algoprep.common.domain.ports.out.ClockPort;
import com.algoprep.common.exception.InvalidRequestException;
import com.algoprep.identity.domain.exception.IdentityErrorCodes;
import com.algoprep.identity.domain.model.AuditCategory;
import com.algoprep.identity.domain.model.AuditLogFilter;
import com.algoprep.identity.domain.ports.out.AuditLogReadRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Validates the criteria shared by search (#39) and export (#40), DD validation section 12. Days
 * are local UTC+7 days and the range is closed at both ends. The server re-checks everything, also
 * when the client already did.
 */
@Service
class AuditLogFilterParser {
  static final int MAX_KEYWORD = 100;
  private static final ZoneOffset LOCAL = ZoneOffset.ofHours(7);

  private final AuditLogReadRepository logs;
  private final ClockPort clock;

  AuditLogFilterParser(AuditLogReadRepository logs, ClockPort clock) {
    this.logs = logs;
    this.clock = clock;
  }

  AuditLogFilter parse(String q, String categoryCode, String dateFrom, String dateTo) {
    String keyword = keyword(q);
    AuditCategory category = category(categoryCode);
    LocalDate from = date(dateFrom);
    LocalDate to = date(dateTo);
    LocalDate today = clock.now().atOffset(LOCAL).toLocalDate();
    if ((from != null && from.isAfter(today))
        || (to != null && to.isAfter(today))
        || (from != null && to != null && from.isAfter(to))
        || (from != null && from.isBefore(today.minusDays(logs.retentionDays())))) {
      throw invalid("Date range is not allowed");
    }
    Instant fromInstant = from == null ? null : from.atStartOfDay(LOCAL).toInstant();
    Instant toExclusive = to == null ? null : to.plusDays(1).atStartOfDay(LOCAL).toInstant();
    return new AuditLogFilter(keyword, category, fromInstant, toExclusive);
  }

  private static String keyword(String q) {
    if (q == null) {
      return null;
    }
    String trimmed = q.trim();
    if (trimmed.length() > MAX_KEYWORD) {
      throw new InvalidRequestException(
          IdentityErrorCodes.AUDIT_SEARCH_TOO_LONG, "Search keyword is too long");
    }
    return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
  }

  private static AuditCategory category(String code) {
    if (code == null || code.isBlank() || "ALL".equals(code)) {
      return null;
    }
    try {
      return AuditCategory.valueOf(code);
    } catch (IllegalArgumentException unknown) {
      throw invalid("Unknown category");
    }
  }

  private static LocalDate date(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.trim());
    } catch (DateTimeParseException malformed) {
      throw invalid("Date must be yyyy-MM-dd");
    }
  }

  private static InvalidRequestException invalid(String message) {
    return new InvalidRequestException(IdentityErrorCodes.AUDIT_FILTER_INVALID, message);
  }
}
