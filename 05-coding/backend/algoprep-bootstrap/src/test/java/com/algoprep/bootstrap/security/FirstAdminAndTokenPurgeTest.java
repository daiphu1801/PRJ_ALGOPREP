package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.algoprep.identity.application.ports.in.BootstrapFirstAdminUseCase;
import com.algoprep.identity.application.ports.in.PurgePendingAccountsUseCase;
import com.algoprep.identity.application.ports.in.PurgeRefreshTokensUseCase;
import com.algoprep.identity.domain.ports.out.RefreshTokenRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/** JOB-ID-01 (BR-23) and JOB-ID-04 against the real schema. Needs Docker. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,"
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=2"
    })
@ActiveProfiles("test")
@Testcontainers
class FirstAdminAndTokenPurgeTest {
  private static final String PASSWORD = "Sup3rSecretPass";

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Autowired private BootstrapFirstAdminUseCase bootstrap;
  @Autowired private PurgeRefreshTokensUseCase purge;
  @Autowired private PurgePendingAccountsUseCase pendingPurge;
  @Autowired private RefreshTokenRepository tokens;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void clean() {
    jdbc.update("DELETE FROM identity.refresh_tokens");
    jdbc.update(
        "DELETE FROM identity.users WHERE role_id IN"
            + " (SELECT id FROM identity.roles WHERE base_category = 'ADMIN')");
    jdbc.update("DELETE FROM identity.users WHERE email LIKE '%@first-admin.test'");
  }

  @Test
  void createsPendingAdminOnceAndIgnoresLaterConfigurationChanges() {
    bootstrap.bootstrap("Root@First-Admin.test", PASSWORD);

    Map<String, Object> row = adminRow();
    assertThat(row)
        .containsEntry("email", "root@first-admin.test")
        .containsEntry("status", "PENDING_VERIFICATION")
        .containsEntry("must_change_password", true)
        .containsEntry("role_code", "ADMIN");
    String hash = (String) row.get("password_hash");
    assertThat(new BCryptPasswordEncoder().matches(PASSWORD, hash)).isTrue();

    bootstrap.bootstrap("other@first-admin.test", "Different1Pass");
    bootstrap.bootstrap(null, null);

    assertThat(adminRow()).containsEntry("password_hash", hash);
    assertThat(count("SELECT count(*) FROM identity.users WHERE email LIKE '%@first-admin.test'"))
        .isEqualTo(1);
    assertThat(count("SELECT count(*) FROM identity.system_audit_logs")).isZero();
  }

  @Test
  void missingConfigurationWithoutAdminStopsStartup() {
    assertThatThrownBy(() -> bootstrap.bootstrap("root@first-admin.test", ""))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ALGOPREP_BOOTSTRAP_ADMIN_PASSWORD");
    assertThat(count("SELECT count(*) FROM identity.users WHERE email LIKE '%@first-admin.test'"))
        .isZero();
  }

  @Test
  void emailOwnedByNonAdminAccountStopsStartup() {
    insertUser("student@first-admin.test", "STUDENT");
    assertThatThrownBy(() -> bootstrap.bootstrap("student@first-admin.test", PASSWORD))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("non-ADMIN");
  }

  @Test
  void purgeRemovesOnlyDeadTokensOlderThanSixMonthsInBatches() {
    UUID user = insertUser("tokens@first-admin.test", "STUDENT");
    token(user, "7 months", "-6 months", true); // old, revoked: delete
    token(user, "7 months", "-6 months", false); // old, expired: delete
    token(user, "7 months", "+10 days", false); // old but still usable: keep
    token(user, "1 day", "+29 days", true); // recent, revoked: keep (retention chart)
    token(user, "5 months", "-4 months", false); // expired but inside 6 months: keep

    assertThat(tokens.deleteExpiredBatch(1)).isEqualTo(1);
    assertThat(purge.purge()).isEqualTo(1);
    assertThat(purge.purge()).isZero();
    assertThat(count("SELECT count(*) FROM identity.refresh_tokens")).isEqualTo(3);
  }

  @Test
  void pendingPurgeRemovesOnlyOldSelfRegisteredAccountsAndTheirRows() {
    UUID stale = pending("stale@first-admin.test", "8 days", false);
    UUID fresh = pending("fresh@first-admin.test", "1 day", false);
    UUID adminMade = pending("admin-made@first-admin.test", "30 days", true);
    UUID active = insertUser("active@first-admin.test", "STUDENT");
    jdbc.update(
        "UPDATE identity.users SET created_at = now() - interval '30 days' WHERE id = ?", active);
    jdbc.update("INSERT INTO identity.user_preferences (user_id) VALUES (?)", stale);
    token(stale, "8 days", "+1 days", false);

    assertThat(pendingPurge.purge()).isEqualTo(1);

    assertThat(count("SELECT count(*) FROM identity.users WHERE id = '" + stale + "'::uuid"))
        .isZero();
    assertThat(count("SELECT count(*) FROM identity.user_preferences")).isZero();
    assertThat(count("SELECT count(*) FROM identity.refresh_tokens")).isZero();
    assertThat(
            count(
                "SELECT count(*) FROM identity.users WHERE id IN ('"
                    + fresh
                    + "'::uuid, '"
                    + adminMade
                    + "'::uuid, '"
                    + active
                    + "'::uuid)"))
        .isEqualTo(3);
  }

  private UUID pending(String email, String age, boolean adminCreated) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users (id, email, display_name, role_id, status,"
            + " must_change_password, created_at) SELECT ?, ?, 'Test', id, 'PENDING_VERIFICATION',"
            + " ?, now() - CAST(? AS interval) FROM identity.roles"
            + " WHERE code = 'STUDENT' AND is_system = true",
        id,
        email,
        adminCreated,
        age);
    return id;
  }

  private Map<String, Object> adminRow() {
    List<Map<String, Object>> rows =
        jdbc.queryForList(
            "SELECT u.email, u.status, u.must_change_password, u.password_hash, r.code AS role_code"
                + " FROM identity.users u JOIN identity.roles r ON r.id = u.role_id"
                + " WHERE r.base_category = 'ADMIN'");
    assertThat(rows).hasSize(1);
    return rows.get(0);
  }

  private UUID insertUser(String email, String roleCode) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO identity.users (id, email, display_name, role_id, status)"
            + " SELECT ?, ?, 'Test', id, 'ACTIVE' FROM identity.roles"
            + " WHERE code = ? AND is_system = true",
        id,
        email,
        roleCode);
    return id;
  }

  private void token(UUID user, String issuedAgo, String expiresOffset, boolean revoked) {
    jdbc.update(
        "INSERT INTO identity.refresh_tokens"
            + " (id, user_id, token_hash, family_id, issued_at, expires_at, revoked_at)"
            + " VALUES (?, ?, ?, ?, now() - CAST(? AS interval), now() + CAST(? AS interval),"
            + " CASE WHEN ? THEN now() ELSE NULL END)",
        UUID.randomUUID(),
        user,
        UUID.randomUUID().toString(),
        UUID.randomUUID(),
        issuedAgo,
        expiresOffset,
        revoked);
  }

  private int count(String sql) {
    Integer value = jdbc.queryForObject(sql, Integer.class);
    return value == null ? 0 : value;
  }
}
