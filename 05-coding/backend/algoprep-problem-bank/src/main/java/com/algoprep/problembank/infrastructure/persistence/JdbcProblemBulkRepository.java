package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.ports.out.ProblemBulkRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProblemBulkRepository implements ProblemBulkRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcProblemBulkRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean levelExists(UUID levelId) {
    return exists("SELECT EXISTS(SELECT 1 FROM problem.problem_levels WHERE id = :id)", levelId);
  }

  @Override
  public boolean topicExists(UUID topicId) {
    return exists("SELECT EXISTS(SELECT 1 FROM problem.topics WHERE id = :id)", topicId);
  }

  @Override
  public void setLevel(UUID problemId, UUID actorId, UUID levelId) {
    jdbc.getObject()
        .sql(
            """
            UPDATE problem.problems
            SET level_id = :level, updated_by = :actor, updated_at = clock_timestamp()
            WHERE id = :id AND NOT deleted
            """)
        .param("level", levelId)
        .param("actor", actorId)
        .param("id", problemId)
        .update();
  }

  @Override
  public boolean addTopic(UUID problemId, UUID actorId, UUID topicId) {
    int inserted =
        jdbc.getObject()
            .sql(
                """
                INSERT INTO problem.problem_topics(problem_id, topic_id) VALUES (:problem, :topic)
                ON CONFLICT DO NOTHING
                """)
            .param("problem", problemId)
            .param("topic", topicId)
            .update();
    if (inserted == 1) {
      jdbc.getObject()
          .sql(
              "UPDATE problem.problems SET updated_by = :actor, updated_at = clock_timestamp()"
                  + " WHERE id = :id")
          .param("actor", actorId)
          .param("id", problemId)
          .update();
    }
    return inserted == 1;
  }

  @Override
  public Instant markDeleted(UUID problemId, UUID actorId) {
    return jdbc.getObject()
        .sql(
            """
            UPDATE problem.problems
            SET status = 'UNPUBLISHED', deleted = true, updated_by = :actor,
                updated_at = clock_timestamp()
            WHERE id = :id AND NOT deleted
            RETURNING updated_at
            """)
        .param("actor", actorId)
        .param("id", problemId)
        .query((rs, row) -> rs.getTimestamp("updated_at").toInstant())
        .single();
  }

  private boolean exists(String sql, UUID id) {
    return jdbc.getObject().sql(sql).param("id", id).query(Boolean.class).single();
  }
}
