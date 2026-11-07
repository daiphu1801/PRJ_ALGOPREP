package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.ProblemStatusWrite;
import com.algoprep.problembank.domain.ports.out.ProblemPublishRepository;
import java.sql.Timestamp;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** One guarded UPDATE for the status and one INSERT for the Outbox row. */
@Repository
class JdbcProblemPublishRepository implements ProblemPublishRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcProblemPublishRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public ProblemStatusWrite setStatus(UUID problemId, UUID actorId, String status) {
    return jdbc.getObject()
        .sql(
            """
            UPDATE problem.problems
            SET status = :status,
                published_at = CASE WHEN :status = 'PUBLISHED' THEN clock_timestamp()
                                    ELSE published_at END,
                updated_by = :actor,
                updated_at = clock_timestamp()
            WHERE id = :id AND NOT deleted
            RETURNING updated_at, published_at, current_testcase_set_version
            """)
        .param("status", status)
        .param("actor", actorId)
        .param("id", problemId)
        .query(
            (rs, row) -> {
              Timestamp published = rs.getTimestamp("published_at");
              return new ProblemStatusWrite(
                  rs.getTimestamp("updated_at").toInstant(),
                  published == null ? null : published.toInstant(),
                  rs.getInt("current_testcase_set_version"));
            })
        .single();
  }

  @Override
  public void appendEvent(UUID problemId, String eventType, String routingKey, String envelope) {
    jdbc.getObject()
        .sql(
            """
            INSERT INTO problem.outbox_events(id, aggregate_type, aggregate_id, event_type,
                                              routing_key, payload)
            VALUES (CAST(CAST(:envelope AS jsonb) ->> 'eventId' AS uuid), 'Problem', :problem,
                    :type, :key, CAST(:envelope AS jsonb))
            """)
        .param("envelope", envelope)
        .param("problem", problemId)
        .param("type", eventType)
        .param("key", routingKey)
        .update();
  }
}
