package com.algoprep.bootstrap.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.in.PurgeExpiredAuditLogsUseCase;
import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

/**
 * #51, #52 and JOB-ID-05 against real PostgreSQL: read and edit of the retention settings, the
 * Function gate, the audit row, and the purge of expired audit rows. Needs Docker.
 */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.data.redis.autoconfigure.RedisAutoConfiguration,"
          + "org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=4"
    })
@ActiveProfiles("test")
@Testcontainers
class LogRetentionFlowTest {
  private static final String PATH = "/api/v1/identity/admin/settings/log-retention";
  private static final String AUDIT_ACTION = "AUDIT_LOG_SETTINGS_CHANGED";

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Autowired private WebApplicationContext context;
  @Autowired private AccessTokenPort tokens;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private PurgeExpiredAuditLogsUseCase purge;

  private MockMvc mvc;
  private UUID adminId;
  private String admin;

  @BeforeEach
  void setUp() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    adminId = UUID.randomUUID();
    admin = tokenFor(adminId, "ADMIN", BaseCategory.ADMIN);
    jdbc.update("DELETE FROM identity.system_audit_logs");
    jdbc.update(
        "UPDATE identity.system_settings SET value_json = '90'::jsonb"
            + " WHERE key = 'audit_log.retention_days'");
    jdbc.update(
        "UPDATE identity.system_settings SET value_json = '\"DELETE\"'::jsonb"
            + " WHERE key = 'audit_log.expired_policy'");
  }

  @Test
  void readReturnsTheSeededDefaultsAndReflectsAnEdit() throws Exception {
    MockHttpServletResponse first = call(get(PATH), admin);
    assertThat(first.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Integer>read(first.getContentAsString(), "$.retentionDays")).isEqualTo(90);
    assertThat(JsonPath.<String>read(first.getContentAsString(), "$.expiredPolicy"))
        .isEqualTo("DELETE");

    MockHttpServletResponse saved =
        edit(admin, "{\"retentionDays\":30,\"expiredPolicy\":\"ARCHIVE\"}");
    assertThat(saved.getStatus()).isEqualTo(200);
    assertThat(JsonPath.<Integer>read(saved.getContentAsString(), "$.retentionDays")).isEqualTo(30);

    MockHttpServletResponse again = call(get(PATH), admin);
    assertThat(JsonPath.<Integer>read(again.getContentAsString(), "$.retentionDays")).isEqualTo(30);
    assertThat(JsonPath.<String>read(again.getContentAsString(), "$.expiredPolicy"))
        .isEqualTo("ARCHIVE");
    assertThat(
            jdbc.queryForObject(
                "SELECT updated_by FROM identity.system_settings"
                    + " WHERE key = 'audit_log.retention_days'",
                UUID.class))
        .isEqualTo(adminId);
  }

  @Test
  void editWritesExactlyOneAuditRowWithTheOldAndNewValues() throws Exception {
    assertThat(edit(admin, "{\"retentionDays\":7,\"expiredPolicy\":\"DELETE\"}").getStatus())
        .isEqualTo(200);

    assertThat(auditRows()).isEqualTo(1);
    var row =
        jdbc.queryForMap(
            "SELECT actor_user_id, target_type, before_json ->> 'retentionDays' AS old_days,"
                + " after_json ->> 'retentionDays' AS new_days,"
                + " after_json ->> 'expiredPolicy' AS new_policy"
                + " FROM identity.system_audit_logs WHERE action_type = ?",
            AUDIT_ACTION);
    assertThat(row.get("actor_user_id")).isEqualTo(adminId);
    assertThat(row.get("target_type")).isEqualTo("AUDIT_LOG_SETTINGS");
    assertThat(row.get("old_days")).isEqualTo("90");
    assertThat(row.get("new_days")).isEqualTo("7");
    assertThat(row.get("new_policy")).isEqualTo("DELETE");
  }

  @Test
  void invalidBodiesAre422Idt442AndChangeNothing() throws Exception {
    String[] bodies = {
      "{\"retentionDays\":0,\"expiredPolicy\":\"DELETE\"}",
      "{\"retentionDays\":-5,\"expiredPolicy\":\"DELETE\"}",
      "{\"retentionDays\":\"30\",\"expiredPolicy\":\"DELETE\"}",
      "{\"retentionDays\":1.5,\"expiredPolicy\":\"DELETE\"}",
      "{\"retentionDays\":99999999999999999999,\"expiredPolicy\":\"DELETE\"}",
      "{\"retentionDays\":2000000,\"expiredPolicy\":\"DELETE\"}",
      "{\"expiredPolicy\":\"DELETE\"}",
      "{\"retentionDays\":30}",
      "{\"retentionDays\":30,\"expiredPolicy\":\"KEEP\"}"
    };
    for (String body : bodies) {
      MockHttpServletResponse response = edit(admin, body);
      assertThat(response.getStatus()).as(body).isEqualTo(422);
      assertThat(response.getContentAsString()).as(body).contains("\"errorCode\":\"IDT-442\"");
    }
    assertThat(auditRows()).isZero();
    assertThat(storedDays()).isEqualTo(90);
  }

  @Test
  void oneDayIsAccepted() throws Exception {
    assertThat(edit(admin, "{\"retentionDays\":1,\"expiredPolicy\":\"DELETE\"}").getStatus())
        .isEqualTo(200);
    assertThat(storedDays()).isEqualTo(1);
  }

  @Test
  void nonAdminsAreRefusedOnBothEndpointsAndNoTokenIs401() throws Exception {
    for (BaseCategory category :
        new BaseCategory[] {BaseCategory.STUDENT, BaseCategory.INSTRUCTOR}) {
      String token = tokenFor(UUID.randomUUID(), category.name(), category);
      assertForbidden(call(get(PATH), token));
      assertForbidden(edit(token, "{\"retentionDays\":30,\"expiredPolicy\":\"DELETE\"}"));
    }
    assertThat(mvc.perform(get(PATH)).andReturn().getResponse().getStatus()).isEqualTo(401);
    assertThat(storedDays()).isEqualTo(90);
    assertThat(auditRows()).isZero();
  }

  @Test
  void adminWithoutTheFunctionCellCanReadButNotEdit() throws Exception {
    setCell(false);
    try {
      assertThat(call(get(PATH), admin).getStatus()).isEqualTo(200);
      assertForbidden(edit(admin, "{\"retentionDays\":30,\"expiredPolicy\":\"DELETE\"}"));
      assertThat(storedDays()).isEqualTo(90);
      assertThat(auditRows()).isZero();
    } finally {
      setCell(true);
    }
    assertThat(edit(admin, "{\"retentionDays\":30,\"expiredPolicy\":\"DELETE\"}").getStatus())
        .isEqualTo(200);
  }

  @Test
  void purgeDeletesOnlyRowsOlderThanTheThresholdAndRepeatsAsANoOp() {
    jdbc.update(
        "UPDATE identity.system_settings SET value_json = '10'::jsonb"
            + " WHERE key = 'audit_log.retention_days'");
    seedRows(3, "clock_timestamp() - interval '11 days'");
    seedRows(2, "clock_timestamp() - interval '9 days'");
    seedRows(1, "clock_timestamp()");

    assertThat(purge.purge()).isEqualTo(3);
    assertThat(auditRows()).isEqualTo(3);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.system_audit_logs"
                    + " WHERE created_at < clock_timestamp() - interval '10 days'",
                Integer.class))
        .isZero();
    assertThat(purge.purge()).isZero();
    assertThat(auditRows()).isEqualTo(3);
  }

  @Test
  void purgeRunsInBatchesAndStopsAtTheCapPerRun() {
    seedRows(5_001, "clock_timestamp() - interval '100 days'");
    assertThat(purge.purge()).isEqualTo(5_001);

    seedRows(100_005, "clock_timestamp() - interval '100 days'");
    assertThat(purge.purge()).isEqualTo(100_000);
    assertThat(auditRows()).isEqualTo(5);
    assertThat(purge.purge()).isEqualTo(5);
    assertThat(purge.purge()).isZero();
  }

  @Test
  void archivePolicyKeepsEveryRow() {
    jdbc.update(
        "UPDATE identity.system_settings SET value_json = '\"ARCHIVE\"'::jsonb"
            + " WHERE key = 'audit_log.expired_policy'");
    seedRows(4, "clock_timestamp() - interval '400 days'");

    assertThat(purge.purge()).isZero();
    assertThat(auditRows()).isEqualTo(4);
  }

  private MockHttpServletResponse edit(String token, String body) throws Exception {
    return call(put(PATH).contentType(MediaType.APPLICATION_JSON).content(body), token);
  }

  private MockHttpServletResponse call(
      org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request,
      String token)
      throws Exception {
    return mvc.perform(request.header("Authorization", "Bearer " + token))
        .andReturn()
        .getResponse();
  }

  private static void assertForbidden(MockHttpServletResponse response) throws Exception {
    assertThat(response.getStatus()).isEqualTo(403);
    assertThat(response.getContentAsString()).contains("\"errorCode\":\"IDT-445\"");
  }

  private int auditRows() {
    return jdbc.queryForObject("SELECT count(*) FROM identity.system_audit_logs", Integer.class);
  }

  private int storedDays() {
    return jdbc.queryForObject(
        "SELECT (value_json #>> '{}')::int FROM identity.system_settings"
            + " WHERE key = 'audit_log.retention_days'",
        Integer.class);
  }

  private void seedRows(int count, String createdAt) {
    jdbc.update(
        "INSERT INTO identity.system_audit_logs (id, actor_user_id, action_type, target_type,"
            + " created_at) SELECT gen_random_uuid(), ?, 'TEST_SEED', 'TEST', "
            + createdAt
            + " FROM generate_series(1, ?)",
        adminId,
        count);
  }

  /** Grants or revokes the ADMIN cell {@code SYSTEM_AUDIT_LOG:UPDATE} and bumps the version. */
  private void setCell(boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted = ? FROM identity.roles r, identity.functions f,"
            + " identity.actions a WHERE r.id = p.role_id AND f.id = p.function_id AND a.id ="
            + " p.action_id AND r.code = 'ADMIN' AND f.code = 'SYSTEM_AUDIT_LOG' AND a.code ="
            + " 'UPDATE'",
        granted);
    jdbc.update(
        "UPDATE identity.roles SET permission_version = permission_version + 1"
            + " WHERE code = 'ADMIN'");
  }

  private String tokenFor(UUID userId, String roleCode, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users (id, email, display_name, role_id, status)"
            + " SELECT ?, ?, ?, r.id, 'ACTIVE' FROM identity.roles r WHERE r.code = ?",
        userId,
        userId + "@test.local",
        "User " + roleCode,
        roleCode);
    return tokens.issue(userId, category, roleCode, UUID.randomUUID()).value();
  }
}
