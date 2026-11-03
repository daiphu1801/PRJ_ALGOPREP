package com.algoprep.judge.infrastructure.persistence;

import com.algoprep.judge.application.dto.AdminConfigFields;
import com.algoprep.judge.domain.model.AdminLanguageConfig;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Defaults;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Generator;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Language;
import com.algoprep.judge.domain.model.AdminLanguageConfig.Sandbox;
import com.algoprep.judge.domain.ports.out.AdminLanguageConfigRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
class JdbcAdminLanguageConfigRepository implements AdminLanguageConfigRepository {
  private final ObjectProvider<JdbcClient> jdbc;
  private final Map<String, String> labels;

  JdbcAdminLanguageConfigRepository(
      ObjectProvider<JdbcClient> jdbc,
      @Value("${algoprep.judge.compiler-display.java:Java (javac)}") String javaLabel,
      @Value("${algoprep.judge.compiler-display.cpp:C++ (g++)}") String cppLabel,
      @Value("${algoprep.judge.compiler-display.python:Python 3}") String pythonLabel) {
    this.jdbc = jdbc;
    labels = Map.of("JAVA", javaLabel, "CPP", cppLabel, "PYTHON", pythonLabel);
  }

  @Override
  public AdminLanguageConfig read() {
    return load(false);
  }

  @Override
  public AdminLanguageConfig lock() {
    return load(true);
  }

  private AdminLanguageConfig load(boolean locking) {
    String lock = locking ? " FOR UPDATE" : "";
    var languages =
        jdbc.getObject()
            .sql(
                "SELECT language,time_limit_multiplier,memory_limit_multiplier,"
                    + "enabled,updated_at FROM judge.language_configs ORDER BY language"
                    + lock)
            .query((rs, n) -> language(rs))
            .list();
    if (languages.size() != 3
        || languages.stream().anyMatch(row -> row.compilerDisplayName() == null)) {
      throw new IllegalStateException("Judge configuration unavailable");
    }
    var defaults =
        jdbc.getObject()
            .sql("SELECT * FROM judge.grading_defaults WHERE id=1" + lock)
            .query(
                (rs, n) ->
                    new Defaults(
                        rs.getInt("time_limit_ms"),
                        rs.getInt("memory_limit_mb"),
                        rs.getInt("output_limit_kb"),
                        rs.getInt("compile_timeout_sec"),
                        at(rs)))
            .single();
    var sandbox =
        jdbc.getObject()
            .sql("SELECT * FROM judge.sandbox_settings WHERE id=1" + lock)
            .query(
                (rs, n) ->
                    new Sandbox(
                        rs.getBoolean("network_access_enabled"),
                        rs.getBoolean("child_process_limit_enabled"),
                        rs.getBoolean("return_stderr_to_student"),
                        at(rs)))
            .single();
    var generator =
        jdbc.getObject()
            .sql("SELECT * FROM judge.generator_sandbox_settings WHERE id=1" + lock)
            .query(
                (rs, n) ->
                    new Generator(
                        rs.getInt("max_runtime_seconds"), rs.getInt("max_output_mb"), at(rs)))
            .single();
    return new AdminLanguageConfig(withPendingCounts(languages), defaults, sandbox, generator);
  }

  private Language language(ResultSet rs) throws SQLException {
    return new Language(
        rs.getString("language"),
        labels.get(rs.getString("language")),
        rs.getBigDecimal("time_limit_multiplier"),
        rs.getBigDecimal("memory_limit_multiplier"),
        rs.getBoolean("enabled"),
        0,
        at(rs));
  }

  private List<Language> withPendingCounts(List<Language> languages) {
    Map<String, Long> counts = new java.util.HashMap<>();
    jdbc.getObject()
        .sql(
            "SELECT language,count(*) AS jobs FROM judge.submissions WHERE status='PENDING' GROUP BY language")
        .query((rs, n) -> Map.entry(rs.getString("language"), rs.getLong("jobs")))
        .list()
        .forEach(row -> counts.put(row.getKey(), row.getValue()));
    return languages.stream()
        .map(
            row ->
                new Language(
                    row.language(),
                    row.compilerDisplayName(),
                    row.timeLimitMultiplier(),
                    row.memoryLimitMultiplier(),
                    row.enabled(),
                    counts.getOrDefault(row.language(), 0L),
                    row.updatedAt()))
        .toList();
  }

  private static Instant at(ResultSet rs) throws SQLException {
    return rs.getObject("updated_at", OffsetDateTime.class).toInstant();
  }

  @Override
  public void saveChanges(AdminLanguageConfig before, AdminLanguageConfig after, UUID actor) {
    var oldFields = AdminConfigFields.values(before);
    var newFields = AdminConfigFields.values(after);
    for (var row : after.languages()) {
      if (changed("languages." + row.language() + ".", oldFields, newFields)) {
        jdbc.getObject()
            .sql(
                "UPDATE judge.language_configs SET time_limit_multiplier=:time,"
                    + "memory_limit_multiplier=:memory,enabled=:enabled,updated_by=:actor,updated_at=clock_timestamp() WHERE language=:language")
            .param("time", row.timeLimitMultiplier())
            .param("memory", row.memoryLimitMultiplier())
            .param("enabled", row.enabled())
            .param("actor", actor)
            .param("language", row.language())
            .update();
      }
    }
    saveSingletons(after, actor, oldFields, newFields);
  }

  private void saveSingletons(
      AdminLanguageConfig after,
      UUID actor,
      Map<String, Object> oldFields,
      Map<String, Object> newFields) {
    if (changed("gradingDefaults.", oldFields, newFields)) {
      var row = after.gradingDefaults();
      jdbc.getObject()
          .sql(
              "UPDATE judge.grading_defaults SET time_limit_ms=:time,memory_limit_mb=:memory,"
                  + "output_limit_kb=:output,compile_timeout_sec=:compile,updated_by=:actor,updated_at=clock_timestamp() WHERE id=1")
          .param("time", row.timeLimitMs())
          .param("memory", row.memoryLimitMb())
          .param("output", row.maxOutputSizeKb())
          .param("compile", row.compileTimeoutSeconds())
          .param("actor", actor)
          .update();
    }
    if (changed("sandbox.", oldFields, newFields)) {
      var row = after.sandbox();
      jdbc.getObject()
          .sql(
              "UPDATE judge.sandbox_settings SET network_access_enabled=:network,"
                  + "child_process_limit_enabled=:child,return_stderr_to_student=:stderr,updated_by=:actor,updated_at=clock_timestamp() WHERE id=1")
          .param("network", row.networkAccessEnabled())
          .param("child", row.childProcessLimitEnabled())
          .param("stderr", row.returnStderrToStudent())
          .param("actor", actor)
          .update();
    }
    if (changed("generatorSandbox.", oldFields, newFields)) {
      var row = after.generatorSandbox();
      jdbc.getObject()
          .sql(
              "UPDATE judge.generator_sandbox_settings SET max_runtime_seconds=:runtime,"
                  + "max_output_mb=:output,updated_by=:actor,updated_at=clock_timestamp() WHERE id=1")
          .param("runtime", row.maxRuntimeSeconds())
          .param("output", row.maxOutputMb())
          .param("actor", actor)
          .update();
    }
  }

  private static boolean changed(
      String prefix, Map<String, Object> before, Map<String, Object> after) {
    return after.entrySet().stream()
        .anyMatch(
            row ->
                row.getKey().startsWith(prefix)
                    && !row.getValue().equals(before.get(row.getKey())));
  }
}
