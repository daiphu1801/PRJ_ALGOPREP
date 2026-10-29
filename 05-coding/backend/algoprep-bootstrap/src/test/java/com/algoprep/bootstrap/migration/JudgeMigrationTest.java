package com.algoprep.bootstrap.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Applies the real Flyway migrations of module {@code judge-orchestration} (V4.001, V4.002) to a
 * clean PostgreSQL 16 and checks tables, seed data and constraints against {@code
 * 02-bd/database/F4_judge-orchestration.md} (tables 1.1 to 1.10 and the DDL contract in section 8).
 *
 * <p>Every Spring context in this module runs all module migrations, so the assertions here only
 * look at schema {@code judge}. One test applies the F4 migrations alone to an empty database to
 * prove F4 does not depend on any other schema.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,"
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=2"
    })
@ActiveProfiles("test")
@Testcontainers
class JudgeMigrationTest {

  private static final List<String> EXPECTED_TABLES =
      List.of(
          "language_configs",
          "grading_defaults",
          "sandbox_settings",
          "generator_sandbox_settings",
          "cluster_settings",
          "submissions",
          "judge_run_tokens",
          "submission_testcase_results",
          "callback_logs",
          "outbox_events");

  /** Tables that must hold no rows right after a clean install (BD 8: seed is configuration). */
  private static final List<String> EMPTY_AT_START =
      List.of(
          "submissions",
          "judge_run_tokens",
          "submission_testcase_results",
          "callback_logs",
          "outbox_events");

  private static final String INSERT_SUBMISSION =
      "INSERT INTO judge.submissions (id, user_id, problem_id, language, submission_model,"
          + " source_code, source_input_method, status, testcase_set_version_at_grading,"
          + " failure_reason, finished_at) VALUES (?, ?, ?, 'JAVA', 'FUNCTION_WRAPPER', ?,"
          + " 'EDITOR', ?, ?, ?, ?)";

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Autowired private JdbcTemplate jdbc;

  @Autowired private Flyway flyway;

  @Test
  @DisplayName("creates exactly the 10 judge tables from BD section 4")
  void createsAllTables() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'judge'",
            String.class);
    assertThat(tables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
  }

  @Test
  @DisplayName("has no foreign key that leaves schema judge")
  void noCrossSchemaForeignKeys() {
    Integer crossing =
        jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.table_constraints tc"
                + " JOIN information_schema.constraint_column_usage ccu"
                + "   ON ccu.constraint_name = tc.constraint_name"
                + "  AND ccu.constraint_schema = tc.constraint_schema"
                + " WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'judge'"
                + "   AND ccu.table_schema <> 'judge'",
            Integer.class);
    assertThat(crossing).isZero();
  }

  @Test
  @DisplayName("running Flyway again changes nothing and does not undo admin edits to seed rows")
  void adminEditsSurviveNextMigrate() {
    jdbc.update(
        "UPDATE judge.language_configs SET time_limit_multiplier = 4.00 WHERE language = 'PYTHON'");
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    flyway.validate();
    assertThat(
            jdbc.queryForObject(
                "SELECT time_limit_multiplier FROM judge.language_configs WHERE language = 'PYTHON'",
                BigDecimal.class))
        .isEqualByComparingTo("4.00");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM public.flyway_schema_history"
                    + " WHERE version IN ('4.001','4.002') AND success",
                Integer.class))
        .isEqualTo(2);
  }

  @Test
  @DisplayName("clean install on its own: seeds 3 languages and 4 singleton rows, all else empty")
  void cleanInstallSeedsConfigurationOnly() {
    jdbc.execute("CREATE DATABASE f4_isolated");
    DriverManagerDataSource isolated =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl().replace("/algoprep", "/f4_isolated"),
            POSTGRES.getUsername(),
            POSTGRES.getPassword());
    JdbcTemplate clean = new JdbcTemplate(isolated);
    clean.execute("CREATE SCHEMA judge");
    var result =
        Flyway.configure()
            .dataSource(isolated)
            .locations("classpath:db/migration/judge")
            .defaultSchema("public")
            .load()
            .migrate();
    assertThat(result.success).isTrue();
    assertThat(result.migrationsExecuted).isEqualTo(2);
    assertSeededConfiguration(clean);
    for (String empty : EMPTY_AT_START) {
      assertThat(count(clean, "judge." + empty)).as(empty).isZero();
    }
  }

  private static void assertSeededConfiguration(JdbcTemplate clean) {
    assertThat(
            clean.queryForList(
                "SELECT language || ':' || time_limit_multiplier || ':' || memory_limit_multiplier"
                    + " FROM judge.language_configs ORDER BY language",
                String.class))
        .containsExactly("CPP:1.00:1.00", "JAVA:2.00:2.00", "PYTHON:3.00:1.00");
    assertThat(
            clean.queryForObject(
                "SELECT count(*) FROM judge.language_configs WHERE enabled", Integer.class))
        .isEqualTo(3);
    for (String single :
        List.of(
            "grading_defaults",
            "sandbox_settings",
            "generator_sandbox_settings",
            "cluster_settings")) {
      assertThat(count(clean, "judge." + single)).as(single).isEqualTo(1);
    }
    assertThat(
            clean.queryForObject(
                "SELECT time_limit_ms || '/' || memory_limit_mb || '/' || output_limit_kb || '/'"
                    + " || compile_timeout_sec FROM judge.grading_defaults",
                String.class))
        .isEqualTo("1000/256/64/10");
    assertThat(
            clean.queryForObject(
                "SELECT autoscale_job_threshold || '/' || jobs_per_page || '/' || error_retention_hours"
                    + " || '/' || latency_cap_seconds || '/' || queue_paused"
                    + " FROM judge.cluster_settings",
                String.class))
        .isEqualTo("20/20/24/10/false");
    assertThat(
            count(clean, "judge.language_configs WHERE updated_by IS NOT NULL")
                + count(clean, "judge.cluster_settings WHERE updated_by IS NOT NULL"))
        .as("seed rows have no acting user")
        .isZero();
  }

  @Test
  @DisplayName(
      "singleton tables accept only id = 1; language set and multiplier range are enforced")
  void configurationRules() {
    assertThatThrownBy(() -> jdbc.update("INSERT INTO judge.cluster_settings (id) VALUES (2)"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> jdbc.update("UPDATE judge.cluster_settings SET autoscale_job_threshold = 0"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE judge.generator_sandbox_settings SET max_runtime_seconds = 301"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO judge.language_configs (language, time_limit_multiplier)"
                        + " VALUES ('RUST', 1.00)"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE judge.language_configs SET memory_limit_multiplier = 0.05"
                        + " WHERE language = 'CPP'"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName(
      "submission: status, failure_reason and finished_at must agree; pin version required")
  void submissionStateRules() {
    insert("PENDING", null, false, 1, "ok");
    insert("SYSTEM_ERROR", "WORKER_TIMEOUT", true, 1, "ok");
    insert("ACCEPTED", null, true, 1, "ok");
    assertThatThrownBy(() -> insert("SYSTEM_ERROR", null, true, 1, "ok"))
        .as("SYSTEM_ERROR needs a failure_reason")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insert("WRONG_ANSWER", "SANDBOX_FAULT", true, 1, "ok"))
        .as("failure_reason only with SYSTEM_ERROR")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insert("ACCEPTED", null, false, 1, "ok"))
        .as("terminal status needs finished_at")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insert("JUDGING", null, true, 1, "ok"))
        .as("running status has no finished_at")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insert("PENDING", null, false, 0, "ok"))
        .as("pinned testcase version must be positive")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insert("PENDING", null, false, null, "ok"))
        .as("pinned testcase version is NOT NULL")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insert("QUEUED", null, false, 1, "ok"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("submission: source is capped at 65536 UTF-8 bytes; counts and score ranges hold")
  void submissionValueRules() {
    insert("PENDING", null, false, 1, "x".repeat(65536));
    insert("PENDING", null, false, 1, "é".repeat(32768));
    assertThatThrownBy(() -> insert("PENDING", null, false, 1, "x".repeat(65537)))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insert("PENDING", null, false, 1, "é".repeat(32769)))
        .as("limit is bytes, not characters")
        .isInstanceOf(DataIntegrityViolationException.class);
    UUID id = insert("JUDGING", null, false, 1, "ok");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE judge.submissions SET total_testcase_count = 3, passed_testcase_count = 4"
                        + " WHERE id = ?",
                    id))
        .as("passed cannot exceed total")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> jdbc.update("UPDATE judge.submissions SET score_ratio = 1.500 WHERE id = ?", id))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE judge.submissions SET compile_error_message = 'x' WHERE id = ?", id))
        .as("compile message only with COMPILE_ERROR")
        .isInstanceOf(DataIntegrityViolationException.class);
    jdbc.update(
        "UPDATE judge.submissions SET status = 'SYSTEM_ERROR', failure_reason = 'SANDBOX_FAULT',"
            + " finished_at = now(), total_testcase_count = 5, passed_testcase_count = 2"
            + " WHERE id = ?",
        id);
  }

  @Test
  @DisplayName(
      "results: one per testcase, one consumed token per testcase, snippets only for samples")
  void resultRules() {
    UUID submission = insert("JUDGING", null, false, 1, "ok");
    UUID testcase = UUID.randomUUID();
    UUID token1 = token(submission, testcase, true);
    result(submission, testcase, token1, "WA", true);
    assertThatThrownBy(
            () -> result(submission, testcase, token(submission, testcase, false), "AC", true))
        .as("a second result for the same testcase")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> token(submission, testcase, true))
        .as("a second consumed token for the same testcase")
        .isInstanceOf(DataIntegrityViolationException.class);
    token(submission, testcase, false);
    token(submission, testcase, false);

    UUID other = UUID.randomUUID();
    UUID otherToken = token(submission, other, true);
    assertThatThrownBy(() -> result(submission, other, otherToken, "TIMEOUT", true))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> snippets(submission, other, otherToken, "AC", "err"))
        .as("stderr only for a failing sample")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO judge.submission_testcase_results (id, submission_id, testcase_id,"
                        + " is_sample, testcase_order, verdict, runtime_ms, memory_kb,"
                        + " actual_output_snippet, judge_run_token_id)"
                        + " VALUES (?, ?, ?, false, 1, 'WA', 1, 1, 'leak', ?)",
                    UUID.randomUUID(),
                    submission,
                    other,
                    otherToken))
        .as("a hidden testcase never stores output")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName(
      "outbox: only Submission aggregates and the two event types; callback outcome is closed")
  void outboxAndCallbackRules() {
    UUID aggregate = UUID.randomUUID();
    outbox("Submission", "SubmissionGraded", aggregate);
    outbox("Submission", "SubmissionAccepted", aggregate);
    assertThatThrownBy(() -> outbox("Problem", "SubmissionGraded", aggregate))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> outbox("Submission", "SubmissionDeleted", aggregate))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("UPDATE judge.outbox_events SET publish_attempts = -1"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM judge.outbox_events WHERE published_at IS NULL"
                    + " AND aggregate_id = ?",
                Integer.class,
                aggregate))
        .isEqualTo(2);
    jdbc.update(
        "INSERT INTO judge.callback_logs (id, raw_payload, outcome) VALUES (?, '{}'::jsonb,"
            + " 'INVALID_TOKEN')",
        UUID.randomUUID());
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO judge.callback_logs (id, raw_payload, outcome) VALUES (?,"
                        + " '{}'::jsonb, 'UNKNOWN')",
                    UUID.randomUUID()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("a submission that has tokens or results can never be deleted (NO ACTION)")
  void submissionsAreNeverDeleted() {
    UUID submission = insert("JUDGING", null, false, 1, "ok");
    token(submission, UUID.randomUUID(), false);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM judge.submissions WHERE id = ?", submission))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private UUID insert(
      String status, String failureReason, boolean finished, Integer version, String source) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        INSERT_SUBMISSION,
        id,
        UUID.randomUUID(),
        UUID.randomUUID(),
        source,
        status,
        version,
        failureReason,
        finished ? java.sql.Timestamp.from(java.time.Instant.now()) : null);
    return id;
  }

  private UUID token(UUID submission, UUID testcase, boolean consumed) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO judge.judge_run_tokens (id, submission_id, testcase_id, token, consumed_at)"
            + " VALUES (?, ?, ?, ?, CASE WHEN ? THEN now() END)",
        id,
        submission,
        testcase,
        UUID.randomUUID(),
        consumed);
    return id;
  }

  private void result(UUID submission, UUID testcase, UUID token, String verdict, boolean sample) {
    jdbc.update(
        "INSERT INTO judge.submission_testcase_results (id, submission_id, testcase_id, is_sample,"
            + " testcase_order, verdict, runtime_ms, memory_kb, judge_run_token_id)"
            + " VALUES (?, ?, ?, ?, 1, ?, 10, 1024, ?)",
        UUID.randomUUID(),
        submission,
        testcase,
        sample,
        verdict,
        token);
  }

  private void snippets(UUID submission, UUID testcase, UUID token, String verdict, String stderr) {
    jdbc.update(
        "INSERT INTO judge.submission_testcase_results (id, submission_id, testcase_id, is_sample,"
            + " testcase_order, verdict, runtime_ms, memory_kb, stderr_snippet, judge_run_token_id)"
            + " VALUES (?, ?, ?, true, 1, ?, 10, 1024, ?, ?)",
        UUID.randomUUID(),
        submission,
        testcase,
        verdict,
        stderr,
        token);
  }

  private void outbox(String aggregateType, String eventType, UUID aggregate) {
    jdbc.update(
        "INSERT INTO judge.outbox_events (id, aggregate_type, aggregate_id, event_type, payload)"
            + " VALUES (?, ?, ?, ?, '{}'::jsonb)",
        UUID.randomUUID(),
        aggregateType,
        aggregate,
        eventType);
  }

  private static int count(JdbcTemplate template, String tableAndFilter) {
    return template.queryForObject("SELECT count(*) FROM " + tableAndFilter, Integer.class);
  }
}
