package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.ProblemBankStatsModels.LevelRef;
import com.algoprep.problembank.domain.model.ProblemBankStatsModels.ProblemStat;
import com.algoprep.problembank.domain.ports.out.ProblemBankStatsRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcProblemBankStatsRepository implements ProblemBankStatsRepository {
  private static final RowMapper<ProblemStat> STAT =
      (rs, row) ->
          new ProblemStat(
              rs.getObject("id", UUID.class),
              rs.getString("title"),
              rs.getObject("level_id", UUID.class),
              rs.getString("code"),
              rs.getString("display_name"),
              rs.getBoolean("published"));

  private final ObjectProvider<JdbcClient> jdbc;

  JdbcProblemBankStatsRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<LevelRef> levels() {
    return jdbc.getObject()
        .sql(
            "SELECT id, code, display_name, sort_order FROM problem.problem_levels ORDER BY sort_order, code")
        .query(
            (rs, row) ->
                new LevelRef(
                    rs.getObject("id", UUID.class),
                    rs.getString("code"),
                    rs.getString("display_name"),
                    rs.getInt("sort_order")))
        .list();
  }

  @Override
  public List<ProblemStat> problems(Set<UUID> ids) {
    return jdbc.getObject()
        .sql(
            """
            SELECT p.id, p.title, l.id AS level_id, l.code, l.display_name,
                   (p.status = 'PUBLISHED' AND NOT p.deleted) AS published
            FROM problem.problems p JOIN problem.problem_levels l ON l.id = p.level_id
            WHERE p.id IN (:ids)
            ORDER BY p.code, p.id
            """)
        .param("ids", ids)
        .query(STAT)
        .list();
  }

  @Override
  public List<ProblemStat> published() {
    return jdbc.getObject()
        .sql(
            """
            SELECT p.id, p.title, l.id AS level_id, l.code, l.display_name, true AS published
            FROM problem.problems p JOIN problem.problem_levels l ON l.id = p.level_id
            WHERE p.status = 'PUBLISHED' AND NOT p.deleted
            ORDER BY p.code, p.id
            """)
        .query(STAT)
        .list();
  }
}
