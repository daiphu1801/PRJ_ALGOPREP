package com.algoprep.bootstrap.judge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import com.algoprep.identity.application.ports.out.AccessTokenPort;
import com.algoprep.identity.domain.model.BaseCategory;
import com.algoprep.judge.application.dto.WorkerScaling;
import com.algoprep.judge.domain.ports.out.AdminQueueRepository;
import com.algoprep.judge.domain.ports.out.JudgeMaintenanceRepository;
import com.algoprep.judge.domain.ports.out.WorkerJobRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/** Real current JWT authorities, PostgreSQL kernel/locks and REQUIRED identity audit. */
@SpringBootTest(
    properties = {
      "spring.autoconfigure.exclude=org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration",
      "spring.datasource.hikari.maximum-pool-size=6"
    })
@ActiveProfiles("test")
@Testcontainers
class AdminQueueControlFlowTest {
  private static final String BASE = "/api/v1/judge/admin/queue";
  private static final String DEFAULTS =
      "{\"autoscaleJobThreshold\":20,\"jobsPerPage\":20,\"errorRetentionHours\":24,\"latencyCapSeconds\":10}";

  @Container @ServiceConnection
  static final PostgreSQLContainer POSTGRES =
      new PostgreSQLContainer("postgres:16.15-alpine")
          .withDatabaseName("algoprep")
          .withCopyFileToContainer(
              MountableFile.forHostPath("../../database/init/01-schemas.sql"),
              "/docker-entrypoint-initdb.d/01-schemas.sql");

  @Container
  static final GenericContainer<?> REDIS =
      new GenericContainer<>("redis:7.4-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void infrastructure(DynamicPropertyRegistry p) {
    p.add("spring.data.redis.host", REDIS::getHost);
    p.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired AccessTokenPort tokens;
  @Autowired WebApplicationContext context;
  @Autowired WorkerJobRepository worker;
  @Autowired JudgeMaintenanceRepository maintenance;
  @MockitoSpyBean AdminQueueRepository repository;
  private final JsonMapper json = JsonMapper.builder().build();
  private MockMvc mvc;
  private UUID adminId;
  private String admin;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    adminId = UUID.randomUUID();
    admin = token(adminId, BaseCategory.ADMIN);
    jdbc.update(
        "UPDATE judge.cluster_settings SET queue_paused=false,autoscale_enabled=true,autoscale_job_threshold=20,jobs_per_page=20,error_retention_hours=24,latency_cap_seconds=10,updated_at=clock_timestamp(),updated_by=NULL WHERE id=1");
    setRead(true);
    setCell("UPDATE", true);
  }

  private String token(UUID id, BaseCategory category) {
    jdbc.update(
        "INSERT INTO identity.users(id,email,display_name,role_id,status) SELECT ?,?,'Queue user',id,'ACTIVE' FROM identity.roles WHERE code=?",
        id,
        id + "@example.test",
        category.name());
    return tokens.issue(id, category, category.name(), UUID.randomUUID()).value();
  }

  private void setRead(boolean granted) {
    setCell("READ", granted);
  }

  private void setCell(String action, boolean granted) {
    jdbc.update(
        "UPDATE identity.permissions p SET granted=? FROM identity.roles r,identity.functions f,identity.actions a WHERE p.role_id=r.id AND p.function_id=f.id AND p.action_id=a.id AND r.code='ADMIN' AND f.code='JUDGE_QUEUE_MONITOR' AND a.code=?",
        granted,
        action);
    jdbc.update(
        "UPDATE identity.roles SET permission_version=permission_version+1 WHERE code='ADMIN'");
  }

  private MockHttpServletResponse response(
      String method, String endpoint, String bearer, String body) throws Exception {
    var request = method.equals("GET") ? get(BASE + endpoint) : put(BASE + endpoint);
    if (bearer != null) {
      request.header("Authorization", "Bearer " + bearer);
    }
    if (body != null) {
      request.contentType(MediaType.APPLICATION_JSON).content(body);
    }
    return mvc.perform(request).andReturn().getResponse();
  }

  private JsonNode send(String method, String endpoint, String bearer, String body, int expected)
      throws Exception {
    var response = response(method, endpoint, bearer, body);
    assertThat(response.getStatus()).as(response.getContentAsString()).isEqualTo(expected);
    return json.readTree(response.getContentAsString());
  }

  private JsonNode save(String endpoint, String body, int expected) throws Exception {
    return send("PUT", endpoint, admin, body, expected);
  }

  private Map<String, Object> cluster() {
    return jdbc.queryForMap("SELECT * FROM judge.cluster_settings WHERE id=1");
  }

  private long audits() {
    return jdbc.queryForObject(
        "SELECT count(*) FROM identity.system_audit_logs WHERE actor_user_id=? AND action_type LIKE 'QUEUE_%'",
        Long.class, adminId);
  }

  private Instant databaseNow() {
    return jdbc.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class).toInstant();
  }

  private UUID submission(String status) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO judge.submissions(id,user_id,problem_id,language,submission_model,source_code,source_input_method,status,testcase_set_version_at_grading,updated_at,finished_at) VALUES(?,?,?,'JAVA','STANDARD_IO','private-queue-source','EDITOR',?,1,clock_timestamp()-interval '15 minutes',CASE WHEN ?='ACCEPTED' THEN clock_timestamp() ELSE NULL END)",
        id,
        adminId,
        UUID.randomUUID(),
        status,
        status);
    return id;
  }

  @Test
  void settingsResponseIsCurrentAndContainsOnlyTheFiveContractFields() throws Exception {
    var value = send("GET", "/settings", admin, null, 200);
    assertThat(value.properties().stream().map(Map.Entry::getKey))
        .containsExactlyInAnyOrder(
            "autoscaleJobThreshold",
            "jobsPerPage",
            "errorRetentionHours",
            "latencyCapSeconds",
            "updatedAt");
    assertThat(value.path("autoscaleJobThreshold").asInt()).isEqualTo(20);
    assertThat(value.path("jobsPerPage").asInt()).isEqualTo(20);
    assertThat(value.path("errorRetentionHours").asInt()).isEqualTo(24);
    assertThat(value.path("latencyCapSeconds").asInt()).isEqualTo(10);
    assertThat(Instant.parse(value.path("updatedAt").asText())).isNotNull();
    assertThat(audits()).isZero();
  }

  @Test
  void pauseAndResumeRefreshOnlyPendingAndShareTheActorAuditTransaction() throws Exception {
    UUID pending = submission("PENDING");
    UUID judging = submission("JUDGING");
    UUID accepted = submission("ACCEPTED");
    var oldPending = jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", pending);
    var oldJudging = jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", judging);
    var oldAccepted = jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", accepted);
    var paused = save("/consumption", "{\"paused\":true,\"expectedPaused\":false}", 200);
    assertThat(paused.properties().stream().map(Map.Entry::getKey))
        .containsExactlyInAnyOrder("paused", "updatedAt");
    assertThat(paused.path("paused").asBoolean()).isTrue();
    assertThat(worker.control().paused()).isTrue();
    assertThat(maintenance.staleCandidates(100, 300)).doesNotContain(pending).contains(judging);
    assertThat(jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", pending))
        .isEqualTo(oldPending);
    Instant start = databaseNow();
    var resumed = save("/consumption", "{\"paused\":false,\"expectedPaused\":true}", 200);
    assertThat(resumed.path("paused").asBoolean()).isFalse();
    Instant refreshed =
        jdbc.queryForObject(
                "SELECT updated_at FROM judge.submissions WHERE id=?",
                OffsetDateTime.class,
                pending)
            .toInstant();
    assertThat(refreshed).isBetween(start, databaseNow());
    assertThat(maintenance.staleCandidates(100, 300)).doesNotContain(pending);
    assertThat(jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", judging))
        .isEqualTo(oldJudging);
    assertThat(jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", accepted))
        .isEqualTo(oldAccepted);
    assertThat(save("/consumption", "{\"paused\":false,\"expectedPaused\":false}", 200))
        .isEqualTo(resumed);
    assertThat(
            jdbc.queryForObject(
                    "SELECT updated_at FROM judge.submissions WHERE id=?",
                    OffsetDateTime.class,
                    pending)
                .toInstant())
        .isEqualTo(refreshed);
    assertThat(cluster().get("updated_by")).isEqualTo(adminId);
    assertThat(audits()).isEqualTo(2);
    assertThat(worker.control().paused()).isFalse();
    var audit =
        jdbc.queryForList(
            "SELECT before_json::text,after_json::text FROM identity.system_audit_logs WHERE actor_user_id=? AND action_type='QUEUE_PAUSE_CHANGED'",
            adminId);
    assertThat(audit.toString())
        .contains("queuePaused", "true", "false")
        .doesNotContain("private-queue-source");
  }

  @Test
  void autoscaleAndThresholdAreVisibleToTheExistingWorkerPolicy() throws Exception {
    submission("PENDING");
    submission("PENDING");
    save("/settings", DEFAULTS.replace("Threshold\":20", "Threshold\":1"), 200);
    var scaling = new WorkerScaling();
    assertThat(worker.control().threshold()).isEqualTo(1);
    assertThat(scaling.poll(worker.control())).isEqualTo(1);
    assertThat(scaling.poll(worker.control())).isEqualTo(2);
    var stopped = save("/autoscale", "{\"enabled\":false,\"expectedEnabled\":true}", 200);
    assertThat(stopped.properties().stream().map(Map.Entry::getKey))
        .containsExactlyInAnyOrder("enabled", "updatedAt");
    assertThat(stopped.path("enabled").asBoolean()).isFalse();
    assertThat(worker.control().autoscale()).isFalse();
    assertThat(scaling.poll(worker.control())).isEqualTo(1);
    assertThat(save("/autoscale", "{\"enabled\":false,\"expectedEnabled\":false}", 200))
        .isEqualTo(stopped);
    var before = cluster();
    assertThat(
            save("/autoscale", "{\"enabled\":true,\"expectedEnabled\":true}", 409)
                .path("errorCode")
                .asText())
        .isEqualTo("JDG-201");
    assertThat(cluster()).isEqualTo(before);
    assertThat(audits()).isEqualTo(2);
  }

  @Test
  void settingsAuditOnlyChangedFieldsIgnoreReadonlyExtrasAndUseLastWriterWins() throws Exception {
    var before = cluster();
    var saved =
        save(
            "/settings",
            "{\"autoscaleJobThreshold\":20,\"jobsPerPage\":7,\"errorRetentionHours\":100000,\"latencyCapSeconds\":10,\"updatedAt\":\"stale\",\"queuePaused\":true,\"actorUserId\":\"forged\"}",
            200);
    assertThat(saved.path("jobsPerPage").asInt()).isEqualTo(7);
    assertThat(saved.path("errorRetentionHours").asInt()).isEqualTo(100000);
    assertThat(cluster().get("queue_paused")).isEqualTo(before.get("queue_paused"));
    assertThat(cluster().get("autoscale_enabled")).isEqualTo(before.get("autoscale_enabled"));
    assertThat(cluster().get("updated_by")).isEqualTo(adminId);
    String audit =
        jdbc.queryForObject(
            "SELECT after_json::text FROM identity.system_audit_logs WHERE actor_user_id=? AND action_type='QUEUE_SETTINGS_CHANGED'",
            String.class,
            adminId);
    assertThat(json.readTree(audit).properties().stream().map(Map.Entry::getKey))
        .containsExactlyInAnyOrder("jobsPerPage", "errorRetentionHours");
    assertThat(save("/settings", saved.toString(), 200)).isEqualTo(saved);
    assertThat(audits()).isEqualTo(1);
    var later = save("/settings", DEFAULTS, 200);
    assertThat(later.path("jobsPerPage").asInt()).isEqualTo(20);
    assertThat(audits()).isEqualTo(2);
  }

  @Test
  void allFlagAndSettingTypeErrorsLeaveDatabaseAndAuditUntouched() throws Exception {
    var before = cluster();
    for (String endpoint : List.of("/consumption", "/autoscale")) {
      for (String body :
          List.of(
              "",
              " ",
              "null",
              "{",
              "{} {}",
              "{}",
              "{\"paused\":true}",
              "{\"enabled\":false}",
              "{\"paused\":\"true\",\"expectedPaused\":false}",
              "{\"enabled\":0,\"expectedEnabled\":true}")) {
        assertThat(save(endpoint, body, 400).path("errorCode").asText()).isEqualTo("JDG-V16");
      }
    }
    for (String field :
        List.of(
            "autoscaleJobThreshold", "jobsPerPage", "errorRetentionHours", "latencyCapSeconds")) {
      for (String value : List.of("0", "-1", "1.5", "\"\"", "null", "2147483648")) {
        var body = (ObjectNode) json.readTree(DEFAULTS);
        body.set(field, json.readTree(value));
        assertThat(save("/settings", body.toString(), 400).path("errorCode").asText())
            .isEqualTo("JDG-V09");
      }
    }
    assertThat(cluster()).isEqualTo(before);
    assertThat(audits()).isZero();
  }

  @Test
  void anonymousWrongActorsAndRevokedReadAreDeniedButAdminWritesNeedNoFunctionCell()
      throws Exception {
    var before = cluster();
    send("GET", "/settings", null, null, 401);
    for (String endpoint : List.of("/settings", "/consumption", "/autoscale")) {
      send("PUT", endpoint, null, "{}", 401);
    }
    for (var role : List.of(BaseCategory.INSTRUCTOR, BaseCategory.STUDENT)) {
      String bearer = token(UUID.randomUUID(), role);
      assertThat(send("GET", "/settings", bearer, null, 403).path("errorCode").asText())
          .isEqualTo("JDG-301");
      for (String endpoint : List.of("/settings", "/consumption", "/autoscale")) {
        assertThat(
                send("PUT", endpoint, bearer, validWrite(endpoint), 403).path("errorCode").asText())
            .isEqualTo("JDG-301");
      }
    }
    assertThat(cluster()).isEqualTo(before);
    assertThat(audits()).isZero();
    try {
      setRead(false);
      setCell("UPDATE", false);
      assertThat(send("GET", "/settings", admin, null, 403).path("errorCode").asText())
          .isEqualTo("JDG-301");
      save("/settings", DEFAULTS.replace("PerPage\":20", "PerPage\":7"), 200);
      save("/consumption", "{\"paused\":true,\"expectedPaused\":false}", 200);
      save("/autoscale", "{\"enabled\":false,\"expectedEnabled\":true}", 200);
    } finally {
      setRead(true);
      setCell("UPDATE", true);
    }
    assertThat(audits()).isEqualTo(3);
  }

  private static String validWrite(String endpoint) {
    return switch (endpoint) {
      case "/settings" -> DEFAULTS.replace("PerPage\":20", "PerPage\":7");
      case "/consumption" -> "{\"paused\":true,\"expectedPaused\":false}";
      default -> "{\"enabled\":false,\"expectedEnabled\":true}";
    };
  }

  @Test
  void eachFlagSerializesTwoAdminsWithOneWinnerAndOneAudit() throws Exception {
    UUID secondId = UUID.randomUUID();
    String second = token(secondId, BaseCategory.ADMIN);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (String endpoint : List.of("/consumption", "/autoscale")) {
        String body =
            endpoint.equals("/consumption")
                ? "{\"paused\":true,\"expectedPaused\":false}"
                : "{\"enabled\":false,\"expectedEnabled\":true}";
        Callable<Integer> a = () -> response("PUT", endpoint, admin, body).getStatus();
        Callable<Integer> b = () -> response("PUT", endpoint, second, body).getStatus();
        var results = executor.invokeAll(List.of(a, b));
        assertThat(List.of(results.get(0).get(), results.get(1).get()))
            .containsExactlyInAnyOrder(200, 409);
      }
    }
    assertThat(cluster().get("queue_paused")).isEqualTo(true);
    assertThat(cluster().get("autoscale_enabled")).isEqualTo(false);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM identity.system_audit_logs WHERE actor_user_id IN (?,?) AND action_type LIKE 'QUEUE_%'",
                Integer.class, adminId, secondId))
        .isEqualTo(2);
  }

  @Test
  void failedAuditRollsBackEveryCommandIncludingResumedPendingHeartbeat() throws Exception {
    jdbc.update("UPDATE judge.cluster_settings SET queue_paused=true");
    UUID pending = submission("PENDING");
    var before = cluster();
    var oldPending = jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", pending);
    jdbc.execute(
        "CREATE FUNCTION identity.fail_queue_audit() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF NEW.action_type LIKE 'QUEUE_%' THEN RAISE EXCEPTION 'private-queue-audit-fault'; END IF; RETURN NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER fail_queue_audit BEFORE INSERT ON identity.system_audit_logs FOR EACH ROW EXECUTE FUNCTION identity.fail_queue_audit()");
    try {
      for (String endpoint : List.of("/consumption", "/autoscale", "/settings")) {
        String body =
            switch (endpoint) {
              case "/consumption" -> "{\"paused\":false,\"expectedPaused\":true}";
              case "/autoscale" -> "{\"enabled\":false,\"expectedEnabled\":true}";
              default -> DEFAULTS.replace("PerPage\":20", "PerPage\":7");
            };
        var error = save(endpoint, body, 503);
        assertThat(error.toString())
            .doesNotContain("private-queue-audit-fault", "SQL", "errorCode", "JDG-506", "JDG-507");
        assertThat(cluster()).isEqualTo(before);
        assertThat(jdbc.queryForMap("SELECT * FROM judge.submissions WHERE id=?", pending))
            .isEqualTo(oldPending);
        assertThat(audits()).isZero();
      }
    } finally {
      jdbc.execute("DROP TRIGGER fail_queue_audit ON identity.system_audit_logs");
      jdbc.execute("DROP FUNCTION identity.fail_queue_audit()");
    }
    save("/consumption", "{\"paused\":false,\"expectedPaused\":true}", 200);
    assertThat(audits()).isEqualTo(1);
  }

  @Test
  void failureAfterAuditRollsBackAuditAndDbErrorsNeverReturnFakeSettings() throws Exception {
    var before = cluster();
    doThrow(new IllegalStateException("private-read-fault")).when(repository).read();
    var error = send("GET", "/settings", admin, null, 503);
    assertThat(error.toString())
        .doesNotContain("private-read-fault", "autoscaleJobThreshold", "SQL");
    save("/autoscale", "{\"enabled\":false,\"expectedEnabled\":true}", 503);
    assertThat(cluster()).isEqualTo(before);
    assertThat(audits()).isZero();
  }

  @Test
  void settingsAcceptTheFullPositivePostgresIntRange() throws Exception {
    for (int value : new int[] {1, 100000, Integer.MAX_VALUE}) {
      var body = (ObjectNode) json.readTree(DEFAULTS);
      for (String field :
          List.of(
              "autoscaleJobThreshold", "jobsPerPage", "errorRetentionHours", "latencyCapSeconds")) {
        body.put(field, value);
      }
      var saved = save("/settings", body.toString(), 200);
      assertThat(saved.path("jobsPerPage").asInt()).isEqualTo(value);
      assertThat(saved.path("errorRetentionHours").asInt()).isEqualTo(value);
      assertThat(worker.control().threshold()).isEqualTo(value);
      assertThat(send("GET", "/settings", admin, null, 200)).isEqualTo(saved);
    }
    assertThat(audits()).isEqualTo(3);
  }
}
