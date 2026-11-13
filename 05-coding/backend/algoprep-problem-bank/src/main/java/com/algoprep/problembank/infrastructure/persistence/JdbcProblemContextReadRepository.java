package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.ProblemContextView;
import com.algoprep.problembank.domain.model.ProblemContextView.FunctionSignature;
import com.algoprep.problembank.domain.ports.out.ProblemContextReadRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** One SELECT; only the columns of {@code ProblemContext}, never testcases or solutions. */
@Repository
class JdbcProblemContextReadRepository implements ProblemContextReadRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcProblemContextReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<ProblemContextView> findVisible(UUID problemId, String language) {
    return jdbc.getObject()
        .sql(
            """
        SELECT p.title, p.statement_md, p.constraints_md, pl.display_name,
               p.known_optimal_complexity, p.ai_brief, p.no_full_code, p.socratic_only,
               s.function_name, s.return_type::text AS return_type, s.parameters::text AS parameters,
               CAST(s.name_overrides ->> CAST(:lang AS text) AS text) AS override_name,
               ARRAY(SELECT t.name FROM problem.problem_topics pt
                     JOIN problem.topics t ON t.id = pt.topic_id
                     WHERE pt.problem_id = p.id ORDER BY t.sort_order, t.name) AS topics
        FROM problem.problems p
        JOIN problem.problem_levels pl ON pl.id = p.level_id
        LEFT JOIN problem.problem_specs s ON s.problem_id = p.id
        WHERE p.id = :id AND p.status = 'PUBLISHED' AND NOT p.deleted
        """)
        .param("id", problemId)
        .param("lang", language)
        .query(
            (rs, row) -> {
              var name = rs.getString("function_name");
              var override = rs.getString("override_name");
              var signature =
                  name == null
                      ? null
                      : new FunctionSignature(
                          override == null || override.isBlank() ? name : override,
                          rs.getString("return_type"),
                          rs.getString("parameters"));
              return new ProblemContextView(
                  rs.getString("title"),
                  nullToEmpty(rs.getString("statement_md")),
                  rs.getString("constraints_md"),
                  List.of((String[]) rs.getArray("topics").getArray()),
                  rs.getString("display_name"),
                  signature,
                  rs.getString("known_optimal_complexity"),
                  rs.getString("ai_brief"),
                  rs.getBoolean("no_full_code"),
                  rs.getBoolean("socratic_only"));
            })
        .optional();
  }

  private static String nullToEmpty(String value) {
    return value == null ? "" : value;
  }
}
