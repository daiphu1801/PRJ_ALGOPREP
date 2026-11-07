package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.PublishedProblemModels.SolveStates;
import com.algoprep.problembank.domain.model.StudentCatalogModels.CatalogSummary;
import com.algoprep.problembank.domain.model.StudentCatalogModels.LevelCount;
import com.algoprep.problembank.domain.model.StudentCatalogModels.LevelProgress;
import com.algoprep.problembank.domain.model.StudentCatalogModels.TopicCount;
import com.algoprep.problembank.domain.model.StudentCatalogModels.TopicProgress;
import com.algoprep.problembank.domain.ports.out.StudentCatalogReadRepository;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Every count joins problems on {@code PUBLISHED AND NOT deleted}; the join sits in the ON clause
 * so a topic or level with no visible problem still appears with a zero.
 */
@Repository
class JdbcStudentCatalogReadRepository implements StudentCatalogReadRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcStudentCatalogReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<TopicProgress> topicProgress(SolveStates states) {
    return jdbc.getObject()
        .sql(
            """
            SELECT t.id, t.name, t.sort_order, count(p.id) AS total,
                   count(p.id) FILTER (WHERE p.id = ANY(CAST(:solved AS uuid[]))) AS solved
            FROM problem.topics t
            LEFT JOIN problem.problem_topics pt ON pt.topic_id = t.id
            LEFT JOIN problem.problems p
              ON p.id = pt.problem_id AND p.status = 'PUBLISHED' AND NOT p.deleted
            GROUP BY t.id, t.name, t.sort_order
            ORDER BY t.sort_order, t.name
            """)
        .param("solved", literal(states))
        .query(
            (rs, row) ->
                new TopicProgress(
                    rs.getObject("id", UUID.class),
                    rs.getString("name"),
                    rs.getInt("sort_order"),
                    rs.getInt("total"),
                    states == null ? null : rs.getInt("solved")))
        .list();
  }

  @Override
  public List<LevelProgress> levelProgress(SolveStates states) {
    return jdbc.getObject()
        .sql(
            """
            SELECT l.code, l.display_name, l.sort_order, count(p.id) AS total,
                   count(p.id) FILTER (WHERE p.id = ANY(CAST(:solved AS uuid[]))) AS solved
            FROM problem.problem_levels l
            LEFT JOIN problem.problems p
              ON p.level_id = l.id AND p.status = 'PUBLISHED' AND NOT p.deleted
            GROUP BY l.id, l.code, l.display_name, l.sort_order
            ORDER BY l.sort_order, l.code
            """)
        .param("solved", literal(states))
        .query(
            (rs, row) ->
                new LevelProgress(
                    rs.getString("code"),
                    rs.getString("display_name"),
                    rs.getInt("sort_order"),
                    rs.getInt("total"),
                    rs.getInt("solved")))
        .list();
  }

  @Override
  public CatalogSummary summary() {
    int total =
        jdbc.getObject()
            .sql("SELECT count(*) FROM problem.problems WHERE status = 'PUBLISHED' AND NOT deleted")
            .query(Integer.class)
            .single();
    var byTopic =
        jdbc.getObject()
            .sql(
                """
                SELECT t.id, t.name, count(p.id) AS count
                FROM problem.topics t
                JOIN problem.problem_topics pt ON pt.topic_id = t.id
                JOIN problem.problems p
                  ON p.id = pt.problem_id AND p.status = 'PUBLISHED' AND NOT p.deleted
                GROUP BY t.id, t.name, t.sort_order
                ORDER BY t.sort_order, t.name
                """)
            .query(
                (rs, row) ->
                    new TopicCount(
                        rs.getObject("id", UUID.class), rs.getString("name"), rs.getInt("count")))
            .list();
    var byLevel =
        jdbc.getObject()
            .sql(
                """
                SELECT l.code, l.display_name, l.sort_order, count(p.id) AS count
                FROM problem.problem_levels l
                LEFT JOIN problem.problems p
                  ON p.level_id = l.id AND p.status = 'PUBLISHED' AND NOT p.deleted
                GROUP BY l.id, l.code, l.display_name, l.sort_order
                ORDER BY l.sort_order, l.code
                """)
            .query(
                (rs, row) ->
                    new LevelCount(
                        rs.getString("code"),
                        rs.getString("display_name"),
                        rs.getInt("sort_order"),
                        rs.getInt("count")))
            .list();
    return new CatalogSummary(total, byTopic, byLevel);
  }

  private static String literal(SolveStates states) {
    return states == null
        ? "{}"
        : states.solved().stream().map(UUID::toString).collect(Collectors.joining(",", "{", "}"));
  }
}
