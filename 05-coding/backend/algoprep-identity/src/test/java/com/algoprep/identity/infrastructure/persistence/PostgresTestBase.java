package com.algoprep.identity.infrastructure.persistence;

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
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real PostgreSQL for the identity repository tests. One container is started per test JVM and
 * shared by every subclass. Before each test the {@code identity} schema is dropped and rebuilt
 * from the module's own migrations (V1.001 to V1.004, in order, no Flyway), so every test starts
 * from the seeded roles, functions, actions and the 132-cell default matrix.
 *
 * <p>Every table the identity queries touch is owned by identity (read models carry no foreign keys
 * to other contexts, BD F1 section 1.12), so no stand-in tables are needed.
 */
abstract class PostgresTestBase {
  private static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine").withDatabaseName("algoprep");
  private static final DataSource DATA;
  private static final String[] MIGRATIONS = {
    "V1.001__init.sql",
    "V1.002__processed_domain_events.sql",
    "V1.003__class_event_outbox.sql",
    "V1.004__instructor_interview_bank_grant.sql",
    "V1.005__session_revocations.sql",
    "V1.006__user_avatars.sql"
  };

  static {
    POSTGRES.start();
    DATA =
        new DriverManagerDataSource(
            POSTGRES.getJdbcUrl() + "&currentSchema=public",
            POSTGRES.getUsername(),
            POSTGRES.getPassword());
  }

  protected final DataSource data = DATA;
  protected final JdbcClient jdbc = JdbcClient.create(DATA);
  protected final TransactionTemplate tx =
      new TransactionTemplate(new DataSourceTransactionManager(DATA));

  @BeforeEach
  void rebuildSchema() throws SQLException {
    try (Connection connection = DATA.getConnection()) {
      connection.createStatement().execute("DROP SCHEMA IF EXISTS identity CASCADE");
      connection.createStatement().execute("CREATE SCHEMA identity");
      for (String name : MIGRATIONS) {
        connection.setAutoCommit(false);
        ScriptUtils.executeSqlScript(
            connection,
            new EncodedResource(new ClassPathResource("db/migration/identity/" + name)));
        connection.commit();
        connection.setAutoCommit(true);
      }
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

  protected UUID roleId(String code) {
    return jdbc.sql("SELECT id FROM identity.roles WHERE code = :c")
        .param("c", code)
        .query(UUID.class)
        .single();
  }

  protected UUID functionId(String code) {
    return jdbc.sql("SELECT id FROM identity.functions WHERE code = :c")
        .param("c", code)
        .query(UUID.class)
        .single();
  }

  protected UUID actionId(String code) {
    return jdbc.sql("SELECT id FROM identity.actions WHERE code = :c")
        .param("c", code)
        .query(UUID.class)
        .single();
  }

  /** Inserts a user of the given system role with the given status; returns its id. */
  protected UUID user(String email, String roleCode, String status) {
    var id = UUID.randomUUID();
    jdbc.sql(
            "INSERT INTO identity.users(id,email,display_name,role_id,status)"
                + " VALUES(:id,:email,:name,:role,:status)")
        .param("id", id)
        .param("email", email)
        .param("name", "Name of " + email)
        .param("role", roleId(roleCode))
        .param("status", status)
        .update();
    return id;
  }

  /** Inserts an ACTIVE user of the given system role. */
  protected UUID user(String email, String roleCode) {
    return user(email, roleCode, "ACTIVE");
  }

  protected UUID classOf(UUID instructor, String name) {
    var id = UUID.randomUUID();
    jdbc.sql("INSERT INTO identity.classes(id,instructor_id,name) VALUES(:id,:i,:n)")
        .param("id", id)
        .param("i", instructor)
        .param("n", name)
        .update();
    return id;
  }

  protected int count(String table) {
    return jdbc.sql("SELECT count(*) FROM identity." + table).query(Integer.class).single();
  }

  /** SQLSTATE of the nearest SQLException in the cause chain. */
  protected static String sqlState(DataAccessException failure) {
    for (Throwable t = failure; t != null; t = t.getCause()) {
      if (t instanceof SQLException sql) {
        return sql.getSQLState();
      }
    }
    return null;
  }
}
