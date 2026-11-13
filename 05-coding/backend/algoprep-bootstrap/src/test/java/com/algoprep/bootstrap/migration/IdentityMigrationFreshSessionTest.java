package com.algoprep.bootstrap.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Regression for the V1.004 outage: it used unqualified table names, the app failed to start on a
 * fresh database, yet {@link IdentityMigrationTest} stayed green because {@code 01-schemas.sql}
 * sets a database-level {@code search_path} that only the docker-compose database has. Here that
 * setting is reset and each identity migration runs in its own brand-new session, so a file that
 * depends on a {@code search_path} it did not set itself fails.
 */
@Testcontainers
class IdentityMigrationFreshSessionTest {

  @Container
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  private static final Path MIGRATIONS =
      Path.of("../algoprep-identity/src/main/resources/db/migration/identity");

  @Test
  @DisplayName(
      "every identity migration applies on its own fresh session without relying on search_path")
  void eachFileAppliesOnFreshSession() throws IOException, SQLException {
    List<Path> files;
    try (Stream<Path> s = Files.list(MIGRATIONS)) {
      files = s.filter(p -> p.toString().endsWith(".sql")).sorted().toList();
    }
    assertThat(files).hasSizeGreaterThanOrEqualTo(4);

    try (Connection c =
            DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        Statement st = c.createStatement()) {
      st.execute("ALTER DATABASE algoprep RESET search_path"); // a database made by CREATE DATABASE
    }

    for (Path file : files) {
      String sql = Files.readString(file, StandardCharsets.UTF_8);
      try (Connection c =
              DriverManager.getConnection(
                  POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
          Statement st = c.createStatement()) {
        c.setAutoCommit(
            false); // Flyway runs each migration in one transaction (SET LOCAL needs it)
        try {
          st.execute(sql);
          c.commit();
        } catch (SQLException e) {
          throw new AssertionError(
              "Migration " + file.getFileName() + " failed: " + e.getMessage(), e);
        }
      }
    }
  }
}
