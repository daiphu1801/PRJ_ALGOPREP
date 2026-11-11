package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.AiOutboxEvent;
import com.algoprep.aireview.domain.ports.out.AiOutboxRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAiOutboxRepository implements AiOutboxRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcAiOutboxRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void insert(AiOutboxEvent event, String aggregateType, UUID aggregateId) {
    jdbc.getObject()
        .sql(
            "INSERT INTO ai.outbox_events (id, aggregate_type, aggregate_id, event_type, payload,"
                + " created_at) VALUES (:id, :aggType, :aggId, :type, CAST(:payload AS jsonb),"
                + " :createdAt)")
        .param("id", event.id())
        .param("aggType", aggregateType)
        .param("aggId", aggregateId)
        .param("type", event.eventType())
        .param("payload", event.payload())
        .param("createdAt", OffsetDateTime.ofInstant(event.createdAt(), ZoneOffset.UTC))
        .update();
  }

  @Override
  public List<AiOutboxEvent> lockPending(int limit) {
    return jdbc.getObject()
        .sql(
            """
            SELECT id, event_type, payload::text AS payload, created_at, publish_attempts
            FROM ai.outbox_events WHERE published_at IS NULL
            ORDER BY created_at ASC, id ASC LIMIT :limit FOR UPDATE SKIP LOCKED
            """)
        .param("limit", limit)
        .query(
            (r, n) ->
                new AiOutboxEvent(
                    r.getObject("id", UUID.class),
                    r.getString("event_type"),
                    r.getString("payload"),
                    r.getObject("created_at", OffsetDateTime.class).toInstant(),
                    r.getInt("publish_attempts")))
        .list();
  }

  @Override
  public void attempted(UUID id, boolean confirmed) {
    jdbc.getObject()
        .sql(
            """
            UPDATE ai.outbox_events
            SET publish_attempts = CASE WHEN publish_attempts < 2147483647
                                        THEN publish_attempts + 1 ELSE publish_attempts END,
                published_at = CASE WHEN :confirmed THEN clock_timestamp() ELSE NULL END
            WHERE id = :id AND published_at IS NULL
            """)
        .param("id", id)
        .param("confirmed", confirmed)
        .update();
  }

  @Override
  public boolean tryCleanupLock() {
    return Boolean.TRUE.equals(
        jdbc.getObject()
            .sql("SELECT pg_try_advisory_xact_lock(hashtextextended('ai:outbox:cleanup', 0))")
            .query(Boolean.class)
            .single());
  }

  @Override
  public int deletePublishedOlderThanDays(int days, int limit) {
    return jdbc.getObject()
        .sql(
            """
            DELETE FROM ai.outbox_events WHERE id IN (
              SELECT id FROM ai.outbox_events
              WHERE published_at IS NOT NULL
                AND published_at < now() - make_interval(days => :days)
              ORDER BY published_at, id LIMIT :limit)
            """)
        .param("days", days)
        .param("limit", limit)
        .update();
  }
}
