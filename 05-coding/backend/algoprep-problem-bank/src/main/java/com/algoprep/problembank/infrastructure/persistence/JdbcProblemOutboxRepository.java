package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.OutboxEvent;
import com.algoprep.problembank.domain.ports.out.ProblemOutboxRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcProblemOutboxRepository implements ProblemOutboxRepository {
  // DEC-2026-1112-problem-outbox-retry-backoff: a failed row waits base * 2^(attempts-1), capped.
  static final int RETRY_BASE_SECONDS = 2;
  static final int RETRY_CAP_SECONDS = 300;

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcProblemOutboxRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<OutboxEvent> lockPending(int limit) {
    return jdbc.getObject()
        .sql(
            """
            SELECT id, event_type, routing_key, payload::text AS envelope, publish_attempts
            FROM problem.outbox_events WHERE published_at IS NULL
              AND (last_attempted_at IS NULL OR last_attempted_at
                   + LEAST(:capSeconds, :baseSeconds * power(2, LEAST(GREATEST(publish_attempts - 1, 0), 20)))
                     * interval '1 second' <= clock_timestamp())
            ORDER BY COALESCE(last_attempted_at, created_at), created_at, id
            LIMIT :limit FOR UPDATE SKIP LOCKED
            """)
        .param("limit", limit)
        .param("baseSeconds", RETRY_BASE_SECONDS)
        .param("capSeconds", RETRY_CAP_SECONDS)
        .query(
            (r, n) ->
                new OutboxEvent(
                    r.getObject("id", UUID.class),
                    r.getString("event_type"),
                    r.getString("routing_key"),
                    r.getString("envelope"),
                    r.getInt("publish_attempts")))
        .list();
  }

  @Override
  public void attempted(UUID id, boolean confirmed) {
    jdbc.getObject()
        .sql(
            """
            UPDATE problem.outbox_events
            SET publish_attempts = CASE WHEN publish_attempts < 2147483647
                                        THEN publish_attempts + 1 ELSE publish_attempts END,
                published_at = CASE WHEN :confirmed THEN clock_timestamp() ELSE NULL END,
                last_attempted_at = clock_timestamp()
            WHERE id = :id AND published_at IS NULL
            """)
        .param("id", id)
        .param("confirmed", confirmed)
        .update();
  }

  @Override
  @Transactional
  public int deletePublishedOlderThanDays(int days, int limit) {
    return jdbc.getObject()
        .sql(
            """
            DELETE FROM problem.outbox_events WHERE id IN (
              SELECT id FROM problem.outbox_events
              WHERE published_at IS NOT NULL
                AND published_at < now() - make_interval(days => :days)
              ORDER BY published_at, id LIMIT :limit)
            """)
        .param("days", days)
        .param("limit", limit)
        .update();
  }
}
