package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.SuggestedProblemModels.Candidate;
import com.algoprep.problembank.domain.ports.out.SuggestedProblemReadRepository;
import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * ponytail: loads every matching published problem and lets the handler rank them in memory, so the
 * ranking stays one testable function; move the score into SQL if the catalogue reaches the
 * thousands.
 */
@Repository
class JdbcSuggestedProblemReadRepository implements SuggestedProblemReadRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcSuggestedProblemReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<Candidate> candidates(String difficulty, UUID topic) {
    return jdbc.getObject()
        .sql(
            """
            SELECT p.id, p.code, p.title, pl.code AS level_code, pl.display_name, s.ac_rate,
                   p.function_wrapper_supported,
                   ARRAY(SELECT t.name FROM problem.problem_topics pt
                         JOIN problem.topics t ON t.id = pt.topic_id
                         WHERE pt.problem_id = p.id ORDER BY t.sort_order, t.name) AS topic_names,
                   ARRAY(SELECT CAST(pt.topic_id AS text) FROM problem.problem_topics pt
                         WHERE pt.problem_id = p.id) AS topic_ids
            FROM problem.problems p
            JOIN problem.problem_levels pl ON pl.id = p.level_id
            LEFT JOIN problem.problem_stats s ON s.problem_id = p.id
            WHERE p.status = 'PUBLISHED' AND NOT p.deleted
              AND (CAST(:difficulty AS text) IS NULL
                   OR NOT EXISTS (SELECT 1 FROM problem.problem_levels x WHERE x.code = :difficulty)
                   OR pl.code = :difficulty)
              AND (CAST(:topic AS uuid) IS NULL OR EXISTS (
                   SELECT 1 FROM problem.problem_topics pt
                   WHERE pt.problem_id = p.id AND pt.topic_id = CAST(:topic AS uuid)))
            """)
        .param("difficulty", difficulty)
        .param("topic", topic == null ? null : topic.toString())
        .query(
            (rs, row) ->
                new Candidate(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getString("title"),
                    rs.getString("level_code"),
                    rs.getString("display_name"),
                    strings(rs.getArray("topic_names")),
                    strings(rs.getArray("topic_ids")).stream().map(UUID::fromString).toList(),
                    rs.getBigDecimal("ac_rate"),
                    rs.getBoolean("function_wrapper_supported")))
        .list();
  }

  private static List<String> strings(Array array) throws SQLException {
    return array == null ? List.of() : Arrays.asList((String[]) array.getArray());
  }
}
