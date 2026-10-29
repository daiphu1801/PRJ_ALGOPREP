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
 * Applies the real Flyway migrations of module {@code interview-bank} (V6.001, V6.002) to a clean
 * PostgreSQL 16 and checks tables, search objects, seed data and constraints against {@code
 * 02-bd/database/F6_interview-bank.md} (tables 1.1 to 1.7 and the DDL contract in section 9).
 *
 * <p>Every Spring context in this module runs all module migrations, so the assertions here only
 * look at schema {@code interview_bank}. One test applies the F6 migrations alone to an empty
 * database to prove F6 does not depend on any other schema.
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
class InterviewBankMigrationTest {

  private static final List<String> EXPECTED_TABLES =
      List.of(
          "question_topics",
          "question_levels",
          "interview_questions",
          "answer_rubrics",
          "bookmarks",
          "user_answers",
          "practice_history",
          "recall_ratings");

  /** Tables that must hold no rows right after a clean install (BD 9: seed is catalogues only). */
  private static final List<String> EMPTY_AT_START =
      List.of(
          "interview_questions",
          "answer_rubrics",
          "bookmarks",
          "user_answers",
          "practice_history",
          "recall_ratings");

  private static final String FTS =
      "to_tsvector('simple', interview_bank.f_unaccent(title || ' ' ||"
          + " coalesce(content_markdown, '')))";

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
  @DisplayName("creates exactly the 8 interview_bank tables from BD section 4")
  void createsAllTables() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables"
                + " WHERE table_schema = 'interview_bank' AND table_type = 'BASE TABLE'",
            String.class);
    assertThat(tables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
  }

  @Test
  @DisplayName("has no foreign key that leaves schema interview_bank")
  void noCrossSchemaForeignKeys() {
    Integer crossing =
        jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.table_constraints tc"
                + " JOIN information_schema.constraint_column_usage ccu"
                + "   ON ccu.constraint_name = tc.constraint_name"
                + "  AND ccu.constraint_schema = tc.constraint_schema"
                + " WHERE tc.constraint_type = 'FOREIGN KEY'"
                + "   AND tc.table_schema = 'interview_bank'"
                + "   AND ccu.table_schema <> 'interview_bank'",
            Integer.class);
    assertThat(crossing).isZero();
  }

  @Test
  @DisplayName("running Flyway again changes nothing and does not undo admin edits to seed rows")
  void adminEditsSurviveNextMigrate() {
    jdbc.update(
        "UPDATE interview_bank.question_topics SET display_name = 'Co so du lieu'"
            + " WHERE code = 'DATABASE'");
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    flyway.validate();
    assertThat(
            jdbc.queryForObject(
                "SELECT display_name FROM interview_bank.question_topics WHERE code = 'DATABASE'",
                String.class))
        .isEqualTo("Co so du lieu");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM public.flyway_schema_history"
                    + " WHERE version IN ('6.001','6.002') AND success",
                Integer.class))
        .isEqualTo(2);
  }

  @Test
  @DisplayName(
      "clean install on its own: seeds 5 topics and 3 levels, sequence untouched, all else empty")
  void cleanInstallSeedsCatalogsOnly() {
    jdbc.execute("CREATE DATABASE f6_isolated");
    DriverManagerDataSource isolated =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl().replace("/algoprep", "/f6_isolated"),
            POSTGRES.getUsername(),
            POSTGRES.getPassword());
    JdbcTemplate clean = new JdbcTemplate(isolated);
    clean.execute("CREATE SCHEMA interview_bank");
    var result =
        Flyway.configure()
            .dataSource(isolated)
            .locations("classpath:db/migration/interview_bank")
            .defaultSchema("public")
            .load()
            .migrate();
    assertThat(result.success).isTrue();
    assertThat(result.migrationsExecuted).isEqualTo(2);
    assertSeededCatalogs(clean);
    for (String empty : EMPTY_AT_START) {
      assertThat(count(clean, "interview_bank." + empty)).as(empty).isZero();
    }
  }

  @Test
  @DisplayName(
      "search objects: unaccent in own schema, f_unaccent immutable, GIN index, d-bar handled")
  void searchObjects() {
    assertThat(
            jdbc.queryForObject(
                "SELECT extnamespace::regnamespace::text FROM pg_extension WHERE extname = 'unaccent'",
                String.class))
        .isEqualTo("interview_bank");
    assertThat(
            jdbc.queryForObject(
                "SELECT provolatile::text FROM pg_proc WHERE proname = 'f_unaccent'", String.class))
        .as("i = IMMUTABLE")
        .isEqualTo("i");
    assertThat(
            jdbc.queryForObject(
                "SELECT interview_bank.f_unaccent('Đường đi ngắn nhất')", String.class))
        .isEqualTo("Duong di ngan nhat");
    assertThat(
            jdbc.queryForObject(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'interview_questions_fts_idx'",
                String.class))
        .contains("USING gin");

    insertQuestion("Câu hỏi về cấu trúc dữ liệu", "Đường đi ngắn nhất");
    assertThat(matches("cau:* & hoi:*")).as("unaccented query").isEqualTo(1);
    assertThat(matches("interview_bank.f_unaccent('câu:* & hỏi:*')"))
        .as("accented query through f_unaccent")
        .isEqualTo(1);
    assertThat(matches("'câu:*'::text"))
        .as("an accented tsquery without f_unaccent finds nothing: the pipeline must unaccent q")
        .isZero();
  }

  @Test
  @DisplayName("display_no comes from a sequence and never repeats")
  void displayNumbers() {
    UUID first = insertQuestion("Seq one", null);
    UUID second = insertQuestion("Seq two", null);
    Integer a =
        jdbc.queryForObject(
            "SELECT display_no FROM interview_bank.interview_questions WHERE id = ?",
            Integer.class,
            first);
    Integer b =
        jdbc.queryForObject(
            "SELECT display_no FROM interview_bank.interview_questions WHERE id = ?",
            Integer.class,
            second);
    assertThat(b).isGreaterThan(a);
    jdbc.update(
        "UPDATE interview_bank.interview_questions SET status = 'RETIRED' WHERE id = ?", first);
    UUID third = insertQuestion("Seq three", null);
    assertThat(
            jdbc.queryForObject(
                "SELECT display_no FROM interview_bank.interview_questions WHERE id = ?",
                Integer.class,
                third))
        .as("a retired question keeps its number, it is not reused")
        .isGreaterThan(b);
  }

  @Test
  @DisplayName(
      "a topic with questions cannot be deleted; an emptied topic takes its progress rows along")
  void topicDeleteRules() {
    UUID used = insertTopic("USED-T");
    insertQuestion(used, "Uses topic", null);
    assertThatThrownBy(
            () -> jdbc.update("DELETE FROM interview_bank.question_topics WHERE id = ?", used))
        .as("interview_questions.topic_id is NO ACTION")
        .isInstanceOf(DataIntegrityViolationException.class);

    UUID emptied = insertTopic("EMPTY-T");
    UUID user = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO interview_bank.practice_history (id, user_id, topic_id) VALUES (?, ?, ?)",
        UUID.randomUUID(),
        user,
        emptied);
    jdbc.update("DELETE FROM interview_bank.question_topics WHERE id = ?", emptied);
    assertThat(count(jdbc, "interview_bank.practice_history WHERE topic_id = '" + emptied + "'"))
        .as("practice_history is a derived read model: ON DELETE CASCADE")
        .isZero();
  }

  @Test
  @DisplayName(
      "rubric: weight 0..100, criterion unique ignoring case, sort order unique per question")
  void rubricRules() {
    UUID q = insertQuestion("Rubric question", null);
    rubric(q, "Clarity", "60.00", 1);
    assertThatThrownBy(() -> rubric(q, "CLARITY", "10.00", 2))
        .as("criterion_code is unique ignoring case within a question")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> rubric(q, "Depth", "10.00", 1))
        .as("sort_order is unique within a question")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> rubric(q, "Too much", "100.01", 3))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> rubric(q, "Negative", "-1.00", 4))
        .isInstanceOf(DataIntegrityViolationException.class);
    rubric(q, "Depth", "40.00", 2);
    rubric(insertQuestion("Other question", null), "Clarity", "100.00", 1);
  }

  @Test
  @DisplayName(
      "attempts: status and feedback agree, snapshot is an array, attempt numbers are unique")
  void attemptRules() {
    UUID q = insertQuestion("Attempt question", null);
    UUID user = UUID.randomUUID();
    attempt(q, user, 1, "COMPLETED", "{\"criteria\": []}");
    attempt(q, user, 2, "FAILED", null);
    assertThatThrownBy(() -> attempt(q, user, 2, "FAILED", null))
        .as("attempt_no is unique per user and question")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> attempt(q, user, 3, "COMPLETED", null))
        .as("COMPLETED needs a feedback result")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> attempt(q, user, 3, "FAILED", "{}"))
        .as("FAILED has no feedback result")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> attempt(q, user, 3, "PENDING", null))
        .as("PENDING is never stored")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> attempt(q, user, 0, "FAILED", null))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> attemptWithSnapshot(q, user, 3, "{}"))
        .as("rubric snapshot is an array")
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("recall: rating and interval go together; progress needs a last_practiced_at")
  void recallAndProgressRules() {
    UUID q = insertQuestion("Recall question", null);
    UUID user = UUID.randomUUID();
    recall(q, user, "VAGUE", 3);
    assertThatThrownBy(() -> recall(q, user, "KNOWN", 7))
        .as("one row per user and question, overwritten in place by the application")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> recall(q, UUID.randomUUID(), "KNOWN", 3))
        .as("KNOWN is 7 days")
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(() -> recall(q, UUID.randomUUID(), "SKIPPED", 1))
        .isInstanceOf(DataIntegrityViolationException.class);

    UUID topic = insertTopic("PROGRESS-T");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO interview_bank.practice_history (id, user_id, topic_id,"
                        + " questions_attempted) VALUES (?, ?, ?, 2)",
                    UUID.randomUUID(),
                    user,
                    topic))
        .as("attempted > 0 needs last_practiced_at")
        .isInstanceOf(DataIntegrityViolationException.class);
    jdbc.update(
        "INSERT INTO interview_bank.practice_history (id, user_id, topic_id) VALUES (?, ?, ?)",
        UUID.randomUUID(),
        user,
        topic);
  }

  private static void assertSeededCatalogs(JdbcTemplate clean) {
    assertThat(
            clean.queryForList(
                "SELECT code FROM interview_bank.question_topics ORDER BY sort_order",
                String.class))
        .containsExactly("CS_THEORY", "SYSTEM_DESIGN", "DATABASE", "LANGUAGE", "BEHAVIORAL");
    assertThat(
            clean.queryForList(
                "SELECT code FROM interview_bank.question_topics WHERE uses_star_framework",
                String.class))
        .containsExactly("BEHAVIORAL");
    assertThat(
            clean.queryForList(
                "SELECT code FROM interview_bank.question_levels ORDER BY sort_order",
                String.class))
        .containsExactly("EASY", "MEDIUM", "HARD");
    assertThat(
            clean.queryForObject(
                "SELECT is_called FROM interview_bank.interview_question_display_no_seq",
                Boolean.class))
        .as("no question was seeded, so the sequence has not been used")
        .isFalse();
  }

  private int matches(String tsquery) {
    String query =
        tsquery.startsWith("interview_bank.") || tsquery.endsWith("::text")
            ? "to_tsquery('simple', " + tsquery + ")"
            : "to_tsquery('simple', '" + tsquery + "')";
    return jdbc.queryForObject(
        "SELECT count(*) FROM interview_bank.interview_questions WHERE " + FTS + " @@ " + query,
        Integer.class);
  }

  private UUID insertTopic(String code) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO interview_bank.question_topics (id, code, display_name, sort_order)"
            + " VALUES (?, ?, ?, 90)",
        id,
        code,
        "Chủ đề " + code);
    return id;
  }

  private UUID insertQuestion(String title, String content) {
    UUID topic =
        jdbc.queryForObject(
            "SELECT id FROM interview_bank.question_topics WHERE code = 'CS_THEORY'", UUID.class);
    return insertQuestion(topic, title, content);
  }

  private UUID insertQuestion(UUID topic, String title, String content) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO interview_bank.interview_questions (id, topic_id, level_id, title,"
            + " content_markdown, created_by) VALUES (?, ?,"
            + " (SELECT id FROM interview_bank.question_levels WHERE code = 'EASY'), ?, ?, ?)",
        id,
        topic,
        title,
        content,
        UUID.randomUUID());
    return id;
  }

  private void rubric(UUID question, String code, String weight, int order) {
    jdbc.update(
        "INSERT INTO interview_bank.answer_rubrics (id, question_id, criterion_code,"
            + " weight_percent, sort_order) VALUES (?, ?, ?, ?::numeric, ?)",
        UUID.randomUUID(),
        question,
        code,
        weight,
        order);
  }

  private record Attempt(UUID user, int no, String status, String result, String snapshot) {}

  private void attempt(UUID question, UUID user, int no, String status, String result) {
    insertAttempt(question, new Attempt(user, no, status, result, "[]"));
  }

  private void attemptWithSnapshot(UUID question, UUID user, int no, String snapshot) {
    insertAttempt(question, new Attempt(user, no, "FAILED", null, snapshot));
  }

  private void insertAttempt(UUID question, Attempt a) {
    jdbc.update(
        "INSERT INTO interview_bank.user_answers (id, user_id, question_id, answer_text, attempt_no,"
            + " feedback_status, feedback_result_json, rubric_snapshot_json)"
            + " VALUES (?, ?, ?, 'answer', ?, ?, ?::jsonb, ?::jsonb)",
        UUID.randomUUID(),
        a.user(),
        question,
        a.no(),
        a.status(),
        a.result(),
        a.snapshot());
  }

  private void recall(UUID question, UUID user, String rating, int days) {
    jdbc.update(
        "INSERT INTO interview_bank.recall_ratings (id, user_id, question_id, rating,"
            + " interval_days, next_review_at) VALUES (?, ?, ?, ?, ?, now() + ? * interval '1 day')",
        UUID.randomUUID(),
        user,
        question,
        rating,
        days,
        days);
  }

  private static int count(JdbcTemplate template, String tableAndFilter) {
    return template.queryForObject("SELECT count(*) FROM " + tableAndFilter, Integer.class);
  }
}
