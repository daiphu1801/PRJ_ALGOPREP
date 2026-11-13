package com.algoprep.judge.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Safe operational projections; no payload or learner content enters this model. */
public final class InfraEvents {
  private InfraEvents() {}

  public record Summary(long warnCount, long errorCount, int windowHours) {}

  public record Event(Instant occurredAt, String level, String message, String service) {}

  public record Page(List<Event> events, String nextCursor, List<String> services) {
    public Page {
      events = List.copyOf(events);
      services = List.copyOf(services);
    }
  }

  public record Position(Instant occurredAt, String sourceKey) {}

  public record Filter(
      String q, String level, String service, Instant from, Instant until, Position cursor) {}

  public record Row(
      Position position,
      String level,
      String service,
      UUID submissionId,
      String kind,
      int attempts) {}
}
