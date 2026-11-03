package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.domain.model.GeneratorSandboxLimits;
import com.algoprep.judge.domain.model.LanguageMultipliers;
import com.algoprep.judge.domain.ports.out.JudgeConfigRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcJudgeConfigRepository implements JudgeConfigRepository {
  private final ObjectProvider<JdbcClient> jdbc;

  JdbcJudgeConfigRepository(ObjectProvider<JdbcClient> jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public GeneratorSandboxLimits generatorLimits() {
    return jdbc.getObject()
        .sql(
            "SELECT max_runtime_seconds,max_output_mb FROM judge.generator_sandbox_settings WHERE id=1")
        .query(
            (rs, n) ->
                new GeneratorSandboxLimits(
                    rs.getInt("max_runtime_seconds"), rs.getInt("max_output_mb")))
        .single();
  }

  @Override
  public List<String> enabledLanguages() {
    return jdbc.getObject()
        .sql("SELECT language FROM judge.language_configs WHERE enabled=true ORDER BY language")
        .query(String.class)
        .list();
  }

  @Override
  public Optional<LanguageMultipliers> multipliers(String language) {
    return jdbc.getObject()
        .sql(
            "SELECT time_limit_multiplier,memory_limit_multiplier FROM judge.language_configs WHERE language=:language")
        .param("language", language)
        .query(
            (rs, n) ->
                new LanguageMultipliers(
                    rs.getBigDecimal("time_limit_multiplier"),
                    rs.getBigDecimal("memory_limit_multiplier")))
        .optional();
  }
}
