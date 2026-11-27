package com.algoprep.problembank.infrastructure.persistence;

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
 * Real PostgreSQL for the problem-bank repository tests. One container is started per test JVM and
 * shared by every subclass; tables are truncated and the catalogue seed (V2.002) is re-applied
 * before each test.
 *
 * <p>The problem schema owns every table its repositories read. user_id, author_id and class_id are
 * plain UUIDs without foreign keys (BD F2 section 1), so no stand-in tables for the identity
 * context are needed. The module's own migrations are applied from the classpath in version order.
 */
public abstract class PostgresTestBase {
  protected static final UUID EASY = UUID.fromString("20000000-0000-4000-8000-000000000001");
  protected static final UUID MEDIUM = UUID.fromString("20000000-0000-4000-8000-000000000002");
  protected static final UUID HARD = UUID.fromString("20000000-0000-4000-8000-000000000003");
  protected static final UUID MANG = UUID.fromString("20000000-0000-4000-8000-000000000101");
  protected static final UUID CHUOI = UUID.fromString("20000000-0000-4000-8000-000000000102");
  protected static final UUID AUTHOR = id(900);

  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine").withDatabaseName("algoprep");
  private static final DataSource DATA;
  private static final String MIGRATION = "db/migration/problem/";

  static {
    POSTGRES.start();
    DATA = dataSource();
    try (Connection connection = DATA.getConnection()) {
      connection.createStatement().execute("CREATE SCHEMA IF NOT EXISTS problem");
      runScript(connection, "V2.001__create_problem_bank_tables.sql");
      runScript(connection, "V2.003__catalog_cache_epoch.sql");
      runScript(connection, "V2.004__outbox_retry_fairness.sql");
    } catch (SQLException failure) {
      throw new IllegalStateException(failure);
    }
  }

  protected final DataSource data = DATA;
  protected final JdbcClient jdbc = JdbcClient.create(DATA);
  protected final TransactionTemplate tx =
      new TransactionTemplate(new DataSourceTransactionManager(DATA));

  /** A new connection source: it never joins a transaction opened on another DataSource. */
  protected static DataSource dataSource() {
    return new DriverManagerDataSource(
        POSTGRES.getJdbcUrl() + "&currentSchema=problem,public",
        POSTGRES.getUsername(),
        POSTGRES.getPassword());
  }

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
              "TRUNCATE problem.problems, problem.problem_levels, problem.topics, problem.tags,"
                  + " problem.problem_bank_settings, problem.consumed_events,"
                  + " problem.outbox_events CASCADE");
      runScript(connection, "V2.002__seed_problem_bank_catalogs.sql");
    }
  }

  protected ObjectProvider<JdbcClient> provider() {
    return provider(jdbc);
  }

  protected static ObjectProvider<JdbcClient> provider(JdbcClient client) {
    return new ObjectProvider<>() {
      @Override
      public JdbcClient getObject() {
        return client;
      }
    };
  }

  protected static UUID id(long n) {
    return new UUID(0L, n);
  }

  protected static OffsetDateTime utc(Instant value) {
    return OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
  }

  /** A problem row with the minimal required columns; PUBLISHED also gets published_at. */
  protected UUID problem(long n, String code, String title, String status, UUID level) {
    jdbc.sql(
            "INSERT INTO problem.problems(id,code,title,level_id,status,author_id,time_limit_ms,"
                + "memory_limit_mb,max_output_size_kb,published_at) VALUES(:id,:code,:title,:level,"
                + ":status,:author,1000,256,64,CASE WHEN :status='PUBLISHED' THEN now() END)")
        .param("id", id(n))
        .param("code", code)
        .param("title", title)
        .param("level", level)
        .param("status", status)
        .param("author", AUTHOR)
        .update();
    return id(n);
  }

  protected UUID published(long n, String code) {
    return problem(n, code, "Title " + code, "PUBLISHED", EASY);
  }

  protected void softDelete(UUID problem) {
    jdbc.sql("UPDATE problem.problems SET status='UNPUBLISHED', deleted=true WHERE id=:id")
        .param("id", problem)
        .update();
  }

  protected void link(UUID problem, UUID topic) {
    jdbc.sql("INSERT INTO problem.problem_topics(problem_id,topic_id) VALUES(:p,:t)")
        .param("p", problem)
        .param("t", topic)
        .update();
  }

  protected void stats(UUID problem, int submissions, int accepted) {
    jdbc.sql(
            "INSERT INTO problem.problem_stats(problem_id,submission_count,accepted_count,ac_rate)"
                + " VALUES(:p,:s,:a,round(CAST(:a AS numeric)*100.0/:s,2))")
        .param("p", problem)
        .param("s", submissions)
        .param("a", accepted)
        .update();
  }

  /** Position and payload text of a test testcase. */
  protected record Slot(int position, String text) {}

  protected static Slot at(int position, String text) {
    return new Slot(position, text);
  }

  /** An INLINE/INLINE testcase; visibility is SAMPLE or HIDDEN. */
  protected UUID testcase(UUID problem, long n, String visibility, boolean approved, Slot slot) {
    String text = slot.text();
    int position = slot.position();
    jdbc.sql(
            "INSERT INTO problem.testcases(id,problem_id,visibility,input_storage,input_inline,"
                + "expected_output_storage,expected_output_inline,approved,position)"
                + " VALUES(:id,:p,:v,'INLINE',:in,'INLINE',:out,:a,:pos)")
        .param("id", id(n))
        .param("p", problem)
        .param("v", visibility)
        .param("in", "in-" + text)
        .param("out", "out-" + text)
        .param("a", approved)
        .param("pos", position)
        .update();
    return id(n);
  }

  protected void assignment(UUID problem, UUID clazz, boolean removed) {
    jdbc.sql(
            "INSERT INTO problem.class_assignments(id,problem_id,class_id,assigned_by,removed_at)"
                + " VALUES(gen_random_uuid(),:p,:c,:by,CASE WHEN :removed THEN now() END)")
        .param("p", problem)
        .param("c", clazz)
        .param("by", AUTHOR)
        .param("removed", removed)
        .update();
  }

  protected int count(String table) {
    return jdbc.sql("SELECT count(*) FROM problem." + table).query(Integer.class).single();
  }

  protected int count(String table, String where) {
    return jdbc.sql("SELECT count(*) FROM problem." + table + " WHERE " + where)
        .query(Integer.class)
        .single();
  }
}
