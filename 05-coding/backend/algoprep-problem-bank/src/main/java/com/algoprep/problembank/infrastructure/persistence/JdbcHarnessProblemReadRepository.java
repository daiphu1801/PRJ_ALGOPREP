package com.algoprep.problembank.infrastructure.persistence;

import com.algoprep.problembank.domain.model.ProblemSpecData;
import com.algoprep.problembank.domain.model.ProblemSpecReadView;
import com.algoprep.problembank.domain.ports.out.HarnessProblemReadRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcHarnessProblemReadRepository implements HarnessProblemReadRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcHarnessProblemReadRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public Optional<ProblemSpecReadView> findSpec(UUID problemId) {
    return jdbc.getObject()
        .sql(
            """
        SELECT s.*, p.function_wrapper_supported FROM problem.problem_specs s
        JOIN problem.problems p ON p.id=s.problem_id WHERE s.problem_id=:id
        """)
        .param("id", problemId)
        .query(
            (rs, row) ->
                new ProblemSpecReadView(
                    problemId,
                    rs.getInt("spec_version"),
                    new ProblemSpecData(
                        rs.getString("matching_strategy"), rs.getBigDecimal("epsilon_value"),
                        rs.getString("stdin_format_md"), rs.getString("stdout_format_md"),
                        rs.getString("function_name"), rs.getString("return_type"),
                        rs.getString("parameters"), rs.getString("name_overrides")),
                    rs.getBoolean("function_wrapper_supported")))
        .optional();
  }

  @Override
  public boolean canRead(UUID problemId, UUID callerUserId, String baseCategory) {
    return jdbc.getObject()
        .sql(
            """
        SELECT EXISTS(SELECT 1 FROM problem.problems WHERE id=:id AND NOT deleted
          AND (status='PUBLISHED' OR author_id=:caller OR :admin))
        """)
        .param("id", problemId)
        .param("caller", callerUserId)
        .param("admin", "ADMIN".equals(baseCategory))
        .query(Boolean.class)
        .single();
  }
}
