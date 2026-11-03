package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.OutboxEvent;
import com.algoprep.judge.domain.ports.out.JudgeOutboxRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcJudgeOutboxRepository implements JudgeOutboxRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcJudgeOutboxRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<OutboxEvent> lockPending() {
    return jdbc.getObject()
        .sql(
            """
        SELECT id, event_type, payload::text AS envelope, publish_attempts
        FROM judge.outbox_events WHERE published_at IS NULL
        ORDER BY created_at ASC, id ASC LIMIT 100 FOR UPDATE SKIP LOCKED
        """)
        .query(
            (r, n) ->
                new OutboxEvent(
                    r.getObject("id", UUID.class),
                    r.getString("event_type"),
                    r.getString("envelope"),
                    r.getInt("publish_attempts")))
        .list();
  }

  @Override
  public void attempted(UUID id, boolean confirmed) {
    jdbc.getObject()
        .sql(
            """
        UPDATE judge.outbox_events
        SET publish_attempts = CASE WHEN publish_attempts < 2147483647
                                   THEN publish_attempts + 1 ELSE publish_attempts END,
            published_at = CASE WHEN :confirmed THEN clock_timestamp() ELSE NULL END
        WHERE id = :id AND published_at IS NULL
        """)
        .param("id", id)
        .param("confirmed", confirmed)
        .update();
  }
}
