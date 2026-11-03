package com.algoprep.judge.application.dto;

import com.algoprep.judge.domain.model.InfraEvents.Position;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Locale;
import java.util.Set;

/** Normalized filters and a strict, bounded keyset cursor for DD #20. */
public record InfraEventQuery(
    String q, String level, String service, Dates dates, Position cursor) {
  public record Dates(Instant from, Instant until) {}

  public static InfraEventQuery parse(
      String q, String level, String service, Dates dates, String cursor) {
    String text =
        q == null
            ? ""
            : q.substring(
                0, q.offsetByCodePoints(0, Math.min(100, q.codePointCount(0, q.length()))));
    return new InfraEventQuery(
        text,
        normalize(level, Set.of("error", "warn", "info")),
        service != null && Set.of("go-judge", "outbox").contains(service) ? service : "",
        dates,
        decode(cursor));
  }

  public static Dates dates(String from, String to) {
    var start = date(from);
    var end = date(to);
    if (start != null && end != null && start.isAfter(end)) {
      throw new IllegalArgumentException("Invalid infrastructure event date range");
    }
    var zone = ZoneOffset.ofHours(7);
    return new Dates(
        start == null ? null : start.atStartOfDay().toInstant(zone),
        end == null ? null : end.plusDays(1).atStartOfDay().toInstant(zone));
  }

  private static LocalDate date(String text) {
    if (text == null || text.isEmpty()) {
      return null;
    }
    if (!text.matches("(?!0000)[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
      throw new IllegalArgumentException("Invalid infrastructure event date");
    }
    return LocalDate.parse(text);
  }

  private static String normalize(String value, Set<String> accepted) {
    String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT);
    return accepted.contains(normalized) ? normalized : "";
  }

  public static String encode(Position position) {
    String value = position.occurredAt() + "|" + position.sourceKey();
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static Position decode(String cursor) {
    if (cursor == null || cursor.isEmpty()) {
      return null;
    }
    if (cursor.length() > 180 || !cursor.matches("[A-Za-z0-9_-]+")) {
      throw new IllegalArgumentException("Invalid infrastructure event cursor");
    }
    String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
    String[] parts = decoded.split("\\|", -1);
    if (parts.length != 2
        || !parts[1].matches("[os]:[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
      throw new IllegalArgumentException("Invalid infrastructure event cursor");
    }
    Instant time = Instant.parse(parts[0]);
    if (time.isBefore(Instant.parse("0001-01-01T00:00:00Z"))
        || time.isAfter(Instant.parse("9999-12-31T23:59:59.999999Z"))) {
      throw new IllegalArgumentException("Invalid infrastructure event cursor");
    }
    return new Position(time, parts[1]);
  }
}
