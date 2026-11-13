package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.GradingDefaults;
import com.algoprep.judge.domain.ports.out.GradingDefaultsRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** A schema-qualified, single-statement read; missing rows and DB errors propagate to caller. */
@Repository
public class JdbcGradingDefaultsRepository implements GradingDefaultsRepository {
  private final ObjectProvider<JdbcTemplate> jdbc;

  public JdbcGradingDefaultsRepository(ObjectProvider<JdbcTemplate> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public GradingDefaults read() {
    return jdbc.getObject()
        .queryForObject(
            """
        SELECT time_limit_ms, memory_limit_mb, output_limit_kb, compile_timeout_sec
        FROM judge.grading_defaults WHERE id = 1
        """,
            (row, index) ->
                new GradingDefaults(
                    row.getInt("time_limit_ms"),
                    row.getInt("memory_limit_mb"),
                    row.getInt("output_limit_kb"),
                    row.getInt("compile_timeout_sec")));
  }
}
