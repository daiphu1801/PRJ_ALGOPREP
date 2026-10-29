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
 * Applies the real Flyway migrations of module {@code problem-bank} (V2.001, V2.002) to a clean
 * PostgreSQL 16 and checks tables, seed data and constraints against {@code
 * 02-bd/database/F2_problem-bank.md}.
 *
 * <p>Every Spring context in this module runs all module migrations, so the assertions here only
 * look at schema {@code problem} and versions 2.001 and 2.002. One test additionally applies the F2
 * migrations alone to an empty database to prove F2 does not depend on any other schema.
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
class ProblemMigrationTest {

  private static final UUID LEVEL_EASY = UUID.fromString("20000000-0000-4000-8000-000000000001");

  private static final List<String> EXPECTED_TABLES =
      List.of(
          "topics",
          "problem_levels",
          "tags",
          "problems",
          "problem_topics",
          "problem_tags",
          "problem_specs",
          "testcases",
          "problem_reference_solutions",
          "worked_examples",
          "class_assignments",
          "bookmarks",
          "problem_stats",
          "problem_bank_settings",
          "testcase_generation_runs",
          "consumed_events",
          "testcase_set_snapshots",
          "outbox_events");

  /**
   * Tables that must hold no rows right after a clean install (BD 7.2: seed is catalogues only).
   */
  private static final List<String> EMPTY_AT_START =
      List.of(
          "tags",
          "problems",
          "problem_topics",
          "problem_tags",
          "problem_specs",
          "testcases",
          "problem_reference_solutions",
          "worked_examples",
          "class_assignments",
          "bookmarks",
          "problem_stats",
          "testcase_generation_runs",
          "consumed_events",
          "testcase_set_snapshots",
          "outbox_events");

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
  @DisplayName("creates exactly the 18 problem tables from BD section 4")
  void createsAllTables() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'problem'",
            String.class);
    assertThat(tables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
  }

  @Test
  @DisplayName("has no foreign key that leaves schema problem")
  void noCrossSchemaForeignKeys() {
    Integer crossing =
        jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.table_constraints tc"
                + " JOIN information_schema.constraint_column_usage ccu"
                + "   ON ccu.constraint_name = tc.constraint_name"
                + "  AND ccu.constraint_schema = tc.constraint_schema"
                + " WHERE tc.constraint_type = 'FOREIGN KEY' AND tc.table_schema = 'problem'"
                + "   AND ccu.table_schema <> 'problem'",
            Integer.class);
    assertThat(crossing).isZero();
  }

  @Test
  @DisplayName("running Flyway again changes nothing and does not undo admin edits to seed rows")
  void adminEditsSurviveNextMigrate() {
    jdbc.update("UPDATE problem.topics SET name = 'Mang 1 chieu' WHERE code = 'MANG'");
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    flyway.validate();
    assertThat(
            jdbc.queryForObject(
                "SELECT name FROM problem.topics WHERE code = 'MANG'", String.class))
        .isEqualTo("Mang 1 chieu");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM public.flyway_schema_history"
                    + " WHERE version IN ('2.001','2.002') AND success",
                Integer.class))
        .isEqualTo(2);
  }

  @Test
  @DisplayName("clean install on its own: seeds 3 levels, 6 topics, 1 settings row, all else empty")
  void cleanInstallSeedsCatalogsOnly() {
    jdbc.execute("CREATE DATABASE f2_isolated");
    DriverManagerDataSource isolated =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl().replace("/algoprep", "/f2_isolated"),
            POSTGRES.getUsername(),
            POSTGRES.getPassword());
    JdbcTemplate clean = new JdbcTemplate(isolated);
    clean.execute("CREATE SCHEMA problem");
    var result =
        Flyway.configure()
            .dataSource(isolated)
            .locations("classpath:db/migration/problem")
            .defaultSchema("public")
            .load()
            .migrate();
    assertThat(result.success).isTrue();
    assertThat(result.migrationsExecuted).isEqualTo(2);

    assertThat(count(clean, "problem.problem_levels")).isEqualTo(3);
    assertThat(count(clean, "problem.topics")).isEqualTo(6);
    assertThat(count(clean, "problem.problem_bank_settings")).isEqualTo(1);
    assertThat(
            clean.queryForObject(
                "SELECT max_ai_generations_per_problem FROM problem.problem_bank_settings WHERE id = 1",
                Integer.class))
        .isEqualTo(2);
    assertThat(
            clean.queryForList(
                "SELECT code FROM problem.problem_levels ORDER BY sort_order", String.class))
        .containsExactly("EASY", "MEDIUM", "HARD");
    assertThat(
            clean.queryForList("SELECT code FROM problem.topics ORDER BY sort_order", String.class))
        .containsExactly("MANG", "CHUOI", "DANH-SACH-LIEN-KET", "CAY", "DO-THI", "QUY-HOACH-DONG");
    for (String empty : EMPTY_AT_START) {
      assertThat(count(clean, "problem." + empty)).as(empty).isZero();
    }
  }

  @Test
  @DisplayName("tags, topics and levels are unique ignoring case")
  void namesUniqueIgnoringCase() {
    jdbc.update("INSERT INTO problem.tags (id, name) VALUES (?, 'Array')", UUID.randomUUID());
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO problem.tags (id, name) VALUES (?, 'array')", UUID.randomUUID()))
        .isInstanceOf(DataIntegrityViolationException.class);
    // Own rows, not the seed: other tests sharing this database may rename seed rows.
    jdbc.update(
        "INSERT INTO problem.topics (id, code, name, sort_order) VALUES (?, 'UNI-T', 'Chủ đề kiểm', 90)",
        UUID.randomUUID());
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO problem.topics (id, code, name, sort_order)"
                        + " VALUES (?, 'UNI-T2', 'CHỦ ĐỀ KIỂM', 91)",
                    UUID.randomUUID()))
        .isInstanceOf(DataIntegrityViolationException.class);
    jdbc.update(
        "INSERT INTO problem.problem_levels (id, code, display_name, sort_order)"
            + " VALUES (?, 'UNI-L', 'Mức kiểm', 90)",
        UUID.randomUUID());
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO problem.problem_levels (id, code, display_name, sort_order)"
                        + " VALUES (?, 'UNI-L2', 'MỨC KIỂM', 91)",
                    UUID.randomUUID()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName(
      "a level or topic still referenced by a problem cannot be deleted, deleted problem included")
  void referencedCatalogRowsAreProtected() {
    UUID problem = insertProblem("REF-PROTECT");
    jdbc.update(
        "INSERT INTO problem.problem_topics (problem_id, topic_id)"
            + " SELECT ?, id FROM problem.topics WHERE code = 'CAY'",
        problem);
    jdbc.update("UPDATE problem.problems SET deleted = true WHERE id = ?", problem);
    assertThatThrownBy(
            () -> jdbc.update("DELETE FROM problem.problem_levels WHERE id = ?", LEVEL_EASY))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM problem.topics WHERE code = 'CAY'"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName(
      "problem code stays taken after soft delete; delete requires UNPUBLISHED; limits are positive")
  void problemRules() {
    UUID problem = insertProblem("CODE-KEEP");
    jdbc.update("UPDATE problem.problems SET deleted = true WHERE id = ?", problem);
    assertThatThrownBy(() -> insertProblem("CODE-KEEP"))
        .isInstanceOf(DataIntegrityViolationException.class);
    UUID published = insertProblem("PUB-1");
    jdbc.update("UPDATE problem.problems SET status = 'PUBLISHED' WHERE id = ?", published);
    assertThatThrownBy(
            () -> jdbc.update("UPDATE problem.problems SET deleted = true WHERE id = ?", published))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE problem.problems SET time_limit_ms = 0 WHERE id = ?", published))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "UPDATE problem.problems SET max_submissions_per_hour = 0 WHERE id = ?",
                    published))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThat(
            jdbc.queryForObject(
                "SELECT max_submissions_per_hour FROM problem.problems WHERE id = ?",
                Integer.class,
                published))
        .as("NULL means system default; 60 is never stored")
        .isNull();
  }

  @Test
  @DisplayName("spec: EPSILON needs a positive value; signature is all or none; [] is valid")
  void specRules() {
    UUID p1 = insertProblem("SPEC-1");
    assertThatThrownBy(() -> insertSpec(p1, "EPSILON", null, null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertSpec(p1, "EPSILON", BigDecimal.ZERO, null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> insertSpec(p1, "EPSILON", new BigDecimal("-0.5"), null))
        .isInstanceOf(DataIntegrityViolationException.class);
    insertSpec(p1, "EPSILON", new BigDecimal("0.000000000001"), null);
    UUID p2 = insertProblem("SPEC-2");
    insertSpec(p2, "EXACT", new BigDecimal("999999.999999999999"), null);
    UUID p3 = insertProblem("SPEC-3");
    insertSpec(p3, "EXACT", null, "solve");
    UUID p4 = insertProblem("SPEC-4");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO problem.problem_specs (id, problem_id, function_name)"
                        + " VALUES (?, ?, 'solve')",
                    UUID.randomUUID(),
                    p4))
        .isInstanceOf(DataIntegrityViolationException.class);
    UUID p5 = insertProblem("SPEC-5");
    insertSpec(p5, "EXACT", null, null);
    assertThatThrownBy(() -> insertSpec(p5, "EXACT", null, null))
        .as("one spec per problem")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("testcase: each side holds exactly one storage form; empty inline text is valid")
  void testcaseStorageBranches() {
    UUID problem = insertProblem("TC-1");
    insertTestcase(problem, Side.inline(""), Side.inline(""));
    insertTestcase(problem, Side.minio("k/in"), Side.inline("x"));
    insertTestcase(problem, Side.inline("x"), Side.minio("k/out"));
    insertTestcase(problem, Side.minio("k/in2"), Side.minio("k/out2"));
    assertThatThrownBy(
            () -> insertTestcase(problem, new Side("INLINE", null, null), Side.inline("x")))
        .as("INLINE without data")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> insertTestcase(problem, new Side("INLINE", "x", "k/in"), Side.inline("x")))
        .as("both forms on input")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> insertTestcase(problem, new Side("MINIO", "x", "k/in"), Side.inline("x")))
        .as("MINIO with inline data")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () -> insertTestcase(problem, Side.inline("x"), new Side("MINIO", null, null)))
        .as("MINIO output without key")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName(
      "class assignment: one active row per problem and class; re-assign after removal keeps history")
  void assignmentRules() {
    UUID problem = insertProblem("ASG-1");
    UUID cls = UUID.randomUUID();
    insertAssignment(problem, cls);
    assertThatThrownBy(() -> insertAssignment(problem, cls))
        .isInstanceOf(DataIntegrityViolationException.class);
    jdbc.update("UPDATE problem.class_assignments SET removed_at = now() WHERE class_id = ?", cls);
    insertAssignment(problem, cls);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM problem.class_assignments WHERE class_id = ?",
                Integer.class,
                cls))
        .isEqualTo(2);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO problem.class_assignments (id, problem_id, class_id, assigned_by)"
                        + " VALUES (?, ?, NULL, ?)",
                    UUID.randomUUID(),
                    problem,
                    UUID.randomUUID()))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("problem_stats: counters are non-negative and accepted never exceeds submissions")
  void statsChecks() {
    UUID problem = insertProblem("STAT-1");
    jdbc.update(
        "INSERT INTO problem.problem_stats (problem_id, submission_count, accepted_count)"
            + " VALUES (?, 3, 3)",
        problem);
    UUID other = insertProblem("STAT-2");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO problem.problem_stats (problem_id, submission_count, accepted_count)"
                        + " VALUES (?, 1, 2)",
                    other))
        .as("accepted > submission")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO problem.problem_stats (problem_id, submission_count) VALUES (?, -1)",
                    other))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("problem_bank_settings: singleton row and AI generation cap between 1 and 10")
  void settingsChecks() {
    assertThatThrownBy(
            () -> jdbc.update("INSERT INTO problem.problem_bank_settings (id) VALUES (2)"))
        .as("singleton")
        .isInstanceOf(DataIntegrityViolationException.class);
    for (int outOfRange : new int[] {0, 11}) {
      assertThatThrownBy(
              () ->
                  jdbc.update(
                      "UPDATE problem.problem_bank_settings SET max_ai_generations_per_problem = ?",
                      outOfRange))
          .as("cap " + outOfRange)
          .isInstanceOf(DataIntegrityViolationException.class);
    }
  }

  @Test
  @DisplayName("snapshots, outbox and generation runs reject duplicates and non-positive counts")
  void snapshotOutboxAndRunChecks() {
    UUID problem = insertProblem("MISC-1");
    jdbc.update(
        "INSERT INTO problem.testcase_set_snapshots (problem_id, version, object_key, testcase_count,"
            + " content_sha256) VALUES (?, 1, 'bundle/1', 8, repeat('a', 64))",
        problem);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO problem.testcase_set_snapshots (problem_id, version, object_key,"
                        + " testcase_count, content_sha256)"
                        + " VALUES (?, 1, 'bundle/other', 8, repeat('a', 64))",
                    problem))
        .as("duplicate (problem, version)")
        .isInstanceOf(DataIntegrityViolationException.class);
    UUID event = UUID.randomUUID();
    insertOutbox(event);
    assertThatThrownBy(() -> insertOutbox(event))
        .as("duplicate event id")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO problem.testcase_generation_runs (id, problem_id, generator_script,"
                        + " validator_script, seed, model_id, prompt_version, requested_by, added_count)"
                        + " VALUES (?, ?, 'g', 'v', 1, 'm', 'p', ?, 0)",
                    UUID.randomUUID(),
                    problem,
                    UUID.randomUUID()))
        .as("added_count must be positive")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  private static int count(JdbcTemplate template, String table) {
    return template.queryForObject("SELECT count(*) FROM " + table, Integer.class);
  }

  private UUID insertProblem(String code) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO problem.problems (id, code, title, level_id, time_limit_ms, memory_limit_mb,"
            + " max_output_size_kb, author_id) VALUES (?, ?, 'T', ?, 1000, 256, 64, ?)",
        id,
        code,
        LEVEL_EASY,
        UUID.randomUUID());
    return id;
  }

  private void insertSpec(UUID problem, String strategy, BigDecimal epsilon, String functionName) {
    String signature =
        functionName == null
            ? "NULL, NULL, NULL"
            : "'" + functionName + "', '{}'::jsonb, '[]'::jsonb";
    jdbc.update(
        "INSERT INTO problem.problem_specs (id, problem_id, matching_strategy, epsilon_value,"
            + " function_name, return_type, parameters) VALUES (?, ?, ?, ?, "
            + signature
            + ")",
        UUID.randomUUID(),
        problem,
        strategy,
        epsilon);
  }

  /**
   * One side (input or expected output) of a testcase as stored: mode plus the two branch columns.
   */
  private record Side(String storage, String inline, String key) {
    static Side inline(String text) {
      return new Side("INLINE", text, null);
    }

    static Side minio(String objectKey) {
      return new Side("MINIO", null, objectKey);
    }
  }

  private void insertTestcase(UUID problem, Side input, Side output) {
    jdbc.update(
        "INSERT INTO problem.testcases (id, problem_id, visibility, input_storage, input_inline,"
            + " input_object_key, expected_output_storage, expected_output_inline,"
            + " expected_output_object_key, approved, position)"
            + " VALUES (?, ?, 'HIDDEN', ?, ?, ?, ?, ?, ?, true, 1)",
        UUID.randomUUID(),
        problem,
        input.storage(),
        input.inline(),
        input.key(),
        output.storage(),
        output.inline(),
        output.key());
  }

  private void insertAssignment(UUID problem, UUID classId) {
    jdbc.update(
        "INSERT INTO problem.class_assignments (id, problem_id, class_id, assigned_by)"
            + " VALUES (?, ?, ?, ?)",
        UUID.randomUUID(),
        problem,
        classId,
        UUID.randomUUID());
  }

  private void insertOutbox(UUID id) {
    jdbc.update(
        "INSERT INTO problem.outbox_events (id, aggregate_type, aggregate_id, event_type, routing_key,"
            + " payload) VALUES (?, 'Problem', ?, 'ProblemPublished', 'problem.published', '{}'::jsonb)",
        id,
        UUID.randomUUID());
  }
}
