package com.algoprep.identity.infrastructure.persistence;

import com.algoprep.identity.domain.model.OutboxEvent;
import com.algoprep.identity.domain.ports.out.IdentityOutboxRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JdbcIdentityOutboxRepository implements IdentityOutboxRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcIdentityOutboxRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<OutboxEvent> lockPending(int limit) {
    return jdbc.getObject()
        .sql(
            """
            SELECT id, event_type, routing_key, payload::text AS envelope, publish_attempts
            FROM identity.outbox_events WHERE published_at IS NULL
            ORDER BY created_at ASC, id ASC LIMIT :limit FOR UPDATE SKIP LOCKED
            """)
        .param("limit", limit)
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
            UPDATE identity.outbox_events
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
  @Transactional
  public int deletePublishedOlderThanDays(int days, int limit) {
    return jdbc.getObject()
        .sql(
            """
            DELETE FROM identity.outbox_events WHERE id IN (
              SELECT id FROM identity.outbox_events
              WHERE published_at IS NOT NULL
                AND published_at < now() - make_interval(days => :days)
              ORDER BY published_at, id LIMIT :limit)
            """)
        .param("days", days)
        .param("limit", limit)
        .update();
  }
}
