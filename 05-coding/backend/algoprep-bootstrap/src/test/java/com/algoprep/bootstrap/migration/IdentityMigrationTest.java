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
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Applies the real Flyway migrations of module {@code identity} to a clean PostgreSQL 16 started by
 * Testcontainers, then checks tables, seed data and key constraints against {@code
 * 02-bd/database/F1_identity.md}.
 *
 * <p>Schemas come from {@code database/init/01-schemas.sql}, the same file docker-compose mounts,
 * so there is one source of truth. Needs Docker (01-rd/system/SYS0301_environment.md section 3).
 */
@SpringBootTest(
    properties = {
      // The `test` profile excludes the DataSource and Flyway wiring; keep only Redis and RabbitMQ
      // out
      // so Flyway runs against the container.
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,"
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=2"
    })
@ActiveProfiles("test")
@Testcontainers
class IdentityMigrationTest {

  private static final List<String> EXPECTED_TABLES =
      List.of(
          "roles",
          "functions",
          "actions",
          "users",
          "permissions",
          "user_preferences",
          "oauth_identities",
          "refresh_tokens",
          "classes",
          "class_invite_codes",
          "class_enrollments",
          "system_audit_logs",
          "system_settings",
          "user_problem_best_score",
          "user_submission_stats",
          "user_daily_submission_stats",
          "identity_recent_activity");

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
  @DisplayName("creates exactly the 17 identity tables from BD section 1")
  void createsAllTables() {
    List<String> tables =
        jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'identity'",
            String.class);
    assertThat(tables).containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
  }

  @Test
  @DisplayName("seeds 3 system roles, 11 functions, 4 actions and 2 settings")
  void seedsFixedData() {
    assertThat(count("identity.roles WHERE is_system")).isEqualTo(3);
    assertThat(count("identity.functions")).isEqualTo(11);
    assertThat(count("identity.actions")).isEqualTo(4);
    assertThat(
            jdbc.queryForObject(
                "SELECT value_json::text FROM identity.system_settings"
                    + " WHERE key = 'audit_log.retention_days'",
                String.class))
        .isEqualTo("90");
    assertThat(count("identity.system_settings")).isEqualTo(2);
  }

  @Test
  @DisplayName("seeds the default permission matrix: 132 cells, ADMIN all, INSTRUCTOR 3 functions")
  void seedsPermissionMatrix() {
    assertThat(count("identity.permissions")).isEqualTo(132);
    assertThat(granted("ADMIN")).isEqualTo(44);
    assertThat(granted("INSTRUCTOR")).isEqualTo(12);
    assertThat(granted("STUDENT")).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.permissions p"
                    + " JOIN identity.roles r ON r.id = p.role_id"
                    + " JOIN identity.functions f ON f.id = p.function_id"
                    + " WHERE r.code = 'INSTRUCTOR' AND p.granted"
                    + " AND f.code IN ('CLASS_MANAGEMENT','PROBLEM_AUTHORING','TESTCASE_MANAGEMENT')",
                Integer.class))
        .isEqualTo(12);
  }

  @Test
  @DisplayName("email is unique ignoring case")
  void emailUniqueIgnoringCase() {
    UUID role = roleId("STUDENT");
    insertUser("Dup@Example.com", role, "ACTIVE", null);
    assertThatThrownBy(() -> insertUser("dup@example.COM", role, "ACTIVE", null))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("only a self-deleted account may be anonymized")
  void anonymizedRequiresSelfDeletion() {
    UUID role = roleId("STUDENT");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO identity.users (id, email, display_name, role_id, status,"
                        + " deactivation_source, anonymized_at)"
                        + " VALUES (?, 'anon@example.com', 'X', ?, 'DEACTIVATED', 'ADMIN', now())",
                    UUID.randomUUID(),
                    role))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("a role that still has users cannot be deleted")
  void roleWithUsersIsRestricted() {
    UUID role = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.roles (id, code, name, base_category)"
            + " VALUES (?, 'CUSTOM_TA', 'TA', 'STUDENT')",
        role);
    insertUser("ta@example.com", role, "ACTIVE", null);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM identity.roles WHERE id = ?", role))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("migrating again applies nothing and history shows version 1.001 as success")
  void isIdempotent() {
    assertThat(flyway.migrate().migrationsExecuted).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT success FROM public.flyway_schema_history WHERE version = '1.001'",
                Boolean.class))
        .isTrue();
  }

  private UUID roleId(String code) {
    return jdbc.queryForObject("SELECT id FROM identity.roles WHERE code = ?", UUID.class, code);
  }

  private int count(String fromAndWhere) {
    return jdbc.queryForObject("SELECT count(*) FROM " + fromAndWhere, Integer.class);
  }

  private int granted(String roleCode) {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.permissions p JOIN identity.roles r ON r.id = p.role_id"
            + " WHERE r.code = ? AND p.granted",
        Integer.class,
        roleCode);
  }

  private void insertUser(String email, UUID roleId, String status, String deactivationSource) {
    jdbc.update(
        "INSERT INTO identity.users (id, email, display_name, role_id, status, deactivation_source)"
            + " VALUES (?, ?, 'Test', ?, ?, ?)",
        UUID.randomUUID(),
        email,
        roleId,
        status,
        deactivationSource);
  }
}
