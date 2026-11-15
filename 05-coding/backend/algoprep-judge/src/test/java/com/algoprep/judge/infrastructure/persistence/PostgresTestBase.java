package com.algoprep.judge.infrastructure.persistence;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real PostgreSQL for the repository tests. One container is started per test JVM and shared by
 * every subclass; tables are truncated and re-seeded before each test.
 *
 * <p>The judge schema owns every table the repositories read (user_id and problem_id are plain
 * UUIDs without foreign keys, BD F4 section 8), so no stand-in tables for other contexts are
 * needed. The module's own migrations are applied from the classpath, in version order.
 */
abstract class PostgresTestBase {
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine").withDatabaseName("algoprep");
  private static final DataSource DATA;
  private static final String MIGRATION = "db/migration/judge/";

  static {
    POSTGRES.start();
    DATA =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl() + "&currentSchema=judge,public",
            POSTGRES.getUsername(),
            POSTGRES.getPassword());
    try (Connection connection = DATA.getConnection()) {
      connection.createStatement().execute("CREATE SCHEMA IF NOT EXISTS judge");
      runScript(connection, "V4.001__create_judge_tables.sql");
    } catch (SQLException failure) {
      throw new IllegalStateException(failure);
    }
  }

  protected final DataSource data = DATA;
  protected final JdbcClient jdbc = JdbcClient.create(DATA);
  protected final TransactionTemplate tx =
      new TransactionTemplate(new DataSourceTransactionManager(DATA));

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
              "TRUNCATE judge.callback_logs, judge.submission_testcase_results, judge.judge_run_tokens,"
                  + " judge.outbox_events, judge.submissions, judge.language_configs,"
                  + " judge.grading_defaults, judge.sandbox_settings,"
                  + " judge.generator_sandbox_settings, judge.cluster_settings CASCADE");
      runScript(connection, "V4.002__seed_judge_defaults.sql");
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

  protected static OffsetDateTime utc(Instant value) {
    return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
  }

  /** Inserts a submission; terminal statuses get finished_at, SYSTEM_ERROR a failure reason. */
  protected UUID submission(UUID user, UUID problem, String status, Instant submittedAt) {
    return submission(user, problem, status, "JAVA", submittedAt, submittedAt);
  }

  protected UUID submission(
      UUID user, UUID problem, String status, String language, Instant... times) {
    Instant submittedAt = times[0];
    Instant updatedAt = times[1];
    var id = UUID.randomUUID();
    boolean terminal = !java.util.Set.of("PENDING", "COMPILING", "JUDGING").contains(status);
    jdbc.sql(
            "INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,"
                + "source_code,source_input_method,status,failure_reason,compile_error_message,"
                + "testcase_set_version_at_grading,submitted_at,finished_at,updated_at,spec_version)"
                + " VALUES(:id,:u,:p,:lang,'STANDARD_IO','src','EDITOR',:status,:reason,:compile,1,"
                + ":at,:fin,:upd,1)")
        .param("id", id)
        .param("u", user)
        .param("p", problem)
        .param("lang", language)
        .param("status", status)
        .param("reason", status.equals("SYSTEM_ERROR") ? "SANDBOX_FAULT" : null)
        .param("compile", status.equals("COMPILE_ERROR") ? "boom" : null)
        .param("at", utc(submittedAt))
        .param(
            "fin",
            terminal ? utc(submittedAt.plusSeconds(1)) : null,
            java.sql.Types.TIMESTAMP_WITH_TIMEZONE)
        .param("upd", utc(updatedAt))
        .update();
    return id;
  }

  protected int count(String table) {
    return jdbc.sql("SELECT count(*) FROM judge." + table).query(Integer.class).single();
  }
}
