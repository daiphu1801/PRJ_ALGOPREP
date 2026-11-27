package com.algoprep.aireview.infrastructure.persistence;

import com.algoprep.aireview.domain.model.FeatureCode;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real PostgreSQL for the repository tests. One container is started per test JVM and shared by
 * every subclass; the tables are truncated and the V5.002 seed is re-applied before each test.
 *
 * <p>The ai schema owns every table the repositories touch (user_id, problem_id, submission_id are
 * plain UUIDs without foreign keys, BD F5 section 1), so no stand-in tables for other contexts are
 * needed. The module's own migrations are applied from the classpath, in version order, without
 * Flyway.
 */
abstract class PostgresTestBase {
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine").withDatabaseName("algoprep");
  private static final DataSource DATA;
  private static final String MIGRATION = "db/migration/ai/";

  static {
    POSTGRES.start();
    DATA =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl() + "&currentSchema=ai,public",
            POSTGRES.getUsername(),
            POSTGRES.getPassword());
    try (Connection connection = DATA.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute("CREATE SCHEMA IF NOT EXISTS ai");
      runScript(connection, "V5.001__create_ai_tables.sql");
      runScript(connection, "V5.003__add_prompt_max_tokens.sql");
    } catch (SQLException failure) {
      throw new IllegalStateException(failure);
    }
  }

  protected final JdbcClient jdbc = JdbcClient.create(DATA);
  private final DataSourceTransactionManager manager = new DataSourceTransactionManager(DATA);
  protected final TransactionTemplate tx = new TransactionTemplate(manager);

  /** A second, independent connection: what a concurrent worker would see. */
  protected final TransactionTemplate other =
      new TransactionTemplate(manager) {
        {
          setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        }
      };

  private static void runScript(Connection connection, String name) throws SQLException {
    connection.setAutoCommit(false);
    ScriptUtils.executeSqlScript(
        connection,
        new EncodedResource(new ClassPathResource(MIGRATION + name), StandardCharsets.UTF_8));
    connection.commit();
    connection.setAutoCommit(true);
  }

  @BeforeEach
  void resetDatabase() throws SQLException {
    try (Connection connection = DATA.getConnection();
        Statement statement = connection.createStatement()) {
      statement.execute(
          "TRUNCATE ai.ai_cache_entries, ai.solution_reviews, ai.interview_turns,"
              + " ai.interview_sessions, ai.rubric_scores, ai.token_usage,"
              + " ai.ai_usage_anomaly_alerts, ai.outbox_events, ai.ai_model_prices,"
              + " ai.ai_token_budget_configs, ai.prompt_templates CASCADE");
      runScript(connection, "V5.002__seed_ai_defaults.sql");
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

  /** PostgreSQL keeps microseconds, so round-trip assertions need an Instant cut to micros. */
  protected static Instant now() {
    return Instant.now().truncatedTo(ChronoUnit.MICROS);
  }

  protected int count(String table) {
    return jdbc.sql("SELECT count(*) FROM ai." + table).query(Integer.class).single();
  }

  /** Token counts, model and time of one ledger row. */
  protected record Spend(int prompt, int completion, String model, Instant at) {}

  /** Inserts a ledger row; GENERATE_TESTCASE rows use the problem as owner (table CHECK). */
  protected void usage(UUID user, FeatureCode feature, UUID problem, Spend spend) {
    jdbc.sql(
            "INSERT INTO ai.token_usage(id,call_id,user_id,feature_code,problem_id,owner_id,"
                + "tokens_prompt,tokens_completion,model_name,created_at)"
                + " VALUES(:id,:call,:u,:f,:p,:o,:tp,:tc,:m,:at)")
        .param("id", UUID.randomUUID())
        .param("call", UUID.randomUUID())
        .param("u", user)
        .param("f", feature.name())
        .param("p", problem)
        .param("o", problem != null ? problem : UUID.randomUUID())
        .param("tp", spend.prompt())
        .param("tc", spend.completion())
        .param("m", spend.model())
        .param("at", utc(spend.at()))
        .update();
  }
}
