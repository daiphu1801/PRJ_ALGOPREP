package com.algoprep.bootstrap.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * Applies the real Flyway migrations of module {@code ai-review} (V5.001, V5.002) to a clean
 * PostgreSQL 16 and checks tables, seed data and constraints against {@code
 * 02-bd/database/F5_ai-review.md} (tables 1.1 to 1.10 and the DDL contract in section 9).
 *
 * <p>Every Spring context in this module runs all module migrations, so the assertions here only
 * look at schema {@code ai}. One test applies the F5 migrations alone to an empty database to prove
 * F5 does not depend on any other schema.
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
class AiMigrationTest {

  private static final List<String> EXPECTED_TABLES =
      List.of(
          "prompt_templates",
          "ai_token_budget_configs",
          "ai_model_prices",
          "solution_reviews",
          "rubric_scores",
          "interview_sessions",
          "interview_turns",
          "token_usage",
          "ai_cache_entries",
          "ai_usage_anomaly_alerts",
          "outbox_events");

  /** Tables that must hold no rows right after a clean install (BD 9: seed is configuration). */
  private static final List<String> EMPTY_AT_START =
      List.of(
          "ai_model_prices",
          "solution_reviews",
          "rubric_scores",
          "interview_sessions",
          "interview_turns",
          "token_usage",
          "ai_cache_entries",
          "ai_usage_anomaly_alerts",
          "outbox_events");

  private static final String HASH_A = "a".repeat(64);

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
  @DisplayName("creates exactly the 11 ai tables from BD section 5")
  void createsAllTables() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'ai'",
            String.class);
    assertThat(tables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
  }

  @Test
  @DisplayName("has no foreign key that leaves schema ai")
  void noCrossSchemaForeignKeys() {
    Integer crossing =
        jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.table_constraints tc"
                + " JOIN information_schema.constraint_column_usage ccu"
                + "   ON ccu.constraint_name = tc.constraint_name"
                + "  AND ccu.constraint_schema = tc.constraint_schema"
                + " WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'ai'"
                + "   AND ccu.table_schema <> 'ai'",
            Integer.class);
    assertThat(crossing).isZero();
  }

  @Test
  @DisplayName("running Flyway again changes nothing and does not undo admin edits to seed rows")
  void adminEditsSurviveNextMigrate() {
    jdbc.update("UPDATE ai.ai_token_budget_configs SET limit_tokens = 5000000 WHERE id = 1");
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    flyway.validate();
    assertThat(
            jdbc.queryForObject(
                "SELECT limit_tokens FROM ai.ai_token_budget_configs WHERE id = 1", Long.class))
        .isEqualTo(5_000_000L);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM public.flyway_schema_history"
                    + " WHERE version IN ('5.001','5.002') AND success",
                Integer.class))
        .isEqualTo(2);
  }

  @Test
  @DisplayName("clean install on its own: seeds 3 ACTIVE prompts and 1 budget row, all else empty")
  void cleanInstallSeedsConfigurationOnly() {
    jdbc.execute("CREATE DATABASE f5_isolated");
    DriverManagerDataSource isolated =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl().replace("/algoprep", "/f5_isolated"),
            POSTGRES.getUsername(),
            POSTGRES.getPassword());
    JdbcTemplate clean = new JdbcTemplate(isolated);
    clean.execute("CREATE SCHEMA ai");
    var result =
        Flyway.configure()
            .dataSource(isolated)
            .locations("classpath:db/migration/ai")
            .defaultSchema("public")
            .load()
            .migrate();
    assertThat(result.success).isTrue();
    assertThat(result.migrationsExecuted).isEqualTo(2);
    assertSeededConfiguration(clean);
    for (String empty : EMPTY_AT_START) {
      assertThat(count(clean, "ai." + empty)).as(empty).isZero();
    }
  }

  @Test
  @DisplayName("prompt templates: one ACTIVE and one DRAFT per feature, guards only where allowed")
  void promptRules() {
    assertThatThrownBy(() -> prompt("SOLUTION_REVIEW", "v9.0", "ACTIVE", "{}", true))
        .as("a second ACTIVE for the same feature")
        .isInstanceOf(DataIntegrityViolationException.class);
    prompt("SOLUTION_REVIEW", "v9.1", "DRAFT", "{}", false);
    assertThatThrownBy(() -> prompt("SOLUTION_REVIEW", "v9.2", "DRAFT", "{}", false))
        .as("a second DRAFT for the same feature")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> prompt("SOLUTION_REVIEW", "v1.0", "ARCHIVED", "{}", true))
        .as("version unique per feature")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> prompt("GENERATE_TESTCASE", "v9.3", "ARCHIVED", "{}", true))
        .as("GENERATE_TESTCASE has no guards")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> prompt("MOCK_INTERVIEW", "v9.4", "ARCHIVED", null, true))
        .as("the other features must have guards")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> prompt("MOCK_INTERVIEW", "v9.5", "DRAFT", "{}", true))
        .as("a DRAFT has no published_at")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> prompt("MOCK_INTERVIEW", "v9.6", "ARCHIVED", "{}", false))
        .as("a published prompt has published_at")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("interview session fields follow entry_type; hints need hint_allowed")
  void sessionEntryRules() {
    session("SUBMISSION", "submission_id", UUID.randomUUID());
    insertTopicSession();
    insertQuestionSession();
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO ai.interview_sessions (id, user_id, entry_type, submission_id,"
                        + " interviewer_level, stage) VALUES (?, ?, 'SUBMISSION', ?, 'JUNIOR',"
                        + " 'EXPLAIN')",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID()))
        .as("SUBMISSION entry has no F5-28 parameters")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO ai.interview_sessions (id, user_id, entry_type, topics, stage)"
                        + " VALUES (?, ?, 'TOPIC_SELECTION', '[\"DP\"]'::jsonb, 'EXPLAIN')",
                    UUID.randomUUID(),
                    UUID.randomUUID()))
        .as("self-practice entry needs level, max_turns and hint_allowed")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO ai.interview_sessions (id, user_id, entry_type, question_ref,"
                        + " interviewer_level, max_turns, hint_allowed, stage) VALUES (?, ?,"
                        + " 'QUESTION_BANK', 'q1', 'JUNIOR', 8, true, 'EXPLAIN')",
                    UUID.randomUUID(),
                    UUID.randomUUID()))
        .as("QUESTION_BANK keeps the original question text")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("session counters, stage and ended_at must agree")
  void sessionStateRules() {
    UUID id = insertQuestionSession();
    assertThatThrownBy(
            () -> jdbc.update("UPDATE ai.interview_sessions SET turn_count = 9 WHERE id = ?", id))
        .as("turn_count cannot pass max_turns")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE ai.interview_sessions SET stage = 'COMPLETED' WHERE id = ?", id))
        .as("a closed session needs ended_at")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> jdbc.update("UPDATE ai.interview_sessions SET ended_at = now() WHERE id = ?", id))
        .as("a running session has no ended_at")
        .isInstanceOf(DataIntegrityViolationException.class);
    jdbc.update("UPDATE ai.interview_sessions SET hint_count = 2 WHERE id = ?", id);
    jdbc.update(
        "UPDATE ai.interview_sessions SET stage = 'ABANDONED', ended_at = now() WHERE id = ?", id);
    UUID noHint = insertTopicSession();
    assertThatThrownBy(
            () ->
                jdbc.update("UPDATE ai.interview_sessions SET hint_count = 1 WHERE id = ?", noHint))
        .as("hints are only counted when hint_allowed")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("transcript: unique index per session; rubric: score 0..10, four fixed criteria")
  void transcriptAndRubricRules() {
    UUID session = insertQuestionSession();
    turn(session, 1, "AI", "EXPLAIN");
    turn(session, 2, "USER", "EXPLAIN");
    assertThatThrownBy(() -> turn(session, 2, "AI", "EXPLAIN"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> turn(session, 0, "AI", "EXPLAIN"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> turn(session, 3, "AI", "COMPLETED"))
        .isInstanceOf(DataIntegrityViolationException.class);
    rubric(session, "CLARITY", "10.00");
    assertThatThrownBy(() -> rubric(session, "CLARITY", "5.00"))
        .as("each criterion is scored once")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> rubric(session, "SPEED", "5.00"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> rubric(session, "TECHNICAL_ACCURACY", "10.01"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("review and cache: one review per submission, hash shape, cache key unique")
  void reviewAndCacheRules() {
    UUID template = activePrompt("SOLUTION_REVIEW");
    UUID problem = UUID.randomUUID();
    UUID review = review(UUID.randomUUID(), problem, template, HASH_A, 3);
    assertThatThrownBy(() -> review(UUID.randomUUID(), problem, template, "XYZ", 3))
        .as("source_hash is 64 lowercase hex characters")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> review(UUID.randomUUID(), problem, template, HASH_A, 6))
        .as("readability is 1..5")
        .isInstanceOf(DataIntegrityViolationException.class);
    cache(problem, template, review);
    assertThatThrownBy(() -> cache(problem, template, review))
        .as("one cache entry per (problem, language, hash, prompt)")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> review(UUID.randomUUID(), problem, UUID.randomUUID(), HASH_A, 3))
        .as("prompt_template_id must exist")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("token usage: call_id makes a retried write idempotent; problem_id follows feature")
  void tokenUsageRules() {
    UUID call = UUID.randomUUID();
    UUID problem = UUID.randomUUID();
    usage(call, "SOLUTION_REVIEW", problem, UUID.randomUUID());
    assertThatThrownBy(() -> usage(call, "SOLUTION_REVIEW", problem, UUID.randomUUID()))
        .as("the same provider call is never counted twice")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(
            jdbc.update(
                "INSERT INTO ai.token_usage (id, call_id, user_id, feature_code, problem_id,"
                    + " owner_id, tokens_prompt, tokens_completion, model_name)"
                    + " VALUES (?, ?, ?, 'SOLUTION_REVIEW', ?, ?, 1, 1, 'default')"
                    + " ON CONFLICT (call_id) DO NOTHING",
                UUID.randomUUID(),
                call,
                UUID.randomUUID(),
                problem,
                UUID.randomUUID()))
        .as("ON CONFLICT DO NOTHING is the retry path")
        .isZero();
    usage(UUID.randomUUID(), "GENERATE_TESTCASE", problem, problem);
    usage(UUID.randomUUID(), "MOCK_INTERVIEW", null, UUID.randomUUID());
    assertThatThrownBy(
            () -> usage(UUID.randomUUID(), "GENERATE_TESTCASE", problem, UUID.randomUUID()))
        .as("GENERATE_TESTCASE owner is the problem")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> usage(UUID.randomUUID(), "MOCK_INTERVIEW", problem, UUID.randomUUID()))
        .as("a mock interview has no problem_id")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> usage(UUID.randomUUID(), "SOLUTION_REVIEW", null, UUID.randomUUID()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("budget singleton, model price case-insensitive unique, alert and outbox rules")
  void configurationAndAlertRules() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO ai.ai_token_budget_configs (id, period_start) VALUES (2, now())"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("UPDATE ai.ai_token_budget_configs SET warn_percent = 0"))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("UPDATE ai.ai_token_budget_configs SET limit_tokens = -1"))
        .isInstanceOf(DataIntegrityViolationException.class);
    jdbc.update(
        "INSERT INTO ai.ai_model_prices (id, model_name) VALUES (?, 'Model-X')", UUID.randomUUID());
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO ai.ai_model_prices (id, model_name) VALUES (?, 'model-x')",
                    UUID.randomUUID()))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> alert("REVIEWED", false))
        .as("REVIEWED needs reviewed_at")
        .isInstanceOf(DataIntegrityViolationException.class);
    alert("OPEN", false);
    alert("REVIEWED", true);
    outbox("SolutionReview", "SolutionReviewCompleted");
    outbox("InterviewSession", "MockInterviewFinished");
    assertThatThrownBy(() -> outbox("SolutionReview", "MockInterviewFinished"))
        .as("event type must match its aggregate")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private static void assertSeededConfiguration(JdbcTemplate clean) {
    assertThat(
            clean.queryForList(
                "SELECT feature_code || ':' || version || ':' || status || ':' || model_id"
                    + " FROM ai.prompt_templates ORDER BY feature_code",
                String.class))
        .containsExactly(
            "GENERATE_TESTCASE:v1.0:ACTIVE:default",
            "MOCK_INTERVIEW:v1.0:ACTIVE:default",
            "SOLUTION_REVIEW:v1.0:ACTIVE:default");
    assertThat(
            count(
                clean,
                "ai.prompt_templates WHERE feature_code = 'GENERATE_TESTCASE'"
                    + " AND response_guards IS NULL"))
        .isEqualTo(1);
    assertThat(
            clean.queryForObject(
                "SELECT limit_tokens || '/' || warn_percent || '/' || review_per_hour || '/'"
                    + " || interview_per_hour || '/' || cooldown_seconds || '/' || period"
                    + " FROM ai.ai_token_budget_configs",
                String.class))
        .isEqualTo("0/70/10/3/20/MONTHLY");
    assertThat(
            clean.queryForObject(
                "SELECT extract(day FROM period_start) FROM ai.ai_token_budget_configs",
                Integer.class))
        .isEqualTo(1);
    assertThat(count(clean, "ai.prompt_templates WHERE created_by IS NOT NULL"))
        .as("seed rows have no acting user")
        .isZero();
  }

  private void prompt(
      String feature, String version, String status, String guards, boolean published) {
    jdbc.update(
        "INSERT INTO ai.prompt_templates (id, feature_code, version, status, system_instruction,"
            + " response_schema_version, model_id, response_guards, published_at)"
            + " VALUES (?, ?, ?, ?, 'x', 'v1', 'default', ?::jsonb, CASE WHEN ? THEN now() END)",
        UUID.randomUUID(),
        feature,
        version,
        status,
        guards,
        published);
  }

  private void session(String entryType, String column, UUID value) {
    jdbc.update(
        "INSERT INTO ai.interview_sessions (id, user_id, entry_type, "
            + column
            + ", stage) VALUES (?, ?, ?, ?, 'EXPLAIN')",
        UUID.randomUUID(),
        UUID.randomUUID(),
        entryType,
        value);
  }

  private UUID insertTopicSession() {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO ai.interview_sessions (id, user_id, entry_type, topics, interviewer_level,"
            + " max_turns, hint_allowed, stage) VALUES (?, ?, 'TOPIC_SELECTION', '[\"DP\"]'::jsonb,"
            + " 'INTERN', 8, false, 'EXPLAIN')",
        id,
        UUID.randomUUID());
    return id;
  }

  private UUID insertQuestionSession() {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO ai.interview_sessions (id, user_id, entry_type, question_ref, question_text,"
            + " interviewer_level, max_turns, hint_allowed, stage) VALUES (?, ?, 'QUESTION_BANK',"
            + " 'q1', 'What is a hash map?', 'JUNIOR', 8, true, 'EXPLAIN')",
        id,
        UUID.randomUUID());
    return id;
  }

  private void turn(UUID session, int index, String role, String stage) {
    jdbc.update(
        "INSERT INTO ai.interview_turns (id, interview_session_id, turn_index, role, stage_at_turn,"
            + " content) VALUES (?, ?, ?, ?, ?, 'hello')",
        UUID.randomUUID(),
        session,
        index,
        role,
        stage);
  }

  private void rubric(UUID session, String criterion, String score) {
    jdbc.update(
        "INSERT INTO ai.rubric_scores (id, owner_type, owner_id, criterion_code, score, comment)"
            + " VALUES (?, 'MOCK_INTERVIEW', ?, ?, ?::numeric, 'ok')",
        UUID.randomUUID(),
        session,
        criterion,
        score);
  }

  private UUID activePrompt(String feature) {
    return jdbc.queryForObject(
        "SELECT id FROM ai.prompt_templates WHERE feature_code = ? AND status = 'ACTIVE'",
        UUID.class,
        feature);
  }

  private UUID review(UUID submission, UUID problem, UUID template, String hash, int readability) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO ai.solution_reviews (id, submission_id, user_id, problem_id, language,"
            + " source_hash, prompt_template_id, result_json, readability_score)"
            + " VALUES (?, ?, ?, ?, 'JAVA', ?, ?, '{}'::jsonb, ?)",
        id,
        submission,
        UUID.randomUUID(),
        problem,
        hash,
        template,
        readability);
    return id;
  }

  private void cache(UUID problem, UUID template, UUID review) {
    jdbc.update(
        "INSERT INTO ai.ai_cache_entries (id, problem_id, language, source_hash, prompt_template_id,"
            + " solution_review_id) VALUES (?, ?, 'JAVA', ?, ?, ?)",
        UUID.randomUUID(),
        problem,
        HASH_A,
        template,
        review);
  }

  private void usage(UUID call, String feature, UUID problem, UUID owner) {
    jdbc.update(
        "INSERT INTO ai.token_usage (id, call_id, user_id, feature_code, problem_id, owner_id,"
            + " tokens_prompt, tokens_completion, model_name)"
            + " VALUES (?, ?, ?, ?, ?, ?, 100, 50, 'default')",
        UUID.randomUUID(),
        call,
        UUID.randomUUID(),
        feature,
        problem,
        owner);
  }

  private void alert(String status, boolean reviewed) {
    jdbc.update(
        "INSERT INTO ai.ai_usage_anomaly_alerts (id, user_id, window_start, window_end, call_count,"
            + " baseline_avg, ratio, status, reviewed_at) VALUES (?, ?, now() - interval '1 day',"
            + " now(), 50, 5.00, 10.00, ?, CASE WHEN ? THEN now() END)",
        UUID.randomUUID(),
        UUID.randomUUID(),
        status,
        reviewed);
  }

  private void outbox(String aggregateType, String eventType) {
    jdbc.update(
        "INSERT INTO ai.outbox_events (id, aggregate_type, aggregate_id, event_type, payload)"
            + " VALUES (?, ?, ?, ?, '{}'::jsonb)",
        UUID.randomUUID(),
        aggregateType,
        UUID.randomUUID(),
        eventType);
  }

  private static int count(JdbcTemplate template, String tableAndFilter) {
    return template.queryForObject("SELECT count(*) FROM " + tableAndFilter, Integer.class);
  }
}
