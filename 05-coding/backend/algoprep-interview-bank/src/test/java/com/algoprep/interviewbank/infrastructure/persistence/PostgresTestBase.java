package com.algoprep.interviewbank.infrastructure.persistence;

import com.algoprep.common.domain.ports.out.IdGeneratorPort;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real PostgreSQL for the repository tests. One container is started per test JVM and shared by
 * every subclass; tables are truncated and the catalogue seed re-applied before each test.
 *
 * <p>The interview_bank schema owns every table the repositories touch (user_id and created_by are
 * plain UUIDs without foreign keys, BD F6 section 9), so no stand-in tables for other contexts are
 * needed. The module's own migrations are applied from the classpath, in version order.
 */
abstract class PostgresTestBase {
  static final UUID TOPIC_CS = UUID.fromString("60000000-0000-4000-8000-000000000101");
  static final UUID TOPIC_DESIGN = UUID.fromString("60000000-0000-4000-8000-000000000102");
  static final UUID TOPIC_BEHAVIORAL = UUID.fromString("60000000-0000-4000-8000-000000000105");
  static final UUID LEVEL_EASY = UUID.fromString("60000000-0000-4000-8000-000000000001");
  static final UUID LEVEL_MEDIUM = UUID.fromString("60000000-0000-4000-8000-000000000002");

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine").withDatabaseName("algoprep");
  private static final DataSource DATA;
  private static final String MIGRATION = "db/migration/interview_bank/";

  static {
    POSTGRES.start();
    DATA =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
    try (Connection connection = DATA.getConnection()) {
      connection.createStatement().execute("CREATE SCHEMA IF NOT EXISTS interview_bank");
      runScript(connection, "V6.001__create_interview_bank_tables.sql");
    } catch (SQLException failure) {
      throw new IllegalStateException(failure);
    }
  }

  protected final JdbcClient jdbc = JdbcClient.create(DATA);
  protected final IdGeneratorPort ids = UUID::randomUUID;

  /** The migrations use SET LOCAL, so each runs inside one explicit transaction. */
  private static void runScript(Connection connection, String name) throws SQLException {
    connection.setAutoCommit(false);
    ScriptUtils.executeSqlScript(
        connection, new EncodedResource(new ClassPathResource(MIGRATION + name)));
    connection.commit();
    connection.setAutoCommit(true);
  }

  @BeforeEach
  void resetDatabase() throws SQLException {
    try (Connection connection = DATA.getConnection()) {
      connection
          .createStatement()
          .execute(
              "TRUNCATE interview_bank.recall_ratings, interview_bank.practice_history,"
                  + " interview_bank.user_answers, interview_bank.bookmarks,"
                  + " interview_bank.answer_rubrics, interview_bank.interview_questions,"
                  + " interview_bank.question_levels, interview_bank.question_topics"
                  + " RESTART IDENTITY CASCADE");
      runScript(connection, "V6.002__seed_interview_bank_catalogs.sql");
    }
  }

  protected ObjectProvider<JdbcClient> provider() {
    return new ObjectProvider<>() {
      @Override
      public JdbcClient getObject() {
        return jdbc;
      }
    };
  }

  /** Inserts an ACTIVE question with the given topic, level and title; returns its id. */
  protected UUID question(UUID topic, UUID level, String title, String content) {
    return insertQuestion(topic, level, title, content, "ACTIVE");
  }

  protected UUID retired(UUID topic, UUID level, String title) {
    return insertQuestion(topic, level, title, "x", "RETIRED");
  }

  /** Overwrites the optional JSON columns (CSV-only data) of an existing question. */
  protected UUID withJson(UUID id, String followUps, String keywords) {
    jdbc.sql(
            "UPDATE interview_bank.interview_questions SET follow_up_questions ="
                + " CAST(:fu AS jsonb), core_keywords = CAST(CAST(:kw AS text) AS jsonb)"
                + " WHERE id = :id")
        .param("id", id)
        .param("fu", followUps)
        .param("kw", keywords)
        .update();
    return id;
  }

  private UUID insertQuestion(UUID topic, UUID level, String title, String content, String status) {
    var id = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO interview_bank.interview_questions(id, topic_id, level_id, title,"
                + " content_markdown, suggested_approach, status, created_by)"
                + " VALUES (:id, :t, :l, :title, :content, 'approach', :status, :by)")
        .param("id", id)
        .param("t", topic)
        .param("l", level)
        .param("title", title)
        .param("content", content)
        .param("status", status)
        .param("by", UUID.randomUUID())
        .update();
    return id;
  }

  protected void rubric(UUID question, int order, String code, int weight) {
    jdbc.sql(
            "INSERT INTO interview_bank.answer_rubrics(id, question_id, criterion_code,"
                + " weight_percent, sort_order) VALUES (:id, :q, :code, :w, :o)")
        .param("id", UUID.randomUUID())
        .param("q", question)
        .param("code", code)
        .param("w", weight)
        .param("o", order)
        .update();
  }

  protected int count(String table) {
    return jdbc.sql("SELECT count(*) FROM interview_bank." + table).query(Integer.class).single();
  }
}
