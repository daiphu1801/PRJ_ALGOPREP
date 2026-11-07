package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.ProblemStatsModels.ProblemStatsRow;
import com.algoprep.problembank.domain.ports.out.ProblemStatsRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProblemStatsRepository implements ProblemStatsRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcProblemStatsRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean markConsumed(UUID eventId) {
    return jdbc.getObject()
            .sql(
                """
                INSERT INTO problem.consumed_events(event_id, consumed_at)
                VALUES (:id, now()) ON CONFLICT (event_id) DO NOTHING
                """)
            .param("id", eventId)
            .update()
        == 1;
  }

  @Override
  public boolean addSubmission(UUID problemId, boolean accepted) {
    // The rate is written in the same statement as both counters (BD database F2 section 1.9).
    return jdbc.getObject()
            .sql(
                """
                INSERT INTO problem.problem_stats(
                    problem_id, submission_count, accepted_count, ac_rate, updated_at)
                SELECT p.id, 1, :acc, :acc * 100.0, now()
                FROM problem.problems p WHERE p.id = :problemId
                ON CONFLICT (problem_id) DO UPDATE SET
                    submission_count = problem_stats.submission_count + 1,
                    accepted_count = problem_stats.accepted_count + :acc,
                    ac_rate = round((problem_stats.accepted_count + :acc) * 100.0
                                    / (problem_stats.submission_count + 1), 2),
                    updated_at = now()
                """)
            .param("problemId", problemId)
            .param("acc", accepted ? 1 : 0)
            .update()
        == 1;
  }

  @Override
  public Optional<ProblemStatsRow> find(UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
            SELECT p.author_id, p.status, COALESCE(s.submission_count, 0) AS submission_count,
                   s.ac_rate, s.updated_at
            FROM problem.problems p
            LEFT JOIN problem.problem_stats s ON s.problem_id = p.id
            WHERE p.id = :id AND NOT p.deleted
            """)
        .param("id", problemId)
        .query(
            (rs, row) ->
                new ProblemStatsRow(
                    rs.getObject("author_id", UUID.class),
                    rs.getString("status"),
                    rs.getInt("submission_count"),
                    rs.getBigDecimal("ac_rate"),
                    rs.getTimestamp("updated_at") == null
                        ? null
                        : rs.getTimestamp("updated_at").toInstant()))
        .optional();
  }
}
